package com.hp.uca.expert.vp.pd.skeleton;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;

import junit.framework.JUnit4TestAdapter;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.loader.csv.Loader;
import org.neo4j.loader.csv.Report;
import org.neo4j.loader.csv.utils.TmpDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.helper.GFPFields;
import com.hp.uca.common.misc.Constants;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.testmaterial.AbstractJunitIntegrationTest;
import com.hp.uca.expert.topology.TopoAccess;
//import com.hp.uca.expert.vp.pd.tools.DisplayResult;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class Device_LAG_Tests extends AbstractJunitIntegrationTest {

	private static Logger log = LoggerFactory.getLogger(Device_LAG_Tests.class);
	private static final String SCENARIO_BEAN_NAME = "com.hp.uca.expert.vp.pd.ProblemDetection";
	
	
	private static final String ALARM_DEVICE_LAG_SUBALARM = "src/test/resources/com/hp/uca/expert/vp/pd/skeleton/DeviceLinkDownAlarLagSubAlarm.xml";
	private static final String ALARM_DEVICE_LAG_TRIGGER = "src/test/resources/com/hp/uca/expert/vp/pd/skeleton/DeviceLinkDownAlarLagTrigger.xml";
	private static final String ALARM_DEVICE_LAG_TRIGGER2 = "src/test/resources/com/hp/uca/expert/vp/pd/skeleton/DeviceLinkDownAlarLagTrigger2.xml";

	private static TmpDir tmpDir = null;	

	@BeforeClass
	public static void init() {
 		tmpDir = new TmpDir("topologyDataload");
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
		log.info(Constants.TEST_START.val() + Device_LAG_Tests.class.getName());
	
//		Loader loader = new Loader(TopoAccess.getGraphDB(), tmpDir.tmpCsvPath());
//		Report report = loader.loadAll();
//
//		log.info(report.toString());
	}


	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		log.info(Constants.TEST_END.val() + Device_LAG_Tests.class.getName()
				+ Constants.GROUP_ALT1_SEPARATOR.val());
	}

	// Way to run tests via ANT Junit
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(Device_LAG_Tests.class);
	}
	
    @Test
	@DirtiesContext()
	public void testLinkDown() throws Exception {
    	log.info("Starting LinkDown Testing...");
		
		int goodGroups=0;
		
		/*
		 * Initialize variables and Enable engine internal logs
		 */
		initTest(SCENARIO_BEAN_NAME, BMK_PATH);
		getScenario().setTestOnly(true);
		Alarm alarm = null;
		Collection<Group> groups;
		
		log.info("#############");		
		log.info("###############   Testing Juniper Device LAG...");
		log.info("#############");
		
		
//		getProducer().sendAlarms(DEVICE_ALARM_50003_100_7);
//		Thread.sleep(5 * SECOND);	
//				
//		getProducer().sendAlarms(DEVICE_ALARM_50003_100_7_CLEAR);
//		Thread.sleep(60 * SECOND);
//		
	
		
		// NOTE: the second alarm that comes in is suppressed no matter which one it is
		// and the which is first or second is based on the BE time.   That is why
		// different alarms are used when we reverse the order later...
		
		getProducer().sendAlarms(ALARM_DEVICE_LAG_SUBALARM);
		Thread.sleep(5 * SECOND);	
				
		getProducer().sendAlarms(ALARM_DEVICE_LAG_TRIGGER);
		Thread.sleep(5 * SECOND);	
		
		getProducer().sendAlarms(ALARM_DEVICE_LAG_TRIGGER2);
		Thread.sleep(5 * SECOND);	

		getScenario().getSession().dump();

/*		
		// the juniper LD is not suppressed
		alarm = getAlarm(ALARM_TRIGGER1a_ID);
		assertNotNull(alarm);
		assertTrue(!((EnrichedAlarm) alarm).isSuppressed());

		// the ciena alarm is suppressed
		alarm = getAlarm(ALARM_50002_100_1_ID);
		assertNotNull(alarm);
		assertTrue(((EnrichedAlarm) alarm).isSuppressed());

		Thread.sleep(5 * SECOND);

		groups = getGroupsFromWorkingMemory();
		
		for( Group mygroup : groups) {
			if((mygroup.getFullProblemKeys().contains("<p>Ciena_Juniper_A_Z_AlarmProcessing</p><k>10.204.65.149/25</k>") &&
					mygroup.getNumber() == 2))
				goodGroups++;			
		}

		assertEquals(1, goodGroups);		
		assertEquals(4, groups.size());
		
		//send clears
		getProducer().sendAlarms(ALARM_TRIGGER1_CLEAR);
		getProducer().sendAlarms(ALARM_50002_100_1CLEAR);
		Thread.sleep(5 * SECOND);

		log.info("#############");		
		log.info("###############   Now we reverse and send the ciena first...");
		log.info("#############");

		getProducer().sendAlarms(ALARM_50002_100_1FIRST);
		Thread.sleep(5 * SECOND);	
		
		log.info("############# SENDING SECOND ALARM");
		getProducer().sendAlarms(ALARM_TRIGGER1SECOND);
		Thread.sleep(5 * SECOND);	
		
		getScenario().getSession().dump();
		
		
		// the ciena LD is not suppressed
		alarm = getAlarm(ALARM_50002_100_1_ID);
		assertNotNull(alarm);
		assertTrue(!((EnrichedAlarm) alarm).isSuppressed());

		// juniper LD is suppressed
		alarm = getAlarm(ALARM_TRIGGER1a_ID);
		assertNotNull(alarm);
		
		assertTrue(((EnrichedAlarm) alarm).isSuppressed());

		Thread.sleep(5 * SECOND);

		groups = getGroupsFromWorkingMemory();
		
		// both the a-Z and AAF_DA processing should have suppressed this alarm
		for( Group mygroup : groups) {
			if((mygroup.getFullProblemKeys().contains("<p>Ciena_Juniper_A_Z_AlarmProcessing</p><k>10.204.65.149/25</k>") &&
					mygroup.getNumber() == 2))
				goodGroups++;			
		}

		assertEquals(2, goodGroups);		
		assertEquals(4, groups.size());
		
		// send clears
		getProducer().sendAlarms(ALARM_50002_100_1CLEAR);	
		getProducer().sendAlarms(ALARM_TRIGGER1_CLEAR);
		Thread.sleep(5 * SECOND);	
		
		
		log.info("#############");		
		log.info("###############   Test Ciena LD with associated Juniper LD, finished...");
		log.info("#############");

		// now we send clears for everything so the groups are deleted 
		//getProducer().sendAlarms(AAF_DA_TEST_CLEARS);
		//Thread.sleep(10 * SECOND);
		
		log.info("#############");
		log.info("#############    Starting reason code, VpnInterfaceProblem Processing Testing...");
		log.info("#############");

		goodGroups = 0;
		
		getProducer().sendAlarms(ALARM_TRIGGER1);
		Thread.sleep(10 * SECOND);
		getProducer().sendAlarms(ALARM_50003_100_7);
		Thread.sleep(10 * SECOND);
			
		// the link down (trigger) alarm should be suppressed
		alarm = getAlarm(ALARM_TRIGGER1_ID);
		assertNotNull(alarm);
		assertTrue(!((EnrichedAlarm) alarm).isSuppressed());

		alarm = getAlarm("DEVICE-12.123.80.143/2/0/13-.1.3.6.1.4.1.664.1.241/6/24150-50003_100_7");
		assertNotNull(alarm);
		assertTrue(((EnrichedAlarm) alarm).isSuppressed());
		
		log.info("#############");
		log.info("#############    Reason code and remote port, VpnInterfaceProblem Processing Tests passed...");
		log.info("#############");
	
		// now we send clears for everything so the groups are deleted 
		getProducer().sendAlarms(OTHER_TEST_CLEARS);
		Thread.sleep(10 * SECOND);

		
		log.info("#############");
		log.info("#############      Device LAG Testing has passed...");
		log.info("#############");
		
		// now we send clears for everything so the groups are deleted 
		getProducer().sendAlarms(DEVICE_LAG_CLEARS);
		Thread.sleep(10 * SECOND);

		// now we send clears for everything so the groups are deleted 
		getProducer().sendAlarms(LPORT_OAM_CLEARS);
		Thread.sleep(10 * SECOND);
	
	
		log.info("#############");		
		log.info("#############      Test to make sure when a lag alarm arrive it is" +
				" not suppressed if there are pports with the same lag.");
		log.info("#############");

		goodGroups = 0;
		
		// send the device level alarm and then the pport trigger
		getProducer().sendAlarms(ALARM_OAM_PPORT_LAG_PRIMARY);
		Thread.sleep(1 * SECOND);
		getProducer().sendAlarms(ALARM_OAM_DEVICE_LAG_SECONDARY);

		Thread.sleep(5 * SECOND);
		
		// The pport (subalarm) alarm is the primary alarm
		alarm = getAlarm(ALARM_OAM_PPORT_LAG_PRIMARY_ID);
		assertNotNull(alarm);
		assertTrue(!((EnrichedAlarm) alarm).isSuppressed());
	
		// The device (trigger) alarm is the secodary alarm
		alarm = getAlarm(ALARM_OAM_DEVICE_LAG_SECONDARY_ID);
		assertNotNull(alarm);
		assertTrue(!((EnrichedAlarm) alarm).isSuppressed());
		
		// make sure the component field was updated with all of the ports for that lag
		assertTrue(alarm.getCustomFieldValue(GFPFields.COMPONENT).contains("portAID=<xe-1/1/0> portAID=<xe-11/3/0> portAID=<xe-1/1/1> portAID=<xe-5/0/0> portAID=<xe-1/0/1> portAID=<xe-5/3/1> portAID=<xe-5/0/1> portAID=<xe-5/3/0> portAID=<xe-1/0/0>"));
					
		log.info("#############");		
		log.info("#############     LAG and associated PPort test passed...");
		log.info("#############");

		log.info("#############");		
		log.info("#############      Here we test to make sure the lag alarm is suppressed if " +
				" there are no pports with the same lag.");
		log.info("#############");

		goodGroups = 0;
		
		// send the device level alarm and then the pport trigger
		getProducer().sendAlarms(ALARM_OAM_DEVICE_LAG_SECONDARY_NOPPORTS);
		Thread.sleep(5 * SECOND);
		
		// The device (trigger) alarm is the secodary alarm
		alarm = getAlarm(ALARM_OAM_DEVICE_LAG_SECONDARY_ID_NOPPorts);

		// the alarm is suppress by LC and never makes it to WM
		assertTrue(alarm == null);
	
		
		log.info("#############");		
		log.info("#############     LAG and associated PPort test passed...");
		log.info("#############");

		
		// now we send clears for everything so the groups are deleted 
		getProducer().sendAlarms(PPORT_OAM_CLEARS);
		Thread.sleep(10 * SECOND);

		log.info("#############");				
		log.info("#############     Starting device ME3 Suppression...");
		log.info("#############");				

		getProducer().sendAlarms(ALARM_DEVICE_ME3_SUPPRESSION);

		Thread.sleep(5 * SECOND);

		// the alarm is suppressed before processing so it should not be in WM 
		alarm = getAlarm(ALARM_DEVICE_ME3_SUPPRESSION_ID);
		assertTrue(alarm == null);
	

		log.info("#############");				
		log.info("#############     Checking evc reason field update...");
		log.info("#############");				

		
		getProducer().sendAlarms(DEVICE_ALARM_50003_100_7);
		Thread.sleep(2 * SECOND);

		alarm = getAlarm("DEVICE-12.82.0.59-.1.3.6.1.4.1.2636.3.26/6/2/3089");
		assertNotNull(alarm);		
		//assertTrue(alarm.getCustomFieldValue(GFPFields.REASON).equals("Down Region=<S>"));
		
		log.info("#############");				
		log.info("#############     Check for evc reason field update completed...");
		log.info("#############");				
		
		
		
		log.info("#############");				
		log.info("#############     Starting Ciena CDM enrichment...");
		log.info("#############");				

		getProducer().sendAlarms(CIENA_CDM_11111);
		Thread.sleep(1 * SECOND);
		getProducer().sendAlarms(CIENA_CDM_22222);
		Thread.sleep(1 * SECOND);
		getProducer().sendAlarms(LD_CIENA_CDM);
		Thread.sleep(10 * SECOND);
		getScenario().getSession().dump();
		
		alarm = getAlarm("PPORT-12.82.102.21/2/2/2-.1.3.6.1.4.1.664.1.241/6/24150");
		assertNotNull(alarm);		
		assertTrue(alarm.getCustomFieldValue(GFPFields.INFO3).contains("CFMAlertKey=<50002/100/52-10.10.10.10/VPWS:11111>"));
		assertTrue(alarm.getCustomFieldValue(GFPFields.INFO3).contains("CFMAlertKey=<50002/100/52-10.10.10.11/VPWS:22222>"));

		
		Thread.sleep(5 * SECOND);

	
		 /* Disable Rule Logger to close the file used to compare engine historical
		 * events
		 */
		
		closeRuleLogFiles(getScenario());

		if (log.isDebugEnabled()) {
			getScenario().getSession().dump();
		}

		log.info("#############     At the end of Testing...");

		LogHelper.exit(log, "testLinkDown()");
	}
    
    
}
