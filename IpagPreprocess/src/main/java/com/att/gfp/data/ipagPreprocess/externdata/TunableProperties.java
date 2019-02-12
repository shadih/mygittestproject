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


public class TunableProperties extends XmlConfiguration implements 
TunablePropertiesMXBean {

	private static final String TUNABLE_JMX_NAME = "Tunable";

	private static final String UCA_EXPERT_APPLICATION = "uca_ebc";

	/**
	 * 
	 */
	public static final String TUNABLE_CONFIG_XML = "OID-KE.xml";

	private static final Logger log = LoggerFactory.getLogger(TunableProperties.class);

	/**
	 * JMX Helper, manages information exported to the JMX interface.
	 */
	private JMXManager jmxManager;

	private File tunableFile;

	private Scenario scenario;

	private TunableXml tunableXml;

	private long lastFileUpdate = 0L;

	private String configurationFileName = TUNABLE_CONFIG_XML;

	private Map<String, OIDToEname> hashOIDToEname = Collections
			.synchronizedMap(new HashMap<String, OIDToEname>());

	/**
	 * 
	 */
	public TunableProperties() {
		super();

		setObjectClass(com.att.gfp.data.ipagPreprocess.externdata.TunableXml.class);

	}


	/**
	 * 
	 */
	@PostConstruct
	public void init() {
		log.info("Enter: TunableProperties  init()");
		
		tunableFile = service_util.getfileFromResourceName(TUNABLE_CONFIG_XML);

		if (tunableFile != null && tunableFile.exists()) {
			lastFileUpdate = tunableFile.lastModified();
		}

		try {
			initialize(configurationFileName);
			refreshFromFile();
		} catch (ConfigurationFileException e) {
			log.error(
					"Unable to retrieve event names from file : " + e);

		}

		if(tunableFile.exists()) {
			log.info("file to open:  " + tunableFile.getAbsolutePath());
		} else {
			log.info("file doesn't exist:  " + tunableFile.getAbsolutePath());
		}
		
		log.trace("Exit: init()");
		
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

		synchronized (getHashOIDToEname()) {

			setTunableXml((TunableXml) unmarshal());

			rebuildHashTables();
		}

			log.trace("refreshFromFile()");

	}

	@Override
	public void saveToFile() throws ConfigurationFileException {
		 log.trace("Enter: saveToFile()");
		
		marshal(getTunableXml());

		log.trace("Exit: saveToFile()");
	}

	protected void rebuildHashTables() {
		log.trace("Enter: rebuildHashTables()");

		getHashOIDToEname().clear();

		if (getTunableXml() != null) {

			for (OIDToEname oidToEname : getTunableXml().getOIDToEname()) {
				getHashOIDToEname().put(
						oidToEname.getOID(),
						oidToEname); 

			}
		}

		log.trace("Exit: rebuildHashTables()");
		
	}

	/**
	 * 
	 */
	public void reloadFileIfUpdated() {

		if (tunableFile != null && tunableFile.exists()
				&& lastFileUpdate < tunableFile.lastModified()) {
			lastFileUpdate = tunableFile.lastModified();
			try {
				log.info(String.format("File [%s] has changed... reloading it",
						tunableFile.getAbsolutePath()));
				refreshFromFile();
			} catch (ConfigurationFileException e) {
				log.trace("Unable to retrieve Enrichment from file : " + e);

			}
		}
	}

	/**
	 * @return the tunableXml
	 */
	public TunableXml getTunableXml() {
		return tunableXml;
	}

	/**
	 * @param enrichmentXml
	 *            the enrichmentXml to set
	 */
	public void setTunableXml(TunableXml tunableXML) {
		this.tunableXml = tunableXML;
	}

	/**
	 * @return the hashManagedObjectToSite
	 */
	public Map<String, OIDToEname> getHashOIDToEname() {
		return hashOIDToEname;
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
	public File getTunableFile() {
		return tunableFile;
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
