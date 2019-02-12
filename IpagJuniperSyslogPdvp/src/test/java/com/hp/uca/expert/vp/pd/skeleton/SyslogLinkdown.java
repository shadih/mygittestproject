/**
 * 
 */
package com.hp.uca.expert.vp.pd.skeleton;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import junit.framework.JUnit4TestAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.loader.csv.Loader;
import org.neo4j.loader.csv.Report;
import org.neo4j.loader.csv.utils.TmpDir;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
//import com.att.gfp.sysloglport.enrichment.vp.sysLogLPORT_Scenario.sysLogLPORT_ScenarioTest;
import com.hp.uca.common.misc.Constants;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.group.Qualifier;
import com.hp.uca.expert.testmaterial.AbstractJunitIntegrationTest;
import com.hp.uca.expert.testmaterial.ActionListener;
import com.hp.uca.mediation.action.client.Action;

/**
 * @author df
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration

public class SyslogLinkdown extends AbstractJunitIntegrationTest {


	private static Logger log = LoggerFactory.getLogger(SyslogLinkdown.class);

	private static final String SCENARIO_BEAN_NAME = "com.hp.uca.expert.vp.pd.ProblemDetection";
	private static final String SyslogLindown50004_1_2_Alarm = "src/test/resources/valuepack/pd/LinkDownTestAlarms/SyslogLinkdown_50004_1_2.xml";
	private static final String SyslogLindown50004_3_2_Alarm = "src/test/resources/valuepack/pd/LinkDownTestAlarms/SyslogLinkdown_50004_3_2.xml";
	private static final String SyslogLindown50004_1_2_Alarm_Clear = "src/test/resources/valuepack/pd/LinkDownTestAlarms/SyslogLinkdown_50004_1_2_Alarm_Clear.xml";
	private static final String SyslogLindown50004_3_2_Alarm_Clear = "src/test/resources/valuepack/pd/LinkDownTestAlarms/SyslogLinkdown_50004_3_2_Alarm_Clear.xml";



	private static TmpDir tmpDir = null;
	
	 
	@BeforeClass
	public static void init() {
		//tmpDir = new TmpDir("valuepack/SyslogLinkdown_50004_1_2/topologyDataload");
	}


	@AfterClass
	public static void cleanup() {
		//tmpDir.cleanup();
	}
	
	/**
	 * @throws java.lang.Exception
	 */
/*	@Before
	public void setUp() throws Exception {
		log.info(Constants.TEST_START.val() + SyslogLinkdown_50004_1_2.class.getName());
	//get graph
		//Loader loader = new Loader(IpagSyslogLinkdown_50004_1_2_TopoAccess.getGraphDB(), tmpDir.tmpCsvPath());
		Report report = loader.loadAll();

		log.info(report.toString());
	}*/

	@After
	public void tearDown() throws Exception {
		log.info(Constants.TEST_END.val() + SyslogLinkdown.class.getName()
				+ Constants.GROUP_ALT1_SEPARATOR.val());
	}
	/**
	 * @throws java.lang.Exception
	 */
	

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		log.info(Constants.TEST_START.val() + SyslogLinkdown.class.getName());
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		log.info(Constants.TEST_END.val() + SyslogLinkdown.class.getName()
				+ Constants.GROUP_ALT1_SEPARATOR.val());
	}

	// Way to run tests via ANT Junit
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(SyslogLinkdown.class);
	}

	@Test
	@DirtiesContext

	public void testGeneratedPbAlarm() throws Exception {
		log.info("Starting test.....         **********");
		/*
		 * Initialize variables and Enable engine internal logs
		 */
		initTest(SCENARIO_BEAN_NAME, BMK_PATH);
		getScenario().setTestOnly(true);

		/*
		 * Send alarms
		 */
		log.trace("SEND Alarms " +getScenario());
		getProducer().sendAlarms(SyslogLindown50004_3_2_Alarm);
		Thread.sleep(5 * SECOND);
		getProducer().sendAlarms(SyslogLindown50004_1_2_Alarm);
		Thread.sleep(5 * SECOND);
        
		// Make sure the LD was suppressed
        Alarm alarm = getAlarm("DEVICE-12.80.72.33 Trigger");
        assertNotNull(alarm);
        assertTrue(((EnrichedAlarm) alarm).isSuppressed());

        alarm = getAlarm("DEVICE-12.80.72.33 SubAlarm");     
        assertNotNull(alarm);
		
        int goodGroups=0;
        Collection<Group> groups = getGroupsFromWorkingMemory();
		
		for( Group mygroup : groups) {
			if((mygroup.getFullProblemKeys().contains("<p>SyslogLinkdown_Event</p><k>device 12.80.72.33</k>") &&
					mygroup.getNumber() == 2))
				goodGroups++;			
		}
		
		assertEquals(1, goodGroups);		
		assertEquals(2, groups.size());
		
    
        
		if (log.isDebugEnabled()) {
			getScenario().getSession().dump();
		}

		/*
		 * Disable Rule Logger to close the file used to compare engine historical
		 * events
		 */
    	Thread.sleep(5 * SECOND);
      //alarms clear
      		getProducer().sendAlarms(SyslogLindown50004_1_2_Alarm_Clear);
      		getProducer().sendAlarms(SyslogLindown50004_3_2_Alarm_Clear);
      		
		closeRuleLogFiles(getScenario());

		if (log.isDebugEnabled()) {
			getScenario().getSession().dump();
		}

	}

}