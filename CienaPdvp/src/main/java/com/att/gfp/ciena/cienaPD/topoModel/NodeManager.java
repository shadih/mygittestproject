package com.att.gfp.ciena.cienaPD.topoModel;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.att.gfp.helper.GFPUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.Node;

import com.att.gfp.ciena.cienaPD.CienaAlarm;
import com.att.gfp.ciena.cienaPD.ExtendedLifeCycle;
import com.att.gfp.ciena.cienaPD.Util;

public class NodeManager
{
	// sample query
	// start device=node:CE_Device(key="10.204.82.209") match (device)-[:Composed_Of_PPort]->(pport) return pport.TDL_instance_name

	private static Logger log = LoggerFactory.getLogger(NodeManager.class);

	public static final String CE_DEVICE_INDEX = "CE_Device";
	public static final String CE_CARD_INDEX = "CE_Card";
	public static final String CE_SLOT_INDEX = "CE_Slot";
	public static final String CE_PPORT_INDEX = "CE_PPort";
	public static final String CE_LPORT_INDEX = "CE_LPort";
	public static final String EVCNODE_INDEX = "EVCNode";
	public static final String EVC_INDEX = "EVC";
	public static final String CE_DEVICE_PPORT_INDEX = "ComposedOfPPort.CE_Device.CE_PPort";
	public static final String CE_PPORT_LPORT_INDEX = "ComposedOf.CE_PPort.CE_LPort";
	public static final String CE_LPORT_EVCNODE_INDEX = "ComposedOf.CE_LPort.EVCNode";

	public static final String CIENA_DYING_GASP = "ciena_dying_gasp";
	public static final String CIENA_DYING_GASP_TIME = "ciena_dying_gasp_time";
	public static final String CIENA_LINK_DOWN = "ciena_link_down";
	public static final String CIENA_LINK_DOWN_TIME = "ciena_link_down_time";
	public static final String JUNIPER_LINK_DOWN = "juniper_link_down";
	public static final String JUNIPER_LINK_DOWN_TIME = "juniper_link_down_time";
	public static final String CDC_SUBSCRIPTION_TYPE = "cdc_subscription_type";
	public static final String SENT_TO_CDC = "sent_to_cdc";
	public static final String CDC_INFO = "cdc_info";

	// private static GraphDatabaseService db = IpagCienaTopoAccess.getGraphDB();
	private static GraphDatabaseService db = null;
	private static ExecutionEngine engine;
	// result MUST be local otherwise it can be overrided by subsequent
	// execute(), see getMptData()
	// private static ExecutionResult result;

