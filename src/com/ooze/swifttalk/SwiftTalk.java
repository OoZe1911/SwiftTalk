package com.ooze.swifttalk;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import com.ooze.bean.ConnectionParams;
import com.ooze.utils.FileUtils;

public class SwiftTalk {
	// Token to quit SwiftTalk
	public static boolean exit=false;
	public static Thread thread_to_swift = null;
	public static Thread thread_from_swift = null;

	public static void main(String[] args) {
		System.out.println("---------------------------");
		System.out.println("SwiftTalk v0.1 is starting ");
		System.out.println("---------------------------");

		// Reading configuration file
		Properties conf = null;
		try {
			conf = FileUtils.readPropertiesFile("SwiftTalk.properties");
		}catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

		ToSwift.FOLDER_TO_SWIFT = conf.getProperty("FOLDER_TO_SWIFT");
		System.out.println("FOLDER_TO_SWIFT = " + ToSwift.FOLDER_TO_SWIFT);
		if(!FileUtils.fileExists(ToSwift.FOLDER_TO_SWIFT)) {
			System.out.println("ERROR : Folder does not exist.");
			System.exit(-1);
		}

		ToSwift.ARCHIVE_FOLDER = conf.getProperty("ARCHIVE_FOLDER");
		System.out.println("ARCHIVE_FOLDER = " + ToSwift.ARCHIVE_FOLDER);
		if(!FileUtils.fileExists(ToSwift.ARCHIVE_FOLDER)) {
			System.out.println("ERROR : Folder does not exist.");
			System.exit(-1);
		}

		int retention_period = Integer.parseInt(conf.getProperty("RETENTION_PERIOD"));
		System.out.println("Retention period for archived files (in days) : " + retention_period);
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
		System.out.println("Removing old files ---");
		File archive_folder = new File(ToSwift.ARCHIVE_FOLDER);
		File[] children = archive_folder.listFiles();
		int nb = 0;
		for (int j = 0; j < children.length; j++) {
			File file = children[j];
			if (!file.isDirectory()) {
				Date fileDate = new Date(file.lastModified());
				if (fileDate.before(cal.getTime())) {
					System.out.println("File deleted : " + file.getName() + " - " + fileDate);
					file.delete();
					nb++;
				}
			}
		}
		System.out.println("--- " + nb + " old files deleted.");

		ConnectionParams connectionParams = new ConnectionParams();
		connectionParams.setQmgrHost(conf.getProperty("QMGRHOST"));
		System.out.println("QMGRHOST = " + connectionParams.getQmgrHost());

		connectionParams.setQmgrName(conf.getProperty("QMGRNAME"));
		System.out.println("QMGRNAME = " + connectionParams.getQmgrName());

		connectionParams.setQmgrPort(Integer.parseInt(conf.getProperty("QMGRPORT")));
		System.out.println("QMGRPORT = " + connectionParams.getQmgrPort());

		connectionParams.setChannel(conf.getProperty("CHANNEL"));
		System.out.println("CHANNEL = " + connectionParams.getChannel());

		connectionParams.setQueueToSwift(conf.getProperty("QUEUE_TO_SWIFT"));
		System.out.println("QUEUE_TO_SWIFT = " + connectionParams.getQueueToSwift());

		connectionParams.setReplyToQueue(conf.getProperty("REPLY_TO_QUEUE"));
		System.out.println("REPLY_TO_QUEUE = " + connectionParams.getReplyToQueue());

		connectionParams.setQueueAckSwift(conf.getProperty("QUEUE_ACK_SWIFT"));
		System.out.println("QUEUE_ACK_SWIFT = " + connectionParams.getQueueAckSwift());

		connectionParams.setQueueFromSwift(conf.getProperty("QUEUE_FROM_SWIFT"));
		System.out.println("QUEUE_FROM_SWIFT = " + connectionParams.getQueueFromSwift());

		// MQ TLS
		connectionParams.setCypher(conf.getProperty("CYPHER"));
		System.out.println("CYPHER = " + connectionParams.getCypher());

		connectionParams.setTrustStore(conf.getProperty("TRUSTSORE"));
		System.out.println("TRUSTSORE = " + connectionParams.getTrustStore());
		if (connectionParams.getTrustStore() != null && connectionParams.getTrustStore().length() > 0)
			System.setProperty("javax.net.ssl.trustStore", connectionParams.getTrustStore());

		connectionParams.setTrustStorePassword(conf.getProperty("TRUSTSORE_PASSWORD"));
		System.out.println("TRUSTSORE_PASSWORD = " + connectionParams.getTrustStorePassword());
		if (connectionParams.getTrustStorePassword() != null && connectionParams.getTrustStorePassword().length() > 0)
			System.setProperty("javax.net.ssl.trustStorePassword", connectionParams.getTrustStorePassword());

		connectionParams.setKeyStore(conf.getProperty("KEYSTORE"));
		System.out.println("KEYSTORE = " + connectionParams.getKeyStore());
		if (connectionParams.getKeyStore() != null && connectionParams.getKeyStore().length() > 0)
			System.setProperty("javax.net.ssl.keyStore", connectionParams.getKeyStore());

		connectionParams.setKeyStorePassword(conf.getProperty("KEYSTORE_PASSWORD"));
		System.out.println("KEYSTORE_PASSWORD = " + connectionParams.getKeyStorePassword());
		if (connectionParams.getKeyStorePassword() != null && connectionParams.getKeyStorePassword().length() > 0)
			System.setProperty("javax.net.ssl.keyStorePassword", connectionParams.getKeyStorePassword());

		connectionParams.setSslPeer(conf.getProperty("SSLPEER"));
		System.out.println("SSLPEER = " + connectionParams.getSslPeer());

		// LAU (Empty, HMAC, AES)
		connectionParams.setLauType(conf.getProperty("LAU_TYPE"));
		System.out.println("LAU_TYPE = " + connectionParams.getLauType());
		connectionParams.setLauKey(conf.getProperty("LAU_KEY"));
		System.out.println("LAU_KEY = " + conf.getProperty("LAU_KEY"));

		connectionParams.setSleepingDuration(Integer.parseInt(conf.getProperty("SLEEPING_DURATION")));
		System.out.println("SLEEPING_DURATION = " + connectionParams.getSleepingDuration());

		System.out.println("-------------------------------------------------");
		System.out.println("SwiftTalk is ready for business.");

		// Running thread in charge of sending messages to SAA
		if (connectionParams.getQueueToSwift() != null && connectionParams.getQueueToSwift().trim().length() > 0) {
			System.out.println("-> Starting sending thread");
			thread_to_swift = new ToSwift(connectionParams);
			thread_to_swift.start();
		}

		// Running thread in charge of receiving messages from SAA
		if(connectionParams.getQueueFromSwift() != null && connectionParams.getQueueFromSwift().trim().length() > 0) {
			System.out.println("-> Starting receiving thread");
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
		exit=true;
		while ((thread_to_swift != null && thread_to_swift.isAlive()) || 
				   (thread_from_swift != null && thread_from_swift.isAlive())) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("SwiftTalk stopped.");
	}
}