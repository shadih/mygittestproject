/**
 * 
 */
package com.hp.uca.expert.vp.pd.skeleton;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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

import com.att.gfp.data.ipag.topoModel.JuniperSyslogTopoAccess;
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
 * @author MASSE
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration

public class SyslogRPD_RSVP_BypassDown extends AbstractJunitIntegrationTest {


	private static Logger log = LoggerFactory.getLogger(SyslogRPD_RSVP_BypassDown.class);

	private static final String SCENARIO_BEAN_NAME = "com.hp.uca.expert.vp.pd.ProblemDetection";

	private static final String RPD_RSVP_BYPASS_DOWN_ALL_CLEAR = "src/test/resources/valuepack/pd/bypassDownAlarms/RPD_RSVP_BYPASS_DOWN_all_clear.xml";
	private static final String JUNIPERlINKDOWN_STARTEND_LAGID_FIRSTHOP_CLEAR = "src/test/resources/valuepack/pd/bypassDownAlarms/JUNIPERlINKDOWN_StartEnd_lagid_firsthop_clear.xml";
	private static final String JUNIPERlINKDOWN_TAIL_LAGIP_FIRSTHOP_CLEAR = "src/test/resources/valuepack/pd/bypassDownAlarms/JUNIPERlINKDOWN_Tail_lagip_firsthop_clear.xml";
	private static final String JUNIPERlINKDOWN_TAIL_LAGID_LAGID_SECONDHOP_CLEAR = "src/test/resources/valuepack/pd/bypassDownAlarms/JUNIPERlINKDOWN_Tail_lagid_secondhop_clear.xml";
	private static final String JUNIPERlINKDOWN_STARTEND_LAGIP_SECONDHOP_CLEAR = "src/test/resources/valuepack/pd/bypassDownAlarms/JUNIPERlINKDOWN_StartEnd_lagip_secondhop_clear.xml";
	
	private static final String RPD_RSVP_BYPASS_DOWN_ALL = "src/test/resources/valuepack/pd/bypassDownAlarms/RPD_RSVP_BYPASS_DOWN_all.xml";
	private static final String JUNIPERlINKDOWN_STARTEND_LAGID_FIRSTHOP = "src/test/resources/valuepack/pd/bypassDownAlarms/JUNIPERlINKDOWN_StartEnd_lagid_firsthop.xml";
	private static final String JUNIPERlINKDOWN_TAIL_LAGIP_FIRSTHOP = "src/test/resources/valuepack/pd/bypassDownAlarms/JUNIPERlINKDOWN_Tail_lagip_firsthop.xml";
	private static final String JUNIPERlINKDOWN_TAIL_LAGID_LAGID_SECONDHOP = "src/test/resources/valuepack/pd/bypassDownAlarms/JUNIPERlINKDOWN_Tail_lagid_secondhop.xml";
	private static final String JUNIPERlINKDOWN_STARTEND_LAGIP_SECONDHOP = "src/test/resources/valuepack/pd/bypassDownAlarms/JUNIPERlINKDOWN_StartEnd_lagip_secondhop.xml";
	private static final String RPD_RSVP_BYPASS_DOWN_NOHOP = "src/test/resources/valuepack/pd/bypassDownAlarms/RPD_RSVP_BYPASS_DOWN_nohop.xml";
	private static final String JUNIPERlINKDOWN_STARTEND_NOMATCHLAGID_FIRSTHOP = "src/test/resources/valuepack/pd/bypassDownAlarms/JUNIPERlINKDOWN_StartEnd_nomatchlagid_firsthop.xml";
	private static final String JUNIPERlINKDOWN_STARTEND_NOMATCHLAGIP_SECONDHOP = "src/test/resources/valuepack/pd/bypassDownAlarms/JUNIPERlINKDOWN_StartEnd_nomatchlagip_secondhop.xml";
	private static final String RPD_RSVP_BYPASS_DOWN_NOMATCH = "src/test/resources/valuepack/pd/bypassDownAlarms/RPD_RSVP_BYPASS_DOWN_nomatch.xml";




	private static TmpDir tmpDir = null;


	@BeforeClass
	public static void init() {
		tmpDir = new TmpDir("valuepack/TestjuniperSyslog/topologyDataload");
	}


