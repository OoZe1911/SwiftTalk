package com.ooze.manager;

import java.util.Hashtable;

import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQPutMessageOptions;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.MQConstants;

public class MQManager {

	public MQQueueManager qMgr = null;
	public MQQueue queue = null;

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

	public void initConnction(String queueName) {
		try {
			queue = qMgr.accessQueue(queueName, MQConstants.MQOO_OUTPUT | MQConstants.MQOO_INPUT_AS_Q_DEF);
		} catch (MQException e) {
			e.printStackTrace();
		}
	}

	public void mqPut(String content) {
		try {
			MQPutMessageOptions pmo = new MQPutMessageOptions();
			pmo.options = MQConstants.MQPMO_ASYNC_RESPONSE;
			MQMessage message = new MQMessage();
			message.report = MQConstants.MQRO_PAN & MQConstants.MQRO_NAN;
			message.replyToQueueManagerName = "QMFAB";
			message.replyToQueueName = "QL.FAB";
			message.format = MQConstants.MQFMT_STRING;
			message.writeString(content);
			queue.put(message, pmo);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void closeConnection() {
		try {
			queue.close();
			qMgr.disconnect();
		} catch (MQException e) {
			e.printStackTrace();
		}
	}
}
