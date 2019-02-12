package com.att.gfp.data.ipagPreprocess.preprocess;

public class CDCAlarm extends EnrichedPreprocessAlarm {
	
	private String operation;
	private String subscriptionId;
	private String subscriptionType;
	private String fromAppId;  
	private boolean existsInToplogy;
	public boolean isExistsInToplogy() {
		return existsInToplogy;
	}

	public void setExistsInToplogy(boolean existsInToplogy) {
		this.existsInToplogy = existsInToplogy;  
	}

	public String getOperation() {
		return operation;  
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public String getSubscriptionId() {
		return subscriptionId;
	}

	public void setSubscriptionId(String subscriptionId) {
		this.subscriptionId = subscriptionId;
	}

	public String getSubscriptionType() {
		return subscriptionType;
	}

	public void setSubscriptionType(String subscriptionType) {
		this.subscriptionType = subscriptionType;
	}

	public String getFromAppId() {
		return fromAppId;
	}

	public void setFromAppId(String fromAppId) {
		this.fromAppId = fromAppId;
	}

	public String getPubEventType() {
		return pubEventType;
	}

	public void setPubEventType(String pubEventType) {
		this.pubEventType = pubEventType;
	}

	public String getInitialize() {
		return initialize;
	}

	public void setInitialize(String initialize) {
		this.initialize = initialize;
	}

	public String getInitializeTimeStamp() {
		return initializeTimeStamp;
	}

	public void setInitializeTimeStamp(String initializeTimeStamp) {
		this.initializeTimeStamp = initializeTimeStamp;
	}

	private String pubEventType;
	private String initialize;
	private String initializeTimeStamp;
	
	public CDCAlarm(EnrichedPreprocessAlarm alarm) {
		super(alarm);
	}  


}
