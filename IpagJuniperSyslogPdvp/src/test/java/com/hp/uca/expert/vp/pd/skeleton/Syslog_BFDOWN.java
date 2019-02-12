/**
 * 
 */
package com.hp.uca.expert.vp.pd.skeleton;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import com.att.gfp.helper.GFPFields;
import com.hp.uca.common.misc.Constants;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.testmaterial.AbstractJunitIntegrationTest;

/**
 * @author MASSE
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
 
public class Syslog_BFDOWN extends AbstractJunitIntegrationTest {

	private static Logger log = LoggerFactory.getLogger(Syslog_BFDOWN.class);

	private static final String SCENARIO_BEAN_NAME = "com.hp.uca.expert.vp.pd.ProblemDetection";
	

	private static final String TwoAlarms_Clear = "src/test/resources/valuepack/pd/bfDownAlarms/BPDown_TwoAlarms_clear.xml";
	private static final String TwoAlarmsOne = "src/test/resources/valuepack/pd/bfDownAlarms/BPDown_TwoAlarmsOne.xml";
	private static final String TwoAlarmsTwo = "src/test/resources/valuepack/pd/bfDownAlarms/BPDown_TwoAlarmsTwo.xml";
	private static final String OverThreshold_Clear = "src/test/resources/valuepack/pd/bfDownAlarms/BPDown_OverThreshold_clear.xml";
	private static final String UnderThreshold_Clear = "src/test/resources/valuepack/pd/bfDownAlarms/BPDown_UnderThreshold_clear.xml";
	private static final String UnderThreshold_SubAlarms = "src/test/resources/valuepack/pd/bfDownAlarms/BPDown_UnderThreshold_Subalarms.xml";
	private static final String UnderThreshold_Trigger = "src/test/resources/valuepack/pd/bfDownAlarms/BPDown_UnderThreshold_Trigger.xml";
	private static final String AboveThreshold_Clear = "src/test/resources/valuepack/pd/bfDownAlarms/BPDown_AboveThreshold_clear.xml";
	private static final String AboveThreshold_SubAlarms = "src/test/resources/valuepack/pd/bfDownAlarms/BPDown_AboveThreshold_Subalarms.xml";
	private static final String AboveThreshold_Trigger = "src/test/resources/valuepack/pd/bfDownAlarms/BPDown_AboveThreshold_Trigger.xml";
	private static final String Site546 = "src/test/resources/valuepack/pd/bfDownAlarms/Trigger546.xml";
	private static final String Site976 = "src/test/resources/valuepack/pd/bfDownAlarms/BPDown_AboveThreshold_Trigger.xml";


	
	
	
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
		log.info(Constants.TEST_START.val() + Syslog_BFDOWN.class.getName());
	//get graph
		Loader loader = new Loader(JuniperSyslogTopoAccess.getGraphDB(), tmpDir.tmpCsvPath());
		Report report = loader.loadAll();

		log.info(report.toString());
	}

	@After
	public void tearDown() throws Exception {
		log.info(Constants.TEST_END.val() + Syslog_BFDOWN.class.getName()
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
		log.info(Constants.TEST_START.val() + Syslog_BFDOWN.class.getName());
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		log.info(Constants.TEST_END.val() + Syslog_BFDOWN.class.getName()
				+ Constants.GROUP_ALT1_SEPARATOR.val());
	}

	// Way to run tests via ANT Junit
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(Syslog_BFDOWN.class);
	}

	@Test
	@DirtiesContext
	public void testGeneratedPbAlarm() throws Exception {
		log.info("Starting test.....                 **********");
		
		// in order for these test to work the following changes have to be made in the ProblemXmlConfig.xml
		
/*		<problemPolicy name="SyslogBFDOWN_LinkDown_Event">
		<problemAlarm>
			<delayForProblemAlarmCreation>5000</delayForProblemAlarmCreation>
			.
			.
			.
			
		<longs>
			<long key="SAVPNSITE-AGGREGATION-COUNT-THRESHOLD"><value>5</value></long>
		</longs>
		
	*/	
		// and here in the LC class
