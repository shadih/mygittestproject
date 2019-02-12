package com.att.gfp.data.config;



import org.slf4j.Logger; 

import org.slf4j.LoggerFactory; 


import com.hp.uca.common.properties.exception.ConfigurationFileException; 
import com.hp.uca.common.trace.LogHelper;

import com.hp.uca.common.xml.XmlConfiguration; 

public final class AdtranCorrelationConfiguration extends XmlConfiguration { 


	public static final String RESOURCE_ADTRAN_CORR_CONFIG_XML = "AdtranCorrelationXmlConfig.xml"; 
  
	private static final Logger LOG = LoggerFactory.getLogger(AdtranCorrelationConfiguration.class); 

//	PreProcessConfiguration theInstance=null;


	public AdtranCorrelationConfiguration() { 
 
		super(); 

		LogHelper.enter(LOG, "AdtranCorrelationConfiguration()");
   
		setObjectClass(AdtranCorrelationPolicies.class); 

		try { 
			initialize(RESOURCE_ADTRAN_CORR_CONFIG_XML); 
			refreshFromFile(); 
		} catch (ConfigurationFileException e) { 
			LogHelper.logErrorDebug(LOG, "Invalid Configuration File", e); 
		} 


		LogHelper.exit(LOG, "AdtranCorrelationConfiguration()");
	} 

/*	static public synchronize  PreProcessConfiguration getInstance() {

		if (theInstance==null) {
			theInstance = new PreProcessConfiguration();
		}
		return theInstance;
	}
	
	*/
	public AdtranCorrelationPolicies getAdtranCorrelationPolicies() { 

		return (AdtranCorrelationPolicies) getTheObject(); 

	} 


	public void setAdtranCorrelationPolicies (AdtranCorrelationPolicies adtranCorrelationPolicies) { 

		setTheObject(adtranCorrelationPolicies); 

	}  

} 
