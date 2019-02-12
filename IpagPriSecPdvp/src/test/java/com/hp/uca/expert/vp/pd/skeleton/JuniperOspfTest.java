package com.hp.uca.expert.vp.pd.skeleton;

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

import com.hp.uca.common.misc.Constants;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.testmaterial.AbstractJunitIntegrationTest;

import com.hp.uca.expert.topology.TopoAccess;



@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class JuniperOspfTest extends AbstractJunitIntegrationTest {

	private static Logger log = LoggerFactory.getLogger(JuniperOspfTest.class);
	private static final String SCENARIO_BEAN_NAME = "com.hp.uca.expert.vp.pd.ProblemDetection";
	//private static final String ALARM_FILE_OSPF = 
	//	"src/test/resources//valuepack/pd/OSFAlarms/Alarms_juniperOspf_1.xml";
	//private static final String ALARM_FILE_OSPF_CLEAR = 
	//	"src/test/resources//valuepack/pd/OSFAlarms/Alarms_juniperOspf_1_clear.xml";
	private static final String ALARM_FILE_LAG_LINKDOWN = 
		"src/test/resources//valuepack/pd/OSFAlarms/Alarms_juniperldFromServer.xml";
	private static final String ALARM_FILE_LDP = 
		"src/test/resources//valuepack/pd/OSFAlarms/Alarms_juniperldpFromServer.xml";
	//private static final String ALARM_FILE_SUBINTF_LINKDOWN = 
	//	"src/test/resources//valuepack/pd/OSFAlarms/Alarms_juniperOspf_SubIntflinkDown.xml";
	private static TmpDir tmpDir = null;
	

	@BeforeClass
	public static void init() {
		tmpDir = new TmpDir("valuepack/TestPriSec/topologyDataload");
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
		log.info(Constants.TEST_START.val() + JuniperOspfTest.class.getName());
	
		Loader loader = new Loader(TopoAccess.getGraphDB(), tmpDir.tmpCsvPath());
		Report report = loader.loadAll();

		//log.info(report.toString());
	}


	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		log.info(Constants.TEST_END.val() + JuniperOspfTest.class.getName()
				+ Constants.GROUP_ALT1_SEPARATOR.val());
	}

	// Way to run tests via ANT Junit
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(JuniperOspfTest.class);
	}
	
    @Test
	@DirtiesContext()
	public void testLinkDown() throws Exception {
		LogHelper.enter(log, "testLinkDown()");
		
		
		/*
		 * Initialize variables and Enable engine internal logs
		 */
		initTest(SCENARIO_BEAN_NAME, BMK_PATH);
	
		//setAlarmListener(createAndAssignAlarmListener("100",getScenario()));
		
		/*
		 * Send alarms
		 */

		log.info("Sending ALARM_FILE_OSPF");
		getProducer().sendAlarms(ALARM_FILE_LAG_LINKDOWN);
		Thread.sleep(10 * SECOND);
		log.info("Sending ALARM_FILE_LDP");
		getProducer().sendAlarms(ALARM_FILE_LDP);
		Thread.sleep(180 * SECOND);

		getScenario().getSession().dump();
		/*
		 * Waiting for the alarm update that is performed by the rule execution
		 */
		
		/***
		waitingForAlarmInsertion(getAlarmListener(), 1 * SECOND, 10*SECOND);
		***/
		
		/*
		 * Check results : validate that the link state between port2 and port 3 state is Down
		 */
		
		/*IndexManager indexMgr = ExtendedTopoAccess.getGraphDB().index();
		Index<Node> nodeIndex = indexMgr.forNodes(ExtendedTopoAccess.PORT_INDEX);
		IndexHits<Node> hits = nodeIndex.get( ExtendedTopoAccess.PORT_UNIQUEID, "topo-example-box2port2");
		Node port = hits.getSingle();
		Iterable<Relationship> relations = port.getRelationships(ExtendedTopoAccess.PortRelationshipType.LINK);
		Relationship link = null;
		for (Relationship relation : relations) {
				link = relation;
				assertEquals("Link "+link.getProperty(ExtendedTopoAccess.LINK_NAME)+ "state ", ExtendedTopoAccess.LINK_STATE_DOWN, link.getProperty(ExtendedTopoAccess.LINK_STATE));
				break;  // only one link per port
		}
		*/
		//setAlarmListener(createAndAssignAlarmListener("200",getScenario()));
		
		/*
		 * Send alarms for testing link up
		 */
		//getProducer().sendAlarms(ALARMS_LINKUP_FILE);
		
		/*
		 * Waiting for the alarm update that is performed by the rule execution
		 */
		//waitingForAlarmInsertion(getAlarmListener(), 1 * SECOND, 10*SECOND);
		
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
