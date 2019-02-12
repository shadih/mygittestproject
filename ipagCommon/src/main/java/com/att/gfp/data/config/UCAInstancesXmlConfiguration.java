package com.att.gfp.data.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.config.IpagPolicies.CpeCdcAlarms.CpeCdcAlarm;
import com.att.gfp.data.config.UCAInstnaces.UCAInstance;
import com.hp.uca.common.properties.exception.ConfigurationFileException;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.common.xml.XmlConfiguration; 

public final class UCAInstancesXmlConfiguration extends XmlConfiguration { 

	public static final String RESOURCE_UCA_INSTANCES_CONFIG_XML = "UCAInstancesConfig.xml"; 
 
	private Map<String, UCAInstance> ucaInstanceMap = Collections
	.synchronizedMap(new HashMap<String, UCAInstance>());
	private static final Logger LOG = LoggerFactory.getLogger(UCAInstancesXmlConfiguration.class); 
  
	public UCAInstancesXmlConfiguration() {     
 
		super(); 
		LogHelper.enter(LOG, "UCAInstancesXmlConfiguration()");
		setObjectClass(UCAInstnaces.class); 
		try { 
			initialize(RESOURCE_UCA_INSTANCES_CONFIG_XML);  
			refreshFromFile();    
			for (UCAInstance ucaInstnace : getUCAInstnaces().getUCAInstance()) {
				ucaInstanceMap.put(
						ucaInstnace.getInstancename(),
						ucaInstnace);  
			} 
			
		} catch (ConfigurationFileException e) { 
			LogHelper.logErrorDebug(LOG, "Invalid Configuration File", e); 
		} 
		LogHelper.exit(LOG, "UCAInstancesXmlConfiguration()");  
	}  
   

	public UCAInstnaces getUCAInstnaces() { 

		return (UCAInstnaces) getTheObject(); 

	} 


	public void setUCAInstnaces (UCAInstnaces ucaInstnaces) {  

		setTheObject(ucaInstnaces); 

	}


	public Map<String, UCAInstance> getUcaInstanceMap() {
		return ucaInstanceMap;
	}


	public void setUcaInstanceMap(Map<String, UCAInstance> ucaInstanceMap) {
		this.ucaInstanceMap = ucaInstanceMap;
	} 

} 
