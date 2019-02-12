package com.att.gfp.data.preprocess.conf;



import org.slf4j.Logger; 

import org.slf4j.LoggerFactory; 


import com.hp.uca.common.properties.exception.ConfigurationFileException; 
import com.hp.uca.common.trace.LogHelper;

import com.hp.uca.common.xml.XmlConfiguration; 

public final class PreProcessConfiguration extends XmlConfiguration { 


	public static final String RESOURCE_PREPROCESS_CONFIG_XML = "PreProcessXmlConfig.xml"; 
  
	private static final Logger LOG = LoggerFactory.getLogger(PreProcessConfiguration.class); 

//	PreProcessConfiguration theInstance=null;


	public PreProcessConfiguration() { 

		super(); 

		LogHelper.enter(LOG, "PreProcessConfiguration()");
   
		setObjectClass(PreProcessPolicies.class); 

		try { 
			initialize(RESOURCE_PREPROCESS_CONFIG_XML); 
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
	public PreProcessPolicies getPreProcessPolicies() { 

		return (PreProcessPolicies) getTheObject(); 

	} 


	public void setProblemPolicies (PreProcessPolicies preProcessPolicies) { 

		setTheObject(preProcessPolicies); 

	}  

} 
