package com.ooze.bean;

import java.util.ArrayList;

public class ConnectionParams {
	public String qmgrHost = null;
	public String qmgrName=null;
	public int qmgrPort=1414;
	public String channel=null;
	public String cipher=null;
	public String trustStore=null;
	public String trustStorePassword=null;
	public String keyStore=null;
	public String keyStorePassword=null;
	public String sslPeer=null;
	public String replyToQueue=null;
	public String queueAckSwift=null;
	public ArrayList<String> queueFromSwift=null;
	public String queueToSwift=null;
	public String lauType=null;
	public String lauKey=null;
	public int sleepingDuration=10;

	public String getQmgrHost() {
		return qmgrHost;
	}
	public void setQmgrHost(String qmgrHost) {
		this.qmgrHost = qmgrHost;
	}
	public String getQmgrName() {
		return qmgrName;
	}
	public void setQmgrName(String qmgrName) {
		this.qmgrName = qmgrName;
	}
	public int getQmgrPort() {
		return qmgrPort;
	}
	public void setQmgrPort(int qmgrPort) {
		this.qmgrPort = qmgrPort;
	}
	public String getChannel() {
		return channel;
	}
	public void setChannel(String channel) {
		this.channel = channel;
	}
	public String getCipher() {
		return cipher;
	}
	public void setCipher(String cipher) {
		this.cipher = cipher;
	}
	public String getTrustStore() {
		return trustStore;
	}
	public void setTrustStore(String trustStore) {
		this.trustStore = trustStore;
	}
	public String getTrustStorePassword() {
		return trustStorePassword;
	}
	public void setTrustStorePassword(String trustStorePassword) {
		this.trustStorePassword = trustStorePassword;
	}
	public String getKeyStore() {
		return keyStore;
	}
	public void setKeyStore(String keyStore) {
		this.keyStore = keyStore;
	}
	public String getKeyStorePassword() {
		return keyStorePassword;
	}
	public void setKeyStorePassword(String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
	}
	public String getSslPeer() {
		return sslPeer;
	}
	public void setSslPeer(String sslPeer) {
		this.sslPeer = sslPeer;
	}
	public String getReplyToQueue() {
		return replyToQueue;
	}
	public void setReplyToQueue(String replyToQueue) {
		this.replyToQueue = replyToQueue;
	}
	public String getQueueAckSwift() {
		return queueAckSwift;
	}
	public void setQueueAckSwift(String queueAckSwift) {
		this.queueAckSwift = queueAckSwift;
	}
	public ArrayList<String> getQueueFromSwift() {
		return queueFromSwift;
	}
	public void setQueueFromSwift(ArrayList<String> queueFromSwift) {
		this.queueFromSwift = queueFromSwift;
	}
	public String getQueueToSwift() {
		return queueToSwift;
	}
	public void setQueueToSwift(String queueToSwift) {
		this.queueToSwift = queueToSwift;
	}
	public String getLauType() {
		return lauType;
	}
	public void setLauType(String lauType) {
		this.lauType = lauType;
	}
	public String getLauKey() {
		return lauKey;
	}
	public void setLauKey(String lauKey) {
		this.lauKey = lauKey;
	}
	public int getSleepingDuration() {
		return sleepingDuration;
	}
	public void setSleepingDuration(int sleepingDuration) {
		this.sleepingDuration = sleepingDuration;
	}

}
