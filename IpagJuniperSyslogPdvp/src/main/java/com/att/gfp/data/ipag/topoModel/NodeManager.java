package com.att.gfp.data.ipag.topoModel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.SyslogAlarm;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;

public class NodeManager
{
	private static Logger log = LoggerFactory.getLogger(NodeManager.class);

	public static final String PE_DEVICE = "PE_Device";
	public static final String PE_SLOT = "PE_Slot";
	public static final String PE_CARD = "PE_Card";
	public static final String PE_PPORT = "PE_PPort";
	public static final String CE_DEVICE = "CE_Device";
	public static final String CE_PPORT = "CE_PPort";
	public static final String EVC = "EVC";
	public static final String EVCNODE = "EVCNode";
	

	private static GraphDatabaseService db;
	private static ExecutionEngine engine;

	static
	{
		try {
			db = JuniperSyslogTopoAccess.getGraphDB();
			engine = GFPUtil.getCypherEngine();
		} catch (Exception e) {
			log.trace("ERROR:"
					+ Arrays.toString(e.getStackTrace()));
		}
	}

	public static void setMobilityByPport(SyslogAlarm alarm)
	{
		String pportInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String deviceInstance = pportInstance.split("/")[0];

		String instance=deviceInstance;
		String indexName=CE_DEVICE;
		String returnName="device";
		String matchClause="(device)-[:Composed_Of_PPort]->(pport)";
		String whereClause="pport.key=\""+pportInstance+"\"";
		String returnClause="device.mobility_ind, pport.mobility_ind_uni";

		alarm.setMobility("N");
		Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		if (row.size() > 0)
			if( ((String) row.get("device.mobility_ind")).equals("Y") || ((String) row.get("pport.mobility_ind_uni")).equals("Y"))
				alarm.setMobility("Y");
	}

	/*public static void getDeviceInfo(SyslogAlarm a, String deviceInstance)
	{
		String instance=deviceInstance;
		String indexName=null;
		
		if (log.isDebugEnabled())
			log.debug("getDeviceInfo():   Device level information missing alarm");
		
		if(a.getNodeType().equals("CE"))
			indexName=CE_DEVICE;
		else
			indexName=PE_DEVICE;
			
		String returnName="device";
		String matchClause=null;
		String whereClause=null;
		String returnClause="device.device_type,device,device.device_sub_role";
		Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		
		a.setDeviceType((String)row.get("device.device_type"));
		a.setDeviceSubRole((String)row.get("device.device_sub_role"));

		if (log.isDebugEnabled())
			log.debug("device_type = " + a.getDeviceType() +
				", device_sub_role = " + a.getDeviceSubRole());
	}*/
	
	public static boolean isDevice_NTE_EMT_800(SyslogAlarm a)
	{
		// if (("CIENA 3900 SERIES".equals(device_type) && "NTE".equals(device_role)) || "ADTRAN 800 SERIES".equals(device_type) || ("CIENA EMUX".equals(device_type) && "EMT".equals(device_sub_role)))
		log.info("device type = " + a.getDeviceType());
		if ("CIENA NTE".equals(a.getDeviceType()) || "ADTRAN 800 SERIES".equals(a.getDeviceType()) || 
				("CIENA EMUX".equals(a.getDeviceType()) && "EMT".equals(a.getDeviceSubRole())))
			return true;
		else
			return false;
	}

	public static boolean isProdTypeFBS(String evcnodeInstance)
	{
		boolean ret=false;
		String instance = evcnodeInstance;
		String indexName=EVCNODE;
		String returnName="evcnode";
		String matchClause=null;
		String whereClause=null;
		//String whereClause="evcnode.product_type=\"FBS\"";
		String returnClause="evcnode.key, evcnode.product_type";

		Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		if (row.size() > 0) {
			if( ((String) row.get("evcnode.product_type")).equals("FBS"))
				ret =true;
		}
		
		return ret;
	}

