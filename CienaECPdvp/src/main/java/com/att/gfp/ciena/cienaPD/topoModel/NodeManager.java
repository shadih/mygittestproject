package com.att.gfp.ciena.cienaPD.topoModel;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.Node;

import com.att.gfp.ciena.cienaPD.CienaAlarm;
import com.att.gfp.ciena.cienaPD.Util;

public class NodeManager
{
	private static Logger log = LoggerFactory.getLogger(NodeManager.class);

	public static final String CE_DEVICE_INDEX = "CE_Device";
	public static final String CE_CARD_INDEX = "CE_Card";
	public static final String CE_SLOT_INDEX = "CE_Slot";
	public static final String CE_PPORT_INDEX = "CE_PPort";
	public static final String PE_PPORT_INDEX = "PE_PPort";
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

	private static GraphDatabaseService db = IpagCienaTopoAccess.getGraphDB();
	private static ExecutionEngine engine;
	// result MUST be local otherwise it can be overrided by subsequent 
	// execute(), see getMptData() of CienaPdvp
	// private static ExecutionResult result;

	public NodeManager()
	{
		try {
			if (engine == null)
			{
				
				if (log.isInfoEnabled())
				log.info("Using UCA DB.");
				db = IpagCienaTopoAccess.getGraphDB();
				// log.info("Using UCA DB.");
				// db = IpagCienaTopoAccess.getGraphDB();
				// engine = new ExecutionEngine(db);
				engine = GFPUtil.getCypherEngine();
				log.info("Cypher Engine = " + engine);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// alarm Must be PPORT alarm
	public void setDevicePportNode(CienaAlarm alarm)
	{
		String pportInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String instance = pportInstance;
		String indexName=CE_PPORT_INDEX;
		String returnName="pport";
		String matchClause="(pport)<-[:Composed_Of_PPort]-(device)";
		String whereClause=null;
		String returnClause="pport,device";
		Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		if (row.size() > 0)
		{
			alarm.setPportNode((Node) row.get("pport"));
			alarm.setDeviceNode((Node) row.get("device"));
		}
	}

	// alarm Must be DEVICE alarm
	public void setDeviceNode(CienaAlarm alarm)
	{
		String deviceInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String instance = deviceInstance;
		String indexName=CE_DEVICE_INDEX;
		String returnName="device";
		String matchClause=null;
		String whereClause=null;
		String returnClause="device";
		Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		if (row.size() > 0)
			alarm.setDeviceNode((Node) row.get("device"));
	}

	// alarm Must be CFM alarm
	public void setDeviceEvcNode(CienaAlarm alarm)
	{
		String evcnodeInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String instance = evcnodeInstance;
		String indexName=EVCNODE_INDEX;
		String returnName="evcnode";
		String matchClause="(evcnode)<-[:Composed_Of]-(pport)<-[:Composed_Of_PPort]-(device)";
		String whereClause=null;
		String returnClause="evcnode,device";

		Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		if (row.size() > 0)
		{
			alarm.setEvcNode((Node) row.get("evcnode"));
			alarm.setDeviceNode((Node) row.get("device"));
		}
	}

	public Map<String, Object> getSDNInfo(String pportInstance, CienaAlarm alarm)
	{
		Node pportNode = alarm.getPportNode();
		Node deviceNode = alarm.getDeviceNode();
/*
		String deviceInstance = pportInstance.split("/")[0];

		String instance=deviceInstance;
		String indexName=CE_DEVICE_INDEX;
		String returnName="device";
		String matchClause="(device)-[:Composed_Of_PPort]->(pport)-[:Composed_Of]->(evcNode)";
		String whereClause="device.device_sub_role=\"EMT\" and pport.key=\""+pportInstance+"\" and pport.device_type=\"CIENA EMUX\" and pport.remote_pport_key=\"\" and (evcNode.product_type=\"SDN-ETHERNET\")";
		String returnClause="device.device_model,pport.clci,pport.port_aid,evcNode.clli";
*/
		String device_sub_role = getNodeValue(deviceNode, "device_sub_role");
		String pport_key = getNodeValue(pportNode, "key");
		String device_type = getNodeValue(pportNode, "device_type");
		String remote_pport_key = getNodeValue(pportNode, "remote_pport_key");
		log.info("device_sub_role = " + device_sub_role + ", pport_key = " + pport_key + ", device_type = " + device_type + ", remote_pport_key = " + remote_pport_key);
		if ("EMT".equals(device_sub_role) && 
			pportInstance.equals(pport_key) && 
			"CIENA EMUX".equals(device_type) &&
			"".equals(remote_pport_key))
		{

			String instance=pportInstance;
			String indexName=CE_PPORT_INDEX;
			String returnName="pport";
			String matchClause="(pport)-[:Composed_Of]->(evcNode)";
			String whereClause="evcNode.product_type=\"SDN-ETHERNET\"";
			String returnClause="evcNode.clli";
	
			return queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		}
		else
		{
			log.info("Skip getSDNInfo query.");
			return new HashMap<String, Object>();
		}
	}

	public Map<String, Object> getOEMInfo(String pportInstance, CienaAlarm alarm)
	{
		Node pportNode = alarm.getPportNode();
		Node deviceNode = alarm.getDeviceNode();

		String device_sub_role = getNodeValue(deviceNode, "device_sub_role");
		String pport_key = getNodeValue(pportNode, "key");
		String device_type = getNodeValue(pportNode, "device_type");
		String remote_pport_key = getNodeValue(pportNode, "remote_pport_key");
		log.info("device_sub_role = " + device_sub_role + ", pport_key = " + pport_key + ", device_type = " + device_type + ", remote_pport_key = " + remote_pport_key);
		if ("EMT".equals(device_sub_role) && 
			pportInstance.equals(pport_key) && 
			"CIENA EMUX".equals(device_type) &&
			"".equals(remote_pport_key))
		{

			String instance=pportInstance;
			String indexName=CE_PPORT_INDEX;
			String returnName="pport";
			String matchClause="(pport)-[:Composed_Of]->(evcNode)";
			String whereClause="(evcNode.product_type=\"OEM\") or (evcNode.product_type=\"OEM-AC\")";
			String returnClause="evcNode.clli";
	
			return queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		}
		else
		{
			log.info("Skip getOEMInfo query.");
			return new HashMap<String, Object>();
		}
	}

	// HLD-90
	public Map<String, Object> getInfovistaInfo(String pportInstance, CienaAlarm alarm)
	{
		Node pportNode = alarm.getPportNode();
		Node deviceNode = alarm.getDeviceNode();

		String device_sub_role = getNodeValue(deviceNode, "device_sub_role");
		String pport_key = getNodeValue(pportNode, "key");
		String device_type = getNodeValue(pportNode, "device_type");
		String mobility_ind_uni = getNodeValue(pportNode, "mobility_ind_uni");
		String remote_device_type = getNodeValue(pportNode, "remote_device_type");

		log.info("device_sub_role = " + device_sub_role + ", pport_key = " + pport_key + ", device_type = " + device_type + ", mobility_ind_uni = " + mobility_ind_uni + ", remote_device_type = " + remote_device_type);
		if ("EMT".equals(device_sub_role) && 
			"Y".equals(mobility_ind_uni) &&
			pportInstance.equals(pport_key) && 
			"CIENA EMUX".equals(device_type) &&
			"CIENA NTE".equals(remote_device_type))
		{

			String instance=pportInstance;
			String indexName=CE_PPORT_INDEX;
			String returnName="pport";
			String matchClause="(pport)-[:Composed_Of]->(evcNode)";
			String whereClause=null;
			String returnClause="evcNode.key";
	
			return queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		}
		else
		{
			log.info("Skip getInfovistaInfo query.");
			return new HashMap<String, Object>();
		}
	}

	// HLD-40
	public Map<String, Object> getFBSInfo(String pportInstance, CienaAlarm alarm)
	{
		Node pportNode = alarm.getPportNode();
		Node deviceNode = alarm.getDeviceNode();

		String device_sub_role = getNodeValue(deviceNode, "device_sub_role");
		String pport_key = getNodeValue(pportNode, "key");
		String device_type = getNodeValue(pportNode, "device_type");
		String remote_pport_key = getNodeValue(pportNode, "remote_pport_key");
		String product_type = getNodeValue(pportNode, "product_type");

		log.info("device_sub_role = " + device_sub_role + ", pport_key = " + pport_key + ", device_type = " + device_type + ", product_type = " + product_type + ", remote_pport_key = " + remote_pport_key);
		if ("EMT".equals(device_sub_role) && 
			pportInstance.equals(pport_key) && 
			"CIENA EMUX".equals(device_type) &&
			"".equals(remote_pport_key) &&
			"FBS".equals(product_type))
		{

			String instance=pportInstance;
			String indexName=CE_PPORT_INDEX;
			String returnName="pport";
			String matchClause="(pport)-[:Composed_Of]->(evcNode)";
			String whereClause="evcNode.key <> \"\"";
			String returnClause="evcNode.product_type,evcNode.evc_name,evcNode.clli";
	
			return queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		}
		else
		{
			log.info("Skip getFBSInfo query.");
			return new HashMap<String, Object>();
		}
	}

	public boolean isDevice_NTE_EMT(CienaAlarm alarm)
	{
		Node evcNode = alarm.getEvcNode();
		Node deviceNode = alarm.getDeviceNode();

		/*
		String instance=deviceInstance;
		String indexName=CE_DEVICE_INDEX;
		String returnName="device";
		String matchClause=null;
		String whereClause=null;
		String returnClause="device.device_type,device.device_role,device.device_sub_role";
		Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		if (row.size() == 0)
			return false;
		String device_type = (String)row.get("device.device_type");
		String device_role = (String)row.get("device.device_role");
		String device_sub_role = (String)row.get("device.device_sub_role");
		*/
		String device_type = getNodeValue(deviceNode, "device_type");
		String device_role = getNodeValue(deviceNode, "device_role");
		String device_sub_role = getNodeValue(deviceNode, "device_sub_role");
		if (log.isInfoEnabled())
		log.info("device_type = " + device_type +
				", device_role = " + device_role +
				", device_sub_role = " + device_sub_role);

		// if ( (device_type.equals("CIENA 3900 SERIES") && device_role.equals("NTE")) || device_type.equals("ADTRAN 800 SERIES") || (device_type.equals("CIENA EMUX") && device_sub_role.equals("EMT")))
		if ("CIENA NTE".equals(device_type)|| "ADTRAN 800 SERIES".equals(device_type) || ("CIENA EMUX".equals(device_type) && "EMT".equals(device_sub_role)))
			return true;
		else
			return false;
	}

	public void createVrfSet(CienaAlarm alarm, String pportInstance)
	{
		if (log.isInfoEnabled())
		log.info("Update VRF status.");

		String instance=pportInstance;
		String indexName=null;
		String returnName="pport";
		String matchClause=null;

		String[] ary = pportInstance.split("/");
		if (ary.length > 2)
		{
			indexName=PE_PPORT_INDEX;
			matchClause="(pport)-[:Composed_Of]->(lport)-[:Composed_Of]->(evcNode) ";
		}
		else
		{
			indexName=CE_PPORT_INDEX;
			matchClause="(pport)-[:Composed_Of]->(evcNode) ";
		}

		String whereClause=null;
		String returnClause="evcNode.key";

		HashSet<Map<String, Object>> rset = queryTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);

		String evcnodeInstance = null;
		String evcInstance = null;
		for ( Map<String, Object> row : rset )
		{
			evcnodeInstance = (String)row.get("evcNode.key"); 
			evcInstance = evcnodeInstance.split("/")[1];
			// updateVRFStatus(evcInstance, status, downType, downTimeType);
			alarm.addVrf(evcInstance);
			
		}
	}

/*
	private void updateVRFStatus(String evcInstance, String status, String downType, String downTimeType)
	{
		StringBuilder qbld = new StringBuilder("START evc=node:EVC(key=\""+evcInstance+"\") return evc");

		if (log.isInfoEnabled())
                log.info("Query ="+ qbld.toString());
		ExecutionResult result = engine.execute(qbld.toString());
		HashSet<Map<String, Object>> rset = getRows(result);

		Node nd = null;
		for ( Map<String, Object> row : rset )
		{
			nd = (Node) row.get("evc");
			setNodeProperty(nd, downType, status);
			double currentT = System.currentTimeMillis()/1000;
			setNodeProperty(nd, downTimeType, new BigDecimal(currentT)+"");
		}
	}
*/

	public HashSet<String> getPportSetForDyingGasp(String deviceInstance, String whereClausex)
	{
		String instance=deviceInstance;
		String indexName=CE_DEVICE_INDEX;
		String returnName="device";
		String matchClause="(device)-[:Composed_Of_PPort]->(pport)";
		String whereClause=whereClausex;
		String returnClause="pport.key";

		HashSet<Map<String, Object>> rset = queryTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);

		HashSet<String> pset = new HashSet<String>();
		for ( Map<String, Object> row : rset )
		{
			String pport = (String)row.get("pport.key"); 
			pset.add(pport);
		}
		return pset;
	}

	public HashSet<String> getRemotePportSetForDyingGasp(String deviceInstance, String whereClausex)
	{
		String instance=deviceInstance;
		String indexName=CE_DEVICE_INDEX;
		String returnName="device";
		String matchClause="(device)-[:Composed_Of_PPort]->(pport)";
		String whereClause=whereClausex;
		String returnClause="pport.remote_pport_key";

		HashSet<Map<String, Object>> rset = queryTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);

		HashSet<String> pset = new HashSet<String>();
		for ( Map<String, Object> row : rset )
		{
			String pport = (String)row.get("pport.remote_pport_key"); 
			pset.add(pport);
		}
		return pset;
	}

