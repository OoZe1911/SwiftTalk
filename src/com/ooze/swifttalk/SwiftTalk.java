package com.ooze.swifttalk;

import java.util.Properties;

import com.ooze.utils.FileUtils;

public class SwiftTalk {
	// Token to quit SwiftTalk
	public static boolean exit=false;
	public static Thread thread_to_swift = null;

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
		if(!FileUtils.fileExists(ToSwift.FOLDER_TO_SWIFT))
			System.out.println("ERROR : Folder does not exist.");
		ToSwift.QMGRHOST = conf.getProperty("QMGRHOST");
		System.out.println("QMGRHOST = " + ToSwift.QMGRHOST);
		ToSwift.QMGRNAME = conf.getProperty("QMGRNAME");
		System.out.println("QMGRNAME = " + ToSwift.QMGRNAME);
		ToSwift.QMGRPORT = Integer.parseInt(conf.getProperty("QMGRPORT"));
		System.out.println("QMGRPORT = " + ToSwift.QMGRPORT);
		ToSwift.CHANNEL = conf.getProperty("CHANNEL");
		System.out.println("CHANNEL = " + ToSwift.CHANNEL);
		ToSwift.QUEUE_TO_SWIFT = conf.getProperty("QUEUE_TO_SWIFT");
		System.out.println("QUEUE_TO_SWIFT = " + ToSwift.QUEUE_TO_SWIFT);
		ToSwift.SLEEPING_DURATION = Integer.parseInt(conf.getProperty("SLEEPING_DURATION"));
		System.out.println("SLEEPING_DURATION = " + ToSwift.SLEEPING_DURATION);
		System.out.println("SwiftTalk is ready for business.");
		System.out.println("-------------------------------------------------");

		// Running thread in charge of sending messages to SAA
		thread_to_swift = new ToSwift();
		thread_to_swift.start();
		
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
		while (thread_to_swift.isAlive()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("SwiftTalk stopped.");
	}
}