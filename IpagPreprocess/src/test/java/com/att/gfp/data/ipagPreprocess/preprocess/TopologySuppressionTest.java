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
import com.hp.uca.expert.testmaterial.AlarmListener;





@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class TopologySuppressionTest extends AbstractJunitIntegrationTest {

	private static Logger log = LoggerFactory.getLogger(TopologySuppressionTest.class);
	private static final String SCENARIO_BEAN_NAME = "ipagPreprocess.preprocess";
	
	private static final String ALARM1 = "src/test/resources//com/att/gfp/data/ipagPreprocess/preprocess/TopologySuppressionPPort.xml";
	private static final String ALARM2 = "src/test/resources//com/att/gfp/data/ipagPreprocess/preprocess/TopologySuppressionTestDevice.xml";
	private static final String ALARM3 = "src/test/resources//com/att/gfp/data/ipagPreprocess/preprocess/TopologySuppressionTestATTUVERSE.xml";
	//private static final String IPAG_CLEARALARMTEST_CLEAR = "src/test/resources//com/att/gfp/data/ipagPreprocess/preprocess/clearAlarmTestClear.xml";
	//private static final String IPAG_DEVSEVSUPPRESS_ALARM = "src/test/resources//com/att/gfp/data/ipagPreprocess/preprocess/DeviceAndSeveritySuppressionAlarm.xml";
	
	private static TmpDir tmpDir = null;
	private AlarmListener alarmListener;

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
		log.info(Constants.TEST_START.val() + TopologySuppressionTest.class.getName());
	
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
		log.info(Constants.TEST_END.val() + TopologySuppressionTest.class.getName()
				+ Constants.GROUP_ALT1_SEPARATOR.val());
	}

	// Way to run tests via ANT Junit
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(TopologySuppressionTest.class);
	}
	
    @Test
	@DirtiesContext()
	public void basicTests() throws Exception {
		LogHelper.enter(log, "basicTests()");
		
		
		/*
		 * Initialize variables and Enable engine internal logs
		 */
		initTest(SCENARIO_BEAN_NAME, BMK_PATH);
	
		// alarm2
		//alarmListener = createAndAssignAlarmListener("PPORT-10.204.160.81/SM-A/1-.1.3.6.1.4.1.664.5.63/6/1006301/NA",
		//		getScenario());

		/*
		 * Send alarms
		 */
		log.info("************ SEND SUPPRESSED ALARM");
		getProducer().sendAlarms(ALARM1);
		

		getProducer().sendAlarms(ALARM2);
		getProducer().sendAlarms(ALARM3);
		Thread.sleep(20*SECOND); 
/*		log.info("************ SEND ALARM");

		getProducer().sendAlarms(IPAG_CLEARALARMTEST_ALARM);

		Thread.sleep(5*SECOND);
		
		log.info("************ SEND CLEAR ALARM");
		
		getProducer().sendAlarms(IPAG_CLEARALARMTEST_CLEAR);
	*/	
		/*
		 * Waiting for the last Alarm that should be updated by the rule itself
		 */
		//waitingForAlarmRetract(alarmListener, 1 * SECOND, 60 * SECOND);

		
	//	Thread.sleep(2*SECOND);

		LogHelper.exit(log, "preprocessClearAlarmTest()");
	}
    
    
}
