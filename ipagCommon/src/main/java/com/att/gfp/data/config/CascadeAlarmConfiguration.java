package com.att.gfp.data.config;



import org.slf4j.Logger; 

import org.slf4j.LoggerFactory; 


import com.hp.uca.common.properties.exception.ConfigurationFileException; 
import com.hp.uca.common.trace.LogHelper;

import com.hp.uca.common.xml.XmlConfiguration; 

public final class CascadeAlarmConfiguration extends XmlConfiguration { 

  
	public static final String RESOURCE_CASCADE_CONFIG_XML = "CascadeXmlConfig.xml"; 

	private static final Logger LOG = LoggerFactory.getLogger(CascadeAlarmConfiguration.class); 

//	PreProcessConfiguration theInstance=null;

 
	public CascadeAlarmConfiguration() { 

		super(); 

		LogHelper.enter(LOG, "CascadeAlarmConfiguration()");

		setObjectClass(CascadePolicies.class); 
  
		try { 
			initialize(RESOURCE_CASCADE_CONFIG_XML); 
			refreshFromFile(); 
		} catch (ConfigurationFileException e) { 
			LogHelper.logErrorDebug(LOG, "Invalid Configuration File", e); 
		} 


		LogHelper.exit(LOG, "PreProcessConfiguration()");
	} 

/*	static public synchronize  PreProcessConfiguration getInstance() {

		if (theInstance==null) {
			theInstance = new PreProcessConfiguration();
		}
		return theInstance;
	}
	
	*/
	public CascadePolicies getCascadePolicies() { 

		return (CascadePolicies) getTheObject(); 

	}     


	public void setProblemPolicies (CascadePolicies cascadePolicies) { 

		setTheObject(cascadePolicies); 
  
	}  

} 