	public static boolean isRemoteDeviceTypeAlcatel(String evcnodeInstance)
	{
		String deviceInstance = evcnodeInstance.split("/")[0];

		boolean ret=false;
		String instance=deviceInstance;
		String indexName=PE_DEVICE;
		String returnName="device";
		String matchClause="(device)-[:Composed_Of]->(slot)-[:Composed_Of]->(card)-[:Composed_Of]->(pport)-[:Composed_Of]->(lport)-[:Composed_Of]->(evcNode) ";
		//String whereClause="evcNode.TDL_instance_name=\""+evcnodeInstance+"\" and pport.remote_device_type=\"ALCATEL 7450\"";
		String whereClause=null;
		String returnClause="evcNode.TDL_instance_name, pport.remote_device_type";

		//Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		HashSet<Map<String, Object>> rset = queryTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);

		for (Map<String, Object> rowp : rset)
		{
			if ((((String)rowp.get("evcNode.TDL_instance_name")).equals(evcnodeInstance) &&
					((String)rowp.get("pport.remote_device_type")).contains("ALCATEL"))) 
				ret = true;
		}
		
		return ret;
	}

	public static boolean setFBS_PtpMpt(SyslogAlarm alarm, boolean isJuniper)
	{
		String evcnodeInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String deviceInstance = evcnodeInstance.split("/")[0];
		// String vrf_name = evcnodeInstance.split("/")[1];

		String instance = evcnodeInstance;
		String indexName=EVCNODE;
		String returnName="evcnode";
		String matchClause=null;
		String whereClause=null;
		// String returnClause="evcnode,evcnode.vrf_name,evcnode.unickt,evcnode.acnaban,evcnode.evc_name";
		String returnClause="evcnode.vrf_name,evcnode.unickt,evcnode.acnaban,evcnode.evc_name";
		
		Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);

