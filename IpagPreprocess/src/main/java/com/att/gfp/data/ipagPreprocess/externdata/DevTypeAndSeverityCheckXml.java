package com.att.gfp.data.ipagPreprocess.externdata;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "devTypeAndSeverityCheck"
})
@XmlRootElement(name = "devTypeAndSeveritySuppress")
public class DevTypeAndSeverityCheckXml {

    @XmlElement(required = true)
    private List<DevTypeAndSeverityCheck> devTypeAndSeverityCheck;

	/**
	 * @return the device type and severity check
	 */
	public final List<DevTypeAndSeverityCheck> getDevTypeAndSeverityCheck() {
        if (devTypeAndSeverityCheck == null) {
        	devTypeAndSeverityCheck = new ArrayList<DevTypeAndSeverityCheck>();
        }
        return this.devTypeAndSeverityCheck;
	}


	
	
	
}
