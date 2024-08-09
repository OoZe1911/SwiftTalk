package com.ooze.manager;

import java.util.Hashtable;

import com.ibm.mq.MQException;
import com.ibm.mq.MQGetMessageOptions;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQPutMessageOptions;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.MQConstants;
import com.ooze.bean.ConnectionParams;

public class MQManager {

	public MQQueueManager qMgr = null;

	public MQManager(String hostname, String qmgrname, int port, String channel, String cypher, String sslPeer) {
		super();
		Hashtable<String, Object> props = new Hashtable<String, Object>();
		props.put(MQConstants.HOST_NAME_PROPERTY, hostname);
		props.put(MQConstants.CHANNEL_PROPERTY, channel);
		props.put(MQConstants.PORT_PROPERTY, port);
		if(cypher != null && cypher.length() > 0)
			props.put(MQConstants.SSL_CIPHER_SUITE_PROPERTY, cypher);
		if(sslPeer!= null && sslPeer.length() > 0)
			props.put(MQConstants.SSL_PEER_NAME_PROPERTY, sslPeer);
		try {
			qMgr = new MQQueueManager(qmgrname, props);
		} catch (MQException e) {
			e.printStackTrace();
		}
	}

	public MQQueue initConnctionToQueue(String queueName) {
		try {
			MQQueue queue = qMgr.accessQueue(queueName, MQConstants.MQOO_OUTPUT | MQConstants.MQOO_INPUT_AS_Q_DEF);
			return queue;
		} catch (MQException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void mqPut(MQQueue queue, String content, ConnectionParams connectionParams) {
		try {
			MQPutMessageOptions pmo = new MQPutMessageOptions();
			pmo.options = MQConstants.MQPMO_ASYNC_RESPONSE;
			MQMessage message = new MQMessage();
			message.report = MQConstants.MQRO_PAN + MQConstants.MQRO_NAN;
			message.replyToQueueManagerName = connectionParams.getQmgrName();
			message.replyToQueueName = connectionParams.getReplyToQueue();
			// CorrelationID
			String correlationIdValue = "CORRELATIONID";
			byte[] correlationIdBytes = correlationIdValue.getBytes("UTF-8");
			message.correlationId = correlationIdBytes;
			message.format = MQConstants.MQFMT_STRING;
			message.writeString(content);
			queue.put(message, pmo);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public MQMessage mqGet(MQQueue queue) {
		MQMessage message = new MQMessage();
		MQGetMessageOptions gmo = new MQGetMessageOptions();
		gmo.options = CMQC.MQGMO_WAIT + CMQC.MQGMO_FAIL_IF_QUIESCING;
		// Wait 200 ms
		gmo.waitInterval = 200;
		try {
			queue.get(message, gmo);
			/* DEBUG
			String messageContent = null;
			System.out.println("DEBUG : messageType = "+ message.messageType);
			System.out.println("DEBUG : feedback = "+ message.feedback);
			System.out.println("DEBUG : report = "+ message.report);
			System.out.println("DEBUG FAB : " + message.format + " - " + message.putApplicationName + " - " + message.userId);
			try {
				byte[] correlationId = message.correlationId;
				System.out.println("DEBUG : correlationId = "+ new String(correlationId));
				byte[] contentBytes = new byte[message.getMessageLength()];
				message.readFully(contentBytes);
				messageContent = new String(contentBytes);
				System.out.println("DEBUG : message = " + messageContent);

			} catch (IOException e) {
				e.printStackTrace();
			}
			 */
		} catch (MQException e) {
			/* DEBUG
			if ( (e.completionCode == CMQC.MQCC_FAILED) && (e.reasonCode == CMQC.MQRC_NO_MSG_AVAILABLE) ) {
				System.out.println("No message in queue " + queue.getResolvedQName());
				return null;
			} else  {
				System.out.println("MQException: " + e.getLocalizedMessage());
				System.out.println("CC=" + e.completionCode + " : RC=" + e.reasonCode);
				return null;
			}
			 */
			if ( (e.completionCode != CMQC.MQCC_FAILED) || (e.reasonCode != CMQC.MQRC_NO_MSG_AVAILABLE) ) {
				System.out.println("MQException: " + e.getLocalizedMessage());
				System.out.println("CC=" + e.completionCode + " : RC=" + e.reasonCode);
				return null;				
			} else {
				return null;
			}
		}

		return message;
	}

	public void closeConnection() {
		try {
			qMgr.disconnect();
		} catch (MQException e) {
			e.printStackTrace();
		}
	}
}
