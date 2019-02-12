/**
 * 
 */
package com.hp.uca.expert.vp.pd.skeleton;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import junit.framework.JUnit4TestAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import com.att.gfp.data.ipag.topoModel.IpagAdtranTopoAccess;
import com.att.gfp.data.ipag.topoModel.IpagAdtranTopoAccess;
import com.hp.uca.common.misc.Constants;
import com.hp.uca.expert.alarm.Alarm; 
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.group.Qualifier;
import com.hp.uca.expert.testmaterial.AbstractJunitIntegrationTest;
import com.hp.uca.expert.testmaterial.ActionListener;
import com.hp.uca.expert.vp.pd.tools.DisplayResult;
import com.hp.uca.mediation.action.client.Action;

/**
 * @author MASSE
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class MyProblemTest extends AbstractJunitIntegrationTest {

	private static final int EXPECTED_NB_ACTIONS = 6;

	private static final int EXPECTED_NB_ALARMS = 3;

	private static final int EXPECTED_NB_GROUP = 1;  

	private static Logger log = LoggerFactory.getLogger(MyProblemTest.class);
	private static TmpDir tmpDir = null;

	private static final String SCENARIO_BEAN_NAME = "com.hp.uca.expert.vp.pd.ProblemDetection";

	private static final String ALARM_FILE = "src/main/resources/valuepack/pd/Alarms.xml";
	private static final String PB_ALARM_FILE = "src/main/resources/valuepack/pd/50001_100_1_3_Output.xml";
	private static final String PB_TR_ALARM_FILE = "src/main/resources/valuepack/pd/50001_100_1_3_Trigger_Output.xml";
	private static final String SUB_ALARM_FILE = "src/main/resources/valuepack/pd/50001_100_37_17_Output.xml"; 
	private static final String ORPHAN_ALARM_FILE = "src/main/resources/valuepack/pd/Orphan_50001_100_38_17_Output.xml";
	private static final String GROPU2_7_FILE = "src/main/resources/valuepack/pd/50001_100_1_7_Output.xml";
	private static final String GROPU2_10_FILE = "src/main/resources/valuepack/pd/50001_100_10_Trigger_Output.xml";
	private static final String GROUP2_11_FILE = "src/main/resources/valuepack/pd/50001_100_11_Trigger2_Output.xml";
	private static final String GROUP2_84_FILE = "src/main/resources/valuepack/pd/50001_100_84_Trigger_Output.xml";	
	private static final String GROUP2_85_FILE = "src/main/resources/valuepack/pd/50001_100_85_Trigger_Output.xml"; 
	private static final String GROUP2_60_FILE = "src/main/resources/valuepack/pd/50001_100_1_60_Output.xml"; 
	private static final String GROPU2_8_FILE = "src/main/resources/valuepack/pd/50001_100_1_8_Output.xml";
	private static final String GROPU2_1_FILE = "src/main/resources/valuepack/pd/50001_100_1_1_Output.xml";
	private static final String GROPU2_1_SUB_FILE = "src/main/resources/valuepack/pd/50001_100_1_1_Sub_Output.xml";
	/**  
	 * @throws java.lang.Exception  
	 */     
	@BeforeClass  
	public static void setUpBeforeClass() throws Exception {
		log.info(Constants.TEST_START.val() + MyProblemTest.class.getName());
	}
	@BeforeClass
	public static void init() {
		log.info("********* We are starting...");
		tmpDir = new TmpDir("valuepack/pd/topologyDataload");   
	}

	@Before
	public void setUp() throws Exception {
		log.info("******** we are wicked starting " + Constants.TEST_START.val() + MyProblemTest.class.getName());
	
		Loader loader = new Loader(IpagAdtranTopoAccess.getGraphDB(), tmpDir.tmpCsvPath());
		Report report = loader.loadAll();

		log.info(report.toString());			
	}
	@AfterClass
	public static void cleanup() {
		tmpDir.cleanup();
	}
	
	 
	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		log.info(Constants.TEST_END.val() + MyProblemTest.class.getName()
				+ Constants.GROUP_ALT1_SEPARATOR.val());
	}

	// Way to run tests via ANT Junit
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(MyProblemTest.class);
	}

	@Test
	@DirtiesContext
	public void testGeneratedPbAlarm() throws Exception {
		/*
		 * Initialize variables and Enable engine internal logs
		 */
		log.info("testGeneratedPbAlarm statring**************");
		initTest(SCENARIO_BEAN_NAME, BMK_PATH);
		getScenario().setTestOnly(true);

//		Map<String,String> keyValues = new HashMap<String,String>();
//		keyValues.put("directiveName", "SET");
//		keyValues.put("entityName", "operation_context .uca_network alarm_object 123456");
//
//		ActionListener actionListener = new ActionListener(keyValues);
//
//		getScenario().getSession().addEventListener(actionListener);

		/*
		 * Send alarms
		 */
		//Thread.sleep(5 * SECOND);   
//		getProducer().sendAlarms(SUB_ALARM_FILE); 
//		Thread.sleep(2 * SECOND);   
//		getProducer().sendAlarms(PB_ALARM_FILE);           
//		Thread.sleep(2 * SECOND);
//		getProducer().sendAlarms(PB_TR_ALARM_FILE);       
//		Thread.sleep(2 * SECOND);      
//		log.info("sent two alarms");
		DisplayResult displayResult = new DisplayResult(getScenario(),
				getAlarmsFromWorkingMemory()); 
//		log.info("display result : " + displayResult.displayResult(log.isTraceEnabled(),
//				log.isDebugEnabled()).toString());  
		getProducer().sendAlarms(GROPU2_1_SUB_FILE);
		getProducer().sendAlarms(GROPU2_1_FILE);            
		Thread.sleep(1 * SECOND);  
		getProducer().sendAlarms(GROPU2_8_FILE);   
		Thread.sleep(2 * SECOND);      
		getProducer().sendAlarms(GROPU2_7_FILE);   
		Thread.sleep(2 * SECOND);    
		getProducer().sendAlarms(GROUP2_84_FILE);         
		Thread.sleep(2 * SECOND);     
		getProducer().sendAlarms(GROPU2_10_FILE);      
		Thread.sleep(2 * SECOND);     
		getProducer().sendAlarms(GROUP2_60_FILE);     
		Thread.sleep(2 * SECOND);  
		displayResult = new DisplayResult(getScenario(),
				getAlarmsFromWorkingMemory());     
//		log.info(displayResult.displayResult(true,  
//				true).toString());
		/*   
		 * Waiting for the last Alarm that should be updated by the rule itself
		 */
//		waitingForActionInsertion(actionListener, 1 * SECOND, 10 * SECOND);
		Thread.sleep(5 * SECOND);  
 
		/*  
		 * Disable Rule Logger to close the file used to compare engine historical
		 * events
		 */
		closeRuleLogFiles(getScenario());  
//		displayResult = new DisplayResult(getScenario(),
//				getAlarmsFromWorkingMemory());
//		log.info(displayResult.displayResult(true,
//				true).toString());

		//
//		if (log.isDebugEnabled()) {
//			getScenario().getSession().dump();
//		}
//
//		/*
//		 * Checking Actions Number
//		 */  
//		Collection<Action> actions = getActionsFromWorkingMemory();
////		assertEquals(EXPECTED_NB_ACTIONS, actions.size());
//		
//		assertTrue(EXPECTED_NB_ACTIONS >= actions.size());
//
//		/*
//		 * Checking Alarm Number
//		 */
//		Collection<Alarm> alarms = getAlarmsFromWorkingMemory();
//		assertEquals(EXPECTED_NB_ALARMS, alarms.size());
//
//		/*
//		 * Other kind of assert, using the lifecycleAnalysis hash map
//		 */
//		Alarm subalarm1 = getAlarm("operation_context .uca_network alarm_object 278");
//		assertTrue(subalarm1.getCustomFieldValue("pb").equals(
//				Qualifier.SubAlarm.toString()));
//
////		Alarm subalarm2 = getAlarm("operation_context .uca_network alarm_object 279");
////		assertTrue(subalarm2.getCustomFieldValue("pb").equals(
////				Qualifier.SubAlarm.toString()));
//
//		Alarm trigger = getAlarm("operation_context .uca_network alarm_object 281");
//		assertTrue(trigger.getCustomFieldValue("pb").equals(
//				Qualifier.SubAlarm.toString()));
//		
//		Alarm pbAlarm = getAlarm("operation_context .uca_network alarm_object 123456");
//		assertTrue(pbAlarm.getCustomFieldValue("pb").equals(
//				Qualifier.ProblemAlarm.toString()));
//
//		/*
//		 * Checking Group Number
//		 */
//		Collection<Group> groups = getGroupsFromWorkingMemory();
//		assertEquals(EXPECTED_NB_GROUP, groups.size());

	}

}
