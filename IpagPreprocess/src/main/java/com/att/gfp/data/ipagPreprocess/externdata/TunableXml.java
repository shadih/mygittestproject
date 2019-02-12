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
    "oidToEname"
})
@XmlRootElement(name = "oidMap")
public class TunableXml {

    @XmlElement(required = true)
    private List<OIDToEname> oidToEname;

	/**
	 * @return the oid to event name
	 */
	public final List<OIDToEname> getOIDToEname() {
        if (oidToEname == null) {
        	oidToEname = new ArrayList<OIDToEname>();
        }
        return this.oidToEname;
	}


	
	
	
}