	public void updateCDCInfo(String evcnodeInstance, String cdcInfo)
	{
		StringBuilder qbld = new StringBuilder("START evcnode=node:EVCNode(key=\""+evcnodeInstance+"\") return evcnode");

		if (log.isInfoEnabled())
                log.info("Query ="+ qbld.toString());
		ExecutionResult result = engine.execute(qbld.toString());
		HashSet<Map<String, Object>> rset = getRows(result);
		
		Node nd = null;
		for ( Map<String, Object> row : rset )
		{
			nd = (Node) row.get("evcnode");
			setNodeProperty(nd, CDC_INFO, cdcInfo);
		}
	}

	public void setNodeProperty(Node node, String fldnm, String value)
	{
		Transaction tx = db.beginTx();
		try
	        {
			if (log.isInfoEnabled())
			log.info("set " + fldnm + " to " + value);
	        	node.setProperty(fldnm, value);
	        	tx.success();
	        }
	        finally
	        {
	           	tx.finish();
	        }
	}

	public String getNodeProperty(Node node, String fldnm, String defValue)
	{
		String ret = defValue;
		try {
			ret = (String) node.getProperty(fldnm);
		}
		catch(Exception e)
		{
			log.error("Error processing fldnm = " + fldnm + " defValue = " + defValue);
			//e.printStackTrace();
		    Transaction tx = db.beginTx();
	            try
	            {
	            	node.setProperty(fldnm, defValue);
			ret = defValue;
	            	tx.success();
	            }
	            finally
	            {
	               	tx.finish();
	            }
		}
		return ret;
	}