	public NodeManager()
	{
		try {
			if (engine == null)
			{
				if (Util.getJunit())
				{
					log.info("Using local DB.");
					db = DbQuery.getGraphDB();
				}
				else
				{
					log.info("Using UCA DB.");
					db = IpagCienaTopoAccess.getGraphDB();
				}
				// engine = new ExecutionEngine(db);
				engine = GFPUtil.getCypherEngine();
				log.info("Cypher Engine = " + engine);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static String getNodeValue(Node node, String fldnm)
	{
		String ret = "";
		try {
			ret = (String) node.getProperty(fldnm);
		}
		catch(Exception e)
		{
			log.info("Node has an null value.");
		}
		return ret;
	}
	public static void logNodeInfo(Node dn, Node pn, Node en)
	{
		String mobility_ind = getNodeValue(dn, "mobility_ind");
		String device_type = getNodeValue(dn, "device_type");
		String device_sub_role = getNodeValue(dn, "device_sub_role");
		String device_role = getNodeValue(dn, "device_role");
		String device_name = getNodeValue(dn, "device_name");
		String device_model = getNodeValue(dn, "device_model");
		String mpa_ind = getNodeValue(dn, "mpa_ind");
		String clci = getNodeValue(pn, "clci");
		String clfi = getNodeValue(pn, "clfi");
		String mobility_ind_uni = getNodeValue(pn, "mobility_ind_uni");
		String mpa_connect_type = getNodeValue(pn, "mpa_connect_type");
		String port_aid = getNodeValue(en,"port_aid");
		String product_type = getNodeValue(en, "product_type");
		String clli = getNodeValue(en,"clli");
		String evc_name = getNodeValue(en, "evc_name");
		log.info("mobility_ind = "+mobility_ind+
			", device_type = "+device_type+
			", device_sub_role = "+device_sub_role+
			", device_role = "+device_role+
			", device_name = "+device_name+
			", device_model = "+device_model+
			", mpa_ind = "+mpa_ind+
			", clci = "+clci+
			", clfi = "+clfi+
			", mobility_ind_uni = "+mobility_ind_uni+
			", mpa_connect_type = "+mpa_connect_type+
			", port_aid = "+port_aid+
			", product_type = "+product_type+
			", clli = "+clli+
			", evc_name = "+evc_name);
	}
	// alarm Must be CFM alarm
	//
	// per chat with Nga:
	// the pport for PTP/MPT MUST be UNI for both alarming and faulty ends.
	// for FBS: one UNI and one JUNIPER IPAG port
	//
	// All alarming end needs to call below method.
	// MPT calls getIsCC() which calls isMobility() which needs the alarming
	// end's Node info.  ==> MPT needs call below method
	//
	// alarm is the alarming end
	//
	public static void setDevicePportEvcNode(CienaAlarm alarm)
	{
		String alarmingEndEvc = alarm.getOriginatingManagedEntity().split(" ")[1];
		String deviceInstance = alarmingEndEvc.split("/")[0];

		String instance=deviceInstance;
		String indexName=CE_DEVICE_INDEX;
		String returnName="device";
		String matchClause=null;
		if (isEvcNodeAdtran5000(alarmingEndEvc))
			matchClause="(device)-[:Composed_Of]->(slot)-[:Composed_Of]->(card)-[:Composed_Of]->(pport)-[:Composed_Of]->(evcNode) ";
		else
			matchClause="(device)-[:Composed_Of_PPort]->(pport)-[:Composed_Of]->(evcNode) ";

		String whereClause=null;
		String returnClause="evcNode,pport,device,pport.remote_device_type,evcNode.key";

		HashSet<Map<String, Object>> rset = queryTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		for (Map<String, Object> row : rset)
		{
			if (alarmingEndEvc.equals((String)row.get("evcNode.key"))) 
			{
				alarm.setDeviceNode((Node) row.get("device"));
				// set pport node here in case no pport	
				// has remote_device_type = ""
				alarm.setPportNode((Node) row.get("pport"));
				alarm.setEvcNode((Node) row.get("evcNode"));
				if ("".equals((String)row.get("pport.remote_device_type"))) 
					break;
			}
		}
		Node dn = alarm.getDeviceNode();
		Node pn = alarm.getPportNode();
		Node en = alarm.getEvcNode();
		log.info("Alarming end info for "+alarm.getIdentifier());
		logNodeInfo(dn, pn, en);
	}

	// alarm is the alarming end.  below is only called by PTP ONE alarm ==>
	// getFaultyEndXXXNode() is valid for PTP ONE alarm only
	//
	// PTP two alarms: both alarms are both alarming and faulty end
	//	==> faulty end info is in alarming end
	// MPT: faulty end info is in MptData
	// No need to call below method for PTP two alarms and MPT.
	//
	public static void setFaultyEndDevicePportEvcNode(CienaAlarm alarm, String faultyEndEvc)
	{
		String deviceInstance = faultyEndEvc.split("/")[0];

		String instance=deviceInstance;
		String indexName=CE_DEVICE_INDEX;
		String returnName="device";
		String matchClause=null;
		if (isEvcNodeAdtran5000(faultyEndEvc))
			matchClause="(device)-[:Composed_Of]->(slot)-[:Composed_Of]->(card)-[:Composed_Of]->(pport)-[:Composed_Of]->(evcNode) ";
		else
			matchClause="(device)-[:Composed_Of_PPort]->(pport)-[:Composed_Of]->(evcNode) ";

		String whereClause=null;
		String returnClause="evcNode,pport,device,pport.remote_device_type,evcNode.key";

		HashSet<Map<String, Object>> rset = queryTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		for (Map<String, Object> row : rset)
		{
			if (faultyEndEvc.equals((String)row.get("evcNode.key"))) 
			{
				alarm.setFaultyEndDeviceNode((Node) row.get("device"));
				// set pport node here in case no pport has 	
				// remote_device_type = ""
				alarm.setFaultyEndPportNode((Node) row.get("pport"));
				alarm.setFaultyEndEvcNode((Node) row.get("evcNode"));
				if ("".equals((String)row.get("pport.remote_device_type"))) 
					break;

			}
		}
		Node dn = alarm.getFaultyEndDeviceNode();
		Node pn = alarm.getFaultyEndPportNode();
		Node en = alarm.getFaultyEndEvcNode();
		log.info("Faulty end info (for PTP ONE alarm case) "+alarm.getIdentifier());
		logNodeInfo(dn, pn, en);
	}

	public static String getFaultyend(CienaAlarm trigger, String evcNode, String vrfname)
	{
		// evcNode.nte_uni="true" ==> UNI port
		StringBuilder qbld = new StringBuilder("START evc=node:EVC(key=\""+vrfname+"\") match (evc)-[:Associated_To]->(evcNode) where evcNode.key<>\""+evcNode+"\" and evcNode.nte_uni=\"true\" return evcNode.key"); 

                log.info("Query ="+ qbld.toString());
		ExecutionResult result = engine.execute(qbld.toString());
		HashSet<Map<String, Object>> rset = getRows(result);
		for ( Map<String, Object> row : rset )
		{
			String fe = (String)row.get("evcNode.key");
			String deviceInstance = fe.split("/")[0];
			// only NTE/EMT is qualified for faulty end
			if (NodeManager.isDevice_NTE_EMT(deviceInstance))
			{
				log.info("faulty end = " + fe);
				// trigger is alarming end
				setFaultyEndDevicePportEvcNode(trigger, fe);
				return fe;
			}
		}
		log.info("No faulty end");
		return null;
	}

	public static HashSet<String> getAllEnds(String vrfname)
	{
		StringBuilder qbld = new StringBuilder("START evc=node:EVC(key=\""+vrfname+"\") match (evc)-[:Associated_To_Mepid]->(mepid_tbl) return mepid_tbl.key"); 
		HashSet<String> eset = new HashSet<String>();

                log.info("Query = " + qbld.toString());
		ExecutionResult result = engine.execute(qbld.toString());
		HashSet<Map<String, Object>> rset = getRows(result);
		for ( Map<String, Object> row : rset )
		{
			String key = (String)row.get("mepid_tbl.key"); 
			if (key == null)
				continue;
			log.info("key = "+key);
			String ip = key.split("/")[0];
			// this is the faulty end for MPT
			String faultyend = ip+"/"+vrfname;
			eset.add(faultyend);
		}
		log.info("Potential faulty ends = " + eset);
		return eset;
	}

	public static MptData getMptData(String mepid, String vrfname, boolean isCienaAlarm)
	{
		String clli="";
		String port_aid="";
		String clci="";
		String clfi="";
		String prodType="";
		String mpa_ind="";
		String evc_name="";
		String device_name="";

		StringBuilder qbld = null;
		//
		// evc.key=mepid_tbl.vrf_name doesn't need to add to
		// the whereClause as the assoication already guarantee it.
		//
		if (isCienaAlarm == true)
			qbld = new StringBuilder("START evc=node:EVC(key=\""+vrfname+"\") match (evc)-[:Associated_To_Mepid]->(mepid_tbl) where mepid_tbl.mepid=\""+mepid+"\" return mepid_tbl.clli,mepid_tbl.clci,mepid_tbl.key"); 
		else
			qbld = new StringBuilder("START evc=node:EVC(key=\""+vrfname+"\") match (evc)-[:Associated_To_Mepid]->(mepid_tbl) where mepid_tbl.mepid<>\""+mepid+"\" return mepid_tbl.clli,mepid_tbl.clci,mepid_tbl.key"); 

                log.info("Query = " + qbld.toString());
		ExecutionResult result = engine.execute(qbld.toString());
		Map<String, Object> row = getRow(result);
		//
		if (row.size() > 0)
		{
			String mpa_connect_type = "";
			String device_type = "";
			String device_sub_role = "";
			clli = (String)row.get("mepid_tbl.clli"); 
			clci = (String)row.get("mepid_tbl.clci"); 
			String key = (String)row.get("mepid_tbl.key"); 
			log.info("key = "+key);
			String ary[] = key.split("/");
			String ip = null;
			port_aid = null;
			if (ary.length == 3)
			{
				ip = ary[0];
				port_aid = ary[2];
				// this is the faulty end for MPT
				String faultyEndEvc = ip+"/"+vrfname;

				String instance=ip;
				String indexName=CE_DEVICE_INDEX;
				String returnName="device";
				String matchClause;
				// faultyEndEvc is faulty end
				if (isEvcNodeAdtran5000(faultyEndEvc))
					matchClause="(device)-[:Composed_Of]->(slot)-[:Composed_Of]->(card)-[:Composed_Of]->(pport)-[:Composed_Of]->(evcNode) ";
				else
					matchClause="(device)-[:Composed_Of_PPort]->(pport)-[:Composed_Of]->(evcNode) ";

				// String whereClause="evcNode.TDL_instance_name=\""+faultyEndEvc+"\" and pport.remote_device_type=\"\"";
				// String returnClause="device.device_type,device.device_sub_role,pport.clfi,pport.mpa_connect_type,device.mpa_ind,evcNode.product_type";
				// Map<String, Object> rowp = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);

				String whereClause=null;
				// String returnClause="device.device_type,device.device_sub_role,pport.clfi,pport.mpa_connect_type,device.mpa_ind,evcNode.product_type,evcNode.TDL_instance_name,pport.remote_device_type,";

				String returnClause="evcNode,pport,device,pport.remote_device_type,evcNode.key";

				HashSet<Map<String, Object>> rset = queryTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
				for (Map<String, Object> rowp : rset)
				{
					if (faultyEndEvc.equals((String)rowp.get("evcNode.key")) && "".equals((String)rowp.get("pport.remote_device_type")))
					{
						// clfi = (String)rowp.get("pport.clfi");
						// mpa_connect_type = (String)rowp.get("pport.mpa_connect_type");
						// device_type = (String)rowp.get("device.device_type");
						// device_sub_role = (String)rowp.get("device.device_sub_role");
						// prodType = (String)rowp.get("evcNode.product_type");
						// mpa_ind = (String)rowp.get("device.mpa_ind");

						Node dn = (Node) rowp.get("device");
						Node pn = (Node) rowp.get("pport");
						Node en = (Node) rowp.get("evcNode");

						device_type = getNodeValue(dn, "device_type");
						device_sub_role = getNodeValue(dn, "device_sub_role");
						device_name = getNodeValue(dn, "device_name");
						mpa_ind = getNodeValue(dn, "mpa_ind");
						clfi = getNodeValue(pn, "clfi");
						mpa_connect_type = getNodeValue(pn, "mpa_connect_type");
						prodType = getNodeValue(en, "product_type");
						evc_name = getNodeValue(en, "evc_name");

						log.info("faulty end = "+faultyEndEvc+", port_aid = "+port_aid+", clli = "+clli+", clci = "+clci+", key = "+key+", clfi = "+clfi+", mpa_connect_type = "+mpa_connect_type+", device_sub_role = "+device_sub_role+", device_type = "+device_type+", prodType = "+prodType+", mpa_ind ="+mpa_ind+", device_name = "+device_name+", evc_name = "+evc_name
);
						return new MptData(faultyEndEvc, port_aid, clli, clci, clfi, mpa_connect_type,device_type,device_sub_role,prodType,mpa_ind,device_name,evc_name);
					}
				}
			}
			else
				log.info("drop the assoication: " + vrfname + "-> " + key);
		}
		log.info("No faulty end found for " + vrfname);
		return null;
	}

	// below is called for Ciean CFM only
	// alarm is the alarming end
	public static boolean isMobility(CienaAlarm alarm)
	{
		Node pn = alarm.getPportNode();
		Node dn = alarm.getDeviceNode();

		String mobility_ind = getNodeValue(dn, "mobility_ind");
		String mobility_ind_uni = getNodeValue(pn, "mobility_ind_uni");
		if ("Y".equals(mobility_ind)||"Y".equals(mobility_ind_uni))
			return true;
		else
			return false;
	}

	// below cannot be from the Device node. it needs query
	public static boolean isDevice_NTE_EMT(String deviceInstance)
	{
		String instance=deviceInstance;
		String indexName=CE_DEVICE_INDEX;
		String returnName="device";
		String matchClause=null;
		String whereClause=null;
		String returnClause="device.device_type,device.device_role,device.device_sub_role";
		Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		if (row.size() == 0)
		{
			log.info("Device is NOT NTE/EMT.");
			return false;
		}
		String device_type = (String)row.get("device.device_type");
		String device_role = (String)row.get("device.device_role");
		String device_sub_role = (String)row.get("device.device_sub_role");
		log.info("device_type = " + device_type +
				", device_role = " + device_role +
				", device_sub_role = " + device_sub_role);
		// if (("CIENA 3900 SERIES".equals(device_type) && "NTE".equals(device_role)) || "ADTRAN 800 SERIES".equals(device_type) || ("CIENA EMUX".equals(device_type) && "EMT".equals(device_sub_role)))
		if ("CIENA NTE".equals(device_type)|| "ADTRAN 800 SERIES".equals(device_type) || ("CIENA EMUX".equals(device_type) && "EMT".equals(device_sub_role)))
		{
			log.info("Device is NTE/EMT.");
			return true;
		}
		else
		{
			log.info("Device NOT is NTE/EMT.");
			return false;
		}
	}

	public static boolean isDevice_NTE_EMT(CienaAlarm alarm)
	{
		Node dn = alarm.getDeviceNode();

		String device_type = getNodeValue(dn, "device_type");
		String device_role = getNodeValue(dn, "device_role");
		String device_sub_role = getNodeValue(dn, "device_sub_role");
		log.info("device_type = " + device_type +
				", device_role = " + device_role +
				", device_sub_role = " + device_sub_role);
		// if (("CIENA 3900 SERIES".equals(device_type) && "NTE".equals(device_role)) || "ADTRAN 800 SERIES".equals(device_type) || ("CIENA EMUX".equals(device_type) && "EMT".equals(device_sub_role)))
		if ("CIENA NTE".equals(device_type)|| "ADTRAN 800 SERIES".equals(device_type) || ("CIENA EMUX".equals(device_type) && "EMT".equals(device_sub_role)))
		{
			log.info("Device is NTE/EMT.");
			return true;
		}
		else
		{
			log.info("Device NOT is NTE/EMT.");
			return false;
		}
	}

	/////////////////////////
	// isEvcNodeAdtran5000() is called before the device/pport/evc nodes
	// get query ==> db query cannot be avoid.
	/////////////////////////
	public static boolean isEvcNodeAdtran5000(String evcNodeInstance)
	{
		String deviceInstance = evcNodeInstance.split("/")[0];
		String deviceType = queryDeviceTypeByDevice(deviceInstance);

		if ("ADTRAN 5000 SERIES".equals(deviceType))
			return true;
		else
			return false;
	}

	public static String queryDeviceTypeByDevice(String deviceInstance)
	{
		String instance=deviceInstance;
		String indexName=CE_DEVICE_INDEX;
		String returnName="device";
		String matchClause=null;
		String whereClause=null;
		String returnClause="device.device_type";

		Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		if (row.size() == 0)
			return null;
		return (String)row.get("device.device_type"); 
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

		HashSet<Map<String, Object>> rset = new HashSet<Map<String, Object>>();
                log.info("Query = " + qbld.toString());
        	ExecutionResult result = null;
		try {
			result = engine.execute(qbld.toString());
			return getRows(result);
		} catch (Exception e) {
                	log.error("query exception", e);
			return rset;
		}

		/*
		if (result != null)
		{
			for ( Map<String, Object> row : result )
				rset.add(row);
		}
		return rset;
		*/
		/*
        	for ( Map<String, Object> row : result )
        	{
        		Node node = (Node) row.get(RETURNNAME_D);
		
            		for (String name : node.getPropertyKeys())
                		log.info("property name: " + name + ", value: " +  node.getProperty(name));
		}
		*/
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
			log.info("result is null.");
			return rset;
		}
		log.info("++++++++");
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
			log.info("### row = " + row);
/*
			try {
			Set set = row.entrySet();
			Iterator i = set.iterator();
			StringBuilder rowStr = new StringBuilder("");
			while(i.hasNext())
			{
				// if querry returns a node instead of its
				// fields ==> below throw exception
				// ==> catch and not print result values
				Map.Entry me = (Map.Entry)i.next();
				rowStr.append((String)me.getKey()+"="+(String)me.getValue()+", " );
			}
			log.info(rowStr.toString());
			} catch (Exception e) { continue; }
*/
		}
		log.info("++++++++++++++++++");
		return rset;
	}
}
