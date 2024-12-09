package com.ooze.swifttalk;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQQueue;
import com.ooze.bean.ConnectionParams;
import com.ooze.manager.MQManager;

public class FromSwift extends Thread {
	private ConnectionParams connectionParams;
	public static String FOLDER_FROM_SWIFT = null;
	private static final Pattern MT_MUR = Pattern.compile("\\{3:.*\\{108:(.+?)\\}");
	private static final Pattern MT_TRN = Pattern.compile("(?::20:(.*)|:20C:.*//(.*)|:20D:.*//(.*))");

	public FromSwift(ConnectionParams connectionParams) {
		super();
		this.connectionParams = connectionParams;
	}

	public void run() {
		getMessagesFromSAA();
	}

	public void getMessagesFromSAA() {
		// Compute queues to acquire
		ArrayList<String> queue_list_with_potential_duplicates = connectionParams.getQueueFromSwift();
		queue_list_with_potential_duplicates.add(connectionParams.getReplyToQueue());
		queue_list_with_potential_duplicates.add( connectionParams.getQueueAckSwift());
		String[] queue_list_as_a_string_table = queue_list_with_potential_duplicates.toArray(new String[0]);
		String[] queue_list = removeDuplicates(queue_list_as_a_string_table);

        // Set to avoid duplicates
		// Scanning queues
		while(!SwiftTalk.exit) {
			// Try to connect to MQ
			MQManager queueManager = new MQManager(connectionParams.getQmgrHost(), connectionParams.getQmgrName(), connectionParams.getQmgrPort(), connectionParams.getChannel(), connectionParams.getCypher(), connectionParams.getSslPeer());

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
						byte[] contentBytes = null;
						String messageContent = null;
						try {
							contentBytes = new byte[message.getMessageLength()];
							message.readFully(contentBytes);
							messageContent = new String(contentBytes);
						} catch (IOException e) {
							e.printStackTrace();
						}

						String messageReference = null;
						String fileExtension = ".xml";
						if(messageContent !=null && messageContent.indexOf("<Saa:SenderReference>") != -1) {
							messageReference = messageContent.substring(messageContent.indexOf("<Saa:SenderReference>") + 21,messageContent.indexOf("</Saa:SenderReference>"));
						} else {
							if(messageContent.indexOf("{1:") != -1) {
								fileExtension = ".fin";
								Matcher matcher = MT_MUR.matcher(messageContent);
								String reference = extract(matcher);

								if (reference == null || reference.isEmpty()) {
								    matcher = MT_TRN.matcher(messageContent);
								    reference = extract(matcher);
								}

								messageReference = (reference == null || reference.isEmpty()) ? "Unknown" : reference;
							} else {
								messageReference = "Unknown";
							}
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
								// Write received message to disk
								if(contentBytes.length > 0) {
									String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmssSSS").format(new Date());
									String fileName = FOLDER_FROM_SWIFT + "/" + timestamp + "-" + messageReference + fileExtension;
									try (FileOutputStream fileOutputStream = new FileOutputStream(fileName)) {
										fileOutputStream.write(contentBytes);
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
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
				Thread.sleep(1000 * connectionParams.getSleepingDuration());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}


	public static String[] removeDuplicates(String[] array) {
		Set<String> set = new HashSet<>();
		for (String s : array) {
			if (s != null && s.length() > 0) {
				set.add(s);
			}
		}
		return set.toArray(new String[0]);
	}

	// Extract regex from string
	public static String extract(Matcher matcher) {
		String result = "";
		while (matcher.find()) {
			for(int j=1;j<=matcher.groupCount();j++) {
				if(matcher.group(j)!=null)
					result = result + matcher.group(j);
			}
		}
		return result;
	}

}