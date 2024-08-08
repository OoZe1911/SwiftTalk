package com.ooze.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.xml.security.c14n.Canonicalizer;
import org.w3c.dom.Document;

public class LAUUtils {

    static {
        org.apache.xml.security.Init.init();
    }

	// Cipher parameters that are used
	public static final int AES_KEY_SIZE = 256; // in bits
	public static final int IV_LENGTH = 12; // in bytes
	public static final int GCM_TAG_LENGTH = 16; // in bytes

	// Compute LAU for FIN message (inputText should be the FIN message without block S Security)
	public static String computeLAUforFIN(String inputText, String LAU) {
		String returningText = null;

		try {

			SecretKeySpec key = new SecretKeySpec((LAU).getBytes("UTF-8"), "hmac256");
			Mac mac = Mac.getInstance("hmacsha256");
			mac.init(key);

			byte[] bytes = mac.doFinal(inputText.getBytes("ASCII"));

			StringBuilder hash = new StringBuilder();
			for(int i = 0; i < bytes.length; i++) {
				String hex = Integer.toHexString(0xff & bytes[i]);
				if(hex.length() == 1) hex = "0" + hex;
				hash.append(hex);
			}
			returningText = hash.toString().toUpperCase();

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return returningText;
	}

	// Compute LAU for DataPDU message
	public static String computeLAU(String dataPDU, String LAU_key) {
		try {
			// Step 0 : add the LAU tag
			dataPDU = dataPDU.replace("</Saa:DataPDU>", "<Saa:LAU></Saa:LAU></Saa:DataPDU>");

			// Step 1 : Convert string to XML document
			Document doc = convertStringToDocument(dataPDU);

			// Step 2 : Canonicalize the DataPDU and calculate SHA256 digest
			byte[] canonicalDataPDU = canonicalizeDocument(doc);
			byte[] sha256Hash = calculateSHA256(canonicalDataPDU);
			String sha256base64Encoded = Base64.getEncoder().encodeToString(sha256Hash);

			// Step 3 : Create signedInfo
			String signedInfo = "<ds:SignedInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n";
			signedInfo += "<ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>\n";
			signedInfo += "<ds:SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#hmac-sha256\"/>\n";
			signedInfo += "<ds:Reference URI=\"\">\n";
			signedInfo += "<ds:Transforms>\n";
			signedInfo += "<ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/>\n";
			signedInfo += "<ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>\n";
			signedInfo += "</ds:Transforms>\n";
			signedInfo += "<ds:DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"/>\n";
			signedInfo += "<ds:DigestValue>" + sha256base64Encoded + "</ds:DigestValue>\n";
			signedInfo += "</ds:Reference>\n";
			signedInfo += "</ds:SignedInfo>\n";

            // Convert signedInfo string to Document for canonicalization
            Document signedInfoDoc = convertStringToDocument(signedInfo);
            byte[] canonicalSignedInfo = canonicalizeDocument(signedInfoDoc);

            // Calculate HMAC-SHA256 of the canonicalized SignedInfo using the full LAU key
            byte[] secretKey = LAU_key.getBytes(StandardCharsets.UTF_8);
            byte[] hmacValue = calculateHMACSHA256(canonicalSignedInfo, secretKey);
            String hmacBase64 = Base64.getEncoder().encodeToString(hmacValue);

			// Step 4 : Add LAU part to the DataPDU
			dataPDU = dataPDU.substring(0, dataPDU.indexOf("<Saa:LAU>"));
			dataPDU += "<Saa:LAU><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n";
			dataPDU += signedInfo.replace("<ds:SignedInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">", "<ds:SignedInfo>");
			dataPDU += "<ds:SignatureValue>" + hmacBase64 + "</ds:SignatureValue>\n";
			dataPDU += "</ds:Signature></Saa:LAU></Saa:DataPDU>";

			return dataPDU;

		} catch (Exception ex) {
			return null;
		}
	}

	public static byte[] calculateSHA256(byte[] data) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		return digest.digest(data);
	}

	public static byte[] calculateHMACSHA256(byte[] data, byte[] secretKey) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, "HmacSHA256");
		mac.init(secretKeySpec);
		return mac.doFinal(data);
	}

	// Convert XML String to Document
	public static Document convertStringToDocument(String xmlStr) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		ByteArrayInputStream input = new ByteArrayInputStream(xmlStr.getBytes(StandardCharsets.UTF_8));
		return builder.parse(input);
	}

	// Canonicalize the Document
	public static byte[] canonicalizeDocument(Document doc) throws Exception {
		Canonicalizer canon = Canonicalizer.getInstance(Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		canon.canonicalizeSubtree(doc, outputStream);
		return outputStream.toByteArray();
	}

	// LAU with AES-GCM -> Encrypt string
	public static void encryptAESGCM(String keyString, String data, OutputStream out) throws Exception {
		// Convert the string data to InputStream
		InputStream input = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));

		// Call the existing encryptAESGCM method
		encryptAESGCM(keyString, input, out);
	}

	// LAU with AES-GCM -> Encrypt file
	public static void encryptAESGCM(String keyString, InputStream input, OutputStream out) throws Exception {
		// Initialise SecureRandom
		//SecureRandom random = SecureRandom.getInstanceStrong();
		SecureRandom random = new SecureRandom();

		// Initialise and generate the IV
		final byte[] iv = new byte[IV_LENGTH];
		random.nextBytes(iv);

		// Generate the key
		SecretKey key = new SecretKeySpec(keyString.getBytes(), 0, keyString.length(), "AES");

		// Initialise the cipher
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "SunJCE");
		GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
		cipher.init(Cipher.ENCRYPT_MODE, key, spec);

		// Write the IV at the beginning of the file (first 12 Bytes)
		out.write(iv);

		// Initialise cipher stream and write
		CipherOutputStream cos = new CipherOutputStream(out, cipher);
		byte[] buffer = new byte[1024 * 1024];
		int len;
		while ((len = input.read(buffer)) != -1) {
			cos.write(buffer, 0, len);
		}
		
		// Flush and close the output stream
		cos.flush();
		cos.close();
	}

	// LAU with AES-GCM -> Decrypt beginning of a file to string
	public static String decryptAESGCMToString(String keyString, File file) throws Exception {
		try (FileInputStream fis = new FileInputStream(file);
			 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			
			// Lire seulement les premiers 102400 bytes (ou la taille du fichier si plus petit)
			byte[] buffer = new byte[102400];
			int bytesRead = fis.read(buffer);
			byte[] encryptedData = Arrays.copyOf(buffer, bytesRead);

			try (ByteArrayInputStream bis = new ByteArrayInputStream(encryptedData)) {
				decryptAESGCM(keyString, bis, bos);
			}

			return new String(bos.toByteArray(), StandardCharsets.UTF_8);
		}
	}

	// LAU with AES-GCM -> Decrypt file
	public static void decryptAESGCM(String keyString, InputStream input, OutputStream out) throws Exception {
		// Read the IV at the beginning of the file (first 12 Bytes)
		final byte[] iv = new byte[IV_LENGTH];
		input.read(iv);

		// Generate the key
		SecretKey key = new SecretKeySpec(keyString.getBytes(), 0, keyString.length(), "AES");

		// Initialise the cipher
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "SunJCE");
		GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
		cipher.init(Cipher.DECRYPT_MODE, key, spec);

		// Initialise cipher stream and write
		CipherInputStream cis = new CipherInputStream(input, cipher);
		byte[] buffer = new byte[1024 * 1024];
		int len;
		while ((len = cis.read(buffer)) != -1) {
			out.write(buffer, 0, len);
		}

		// Flush and close the output stream
		out.flush();
		out.close();

		// Close the input stream
		cis.close();
	}
}
