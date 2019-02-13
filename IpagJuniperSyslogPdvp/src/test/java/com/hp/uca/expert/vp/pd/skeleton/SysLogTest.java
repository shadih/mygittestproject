/**
 * 
 */
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

import com.att.gfp.data.ipag.topoModel.JuniperSyslogTopoAccess;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.data.ipagAlarm.SyslogAlarm;
import com.hp.uca.common.misc.Constants;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.testmaterial.AbstractJunitIntegrationTest;
import com.hp.uca.expert.localvariable.LocalVariable;

/**
 * @author MASSE
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class SysLogTest extends AbstractJunitIntegrationTest {
	
	private static Logger log = LoggerFactory.getLogger(SysLogTest.class);

	private static final String SCENARIO_BEAN_NAME = "com.hp.uca.expert.vp.pd.ProblemDetection";

	private static final String alarm_50001_100_52 = "src/test/resources/valuepack/pd/LinkDownTestAlarms/50001_100_52_Alarm.xml";
	private static final String alarm_50002_100_55 = "src/test/resources/valuepack/pd/LinkDownTestAlarms/50002_100_55_Alarm.xml";
	private static final String alarm_50004_1_2 = "src/test/resources/valuepack/pd/LinkDownTestAlarms/50004_1_2_Alarm.xml";
	private static final String alarm_50004_3_2 = "src/test/resources/valuepack/pd/LinkDownTestAlarms/50004_3_2_Alarm.xml";
	private static final String clearalarm_50001_100_52 = "src/test/resources/valuepack/pd/LinkDownTestAlarms/50001_100_52_Clear_Alarm.xml";
	private static final String clearalarm_50002_100_55 = "src/test/resources/valuepack/pd/LinkDownTestAlarms/50002_100_55_Clear_Alarm.xml";
	private static final String clearalarm_50004_1_2 = "src/test/resources/valuepack/pd/LinkDownTestAlarms/50004_1_2_Clear_Alarm.xml";
	private static final String clearalarm_50004_3_2 = "src/test/resources/valuepack/pd/LinkDownTestAlarms/50004_3_2_Clear_Alarm.xml";

	private static final String LPORT_ENRICHMENT = "src/test/resources/valuepack/pd/LinkDownTestAlarms/LPortEnrichment_Alarms.xml";

	private static final String TRIGGER_50004_3_2_ID = "DEVICE-10.204.65.149-.1.3.6.1.4.1.664.5.63-4";
	
	private static final String RPD_MPLS_LSP_ALARMS_FILEA =
			"src/test/resources/valuepack/pd/LinkDownTestAlarms/Alarms_juniperRpdMplsLsp_a.xml";
		private static final String RPD_MPLS_LSP_ALARMS_FILEB = 
			"src/test/resources/valuepack/pd/LinkDownTestAlarms/Alarms_juniperRpdMplsLsp_b.xml";
		private static final String RPD_MPLS_LSP_ALARMS_FILEC = 
			"src/test/resources/valuepack/pd/LinkDownTestAlarms/Alarms_juniperRpdMplsLsp_c.xml";
		private static final String RPD_MPLS_LSP_ALARMS_FILED = 
			"src/test/resources/valuepack/pd/LinkDownTestAlarms/Alarms_juniperRpdMplsLsp_d.xml";
		
		private static final String RPD_MPLS_LSP_GRP1_TRIGGER = 
			"50004/1/7-12.82.0.59/AN1CA302IA2_AN1CA301IB1_B_2_1_1_BYPASS";
		private static final String RPD_MPLS_LSP_GRP1_SUB1 = 
			"50004/1/7-12.82.0.59/AN1CA302IA2_AN1CA301IB1_P_2_15_2_DEFAULT";
		private static final String RPD_MPLS_LSP_GRP1_SUB2 = 
			"50004/1/7-12.82.0.59/AN1CA302IA2_AN1CA301IB1_P_2_5_1_PRIORITY";
		private static final String ALARM_RPD_MPLS_LSP_SUB2 = 
			"50004/1/7-12.82.0.59/AN1CA302IA2_AN1CA301IB1_P_2_15_2_DEFAULT";
		private static final String RPD_MPLS_LSP_GRP2_TRIGGER = 
			"50004/1/7-12.82.0.59/AN1CA302IA2_ALACA301IA2_P_2_15_2_DEFAULT";
		private static final String RPD_MPLS_LSP_GRP3_TRIGGER = 
			"50004/1/7-12.82.0.59/AN1CA302IA2_CSACA301IA1_B_2_1_1_BYPASS";
		private static final String RPD_MPLS_LSP_GRP3_SUB1 = 
			"50004/1/7-12.82.0.59/AN1CA302IA2_CSACA301IA1_P_2_5_1_PRIORITY";
		private static final String RPD_MPLS_LSP_GRP3_CLEAR = 
			"50004/1/7-12.82.0.59/AN1CA302IA2_CSACA301IA1_P_2_15_2_DEFAULT";
		private static final String RPD_MPLS_LSP_GRP4_TRIGGER = 
			"50004/1/7-12.82.0.59/AN1CA302IA2_ALACA302IA2_P_2_15_2_ABC1";
		private static final String RPD_MPLS_LSP_GRP4_SUB1 = 
			"50004/1/7-12.82.0.59/AN1CA302IA2_ALACA302IA2_P_2_15_2_ABC2";
		private static final String RPD_MPLS_LSP_GRP5_TRIGGER = 
			"50004/1/7-12.82.0.59/ABC3";
		private static final String RPD_MPLS_LSP_GRP6_TRIGGER = 
			"50004/1/7-1.2.3.4/XYZ_GLACA301IA2_P_2_15_2_DEFAULT";
		
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
		log.info(Constants.TEST_START.val() + SysLogTest.class.getName());
	//get graph
		Loader loader = new Loader(JuniperSyslogTopoAccess.getGraphDB(), tmpDir.tmpCsvPath());
		Report report = loader.loadAll();

		log.info(report.toString());
	}

	@After
	public void tearDown() throws Exception {
		log.info(Constants.TEST_END.val() + SysLogTest.class.getName()
				+ Constants.GROUP_ALT1_SEPARATOR.val());
	}
	

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		log.info(Constants.TEST_START.val() + SysLogTest.class.getName());
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		log.info(Constants.TEST_END.val() + SysLogTest.class.getName()
				+ Constants.GROUP_ALT1_SEPARATOR.val());
	}

	// Way to run tests via ANT Junit
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(SysLogTest.class);
	}

	@Test
	@DirtiesContext
	public void SysLogTests() throws Exception {
		
		log.info("Starting polling event test.....      ");
		/*
		 * Initialize variables and Enable engine internal logs
		 */
		initTest(SCENARIO_BEAN_NAME, BMK_PATH);
		getScenario().setTestOnly(true);
		int goodGroups=0;
		Alarm alarm;
		Collection<Group> groups;
		
		/*
		 * Send alarms
		 */
		log.info("###########      ");
		log.info("########### Sending Polling event with adtran-nEpingTimedOut (50001/100/52)      ");
		log.info("###########      ");
		
		getProducer().sendAlarms(alarm_50001_100_52);
		Thread.sleep(5 * SECOND);
		
		getProducer().sendAlarms(alarm_50004_3_2);
		Thread.sleep(10 * SECOND);

		// Make sure the pollling event was suppressed
		alarm = getAlarm(TRIGGER_50004_3_2_ID);
		assertNotNull(alarm);
		if (alarm instanceof SyslogAlarm) {
			SyslogAlarm a = (SyslogAlarm) alarm;
			assertTrue(a.isSuppressed());
		}
		
		groups = getGroupsFromWorkingMemory();
		
		// there should be one group created 
		for( Group mygroup : groups) {
			List<String> keys = mygroup.getFullProblemKeys();
			for( String key : keys) {
				if(key.contains("Syslog_Polling_Event</p><k>device 10.204.65.149")) {
					if(mygroup.getNumber() == 2) {
						goodGroups++;
						break;
					}			
				}
			}
		}
		
		assertEquals(1, goodGroups);
		assertEquals(1, groups.size());

		// send the clears 
		getProducer().sendAlarms(clearalarm_50001_100_52);
		getProducer().sendAlarms(clearalarm_50004_3_2);
		Thread.sleep(20 * SECOND);

		log.info("###########      ");
		log.info("########### Sending Polling event with ciena-unreachable (50002/100/55)      ");
		log.info("###########      ");
		
		getProducer().sendAlarms(alarm_50002_100_55);
		Thread.sleep(5 * SECOND);
		
		getProducer().sendAlarms(alarm_50004_3_2);
		Thread.sleep(10 * SECOND);

		// Make sure the polling event was suppressed
		alarm = getAlarm(TRIGGER_50004_3_2_ID);
		assertNotNull(alarm);
		if (alarm instanceof SyslogAlarm) {
			SyslogAlarm a = (SyslogAlarm) alarm;
			assertTrue(a.isSuppressed());
		}
		
		groups = getGroupsFromWorkingMemory();
		goodGroups = 0;
		// there should be again, one group
		for( Group mygroup : groups) {
			List<String> keys = mygroup.getFullProblemKeys();
			for( String key : keys) {
				if(key.contains("Syslog_Polling_Event</p><k>device 10.204.65.149")) {
					if(mygroup.getNumber() == 2) {
						goodGroups++;
						break;
					}			
				}
			}
		}
		
		assertEquals(1, goodGroups);
		assertEquals(1, groups.size());
		
		// send the clears
		getProducer().sendAlarms(clearalarm_50002_100_55);
		getProducer().sendAlarms(clearalarm_50004_3_2);
		Thread.sleep(20 * SECOND);
		
		log.info("###########      ");
		log.info("########### Sending Polling event with juniper-syslog-alarm (50004/1/2)      ");
		log.info("###########      ");
		
		getProducer().sendAlarms(alarm_50004_1_2);
		Thread.sleep(5 * SECOND);
		
		getProducer().sendAlarms(alarm_50004_3_2);
		Thread.sleep(10 * SECOND);

		// make sure the polling event was again suppressed
		alarm = getAlarm(TRIGGER_50004_3_2_ID);
		assertNotNull(alarm);
		assertTrue(((EnrichedAlarm) alarm).isSuppressed());
		
		
		groups = getGroupsFromWorkingMemory();
		goodGroups = 0;
		// one group again
		for( Group mygroup : groups) {
			List<String> keys = mygroup.getFullProblemKeys();
			for( String key : keys) {
				if(key.contains("Syslog_Polling_Event</p><k>device 10.204.65.149")) {
					if(mygroup.getNumber() == 2) {
						goodGroups++;
						break;
					}			
				}
			}
		}
		
		assertEquals(1, goodGroups);
		assertEquals(1, groups.size());

		// send the clears
		getProducer().sendAlarms(clearalarm_50004_1_2);
		getProducer().sendAlarms(clearalarm_50004_3_2);
		Thread.sleep(20 * SECOND);
			
		log.info("###########      ");
		log.info("########### Sending Enrichment Alarms      ");
		log.info("###########      ");
		
		getProducer().sendAlarms(LPORT_ENRICHMENT);
		
		
		log.info("###########      ");
		log.info("########### Sending Ciena Alarm      ");
		log.info("###########      ");
		
		
		getProducer().sendAlarms("src/test/resources/valuepack/pd/CienaAlarms/Alarm1.xml");
		Thread.sleep(5 * SECOND);
		
		
		/*
		 * Disable Rule Logger to close the file used to compare engine historical
		 * events
		 */
		closeRuleLogFiles(getScenario());

		if (log.isDebugEnabled()) {
			getScenario().getSession().dump();
		}

		log.info("#############  At the end of Polling event Testing...");

	}
	
