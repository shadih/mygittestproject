package com.att.gfp.data.ipagPreprocess.externdata;

import java.util.Map;

import com.hp.uca.common.properties.exception.ConfigurationFileException;

public interface TunablePropertiesMXBean {

	/**
	 * @throws ConfigurationFileException
	 */
	void refreshFromFile() throws ConfigurationFileException;

	/**
	 * @throws ConfigurationFileException
	 */
	void saveToFile() throws ConfigurationFileException;

	/**
	 * @return
	 */
	Map<String, OIDToEname> getHashOIDToEname();

	/**
	 * @return
	 */ 
	String getLastFileUpdate();	
}
