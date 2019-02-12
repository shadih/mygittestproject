package com.att.gfp.data.ipagPreprocess.preprocess;

import junit.framework.JUnit4TestAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.Relationship;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.neo4j.loader.csv.Loader;
import org.neo4j.loader.csv.Report;
import org.neo4j.loader.csv.utils.TmpDir;

import com.att.gfp.data.ipag.topoModel.IpagTopoAccess;
import com.hp.uca.common.misc.Constants;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.testmaterial.AbstractJunitIntegrationTest;



@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class preProcessTunableTest extends AbstractJunitIntegrationTest {

	private static Logger log = LoggerFactory.getLogger(preProcessTunableTest.class);
	private static final String SCENARIO_BEAN_NAME = "ipagPreprocess.preprocess";
	private static final String IPAG_TUNABLE_ALARM1 = "src/test/resources/com/att/gfp/data/ipagPreprocess/preprocess/tunableTestAlarms.xml";
	private static final String ALARM_ONE_IDENTIFIER = "PPORT-192.168.113.200/20/5-.1.3.111.2.802.1.1.8/6/1/NA";
	private static final String ALARM_TWO_IDENTIFIER = "PPORT-8.8.0.129/20/5-.1.3.111.2.802.1.1.8/512064/1/NA";
	private static final String ALARM_THREE_IDENTIFIER = "PPORT-8.8.0.191/20/5-.1.3.111.2.802.1.1.8/512064/1/NA";
	private static final String ALARM_FOUR_IDENTIFIER = "PPORT-8.8.8.8/512064/5-.1.3.111.2.802.1.1.8/512064/1/NA";
	private static final String ALARM_FIVE_IDENTIFIER = "DEVICE-10.144.0.147-.1.3.6.1.4.1.664.5.63/6/1006301/bgpL2vpn_VPWS:1000364_197594157";
	private static final String ALARM_SIX_IDENTIFIER = "DEVICE-10.204.9.95-.1.3.6.1.4.1.664.5.63/512064/1006301/bgpL2vpn_VPWS:1000364_197594157";
	private static final String ALARM_SEVEN_IDENTIFIER = "DEVICE-10.10.10.10-.1.3.6.1.4.1.664.5.63/512064/1006301/bgpL2vpn_VPWS:1000364_197594157";

	
	private static TmpDir tmpDir = null;
	

	@BeforeClass
	public static void init() {
		tmpDir = new TmpDir("valuepack/preprocess/topologyDataload");
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
		log.info(Constants.TEST_START.val() + preProcessTunableTest.class.getName());
	
		Loader loader = new Loader(IpagTopoAccess.getGraphDB(), tmpDir.tmpCsvPath());
		Report report = loader.loadAll();

		log.info(report.toString());
		log.info("directory : " + tmpDir.tmpCsvPath());
	}


	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		log.info(Constants.TEST_END.val() + preProcessTunableTest.class.getName()
				+ Constants.GROUP_ALT1_SEPARATOR.val());
	}

	// Way to run tests via ANT Junit
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(preProcessTunableTest.class);
	}
	
    @Test
	@DirtiesContext()
	public void testLinkDown() throws Exception {
		LogHelper.enter(log, "testLinkDown()");
		
		
		/*
		 * Initialize variables and Enable engine internal logs
		 */
		initTest(SCENARIO_BEAN_NAME, BMK_PATH);
	
		// Set the listeners for alarms to remain in the Working Memory		
		setAlarmListener(createAndAssignAlarmListener(ALARM_ONE_IDENTIFIER,getScenario()));
		setAlarmListener(createAndAssignAlarmListener(ALARM_TWO_IDENTIFIER,getScenario()));
		setAlarmListener(createAndAssignAlarmListener(ALARM_THREE_IDENTIFIER,getScenario()));
		setAlarmListener(createAndAssignAlarmListener(ALARM_FOUR_IDENTIFIER,getScenario()));
		setAlarmListener(createAndAssignAlarmListener(ALARM_FIVE_IDENTIFIER,getScenario()));
		setAlarmListener(createAndAssignAlarmListener(ALARM_SIX_IDENTIFIER,getScenario()));
		setAlarmListener(createAndAssignAlarmListener(ALARM_SEVEN_IDENTIFIER,getScenario()));
		
		// Send the preProcess Tunable Alarm
		getProducer().sendAlarms(IPAG_TUNABLE_ALARM1);
		
		/*
		 * Waiting for the alarm update that is performed by the rule execution
		 */
		waitingForAlarmInsertion(getAlarmListener(), 1 * SECOND, 10*SECOND);

		Thread.sleep(5*SECOND);
				
		/*
		 * Check results : validate that the link state between port2 and port 3 state is Up again
		 */
		//assertEquals("Link "+link.getProperty(ExtendedTopoAccess.LINK_NAME)+ "state ", ExtendedTopoAccess.LINK_STATE_UP, link.getProperty(ExtendedTopoAccess.LINK_STATE));

		
//		/*
//		 * Check test result by comparing the historical engine events with a
//		 * benchmark
//		 */
//		checkTestResult(getLogRuleFileName(),getLogRuleFileNameBmk());

		LogHelper.exit(log, "testLinkDown()");
	}
    
    
}
