package com.att.gfp.data.ipagAlarm;

import javax.xml.bind.annotation.XmlRootElement;

import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.alarm.AlarmHelper;

/**
 * Sub class for EnrichedAlarm class, that has attributes required for adtran adtran-pport-alarm() processing 
 * 
 * @author st133d   
 *
 */

@XmlRootElement  
public class EnrichedAdtranLinkDownAlarm extends EnrichedAdtranAlarm {

	/**
	 * EnrichedAdtranLinkDownAlarm
	 */
	private static final String F_UNINNITYPE = "uninniType";
	private static final String F_CLCI = "clci";
	private static final String F_IPADDRESS = "ipAddress";
	private static final String F_SLOTNAME = "slotName";
	private static final String F_SLOT = "slot";
	private static final String F_PORT = "port";
	private static final String F_CLFI = "clfi";
	
	
	private String uninniType;
	private String clci;
	private String ipAddress;
	private String clfi;
	public String getClfi() {
		return clfi;
	}

	public void setClfi(String clfi) {
		this.clfi = clfi;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getClci() {
		return clci;
	}

	public void setClci(String clci) {
		this.clci = clci;
	}

	public String getUninniType() {
		return uninniType;
	}

	public void setUninniType(String uninniType) {
		this.uninniType = uninniType;
	}


	private String slotName;
	public String getSlotName() {
		return slotName;
	}

	public void setSlotName(String slotName) {
		this.slotName = slotName;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getSlot() {
		return slot;
	}

	public void setSlot(String slot) {
		this.slot = slot;
	}


	private String port;
	private String slot;
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5235137849600943223L;
	
	/**
	 * EnrichedAlarm Constructor
	 */
	public EnrichedAdtranLinkDownAlarm() {
		super();
	}
	/**
	 * EnrichedAlarm Copy Constructor.
	 * 
	 * This constructor constructs an Extended Alarm and uses the Alarm provided as
	 * parameter to set all the standard alarm fields
	 * @throws Exception 
	 */
	public EnrichedAdtranLinkDownAlarm(Alarm alarm){
		super(alarm);
		
		uninniType = null;
		clci = null;  
		ipAddress = null; 
		clfi = null;
		port = null;
		slot = null;
		slotName = null; 
	} 
	
	/**
	 * EnrichedAlarm Copy Constructor.
	 * 
	 * This constructor constructs an Extended Alarm and uses the Alarm provided as
	 * parameter to set all the standard alarm fields
	 * @throws Exception 
	 */
	public EnrichedAdtranLinkDownAlarm(EnrichedAdtranAlarm alarm){
		super(alarm);
		
		uninniType = null;
		clci = null;
		ipAddress = null; 
		clfi = null;
		port = null;
		slot = null;
		slotName = null; 
	} 
	
	/**
	 * EnrichedAlarm Copy Constructor.
	 * 
	 * This constructor constructs an Extended Alarm and uses the Alarm provided as
	 * parameter to set all the standard alarm fields
	 * @throws Exception 
	 */
	public EnrichedAdtranLinkDownAlarm(EnrichedAlarm alarm){
		super(alarm);
		
		uninniType = null;
		clci = null;  
		ipAddress = null; 
		clfi = null;
		port = null;
		slot = null;
		slotName = null; 
	} 
	
	/**
	 * Clones the provided Extended Alarm object
	 * @return a copy of the current object
	 * @throws CloneNotSupportedException
	 */
	@Override
	public EnrichedAdtranLinkDownAlarm clone() throws CloneNotSupportedException {
		EnrichedAdtranLinkDownAlarm newAlarm = (EnrichedAdtranLinkDownAlarm) super.clone();
		newAlarm.slot = this.slot;
		newAlarm.slotName = this.slotName;
		newAlarm.port = this.port;
		newAlarm.ipAddress = this.ipAddress;
		newAlarm.clci = this.clci;
		newAlarm.uninniType = this.uninniType;
		newAlarm.clfi = this.clfi;
		return newAlarm;
	} 

	

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.uca.expert.alarm.AlarmCommon#toFormattedString()
	 */
	@Override
	public String toFormattedString() {
		StringBuffer toStringBuffer= AlarmHelper.toFormattedStringBuffer(this);
		
		AlarmHelper.addFormatedItem(toStringBuffer, F_UNINNITYPE, getUninniType());
		AlarmHelper.addFormatedItem(toStringBuffer, F_CLCI, getClci());
		AlarmHelper.addFormatedItem(toStringBuffer, F_IPADDRESS, getIpAddress());
		AlarmHelper.addFormatedItem(toStringBuffer, F_SLOT, getSlot());
		AlarmHelper.addFormatedItem(toStringBuffer, F_SLOTNAME, getSlotName());
		AlarmHelper.addFormatedItem(toStringBuffer, F_PORT, getPort());
		AlarmHelper.addFormatedItem(toStringBuffer, F_CLFI, getClfi()); 
		 
		return toStringBuffer.toString();
	}

}