	public static String getNodeValue(Node node, String fldnm)
	{
		String ret = "";
		try {
			ret = (String) node.getProperty(fldnm);
		}
		catch(Exception e)
		{
			log.error("Error processing fldnm = " + fldnm);
			//e.printStackTrace();
		}
		return ret;
	}
/*
	public void updateTopoAttributes(String instance, String indexName, String returnName, String whereClause, String setClause)
	{
		StringBuilder qbld = new StringBuilder("START "+returnName+"=node:"+indexName+"(key=\""+instance+"\")"); 

		if (whereClause != null)
			qbld.append(" where " + whereClause);
		qbld.append(" set " + setClause);
		qbld.append(" return " + returnName);

                // System.out.println("Query ="+ qbld.toString());
		if (log.isInfoEnabled())
                log.info("Query = " + qbld.toString());
		engine.execute(qbld.toString());
	}
*/
/*
	public String queryVRFName(String evcnodeInstance)
	{
		String instance = evcnodeInstance;
		String indexName=EVCNODE_INDEX;
		String returnName="evcnode";
		String matchClause=null;
		String whereClause=null;
		String returnClause="evcnode.vrf_name";
		
		Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		if (row.size() == 0)
			return null;
		return (String)row.get("evcnode.vrf_name"); 
	}
*/

