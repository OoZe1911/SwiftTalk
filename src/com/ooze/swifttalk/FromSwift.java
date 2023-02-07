package com.ooze.swifttalk;

import java.util.HashSet;
import java.util.Set;

public class FromSwift extends Thread {
	public static String QMGRHOST = null;
	public static String QMGRNAME=null;
	public static int QMGRPORT=1414;
	public static String CHANNEL=null;
	public static String REPLY_TO_QUEUE=null;
	public static String QUEUE_ACK_SWIFT=null;
	public static String QUEUE_FROM_SWIFT=null;
	public static int SLEEPING_DURATION=10;

	public FromSwift() {
		super();
	}

	public void run() {
		getMessagesFromSAA();
	}

	public void getMessagesFromSAA() {
		// Compute queues to aqcuire
		String[] queue_list_with_potential_duplicates = {REPLY_TO_QUEUE, QUEUE_ACK_SWIFT, QUEUE_FROM_SWIFT};
		String[] queue_list = removeDuplicates(queue_list_with_potential_duplicates);

	}


	public static String[] removeDuplicates(String[] array) {
		Set<String> set = new HashSet<>();
		for (String s : array) {
			set.add(s);
		}
		return set.toArray(new String[0]);
	}

}