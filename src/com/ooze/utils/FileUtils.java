package com.ooze.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Properties;

public class FileUtils {

	// Read plain text file
	public static String readFile(String path) {
		if(fileExists(path)) {
			byte[] encoded;
			try {
				encoded = Files.readAllBytes(Paths.get(path));
				return new String(encoded, StandardCharsets.UTF_8);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			return null;
		}
	}

	// Read external properties file
	public static Properties readPropertiesFile(String path) throws Exception {
		Properties props = new Properties();
		FileInputStream fis = new FileInputStream(path);
		props.load(fis);
		fis.close();
		return props;
	}

	// Check if file exists
	public static boolean fileExists(String filename) {
		File file = new File(filename);
		if(file.exists()) {
			return true;
		} else {
			return false;
		}
	}

	// Check if file is locked
	public static boolean isFileLocked(File file) {
		try {
			if (System.getProperty("os.name").indexOf("Windows") == -1) {
				String[] command = {"fuser", file.getPath()};
				try {
					ProcessBuilder builder = new ProcessBuilder(command);
					Process process = builder.start();
					BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
					@SuppressWarnings("unused")
					String line;
					boolean processLocking = false;
					while ((line = reader.readLine()) != null) {
						processLocking = true;
					}
					reader.close();
					if(processLocking)
						return true;
					else
						return false;
				} catch (Exception e) {
					e.printStackTrace();
					return true;
				}
			} 
			long taille = file.length();
			Thread.sleep(2000L);
			if (taille < file.length())
				return true; 
			return false;
		} catch (Exception e) {
			return true;
		} 
	}

	// Return SHA256 encoded in base64
	public static String calculateHash(String content) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(content.getBytes());
			return Base64.getEncoder().encodeToString(hash);
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		} 
	}

}