	// cannot get from PP as the multu_nni is for remote_pport_key
	public String queryMultinni(String deviceInstance)
	{
		String instance=deviceInstance;
		String indexName=CE_DEVICE_INDEX;
		String returnName="device";
		String matchClause=null;
		String whereClause=null;
		String returnClause="device.multi_nni";

		Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		if (row.size() == 0)
			return null;
		return (String)row.get("device.multi_nni"); 
	}

/*
	public String queryDeviceRole(String deviceInstance)
	{
		String instance=deviceInstance;
		String indexName=CE_DEVICE_INDEX;
		String returnName="device";
		String matchClause=null;
		String whereClause=null;
		String returnClause="device.device_role";

		Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		if (row.size() == 0)
			return null;
		return (String)row.get("device.device_role"); 
	}
*/

/*
	public String queryDeviceTypeByDevice(String deviceInstance)
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

*/
/*
	public String queryDeviceTypeByPport(String pportInstance)
	{
		String instance=pportInstance;
		String indexName=CE_PPORT_INDEX;
		String returnName="pport";
		String matchClause=null;
		String whereClause=null;
		String returnClause="pport.device_type";

		Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		if (row.size() == 0)
			return null;
		return (String)row.get("pport.device_type"); 
	}
*/
/*
	public String queryDeviceSubRole(String deviceInstance)
	{
		String instance=deviceInstance;
		String indexName=CE_DEVICE_INDEX;
		String returnName="device";
		String matchClause=null;
		String whereClause=null;
		String returnClause="device.device_sub_role";

		Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		if (row.size() == 0)
			return null;
		return (String)row.get("device.device_sub_role"); 
	}
*/

/*
	public String queryRemotePportByPport(String pportInstance)
	{
		String instance=pportInstance;
		String indexName=CE_PPORT_INDEX;
		String returnName="pport";
		String matchClause=null;
		String whereClause=null;
		String returnClause="pport.remote_pport_key";

		Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		if (row.size() == 0)
			return null;
		return (String)row.get("pport.remote_pport_key"); 
	}
*/

