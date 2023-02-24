package com.ooze.utils;

import java.io.StringReader;
import java.io.StringWriter;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.nio.charset.StandardCharsets;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class SwiftUtils {
	
	public static String LAU = null;
	
	// Calculate HMAC_SHA256 and return result in base64
	public static String calculateHmacSha256(String key, String message) throws NoSuchAlgorithmException, InvalidKeyException {
		SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		Mac hmacSha256 = Mac.getInstance("HmacSHA256");
		hmacSha256.init(keySpec);
		byte[] hmacBytes = hmacSha256.doFinal(message.getBytes(StandardCharsets.UTF_8));
		return Base64.getEncoder().encodeToString(hmacBytes);
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
			factory.setExpandEntityReferences(false);
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
			if(XML_canonised.indexOf("<Saa:Revision>") != -1) {
				XML_canonised = XML_canonised.substring(XML_canonised.indexOf("<Saa:Revision>"));
				XML_canonised = "<Saa:DataPDU xmlns:Saa=\"urn:swift:saa:xsd:saa.2.0\">" + XML_canonised;
			} else {
				if(XML_canonised.indexOf("<ds:SignedInfo>") != -1 && XML_canonised.indexOf("<ds:SignedInfo>") > 0) {
					XML_canonised = XML_canonised.substring(XML_canonised.indexOf("<ds:SignedInfo>"),XML_canonised.indexOf("</ds:Signature>"));
				}
			}
			return XML_canonised;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}