package com.att.gfp.data.ipagPreprocess.externdata;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OIDToEname", propOrder = {

})
public class OIDToEname {

	@XmlElement(required = true) 
	private  String oid;
	@XmlElement(required = true)
	private String Ename;
	@XmlElement(required = true)
	private String sendToRuby; 
	@XmlElement(required = true)
	private String purgeInterval; 

	public final String getPurgeInterval() { 
		return purgeInterval;
	}

	public final void setPurgeInterval(String purgeInterval) {
		this.purgeInterval = purgeInterval;
	}

	public final String getSendToRuby() {
		return sendToRuby;
	}

	public final void setSendToRuby(String sendToRuby) {
		this.sendToRuby = sendToRuby;
	}

	/**
	 * @return the oid 
	 */
	public final String getOID() {
		return oid;
	}

	/**
	 * @param OID
	 *            the oid to set
	 */
	public final void setOID(String OID) {
		this.oid = OID;
	}

	/**
	 * @return the event name
	 */  
	public final String getEname() {
		return Ename;
	}

	/**
	 * @param ename
	 *            the ename to set
	 */
	public final void setEname(String ename) {
		this.Ename = ename;
	}
}
