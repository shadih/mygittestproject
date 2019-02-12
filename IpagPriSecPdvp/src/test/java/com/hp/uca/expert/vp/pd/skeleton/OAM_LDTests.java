package com.hp.uca.expert.vp.pd.skeleton;

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
public class OAM_LDTests extends AbstractJunitIntegrationTest {

	private static Logger log = LoggerFactory.getLogger(OAM_LDTests.class);
	private static final String SCENARIO_BEAN_NAME = "com.hp.uca.expert.vp.pd.ProblemDetection";

	private static final String ALARM_OAM_SUBALARM = "src/test/resources/valuepack/pd/OAMAlarms/50004_1_10Alarm.xml";
	private static final String ALARM_OAM_LPORT_TRIGGER = "src/test/resources/valuepack/pd/OAMAlarms/LinkDownAlarmOAMLportTrigger.xml";
	private static final String ALARM_OAM_DEVICE_LAG_SECONDARY = "src/test/resources/valuepack/pd/OAMAlarms/LinkDownAlarmOAMDevLagTrigger.xml";
	private static final String ALARM_OAM_PPORT_LAG_PRIMARY = "src/test/resources/valuepack/pd/OAMAlarms/LinkDownAlarmOAMPportLagSubAlarm.xml";

	private static final String LPORT_OAM_CLEARS = "src/test/resources/valuepack/pd/OAMAlarms/OAMClear_Alarms.xml";
	private static final String PPORT_OAM_CLEARS = "src/test/resources/valuepack/pd/OAMAlarms/OAMPPClear_Alarms.xml";

	private static final String ALARM_OAM_TRIGGER_ID = "LPORT-150.0.0.212/5/0/0/69-.1.3.6.1.4.1.664.1.241/6/24150";
	private static final String ALARM_OAM_PPORT_LAG_PRIMARY_ID = "PPORT-143.0.68.114/1/0/0-OAMLagPPort_SUBALARM";
	private static final String ALARM_OAM_DEVICE_LAG_SECONDARY_ID = "DEVICE-143.0.68.114-OAMLagDevice_TRIGGER";
	
	private static TmpDir tmpDir = null;	

	@BeforeClass
	public static void init() {
		tmpDir = new TmpDir("valuepack/TestPriSec/topologyDataload");
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
		log.info(Constants.TEST_START.val() + OAM_LDTests.class.getName());
	
		Loader loader = new Loader(TopoAccess.getGraphDB(), tmpDir.tmpCsvPath());
		Report report = loader.loadAll();

		//log.info(report.toString());
	}


	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		log.info(Constants.TEST_END.val() + OAM_LDTests.class.getName()
				+ Constants.GROUP_ALT1_SEPARATOR.val());
	}

	// Way to run tests via ANT Junit
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(OAM_LDTests.class);
	}
	
    @Test
	@DirtiesContext()
	public void testLinkDown() throws Exception {
    	log.info("Starting OAM Testing...");
    	
		initTest(SCENARIO_BEAN_NAME, BMK_PATH);
		getScenario().setTestOnly(true);

		Alarm alarm;
		
		
		log.info("#############");		
		log.info("#############      Starting OAM PPort LAG Testing, correlated with a device level alarm...");
		log.info("#############");

		//getProducer().sendAlarms("src/test/resources/valuepack/pd/OSFAlarms/Alarms_fromServer.xml");
		
		getProducer().sendAlarms("src/test/resources/valuepack/pd/OSFAlarms/Alarm_LD166_fromserver.xml");
		// send the device level alarm and then the pport trigger
		
		Thread.sleep(1 * SECOND);
		
		getProducer().sendAlarms(ALARM_OAM_PPORT_LAG_PRIMARY);
		Thread.sleep(10 * SECOND);
		getProducer().sendAlarms(ALARM_OAM_DEVICE_LAG_SECONDARY);

		Thread.sleep(10 * SECOND);
		
		//getScenario().getSession().dump();

		// The pport (subalarm) alarm is the primary alarm
		alarm = getAlarm(ALARM_OAM_PPORT_LAG_PRIMARY_ID);
		assertNotNull(alarm);
		assertTrue(((EnrichedAlarm) alarm).getCustomFieldValue(GFPFields.SECONDARY_ALERT_ID).equals("1"));
		assertTrue(((EnrichedAlarm) alarm).getCustomFieldValue(GFPFields.SECONDARYTIMESTAMP).equals("0"));
	
		// The device (trigger) alarm is the secodary alarm
		alarm = getAlarm(ALARM_OAM_DEVICE_LAG_SECONDARY_ID);
		assertNotNull(alarm);
		assertTrue(((EnrichedAlarm) alarm).getCustomFieldValue(GFPFields.SECONDARY_ALERT_ID).equals("12.122.122.193/1/1/0-OAMLagPPort_SUBALARM"));
		assertTrue(((EnrichedAlarm) alarm).getCustomFieldValue(GFPFields.SECONDARYTIMESTAMP).equals("1111111111"));

		log.info("#############");		
		log.info("#############      OAM PPort LAG Testing, correlated with a device level alarm has passed...");
		log.info("#############");
		
		// now we send clears for everything so the groups are deleted 
		//getProducer().sendAlarms(PPORT_OAM_CLEARS);
		////Thread.sleep(10 * SECOND);

	
		/*
		 * Disable Rule Logger to close the file used to compare engine historical
		 * events
		 */
		closeRuleLogFiles(getScenario());

		Thread.sleep(180 * SECOND);

		if (log.isDebugEnabled()) {
			getScenario().getSession().dump();
		}

		log.info("#############     At the end of Testing...");

		LogHelper.exit(log, "testLinkDown()");
	}
    
    
}