	@AfterClass
	public static void cleanup() {
		tmpDir.cleanup();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		log.info(Constants.TEST_START.val() + SyslogRPD_RSVP_BypassDown.class.getName());
		//get graph
		Loader loader = new Loader(JuniperSyslogTopoAccess.getGraphDB(), tmpDir.tmpCsvPath());
		Report report = loader.loadAll();

		log.info(report.toString());
	}

	@After
	public void tearDown() throws Exception {
		log.info(Constants.TEST_END.val() + SyslogRPD_RSVP_BypassDown.class.getName()
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
		log.info(Constants.TEST_START.val() + SyslogRPD_RSVP_BypassDown.class.getName());
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		log.info(Constants.TEST_END.val() + SyslogRPD_RSVP_BypassDown.class.getName()
				+ Constants.GROUP_ALT1_SEPARATOR.val());
	}

	// Way to run tests via ANT Junit
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(SyslogRPD_RSVP_BypassDown.class);
	}

	@Test
	@DirtiesContext

	public void testGeneratedPbAlarm() throws Exception {
		log.info("Starting test.....                 **********");
		/*
		 * Initialize variables and Enable engine internal logs
		 */
		initTest(SCENARIO_BEAN_NAME, BMK_PATH);
		getScenario().setTestOnly(true);


		//		/*
		//		 * Send alarms
		//		 */
		log.trace("#############");
		log.trace("First test, get the loopback IP from the discovered start_end_device_clli...");
		log.trace("#############");

		Alarm alarm = null;
		int goodGroups = 0;
		Collection<Group> groups = null;

		// // This first alarm matches the lag Id from the component and the startEndLoopback IP in the first hop
		getProducer().sendAlarms(JUNIPERlINKDOWN_STARTEND_LAGID_FIRSTHOP);
		log.trace("Sent Alarm one" +getScenario());
		Thread.sleep(5 * SECOND);

		// the second alarm matches the lag ip from the component and the tail loopback ip in the first hop
		getProducer().sendAlarms(JUNIPERlINKDOWN_TAIL_LAGIP_FIRSTHOP);
		log.trace("Sent Alarm one" +getScenario());
		Thread.sleep(5 * SECOND);

		// the third alarm matches the lag id from the component and the tail loopback ip in the second hop
		getProducer().sendAlarms(JUNIPERlINKDOWN_TAIL_LAGID_LAGID_SECONDHOP);
		log.trace("Sent Alarm one" +getScenario());
		Thread.sleep(5 * SECOND);

		// the firth alarm matches the lag ip from the compoent and teh startEndLoopback ip from the second hop
		getProducer().sendAlarms(JUNIPERlINKDOWN_STARTEND_LAGIP_SECONDHOP);
		log.trace("Sent Alarm one" +getScenario());
		Thread.sleep(5 * SECOND);		

		getProducer().sendAlarms(RPD_RSVP_BYPASS_DOWN_ALL);
		log.trace("Sent Alarm two " +getScenario());		
		Thread.sleep(15 * SECOND);	



		alarm = getAlarm("TUNNEL-10.144.0.114/chgil311ia1_chgil302mp2_B_2_1_1_BYPASS_TRIGGER");
		assertNotNull(alarm);
		assertTrue(((EnrichedAlarm) alarm).isSuppressed());

		alarm = getAlarm("DEVICE-10.144.0.211-LINKDOWN-SUBALARM1");
		assertNotNull(alarm);
		assertFalse(((EnrichedAlarm) alarm).isSuppressed());

		alarm = getAlarm("DEVICE-10.144.0.211-LINKDOWN-SUBALARM2");
		assertNotNull(alarm);
		assertFalse(((EnrichedAlarm) alarm).isSuppressed());

		alarm = getAlarm("DEVICE-10.144.0.212-LINKDOWN-SUBALARM3");
		assertNotNull(alarm);
		assertFalse(((EnrichedAlarm) alarm).isSuppressed());

		alarm = getAlarm("DEVICE-10.144.0.114-LINKDOWN-SUBALARM4");
		assertNotNull(alarm);
		assertFalse(((EnrichedAlarm) alarm).isSuppressed());

		groups = getGroupsFromWorkingMemory();
		goodGroups = 0;

		// there should be one group created 
		for( Group mygroup : groups) {
			List<String> keys = mygroup.getFullProblemKeys();
			for( String key : keys) {
				if(key.contains("RpdRsvpBypassDown")) {
					if(mygroup.getNumber() == 5) {
						goodGroups++;
						break;
					}			
				}
			}
		}

		assertEquals(1, goodGroups);
		assertEquals(1, groups.size());

		// send clears
		getProducer().sendAlarms(JUNIPERlINKDOWN_STARTEND_LAGID_FIRSTHOP_CLEAR);
		Thread.sleep(1 * SECOND);
		getProducer().sendAlarms(JUNIPERlINKDOWN_TAIL_LAGIP_FIRSTHOP_CLEAR);
		Thread.sleep(1 * SECOND);
		getProducer().sendAlarms(JUNIPERlINKDOWN_TAIL_LAGID_LAGID_SECONDHOP_CLEAR);
		Thread.sleep(1 * SECOND);
		getProducer().sendAlarms(JUNIPERlINKDOWN_STARTEND_LAGIP_SECONDHOP_CLEAR);
		Thread.sleep(1 * SECOND);		
		getProducer().sendAlarms(RPD_RSVP_BYPASS_DOWN_ALL_CLEAR);
		Thread.sleep(5 * SECOND);	
		
		
		// -----------------------------------------

		// This is make sure there is no issue with a tunnel with no protection hops
		getProducer().sendAlarms(RPD_RSVP_BYPASS_DOWN_NOHOP);
		log.trace("Sent Alarm two " +getScenario());		
		Thread.sleep(15 * SECOND);	

		alarm = getAlarm("TUNNEL-110.144.0.137/PE26_PE1_B_2_1_1_BYPASS_TRIGGER");
		assertNotNull(alarm);
		assertFalse(((EnrichedAlarm) alarm).isSuppressed());

		// ----------------------------------------

		// here we have a tunnel with hops but the the information in the LD component fields don't match 
		// This first alarm no matches on the lag Id from the component and the startEndLoopback IP in the first hop
		getProducer().sendAlarms(JUNIPERlINKDOWN_STARTEND_NOMATCHLAGID_FIRSTHOP);
		log.trace("Sent Alarm one" +getScenario());
		Thread.sleep(5 * SECOND);

		// the second alarm no match on the lag ip from the compoent and the startEndLoopback ip from the second hop
		getProducer().sendAlarms(JUNIPERlINKDOWN_STARTEND_NOMATCHLAGIP_SECONDHOP);
		log.trace("Sent Alarm one" +getScenario());
		Thread.sleep(5 * SECOND);		

		// trigger
		getProducer().sendAlarms(RPD_RSVP_BYPASS_DOWN_NOMATCH);
		log.trace("Sent Alarm two " +getScenario());		
		Thread.sleep(15 * SECOND);	

		// nothing is suppressed
		alarm = getAlarm("TUNNEL-10.144.0.137/PE26_PE24_B_2_1_1_BYPASS_TRIGGER");
		assertNotNull(alarm);
		assertFalse(((EnrichedAlarm) alarm).isSuppressed());

		alarm = getAlarm("DEVICE-10.144.0.137-LINKDOWN-SUBALARM1");
		assertNotNull(alarm);
		assertFalse(((EnrichedAlarm) alarm).isSuppressed());

		alarm = getAlarm("DEVICE-10.1.188.165-LINKDOWN-SUBALARM2");
		assertNotNull(alarm);
		assertFalse(((EnrichedAlarm) alarm).isSuppressed());

		if (log.isDebugEnabled()) {
			getScenario().getSession().dump();
		}

		goodGroups = 0;
		groups = getGroupsFromWorkingMemory();
		
		// there should be NO groups created 
		for( Group mygroup : groups) {
			List<String> keys = mygroup.getFullProblemKeys();
			for( String key : keys) {
				if(key.contains("RpdRsvpBypassDown")) {
					if(mygroup.getNumber() > 1)
					goodGroups++;
					break;
				}
			}
		}

		assertEquals(0, goodGroups);
		assertEquals(3, groups.size());   


		log.trace("#############");
		log.trace("Test, Completed...");
		log.trace("#############");


		closeRuleLogFiles(getScenario());

		if (log.isDebugEnabled()) {
			getScenario().getSession().dump();
		}

	}

}