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


public class DevTypeAndSeveritySuppressionProperties extends XmlConfiguration implements
DevTypeAndSeveritySuppressionPropertiesMXBean {

	private static final String TUNABLE_JMX_NAME = "devTypeAndSeveritySuppress";

	private static final String UCA_EXPERT_APPLICATION = "uca_ebc";

	/**
	 * 
	 */
	public static final String DEVTYPESEVERITY_CONFIG_XML = "IPAGConfiguration.xml";

	private static final Logger log = LoggerFactory.getLogger(DevTypeAndSeveritySuppressionProperties.class);

	/**
	 * JMX Helper, manages information exported to the JMX interface.
	 */
	private JMXManager jmxManager;

	private File configFile;

	private Scenario scenario;

	private DevTypeAndSeverityCheckXml devTypeAndSeverityCheckXml;

	private long lastFileUpdate = 0L;

	private String configurationFileNames = DEVTYPESEVERITY_CONFIG_XML;

	private Map<String, String> hashDevTypeAndSeverityCheck = Collections
			.synchronizedMap(new HashMap<String, String>());

	/**
	 * 
	 */
	public DevTypeAndSeveritySuppressionProperties() {
		super();

		setObjectClass(com.att.gfp.data.ipagPreprocess.externdata.DevTypeAndSeverityCheckXml.class);

	}


	/**
	 * 
	 */
	@PostConstruct
	public void init() {
		log.info("Enter: DevTypeAndSeveritySuppressionProperties   init()");
		
		configFile = service_util.getfileFromResourceName(DEVTYPESEVERITY_CONFIG_XML);

		if (configFile != null && configFile.exists()) {
			log.info("file to open:  " + configFile.getAbsolutePath());
			lastFileUpdate = configFile.lastModified();
			
			try {
				initialize(configurationFileNames);
				refreshFromFile();
			} catch (ConfigurationFileException e) {
				log.error(
						"Unable to retrieve event names from file : " + e);

			}	
		} else {
			log.info("file doesn't exist:  " + configFile.getAbsolutePath());
		}
		
		log.trace("Exit: init()   File:" + configFile.getAbsolutePath());		
	}

	/**
	 * @param scenario
	 */
	public void registerJmx(Scenario scenario) {
		this.scenario = scenario;
		jmxManager.register(UCA_EXPERT_APPLICATION, scenario.getValuePack()
				.getNameVersion(), null, TUNABLE_JMX_NAME, this);
	}

	/**
	 * 
	 */
	@PreDestroy
	public void unregisterJmx() {
		try {
			jmxManager
					.unregister(UCA_EXPERT_APPLICATION, scenario.getValuePack()
							.getNameVersion(), null, TUNABLE_JMX_NAME);
		} catch (Exception e) {

		}
	}

	@Override
	public void refreshFromFile() throws ConfigurationFileException {

		synchronized (getHashDevTypeAndSeverityCheck()) {

			setDevTypeAndSeverityCheckXml((DevTypeAndSeverityCheckXml) unmarshal());

			rebuildHashTables();
		}

			log.trace("refreshFromFile()");

	}

	@Override
	public void saveToFile() throws ConfigurationFileException {
		 log.trace("Enter: saveToFile()");
		
		marshal(getDevTypeAndSeverityCheckXml());

		log.trace("Exit: saveToFile()");
	}

	protected void rebuildHashTables() {
		log.trace("Enter: rebuildHashTables()");

		getHashDevTypeAndSeverityCheck().clear();

		if (getDevTypeAndSeverityCheckXml() != null) {

			for (DevTypeAndSeverityCheck devTypeAndSeverityCheck : getDevTypeAndSeverityCheckXml().getDevTypeAndSeverityCheck()) {
				getHashDevTypeAndSeverityCheck().put(
						devTypeAndSeverityCheck.getDevSev(),
						devTypeAndSeverityCheck.getSuppress());

			}
		}

		log.trace("Exit: rebuildHashTables()");
		
	}

	/**
	 * 
	 */
	public void reloadFileIfUpdated() {

		if (configFile != null && configFile.exists()
				&& lastFileUpdate < configFile.lastModified()) {
			lastFileUpdate = configFile.lastModified();
			try {
				log.info(String.format("File [%s] has changed... reloading it",
						configFile.getAbsolutePath()));
				refreshFromFile();
			} catch (ConfigurationFileException e) {
				log.trace("Unable to retrieve Enrichment from file : " + e);

			}
		}
	}

	/**
	 * @return the tunableXml
	 */
	public DevTypeAndSeverityCheckXml getDevTypeAndSeverityCheckXml() {
		return devTypeAndSeverityCheckXml;
	}

	/**
	 * @param enrichmentXml
	 *            the enrichmentXml to set
	 */
	public void setDevTypeAndSeverityCheckXml(DevTypeAndSeverityCheckXml devTypeAndSeverityCheckXml) {
		this.devTypeAndSeverityCheckXml = devTypeAndSeverityCheckXml;
	}

	/**
	 * @return the hashManagedObjectToSite
	 */
	public Map<String, String> getHashDevTypeAndSeverityCheck() {
		return hashDevTypeAndSeverityCheck;
	}

	/**
	 * @return the configurationFileName
	 */
	public String getConfigurationFileNames() {
		return configurationFileNames;
	}

	/**
	 * @param configurationFileName
	 *            the configurationFileName to set
	 */
	public void setConfigurationFileNames(String configurationFileName) {
		this.configurationFileNames = configurationFileName;
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
	 * @return the configFile
	 */
	public File getconfigFile() {
		return configFile;
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
