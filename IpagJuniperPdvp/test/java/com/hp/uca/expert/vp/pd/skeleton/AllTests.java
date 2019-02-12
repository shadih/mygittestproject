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
public class AllTests extends AbstractJunitIntegrationTest {

	private static Logger log = LoggerFactory.getLogger(AllTests.class);
	private static final String SCENARIO_BEAN_NAME = "com.hp.uca.expert.vp.pd.ProblemDetection";
	private static final String ALARMS_FILE_NO_TUNNEL = 
			"src/test/resources/com/hp/uca/expert/vp/pd/skeleton/Alarms_juniperRpdMplsLsp_no_tunnel.xml";
	
	private static final String ALARM_50002_100_1 = "src/test/resources/com/hp/uca/expert/vp/pd/skeleton/50002_100_1Alarm.xml";
	private static final String ALARM_TRIGGER1 = "src/test/resources/com/hp/uca/expert/vp/pd/skeleton/LinkDownAlarmTrigger.xml";
	private static final String ALARM_TRIGGER2 = "src/test/resources/com/hp/uca/expert/vp/pd/skeleton/LinkDownAlarmTrigger2.xml";
	private static final String ALARM_50003_100_7 = "src/test/resources/com/hp/uca/expert/vp/pd/skeleton/50003_100_7Alarm.xml";
	private static final String ALARM_TRIGGER1_CLEAR = "src/test/resources/com/hp/uca/expert/vp/pd/skeleton/LinkDownAlarmTriggerClear.xml";
	private static final String ALARM_DEVICE_LAG_SUBALARM = "src/test/resources/com/hp/uca/expert/vp/pd/skeleton/DeviceLinkDownAlarLagSubAlarm.xml";
	private static final String ALARM_DEVICE_LAG_TRIGGER = "src/test/resources/com/hp/uca/expert/vp/pd/skeleton/DeviceLinkDownAlarLagTrigger.xml";
	private static final String ALARM_DEVICE_ME3_SUPPRESSION = "src/test/resources/com/hp/uca/expert/vp/pd/skeleton/LinkDownAlarmTriggerME3Suppression.xml";
	private static final String ALARM_OAM_SUBALARM = "src/test/resources/com/hp/uca/expert/vp/pd/skeleton/50004_1_10Alarm.xml";
	private static final String ALARM_OAM_LPORT_TRIGGER = "src/test/resources/com/hp/uca/expert/vp/pd/skeleton/LinkDownAlarmOAMLportTrigger.xml";
	private static final String ALARM_OAM_DEVICE_lAG_SUBALARM = "src/test/resources/com/hp/uca/expert/vp/pd/skeleton/LinkDownAlarmOAMDevLagSubAlarm.xml";
	private static final String ALARM_OAM_PPORT_LAG_TRIGGER = "src/test/resources/com/hp/uca/expert/vp/pd/skeleton/LinkDownAlarmOAMPportLagTrigger.xml";

	private static final String AAF_DA_TEST_CLEARS = "src/test/resources/com/hp/uca/expert/vp/pd/skeleton/AFF_DAClear_Alarms.xml";
	private static final String OTHER_TEST_CLEARS = "src/test/resources/com/hp/uca/expert/vp/pd/skeleton/OtherClear_Alarms.xml";
	private static final String DEVICE_LAG_CLEARS = "src/test/resources/com/hp/uca/expert/vp/pd/skeleton/DeviceLagClear_Alarms.xml";
	private static final String LPORT_OAM_CLEARS = "src/test/resources/com/hp/uca/expert/vp/pd/skeleton/OAMClear_Alarms.xml";
	private static final String PPORT_OAM_CLEARS = "src/test/resources/com/hp/uca/expert/vp/pd/skeleton/OAMPPClear_Alarms.xml";

	private static final String ALARM_TRIGGER2_ID = "PPORT-12.123.80.143/2/0/4-.1.3.6.1.4.1.664.1.241/6/24150";
	private static final String ALARM_TRIGGER1_ID = "PPORT-12.123.80.143/2/0/3-.1.3.6.1.4.1.664.1.241/6/24150";
	private static final String ALARM_LAG_TRIGGER1_ID = "DEVICE-12.82.0.81-.1.3.6.1.4.1.664.1.241/6/24150";
	private static final String ALARM_OAM_TRIGGER_ID = "LPORT-150.0.0.212/5/0/0/69-.1.3.6.1.4.1.664.1.241/6/24150";
	private static final String ALARM_OAM_PPORT_LAG_TRIGGER_ID = "PPORT-12.122.122.193/1/1/0-.1.3.6.1.4.1.664.1.241/6/24150";
	
