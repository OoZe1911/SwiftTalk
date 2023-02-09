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
import com.ooze.swifttalk.ToSwift;

public class MQManager {

	public MQQueueManager qMgr = null;

	public MQManager(String hostname, String qmgrname, int port, String channel) {
		super();
		Hashtable<String, Object> props = new Hashtable<String, Object>();
		props.put(MQConstants.HOST_NAME_PROPERTY, hostname);
		props.put(MQConstants.CHANNEL_PROPERTY, channel);
		props.put(MQConstants.PORT_PROPERTY, port);
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

	public void mqPut(MQQueue queue, String content) {
		try {
			MQPutMessageOptions pmo = new MQPutMessageOptions();
			pmo.options = MQConstants.MQPMO_ASYNC_RESPONSE;
			MQMessage message = new MQMessage();
			//message.correlationId="TESTFAB".getBytes();
			message.report = MQConstants.MQRO_PAN & MQConstants.MQRO_NAN;
			message.replyToQueueManagerName = ToSwift.QMGRNAME;
			message.replyToQueueName = ToSwift.REPLY_TO_QUEUE;
			message.format = MQConstants.MQFMT_STRING;
			message.writeString(content);
			queue.put(message, pmo);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String mqGet(MQQueue queue) {
		MQMessage message = new MQMessage();
		MQGetMessageOptions gmo = new MQGetMessageOptions();
		gmo.options = CMQC.MQGMO_WAIT + CMQC.MQGMO_FAIL_IF_QUIESCING;
		// Wait 2 seconds
		gmo.waitInterval = 2000;
		try {
			queue.get(message, gmo);
			System.out.println("DEBUG : messageType = "+ message.messageType);
			System.out.println("DEBUG : feedback = "+ message.feedback);
			System.out.println("DEBUG : report = "+ message.report);
			System.out.println("DEBUG :  correlationId = "+ message.correlationId);
		} catch (MQException e) {
			if ( (e.completionCode == CMQC.MQCC_FAILED) && (e.reasonCode == CMQC.MQRC_NO_MSG_AVAILABLE) ) {
				System.out.println("No message in queue " + queue.getResolvedQName());
			} else  {
				System.out.println("MQException: " + e.getLocalizedMessage());
				System.out.println("CC=" + e.completionCode + " : RC=" + e.reasonCode);
			}
		}

		return null;
	}

	public void closeConnection() {
		try {
			qMgr.disconnect();
		} catch (MQException e) {
			e.printStackTrace();
		}
	}
}