/*		if(syslogAlarm != null ) {

			//test to see if this is a junit test
			setAlarmAttributesForJunitTesting( syslogAlarm);
*/
		
		
		/*
		 * Initialize variables and Enable engine internal logs
		 */
		initTest(SCENARIO_BEAN_NAME, BMK_PATH);
		getScenario().setTestOnly(true);

		log.info("###########      ");
		log.info("########### First test is to send two alarms with differing site ids, alarm with lower is suppressed...");
		log.info("###########      ");

		// for this to work the threshold must be set to 5
		
		getProducer().sendAlarms(TwoAlarmsOne);
		log.trace("SENT Alarms " +getScenario());
		Thread.sleep(1 * SECOND);

		getProducer().sendAlarms(TwoAlarmsTwo);
		log.trace("SENT Alarms " +getScenario());
			
		Thread.sleep(10 * SECOND);
				
		// Make sure the LD was suppressed
        Alarm alarmTrigger = getAlarm("DEVICE-12.82.88.16/976 Trigger");
        assertNotNull(alarmTrigger);
        assertTrue(((EnrichedAlarm) alarmTrigger).isSuppressed());
        
        Alarm alarmSubalarm = getAlarm("DEVICE-12.82.104.28 SubAlarm");     
        assertNotNull(alarmSubalarm);
        assertFalse(((EnrichedAlarm) alarmSubalarm).isSuppressed());
        
	    // check the compoent
        assertTrue(alarmTrigger.getCustomFieldValue(GFPFields.COMPONENT).contains("LocalSite=<DEVNAME2222>, RemoreSite=<DEVNAME1111>"));



        log.info("###########      ");
		log.info("########### First test complete...");
		log.info("###########      ");
            
        // send the clears
        getProducer().sendAlarms(TwoAlarms_Clear);
        
		log.info("###########      ");
		log.info("########### Second, test is to send alarms below the threshold ...");
		log.info("###########      ");
              
		getProducer().sendAlarms(UnderThreshold_SubAlarms);
		log.trace("SENT Alarms " +getScenario());
		Thread.sleep(15 * SECOND);

		getProducer().sendAlarms(UnderThreshold_Trigger);
		log.trace("SENT Alarm " +getScenario());
		Thread.sleep(10 * SECOND);
		
		
		Alarm alarmSubalarm1 = getAlarm("DEVICE-12.82.104.28 SubAlarm 1");     
        assertNotNull(alarmSubalarm1);
        assertFalse(((EnrichedAlarm) alarmSubalarm1).isSuppressed());
        
        Alarm alarmSubalarm2 = getAlarm("DEVICE-12.82.104.28 SubAlarm 2");     
        assertNotNull(alarmSubalarm2);
        assertFalse(((EnrichedAlarm) alarmSubalarm2).isSuppressed());
        
        Alarm alarmSubalarm3 = getAlarm("DEVICE-12.82.104.28 SubAlarm 3");     
        assertNotNull(alarmSubalarm3);
        assertFalse(((EnrichedAlarm) alarmSubalarm3).isSuppressed());
        
        Alarm alarmTrigger2 = getAlarm("DEVICE-12.82.88.16/976 Trigger");     
        assertNotNull(alarmTrigger2);
        assertFalse(((EnrichedAlarm) alarmTrigger2).isSuppressed());
        
		int goodGroups = 0;
		Collection<Group> groups;
		
		groups = getGroupsFromWorkingMemory();
		
		// check the groups 
		for( Group mygroup : groups) {
			List<String> keys = mygroup.getFullProblemKeys();
			for( String key : keys) {
				if(key.contains("SyslogBFDOWN_LinkDown_Event</p><k>12.82.104.28")) {
					if(mygroup.getNumber() == 4) {
						goodGroups++;
						break;
					}			
				}
			}
		}
	

		assertEquals(1, goodGroups);
		assertEquals(1, groups.size());

		log.info("###########      ");
		log.info("########### Second test complete...");
		log.info("###########      ");
  
        // send the clears
        getProducer().sendAlarms(UnderThreshold_Clear);
          
		log.info("###########      ");
		log.info("########### Third, test is to send alarms above the threshold, the agragate alarm will be created ...");
		log.info("###########      ");
		
		getProducer().sendAlarms(AboveThreshold_SubAlarms);
		log.trace("SENT Alarms " +getScenario());
		Thread.sleep(15 * SECOND);

		getProducer().sendAlarms(AboveThreshold_Trigger);
		log.trace("SENT Alarm " +getScenario());
		Thread.sleep(10 * SECOND);
	
		//getScenario().getSession().dump();
		
		goodGroups = 0;		
		groups = getGroupsFromWorkingMemory();
		
		// check the groups 
		for( Group mygroup : groups) {
			List<String> keys = mygroup.getFullProblemKeys();
			for( String key : keys) {
				if(key.contains("SyslogBFDOWN_LinkDown_Event</p><k>12.82.104.28")) {
					if(mygroup.getNumber() == 6) {
						goodGroups++;
						break;
					}			
				}
			}
		}
		
		assertEquals(1, goodGroups);
		assertEquals(1, groups.size());

		Alarm alarmSubalarm1b = getAlarm("DEVICE-12.82.104.28 SubAlarm 1");     
        assertNotNull(alarmSubalarm1b);
        assertTrue(((EnrichedAlarm) alarmSubalarm1b).isSuppressed());
        
        Alarm alarmSubalarm2b = getAlarm("DEVICE-12.82.104.28 SubAlarm 2");     
        assertNotNull(alarmSubalarm2b);
        assertTrue(((EnrichedAlarm) alarmSubalarm2b).isSuppressed());
        
        Alarm alarmSubalarm3b = getAlarm("DEVICE-12.82.104.28 SubAlarm 3");     
        assertNotNull(alarmSubalarm3b);
        assertTrue(((EnrichedAlarm) alarmSubalarm3b).isSuppressed());
     
        Alarm alarmSubalarm4b = getAlarm("DEVICE-12.82.104.28 SubAlarm 4");     
        assertNotNull(alarmSubalarm4b);
        assertTrue(((EnrichedAlarm) alarmSubalarm4b).isSuppressed());

        Alarm alarmSubalarm5b = getAlarm("DEVICE-12.82.104.28 SubAlarm 5");     
        assertNotNull(alarmSubalarm5b);
        assertTrue(((EnrichedAlarm) alarmSubalarm5b).isSuppressed());

        
        Alarm alarmTrigger2b = getAlarm("DEVICE-12.82.88.16/976 Trigger");     
        assertNotNull(alarmTrigger2b);
        assertTrue(((EnrichedAlarm) alarmTrigger2b).isSuppressed());
 
        
 
        // send the clears
        getProducer().sendAlarms(OverThreshold_Clear);
        
		log.info("###########      ");
		log.info("########### Third test complete...");
		log.info("###########      ");
    	
		
		log.info("###########      ");
		log.info("########### Forth, test is to send to 4/100/12, and the lower site is suppressed ...");
		log.info("###########      ");
		
		getProducer().sendAlarms(Site546);
		log.trace("SENT Alarms " +getScenario());
		Thread.sleep(15 * SECOND);

		getProducer().sendAlarms(Site976);
		log.trace("SENT Alarm " +getScenario());
		Thread.sleep(10 * SECOND);
	
		getScenario().getSession().dump();
		
		Alarm site546 = getAlarm("DEVICE-12.82.104.28/546 Trigger");     
        assertNotNull(site546);
        assertTrue(((EnrichedAlarm) site546).isSuppressed());
        
        Alarm site976 = getAlarm("DEVICE-12.82.88.16/976 Trigger");     
        assertNotNull(site976);
        assertFalse(((EnrichedAlarm) site976).isSuppressed());
 
        // check the compoent
        assertTrue(site976.getCustomFieldValue(GFPFields.COMPONENT).contains("LocalSite=<DEVNAME2222>, RemoreSite=<DEVNAME1111>"));
        
 		log.info("###########      ");
		log.info("########### Forth test complete...");
		log.info("###########      ");
 		
		/*
		 * Disable Rule Logger to close the file used to compare engine historical
		 * events
		 */
		closeRuleLogFiles(getScenario());

		if (log.isDebugEnabled()) {
			getScenario().getSession().dump();
		}
		
		
	}

}