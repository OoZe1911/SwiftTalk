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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ibm.mq.MQException;
import com.ibm.mq.MQQueue;
import com.ooze.bean.ConnectionParams;
import com.ooze.manager.MQManager;
import com.ooze.utils.FileUtils;

public class ToSwift extends Thread {
	private static final Logger logger = LogManager.getLogger(ToSwift.class);
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
			MQManager queueManager = null;
			try {
				queueManager = new MQManager(connectionParams.getQmgrHost(), connectionParams.getQmgrName(), connectionParams.getQmgrPort(), connectionParams.getChannel(), connectionParams.getCypher(), connectionParams.getSslPeer());
				logger.debug("Connected to Queue Manager " + connectionParams.getQmgrName());
			} catch (Exception e) {
				logger.error("Can not connect to Queue Manager " + connectionParams.getQmgrName());
				queueManager = null;
			}

			// Initialize queue connection
			if(queueManager != null) {
				MQQueue queue = null;
				try {
					queue = queueManager.initConnctionToQueue(connectionParams.getQueueToSwift());
					logger.debug("Queue " + queue + " opened successfully");
				} catch (Exception e) {
					logger.error("Cannot open queue : " + queue, e);
				}
	
				// Scanning folder
				Path directory = Paths.get(FOLDER_TO_SWIFT);
				try (Stream<Path> stream = Files.list(directory)) {
					MQQueue finalQueue = queue;
					MQManager finalQueueManager = queueManager;
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
										logger.error("File not found", e);
									}
	
									// Put message to queue
									finalQueueManager.mqPut(finalQueue, content.toString(), connectionParams);
									logger.info("File : " + file.toString() + " sent to MQ queue " + connectionParams.getQueueToSwift() + ".");
	
									// Archive file
									archiveFile(file.getFileName().toString());
	
								} else {
									logger.warn("File " + file.toString() + " is locked, skipping.");
								}
							}
	
						}
					});
	
					if(SwiftTalk.exit)
						break;
	
				} catch (IOException e) {
					logger.error("Can not send file to MQ queue", e);
				}
	
				// Close queue
				try {
					queue.close();
					logger.debug("Queue " + queue.getResolvedQName() + " closed.");
				} catch (MQException e) {
					logger.error("Can not close queue " + queue.getResolvedQName(), e);
				}
	
				// Close Queue Manager connection
				try {
					queueManager.closeConnection();
					logger.debug("Queue Manager " + connectionParams.getQmgrName() + " connection closed.");
				} catch (Exception e) {
					logger.error("Can not close connection to Queue Manager " + connectionParams.getQmgrName());
				}

			} else {
				logger.debug("Not connected to Queue Manager");
			}

			// Sleeping
			try {
				logger.debug("Sleeping for " + connectionParams.getSleepingDuration() + " seconds");
				Thread.sleep(1000 * connectionParams.getSleepingDuration());
			} catch (InterruptedException e) {
				logger.error("Error while sleeping", e);
			}

		}

	}

	public static boolean archiveFile(String file) {
		File physicalFile = new File(FOLDER_TO_SWIFT + File.separator + file);
		if (physicalFile.renameTo(new File(ARCHIVE_FOLDER, physicalFile.getName()))) {
			logger.info("File " + physicalFile.toString() + " archived in " + ARCHIVE_FOLDER);
			return true;
		}
		if (FileUtils.fileExists(file)) {
			logger.warn("A file with a same filename has been already archived");
			File oldOne = new File(ARCHIVE_FOLDER + File.separator + physicalFile.getName());
			oldOne.delete();
			if (!physicalFile.renameTo(new File(ARCHIVE_FOLDER, physicalFile.getName()))) {
				logger.info("Can not archives file " + physicalFile.toString() + " in " + ARCHIVE_FOLDER);
				return false;
			}
			return true;
		}
		logger.warn("File does not exist anymore, can not archive.");
		return true;
	}

}