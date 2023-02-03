// This class is in charge of getting files from FOLDER_TO_SWIFT directory and push each file in MQ
// Files must contain SWIFT message in SAA XML v2 format

package com.ooze.swifttalk;

public class ToSwift extends Thread {
	public static String FOLDER_TO_SWIFT = null;
	public static String QMGRHOST = null;
	public static String QMGRNAME=null;
	public static int QMGRPORT=1414;
	public static String CHANNEL=null;
	public static String QUEUE_TO_SWIFT=null;
	public static int SLEEP_DURATION=10;

	public ToSwift() {
		super();
	}

	public void run() {
		sendMessagesToSAA();
	}
	
	public void sendMessagesToSAA() {
		// Scanning folder
		
	}
}