	private static final String ALARM_BGP1 = 
		"src/test/resources/com/hp/uca/expert/vp/pd/skeleton/BGP_Alarms1.xml";
	private static final String ALARM_BGP2 = 
			"src/test/resources/com/hp/uca/expert/vp/pd/skeleton/BGP_Alarms2.xml";
	private static final String ALARM_BGP_CLEAR1 = 
			"src/test/resources/com/hp/uca/expert/vp/pd/skeleton/BGP_Alarms_clear1.xml";
	private static final String ALARM_BGP_CLEAR2 = 
			"src/test/resources/com/hp/uca/expert/vp/pd/skeleton/BGP_Alarms_clear2.xml";
	private static final int TOTAL_BGP_ALARMS = 31;
	private static final String ALARM_VPN = 
			"src/test/resources/com/hp/uca/expert/vp/pd/skeleton/VPN_Alarms.xml";
	private static final String ALARM_VPN_INTF_TRIGGER1 = "50003/100/7-149.9.0.27";
	private static final String ALARM_VPN_INTF_SUB1 = "50003/100/1-10.144.0.154/2/0/0";
	private static final String ALARM_VPN_INTF_TRIGGER2 = "50003/100/7-10.144.0.154";
	private static final String ALARM_VPN_PWR_TRIGGER1 = "50003/100/6-10.144.0.154";
	private static final String ALARM_VPN_PWR_SUB1 = "50003/100/6-149.9.0.27";
	
	private static final String TEST1 = "src/test/resources/com/hp/uca/expert/vp/pd/skeleton/Test1.xml";
	private static final String TEST2 = "src/test/resources/com/hp/uca/expert/vp/pd/skeleton/Test2.xml";

	
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
		log.info(Constants.TEST_START.val() + AllTests.class.getName());
	
		Loader loader = new Loader(TopoAccess.getGraphDB(), tmpDir.tmpCsvPath());
		Report report = loader.loadAll();

		log.info(report.toString());
	}


	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		log.info(Constants.TEST_END.val() + AllTests.class.getName()
				+ Constants.GROUP_ALT1_SEPARATOR.val());
	}

	// Way to run tests via ANT Junit
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(AllTests.class);
	}
	
