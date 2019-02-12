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
    "purgeInterval"
})
@XmlRootElement(name = "purgeIntervalConfig")
public class PurgeIntervalXml {

    @XmlElement(required = true)
    private List<PurgeInterval> purgeInterval;

	/**
	 * @return the purgeInterval
	 */
	public final List<PurgeInterval> getPurgeInterval() {
        if (purgeInterval == null) {
        	purgeInterval = new ArrayList<PurgeInterval>();
        }
        return this.purgeInterval; 
	}

	
}