	public String queryEVCNameforFBS(String pportInstance)
	{
		String instance=pportInstance;
		String indexName=CE_PPORT_INDEX;
		String returnName="pport";
		String matchClause="(pport)-[:Composed_Of]->(evcNode) ";
		String whereClause="evcNode.product_type=\"FBS\"";
		String returnClause="evcNode.evc_name";

		Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		if (row.size() == 0)
			return null;
		return (String)row.get("evcNode.evc_name"); 
	}

	public static boolean isDevice_EMT_MX(CienaAlarm alarm)
	{
		if (("CIENA EMUX".equals(alarm.getDeviceType()) &&
			"EMT".equals(alarm.getDeviceSubRole())) ||
			"JUNIPER MX SERIES".equals(alarm.getDeviceType()))
			return true;
		else
			return false;
	}

	// public static boolean isDevice_NTE_EMT_800(String deviceInstance)
	public static boolean isDevice_NTE_EMT_800(CienaAlarm alarm)
	{
		Node deviceNode = alarm.getDeviceNode();
/*
		String instance=deviceInstance;
		String indexName=CE_DEVICE_INDEX;
		String returnName="device";
		String matchClause=null;
		String whereClause=null;
		String returnClause="device.device_type,device.device_role,device.device_sub_role";
		Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		if (row.size() == 0)
			return false;
		String device_type = (String)row.get("device.device_type");
		String device_role = (String)row.get("device.device_role");
		String device_sub_role = (String)row.get("device.device_sub_role");
*/
		String device_type = getNodeValue(deviceNode, "device_type");
		String device_role = getNodeValue(deviceNode, "device_role");
		String device_sub_role = getNodeValue(deviceNode, "device_sub_role");
		log.info("device_type = " + device_type +
				", device_role = " + device_role +
				", device_sub_role = " + device_sub_role);

		// if (("CIENA 3900 SERIES".equals(device_type) && "NTE".equals(device_role)) || "ADTRAN 800 SERIES".equals(device_type) || ("CIENA EMUX".equals(device_type) && "EMT".equals(device_sub_role)))
		if ("CIENA NTE".equals(device_type)|| "ADTRAN 800 SERIES".equals(device_type) || ("CIENA EMUX".equals(device_type) && "EMT".equals(device_sub_role)))
			return true;
		else
			return false;
	}
/*
	public static boolean isProdTypeFBS(String evcnodeInstance)
	{
		String instance = evcnodeInstance;
		String indexName=EVCNODE_INDEX;
		String returnName="evcnode";
		String matchClause=null;
		String whereClause="evcnode.product_type=\"FBS\"";
		String returnClause="evcnode.key";

		Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		if (row.size() == 0)
			return false;
		else
			return true;
	}
*/