/*	
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
		Thread.sleep(20 * SECOND);	
		
		log.info("#############");		
		log.info("##############    Sending " + ALARM_BGP2);
		log.info("#############");		
    	getProducer().sendAlarms(ALARM_BGP2);
		// DF -> increased the delay to > 3 minutes so that all watchdogs will expire before we check the results
    	Thread.sleep(190 * SECOND);	
		
    	// DF -> this next section is commented out because as soon as a clear is received, the alarm is retracted.
    	// any checks for tha alarm or associated groups will fail.
    	
//		log.info("#############");		
//		log.info("##############    Sending " + ALARM_BGP_CLEAR1);
//		log.info("#############");		
    	//getProducer().sendAlarms(ALARM_BGP_CLEAR1);
		//Thread.sleep(185 * SECOND);	
		
//		log.info("#############");		
//		log.info("##############    Sending " + ALARM_BGP_CLEAR2);
//		log.info("#############");		
    	//getProducer().sendAlarms(ALARM_BGP_CLEAR2);
		//Thread.sleep(120 * SECOND);	
		
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

					// DF -> the section below is bogus because any cleared alarm will be retracted.
//				} else if (group.getTrigger().equals(alarms[22])) {
//					numberOfBgpGroups++;
//					assertTrue(group.getAlarmList().contains(alarms[22]));
//					assertEquals(1, group.getNumber());
//					if (alarms[22] instanceof EnrichedJuniperAlarm) {
//						EnrichedJuniperAlarm a = (EnrichedJuniperAlarm) alarms[22];
//						assertTrue(a.getPerceivedSeverity() == PerceivedSeverity.CLEAR);
//						assertTrue(a.isSuppressed());
//						assertTrue(a.getAlarmState() != AlarmState.sent);
//					}				
//					log.info("############# id = 22 #############");		
//					log.info("#############     BGP alarm suppressed due to clear within aging window; testing passed...");
//					log.info("#############");
//				} else if (group.getTrigger().equals(alarms[23])) {
//					numberOfBgpGroups++;
//					assertTrue(group.getAlarmList().contains(alarms[23]));
//					assertEquals(1, group.getNumber());
//					if (alarms[23] instanceof EnrichedJuniperAlarm) {
//						EnrichedJuniperAlarm a = (EnrichedJuniperAlarm) alarms[23];
//						assertTrue(a.getPerceivedSeverity() == PerceivedSeverity.CLEAR);
//						assertTrue(!a.isSuppressed());
//						assertTrue(a.getAlarmState() == AlarmState.sent);
//					}				
//					log.info("############# id = 23 #############");		
//					log.info("#############     BGP alarm and clear forwarded; testing passed...");
//					log.info("#############");
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
*/

	@Test
	@DirtiesContext()
	public void testVpn() throws Exception {

		initTest(SCENARIO_BEAN_NAME, BMK_PATH);
		getScenario().setTestOnly(true);
 
		log.info("#############");		
		log.info("##############    Sending " + ALARM_VPN);
		log.info("#############");		
    	getProducer().sendAlarms(ALARM_VPN);
		Thread.sleep(5 * SECOND);	
		
		getScenario().getSession().dump();
		
		log.info("#############");		
		log.info("##############    Starting validations");
		log.info("#############");		
		log.info("numberOfAlarms=" + getAlarmsFromWorkingMemory().size());
		int numberOfVpnGroups = 0;
		Collection<Group> groups = getGroupsFromWorkingMemory();
		for( Group group : groups) {
			if (group.getProblemContext().getName().equals("VpnInterfaceProblem")) {
				EnrichedJuniperAlarm trigAlarm = (EnrichedJuniperAlarm) group.getTrigger();
				if (trigAlarm.getIdentifier().equals(ALARM_VPN_INTF_TRIGGER1)) {
					numberOfVpnGroups++;
					Alarm subAlarm = getAlarm(ALARM_VPN_INTF_SUB1);
					assertNotNull(subAlarm);
					assertTrue(group.getAlarmList().contains(subAlarm));
					assertEquals(2, group.getNumber());
					assertTrue(trigAlarm.isSuppressed());
					log.info("############# id = 50003/100/7-149.9.0.27 #############");		
					log.info("#############     jnxVpnIfDown alarm suppressed due to linkDown; testing passed...");
					log.info("#############");
				} else if (trigAlarm.getIdentifier().equals(ALARM_VPN_INTF_TRIGGER2)) {
					numberOfVpnGroups++;
					// DF -> changed to 2 groups
					assertEquals(2, group.getNumber());
					// DF -> in this case both alarms in the group are suppressED.   The trigger because of the explanation below and the 
					// subAlarm because it was suppressed by another grouping.   
					assertTrue(trigAlarm.isSuppressed());
					log.info("############# id = 50003/100/7-10.144.0.154 #############");		
					log.info("#############     Two related jnxVpnIfDown alarms, the trigger is suppressed; testing passed...");
					log.info("#############");
				}
			} else if (group.getProblemContext().getName().equals("VpnPowerProblem")) {
				EnrichedJuniperAlarm trigAlarm = (EnrichedJuniperAlarm) group.getTrigger();
				if (trigAlarm.getIdentifier().equals(ALARM_VPN_PWR_TRIGGER1)) {
					numberOfVpnGroups++;
					EnrichedJuniperAlarm subAlarm = 
						(EnrichedJuniperAlarm) getAlarm(ALARM_VPN_PWR_SUB1);
					assertNotNull(subAlarm);
					assertTrue(group.getAlarmList().contains(subAlarm));
					assertEquals(2, group.getNumber());
					assertTrue(!trigAlarm.isSuppressed());
					assertTrue(subAlarm.isSuppressed());
					log.info("############# id = 50003/100/6-10.144.0.154, 50003/100/6-149.9.0.27 #############");		
					log.info("#############     first jnxVpnPwDown alarm forwarded, second suppressed; testing passed...");
					log.info("#############");
				}
			}
		}
		assertEquals(3, numberOfVpnGroups);
	}   

}

