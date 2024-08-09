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

import com.ibm.mq.MQException;
import com.ibm.mq.MQQueue;
import com.ooze.bean.ConnectionParams;
import com.ooze.manager.MQManager;
import com.ooze.utils.FileUtils;

public class ToSwift extends Thread {
	private ConnectionParams connectionParams;
	public static String FOLDER_TO_SWIFT = null;
	public static String ARCHIVE_FOLDER = null;

	public ToSwift(ConnectionParams connectionParams) {
		super();
		this.connectionParams = connectionParams;
	}

	public void run() {
		sendMessagesToSAA();
	}

	public void sendMessagesToSAA() {

		while(!SwiftTalk.exit) {
			// Try to connect to MQ
			MQManager queueManager = new MQManager(connectionParams.getQmgrHost(), connectionParams.getQmgrName(), connectionParams.getQmgrPort(), connectionParams.getChannel(), connectionParams.getCypher(), connectionParams.getSslPeer());

			// Initialize queue connection
			MQQueue queue = queueManager.initConnctionToQueue(connectionParams.getQueueToSwift());

			// Scanning folder
			Path directory = Paths.get(FOLDER_TO_SWIFT);
			try (Stream<Path> stream = Files.list(directory)) {
				stream.forEach(file -> {
					if (Files.isRegularFile(file)) {

						if(!file.getFileName().toString().substring(0, 1).equals(".")) {

							if (!FileUtils.isFileLocked(new File(file.toString()))) {

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
								queueManager.mqPut(queue, content.toString(), connectionParams);
								System.out.println("File : " + file.toString() + " sent to MQ queue " + connectionParams.getQueueToSwift() + ".");

								// Archive file
								archiveFile(file.getFileName().toString());

							} else {
								System.out.println("File " + file.toString() + " is locked, skipping.");
							}
						}

					}
				});

				if(SwiftTalk.exit)
					break;

			} catch (IOException e) {
				e.printStackTrace();
			}

			// Close queue
			try {
				queue.close();
			} catch (MQException ex) {
				ex.printStackTrace();
			}

			// Close queue connection
			queueManager.closeConnection();

			// Sleeping
			try {
				Thread.sleep(1000 * connectionParams.getSleepingDuration());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

	}

	public static boolean archiveFile(String file) {
		File physicalFile = new File(FOLDER_TO_SWIFT + File.separator + file);
		if (physicalFile.renameTo(new File(ARCHIVE_FOLDER, physicalFile.getName()))) {
			System.out.println("File " + physicalFile.toString() + " archived in " + ARCHIVE_FOLDER);
			return true;
		}
		if (FileUtils.fileExists(file)) {
			System.out.println("A file with a same filename has been already archived");
			File oldOne = new File(ARCHIVE_FOLDER + File.separator + physicalFile.getName());
			oldOne.delete();
			if (!physicalFile.renameTo(new File(ARCHIVE_FOLDER, physicalFile.getName()))) {
				System.out.println("Can not archives file " + physicalFile.toString() + " in " + ARCHIVE_FOLDER);
				return false;
			}
			return true;
		}
		System.out.println("File does not exist anymore, can not archive.");
		return true;
	}

}