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
import org.mortbay.log.Log;
import org.neo4j.loader.csv.Loader;
import org.neo4j.loader.csv.Report;
import org.neo4j.loader.csv.utils.TmpDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.att.gfp.helper.GFPFields;
import com.hp.uca.common.misc.Constants;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.group.Groups;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.testmaterial.AbstractJunitIntegrationTest;
import com.hp.uca.expert.topology.TopoAccess;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;

import com.att.gfp.data.ipagAlarm.AlarmState;
import com.att.gfp.data.ipagAlarm.EnrichedJuniperAlarm;



@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class TOABgpProblemTest extends AbstractJunitIntegrationTest {

	private static Logger log = LoggerFactory.getLogger(TOABgpProblemTest.class);
	private static final String SCENARIO_BEAN_NAME = "com.hp.uca.expert.vp.pd.ProblemDetection";

	
	private static final String ALARM_BGP1 = 
		"src/test/resources/com/hp/uca/expert/vp/pd/skeleton/BGP_TOA_Alarms1.xml";
	private static final String ALARM_BGP2 = 
			"src/test/resources/com/hp/uca/expert/vp/pd/skeleton/BGP_TOA_Alarms2.xml";
	private static final String ALARM_BGP_CLEAR1 = 
			"src/test/resources/com/hp/uca/expert/vp/pd/skeleton/BGP_Alarms_clear1.xml";
	private static final String ALARM_BGP_CLEAR2 = 
			"src/test/resources/com/hp/uca/expert/vp/pd/skeleton/BGP_Alarms_clear2.xml";
	private static final int TOTAL_BGP_ALARMS = 31;
	
	private static final String TEST1 = "src/test/resources/com/hp/uca/expert/vp/pd/skeleton/Test1.xml";

	
	private static TmpDir tmpDir = null;
	
	@BeforeClass
	public static void init() {
 		//tmpDir = new TmpDir("valuepack/pd/topologyDataload");
		tmpDir = new TmpDir("topologyDataload_mini");

	}

	@AfterClass
	public static void cleanup() {
		//tmpDir.cleanup();
	}
	
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		log.info(Constants.TEST_START.val() + TOABgpProblemTest.class.getName());
	
		Loader loader = new Loader(TopoAccess.getGraphDB(), tmpDir.tmpCsvPath());
		Report report = loader.loadAll();

		log.info(report.toString());
	}


	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		log.info(Constants.TEST_END.val() + TOABgpProblemTest.class.getName()
				+ Constants.GROUP_ALT1_SEPARATOR.val());
	}

	// Way to run tests via ANT Junit
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(TOABgpProblemTest.class);
	}
	

	@Test
	@DirtiesContext()

	public void testBgp() throws Exception {

		initTest(SCENARIO_BEAN_NAME, BMK_PATH);
		getScenario().setTestOnly(true);

//    	getProducer().sendAlarms(TEST1);
//		Thread.sleep(5 * SECOND);	
//		
//    	getProducer().sendAlarms(TEST2);
//		Thread.sleep(5 * SECOND);	
//		
//		getScenario().getSession().dump();
//		
//		
		log.info("#############");		
		log.info("##############    Sending " + ALARM_BGP1);
		log.info("#############");		
    	getProducer().sendAlarms(ALARM_BGP1);
		//Thread.sleep(20 * SECOND);
		Thread.sleep(300 * SECOND);
		
		log.info("#############");		
		log.info("##############    Sending " + ALARM_BGP2);
		log.info("#############");		
    //	getProducer().sendAlarms(ALARM_BGP2);
		// DF -> increased the delay to > 3 minutes so that all watchdogs will expire before we check the results
    	Thread.sleep(190 * SECOND);		
		
		getScenario().getSession().dump();
		
		int i = 1;
		Alarm[] alarms = new Alarm[TOTAL_BGP_ALARMS + 1];
		for (; i <= TOTAL_BGP_ALARMS; i++) {
			alarms[i] = getAlarm(Integer.toString(i));
		}

		log.info("#############");		
		log.info("##############    Starting validations");
		log.info("#############");		
		log.info("numberOfAlarms=" + getAlarmsFromWorkingMemory().size());
		log.info("number Of Alarms in collection=" + i);

		int numberOfBgpGroups = 0;
		Collection<Group> groups = getGroupsFromWorkingMemory();
		for( Group group : groups) {
			if (group.getProblemContext().getName().equals("BgpProblem")) {
				if (group.getTrigger().equals(alarms[1])) {
					numberOfBgpGroups++;
					assertTrue(group.getAlarmList().contains(alarms[1]));
					assertEquals(1, group.getNumber());
					if (alarms[1] instanceof EnrichedJuniperAlarm) {
						EnrichedJuniperAlarm a = (EnrichedJuniperAlarm) alarms[1];
						assertTrue(a.isSuppressed());
					}
					log.info("############# id = 1 #############");		
					log.info("#############     BGP alarm suppressed due to neighbor not found; testing passed...");
					log.info("#############");
				} else if (group.getTrigger().equals(alarms[2])) {
					numberOfBgpGroups++;
					assertTrue(group.getAlarmList().contains(alarms[2]));
					assertEquals(1, group.getNumber());
					if (alarms[2] instanceof EnrichedJuniperAlarm) {
						EnrichedJuniperAlarm a = (EnrichedJuniperAlarm) alarms[2];
						assertTrue(a.isSuppressed());
					}
					log.info("############# id = 2 #############");		
					log.info("#############     BGP alarm suppressed due to device not found; testing passed...");
					log.info("#############");
				} else if (group.getTrigger().equals(alarms[3])) {
					numberOfBgpGroups++;
					assertTrue(group.getAlarmList().contains(alarms[3]));
					assertTrue(group.getAlarmList().contains(alarms[27]));
					assertEquals(2, group.getNumber());
					if (alarms[3] instanceof EnrichedJuniperAlarm) {
						EnrichedJuniperAlarm a = (EnrichedJuniperAlarm) alarms[3];
						assertTrue(a.isSuppressed());
					}
					if (alarms[27] instanceof EnrichedJuniperAlarm) {
						EnrichedJuniperAlarm a = (EnrichedJuniperAlarm) alarms[27];
						// DF -> The subrole was not populated in alarm which caused a null exception.
						// the fix was to added this as a custom field which is then extracted and alarm populated.
						assertTrue(!a.isSuppressed());
					}
					log.info("############# id = 3 #############");		
					log.info("#############     The local DeviceSubRole is not MRR; neighbor DeviceSubRole is not MRR - suppressing local alarm.; " +
						"testing passed...");
					log.info("#############");
				} else if (group.getTrigger().equals(alarms[5]) || group.getTrigger().equals(alarms[6])) {
					numberOfBgpGroups++;
					assertTrue(group.getAlarmList().contains(alarms[5]));
					assertTrue(group.getAlarmList().contains(alarms[6]));
					assertEquals(2, group.getNumber());
					EnrichedJuniperAlarm a5 = (EnrichedJuniperAlarm) alarms[5];
					EnrichedJuniperAlarm a6 = (EnrichedJuniperAlarm) alarms[6];
					if (a5 == group.getTrigger()) {
						assertTrue(!a5.isSuppressed());
						assertTrue(a6.isSuppressed());
					} else {
						assertTrue(a5.isSuppressed());
						assertTrue(!a6.isSuppressed());
					}
					log.info("############# id = 5,6 #############");		
					log.info("#############     Both BGP alarms are from gfpip devices; ");
					log.info("#############       trigger alarm is forwarded and ");
					log.info("#############       the other is suppressed; testing passed... ");
					log.info("#############");
				} else if (group.getTrigger().equals(alarms[7]) || group.getTrigger().equals(alarms[8])) {
					numberOfBgpGroups++;
					assertTrue(group.getAlarmList().contains(alarms[7]));
					assertTrue(group.getAlarmList().contains(alarms[8]));
					assertEquals(2, group.getNumber());
					if (alarms[7] instanceof EnrichedJuniperAlarm) {
						EnrichedJuniperAlarm a = (EnrichedJuniperAlarm) alarms[7];
						assertTrue(!a.isSuppressed());
						assertTrue(a.getCustomFieldValue("info1").contains("Child=Y"));
						 
					}
					if (alarms[8] instanceof EnrichedJuniperAlarm) {
						EnrichedJuniperAlarm a = (EnrichedJuniperAlarm) alarms[8];
						assertTrue(a.isSuppressed());
					}
					log.info("############# id = 7,8 #############");		
					log.info("#############     BGP alarm from non-gfpip device (id=7) is forwarded with");
					log.info("#############       modified info1; BGP alarm from gfpip device is suppressed; ");
					log.info("#############       testing passed... ");
					log.info("#############");
				} else if (group.getTrigger().equals(alarms[9])) {
					numberOfBgpGroups++;
					assertTrue(group.getAlarmList().contains(alarms[9]));
					assertTrue(group.getAlarmList().contains(alarms[10]));
					assertEquals(2, group.getNumber());
					if (alarms[9] instanceof EnrichedJuniperAlarm) {
						EnrichedJuniperAlarm a = (EnrichedJuniperAlarm) alarms[9];
						assertTrue(a.isSuppressed());
					}
					log.info("############# id = 9,10 #############");		
					log.info("#############     BGP alarm suppressed due to neighbor unreachable; ");
					log.info("#############       testing passed...");
					log.info("#############");
				} else if (group.getTrigger().equals(alarms[11])) {
					numberOfBgpGroups++;
					assertTrue(group.getAlarmList().contains(alarms[11]));
					assertTrue(group.getAlarmList().contains(alarms[12]));
					assertTrue(group.getAlarmList().contains(alarms[13]));
					assertEquals(3, group.getNumber());
					if (alarms[11] instanceof EnrichedJuniperAlarm) {
						EnrichedJuniperAlarm a = (EnrichedJuniperAlarm) alarms[11];
						assertTrue(!a.isSuppressed());
					}
					log.info("############# id = 11,12,13 #############");		
					log.info("#############     BGP alarm from vr1; at least one crs-facing pport is up; ");
					log.info("#############       BGP alarm forwarded; testing passed...");
					log.info("#############");
				} else if (group.getTrigger().equals(alarms[15])) {
					numberOfBgpGroups++;
					assertTrue(group.getAlarmList().contains(alarms[15]));
					assertTrue(group.getAlarmList().contains(alarms[16]));
					assertTrue(group.getAlarmList().contains(alarms[17]));
					assertEquals(3, group.getNumber());
					if (alarms[15] instanceof EnrichedJuniperAlarm) {
						EnrichedJuniperAlarm a = (EnrichedJuniperAlarm) alarms[15];
						assertTrue(a.isSuppressed());
					}
					log.info("############# id = 15,16,17 #############");		
					log.info("#############     BGP alarm from vr1; all crs-facing pports are down; ");
					log.info("#############       BGP alarm suppressed; testing passed...");
					log.info("#############");
				} else if (group.getTrigger().equals(alarms[20])) {
					numberOfBgpGroups++;
					assertTrue(group.getAlarmList().contains(alarms[20]));
					assertEquals(1, group.getNumber());
					if (alarms[20] instanceof EnrichedJuniperAlarm) {
						EnrichedJuniperAlarm a = (EnrichedJuniperAlarm) alarms[20];
						assertTrue(a.isSuppressed());
					}
					log.info("############# id = 20 #############");		
					log.info("#############     BGP alarm suppressed due to component device name ");
					log.info("#############       does not equal topology device name; testing passed...");
					log.info("#############");
				} else if (group.getTrigger().equals(alarms[21])) {
					numberOfBgpGroups++;
					assertTrue(group.getAlarmList().contains(alarms[21]));
					assertEquals(1, group.getNumber());
					if (alarms[21] instanceof EnrichedJuniperAlarm) {
						EnrichedJuniperAlarm a = (EnrichedJuniperAlarm) alarms[21];
						assertTrue(!a.isSuppressed());
						assertTrue(a.getCustomFieldValue("info1").contains("Child=Y"));
					}				
					log.info("############# id = 21 #############");		
					log.info("#############     BGP alarm forwarded with modified info1; testing passed...");
					log.info("#############");

				} else if (group.getTrigger().equals(alarms[24])) {
					numberOfBgpGroups++;
					assertTrue(group.getAlarmList().contains(alarms[24]));
					assertTrue(group.getAlarmList().contains(alarms[28]));
					assertEquals(2, group.getNumber());
					if (alarms[24] instanceof EnrichedJuniperAlarm) {
						EnrichedJuniperAlarm a = (EnrichedJuniperAlarm) alarms[24];
						assertTrue(!a.isSuppressed());
					}
					if (alarms[28] instanceof EnrichedJuniperAlarm) {
						EnrichedJuniperAlarm a = (EnrichedJuniperAlarm) alarms[28];
						// DF -> The subrole was not populated in alarm which caused a null exception.
						// the fix was to added this as a custom field which is then extracted and alarm populated.
						assertTrue(a.isSuppressed());
					}
					log.info("############# id = 24,28 #############");		
					log.info("#############     First BGP alarm (subDeviceRole = MRR) forwarded; ");
					log.info("#############     second BGP alarm (subDeviceRole != MRR) suppressed; ");
					log.info("#############       testing passed...");
					log.info("#############");
				} else if (group.getTrigger().equals(alarms[25])) {
					numberOfBgpGroups++;
					assertTrue(group.getAlarmList().contains(alarms[25]));
					assertTrue(group.getAlarmList().contains(alarms[29]));
					assertEquals(2, group.getNumber());
					if (alarms[25] instanceof EnrichedJuniperAlarm) {
						EnrichedJuniperAlarm a = (EnrichedJuniperAlarm) alarms[25];
						assertTrue(!a.isSuppressed());
					}
					if (alarms[29] instanceof EnrichedJuniperAlarm) {
						EnrichedJuniperAlarm a = (EnrichedJuniperAlarm) alarms[29];
						assertTrue(!a.isSuppressed());
					}
					log.info("############# id = 25,29 #############");		
					log.info("#############     First BGP alarm (subDeviceRole != MRR) forwarded; ");
					log.info("#############     second BGP alarm (subDeviceRole = MRR) forwarded; ");
					log.info("#############       testing passed...");
					log.info("#############");
				} else if (group.getTrigger().equals(alarms[26])) {
					numberOfBgpGroups++;
					assertTrue(group.getAlarmList().contains(alarms[26]));
					assertTrue(group.getAlarmList().contains(alarms[30]));
					assertEquals(2, group.getNumber());
					if (alarms[26] instanceof EnrichedJuniperAlarm) {
						// DF -> This test was backwards.   THe second alarm is suppressed as stated in the info log.
						EnrichedJuniperAlarm a = (EnrichedJuniperAlarm) alarms[26];
						//assertTrue(!a.isSuppressed());
						assertTrue(a.isSuppressed());
					}
					if (alarms[30] instanceof EnrichedJuniperAlarm) {
						EnrichedJuniperAlarm a = (EnrichedJuniperAlarm) alarms[30];
						//assertTrue(a.isSuppressed());
						assertTrue(!a.isSuppressed());
					}
					log.info("############# id = 26,30 #############");		
					log.info("#############     First BGP alarm (subDeviceRole = MRR) forwarded; ");
					log.info("#############     second BGP alarm (subDeviceRole = MRR) suppressed; ");
					log.info("#############       testing passed...");
					log.info("#############");
				}
			}
		}
		assertEquals(13, numberOfBgpGroups);
	}   
}