		alarm.setIsFBSPtp(false);
		alarm.setIsPtpMpt(false);
		boolean ret = true;
		if (row.size() > 0)
		{
			String classification = alarm.getCustomFieldValue(GFPFields.CLASSIFICATION);
			// below will be sent to AM.  they are not used for
			// correlation
			// Node nd = (Node) row.get("evcnode");
			String vrf_name = (String)row.get("evcnode.vrf_name");
			alarm.setCustomFieldValue("vrf-name", vrf_name);

			boolean isPtpCandidate = false;
			boolean isMptCandidate = false;
			if (vrf_name != null && (vrf_name.indexOf("VPWS") == 0 || vrf_name.indexOf("L2CKT") == 0))
				isPtpCandidate = true;
			if (vrf_name != null && vrf_name.indexOf("VPLS") == 0)
				isMptCandidate = true;

			boolean isFBS = false;
			if (isPtpCandidate && vrf_name != null && vrf_name.indexOf(":") != -1)
			{
				/*
				// below failed for VPWS:34737:FBS
				String vrf[] = vrf_name.split(":");
				if (vrf[1] != null && vrf[1].contains("FBS"))
					isFBS = true;
				*/
				int idx = vrf_name.indexOf(":");
				if (idx != -1)
				{
					String snd = vrf_name.substring(idx+1);
					if (snd.contains("FBS"))
						isFBS = true;
				}
			}
			if (isFBS == false)
				isFBS = isProdTypeFBS(evcnodeInstance);
			if (isFBS == false && isJuniper == true)
				// product type == FBS applies only to Ciena CFM
				// for Juniper CFM, check its remote device type
				// see attachement of Carolyn email dated
				// 6/23/2014 10:58
				isFBS = isRemoteDeviceTypeAlcatel(evcnodeInstance);

			// Syslog is never PtpMpt as its 'isDevice_NTE_EMT_800'
			// is always false
			// below can be true or false for Ciena and Adtran
			boolean isDevice_NTE_EMT_800 = isDevice_NTE_EMT_800(alarm);
			// adtran is never FBS as its 'isDevice_EMT_MX' is 
			// always false
			// below can be true or false for Ciena and Syslog
			boolean isDevice_EMT_MX = isDevice_EMT_MX(alarm);
			if (log.isDebugEnabled())
				log.debug("classification = "+classification+", isPtpCandidate = "+isPtpCandidate+", isMptCandidate = "+isMptCandidate+", isFBS = "+isFBS+", isDevice_NTE_EMT_800 = "+isDevice_NTE_EMT_800+", isDevice_EMT_MX = "+isDevice_EMT_MX);

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
				alarm.setIsFBSPtp(true);
			else if ( 
// comment out below checking
// (classification.equals("NTE-CFM-CFO") || classification.equals("NFO-MOBILITY-CFM") || classification.equals("SDN-CFM-CFO")) &&
				(isMptCandidate || (isPtpCandidate && !isFBS))
				&& isDevice_NTE_EMT_800 == true )
				alarm.setIsPtpMpt(true);
			if (log.isDebugEnabled())
				log.debug("isFBSPtp = "+alarm.getIsFBSPtp()+", isPtpMpt = "+alarm.getIsPtpMpt());
			ret = true;
		}
		else
		{
			if (log.isDebugEnabled())
				log.debug("The EVC node doesn't exist.  Drop it.");
			ret = false;
		}
		return ret;
	}

	public static void setColdStartVRFSet(SyslogAlarm alarm)
	{
		String deviceInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String instance=deviceInstance;
		String indexName=CE_DEVICE;
		String returnName="device";
		String matchClause="(device)-[:Composed_Of_PPort]->(pport)-[:Composed_Of]->(evcNode)";

		// String whereClause="(device.device_type=\"CIENA 3900 SERIES\" and device.device_role=\"NTE\") or (device.device_type=\"CIENA EMUX\" and device.device_sub_role=\"EMT\")";
		String whereClause = null;
		//String whereClause="(device.device_type=\"CIENA NTE\") or (device.device_type=\"CIENA EMUX\" and device.device_sub_role=\"EMT\")";
		String returnClause="evcNode.vrf_name";

		if (alarm.getDeviceType().equals("CIENA NTE") ||
				(alarm.getDeviceType().equals("CIENA EMUX") && 
						alarm.getDeviceSubRole().equals("EMT"))) {

			HashSet<Map<String, Object>> rset = queryTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);

			for (Map<String, Object> rowp : rset)
			{
				alarm.addVRFName((String)rowp.get("evcNode.vrf_name"));
			}
		}
	}

	public static boolean setFRUVRFSet(SyslogAlarm alarm)
	{
		// DF
		/*START slot=node:PE_Slot(key="10.144.0.137/3") match (slot)-[:Composed_Of]->(card)-[:Composed_Of]->(pport)-[:Composed_Of]->(lport)-[:Composed_Of]->(evcNode)  
				WHERE pport.class = "IpagPPort" AND card.class = "IpagCard" AND lport.class = "IpagLPort" 
				RETURN slot.key, card.key, pport.key, lport.key, evcNode.key*/
		String indexName;
		String returnName;
		String matchClause;

		String objectInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String objectClass = alarm.getOriginatingManagedEntity().split(" ")[0];
		String instance=objectInstance;
		if ("SLOT".equals(objectClass))
		{
			indexName=PE_SLOT;
			returnName="slot";
			matchClause="(slot)-[:Composed_Of]->(card)-[:Composed_Of]->(pport)-[:Composed_Of]->(lport)-[:Composed_Of]->(evcNode) ";
		}
		else if ("CARD".equals(objectClass))
		{
			indexName=PE_CARD;
			returnName="card";
			matchClause="(card)-[:Composed_Of]->(pport)-[:Composed_Of]->(lport)-[:Composed_Of]->(evcNode) ";
		}
		else
		{
			if (log.isDebugEnabled())
				log.debug("Unknown object class = " + objectClass + ". Drop it.");
			return false;
		}
		String whereClause=null;
		String returnClause="evcNode.vrf_name";

		HashSet<Map<String, Object>> rset = queryTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);

		for (Map<String, Object> rowp : rset)
		{
			alarm.addVRFName((String)rowp.get("evcNode.vrf_name"));
		}
		return true;
	}

	public static boolean isFaultyEndCiena_EMT(String deviceInstance)
	{
		boolean ret=false;
		String instance=deviceInstance;
		String indexName=CE_DEVICE;
		String returnName="device";
		String matchClause=null;
		String whereClause=null;
		//String whereClause="(device.device_type=\"CIENA EMUX\") and (device.device_sub_role=\"EMT\")";
		String returnClause="device.device_type, device.device_sub_role";

		Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		if (row.size() > 0) {
			if (((String)row.get("device.device_type")).equals("CIENA EMUX") &&
					((String)row.get("device.device_sub_role")).equals("EMT"))
			ret=true;
		}
		
		return ret;
	}

	public static boolean isDevice_EMT_MX(SyslogAlarm alarm)
	{
		if (("CIENA EMUX".equals(alarm.getDeviceType()) &&
			"EMT".equals(alarm.getDeviceSubRole())) ||
			"JUNIPER MX SERIES".equals(alarm.getDeviceType()))
			return true;
		else
			return false;
	}

	public static boolean isFaultyEndJuniper_MX(String deviceInstance)
	{
		boolean ret=false;
		String instance=deviceInstance;
		String indexName=PE_DEVICE;
		String returnName="device";
		String matchClause=null;
		String whereClause=null;
		//String whereClause="device.device_type=\"JUNIPER MX SERIES\"";
		String returnClause="device.device_type";

		Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		if (row.size() > 0)
			if (((String)row.get("device.device_type")).equals("JUNIPER MX SERIES"))			
				ret = true;

		return ret;

	}

	public static FBSPtpData FetchFBSSyslogPtpDataTwoAlarms(SyslogAlarm alarm)
	{
		String evcnodeInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String deviceInstance = evcnodeInstance.split("/")[0];

		String instance=deviceInstance;
		String indexName=PE_DEVICE;
		String returnName="device";
		String matchClause="(device)-[:Composed_Of]->(slot)-[:Composed_Of]->(card)-[:Composed_Of]->(pport)-[:Composed_Of]->(lport)-[:Composed_Of]->(evcNode) ";
		String whereClause="evcNode.TDL_instance_name=\""+evcnodeInstance+"\"";
		String returnClause="device.device_name,evcNode.clli,evcNode.port_aid,evcNode.evc_name";

		Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);

		if (row.size() == 0)
			return null;

		String device_name = (String)row.get("device.device_name");
		String clli = (String)row.get("evcNode.clli");
		String port_aid = (String)row.get("evcNode.port_aid");
		String evc_name = (String)row.get("evcNode.evc_name");
		// String mpa_connect_type = (String)row.get("pport.mpa_connect_type");
		String mpa_connect_type = "";
		// clli is same as device_name.  clli can be empty. 
		// use device_name instead of clli per email from Dickey date
		// 6/24/2014 1:42 PM
		// return new FBSPtpData(port_aid, clli, mpa_connect_type, device_name, "", "", evc_name);
		return new FBSPtpData(port_aid, device_name, mpa_connect_type, device_name, "", "", evc_name);
	}

	public static FBSPtpData FetchFBSCienaPtpDataTwoAlarms(SyslogAlarm alarm)
	{
		String evcnodeInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String deviceInstance = evcnodeInstance.split("/")[0];

		String instance=deviceInstance;
		String indexName=CE_DEVICE;
		String returnName="device";
		String matchClause="(device)-[:Composed_Of_PPort]->(pport)-[:Composed_Of]->(evcNode) ";
		String whereClause="evcNode.TDL_instance_name=\""+evcnodeInstance+"\"";
		String returnClause="device.device_name,evcNode.clli,evcNode.port_aid,evcNode.evc_name";

		Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);

		if (row.size() == 0)
			return null;

		String device_name = (String)row.get("device.device_name");
		String clli = (String)row.get("evcNode.clli");
		String port_aid = (String)row.get("evcNode.port_aid");
		String evc_name = (String)row.get("evcNode.evc_name");
		// String mpa_connect_type = (String)row.get("pport.mpa_connect_type");
		String mpa_connect_type = "";
		// clli is same as device_name.  clli can be empty. 
		// use device_name instead of clli per email from Dickey date
		// 6/24/2014 1:42 PM
		// return new FBSPtpData(port_aid, clli, mpa_connect_type, device_name, "", "", evc_name);
		return new FBSPtpData(port_aid, device_name, mpa_connect_type, device_name, "", "", evc_name);
	}
	public static String getFBSSyslogFaultyend(SyslogAlarm alarm)
	{
		String evcNodeInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String evcInstance = evcNodeInstance.split("/")[1];

		//EVC 192.168.95.22/VPLS:36859
		
		// the preprocessor will in fact suppress an alarm where the target does not exist.
		// so this first query is not needed
		
//		String instance=evcNodeInstance;
//		String indexName=EVCNODE;
//		String returnName="evcnode";
//		String matchClause=null;
//		String whereClause=null;
//		//String whereClause="evcnode.key=\""+evcNodeInstance+"\"";
//		String returnClause="evcnode.key";
//
//		Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
//
//		if (row.size() == 0)
//		{
//			if (log.isDebugEnabled())
//				log.debug("invalid alarming end.");
//			return null;
//		}
//
		String faultyEnd=null;
		String instance=evcInstance;
		String indexName=EVC;
		String returnName="evc";
		String matchClause="(evc)-[:Associated_To]->(evcnode)";
		String whereClause = null;
		//String whereClause="evcnode.key<>\""+evcNodeInstance+"\"";
		String returnClause="evcnode.key";

		HashSet<Map<String, Object>> rset = queryTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);

		String f_evcNodeInstance;
		boolean foundFaultyEnd=false;
		for ( Map<String, Object> rowp : rset )
		{
			if(!((String)rowp.get("evcnode.key")).equals(evcNodeInstance)  &&
					!foundFaultyEnd) {
				f_evcNodeInstance = (String)rowp.get("evcnode.key");
				String deviceInstance = f_evcNodeInstance.split("/")[0];
				// syslog's faulty end is ciena
				if(isFaultyEndCiena_EMT(deviceInstance) == true)
				{
					if (log.isDebugEnabled())
						log.debug("faulty end = " + f_evcNodeInstance);
					faultyEnd = f_evcNodeInstance;
					foundFaultyEnd = true;
				}
			}
		}
			
		if(!foundFaultyEnd)
			if (log.isDebugEnabled())
				log.debug("No faulty end.");

		return faultyEnd;
	}

	public static String getFBSCienaFaultyend(SyslogAlarm alarm)
	{
		String evcNodeInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String evcInstance = evcNodeInstance.split("/")[1];

		//		String instance=evcNodeInstance;
		//		String indexName=EVCNODE;
		//		String returnName="evcnode";
		//		String matchClause=null;
		//		String whereClause="evcnode.key=\""+evcNodeInstance+"\"";
		//		String returnClause="evcnode.key";
		//
		//		Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		//
		//		if (row.size() == 0)
		//		{
		//			if (log.isDebugEnabled())
		//				log.debug("invalid alarming end.");
		//			return null;
		//		}

		String instance=evcInstance;
		String indexName=EVC;
		String returnName="evc";
		String matchClause="(evc)-[:Associated_To]->(evcnode)";
		String whereClause=null;
		//String whereClause="evcnode.key<>\""+evcNodeInstance+"\"";
		String returnClause="evcnode.key";

		HashSet<Map<String, Object>> rset = queryTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);

		boolean foundFaultyEnd=false;
		String faultyEnd=null;
		String f_evcNodeInstance;
		for ( Map<String, Object> rowp : rset )
		{
			if(!((String)rowp.get("evcnode.key")).equals(evcNodeInstance)  &&
					!foundFaultyEnd) {
				f_evcNodeInstance = (String)rowp.get("evcnode.key");
				String deviceInstance = f_evcNodeInstance.split("/")[0];
				// ciena's faulty end is syslog
				if(isFaultyEndJuniper_MX(deviceInstance) == true)
				{
					if (log.isDebugEnabled())
						log.debug("faulty end = " + f_evcNodeInstance);
					faultyEnd = f_evcNodeInstance;
					foundFaultyEnd=true;
				}
			}
		}
		
		if(!foundFaultyEnd)
			if (log.isDebugEnabled())
				log.debug("No faulty end.");

		return faultyEnd;
	}
		
	public static FBSPtpData FetchFBSSyslogPtpDataOneAlarm(String evcnodeInstance) {
		String deviceInstance = evcnodeInstance.split("/")[0];

		String instance=deviceInstance;
		String indexName=PE_DEVICE;
		String returnName="device";
		String matchClause="(device)-[:Composed_Of]->(slot)-[:Composed_Of]->(card)-[:Composed_Of]->(pport)-[:Composed_Of]->(lport)-[:Composed_Of]->(evcNode) ";
		String whereClause="evcNode.TDL_instance_name=\""+evcnodeInstance+"\"";
		String returnClause="device.device_name,device.device_type,device.device_model,evcNode.clli,evcNode.port_aid,evcNode.evc_name";

		Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);

		if (row.size() == 0)
			return null;

		String device_name = (String)row.get("device.device_name");
		String device_type = (String)row.get("device.device_type");
		String device_model = (String)row.get("device.device_model");
		String clli = (String)row.get("evcNode.clli");
		String port_aid = (String)row.get("evcNode.port_aid");
		String evc_name = (String)row.get("evcNode.evc_name");
		// String mpa_connect_type = (String)row.get("pport.mpa_connect_type");
		String mpa_connect_type = "";
		// clli is same as device_name.  clli can be empty. 
		// use device_name instead of clli per email from Dickey date
		// 6/24/2014 1:42 PM
		// return new FBSPtpData(port_aid, clli, mpa_connect_type, "", device_type, device_model, evc_name);
		return new FBSPtpData(port_aid, device_name, mpa_connect_type, "", device_type, device_model, evc_name);
	}

	public static FBSPtpData FetchFBSCienaPtpDataOneAlarm(String evcnodeInstance) {
		String deviceInstance = evcnodeInstance.split("/")[0];

		String instance=deviceInstance;
		String indexName=CE_DEVICE;
		String returnName="device";
		String matchClause="(device)-[:Composed_Of_PPort]->(pport)-[:Composed_Of]->(evcNode) ";
		String whereClause="evcNode.TDL_instance_name=\""+evcnodeInstance+"\"";
		String returnClause="device.device_name,device.device_type,device.device_model,evcNode.clli,evcNode.port_aid,evcNode.evc_name";

		Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);

		if (row.size() == 0)
			return null;

		String device_name = (String)row.get("device.device_name");
		String device_type = (String)row.get("device.device_type");
		String device_model = (String)row.get("device.device_model");
		String clli = (String)row.get("evcNode.clli");
		String port_aid = (String)row.get("evcNode.port_aid");
		String evc_name = (String)row.get("evcNode.evc_name");
		// String mpa_connect_type = (String)row.get("pport.mpa_connect_type");
		String mpa_connect_type = "";
		// clli is same as device_name.  clli can be empty. 
		// use device_name instead of clli per email from Dickey date
		// 6/24/2014 1:42 PM
		// return new FBSPtpData(port_aid, clli, mpa_connect_type, "", device_type, device_model, evc_name);
		return new FBSPtpData(port_aid, device_name, mpa_connect_type, "", device_type, device_model, evc_name);
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
		if (log.isDebugEnabled())
			log.debug("Query = " + qbld.toString());

		ExecutionResult result = null;
		try {
			result = engine.execute(qbld.toString());
		} catch (Exception e) {
                	log.error("query exception", e);
			return rset;	// return empty set
		}

		try {
			return getRows(result);
		} catch (Exception e) {
                	log.error("getRows() exception", e);
			return rset;	// return empty set
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
		if (log.isDebugEnabled())
			log.debug("++++++++++++++++++");
		return rset;
	}
	public static String getRemotePtnii (SyslogAlarm alarm)
	{
		//String instance=alarm.getCustomFieldValue(GFPFields.REMOTE_DEVICE_IPADDR);
		String instance=alarm.getRemoteDeviceIpaddr();
		String indexName="PE_Device";
		String returnName="device";
		String matchClause=null;
		String whereClause = null;
		String returnClause="device.ptnii";

		Map<String, Object> row = queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);

		if (row.size() == 0)
			return null;

		String remotePtnii = (String)row.get("device.ptnii");
		return remotePtnii;
	}
}