	public static boolean isFBS_PtpMpt(CienaAlarm alarm)
	{
		Node evcNode = alarm.getEvcNode();
		Node deviceNode = alarm.getDeviceNode();

		// below is requested by Dave.  it is used in JuniperPdvp
		String mobility_ind = getNodeValue(deviceNode, "mobility_ind");
		log.info("mobility_ind = " + mobility_ind);
		alarm.setCustomFieldValue("mobility_ind", mobility_ind);
/*
		String evcnodeInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String deviceInstance = evcnodeInstance.split("/")[0];
		// String vrf_name = evcnodeInstance.split("/")[1];

		String instance = evcnodeInstance;
		String indexName=EVCNODE_INDEX;
		String returnName="evcnode";
		String matchClause=null;
		String whereClause=null;
		String returnClause="evcnode.vrf_name";
		
		Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
*/

		boolean isFBS = false;
		boolean isPtpMpt = false;
		// if (row.size() > 0)
		if (evcNode != null)
		{
			String classification = alarm.getCustomFieldValue(GFPFields.CLASSIFICATION);
			// below will be sent to AM.  they are not used for
			// correlation
			// String vrf_name = (String)row.get("evcnode.vrf_name");
			String vrf_name = getNodeValue(evcNode, "vrf_name");
			log.info("vrf_name = " + vrf_name);

			alarm.setCustomFieldValue("vrf-name", vrf_name);

			boolean isPtpCandidate = false;
			boolean isMptCandidate = false;
			if (vrf_name != null && (vrf_name.indexOf("VPWS") == 0 || vrf_name.indexOf("L2CKT") == 0))
				isPtpCandidate = true;
			if (vrf_name != null && vrf_name.indexOf("VPLS") == 0)
				isMptCandidate = true;

			if (isPtpCandidate && vrf_name != null && vrf_name.indexOf(":") != -1)
			{
				int idx = vrf_name.indexOf(":");
				if (idx != -1)
				{
					String snd = vrf_name.substring(idx+1);
					if (snd.contains("FBS"))
						isFBS = true;
				}
			}
			if (isFBS == false)
			{
				String product_type = getNodeValue(evcNode, "product_type");
				log.info("evc product_type = " + product_type);
				// isFBS = isProdTypeFBS(evcnodeInstance);
				isFBS = product_type.equals("FBS");
			}

			// Syslog is never PtpMpt as its 'isDevice_NTE_EMT_800'
			// is always false
			// below can be true or false for Ciena and Adtran
			// boolean isDevice_NTE_EMT_800 = isDevice_NTE_EMT_800(deviceInstance);
			boolean isDevice_NTE_EMT_800 = isDevice_NTE_EMT_800(alarm);
			// adtran is never FBS as its 'isDevice_EMT_MX' is 
			// always false
			// below can be true or false for Ciena and Syslog
			boolean isDevice_EMT_MX = isDevice_EMT_MX(alarm);

			log.info("classification = "+classification+", isPtpCandidate = "+isPtpCandidate+", isMptCandidate = "+isMptCandidate+", isFBS = "+isFBS+", isDevice_NTE_EMT_800 = "+isDevice_NTE_EMT_800+", isDevice_EMT_MX = "+isDevice_EMT_MX);

			// ONLY below are valid classification for CFM alarm 
			// 
			// FBS CFM uses classification EMT-CFM-CFO.
			// ASE CFM uses classification NTE-CFM-CFO (for 
			//    non-Mobility) and NTE-MOBILITY-CFM for Mobility.
			// SDN CFM under the Gamma project uses SDN-CFM-CFO.
			// 
			// NTE-MOBILITYUNI is NOT valid classification for CFM
			// but is valid for other Mobility UNI port alarms.
			//
			if (isPtpCandidate && isFBS && isDevice_EMT_MX)
				isFBS = true;
			else if ((isMptCandidate || (isPtpCandidate && !isFBS))
				&& isDevice_NTE_EMT_800 == true )
				isPtpMpt = true;
			log.info("isFBS = "+isFBS+", isPtpMpt = "+isPtpMpt);
			if (isFBS || isPtpMpt)
				return true;
			else
				return false;
		}
		else
		{
			log.info("The EVC node doesn't exist.");
			return false;
		}
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
		if (log.isInfoEnabled())
                log.info("Query = " + qbld.toString());
        	ExecutionResult result = null;
		try {
			result = engine.execute(qbld.toString());
			return getRows(result);
		} catch (Exception e) {
			if (log.isErrorEnabled())
                	log.error("query exception", e);
					e.printStackTrace();
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
			{
				if (log.isInfoEnabled())
                		log.info("property name: " + name + ", value: " +  node.getProperty(name));
			}
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
	public static HashSet<Map<String, Object>> getRows(ExecutionResult result)
	{
		HashSet<Map<String, Object>> rset = new HashSet<Map<String, Object>>();
		if (result == null)
		{
			if (log.isInfoEnabled())
			log.info("result is null.");
			return rset;
		}
		if (log.isInfoEnabled())
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
			if (log.isInfoEnabled())
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
			if (log.isInfoEnabled())
			log.info(rowStr.toString());
			} catch (Exception e) { continue; }
*/
		}
		if (log.isInfoEnabled())
		log.info("++++++++++++++++++");
		return rset;
	}
	
