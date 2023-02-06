package com.ooze.manager;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.msg.client.jms.internal.JmsPropertyContextImpl;
import com.ibm.msg.client.wmq.common.CommonConstants;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.QueueSender;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;

public class MQManager {

	public QueueConnectionFactory connectionFactory = null;
	public QueueConnection queueConnection = null;
	public QueueSession queueSession = null;
	public QueueSender queueSender = null;

	public MQManager(String hostname, String qmgrname, int port, String channel) {
		super();
		try {
			connectionFactory = new MQQueueConnectionFactory();
			((MQConnectionFactory) connectionFactory).setHostName(hostname);
			((MQConnectionFactory) connectionFactory).setPort(port);
			((MQConnectionFactory) connectionFactory).setQueueManager(qmgrname);
			((MQConnectionFactory) connectionFactory).setChannel(channel);
			((JmsPropertyContextImpl) connectionFactory).setIntProperty(CommonConstants.WMQ_CONNECTION_MODE, CommonConstants.WMQ_CM_CLIENT);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	public void initConnction(String queueName) {
		 try {
			queueConnection = connectionFactory.createQueueConnection();
			queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			Queue queue = queueSession.createQueue(queueName);
			queueSender = queueSession.createSender(queue);
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	public void mqPut(String content) {
		try {
			// -> To avoid RFH2 header, configure the queue in QMGR like this :
			// ALTER QL(QUEUE NAME) PROPCTL(NONE)
			TextMessage message = queueSession.createTextMessage(content);
			// To avoid RFH2 header
			message.setIntProperty(CommonConstants.WMQ_MESSAGE_BODY, CommonConstants.WMQ_MESSAGE_BODY_MQ);
			// To request PAN & NAN
			message.setIntProperty(CommonConstants.JMS_IBM_REPORT_PAN, 1);
			message.setIntProperty(CommonConstants.JMS_IBM_REPORT_NAN, 2);
			message.setStringProperty(CommonConstants.JMS_IBM_MQMD_REPLYTOQMGR, "QMFAB");
			message.setStringProperty(CommonConstants.JMS_IBM_MQMD_REPLYTOQ, "QL.FAB");
			queueSender.send(message);
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
	
	public void closeConnection() {
		try {
			queueSender.close();
			queueSession.close();
			queueConnection.close();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
}
