package com.att.gfp.data.ipag.topoModel;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.Node;

import com.att.gfp.data.ipagAlarm.EnrichedJuniperAlarm;
import com.att.gfp.helper.GFPUtil;

public class NodeManager
{
	private static Logger log = LoggerFactory.getLogger(NodeManager.class);

	public static final String PE_DEVICE = "PE_Device";
	public static final String PE_PPORT = "PE_PPort";
	public static final String CE_DEVICE = "CE_Device";
	public static final String CE_PPORT = "CE_PPort";

	private static GraphDatabaseService db;
	private static ExecutionEngine engine;

	static
	{
		try {
			db = IpagJuniperLinkDownTopoAccess.getGraphDB();
			//engine = new ExecutionEngine(db);
			engine = GFPUtil.getCypherEngine();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public static void getPportSetMatchedPortLagID(String deviceInstance, String portLagID, 
			EnrichedJuniperAlarm alarm)
	{
		// 50003/100/1: PE
		String instance=deviceInstance;
		String indexName=PE_DEVICE;
		String returnName="locDev";
		String matchClause="(locDev)-[:Composed_Of]->(locSlot)-[:Composed_Of]->(locCard)-[:Composed_Of]->(locPPort)";
		String whereClause="locPPort.port_lag_id=\""+portLagID+"\"";
		String returnClause="locPPort.TDL_instance_name, locPPort.remote_device_type, locPPort.port_aid";
		boolean foundRemoteEmux = false;
		
		HashSet<Map<String, Object>> rset = queryTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		//HashSet<String>pset = new HashSet<String>();
		for ( Map<String, Object> rowp : rset )
		{
			alarm.setLagIdPport((String)rowp.get("locPPort.TDL_instance_name"));
			alarm.setLagPportAid((String)rowp.get("locPPort.port_aid"));
			
			// We need this information in later PriSec processing so we get it here so we
			// can save a query later...
			if(!foundRemoteEmux &&
					((String)rowp.get("locPPort.remote_device_type")).contains("EMUX")) {
				foundRemoteEmux =true;					
			}
		}
		
		// this is for later PriSec processing.   Using a custom field to carry this information
		// instead of an alarm object attribute (less changes needed)
		if(foundRemoteEmux)
			alarm.setCustomFieldValue("hasRemoteEmux", "true");	
	}

	// never return null
	public static void setRemotePportSetMatchedRemoteDeviceType(EnrichedJuniperAlarm enrichedJuniperAlarm)
	{
		String deviceInstance = enrichedJuniperAlarm.getOriginatingManagedEntity().split(" ")[1];

		// 50002/100/55: CE
		String instance=deviceInstance;
		String indexName=CE_DEVICE;
		String returnName="device";
		String matchClause="(device)-[:Composed_Of_PPort]->(pport)";
		//String whereClause="pport.remote_device_type=\"JUNIPER MX SERIES\"";
		String whereClause=null;
		String returnClause="pport.remote_pport_key, pport.remote_device_type";

		HashSet<Map<String, Object>> rset = queryTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		for ( Map<String, Object> rowp : rset )
		{
			if( !((String)rowp.get("pport.remote_device_type")).isEmpty() &&
					((String)rowp.get("pport.remote_device_type")).equals("JUNIPER MX SERIES"))
				enrichedJuniperAlarm.addRemotePport((String)rowp.get("pport.remote_pport_key"));
		}
	}

	// never return null
	public static void setPeerSet(EnrichedJuniperAlarm enrichedJuniperAlarm)
	{
		if (!"JUNIPER MX SERIES".equals(enrichedJuniperAlarm.getDeviceType()) || !"CIENA NTE".equals(enrichedJuniperAlarm.getRemoteDeviceType()))
			return;
		// getRemotePportKey() will return this alarms's instance if
		// its remote device type is "CIENA NTE" ==> no good.  use
		// getRemotePportInstanceName()

		String cienaPport = enrichedJuniperAlarm.getRemotePportInstanceName();
		if(cienaPport != null && !cienaPport.isEmpty()) {
			String cienaDeviceInstance = cienaPport.split("/")[0];

			String instance=cienaDeviceInstance;
			String indexName=CE_DEVICE;
			String returnName="device";
			String matchClause="(device)-[:Composed_Of_PPort]->(pport)";
			//String whereClause="(device.multi_nni=\"Y\") and (pport.key <> \""+cienaPport+"\") and ((pport.nmvlan <> \"\") or (pport.slavlan <> \"\") or (pport.remote_device_type=\"JUNIPER MX SERIES\"))";
			String whereClause="(pport.key <> \""+cienaPport+"\") ";
			String returnClause="device.multi_nni, pport.nmvlan, pport.slavlan";

			HashSet<Map<String, Object>> rset = queryTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
			for ( Map<String, Object> rowp : rset )
			{
				if(((String)rowp.get("device.multi_nni")).equals("Y")  &&
						(!((String)rowp.get("pport.nmvlan")).isEmpty()  ||
								!((String)rowp.get("pport.slavlan")).isEmpty() || 
								enrichedJuniperAlarm.getRemoteDeviceType().equals("JUNIPER MX SERIES")))
					enrichedJuniperAlarm.addPeer((String)rowp.get("pport.remote_pport_key"));
			}
		}
	}

	public static String queryRemotePportByPport(String pportInstance)
	{
		String instance=pportInstance;
		String indexName="PE_PPort";
		String returnName="pport";
		String matchClause=null;
		String whereClause=null;
		String returnClause="pport.remote_pport_key";

		Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		if (row.size() == 0)
			return null;
		return (String)row.get("pport.remote_pport_key"); 
	}
	
	public static Map<String, Object> queryOneRowTopoAttributes(String instance, String indexName, String returnName, String matchClause, String whereClause, String returnClause)
	{
		HashSet<Map<String, Object>> rset = queryTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		for ( Map<String, Object> row : rset )
			return row;
		return new HashMap<String, Object>();
	}

	public static HashSet<Map<String, Object>> queryTopoAttributes(String instance, String indexName, String returnName, String matchClause, String whereClause, String returnClause)
	{
		StringBuilder qbld = new StringBuilder("START "+returnName+"=node:"+indexName+"(key=\""+instance+"\")"); 

		if (matchClause != null)
			qbld.append(" match " + matchClause);
		if (whereClause != null)
			qbld.append(" where " + whereClause);
		qbld.append(" return " + returnClause);

//		if (log.isDebugEnabled())
			log.info("Query = " + qbld.toString());
        
		HashSet<Map<String, Object>> rset = new HashSet<Map<String, Object>>();
        ExecutionResult result = null;
		try {
			result = engine.execute(qbld.toString());
			return getRows(result);
		} catch (Exception e) {
                	log.error("query exception", e);
			return rset;
		}
	}


	public static Map<String, Object> getRow(ExecutionResult result) 
	{
		HashSet<Map<String, Object>> rset = getRows(result);
		for ( Map<String, Object> row : rset )
			return row;
		return new HashMap<String, Object>();
	}
	//
	// perform two functions:
	// (1) convert result to HashSet<Map<String, Object>>
	// (2) dump the query result (note: after 'result' is accessed ==> it is
	//	cleared ==> it cannot be referred, we have to get query result
	//	from HashSet<Map<String, Object>>
	//
	// note that after below operation, 'result' is cleared
	//
	// (1) result.toString() converts result to String (not sure)
	// (2) result.dumpToString() converts result to String, then
	//	clear result (I confirm)
	// (3) for ( Map<String, Object> row : result ) { .. }
	//
	// conclusion, once 'result' is accessed, it is cleared
	//
	//
	public static HashSet<Map<String, Object>> getRows(ExecutionResult result)
	{
		HashSet<Map<String, Object>> rset = new HashSet<Map<String, Object>>();
		if (result == null)
		{
			if (log.isDebugEnabled())
				log.debug("result is null.");
			return rset;
		}
		if (log.isDebugEnabled())
			log.debug("++++++++");
		//
		// when return 2 rows
		//    C1   C2
		//    a    b	--> row1: map of <C1, a>, <C2, b>
		//    c    d	--> row2: map of <C1, c>, <C2, d>
		// ==> return a set which is composed of  row1 and row2
		// ie, return a set of map, each map is a row
		//
		for ( Map<String, Object> row : result )
		{
			rset.add(row);
			if (log.isDebugEnabled())
				log.debug("### row = " + row);

		}
		if (log.isDebugEnabled())
			log.debug("++++++++++++++++++");
		return rset;
	}
}
