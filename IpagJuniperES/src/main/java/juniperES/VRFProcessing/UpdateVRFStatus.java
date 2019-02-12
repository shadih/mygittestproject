package juniperES.VRFProcessing;

import java.math.BigDecimal;
import java.util.Map;

//import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.Node;
import org.apache.log4j.Logger;

//import com.att.gfp.data.ipag.JunipertopoModel.IpagJuniperESTopoAccess;
import com.hp.uca.expert.topology.TopoAccess;

public class UpdateVRFStatus extends TopoAccess
{
	public static final String LINK_DOWN = "juniper_link_down";
	public static final String LINK_DOWN_TIME = "juniper_link_down_time";
	//private GraphDatabaseService db;
	private ExecutionEngine engine;
	private ExecutionResult result;
	private static final Logger log = Logger.getLogger ( UpdateVRFStatus.class );

	private static UpdateVRFStatus topologyAccessor = null;

	
	public UpdateVRFStatus()
	{
		super();
		engine = new ExecutionEngine(getGraphDB());
	}

	public static synchronized UpdateVRFStatus getInstance() {
		if (topologyAccessor == null) {
			topologyAccessor = new UpdateVRFStatus();
		}
		return topologyAccessor;
	}

	public void updateJuniperLinkDownStatusByPport(String pportInstance, String whereClause, String status)
	{
		log.trace("updateJuniperLinkDownStatusByPport() Enter : ");

		String returnClause = "evcNode.key";

		StringBuilder qbld = new StringBuilder("START pport=node:PE_PPort(key=\""+pportInstance+"\") match (pport)-[:Composed_Of]->(lport)-[:Composed_Of]->(evcNode) ");
		if (whereClause != null)
			qbld.append(" where " + whereClause);
		qbld.append(" return " + returnClause);

		log.trace("##### CYPHER QUERY: " + qbld.toString());

 		result = engine.execute(qbld.toString());

		String evcnodeInstance = null;
		String evcInstance = null;
		for ( Map<String, Object> row : result )
		{
			evcnodeInstance = (String)row.get(returnClause); 
			evcInstance = evcnodeInstance.split("/")[1];
			updateJuniperLinkDownStatus(evcInstance, status);
		}
		
		log.trace("updateJuniperLinkDownStatusByPport() Exit : ");

	}

	/*public void updateCienaDyingGaspStatusByDevice(String deviceInstance, String whereClause, String status)
	{
		String returnClause = "evcNode.key";

		StringBuilder qbld = new StringBuilder("START device=node:CE_Device(key=\""+deviceInstance+"\") match (device)-[:Composed_Of]->(pport)-[:Composed_Of]->(lport)-[:Composed_Of]->(evcNode) ");

		if (whereClause != null)
			qbld.append(" where " + whereClause);
		qbld.append(" return " + returnClause);

                System.out.println("Query ="+ qbld.toString());
		result = engine.execute(qbld.toString());

		String evcnodeInstance = null;
		String evcInstance = null;
		for ( Map<String, Object> row : result )
		{
			evcnodeInstance = (String)row.get("evcNode.key"); 
			evcInstance = evcnodeInstance.split("/")[1];
			updateCienaDyingGaspStatus(evcInstance, status);
		}
	}

	private void updateCienaDyingGaspStatus(String evcInstance, String status)
	{
		StringBuilder qbld = new StringBuilder("START evc=node:EVC(key=\""+evcInstance+"\") set evc.ciena_dying_gasp=\""+status+"\",evc.ciena_dying_gasp_time=\""+System.currentTimeMillis()/1000+"\" return evc");

                System.out.println("Query ="+ qbld.toString());
		engine.execute(qbld.toString());
	}*/


	private void updateJuniperLinkDownStatus(String evcInstance, String status)
	{
		log.trace("updateJuniperLinkDownStatus() Enter : ");

		StringBuilder qbld = new StringBuilder("START evc=node:EVC(key=\""+evcInstance+"\") return evc");

		log.trace("##### CYPHER QUERY: " + qbld.toString());

		//engine.execute(qbld.toString());
		result = engine.execute(qbld.toString());
		
		Node nd = null;
		for ( Map<String, Object> row : result )
		{
			double currentT = 0;
			
			nd = (Node) row.get("evc");
			log.trace("EVC fetched: " + nd.getProperty("key"));
			setProp(nd, LINK_DOWN, status);

			if(status.equals("true")) {
				currentT = System.currentTimeMillis()/1000;
				setProp(nd, LINK_DOWN_TIME, new BigDecimal(currentT)+"");
			}
		}
		
		log.trace("updateJuniperLinkDownStatus() Exit : ");

	}

	private void setProp(Node node, String fldnm, String value)
	{
		log.trace("setProp() Enter : ");

		Transaction tx = getGraphDB().beginTx();


		try
	        {
			log.trace(" Setting property: " + fldnm + " to " + value);
	        	node.setProperty(fldnm, value);
	        	log.trace(" Set property successful");
	        	tx.success();
	        }
	        finally
	        {
	        	log.trace(" Set property finished");
	           	tx.finish();
	        }

		log.trace("setProp() Exit : ");

	}
}
