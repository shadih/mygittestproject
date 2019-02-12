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
public class passThruAlarmTest extends AbstractJunitIntegrationTest {

	private static Logger log = LoggerFactory.getLogger(passThruAlarmTest.class);
	private static final String SCENARIO_BEAN_NAME = "ipagPreprocess.preprocess";
	
	private static final String ALARM1 = "src/test/resources//com/att/gfp/data/ipagPreprocess/preprocess/50001_100_58_19.xml";
	private static final String ALARM4 = "src/test/resources//com/att/gfp/data/ipagPreprocess/preprocess/50001_100_1_3.xml";
	private static final String ALARM5 = "src/test/resources//com/att/gfp/data/ipagPreprocess/preprocess/50001_100_37_17.xml";
	private static final String ALARM2 = "src/test/resources//com/att/gfp/data/ipagPreprocess/preprocess/50001_100_58_20.xml";
	private static final String ALARM3 = "src/test/resources//com/att/gfp/data/ipagPreprocess/preprocess/50001_100_58_21.xml";
	private static final String ALARM6 = "src/test/resources//com/att/gfp/data/ipagPreprocess/preprocess/50003_100_21_4.xml";
	private static final String ALARM7 = "src/test/resources//com/att/gfp/data/ipagPreprocess/preprocess/Active50001_100_21.xml";
	private static final String ALARM8 = "src/test/resources//com/att/gfp/data/ipagPreprocess/preprocess/Clear50001_100_21.xml";
	private static final String ALARM9 = "src/test/resources//com/att/gfp/data/ipagPreprocess/preprocess/50001_100_1_7_Output.xml";
	private static final String ALARM10 = "src/test/resources//com/att/gfp/data/ipagPreprocess/preprocess/Set_50001_100_21.xml";
	private static final String ALARM11 = "src/test/resources//com/att/gfp/data/ipagPreprocess/preprocess/Clear_Set_50001_100_21.xml";
	private static final String ALARM12 = "src/test/resources//com/att/gfp/data/ipagPreprocess/preprocess/AD26373.xml";
	private static final String ALARM13 = "src/test/resources//com/att/gfp/data/ipagPreprocess/preprocess/AD26387.xml";			
	private static final String ALARM14 = "src/test/resources//com/att/gfp/data/ipagPreprocess/preprocess/AD26161.xml";
	private static final String ALARM15 = "src/test/resources//com/att/gfp/data/ipagPreprocess/preprocess/AD26204.xml";
	private static final String ALARM16 = "src/test/resources//com/att/gfp/data/ipagPreprocess/preprocess/AD25658.xml"; 
	private static final String ALARM17 = "src/test/resources//com/att/gfp/data/ipagPreprocess/preprocess/AD25793.xml";
	//private static final String ALARM2 = "src/test/resources//com/att/gfp/data/ipagPreprocess/preprocess/.xml";
	//private static final String ALARM3 = "src/test/resources//com/att/gfp/data/ipagPreprocess/preprocess/.xml";
	
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
		log.info(Constants.TEST_START.val() + passThruAlarmTest.class.getName());
	
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
		log.info(Constants.TEST_END.val() + passThruAlarmTest.class.getName()
				+ Constants.GROUP_ALT1_SEPARATOR.val());
	}

	// Way to run tests via ANT Junit
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(passThruAlarmTest.class);
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
//		alarmListener = createAndAssignAlarmListener("AD11194",
//				getScenario());

		/*
		 * Send alarms
		 */
		
		log.info("************ SEND ALARM");
		
		getProducer().sendAlarms(ALARM12);
		Thread.sleep(2*SECOND);     
		getProducer().sendAlarms(ALARM13); 
	//	getProducer().sendAlarms(ALARM3);     
		Thread.sleep(2*SECOND);     
		Thread.sleep(60*SECOND);     
		getProducer().sendAlarms(ALARM14);
		Thread.sleep(2*SECOND);     
		getProducer().sendAlarms(ALARM15);   
	//	getProducer().sendAlarms(ALARM3);     
		Thread.sleep(2*SECOND);       
		Thread.sleep(60*SECOND);     
		getProducer().sendAlarms(ALARM16);
		Thread.sleep(2*SECOND);     
		getProducer().sendAlarms(ALARM17);   
	//	getProducer().sendAlarms(ALARM3);     
		Thread.sleep(2*SECOND);     
//		Thread.sleep(60*SECOND);     
//		getProducer().sendAlarms(ALARM8);  
//		Thread.sleep(5*SECOND);    
		//getProducer().sendAlarms(ALARM2);
//		Thread.sleep(5*SECOND);    
		//getProducer().sendAlarms(ALARM2); 
		//getProducer().sendAlarms(ALARM3); 
		/*
		 * Waiting for the last Alarm that should be updated by the rule itself
		 */
	//	waitingForAlarmRetract(alarmListener, 1 * SECOND, 60 * SECOND);

		
		Thread.sleep(20*SECOND);  
  
		LogHelper.exit(log, "preprocessClearAlarmTest()");
	} 
    
    
}
