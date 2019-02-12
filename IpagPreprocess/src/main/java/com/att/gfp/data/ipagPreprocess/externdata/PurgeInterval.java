package com.att.gfp.data.ipagPreprocess.externdata;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "purgeInterval", propOrder = {

})
public class PurgeInterval {

	@XmlElement(required = true)
	private  String clearPurgeInterval;
	@XmlElement(required = true)
	private  String module;
	
	public String getModule() {
		return module;
	}
	public void setModule(String module) {
		this.module = module;
	}
	public String getClearPurgeInterval() {
		return clearPurgeInterval; 
	}
	public void setClearPurgeInterval(String clearPurgeInterval) {
		this.clearPurgeInterval = clearPurgeInterval;
	}
	public String getActivePurgeInterval() {
		return activePurgeInterval;
	}
	public void setActivePurgeInterval(String activePurgeInterval) {
		this.activePurgeInterval = activePurgeInterval;
	}
	@XmlElement(required = true)
	private String activePurgeInterval;

}
