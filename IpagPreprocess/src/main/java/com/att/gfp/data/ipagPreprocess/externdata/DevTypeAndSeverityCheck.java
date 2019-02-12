package com.att.gfp.data.ipagPreprocess.externdata;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DevTypeAndSeverityCheck", propOrder = {

})
public class DevTypeAndSeverityCheck {

	@XmlElement(required = true)
	private  String devSev;
	@XmlElement(required = true)
	private  String suppress;

	/**
	 * @return the suppression
	 */
	public final String getSuppress() {
		return suppress;
	}

	/**
	 * @param suppress
	 *            the suppress to set
	 */
	public final void setSuppression(String suppress) {
		this.suppress = suppress;
	}
	
	/**
	 * @return the device and severity
	 */
	public final String getDevSev() {
		return devSev;
	}

	/**
	 * @param device and severity
	 *            the device and severity to set
	 */
	public final void setDevSev(String devSev) {
		this.devSev = devSev;
	}
}
