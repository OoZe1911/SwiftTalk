package com.ooze.utils;

import java.io.StringReader;
import java.io.StringWriter;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class SwiftUtils {
	
	public static String LAU = null;
	
	// Calculate HMAC_SHA256
	public static byte[] calculateHmacSha256(byte[] key, byte[] message) throws NoSuchAlgorithmException, InvalidKeyException {
		Mac hmacSha256 = Mac.getInstance("HmacSHA256");
		SecretKeySpec keySpec = new SecretKeySpec(key, "HmacSHA256");
		hmacSha256.init(keySpec);
		return hmacSha256.doFinal(message);
	}

	public static String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	// Calculate SHA256
	public static byte[] calculateSha256(byte[] message) throws NoSuchAlgorithmException {
		MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
		return sha256.digest(message);
	}

	// XML canonisation
	public static String XMLcanonisation(String xml) {
		try {
			// Document builder
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			// Parser
			InputSource source = new InputSource(new StringReader(xml));
			Document document = builder.parse(source);
			// Canonisation
			StringWriter writer = new StringWriter();
			TransformerFactory tf = TransformerFactory.newInstance();
			tf.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
			tf.newTransformer().transform(new DOMSource(document), new StreamResult(writer));
			String XML_canonised = writer.toString();
			// SAA Specific
			XML_canonised = XML_canonised.substring(XML_canonised.indexOf("<Saa:Revision"));
			XML_canonised = "<Saa:DataPDU xmlns:Saa=\"urn:swift:saa:xsd:saa.2.0\">" + XML_canonised;
			return XML_canonised;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}