	public static String getMatchingPportsEvcs(CienaAlarm alarm, String serviceId)
	{
		String ret = "";
		String deviceInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		
		if ( log.isTraceEnabled() ) {
			log.trace("Entering getMatchingPportsEvcs() "+alarm.getIdentifier() + " deviceInstance = " + deviceInstance);
		}

		String instance=deviceInstance;
		String indexName=CE_DEVICE_INDEX;
		String returnName="device";
		String matchClause=null;
		matchClause="(device)-[:Composed_Of_PPort]->(pport)-[:Composed_Of]->(evcNode) ";

		//String whereClause=null;
		String whereClause="pport.remote_port_aid=\"\""; 
		String returnClause="evcNode.service_ind,evcNode.evc_name,pport.key,pport.clci";

		HashSet<Map<String, Object>> rset = queryTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		String pportKey = "first"; 
		ArrayList<String> processedPports = new ArrayList<String>();
		
		for (Map<String, Object> row : rset)
		{
			
			if (serviceId.equals((String)row.get("evcNode.service_ind"))) 
			{
				if ( !((String)row.get("pport.key")).equals(pportKey) && !processedPports.contains((String)row.get("pport.key")) ) {
					ret = ret + "CLCI=<" + (String)row.get("pport.clci") + "> EVC=<" + (String)row.get("evcNode.evc_name") + "> ";
					pportKey = (String)row.get("pport.key");
					processedPports.add(pportKey);					
				}
			}
		}
		if ( log.isTraceEnabled() ) {
			log.trace("Matching results from getMatchingPportsEvcs() "+alarm.getIdentifier() + " ret = " + ret);
		}
		
		return ret;
	}
}
