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
public class DuplicateGroupTest extends AbstractJunitIntegrationTest {

	private static Logger log = LoggerFactory.getLogger(DuplicateGroupTest.class);
	private static final String SCENARIO_BEAN_NAME = "com.hp.uca.expert.vp.pd.ProblemDetection";
	
	private static final String ALARM = "src/test/resources/com/hp/uca/expert/vp/pd/skeleton/LinkDownAlarm.xml";
	private static final String CLEAR = "src/test/resources/com/hp/uca/expert/vp/pd/skeleton/LinkDownClearAlarm.xml";

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
		log.info(Constants.TEST_START.val() + DuplicateGroupTest.class.getName());
	
//		Loader loader = new Loader(TopoAccess.getGraphDB(), tmpDir.tmpCsvPath());
//		Report report = loader.loadAll();

		//log.info(report.toString());
	}


	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		log.info(Constants.TEST_END.val() + DuplicateGroupTest.class.getName()
				+ Constants.GROUP_ALT1_SEPARATOR.val());
	}

	// Way to run tests via ANT Junit
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(DuplicateGroupTest.class);
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
		
		
		getProducer().sendAlarms(ALARM);
		Thread.sleep(1 * SECOND);
		getProducer().sendAlarms(CLEAR);
		Thread.sleep(1 * SECOND);
		getProducer().sendAlarms(ALARM);
		Thread.sleep(1 * SECOND);
		getProducer().sendAlarms(CLEAR);
		getProducer().sendAlarms(ALARM);
		Thread.sleep(1 * SECOND);
		getProducer().sendAlarms(CLEAR);
		getProducer().sendAlarms(ALARM);
		Thread.sleep(1 * SECOND);
		getProducer().sendAlarms(CLEAR);
		getProducer().sendAlarms(ALARM);
		Thread.sleep(1 * SECOND);
		getProducer().sendAlarms(CLEAR);
		getProducer().sendAlarms(ALARM);
		Thread.sleep(1 * SECOND);
		getProducer().sendAlarms(CLEAR);
		getProducer().sendAlarms(ALARM);
		Thread.sleep(1 * SECOND);
		getProducer().sendAlarms(CLEAR);
		getProducer().sendAlarms(ALARM);
		Thread.sleep(1 * SECOND);
		getProducer().sendAlarms(CLEAR);
		getProducer().sendAlarms(ALARM);
		Thread.sleep(1 * SECOND);
		getProducer().sendAlarms(CLEAR);
		getProducer().sendAlarms(ALARM);
		Thread.sleep(1 * SECOND);
		getProducer().sendAlarms(CLEAR);
		getProducer().sendAlarms(ALARM);
		Thread.sleep(1 * SECOND);
		getProducer().sendAlarms(CLEAR);
		getProducer().sendAlarms(ALARM);
		Thread.sleep(1 * SECOND);
		getProducer().sendAlarms(CLEAR);
		getProducer().sendAlarms(ALARM);
		Thread.sleep(1 * SECOND);
//		getProducer().sendAlarms(CLEAR);
//		getProducer().sendAlarms(ALARM);
//		Thread.sleep(1 * SECOND);
//		getProducer().sendAlarms(CLEAR);
//		getProducer().sendAlarms(ALARM);
//		Thread.sleep(1 * SECOND);
//		getProducer().sendAlarms(CLEAR);
//		getProducer().sendAlarms(ALARM);
//		Thread.sleep(1 * SECOND);
//		getProducer().sendAlarms(CLEAR);
//		getProducer().sendAlarms(ALARM);
//		Thread.sleep(1 * SECOND);
//		getProducer().sendAlarms(CLEAR);
//		getProducer().sendAlarms(ALARM);
//		Thread.sleep(1 * SECOND);
//		getProducer().sendAlarms(CLEAR);
//		getProducer().sendAlarms(ALARM);
//		Thread.sleep(1 * SECOND);
//		getProducer().sendAlarms(CLEAR);
//		getProducer().sendAlarms(ALARM);
//		Thread.sleep(1 * SECOND);
//		getProducer().sendAlarms(CLEAR);
//		getProducer().sendAlarms(ALARM);
//		Thread.sleep(1 * SECOND);
//		getProducer().sendAlarms(CLEAR);
//		getProducer().sendAlarms(ALARM);
//		Thread.sleep(1 * SECOND);
//		getProducer().sendAlarms(CLEAR);
//		getProducer().sendAlarms(ALARM);
//		Thread.sleep(1 * SECOND);
//		getProducer().sendAlarms(CLEAR);
//		getProducer().sendAlarms(ALARM);
//		Thread.sleep(1 * SECOND);
//		getProducer().sendAlarms(CLEAR);
//		getProducer().sendAlarms(ALARM);
//		Thread.sleep(1 * SECOND);
//		getProducer().sendAlarms(CLEAR);
//		getProducer().sendAlarms(ALARM);
		
		
		
		
	/*
		 /* Disable Rule Logger to close the file used to compare engine historical
		 * events
		 */
		closeRuleLogFiles(getScenario());


			getScenario().getSession().dump();


		log.info("#############     At the end of Testing...");

		LogHelper.exit(log, "testLinkDown()");
	}
    
    
}
