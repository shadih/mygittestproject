package com.att.gfp.data.config;

import org.slf4j.Logger; 

import org.slf4j.LoggerFactory; 


import com.hp.uca.common.properties.exception.ConfigurationFileException; 
import com.hp.uca.common.trace.LogHelper;

import com.hp.uca.common.xml.XmlConfiguration; 

public final class DecomposeRulesConfiguration extends XmlConfiguration { 

	public static final String RESOURCE_DECOMPOSE_CONFIG_XML = "DecomposeRulesConfig.xml"; 

	private static final Logger LOG = LoggerFactory.getLogger(DecomposeRulesConfiguration.class); 
  
	public DecomposeRulesConfiguration() {     

		super(); 
		LogHelper.enter(LOG, "DecomposeRulesConfiguration()");
		setObjectClass(DecompseConfig.class); 
		try { 
			initialize(RESOURCE_DECOMPOSE_CONFIG_XML); 
			refreshFromFile();    
		} catch (ConfigurationFileException e) { 
			LogHelper.logErrorDebug(LOG, "Invalid Configuration File", e); 
		} 
		LogHelper.exit(LOG, "DecomposeRulesConfiguration()");  
	}  

	public DecompseConfig getDecompseConfig() { 

		return (DecompseConfig) getTheObject(); 

	} 


	public void setDecomposeRules (DecompseConfig decompseConfig) { 

		setTheObject(decompseConfig); 

	} 

} 
