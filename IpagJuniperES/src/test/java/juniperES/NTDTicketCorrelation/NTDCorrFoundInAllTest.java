package juniperES.NTDTicketCorrelation;

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
public class NTDCorrFoundInAllTest extends AbstractJunitIntegrationTest {

	private static Logger log = LoggerFactory.getLogger(NTDCorrFoundInAllTest.class);
	private static final String SCENARIO_BEAN_NAME = "JuniperES.NTDTicketCorrelation";
	private static final String ALARMS_CLFI3_FILE = "src/test/resources//juniperES/NTDTicketCorrelation/FoundInCLFI3Alarm.xml";
	private static final String ALARMS_CLFI2PLUS_FILE = "src/test/resources//juniperES/NTDTicketCorrelation/FoundInCLFI2PlusAlarm.xml";
	private static final String ALARMS_CLFI2_FILE = "src/test/resources//juniperES/NTDTicketCorrelation/FoundInCLFI2Alarm.xml";
	private static final String ALARMS_CLFI_FILE = "src/test/resources//juniperES/NTDTicketCorrelation/FoundInCLFIAlarm.xml";
	private static final String ALARMS_NOTFOUND_FILE = "src/test/resources//juniperES/NTDTicketCorrelation/NotFoundInCLFIAlarm.xml";

	private static TmpDir tmpDir = null;
	

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		log.info(Constants.TEST_START.val() + NTDCorrFoundInAllTest.class.getName());
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		log.info(Constants.TEST_END.val() + NTDCorrFoundInAllTest.class.getName()
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
		log.info(Constants.TEST_START.val() + NTDCorrFoundInAllTest.class.getName());

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
		log.info(Constants.TEST_END.val() + NTDCorrFoundInAllTest.class.getName()
				+ Constants.GROUP_ALT1_SEPARATOR.val());
		tmpDir.cleanup();
	}

	/**
	 * Way to run tests via ANT Junit
	 * 
	 * @return
	 */
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(NTDCorrFoundInAllTest.class);
	}


    @Test
	@DirtiesContext()
	public void NTDCorrTestOne() throws Exception {
		LogHelper.enter(log, "NTDCorrTest() :  Starting test...");
		
		
		/*
		 * Initialize variables and Enable engine internal logs
		 */
		initTest(SCENARIO_BEAN_NAME, BMK_PATH);
	
		setAlarmListener(createAndAssignAlarmListener("100",getScenario()));
		
		/*
		 * Send alarms
		 */
		// 2.123.80.143/2/0/3       101/P101/GE10/DTRTMIBL0AW/DTRTMIBL0BW
		getProducer().sendAlarms(ALARMS_CLFI3_FILE, 10*MS);
		Thread.sleep(65*SECOND);    
		
		//12.123.80.143/2/0/1       Q101/GE10/DTRTMIBL0AW/DTRTMIBL0BW 
		getProducer().sendAlarms(ALARMS_CLFI2PLUS_FILE, 10*MS);
		Thread.sleep(65*SECOND);    
		
		//12.123.80.143/2/0/2       R101/GE10/DTRTMIBL0AW/DTRTMIBL0BW      
		getProducer().sendAlarms(ALARMS_CLFI2_FILE, 10*MS);
		Thread.sleep(65*SECOND);    
		
		//12.123.80.143/2/0/0       S101/GE10/DTRTMIBL0AW/DTRTMIBL0BW
		getProducer().sendAlarms(ALARMS_CLFI_FILE, 10*MS);
		Thread.sleep(65*SECOND);    
		
		//150.0.0.44/5/0/1          102/GE1N/DYBHFLMAH02/DYBHFLMAH57
		// this is the not found one so it waits for 3 min (+10sec)
		getProducer().sendAlarms(ALARMS_NOTFOUND_FILE, 10*MS);
		Thread.sleep(190*SECOND);    
		
		LogHelper.method(log, "Sent alarms...");
		
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

		LogHelper.exit(log, "NTDCorrTest)");
	}
    
    
}
