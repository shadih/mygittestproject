package com.att.gfp.helper;

/**
 * Enum of Alarm classification types.
 * 
 * @author st133d
 *
 */

public enum AlarmClassificationTypes {
	
	NFOMOBILITY("NFO-MOBILITY"),
	NFOTA5000("NFO-TA5000"),
	NFOEMUX("NFO-EMUX");
	
	private String classifcationType;
	public String getClassifcationType() {
		return classifcationType;
	}
	public void setClassifcationType(String classifcationType) {
		this.classifcationType = classifcationType;
	}
	private AlarmClassificationTypes(String classifcationType) {
		this.classifcationType = classifcationType;
	}
}
