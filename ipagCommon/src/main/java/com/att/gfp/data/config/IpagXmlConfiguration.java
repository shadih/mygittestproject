package com.att.gfp.data.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.config.IpagPolicies.CpeCdcAlarms.CpeCdcAlarm;
import com.att.gfp.data.config.IpagPolicies.Suppressions.Suppression;
import com.hp.uca.common.properties.exception.ConfigurationFileException;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.common.xml.XmlConfiguration; 

public final class IpagXmlConfiguration extends XmlConfiguration { 

	public static final String RESOURCE_IPAG_CONFIG_XML = "IPAGConfig.xml"; 

	private Map<String, CpeCdcAlarm> cpeCdeAlarmMap = Collections
	.synchronizedMap(new HashMap<String, CpeCdcAlarm>());
	private Map<String, Suppression> suppresionMap = Collections
	.synchronizedMap(new HashMap<String, Suppression>()); 
	private static final Logger LOG = LoggerFactory.getLogger(IpagXmlConfiguration.class); 
  
	public IpagXmlConfiguration() {     
 
		super(); 
		LogHelper.enter(LOG, "IpagXmlConfiguration()");
		setObjectClass(IpagPolicies.class); 
		try { 
			initialize(RESOURCE_IPAG_CONFIG_XML); 
			refreshFromFile();    
			for (CpeCdcAlarm cpeCdcAlarm : getIpagPolicies().getCpeCdcAlarms().getCpeCdcAlarm()) {
				cpeCdeAlarmMap.put(
						cpeCdcAlarm.getEventName(),
						cpeCdcAlarm);  

			}
			for (Suppression suppression : getIpagPolicies().getSuppressions().getSuppression()) {
				suppresionMap.put(
						suppression.getKey(),
						suppression);   
 
			} 
			
			
		} catch (ConfigurationFileException e) { 
			LogHelper.logErrorDebug(LOG, "Invalid Configuration File", e); 
		} 
		LogHelper.exit(LOG, "IpagXmlConfiguration()");  
	}  
   
	public Map<String, Suppression> getSuppresionMap() {
		return suppresionMap;
	}

	public void setSuppresionMap(Map<String, Suppression> suppresionMap) {
		this.suppresionMap = suppresionMap;
	}

	public Map<String, CpeCdcAlarm> getCpeCdeAlarmMap() {
		return cpeCdeAlarmMap;
	}

	public void setCpeCdeAlarmMap(Map<String, CpeCdcAlarm> cpeCdeAlarmMap) {
		this.cpeCdeAlarmMap = cpeCdeAlarmMap;
	}

	public IpagPolicies getIpagPolicies() { 

		return (IpagPolicies) getTheObject(); 

	} 


	public void setIpagPolicies (IpagPolicies ipagPolicies) { 

		setTheObject(ipagPolicies); 

	} 

} 
