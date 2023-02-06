// This class is in charge of getting files from FOLDER_TO_SWIFT directory and push each file in MQ
// Files must contain SWIFT message in SAA XML v2 format

package com.ooze.swifttalk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.stream.Stream;

import com.ooze.manager.MQManager;

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
		// Try to connect to MQ
		MQManager queueManager = new MQManager(QMGRHOST, QMGRNAME, QMGRPORT, CHANNEL);

		while(!SwiftTalk.exit) {
			// Initialize queue connection
			queueManager.initConnction(QUEUE_TO_SWIFT);

			// Scanning folder
			Path directory = Paths.get(FOLDER_TO_SWIFT);
			try (Stream<Path> stream = Files.list(directory)) {
					stream.forEach(file -> {
							if (Files.isRegularFile(file)) {
								System.out.println("File found : " + file.getFileName());

								// Get file content
								StringBuilder content = new StringBuilder();
								try (Scanner scanner = new Scanner(new File(file.toString()))) {
									while (scanner.hasNextLine()) {
											content.append(scanner.nextLine()).append("\n");
									}
								} catch (FileNotFoundException e) {
									e.printStackTrace();
								}

								// Put message to queue
								queueManager.mqPut(content.toString());
							}
					});
				if(SwiftTalk.exit)
					break;
			} catch (IOException e) {
				e.printStackTrace();
			}

			// Close queue connection
			queueManager.closeConnection();

			// Sleeping
			try {
				Thread.sleep(1000 * SLEEPING_DURATION);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

		System.out.println("\t-> ToSwift() thread stopped.");
	}

}