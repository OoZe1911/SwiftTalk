package com.ooze.swifttalk;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import com.ooze.bean.ConnectionParams;
import com.ooze.utils.FileUtils;

public class SwiftTalk {
	private static final Logger logger = LogManager.getLogger(SwiftTalk.class);
	// Token to quit SwiftTalk
	public static boolean exit=false;
	public static Thread thread_to_swift = null;
	public static Thread thread_from_swift = null;

	public static void main(String[] args) {
		String log4jConfigFile = "log4j2.xml";
		Configurator.initialize(null, log4jConfigFile);
		logger.info("----- SwiftTalk v0.1 is starting -----");

		// Reading configuration file
		Properties conf = null;
		try {
			conf = FileUtils.readPropertiesFile("SwiftTalk.properties");
		}catch (Exception e) {
			logger.error("SwiftTalk.properties file not found.", e);
			System.exit(-1);
		}

		FromSwift.FOLDER_FROM_SWIFT = conf.getProperty("FOLDER_FROM_SWIFT");
		logger.info("FOLDER_FROM_SWIFT = " + FromSwift.FOLDER_FROM_SWIFT);

		ToSwift.FOLDER_TO_SWIFT = conf.getProperty("FOLDER_TO_SWIFT");
		logger.info("FOLDER_TO_SWIFT = " + ToSwift.FOLDER_TO_SWIFT);

		ToSwift.ARCHIVE_FOLDER = conf.getProperty("ARCHIVE_FOLDER");
		FromSwift.ARCHIVE_FOLDER = conf.getProperty("ARCHIVE_FOLDER");
		logger.info("ARCHIVE_FOLDER = " + ToSwift.ARCHIVE_FOLDER);
		if(!FileUtils.fileExists(ToSwift.ARCHIVE_FOLDER)) {
			logger.error("ERROR : Folder does not exist.");
			System.exit(-1);
		}

		int retention_period = Integer.parseInt(conf.getProperty("RETENTION_PERIOD"));
		logger.info("Retention period for archived files (in days) : " + retention_period);
		Calendar cal = Calendar.getInstance(TimeZone.getDefault());
		boolean chgtAnnee = false;
		if (cal.get(5) < retention_period - 1 && cal.get(2) == 1)
			chgtAnnee = true; 
		for (int i = 1; i < retention_period; i++)
			cal.roll(6, false); 
		cal.set(11, 0);
		cal.set(12, 0);
		cal.set(13, 0);
		cal.set(14, 0);
		if (chgtAnnee)
			cal.roll(1, false); 
		logger.info("Removing old files ---");
		File archive_folder = new File(ToSwift.ARCHIVE_FOLDER);
		File[] children = archive_folder.listFiles();
		int nb = 0;
		for (int j = 0; j < children.length; j++) {
			File file = children[j];
			if (!file.isDirectory()) {
				Date fileDate = new Date(file.lastModified());
				if (fileDate.before(cal.getTime())) {
					logger.info("File deleted : " + file.getName() + " - " + fileDate);
					file.delete();
					nb++;
				}
			}
		}
		logger.info("--- " + nb + " old files deleted.");

		ConnectionParams connectionParams = new ConnectionParams();
		connectionParams.setQmgrHost(conf.getProperty("QMGRHOST"));
		logger.info("QMGRHOST = " + connectionParams.getQmgrHost());

		connectionParams.setQmgrName(conf.getProperty("QMGRNAME"));
		logger.info("QMGRNAME = " + connectionParams.getQmgrName());

		connectionParams.setQmgrPort(Integer.parseInt(conf.getProperty("QMGRPORT")));
		logger.info("QMGRPORT = " + connectionParams.getQmgrPort());

		connectionParams.setChannel(conf.getProperty("CHANNEL"));
		logger.info("CHANNEL = " + connectionParams.getChannel());

		connectionParams.setQueueToSwift(conf.getProperty("QUEUE_TO_SWIFT"));
		logger.info("QUEUE_TO_SWIFT = " + connectionParams.getQueueToSwift());

		connectionParams.setReplyToQueue(conf.getProperty("REPLY_TO_QUEUE"));
		logger.info("REPLY_TO_QUEUE = " + connectionParams.getReplyToQueue());

		connectionParams.setQueueAckSwift(conf.getProperty("QUEUE_ACK_SWIFT"));
		logger.info("QUEUE_ACK_SWIFT = " + connectionParams.getQueueAckSwift());

		// Get list of queues from SWIFT
		String[] tokens = conf.getProperty("QUEUE_FROM_SWIFT").split(",");
		ArrayList<String> result = new ArrayList<>();
		for (String token : tokens) {
			result.add(token.trim());
		}
		connectionParams.setQueueFromSwift(result);
		logger.info("QUEUE_FROM_SWIFT = " + connectionParams.getQueueFromSwift());

		// MQ TLS
		connectionParams.setCypher(conf.getProperty("CYPHER"));
		logger.info("CYPHER = " + connectionParams.getCypher());

		connectionParams.setTrustStore(conf.getProperty("TRUSTSORE"));
		logger.info("TRUSTSORE = " + connectionParams.getTrustStore());
		if (connectionParams.getTrustStore() != null && connectionParams.getTrustStore().length() > 0)
			System.setProperty("javax.net.ssl.trustStore", connectionParams.getTrustStore());

		connectionParams.setTrustStorePassword(conf.getProperty("TRUSTSORE_PASSWORD"));
		logger.info("TRUSTSORE_PASSWORD = " + connectionParams.getTrustStorePassword());
		if (connectionParams.getTrustStorePassword() != null && connectionParams.getTrustStorePassword().length() > 0)
			System.setProperty("javax.net.ssl.trustStorePassword", connectionParams.getTrustStorePassword());

		connectionParams.setKeyStore(conf.getProperty("KEYSTORE"));
		logger.info("KEYSTORE = " + connectionParams.getKeyStore());
		if (connectionParams.getKeyStore() != null && connectionParams.getKeyStore().length() > 0)
			System.setProperty("javax.net.ssl.keyStore", connectionParams.getKeyStore());

		connectionParams.setKeyStorePassword(conf.getProperty("KEYSTORE_PASSWORD"));
		logger.info("KEYSTORE_PASSWORD = " + connectionParams.getKeyStorePassword());
		if (connectionParams.getKeyStorePassword() != null && connectionParams.getKeyStorePassword().length() > 0)
			System.setProperty("javax.net.ssl.keyStorePassword", connectionParams.getKeyStorePassword());

		connectionParams.setSslPeer(conf.getProperty("SSLPEER"));
		logger.info("SSLPEER = " + connectionParams.getSslPeer());

		// LAU (Empty, HMAC, AES)
		connectionParams.setLauType(conf.getProperty("LAU_TYPE"));
		logger.info("LAU_TYPE = " + connectionParams.getLauType());
		connectionParams.setLauKey(conf.getProperty("LAU_KEY"));
		logger.info("LAU_KEY = " + conf.getProperty("LAU_KEY"));

		connectionParams.setSleepingDuration(Integer.parseInt(conf.getProperty("SLEEPING_DURATION")));
		logger.info("SLEEPING_DURATION = " + connectionParams.getSleepingDuration());

		System.out.println("SwiftTalk is ready for business.");
		logger.info("SwiftTalk is ready for business.");
		
		// Running thread in charge of sending messages to SAA
		if (connectionParams.getQueueToSwift() != null && connectionParams.getQueueToSwift().trim().length() > 0) {
			if(!FileUtils.fileExists(ToSwift.FOLDER_TO_SWIFT)) {
				logger.error("ERROR : Folder " + ToSwift.FOLDER_TO_SWIFT + " does not exist.");
				System.exit(-1);
			}
			logger.info("-> Starting sending thread");
			thread_to_swift = new ToSwift(connectionParams);
			thread_to_swift.start();
		}

		// Running thread in charge of receiving messages from SAA
		if(connectionParams.getQueueFromSwift() != null && connectionParams.getQueueFromSwift().size() > 0) {
			if(!FileUtils.fileExists(FromSwift.FOLDER_FROM_SWIFT)) {
				logger.error("ERROR : Folder " + FromSwift.FOLDER_FROM_SWIFT + " does not exist.");
				System.exit(-1);
			}
			logger.info("-> Starting receiving thread");
			thread_from_swift = new FromSwift(connectionParams);
			thread_from_swift.start();
		}

		// Wait in infinite loop
		final Object lock = new Object();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				synchronized (lock) {
					lock.notify();
					SwiftTalk.handleJVMShutdown();
				}
			}
		});

		try {
			if (thread_to_swift != null) {
				thread_to_swift.join();
			}
			if (thread_from_swift != null) {
				thread_from_swift.join();
			}
			synchronized (lock) {
				lock.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void handleJVMShutdown() {
		System.out.println("Trying to stop SwiftTalk...");
		logger.info("Trying to stop SwiftTalk...");
		exit=true;
		while ((thread_to_swift != null && thread_to_swift.isAlive()) || 
				(thread_from_swift != null && thread_from_swift.isAlive())) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				logger.error("Can not stop thread", e);
			}
		}
		logger.info("SwiftTalk stopped.");
		System.out.println("SwiftTalk stopped.");
	}
}