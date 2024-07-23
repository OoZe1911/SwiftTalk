package com.ooze.swifttalk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
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
		getMessagesFromSAA();
	}

	public void getMessagesFromSAA() {
		// Compute queues to acquire
		String[] queue_list_with_potential_duplicates = {REPLY_TO_QUEUE, QUEUE_ACK_SWIFT, QUEUE_FROM_SWIFT};
		String[] queue_list = removeDuplicates(queue_list_with_potential_duplicates);

		// Scanning queues
		while(!SwiftTalk.exit) {
			// Try to connect to MQ
			MQManager queueManager = new MQManager(QMGRHOST, QMGRNAME, QMGRPORT, CHANNEL, null, null);

			// Open all queues
			ArrayList<MQQueue> queue_connexions = new ArrayList<MQQueue>();
			for(int i=0; i < queue_list.length; i++) {
				queue_connexions.add(queueManager.initConnctionToQueue(queue_list[i]));
			}

			// Read messages in queues
			boolean message_found = true;
			while(message_found) {

				message_found = false;

				for(int i=0; i < queue_connexions.size(); i++) {

					// Get message
					MQMessage message = queueManager.mqGet(queue_connexions.get(i));
					if (message != null) {
						message_found = true;

						// Process message
						// messageType = 4 + feedback = 275 = PAN
						// messageType = 4 + feedback = 65537 = NAN
						// messageType = 8 + feedback = 0 = SWIFT Message

						// PAN SAA XML v2 sample :
						// <?xml version="1.0" encoding="UTF-8" ?><Saa:DataPDU xmlns:Saa="urn:swift:saa:xsd:saa.2.0" xmlns:Sw="urn:swift:snl:ns.Sw" xmlns:SwInt="urn:swift:snl:ns.SwInt" xmlns:SwGbl="urn:swift:snl:ns.SwGbl" xmlns:SwSec="urn:swift:snl:ns.SwSec"><Saa:Revision>2.0.8</Saa:Revision><Saa:Header><Saa:MessageStatus><Saa:SenderReference>IMGTCBEBEECL299CANCELLATION1$2301102</Saa:SenderReference><Saa:SeqNr>000061</Saa:SeqNr><Saa:IsSuccess>true</Saa:IsSuccess></Saa:MessageStatus></Saa:Header></Saa:DataPDU>
						// NAN SAA XML v2 sample :
						// <?xml version="1.0" encoding="UTF-8" ?><Saa:DataPDU xmlns:Saa="urn:swift:saa:xsd:saa.2.0" xmlns:Sw="urn:swift:snl:ns.Sw" xmlns:SwInt="urn:swift:snl:ns.SwInt" xmlns:SwGbl="urn:swift:snl:ns.SwGbl" xmlns:SwSec="urn:swift:snl:ns.SwSec"><Saa:Revision>2.0.8</Saa:Revision><Saa:Header><Saa:MessageStatus><Saa:SenderReference>IMGTCBEBEECL299CANCELLATION1$2301102</Saa:SenderReference><Saa:SeqNr>000062</Saa:SeqNr><Saa:IsSuccess>false</Saa:IsSuccess><Saa:ErrorCode>EFORMAT</Saa:ErrorCode><Saa:ErrorText>Message text format error</Saa:ErrorText></Saa:MessageStatus></Saa:Header></Saa:DataPDU>

						// Get message content
						byte[] contentBytes;
						String messageContent = null;
						try {
							contentBytes = new byte[message.getMessageLength()];
							message.readFully(contentBytes);
							messageContent = new String(contentBytes);
						} catch (IOException e) {
							e.printStackTrace();
						}
	
						String messageReference = null;
						if(messageContent !=null && messageContent.indexOf("<Saa:SenderReference>") != -1) {
							messageReference = messageContent.substring(messageContent.indexOf("<Saa:SenderReference>") + 21,messageContent.indexOf("</Saa:SenderReference>"));
						} else {
							messageReference = "Unknown";
						}
	
						// Feedback
						if(message.messageType == 4) {
							// PAN
							if(message.feedback == 275) {
								System.out.println("PAN Received for message reference = " + messageReference);
							} else {
								if(message.feedback == 65537) {
									System.out.println("NAN Received for message reference = " + messageReference);	
								}
							}

						} else {
							// Message
							if(message.messageType == 8 && messageContent != null) {
								System.out.println("SWIFT message received = " + messageContent);
							}
						}

					}
				}
			}

			// Close all queues
			try {
				for(int i=0; i < queue_connexions.size(); i++) {
					queue_connexions.get(i).close();
				}
			} catch (MQException e) {
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
	}


    public static String[] removeDuplicates(String[] array) {
        Set<String> set = new HashSet<>();
        for (String s : array) {
            if (s != null) {
                set.add(s);
            }
        }
        return set.toArray(new String[0]);
    }

}