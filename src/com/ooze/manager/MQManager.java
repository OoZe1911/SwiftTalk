package com.ooze.manager;

import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.msg.client.wmq.common.CommonConstants;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.QueueSender;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;

public class MQManager {

	public MQQueueConnectionFactory connectionFactory = null;
	public QueueConnection queueConnection = null;
	public QueueSession queueSession = null;
	public QueueSender queueSender = null;

	public MQManager(String hostname, String qmgrname, int port, String channel) {
		super();
		try {
			connectionFactory = new MQQueueConnectionFactory();
			connectionFactory.setHostName(hostname);
			connectionFactory.setPort(port);
			connectionFactory.setQueueManager(qmgrname);
			connectionFactory.setChannel(channel);
			connectionFactory.setIntProperty(CommonConstants.WMQ_CONNECTION_MODE, CommonConstants.WMQ_CM_CLIENT);
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
			queueSender.setDisableMessageID(true);
			queueSender.setDisableMessageTimestamp(true);
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	public void mqPut(String content) {
		try {
			// -> To avoid RFH2 header, configure the queue in QMGR like this :
			// ALTER QL(QUEUE NAME) PROPCTL(NONE)
			TextMessage message = queueSession.createTextMessage();
			//message.setStringProperty("JMS_IBM_Format", "MQSTR");
			//message.setIntProperty("JMS_IBM_PutApplType", 7);
			message.setText(content);			
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
