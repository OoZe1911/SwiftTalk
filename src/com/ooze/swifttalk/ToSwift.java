// This class is in charge of getting files from FOLDER_TO_SWIFT directory and push each file in MQ
// Files must contain SWIFT message in SAA XML v2 format

package com.ooze.swifttalk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class ToSwift extends Thread {
	public static String FOLDER_TO_SWIFT = null;
	public static String QMGRHOST = null;
	public static String QMGRNAME=null;
	public static int QMGRPORT=1414;
	public static String CHANNEL=null;
	public static String QUEUE_TO_SWIFT=null;
	public static int SLEEPING_DURATION=10;

	public ToSwift() {
		super();
	}

	public void run() {
		sendMessagesToSAA();
	}
	
	public void sendMessagesToSAA() {
		while(!SwiftTalk.exit) {
			
			// Scanning folder
			Path directory = Paths.get(FOLDER_TO_SWIFT);
			try (Stream<Path> stream = Files.list(directory)) {
					stream.forEach(file -> {
							if (Files.isRegularFile(file)) {
								System.out.println("File found : " + file.getFileName());
							}
					});
			} catch (IOException e) {
				e.printStackTrace();
			}

			// Sleeping
			System.out.println("Seelping for " + SLEEPING_DURATION + " seconds.");
			try {
				Thread.sleep(1000 * SLEEPING_DURATION);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("\t-> ToSwift() thread stopped.");
	}
}
