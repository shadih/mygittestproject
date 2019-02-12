package com.att.gfp.helper;

/**
 * enum of device types
 * @author st133d
 *
 */
public enum DeviceTypes {
	ADTRAN5000SERIES ("ADTRAN 5000 SERIES"), 
	JUNIPERMXSERIES ("JUNIPER MX SERIES");
	
	private final String deviceType;
	public String getDeviceType() {
		return deviceType;
	}

	private DeviceTypes(String deviceType) {
		this.deviceType = deviceType;
	}
	
} 
 
