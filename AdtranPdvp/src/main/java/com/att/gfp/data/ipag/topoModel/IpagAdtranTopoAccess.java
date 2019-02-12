package com.att.gfp.data.ipag.topoModel;


import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.helpers.collection.IteratorUtil;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.topology.TopoAccess;
import com.hp.uca.expert.vp.pd.problem.AdtranTrapsCorrelator;

/* topology access for ipag */
public class IpagAdtranTopoAccess extends TopoAccess 
{
	private static ExecutionEngine engine= GFPUtil.getCypherEngine();
	
	private static final String CE = "CE";
	private static final String PE = "PE";
	public static final String CE_PPORT_INDEX = "CE_PPort";
	public static final String PE_PPORT_INDEX = "PE_PPort";
	public static final String CE_LPORT_INDEX = "CE_LPort";
	public static final String PE_LPORT_INDEX = "PE_LPort";
	public static final String CE_CARD_INDEX = "CE_Card";
	public static final String PE_CARD_INDEX = "PE_Card";
	public static final String CE_SLOT_INDEX = "CE_Slot";
	public static final String PE_SLOT_INDEX = "PE_Slot";

	public static final String KEY_UNIQUEID = "key";
	public static final String PE_DEVICE_INDEX = "PE_Device";
	public static final String CE_DEVICE_INDEX = "CE_Device";
	public static final String DEVICE_UNIQUEID = "key";
	public static final String ADT_LINK_DOWN = "adt_link_down";
	public static final String ADT_LINK_DOWN_TIME = "adt_link_down_time";
	public static final String ADT_SHDSL_DOWN = "adt-shdsl-down"; 
	public static final String ADT_SHDSL_DOWN_TIME = "adt-shdsl-down-time";
	
			
	private static Logger log = LoggerFactory.getLogger(IpagAdtranTopoAccess.class);
	private static GraphDatabaseService override = null;
	private static IpagAdtranTopoAccess topologyAccessor = null;
	
	private IpagAdtranTopoAccess() {
		super();
	}
	
	public static synchronized IpagAdtranTopoAccess getInstance() {
		if (topologyAccessor==null) {
			topologyAccessor =  new IpagAdtranTopoAccess();
		}
		if(engine == null)
			engine = GFPUtil.getCypherEngine(); 
		return topologyAccessor;
	}  

	private static GraphDatabaseService getDb()
	{
		if ( override != null ) return override;
		return TopoAccess.getGraphDB ();
	}

	public static void testAccessSetDb ( GraphDatabaseService db )
	{
		override = db;
	}

	/**
	 * Returns the clfi of pport, associated with an LPORT instance
	 * @param pportInstance
	 * @return 
	 */ 
	public String fetchClfiOfPportFromLportInstance(String lportInstance) {
		
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "fetchPportFromLport()");
		}
		log.trace("##### fetchClfiOfPportFromLportInstance pportInstance " + lportInstance);
		String query = "START lport=node:CE_LPort(key=\""+lportInstance+"\") match (lport)-[:Composed_Of]->(PPort)-[:Composed_Of]->(LPort) " +
				" return PPort.clfi";
		log.trace("##### fetchClfiOfPportFromLportInstance CYPHER QUERY: " + query);
		ExecutionResult result = engine.execute(query);
		int count=0;
		String clfi = null;
		for ( Map<String, Object> row : result ) {			
			clfi = (String)row.get("PPort.clfi");   
			count++;        
			break;  
		}
		log.trace("fetchClfiOfPportFromLportInstance ####count:"+ count + " clfi: " + clfi + "####");
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "fetchPportFromLport()");
		}
 
		return clfi; 
	}
	
	public void updateAdtLinkDownStatusByPport(String pportInstance, String status, boolean isCurrTimeStamp)
	{
		String query = "START pport=node:CE_PPort(key=\""+pportInstance+"\") match (pport)-[:Composed_Of]->(lport)-[:Composed_Of]->(evcNode) return evcNode.key";

       log.trace("Query ="+ query.toString());  
		ExecutionResult result = engine.execute(query);  
		String evcnodeInstance = null; 
		String evcInstance = null;
		for ( Map<String, Object> row : result )
		{
			evcnodeInstance = (String)row.get("evcNode.key"); 
			evcInstance = evcnodeInstance.split("/")[1];
			updateAdtLinkDownStatus(evcInstance, status, isCurrTimeStamp); 
		}
	}
	private void updateAdtLinkDownStatus(String evcInstance, String status, boolean isCurrTimeStamp)
	{
		String query = "START evc=node:EVC(key=\""+evcInstance+"\") return evc";
        log.trace("Query ="+ query.toString());
		ExecutionResult result = engine.execute(query);
		
		Node nd = null;
		for ( Map<String, Object> row : result )
		{
			nd = (Node) row.get("evc");
			setCustomerProp(nd, ADT_SHDSL_DOWN, status);
			if(isCurrTimeStamp) { 
				double currentT = System.currentTimeMillis()/1000;
				setCustomerProp(nd, ADT_SHDSL_DOWN_TIME, new BigDecimal(currentT)+"");
			} else {
				setCustomerProp(nd, ADT_SHDSL_DOWN_TIME, 0 +""); 
			}
		}
	}
  
	private void setCustomerProp(Node node, String fldnm, String value)
	{
		Transaction tx = IpagAdtranTopoAccess.getDb().beginTx();
		try
	        {
	        	node.setProperty(fldnm, value);
	        	tx.success();  
	        }
	        finally
	        {
	           	tx.finish();
	        }  
	}
	
	
}
