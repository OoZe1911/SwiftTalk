package com.ooze.swifttalk;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import com.ooze.utils.FileUtils;
import com.ooze.utils.SwiftUtils;

public class SwiftTalk {
	// Token to quit SwiftTalk
	public static boolean exit=false;
	public static Thread thread_to_swift = null;
	public static Thread thread_from_swift = null;

	public static void main(String[] args) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				SwiftTalk.handleJVMShutdown();
			}
		});

		System.out.println("-------------------------------------------------");
		System.out.println("SwiftTalk v0.1 is starting - (c) 2023 / TALAN SAS");
		System.out.println("-------------------------------------------------");

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

		ToSwift.QMGRHOST = conf.getProperty("QMGRHOST");
		FromSwift.QMGRHOST = ToSwift.QMGRHOST;
		System.out.println("QMGRHOST = " + ToSwift.QMGRHOST);

		ToSwift.QMGRNAME = conf.getProperty("QMGRNAME");
		FromSwift.QMGRNAME = ToSwift.QMGRNAME;
		System.out.println("QMGRNAME = " + ToSwift.QMGRNAME);

		ToSwift.QMGRPORT = Integer.parseInt(conf.getProperty("QMGRPORT"));
		FromSwift.QMGRPORT = ToSwift.QMGRPORT;
		System.out.println("QMGRPORT = " + ToSwift.QMGRPORT);

		ToSwift.CHANNEL = conf.getProperty("CHANNEL");
		FromSwift.CHANNEL = ToSwift.CHANNEL;
		System.out.println("CHANNEL = " + ToSwift.CHANNEL);

		ToSwift.QUEUE_TO_SWIFT = conf.getProperty("QUEUE_TO_SWIFT");
		System.out.println("QUEUE_TO_SWIFT = " + ToSwift.QUEUE_TO_SWIFT);

		ToSwift.REPLY_TO_QUEUE = conf.getProperty("REPLY_TO_QUEUE");
		FromSwift.REPLY_TO_QUEUE = ToSwift.REPLY_TO_QUEUE;
		System.out.println("REPLY_TO_QUEUE = " + ToSwift.REPLY_TO_QUEUE);

		FromSwift.QUEUE_ACK_SWIFT = conf.getProperty("QUEUE_ACK_SWIFT");
		System.out.println("QUEUE_ACK_SWIFT = " + FromSwift.QUEUE_ACK_SWIFT);

		FromSwift.QUEUE_FROM_SWIFT = conf.getProperty("QUEUE_FROM_SWIFT");
		System.out.println("QUEUE_FROM_SWIFT = " + FromSwift.QUEUE_FROM_SWIFT);

		SwiftUtils.LAU = conf.getProperty("LAU");
		System.out.println("LAU = " + SwiftUtils.LAU);
		
		ToSwift.SLEEPING_DURATION = Integer.parseInt(conf.getProperty("SLEEPING_DURATION"));
		FromSwift.SLEEPING_DURATION = ToSwift.SLEEPING_DURATION;
		System.out.println("SLEEPING_DURATION = " + ToSwift.SLEEPING_DURATION);

		System.out.println("-------------------------------------------------");
		System.out.println("SwiftTalk is ready for business.");

		// TEST LAU
		try {
			String file_content = FileUtils.readFile("MT299_WITHOUTLAU.xml");
			String XML_canonised = SwiftUtils.XMLcanonisation(file_content);
			System.out.println(FileUtils.calculateHash(XML_canonised));
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		// Running thread in charge of sending messages to SAA
		thread_to_swift = new ToSwift();
		thread_to_swift.start();

		// Running thread in charge of receiving messages from SAA
		thread_from_swift = new FromSwift();
		thread_from_swift.start();

		
		// Wait in infinite loop
		//Thread currentThread = Thread.currentThread();
		try {
			thread_to_swift.join();
			//currentThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void handleJVMShutdown() {
		System.out.println("Trying to stop SwiftTalk...");
		exit=true;
		while (thread_to_swift.isAlive() && thread_from_swift.isAlive()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("SwiftTalk stopped.");
	}
}