/*	   @Test
		@DirtiesContext()
		public void testLinkDown() throws Exception {
			log.info("Starting Link down test.....      ");			
			
			
			 // Initialize variables and Enable engine internal logs
			 
			initTest(SCENARIO_BEAN_NAME, BMK_PATH);
		
			//setAlarmListener(createAndAssignAlarmListener("100",getScenario()));
			
			
			 // Send alarms
			 

			log.info("Sending RPD_MPLS_LSP_ALARMS_FILEA");
			getProducer().sendAlarms(RPD_MPLS_LSP_ALARMS_FILEA);
			Thread.sleep(3 * SECOND);
			log.info("Sending RPD_MPLS_LSP_ALARMS_FILEB");
			getProducer().sendAlarms(RPD_MPLS_LSP_ALARMS_FILEB);
			Thread.sleep(3 * SECOND);
			log.info("Sending RPD_MPLS_LSP_ALARMS_FILEC");
			getProducer().sendAlarms(RPD_MPLS_LSP_ALARMS_FILEC);
			Thread.sleep(65 * SECOND);
			int numberOfGroups = 0;
			Collection<Group> groups = getGroupsFromWorkingMemory();
			for( Group group : groups) {
				if (group.getProblemContext().getName().equals("RpdMplsLspProblem")) {
					LocalVariable var = group.getVar();
					SyslogAlarm trigAlarm = (SyslogAlarm) group.getTrigger();
					if (trigAlarm.getIdentifier().equals(RPD_MPLS_LSP_GRP1_TRIGGER)) {
						numberOfGroups++;
						Alarm subAlarm = getAlarm(RPD_MPLS_LSP_GRP1_SUB1);
						assertNotNull(subAlarm);
						assertTrue(group.getAlarmList().contains(subAlarm));
						subAlarm = getAlarm(RPD_MPLS_LSP_GRP1_SUB2);
						assertNotNull(subAlarm);
						assertTrue(group.getAlarmList().contains(subAlarm));
						assertEquals(3, group.getNumber());
						assertEquals("50004/1/11-AN1CA301IB1", var.get("AGGR_ALARM_ID"));
						Long longVal = (Long) var.get("AGGR_ALARM_LAST_NOTIFIED_AT");
						assertTrue(longVal > 0);
						assertEquals("", var.get("TUNNEL_CLEAR_ID"));
						longVal = (Long) var.get("TUNNEL_ALARM_LAST_CLEARED_AT");
						assertTrue(longVal == 0);
						log.info("############# triggerId = " + RPD_MPLS_LSP_GRP1_TRIGGER + " #############");		
						log.info("#############     Aggregate alarm generated due to > 1 RpdMplsLsp ");
						log.info("#############       alarms received;  testing passed...");
						log.info("#############");
					} else if (trigAlarm.getIdentifier().equals(RPD_MPLS_LSP_GRP2_TRIGGER)) {
						numberOfGroups++;
						assertEquals(1, group.getNumber());
						assertEquals("", var.get("AGGR_ALARM_ID"));
						Long longVal = (Long) var.get("AGGR_ALARM_LAST_NOTIFIED_AT");
						assertTrue(longVal == 0);
						assertEquals("", var.get("TUNNEL_CLEAR_ID"));
						longVal = (Long) var.get("TUNNEL_ALARM_LAST_CLEARED_AT");
						assertTrue(longVal == 0);
						log.info("############# triggerId = " + RPD_MPLS_LSP_GRP2_TRIGGER + " #############");		
						log.info("#############     Aggregate alarm not generated due to only 1 RpdMplsLsp ");
						log.info("#############       alarms received;  testing passed...");
						log.info("#############");
					} else if (trigAlarm.getIdentifier().equals(RPD_MPLS_LSP_GRP3_TRIGGER)) {
						numberOfGroups++;
						Alarm subAlarm = getAlarm(RPD_MPLS_LSP_GRP3_SUB1);
						assertNotNull(subAlarm);
						assertTrue(group.getAlarmList().contains(subAlarm));
						assertEquals(2, group.getNumber());					
						assertEquals("", var.get("AGGR_ALARM_ID"));
						Long longVal = (Long) var.get("AGGR_ALARM_LAST_NOTIFIED_AT");
						assertTrue(longVal == 0);
						assertEquals(RPD_MPLS_LSP_GRP3_CLEAR, var.get("TUNNEL_CLEAR_ID"));
						longVal = (Long) var.get("TUNNEL_ALARM_LAST_CLEARED_AT");
						assertTrue(longVal > 0);
						log.info("############# triggerId = " + RPD_MPLS_LSP_GRP3_TRIGGER + " #############");		
						log.info("#############     Aggregate alarm not generated due to RpdMplsLsp ");
						log.info("#############       clear received;  testing passed...");
						log.info("#############");
					} else if (trigAlarm.getIdentifier().equals(RPD_MPLS_LSP_GRP4_TRIGGER)) {
						numberOfGroups++;
						Alarm subAlarm = getAlarm(RPD_MPLS_LSP_GRP4_SUB1);
						assertNotNull(subAlarm);
						assertTrue(group.getAlarmList().contains(subAlarm));
						assertEquals(2, group.getNumber());					
						assertEquals("50004/1/11-ALACA302IA2", var.get("AGGR_ALARM_ID"));
						Long longVal = (Long) var.get("AGGR_ALARM_LAST_NOTIFIED_AT");
						assertTrue(longVal > 0);
						assertEquals("", var.get("TUNNEL_CLEAR_ID"));
						longVal = (Long) var.get("TUNNEL_ALARM_LAST_CLEARED_AT");
						assertTrue(longVal == 0);
						log.info("############# triggerId = " + RPD_MPLS_LSP_GRP4_TRIGGER + " #############");		
						log.info("#############     tunnel not found in topo but parsed from tunnel_name");
						log.info("#############     aggregate alarm generated; testing passed...");
						log.info("#############");
					}
				}
			}
			
			assertEquals(4, numberOfGroups);
			
			Alarm alarm = getAlarm(RPD_MPLS_LSP_GRP5_TRIGGER);
			assertNotNull(alarm);
			assertTrue(alarm.getOriginatingManagedEntity().startsWith("DEVICE"));
			log.info("############# triggerId = " + RPD_MPLS_LSP_GRP5_TRIGGER + " #############");		
			log.info("#############     tunnel not found in topo, not parsed from tunnel_name");
			log.info("#############     aggregate alarm not generated; device alarm forwarded;");
			log.info("#############     testing passed...");
			log.info("#############");

			alarm = getAlarm(RPD_MPLS_LSP_GRP6_TRIGGER);
			assertNotNull(alarm);
			assertTrue(alarm.getOriginatingManagedEntity().startsWith("DEVICE"));
			log.info("############# triggerId = " + RPD_MPLS_LSP_GRP6_TRIGGER + " #############");		
			log.info("#############     device not found in topo");
			log.info("#############     aggregate alarm not generated; device alarm forwarded;");
			log.info("#############     testing passed...");
			log.info("#############");

			log.info("Sending RPD_MPLS_LSP_ALARMS_FILED");
			getProducer().sendAlarms(RPD_MPLS_LSP_ALARMS_FILED);
			Thread.sleep(3 * SECOND);
			
			for( Group group : groups) {
				if (group.getProblemContext().getName().equals("RpdMplsLspProblem")) {
					LocalVariable var = group.getVar();
					SyslogAlarm trigAlarm = (SyslogAlarm) group.getTrigger();
					if (trigAlarm.getIdentifier().equals(RPD_MPLS_LSP_GRP1_TRIGGER)) {
						assertEquals("", var.get("AGGR_ALARM_ID"));
						Long longVal = (Long) var.get("AGGR_ALARM_LAST_NOTIFIED_AT");
						assertTrue(longVal == 0);
						assertEquals(RPD_MPLS_LSP_GRP1_TRIGGER, var.get("TUNNEL_CLEAR_ID"));
						longVal = (Long) var.get("TUNNEL_ALARM_LAST_CLEARED_AT");
						assertTrue(longVal > 0);
						log.info("############# triggerId = " + RPD_MPLS_LSP_GRP1_TRIGGER + " #############");		
						log.info("#############     Aggregate alarm cleared due to RpdMplsLsp clear;");
						log.info("#############     testing passed...");
						log.info("#############");
					}
				}
			}

			
			log.info("Completed  Link down test.....      ");			

		}*/

}