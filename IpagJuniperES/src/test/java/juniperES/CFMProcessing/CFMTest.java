package juniperES.CFMProcessing;

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

import com.att.gfp.data.ipag.JunipertopoModel.IpagJuniperESTopoAccess;
import com.hp.uca.common.misc.Constants;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.testmaterial.AbstractJunitIntegrationTest;



@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class CFMTest extends AbstractJunitIntegrationTest {

	private static Logger log = LoggerFactory.getLogger(CFMTest.class);
	private static final String SCENARIO_BEAN_NAME = "JuniperES.CFMProcessing";
	private static final String ALARM_VRF_UPDATE_FILE = "src/test/resources//juniperES/VRFProcessing/UpdateLinkDownVRFAlarm.xml";
	//private static final String CLEAR_ALARM_VRF_UPDATE_FILE = "src/test/resources//juniperES/VRFProcessing/UpdateLinkDownVRFClearAlarm.xml";
	//private static final String ALARM_NO_VRF_UPDATE_FILE = "src/test/resources//juniperES/VRFProcessing/DontUpdateLinkDownVRFAlarm.xml";
	
	private static TmpDir tmpDir = null;
	

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		log.info(Constants.TEST_START.val() + CFMTest.class.getName());
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		log.info(Constants.TEST_END.val() + CFMTest.class.getName()
				+ Constants.GROUP_ALT1_SEPARATOR.val());
	}


	@BeforeClass
	public static void init() {
		tmpDir = new TmpDir("valuepack/NTDTicket/topologyDataload");
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
		log.info(Constants.TEST_START.val() + CFMTest.class.getName());

		tmpDir = new TmpDir("valuepack/NTDTicket/topologyDataload");
		Loader loader = new Loader(IpagJuniperESTopoAccess.getGraphDB(),
				tmpDir.tmpCsvPath());
		Report report = loader.loadAll();

		log.info(report.toString());
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		log.info(Constants.TEST_END.val() + CFMTest.class.getName()
				+ Constants.GROUP_ALT1_SEPARATOR.val());
		tmpDir.cleanup();
	}

	/**
	 * Way to run tests via ANT Junit
	 * 
	 * @return
	 */
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(CFMTest.class);
	}


    @Test
	@DirtiesContext()
	public void CFMTest1() throws Exception {
		LogHelper.enter(log, "CFMTest1() :  Starting test...");
		
		
		/*
		 * Initialize variables and Enable engine internal logs
		 */
		initTest(SCENARIO_BEAN_NAME, BMK_PATH);
	
		setAlarmListener(createAndAssignAlarmListener("100",getScenario()));
		
		/*
		 * Send alarms
		 */
		getProducer().sendAlarms(ALARM_VRF_UPDATE_FILE, 10*MS);
		
		Thread.sleep(1*SECOND);
		
		getProducer().sendAlarms(ALARM_VRF_UPDATE_FILE, 10*MS);	

		LogHelper.method(log, "Sent alarms...");

		Thread.sleep(10*SECOND);
		

		/*
		 * Waiting for the alarm update that is performed by the rule execution
		 */
//		waitingForAlarmInsertion(getAlarmListener(), 1 * SECOND, 10*SECOND);
		
		//setAlarmListener(createAndAssignAlarmListener("200",getScenario()));
		
		//Thread.sleep(65*SECOND);    

		
//		/*
//		 * Check test result by comparing the historical engine events with a
//		 * benchmark
//		 */
//		checkTestResult(getLogRuleFileName(),getLogRuleFileNameBmk());

		LogHelper.exit(log, "CFMTest1)");
	}
    
    
}
