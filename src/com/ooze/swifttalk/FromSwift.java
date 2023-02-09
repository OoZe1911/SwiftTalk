package com.ooze.swifttalk;

import java.util.HashSet;
import java.util.Set;

import com.ibm.mq.MQException;
import com.ibm.mq.MQQueue;
import com.ooze.manager.MQManager;

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
		System.out.println("Plouf");
		//getMessagesFromSAA();
	}

	public void getMessagesFromSAA() {
		// Compute queues to aqcuire
		String[] queue_list_with_potential_duplicates = {REPLY_TO_QUEUE, QUEUE_ACK_SWIFT, QUEUE_FROM_SWIFT};
		String[] queue_list = removeDuplicates(queue_list_with_potential_duplicates);

		// Scanning queues
		while(!SwiftTalk.exit) {
			// Try to connect to MQ
			MQManager queueManager = new MQManager(QMGRHOST, QMGRNAME, QMGRPORT, CHANNEL);

			boolean message_found = true;
			while(message_found) {
				for(int i=0; i < queue_list.length; i++) {
					// Acquire queue
					MQQueue queue = queueManager.initConnctionToQueue(queue_list[i]);
					// Get message
					queueManager.mqGet(queue);
					try {
						queue.close();
					} catch (MQException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					message_found = false;
				}
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
	}


	public static String[] removeDuplicates(String[] array) {
		Set<String> set = new HashSet<>();
		for (String s : array) {
			set.add(s);
		}
		return set.toArray(new String[0]);
	}

}