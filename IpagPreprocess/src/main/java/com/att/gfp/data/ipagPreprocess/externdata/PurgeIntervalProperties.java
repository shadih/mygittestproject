package com.att.gfp.data.ipagPreprocess.externdata;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.util.service_util;
import com.hp.uca.common.misc.DateTimeUtil;
import com.hp.uca.common.properties.exception.ConfigurationFileException;
import com.hp.uca.common.xml.XmlConfiguration;
import com.hp.uca.expert.jmx.JMXManager;
import com.hp.uca.expert.scenario.Scenario;


public class PurgeIntervalProperties extends XmlConfiguration implements 
PurgeIntervalPropertiesMXBean {

	private static final String PURGEINTERVAL_JMX_NAME = "PurgeInterval";

	private static final String UCA_EXPERT_APPLICATION = "uca_ebc";

	/** 
	 * 
	 */
	public static final String PURGEINTERVAL_CONFIG_XML = "PreprocessConfig.xml";

	private static final Logger log = LoggerFactory.getLogger(PurgeIntervalProperties.class);

	/**
	 * JMX Helper, manages information exported to the JMX interface.
	 */
	private JMXManager jmxManager;

	private File purgeIntervalFile;

	private Scenario scenario;

	private PurgeIntervalXml purgeIntervalXml;

	private long lastFileUpdate = 0L;

	private String configurationFileName = PURGEINTERVAL_CONFIG_XML;

	private Map<String, PurgeInterval> hashPurgeInterval = Collections
			.synchronizedMap(new HashMap<String, PurgeInterval>());

	/**
	 * 
	 */
	public PurgeIntervalProperties() {
		super();

		setObjectClass(com.att.gfp.data.ipagPreprocess.externdata.PurgeIntervalXml.class);

	}


	/**
	 * 
	 */
	@PostConstruct
	public void init() {
		log.info("Enter: PurgeIntervalProperties  init()");
		
		purgeIntervalFile = service_util.getfileFromResourceName(PURGEINTERVAL_CONFIG_XML);

		if (purgeIntervalFile != null && purgeIntervalFile.exists()) {
			lastFileUpdate = purgeIntervalFile.lastModified();
		}

		try {
			initialize(configurationFileName);
			refreshFromFile();
		} catch (ConfigurationFileException e) {
			log.error(
					"Unable to retrieve event names from file : " + e);

		}

		if(purgeIntervalFile.exists()) {
			log.info("file to open:  " + purgeIntervalFile.getAbsolutePath());
		} else {
			log.info("file doesn't exist:  " + purgeIntervalFile.getAbsolutePath());
		}
		
		log.trace("Exit: init()");
		
	}

	/**
	 * @param scenario
	 */
	public void registerJmx(Scenario scenario) {
		this.scenario = scenario;
		jmxManager.register(UCA_EXPERT_APPLICATION, scenario.getValuePack()
				.getNameVersion(), null, PURGEINTERVAL_JMX_NAME, this);
	}

	/**
	 * 
	 */
	@PreDestroy
	public void unregisterJmx() {
		try {
			jmxManager
					.unregister(UCA_EXPERT_APPLICATION, scenario.getValuePack()
							.getNameVersion(), null, PURGEINTERVAL_JMX_NAME);
		} catch (Exception e) {

		}
	}

	@Override
	public void refreshFromFile() throws ConfigurationFileException {

		synchronized (getHashPurgeInterval()) {

			setPurgeIntervalXml((PurgeIntervalXml) unmarshal());

			rebuildHashTables();
		}

			log.trace("refreshFromFile()");
 
	}

	@Override
	public void saveToFile() throws ConfigurationFileException {
		 log.trace("Enter: saveToFile()");
		
		marshal(getPurgeIntervalXml());

		log.trace("Exit: saveToFile()");
	}

	protected void rebuildHashTables() {
		log.trace("Enter: PurgeInterval rebuildHashTables()");

		getHashPurgeInterval().clear(); 

		if (getPurgeIntervalXml() != null) {

			for (PurgeInterval purgeInterval : getPurgeIntervalXml().getPurgeInterval()) {
				getHashPurgeInterval().put(
						purgeInterval.getModule(),
						purgeInterval); 
 
				log.trace("Purge interval has reocords = " + purgeInterval.getModule());
				log.trace("Purge interval has reocords = " + purgeInterval.getActivePurgeInterval());
				log.trace("Purge interval has reocords = " + purgeInterval.getClearPurgeInterval());
			}  
		}
		log.trace("Exit: rebuildHashTables()");
		
	}

	/**
	 * 
	 */
	public void reloadFileIfUpdated() {

		if (purgeIntervalFile != null && purgeIntervalFile.exists()
				&& lastFileUpdate < purgeIntervalFile.lastModified()) {
			lastFileUpdate = purgeIntervalFile.lastModified();
			try {
				log.info(String.format("File [%s] has changed... reloading it",
						purgeIntervalFile.getAbsolutePath()));
				refreshFromFile();
			} catch (ConfigurationFileException e) {
				log.trace("Unable to retrieve Enrichment from file : " + e);

			}
		}
	}

	/**
	 * @return the tunableXml
	 */
	public PurgeIntervalXml getPurgeIntervalXml() {
		return purgeIntervalXml;
	}

	/**
	 * @param enrichmentXml
	 *            the enrichmentXml to set
	 */
	public void setPurgeIntervalXml(PurgeIntervalXml purgeIntervalXml) {
		this.purgeIntervalXml = purgeIntervalXml;
	}

	/**
	 * @return the hashManagedObjectToSite
	 */
	public Map<String, PurgeInterval> getHashPurgeInterval() {
		return hashPurgeInterval;
	}

	/**
	 * @return the configurationFileName
	 */
	public String getConfigurationFileName() {
		return configurationFileName;
	}

	/**
	 * @param configurationFileName
	 *            the configurationFileName to set
	 */
	public void setConfigurationFileName(String configurationFileName) {
		this.configurationFileName = configurationFileName;
	}

	/**
	 * @return the jmxManager
	 */
	public JMXManager getJmxManager() {
		return jmxManager;
	}

	/**
	 * @param jmxManager
	 *            the jmxManager to set
	 */
	public void setJmxManager(JMXManager jmxManager) {
		this.jmxManager = jmxManager;
	}

	/**
	 * @return the tunableFile
	 */
	public File getPurgeIntervalFile() {
		return purgeIntervalFile;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acme.enrichment.EnrichmentPropertiesMXBean#getLastFileUpdate()
	 */
	@Override
	public String getLastFileUpdate() {
		return DateTimeUtil.toString(lastFileUpdate);
	}

	
}
