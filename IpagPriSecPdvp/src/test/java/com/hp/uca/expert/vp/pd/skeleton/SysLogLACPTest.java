/**
 * 
 */
package com.hp.uca.expert.vp.pd.skeleton;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import junit.framework.JUnit4TestAdapter;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.loader.csv.utils.TmpDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.neo4j.loader.csv.Loader;
import org.neo4j.loader.csv.Report;
import org.neo4j.loader.csv.utils.TmpDir;

import com.att.gfp.data.ipag.topoModel.PriSecTopoAccess;
import com.att.gfp.helper.GFPFields;
import com.hp.uca.common.misc.Constants;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.testmaterial.AbstractJunitIntegrationTest;

/**
 * @author MASSE
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class SysLogLACPTest extends AbstractJunitIntegrationTest {
	
	private static final String _1111111111 = "1111111111";

	private static final String _6666666666 = "6666666666";

	private static final String _3333333333 = "3333333333";

	private static final String _50003_100_1_150_0_0_212_5_0_0_IPAG01 = "50003/100/1-150.0.0.212/5/0/0-IPAG01";

	private static final String _50004_1_21_150_0_0_212_5_0_0_IPAG01 = "50004/1/21-150.0.0.212/5/0/0-IPAG01";

	private static final String _50004_1_10_150_0_0_212_5_0_0_IPAG01 = "50004/1/10-150.0.0.212/5/0/0-IPAG01";

	private static Logger log = LoggerFactory.getLogger(SysLogLACPTest.class);

	private static final String SCENARIO_BEAN_NAME = "com.hp.uca.expert.vp.pd.ProblemDetection";

	private static final String alarm_late_50003_100_1 = "src/test/resources/valuepack/pd/LCAPAlarms/50003_100_1_Late_Alarm.xml";
	private static final String alarm_50003_100_1 = "src/test/resources/valuepack/pd/LCAPAlarms/50003_100_1_Alarm.xml";
	private static final String alarm_50003_100_24 = "src/test/resources/valuepack/pd/LCAPAlarms/50003_100_24_Alarm.xml";
	private static final String alarm_remote_50004_1_10 = "src/test/resources/valuepack/pd/LCAPAlarms/50004_1_10_remote_Alarm.xml";
	private static final String alarm_50004_1_10 = "src/test/resources/valuepack/pd/LCAPAlarms/50004_1_10_Alarm.xml";
	private static final String alarm_50004_1_18 = "src/test/resources/valuepack/pd/LCAPAlarms/50004_1_18_Alarm.xml";
	private static final String alarm_Early_50004_1_18 = "src/test/resources/valuepack/pd/LCAPAlarms/50004_1_18_Early_Alarm.xml";
	private static final String alarm_50004_1_19 = "src/test/resources/valuepack/pd/LCAPAlarms/50004_1_19_Alarm.xml";
	private static final String alarm_50004_1_20 = "src/test/resources/valuepack/pd/LCAPAlarms/50004_1_20_Alarm.xml";
	private static final String alarm_50004_1_21 = "src/test/resources/valuepack/pd/LCAPAlarms/50004_1_21_Alarm.xml";
		
	private static final String clear_alarm_50003_100_1 = "src/test/resources/valuepack/pd/LCAPAlarms/clear_50003_100_1_Alarm.xml";
	private static final String clear_alarm_50003_100_24 = "src/test/resources/valuepack/pd/LCAPAlarms/clear_50003_100_24_Alarm.xml";
	private static final String clear_alarm_50004_1_10 = "src/test/resources/valuepack/pd/LCAPAlarms/clear_50004_1_10_Alarm.xml";
	private static final String clear_alarm_50004_1_18 = "src/test/resources/valuepack/pd/LCAPAlarms/clear_50004_1_18_Alarm.xml";
	private static final String clear_alarm_50004_1_19 = "src/test/resources/valuepack/pd/LCAPAlarms/clear_50004_1_19_Alarm.xml";
	private static final String clear_alarm_50004_1_20 = "src/test/resources/valuepack/pd/LCAPAlarms/clear_50004_1_20_Alarm.xml";
	private static final String clear_alarm_50004_1_21 = "src/test/resources/valuepack/pd/LCAPAlarms/clear_50004_1_21_Alarm.xml";

	private static final String alarm_50003_100_1_ID = "PPORT-150.0.0.212/5/0/0-50003/100/1_7500";
	private static final String alarm_50003_100_24_ID = "PPORT-150.0.0.212/5/0/0-50003/100/24_VPWS:364";
	private static final String alarm_50004_1_10_ID = "PPORT-150.0.0.212/5/0/0-50004/1/10_VPWS:197594157";
	private static final String alarm_remote_50004_1_10_ID = "PPORT-150.0.0.100/5/0/0-50004/1/10_remote";
	private static final String alarm_50004_1_18_ID = "PPORT-150.0.0.212/5/0/0-50004/1/18_VPWS:1364_7594";
	private static final String alarm_50004_1_19_ID = "PPORT-150.0.0.212/5/0/0-50004/1/19_VPWS:1_197594";
	private static final String alarm_50004_1_20_ID = "PPORT-150.0.0.212/5/0/0-50004/1/20_VPWS:10364_94";
	private static final String alarm_50004_1_21_ID = "PPORT-150.0.0.212/5/0/0-50004/1/21_VPWS:164_4";
		
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
		log.info("############ " + Constants.TEST_START.val() + SysLogLACPTest.class.getName());
	//get graph
		Loader loader = new Loader(PriSecTopoAccess.getGraphDB(), tmpDir.tmpCsvPath());
		Report report = loader.loadAll();

		log.info(report.toString());
	}

	@After
	public void tearDown() throws Exception {
		log.info(Constants.TEST_END.val() + SysLogLACPTest.class.getName()
				+ Constants.GROUP_ALT1_SEPARATOR.val());
	}
	

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		log.info(Constants.TEST_START.val() + SysLogLACPTest.class.getName());
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		log.info(Constants.TEST_END.val() + SysLogLACPTest.class.getName()
				+ Constants.GROUP_ALT1_SEPARATOR.val());
	}

	// Way to run tests via ANT Junit
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(SysLogLACPTest.class);
	}

	@Test
	@DirtiesContext
	public void SysLogTests() throws Exception {
		
		log.info("Starting LAPC tests.....      ");
		/*
		 * Initialize variables and Enable engine internal logs
		 */
		initTest(SCENARIO_BEAN_NAME, BMK_PATH);
		getScenario().setTestOnly(true);
		Alarm alarm;
		
		
		 // Send alarms
		 
		log.info("###########      ");
		log.info("########### Sending all alarms with Juniper Link down 50003/100/1 first      ");
		log.info("###########      ");
		
		// first grouping 
		// 50003_100_1 is the primary, sending all alarms
		//1
		getProducer().sendAlarms(alarm_50003_100_1);
		Thread.sleep(1 * SECOND);		
		//3
		getProducer().sendAlarms(alarm_50004_1_10);
		Thread.sleep(1 * SECOND);
		//4
		getProducer().sendAlarms(alarm_50004_1_18);
		Thread.sleep(1 * SECOND);
		//5
		getProducer().sendAlarms(alarm_50004_1_19);
		Thread.sleep(1 * SECOND);
		//7
		getProducer().sendAlarms(alarm_50004_1_20);
		Thread.sleep(1 * SECOND);
		//6
		getProducer().sendAlarms(alarm_50004_1_21);
		Thread.sleep(1 * SECOND);
		//9
		getProducer().sendAlarms(alarm_50003_100_24);
		Thread.sleep(65 * SECOND);	

		// the 50003/100/1 alarm is the primary
		alarm = getAlarm(alarm_50003_100_1_ID);		
		assertNotNull(alarm);
		assertTrue(alarm.getCustomFieldValue(GFPFields.SECONDARY_ALERT_ID).equals("1"));
		assertTrue(alarm.getCustomFieldValue(GFPFields.SECONDARYTIMESTAMP).equals("0"));
		
		// the rest are secondary alarms
		// the 50003/100/24 alarm is the secondary
		alarm = getAlarm(alarm_50003_100_24_ID);		
		assertNotNull(alarm);
		assertTrue(alarm.getCustomFieldValue(GFPFields.SECONDARY_ALERT_ID).equals(_50003_100_1_150_0_0_212_5_0_0_IPAG01));
		assertTrue(alarm.getCustomFieldValue("SecondaryTimeStamp").equals(_1111111111));

	
		
		log.info("###########      ");
		log.info("########### First test complete      ");
		log.info("###########      ");
		
			
		// send the clears 
		getProducer().sendAlarms(clear_alarm_50003_100_1);
		getProducer().sendAlarms(clear_alarm_50003_100_24);
		getProducer().sendAlarms(clear_alarm_50004_1_10);
		getProducer().sendAlarms(clear_alarm_50004_1_18);
		getProducer().sendAlarms(clear_alarm_50004_1_19);
		getProducer().sendAlarms(clear_alarm_50004_1_20);
		getProducer().sendAlarms(clear_alarm_50004_1_21);
		Thread.sleep(5 * SECOND);

		
		log.info("###########      ");
		log.info("########### Second group...   ");
		log.info("###########      ");

			
		// second grouping the 50003/100/18 alarm is the secondary
		// this is the primary
		//3
		getProducer().sendAlarms(alarm_50004_1_10);
		Thread.sleep(5 * SECOND);
		//4
		getProducer().sendAlarms(alarm_50004_1_18);
		Thread.sleep(5 * SECOND);
		//5
		getProducer().sendAlarms(alarm_50004_1_19);
		Thread.sleep(65 * SECOND);	

		// test the second grouping 
		// the 50004/1/10 alarm is the primary
		alarm = getAlarm(alarm_50004_1_10_ID);		
		assertNotNull(alarm);
		assertTrue(alarm.getCustomFieldValue(GFPFields.SECONDARY_ALERT_ID).equals("1"));
		assertTrue(alarm.getCustomFieldValue(GFPFields.SECONDARYTIMESTAMP).equals("0"));
		
		// the 50003/100/18 alarm is the secondary
		alarm = getAlarm(alarm_50004_1_18_ID);		
		assertNotNull(alarm);
		assertTrue(alarm.getCustomFieldValue(GFPFields.SECONDARY_ALERT_ID).equals(_50004_1_10_150_0_0_212_5_0_0_IPAG01));
		assertTrue(alarm.getCustomFieldValue(GFPFields.SECONDARYTIMESTAMP).equals(_3333333333));
		
		// send the clears 
		getProducer().sendAlarms(clear_alarm_50004_1_10);
		getProducer().sendAlarms(clear_alarm_50004_1_18);
		getProducer().sendAlarms(clear_alarm_50004_1_19);
	

		log.info("###########      ");
		log.info("########### Third group...   ");
		log.info("###########      ");
		
		// third grouping
		//3
		getProducer().sendAlarms(alarm_50004_1_10);
		Thread.sleep(5 * SECOND);
		//7
		getProducer().sendAlarms(alarm_50004_1_20);
		Thread.sleep(5 * SECOND);
		//6
		getProducer().sendAlarms(alarm_50004_1_21);
	
		Thread.sleep(65 * SECOND);	


		// test the third grouping
		// the 50004/1/10 alarm is the primary
		alarm = getAlarm(alarm_50004_1_10_ID);		
		assertNotNull(alarm);
		assertTrue(alarm.getCustomFieldValue(GFPFields.SECONDARY_ALERT_ID).equals("1"));
		assertTrue(alarm.getCustomFieldValue(GFPFields.SECONDARYTIMESTAMP).equals("0"));
	
		// the 50004/1/20 alarm is the secondary
		alarm = getAlarm(alarm_50004_1_20_ID);		
		assertNotNull(alarm);
		assertTrue(alarm.getCustomFieldValue(GFPFields.SECONDARY_ALERT_ID).equals(_50004_1_10_150_0_0_212_5_0_0_IPAG01));
		assertTrue(alarm.getCustomFieldValue(GFPFields.SECONDARYTIMESTAMP).equals(_3333333333));
		
		
		// send the clears 
		getProducer().sendAlarms(clear_alarm_50004_1_10);
		getProducer().sendAlarms(clear_alarm_50004_1_20);
		getProducer().sendAlarms(clear_alarm_50004_1_21);
		Thread.sleep(20 * SECOND);		

		log.info("###########      ");
		log.info("########### Testing Primary arrives after secondary..   ");
		log.info("###########      ");

		//1
		getProducer().sendAlarms(alarm_Early_50004_1_18);
		Thread.sleep(5 * SECOND);
		//3
		getProducer().sendAlarms(alarm_50004_1_10);
		Thread.sleep(65 * SECOND);
				
		// the 50004/1/10 alarm is the primary even though it arrives after the secondary
		alarm = getAlarm(alarm_50004_1_10_ID);		
		assertNotNull(alarm);
		assertTrue(alarm.getCustomFieldValue(GFPFields.SECONDARY_ALERT_ID).equals("1"));
		assertTrue(alarm.getCustomFieldValue(GFPFields.SECONDARYTIMESTAMP).equals("0"));

		// the 50004/1/18 alarm is the secondary
		alarm = getAlarm(alarm_50004_1_18_ID);		
		assertNotNull(alarm);
		assertTrue(alarm.getCustomFieldValue(GFPFields.SECONDARY_ALERT_ID).equals(_50004_1_10_150_0_0_212_5_0_0_IPAG01));
		assertTrue(alarm.getCustomFieldValue(GFPFields.SECONDARYTIMESTAMP).equals(_3333333333));
		
		// send the clears 
		getProducer().sendAlarms(clear_alarm_50004_1_10);
		getProducer().sendAlarms(clear_alarm_50004_1_18);
		Thread.sleep(20 * SECOND);		

		
		
		// now we repeat a test and add the remote alarm
		log.info("###########      ");
		log.info("########### Remote alarm test...   ");
		log.info("###########      ");
		
		//3
		getProducer().sendAlarms(alarm_50004_1_10);
		Thread.sleep(5 * SECOND);
		
		//34
		getProducer().sendAlarms(alarm_remote_50004_1_10);
		Thread.sleep(5 * SECOND);

		//4
		getProducer().sendAlarms(alarm_50004_1_18);
		Thread.sleep(5 * SECOND);
		//5
		getProducer().sendAlarms(alarm_50004_1_19);
		Thread.sleep(65 * SECOND);	

		// the 50004/1/10 alarm is the primary
		alarm = getAlarm(alarm_50004_1_10_ID);		
		assertNotNull(alarm);
		assertTrue(alarm.getCustomFieldValue(GFPFields.SECONDARY_ALERT_ID).equals("1"));
		assertTrue(alarm.getCustomFieldValue(GFPFields.SECONDARYTIMESTAMP).equals("0"));
		
		// the 50003/100/18 alarm is the secondary
		alarm = getAlarm(alarm_50004_1_18_ID);		
		assertNotNull(alarm);
		assertTrue(alarm.getCustomFieldValue(GFPFields.SECONDARY_ALERT_ID).equals(_50004_1_10_150_0_0_212_5_0_0_IPAG01));
		assertTrue(alarm.getCustomFieldValue(GFPFields.SECONDARYTIMESTAMP).equals(_3333333333));

		/* Disable Rule Logger to close the file used to compare engine historical
		  events */
		 
		closeRuleLogFiles(getScenario());

		if (log.isDebugEnabled()) {
			getScenario().getSession().dump();
		}

		log.info("#############  At the end of Polling event Testing...");

	}

}
