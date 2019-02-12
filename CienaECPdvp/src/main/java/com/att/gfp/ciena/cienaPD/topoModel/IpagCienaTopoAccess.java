package com.att.gfp.ciena.cienaPD.topoModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

// import org.apache.log4j.Logger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import com.att.gfp.ciena.cienaPD.CienaAlarm;
import com.att.gfp.ciena.cienaPD.Util;
import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
import com.att.gfp.helper.service_util;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.scenario.ScenarioThreadLocal;
import com.hp.uca.expert.topology.TopoAccess;

public class IpagCienaTopoAccess extends TopoAccess 
{
	private static Logger log = LoggerFactory.getLogger(IpagCienaTopoAccess.class);
	// private static Map<String, CienaAlarm> cfmMap = new HashMap<String, CienaAlarm>();
	public static Scenario scenario = ScenarioThreadLocal.getScenario();

	private static NodeManager nmgr;

	public IpagCienaTopoAccess()
	{
		nmgr = new NodeManager();
	}

	// alarm Must be PPORT alarm
	public void setDevicePportNode(CienaAlarm alarm)
	{
		nmgr.setDevicePportNode(alarm);
	}
	// alarm Must be DEVICE alarm
	public void setDeviceNode(CienaAlarm alarm)
	{
		nmgr.setDeviceNode(alarm);
	}
	// alarm Must be CFM alarm
	public void setDeviceEvcNode(CienaAlarm alarm)
	{
		nmgr.setDeviceEvcNode(alarm);
	}
/*
	public boolean isDevice_NTE_EMT(CienaAlarm alarm)
	{
		String evcnodeInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String deviceInstance = evcnodeInstance.split("/")[0];
		return nmgr.isDevice_NTE_EMT(deviceInstance);
	}
*/
	public void enrichUnreachableAlarm(CienaAlarm alarm)
	{
		Node deviceNode = alarm.getDeviceNode();
		String device_sub_role = nmgr.getNodeValue(deviceNode, "device_sub_role");
		String device_type = nmgr.getNodeValue(deviceNode, "device_type");

		String managedObjectInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String deviceInstance = managedObjectInstance.split("/")[0]; // TJtodo: is it needed?

		// new spec and HLD-110
		// String instance=deviceInstance;
		// String indexName=nmgr.CE_DEVICE_INDEX;
		// String returnName="device";
		// String matchClause="(device)-[:Composed_Of_PPort]->(pport)";
		// String whereClause="(device.device_sub_role=\"EMT\" and device.device_type=\"CIENA EMUX\") or (pport.remote_device_type=\"JUNIPER MX SERIES\")";
		String instance=null;
		String indexName=null;
		String returnName=null;
		String matchClause=null;
		String whereClause=null;
		String returnClause=null;
		if ("EMT".equals(device_sub_role) && 
				"CIENA EMUX".equals(device_type))
		{
			alarm.setUnreachableAlarm2JuniperVP(true);
		}
		else
		{
			instance=deviceInstance;
			indexName=nmgr.CE_DEVICE_INDEX;
			returnName="device";
			matchClause="(device)-[:Composed_Of_PPort]->(pport)";
			whereClause="pport.remote_device_type=\"JUNIPER MX SERIES\"";
			returnClause="device.device_type";


			Map<String, Object> row = nmgr.queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
			if (row.size() > 0)
			{
				alarm.setUnreachableAlarm2JuniperVP(true);
			}
		}

		String multinni = null;
		multinni = alarm.getMultiNni();
		if (multinni == null || !multinni.equals("Y"))
		{
			// System.out.println("multinni is not Y");
			if (log.isInfoEnabled())
			log.info("multinni is not Y.  No enrichment.");
			return;
		}

		instance=deviceInstance;
		indexName=nmgr.CE_DEVICE_INDEX;
		returnName="device";
		matchClause="(device)-[:Composed_Of_PPort]->(pport)";
		// whereClause="(pport.nmvlan <> \"\") or (pport.slavlan <> \"\") or ((pport.nmvlan = \"\") and (pport.slavlan = \"\") and ((pport.remote_device_type = \"JUNIPER MX SERIES\") or (pport.remote_device_type = \"CIENA EMUX\")))";
		whereClause=null;
		returnClause="pport.port_aid,pport.clfi,pport.nmvlan,pport.slavlan,pport.remote_device_type";

		HashSet<Map<String, Object>> rset = nmgr.queryTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);

		// System.out.println("# of row = "+ rset.size());
		if (log.isInfoEnabled())
		log.info("# of row = "+ rset.size());
		String nmvlan=null;
		String slavlan=null;
		String remote_device_type=null;
		String port_aid="";
		String clfi="";
		StringBuilder sbld = new StringBuilder();
		for ( Map<String, Object> rowp : rset )
		{
			nmvlan = (String)rowp.get("pport.nmvlan"); 
			slavlan = (String)rowp.get("pport.slavlan"); 
			remote_device_type = alarm.getRemoteDeviceType(); 
			if (log.isInfoEnabled())
			log.info("nmvlan = " + nmvlan +
				", slavlan = " + slavlan +
				", sm_element = " + alarm.getSm_element() +
				", remote_device_type = " + remote_device_type);

			if((nmvlan != null && !nmvlan.equals("")) || 
			   (slavlan != null && !slavlan.equals("")) || 
			   (remote_device_type != null && (remote_device_type.equals("JUNIPER MX SERIES") || remote_device_type.equals("CIENA EMUX"))))
			{
				port_aid = (String)rowp.get("pport.port_aid"); 
				clfi = (String)rowp.get("pport.clfi"); 
				if (log.isInfoEnabled())
				log.info("port_aid = " + port_aid +
					  ", clfi = " + clfi);

				if (port_aid != null && port_aid.length() > 0)
					sbld.append(" NNIPort=<"+port_aid+">");
				if (clfi != null && clfi.length() > 0)
					sbld.append(" NNICLFI=<"+clfi+">");
			}
		}
		
		String info1 = (String) alarm.getCustomFieldValue(GFPFields.INFO1);
		alarm.setCustomFieldValue(GFPFields.INFO1, info1+" MultiNNI=<Y> "+sbld.toString());
		if (log.isInfoEnabled())
		log.info("info1 = "+ alarm.getCustomFieldValue(GFPFields.INFO1));
	}

	public void enrichVRFStatusDyingGaspAlarm(CienaAlarm alarm)
	{
		if (log.isInfoEnabled())
		log.info("enrichVRFStatusDyingGaspAlarm() runs.");
		String managedObjectInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String deviceInstance = managedObjectInstance.split("/")[0]; // TJtodo: is it needed?
		String deviceType = alarm.getDeviceType();

		// below is similar to VRF status updated by CienaLD
		HashSet<String> pset;
		if ("CIENA NTE".equals(deviceType) || ("CIENA EMUX".equals(deviceType) && "EMT".equals(alarm.getDeviceSubRole())))
		{
			// find pport which meets below conditions under the 
			// device ==> it is done in query, not be in java 
			String whereClause = "(pport.remote_device_type=\"JUNIPER MX SERIES\") or (pport.remote_device_type=\"CIENA EMUX\")";
			pset = nmgr.getPportSetForDyingGasp(deviceInstance, whereClause);
		}
		else if ("CIENA EMUX".equals(deviceType))
		{
			String whereClause = "pport.remote_device_type=\"JUNIPER MX SERIES\"";
			pset = nmgr.getRemotePportSetForDyingGasp(deviceInstance, whereClause);
		}
		else
		{
			if (log.isInfoEnabled())
			log.info("No VRF update.");
			return;
		}
		if (log.isInfoEnabled())
		log.info("pset = " + pset);
		Iterator i = pset.iterator();
		while(i.hasNext())
		{
			String pport = (String)i.next();
			if (pport.length() > 0)
				nmgr.createVrfSet(alarm, pport);
		}
	}

/*
	public void enrichCFM_CDCInfoLinkDownAlarm(CienaAlarm alarm)
	{
		Node pportNode = alarm.getPportNode();
		HashSet<Map<String, Object>> rset = new HashSet<Map<String, Object>>();
		Map<String, Object> m = new HashMap<String, Object>();

		String remote_pport_key = nmgr.getNodeValue(pportNode, "remote_pport_key");
		log.info("remote_pport_key = " + remote_pport_key);
		if (remote_pport_key != null && remote_pport_key.length() > 0)
		{
			m.put("pport.remote_pport_key", remote_pport_key);
			rset.add(m);
		}
		enrichCFM_CDCInfo(rset, alarm);
	}
*/
/*
	public void enrichCFM_CDCInfoDyingGaspAlarm(CienaAlarm alarm)
	{
		String managedObjectInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String deviceInstance = managedObjectInstance.split("/")[0]; // TJtodo: is it needed?

		String instance = deviceInstance;
		String indexName=nmgr.CE_DEVICE_INDEX;
		String returnName="device";
		String matchClause="(device)-[:Composed_Of_PPort]->(pport)";
		String whereClause=null;
		String returnClause="pport.remote_pport_key";

		HashSet<Map<String, Object>> rset = nmgr.queryTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		enrichCFM_CDCInfo(rset, alarm);
	}
	*/

/*
	public void enrichCFM_CDCInfo(HashSet<Map<String, Object>>rset, CienaAlarm alarm)
	{
		String instance = null;
		String indexName=null;
		String returnName=null;
		String matchClause=null;
		String whereClause=null;
		String returnClause=null;

		String remote_pport_key = null;
		StringBuilder sbld = new StringBuilder();
		HashSet<String> cdcInfoSet = new HashSet<String>();
		int count = 0;
		for ( Map<String, Object> row : rset )
		{
			remote_pport_key = (String) row.get("pport.remote_pport_key");
			// System.out.println("remote_pport_key = " + remote_pport_key);
			if (log.isInfoEnabled())
			log.info("remote_pport_key = " + remote_pport_key);

			String[] ary = remote_pport_key.split("/");
			if (ary.length > 2)
			{
				indexName=nmgr.PE_PPORT_INDEX;
				matchClause="(pport)-[:Composed_Of]->(lport)-[:Composed_Of]->(evcNode) ";
			}
			else
			{
				indexName=nmgr.CE_PPORT_INDEX;
				matchClause="(pport)-[:Composed_Of]->(evcNode) ";
			}
			
			instance = remote_pport_key;
			returnName="pport";
			whereClause=null;
			returnClause="evcNode.key";
			HashSet<Map<String, Object>> rset_evcNode = nmgr.queryTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
			String evcnodeInstance;
			for ( Map<String, Object> row_evcNode : rset_evcNode )
			{
				evcnodeInstance = (String) row_evcNode.get("evcNode.key");
				if (log.isInfoEnabled())
				log.info("evcnodeInstance = " + evcnodeInstance);
				CienaAlarm cfmAlarm = cfmMap.get(evcnodeInstance);
				if (cfmAlarm == null)
				{
					if (log.isInfoEnabled())
					log.info("Its corresponding CFM alarm doesn't exist.");
					continue;
				}
				if (log.isInfoEnabled())
				log.info("Its corresponding CFM alarm exists.");
				String cdcInfo = cfmAlarm.getCustomFieldValue("cdc-info");
				if (log.isInfoEnabled())
				log.info("cdc info = " + cdcInfo);
				if (cdcInfo != null && cdcInfo.length() > 0)
				{
					if (cdcInfoSet.contains(cdcInfo))
						continue;
					cdcInfoSet.add(cdcInfo);
					sbld.append(" "+cdcInfo);
					count++;
				}
			}
		}
		if (count == 0)
		{
			if (log.isInfoEnabled())
			log.info("No CDC Info appended.");
		}
		String info3 = (String) alarm.getCustomFieldValue(GFPFields.INFO3);
		alarm.setCustomFieldValue(GFPFields.INFO3, info3+sbld.toString());
		if (log.isInfoEnabled())
		log.info("info3 = "+ alarm.getCustomFieldValue(GFPFields.INFO3));
	}
*/

	public void enrichVRFStatusLinkDownAlarm(CienaAlarm alarm)
	{
		if (log.isInfoEnabled())
		log.info("enrichVRFStatusLinkDownAlarm() runs.");
		String pportInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String device_type = alarm.getDeviceType();

		String remote_pport_key = alarm.getRemotePportKey();

		// changed based on Tom's email on 5-21-2014, 10:11am
		String port;
		if ("CIENA NTE".equals(device_type) || ("CIENA EMUX".equals(device_type) && "EMT".equals(alarm.getDeviceSubRole())))
			port = pportInstance;
		else if ("CIENA EMUX".equals(device_type))
			port = remote_pport_key;
		else
		{
			if (log.isInfoEnabled())
			log.info("No VRF update.");
			return;
		}
		if (log.isInfoEnabled())
		log.info("port = " + port);
		nmgr.createVrfSet(alarm, port);
	}

	public void enrichVRFStatusJuniperLD(CienaAlarm alarm)
	{
		log.info("enrichVRFStatusJuniperLD() runs.");

		String pportInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		nmgr.createVrfSet(alarm, pportInstance);
	}

	public void preEnrichPPortAlarm(CienaAlarm alarm)
	{
		Node pportNode = alarm.getPportNode();
		Node deviceNode = alarm.getDeviceNode();

		String pportInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String deviceInstance = pportInstance.split("/")[0];

		//// (1),(3),(4),(5),(7)
		String instance=null;
		String indexName=null;
		String returnName=null;
		String matchClause=null;
		String whereClause=null;
		String returnClause=null;

/*
		String instance = pportInstance;
		String indexName=nmgr.CE_PPORT_INDEX;
		String returnName="pport";
		String matchClause=null;
		String whereClause=null;
		String returnClause="pport.device_type,pport.remote_device_type,pport.remote_device_name,pport.remote_port_aid,pport.clci,pport.port_ipaddr,pport.legacy_org_ind,pport.clfi,pport.mpa_connect_type";

		Map<String, Object> rowp = nmgr.queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
*/
		String device_type = null;
		String remote_device_type = null;
		String remote_device_name = null;
		String remote_port_aid = null;
		String clci = null;
		String port_ipaddr = null;
		String legacy_org_ind = null;
		String reason = alarm.getCustomFieldValue(GFPFields.REASON);
		String classification = alarm.getCustomFieldValue(GFPFields.CLASSIFICATION);
		String clfi = null;
		String mpa_connect_type = null;

		String mpa_ind = null;
		if (deviceNode != null)
		{
			mpa_ind = nmgr.getNodeValue(deviceNode, "mpa_ind");
		}

		// if (rowp.size() > 0)
		if (pportNode != null)
		{
			// (1)
			// device_type = (String)rowp.get("pport.device_type"); 
			// remote_device_type = (String)rowp.get("pport.remote_device_type"); 
			// remote_device_name = (String)rowp.get("pport.remote_device_name"); 
			// remote_port_aid = (String)rowp.get("pport.remote_port_aid"); 
			clci = nmgr.getNodeValue(pportNode, "clci");
			clfi = nmgr.getNodeValue(pportNode, "clfi");
			mpa_connect_type = nmgr.getNodeValue(pportNode, "mpa_connect_type");
			device_type = nmgr.getNodeValue(pportNode, "device_type");
			remote_device_type = nmgr.getNodeValue(pportNode, "remote_device_type");
			remote_device_name = nmgr.getNodeValue(pportNode, "remote_device_name");
			remote_port_aid = nmgr.getNodeValue(pportNode, "remote_port_aid");
			log.info("device_type = " + device_type + ", remote_device_type = " + remote_device_type + ", remote_device_name = " + remote_device_name + ", remote_port_aid = " + remote_port_aid);

			if (device_type.equals("CIENA EMUX") && remote_device_type != null && !remote_device_type.equals("JUNIPER MX SERIES") && !remote_device_type.equals(""))
			{
				alarm.setCustomFieldValue(GFPFields.INFO2, alarm.getCustomFieldValue(GFPFields.INFO2)+" RemoteNTECLLI=<"+remote_device_name+"> RemotePortAID=<"+remote_port_aid+">");
			}
			// (3)
			// clci = (String)rowp.get("pport.clci"); 
			// port_ipaddr = (String)rowp.get("pport.port_ipaddr"); 
			// legacy_org_ind = (String)rowp.get("pport.legacy_org_ind"); 
			clci = nmgr.getNodeValue(pportNode, "clci");
			port_ipaddr = nmgr.getNodeValue(pportNode, "port_ipaddr");
			legacy_org_ind = nmgr.getNodeValue(pportNode, "legacy_org_ind");
			log.info("clci = " + clci +", port_ipaddr = "+ port_ipaddr + ", legacy_org_ind = " + legacy_org_ind);

			if (!reason.contains("Region"))
				// don't append 'Region' if it already exists
				alarm.setCustomFieldValue(GFPFields.REASON, reason+" Region=<"+legacy_org_ind+">");
			reason = alarm.getCustomFieldValue(GFPFields.REASON);
			if (clci == null || (clci != null && clci.equals("")))
				alarm.setCustomFieldValue(GFPFields.REASON, reason+" IP_ADDRESS=<"+port_ipaddr+">");
			// (4)
			// clfi = (String)rowp.get("pport.clfi"); 
			clfi = nmgr.getNodeValue(pportNode, "clfi");
			log.info("clfi = " + clfi);

			alarm.setCustomFieldValue(GFPFields.CIRCUIT_ID, clci);
			alarm.setCustomFieldValue(GFPFields.CLCI, clci);
			alarm.setCustomFieldValue(GFPFields.CLFI, clfi);
			// (5)
			if (classification != null && clfi != null && 
				clfi.equals("") &&
				(classification.equals("NFO-MOBILITY") ||
				classification.equals("NFO-TA5000") ||
				classification.equals("NFO-EMUX") ||
				classification.equals("NTE-CFO") ||
				classification.equals("NFO-NPA")))
				alarm.setCustomFieldValue(GFPFields.CLFI, "CLFI-UNKNOWN");
			// (7)
			if (classification != null && classification.equals("NFO-MPA"))
			{
				// mpa_connect_type = (String)rowp.get("pport.mpa_connect_type"); 
				mpa_connect_type = nmgr.getNodeValue(pportNode, "mpa_connect_type");
				alarm.setCustomFieldValue(GFPFields.INFO2, alarm.getCustomFieldValue(GFPFields.INFO2)+" MPA=<Y> MPAType=<"+mpa_connect_type+">");
				if (mpa_connect_type != null)
				{
					if (mpa_connect_type.equals("DIRECT LEC"))
					{
						alarm.setCustomFieldValue(GFPFields.CLCI, clci);
						alarm.setCustomFieldValue(GFPFields.CLFI, "");
		
					}
					else if (mpa_connect_type.equals("GE TRUNK"))
					{
						alarm.setCustomFieldValue(GFPFields.CLFI, clfi);
						alarm.setCustomFieldValue(GFPFields.CLCI, "");
					}
				}
			}
		}


		String multinni = null;
		multinni = alarm.getMultiNni();
		if (multinni != null && multinni.equals("Y"))
		{
			//// 2.(a)
			alarm.setCustomFieldValue(GFPFields.INFO1, alarm.getCustomFieldValue(GFPFields.INFO1)+" MultiNNI=<Y>");

			//// 2.(b)
			instance = deviceInstance;
			indexName=nmgr.CE_DEVICE_INDEX;
			returnName="device";
			matchClause="(device)-[:Composed_Of_PPort]->(pport)";
			whereClause="(pport.key <> \""+pportInstance+"\") and ((pport.nmvlan <> \"\") or (pport.slavlan <> \"\"))";
			returnClause="pport.port_aid,pport.clfi";
	
			HashSet<Map<String, Object>> rset = nmgr.queryTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
	
			log.info("# of row = "+ rset.size());
			String port_aid="";
			boolean is2b = false;
	
			StringBuilder sbld = new StringBuilder();
			for ( Map<String, Object> rowdp : rset )
			{
				is2b = true;
				port_aid = (String)rowdp.get("pport.port_aid"); 
				clfi = (String)rowdp.get("pport.clfi"); 
				log.info("port_aid = " + port_aid +
					  ", clfi = " + clfi);
	
				if (port_aid != null && port_aid.length() > 0)
					sbld.append(" NNIPort=<"+port_aid+">");
				if (clfi != null && clfi.length() > 0)
					sbld.append(" NNICLFI=<"+clfi+">");
			}
			
			String info1 = (String) alarm.getCustomFieldValue(GFPFields.INFO1);
			alarm.setCustomFieldValue(GFPFields.INFO1, info1+" "+sbld.toString());
			log.info("info1 = "+ alarm.getCustomFieldValue(GFPFields.INFO1));
	
			if (is2b == false)
			{
				//// 2.(c)
				instance = deviceInstance;
				indexName=nmgr.CE_DEVICE_INDEX;
				returnName="device";
				matchClause="(device)-[:Composed_Of_PPort]->(pport)";
				whereClause="(pport.key <> \""+pportInstance+"\") and ((pport.remote_device_type = \"JUNIPER MX SERIES\") or (pport.remote_device_type = \"CIENA EMUX\"))";
				returnClause="pport.port_aid,pport.clfi";
		
				rset = nmgr.queryTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		
				if (log.isInfoEnabled())
				log.info("# of row = "+ rset.size());
				
				sbld = new StringBuilder();
				for ( Map<String, Object> rowdp : rset )
				{
					port_aid = (String)rowdp.get("pport.port_aid"); 
					clfi = (String)rowdp.get("pport.clfi"); 
					if (log.isInfoEnabled())
					log.info("port_aid = " + port_aid +
						  ", clfi = " + clfi);
		
					if (port_aid != null && port_aid.length() > 0)
						sbld.append(" NNIPort=<"+port_aid+">");
					if (clfi != null && clfi.length() > 0)
						sbld.append(" NNICLFI=<"+clfi+">");
				}
				
				info1 = (String) alarm.getCustomFieldValue(GFPFields.INFO1);
				alarm.setCustomFieldValue(GFPFields.INFO1, info1+" "+sbld.toString());
			}
		}
		if (log.isInfoEnabled())
		log.info("info1 = "+ alarm.getCustomFieldValue(GFPFields.INFO1));
		// (6)

		if (device_type != null && device_type.equals("CIENA EMUX") && remote_device_type != null && remote_device_type.equals("CIENA NTE"))
		{
			String deviceRole = null;
			// deviceRole = nmgr.queryDeviceRole(deviceInstance);
			deviceRole = nmgr.getNodeValue(deviceNode, "device_role");
			if (deviceRole != null && deviceRole.equals("LOOP"))
			{
				// setFlagsInField(alarm, "reason", "LOOP=<Y>");
				String flags = (String) alarm.getCustomFieldValue("flags");
				if (flags == null) flags = "";
				if (flags.equals(""))
					alarm.setCustomFieldValue("flags", "LOOP=<Y>");
				else
					alarm.setCustomFieldValue("flags", flags+" LOOP=<Y>");
				log.info("flags = " + (String) alarm.getCustomFieldValue("flags"));
			}
		}
		// (8)
		if (classification != null && classification.equals("EMT-CFO"))
		{
			String evc_name = nmgr.queryEVCNameforFBS(pportInstance);
			if (evc_name != null)
			{
				// setFlagsInField(alarm, "reason", "FBSID=<"+evc_name+">");
				String flags = (String) alarm.getCustomFieldValue("flags");
				if (flags == null) flags = "";
				if (!flags.contains("FBSID="))
				{
					if (flags.equals(""))
						alarm.setCustomFieldValue("flags", "FBSID=<"+evc_name+">");
					else
						alarm.setCustomFieldValue("flags", flags+" FBSID=<"+evc_name+">");
				}
				log.info("flags = " + (String) alarm.getCustomFieldValue("flags"));
				setFlagsInField(alarm, "info2", "FBSID=<"+evc_name+">");
			}
		}
		//
		// see email from Aaron dated Tue 9/2/2014 8:48 AM 
		// 
		if (("NFO-MOBILITY".equals(classification) || "NFO-MOBILITYUNI".equals(classification)) && "Y".equals(alarm.getMultiUni()) )
		{
			String info2 = (String) alarm.getCustomFieldValue(GFPFields.INFO2);
			// MultiUNI=<> may be enriched by preprocess.
			// check it to avoid duplication
			if (!info2.contains("MultiUNI"))
				alarm.setCustomFieldValue(GFPFields.INFO2, info2+" MultiUNI=<Y>");
		}
		log.info("info2 = "+ alarm.getCustomFieldValue(GFPFields.INFO2));

		// AD-GFP-Data-263779-140 (for EMT)
		// it is done in CienaExtendedLifeCycle

		// AD-GFP-Data-263779-120, 130 (infovista on pport) 
		enrichInfovista(alarm, false);
		
		// Gamma-200
		Map<String, Object> row = nmgr.getSDNInfo(pportInstance, alarm);
		if (row.size() > 0)
		{
			String clli = (String)row.get("evcNode.clli"); 
			// clci = (String)row.get("pport.clci"); 
			clci = nmgr.getNodeValue(pportNode, "clci");
			alarm.setCustomFieldValue("ticket-number", alarm.getCustomFieldValue("alert-id"));
			alarm.setCustomFieldValue("customer-name", "");
			alarm.setCustomFieldValue(GFPFields.CLASSIFICATION, "SDN-CFO");
			alarm.setCustomFieldValue(GFPFields.DOMAIN, "IPAG");
			alarm.setCustomFieldValue(GFPFields.NODE_NAME, clli);
			alarm.setCustomFieldValue(GFPFields.CIRCUIT_ID, clci);
			alarm.setCustomFieldValue("vrf-name", "");
			alarm.setCustomFieldValue("mcn", "");
			alarm.setCustomFieldValue("flags", "ProductType=<SDN-ETHERNET> DeviceSubRole=<EMT>");
			log.info("flags = " + (String) alarm.getCustomFieldValue("flags"));
		}
		//
		// see email from Aaron Bloss dated Tue 1/13/2015 4:51 pm
		// 
		log.info("classification = " + classification +
			", mpa_ind = " + mpa_ind +
			", remote_device_type = " + remote_device_type +
			", device_type = " + device_type +
			", mpa_connect_type = " + mpa_connect_type);

		if ("NTE-CFO".equals(classification) &&
			"Y".equals(mpa_ind) &&
			"".equals(remote_device_type) &&
			"CIENA NTE".equals(device_type))
		{
			String flags = alarm.getCustomFieldValue("flags");
			String fv = "MPA=<Y> MPAType=<"+mpa_connect_type+">";
			if (flags != null && flags.length() > 0)
				flags = flags+ " " + fv;
			else
				flags = fv;
			alarm.setCustomFieldValue("flags", flags);
			if ("GE TRUNK".equals(mpa_connect_type))
				alarm.setCustomFieldValue(GFPFields.CIRCUIT_ID, clfi);
			else
				alarm.setCustomFieldValue(GFPFields.CIRCUIT_ID, clci);
		}
	}

	public void enrichLinkDownAlarm(CienaAlarm alarm)
	{
		Node pportNode = alarm.getPportNode();
		Node deviceNode = alarm.getDeviceNode();

		String pportInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		boolean isUpdateSeverity = true;
/* below is enriched in PP
		String instance = pportInstance;
		String indexName=nmgr.CE_PPORT_INDEX;
		String returnName="pport";
		String matchClause=null;
		String whereClause=null;
		String returnClause="pport.device_type,pport.remote_device_ipaddr,pport.remote_device_type,pport.nmvlan,pport.slavlan,pport.remote_pport_key";
		
		Map<String, Object> row = nmgr.queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		boolean isUpdateSeverity = true;
		
		String nmvlan = "";
		String slavlan = "";
		String remote_device_ipaddr = "";
		String device_type = "";
		String remote_device_type = "";
		String remote_pport_key = "";
		if (row.size() > 0)
		{
			nmvlan = (String)row.get("pport.nmvlan"); 
			slavlan = (String)row.get("pport.slavlan"); 
			remote_device_ipaddr = alarm.getRemoteDeviceIpaddr();
			remote_device_type = alarm.getRemoteDeviceType();
			device_type = alarm.getDeviceType();

			remote_pport_key = (String)row.get("pport.remote_pport_key"); 
			if (log.isInfoEnabled())
			log.info("remote_deviceip = " + remote_device_ipaddr + " remote_pport_key = " + remote_pport_key);
		}
		else
			isUpdateSeverity = false;
*/
		String slavlan_nmvlan = alarm.getSlavlan_nmvlan();
		String remote_device_ipaddr = alarm.getRemoteDeviceIpaddr();
		String device_type = alarm.getDeviceType();
		String remote_device_type = alarm.getRemoteDeviceType();
		String remote_pport_key = alarm.getRemotePportKey();

		if ("".equals(slavlan_nmvlan) && "".equals(remote_device_ipaddr) && "".equals(remote_device_type) && "".equals(device_type) && "".equals(remote_pport_key))
			isUpdateSeverity = false;

		String deviceInstance = pportInstance.split("/")[0];

		if (log.isInfoEnabled())
		log.info("slavlan_nmvlan = " + slavlan_nmvlan);
		if ((slavlan_nmvlan != null && slavlan_nmvlan.length() > 0))
		{
			// below are used for WM only.  they are not sent to AM
			// ==> setCustomFieldValue() cannot be used.
			alarm.setSlavlan_nmvlan("Y");
			alarm.setSm_element(deviceInstance);
		}

		String remoteMultinni = nmgr.queryMultinni(remote_device_ipaddr);
		// String remoteDeviceType = queryDeviceTypeByDevice(remote_device_ipaddr);

		// update severity only when
		// (1) remove device type == "CIENA NTE" and
		// (2) remove device's nni == "Y"
		if (remote_device_type == null || !remote_device_type.equals("CIENA NTE") || remoteMultinni == null || !remoteMultinni.equals("Y"))
		{
			// System.out.println("severity is not changed.");
			if (log.isInfoEnabled())
			log.info("severity is not changed.");
			isUpdateSeverity = false;
		}

		String multinni = null;
		multinni = alarm.getMultiNni();

		alarm.setDevice_ip(deviceInstance);
		alarm.setRemote_pport_key(remote_pport_key);

		// System.out.println("multunni = " + multinni + " remote multunni = " + remoteMultinni);
		if (log.isInfoEnabled())
		log.info("multunni = " + multinni + " remote multunni = " + remoteMultinni);
		if (isUpdateSeverity == true)
		{
			// System.out.println("severity is set to 1. It may be set to 0 by rule engine.");
			if (log.isInfoEnabled())
			log.info("severity is set to 1. It may be set to 0 by rule engine.");
			// it may be changed to 0 in rule action
			alarm.setSeverity(1);
		}
		alarm.setUpdateSeverity(isUpdateSeverity);
		if (log.isInfoEnabled())
		log.info("isUpdateSeverity = " + isUpdateSeverity +
			", slavlan_nmvlan = " + alarm.getSlavlan_nmvlan() +
			", sm_element = " + alarm.getSm_element() +
			", remote_deviceip = " + remote_device_ipaddr +
			", instance = " + alarm.getInstance() +
			", device_ip = " + alarm.getDevice_ip() +
			", remote_pport_key = " + remote_pport_key +
			", remote_devicetype = " + remote_device_type +
			", multinni = " + multinni +
			", severity = " + alarm.getSeverity());
		// enrichVRFStatusLinkDownAlarm(alarm);
	}

	public void enrichOAMAlarm(CienaAlarm alarm)
	{
		Node pportNode = alarm.getPportNode();
		Node deviceNode = alarm.getDeviceNode();

		// preEnrichPPortAlarm(alarm); done in CienaExtendedLifeCycle

		// needed in rule engine
		String pportInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String deviceInstance = pportInstance.split("/")[0];
		String device_type = null;
		device_type = alarm.getDeviceType();
		// String remote_pport_key = nmgr.queryRemotePportByPport(pportInstance);
		String remote_pport_key = nmgr.getNodeValue(pportNode, "remote_pport_key");
		alarm.setDevice_ip(deviceInstance);
		alarm.setRemote_pport_key(remote_pport_key);
		if (log.isInfoEnabled())
		log.info("device_ip = " + deviceInstance + ", device_type = " + device_type + ", remote_pport_key = " + remote_pport_key);
	}

	public void enrichDeviceIP(CienaAlarm alarm)
	{
		// needed in rule engine
		String managedObjectInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String deviceInstance = managedObjectInstance.split("/")[0];
		alarm.setDevice_ip(deviceInstance);
	}

/*
	public boolean removeCFMAlarmFromMap(CienaAlarm alarm)
	{
		boolean inWM = true;
		String evcnodeInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		if (cfmMap.containsKey(evcnodeInstance))
		{
			cfmMap.remove(evcnodeInstance);
			nmgr.updateCDCInfo(evcnodeInstance, "");
		}
		else
			inWM = false;
		return inWM;
	}
*/
/*
	public void enrichColdStart(CienaAlarm alarm)
	{
		String evcnodeInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String vrf_name = nmgr.queryVRFName(evcnodeInstance);
		if (vrf_name != null)
			alarm.setCustomFieldValue("vrf-name", vrf_name);
		else
			alarm.setCustomFieldValue("vrf-name", "");
	}
*/

	//
	// evcnodeInstance = IP/vrfName != evc_name(field of EVCNode.node.csv)
	// evcInstance = vrfName = (vrf_name in EVCNode.node.csv)
	// 
	// in doc: evc refer to evcNode, vrf refer to evcInstance
	//
	public boolean enrichCFMAlarm(CienaAlarm alarm)
	{
		Node evcNode = alarm.getEvcNode();

		boolean inWM = true;
		String evcnodeInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String evcInstance = evcnodeInstance.split("/")[1];

		String instance=null;
		String indexName=null;
		String returnName=null;
		String matchClause=null;
		String whereClause=null;
		String returnClause=null;

/*
		String instance = evcnodeInstance;
		String indexName=nmgr.EVCNODE_INDEX;
		String returnName="evcnode";
		String matchClause=null;
		String whereClause=null;
		String returnClause="evcnode,evcnode.vrf_name,evcnode.unickt,evcnode.acnaban,evcnode.evc_name";
		
		Map<String, Object> row = nmgr.queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
*/

		String cdc_subscription_type = "";
		String vrf_name = "";
		alarm.setCustomFieldValue("vrf-name", "");
		// alarm.setIsFBSPtp(false);
		// alarm.setIsPtpMpt(false);
		// if (row.size() > 0)
		if (evcNode != null)
		{
			String classification = alarm.getCustomFieldValue(GFPFields.CLASSIFICATION);
			// below will be sent to AM.  they are not used for
			// correlation
			// Node nd = (Node) row.get("evcnode");

			vrf_name = nmgr.getNodeValue(evcNode, "vrf_name");
			log.info("vrf name = "+vrf_name);
			// if (vrf_name.equals("VPLS:INFRA_NTE_IPAG1") ||
			//    vrf_name.equals("VPLS:INFRA_NTE_IPACDM"))
			if ("VPLS:INFRA_NTE_IPAG1".equals(vrf_name) ||
			    "VPLS:INFRA_NTE_IPACDM".equals(vrf_name))
			{
				// this is in Feb 2014 requirement
				log.info("Suppress the CFM alarm: " + alarm.getIdentifier() + " as its vrf_name = " + vrf_name);
				inWM = false;
				return inWM;
			}

			alarm.setCustomFieldValue("vrf-name", vrf_name);
			String unickt = nmgr.getNodeValue(evcNode, "unickt");
			String acnaban = nmgr.getNodeValue(evcNode, "acnaban");
			String evc_name = nmgr.getNodeValue(evcNode, "evc_name");
			log.info("unickt = " + unickt + ", acnaban = " + acnaban + ", evc_name = " + evc_name);
			alarm.setCustomFieldValue(GFPFields.CLCI, unickt);
			alarm.setCustomFieldValue(GFPFields.CIRCUIT_ID, unickt);
			alarm.setCustomFieldValue(GFPFields.ACNABAN, acnaban);
			alarm.setCustomFieldValue(GFPFields.EVC_NAME, evc_name);
			// use below to set CDC_SUBSCRIPTION_TYPE for testing
			// purpose
			// nmgr.setNodeProperty(nd, NodeManager.CDC_SUBSCRIPTION_TYPE, "YYY"); // XXXXXXXX
			cdc_subscription_type = nmgr.getNodeProperty(evcNode, NodeManager.CDC_SUBSCRIPTION_TYPE, "");

			if (log.isInfoEnabled())
			log.info("cdc_subscription_type = " + cdc_subscription_type);
			alarm.setCustomFieldValue("subscriptionType", cdc_subscription_type);
			// per email from Shadi dated 4-1-2014, 3:59pm. below 
			// are not needed.
			// But it stays as EVCID and VRFname will be removed
			// from reason field by AM
			alarm.setCustomFieldValue(GFPFields.REASON, alarm.getCustomFieldValue(GFPFields.REASON)+" VRFname=<"+vrf_name+">");
			if (classification != null && classification.equals("NFO-MOBILITY"))
				alarm.setCustomFieldValue(GFPFields.COMPONENT, alarm.getCustomFieldValue(GFPFields.COMPONENT)+" EVCID=<"+evc_name+">");
			else
				alarm.setCustomFieldValue(GFPFields.REASON, alarm.getCustomFieldValue(GFPFields.REASON)+" EVCID=<"+evc_name+">");
		}
		// log.info("isFBSPtp = " + alarm.getIsFBSPtp() + ". isPtpMpt = " + alarm.getIsPtpMpt());

		/////////////////////////////////
/*
		instance = evcInstance;
		indexName=nmgr.EVC_INDEX;
		returnName="evc";
		matchClause=null;
		whereClause=null;
		returnClause="evc";
		
		Map<String, Object> row = nmgr.queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		String sent_to_cdc = null;
		Node evc_node = null;
		if (row.size() > 0)
		{
			evc_node = (Node) row.get("evc");
			sent_to_cdc = nmgr.getNodeProperty(evc_node, NodeManager.SENT_TO_CDC, "false");
			// System.out.println("sent_to_cdc = " + sent_to_cdc);
			if (log.isInfoEnabled())
			log.info("sent_to_cdc = " + sent_to_cdc);
		}

		if (log.isInfoEnabled())
		log.info("cdc_subscription_type = " + cdc_subscription_type +
			", sent_to_cdc = " + sent_to_cdc +
			", severity = " + alarm.getSeverity());
		if (cdc_subscription_type != null && 
			cdc_subscription_type.length() > 0 && sent_to_cdc != null &&
		((sent_to_cdc.equals("false") && alarm.getSeverity()<4) ||
		(sent_to_cdc.equals("true") && alarm.getSeverity()==4)))
		{
			// TJtodo: send to CDC engine.  check with Jason
			// about the connection.
			// API(alarm, "R", "0", "");
			// System.out.println("Sent to CDC = " + alarm.getIdentifier());
			String axml = alarm.toXMLString();
			axml = axml.replaceAll("\\n", " ");
			if (log.isInfoEnabled())
			log.info("Sending alarm to CDC engine = " + axml);
			// send to NOM which will sent alarms to CDC engine
			try {
			if (scenario == null)
			{
				if (log.isInfoEnabled())
				log.info("scenario is null.");
			}
			else
				service_util.forwardAlarmToMobilityCDC(scenario,alarm);
			} catch (Exception e) {
				if (log.isErrorEnabled())
				log.error("Failed to send to CDC", e);
			}
			
			nmgr.setNodeProperty(evc_node, NodeManager.SENT_TO_CDC, "true");

			String alert_id = alarm.getCustomFieldValue(GFPFields.ALERT_ID);
			String be_time_stamp = alarm.getCustomFieldValue("be_time_stamp");
			if (log.isInfoEnabled())
			log.info("alert_id = " + alert_id + ", be_time_stamp = " + be_time_stamp);
			String CDCInfo = "CFMAlertKey=<"+alert_id+"-IPAG01> CFMTimeStamp=<"+be_time_stamp+">";
			if (log.isInfoEnabled())
			log.info("CDCInfo = " + CDCInfo);
			alarm.setCustomFieldValue("cdc-info", CDCInfo);
			nmgr.updateCDCInfo(evcnodeInstance, CDCInfo);
		}
		else
		{
			// System.out.println("Don't send to CDC.");
			if (log.isInfoEnabled())
			log.info("Don't send to CDC.");
		}
*/
		/////////////////////////////////

/*
		instance = evcInstance;
		indexName=nmgr.EVC_INDEX;
		returnName="evc";
		matchClause=null;
		whereClause=null;
		returnClause="evc";
		row = nmgr.queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
		if (row.size() > 0)
		{
			Node nd = (Node) row.get("evc");
			// below values are populated to DB by DyingGasp alarm
			String ciena_dying_gasp = nmgr.getNodeProperty(nd, NodeManager.CIENA_DYING_GASP, "");
			String ciena_dying_gasp_time = nmgr.getNodeProperty(nd, NodeManager.CIENA_DYING_GASP_TIME, "0");

			// below values are populated to DB by LinkDown alarm
			String ciena_link_down = nmgr.getNodeProperty(nd, NodeManager.CIENA_LINK_DOWN, "");
			String ciena_link_down_time = nmgr.getNodeProperty(nd, NodeManager.CIENA_LINK_DOWN_TIME, "0");

			String juniper_link_down = nmgr.getNodeProperty(nd, NodeManager.JUNIPER_LINK_DOWN, "");
			String juniper_link_down_time = nmgr.getNodeProperty(nd, NodeManager.JUNIPER_LINK_DOWN_TIME, "0");

			if (log.isInfoEnabled())
			log.info("ciena_dying_gasp = " + ciena_dying_gasp +
				", ciena_dying_gasp_time = " + ciena_dying_gasp_time +
				", ciena_link_down = " + ciena_link_down +
				", ciena_link_down_time = " + ciena_link_down_time +
				", juniper_link_down = " + juniper_link_down +
				", juniper_link_down_time = " + juniper_link_down_time +
				", current time = " + System.currentTimeMillis()/1000);

			if ((ciena_dying_gasp.equals("true") &&
				System.currentTimeMillis()/1000-Double.parseDouble(ciena_dying_gasp_time) < 180) ||
				(ciena_link_down.equals("true") &&
				System.currentTimeMillis()/1000-Double.parseDouble(ciena_link_down_time) < 180) ||
				(juniper_link_down.equals("true") &&
				System.currentTimeMillis()/1000-Double.parseDouble(juniper_link_down_time) < 180))
			{
				if (log.isInfoEnabled())
				log.info("Suppress the CFM alarm: " + alarm.getIdentifier());
				inWM = false;
			}
		}
*/
		// if (inWM == true)
			// cfmMap.put(evcnodeInstance, alarm);
		return inWM;
	}
	// HLD-GFP-Data-263779-70,80,90(infovista alarm):isInfovistaAlarm=true
	// HLD-GFP-Data-263779-120,130(ciena pport alarm):isInfovistaAlarm=false
	public void enrichInfovista(CienaAlarm alarm, boolean isInfovistaAlarm)
	{
		Node pportNode = alarm.getPportNode();
		Node deviceNode = alarm.getDeviceNode();

		boolean isproceed = true;
		if (isInfovistaAlarm == true)
		{
			// set the default value for "classification" and "domain"
			alarm.setCustomFieldValue(GFPFields.CLASSIFICATION, "NFO-EMUX");
			alarm.setCustomFieldValue(GFPFields.DOMAIN, "EMUX-PMOS");
		}

		String pportInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String deviceInstance = pportInstance.split("/")[0];

		// HLD-70,80(infovista)
		// HLD-120,130(ciena pport alarm)
/*
		String instance=deviceInstance;
		String indexName=nmgr.CE_DEVICE_INDEX;
		String returnName="device";
		String matchClause="(device)-[:Composed_Of_PPort]->(pport)-[:Composed_Of]->(evcNode)";

		String whereClause="device.device_sub_role=\"EMT\" and pport.key=\""+pportInstance+"\" and pport.device_type=\"CIENA EMUX\" and pport.remote_pport_key=\"\" and ((evcNode.product_type=\"OEM\") or (evcNode.product_type=\"OEM-AC\"))";
		String returnClause="pport.mobility_ind_uni,device.device_model,pport.port_aid,evcNode.clli";

		Map<String, Object> row = nmgr.queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
*/

		Map<String, Object> row = nmgr.getOEMInfo(pportInstance, alarm);
		if (row.size() == 0)
			log.info("No OEM enrichment.");
		else
		{
			isproceed = false;
			// String mobility_ind = (String)row.get("pport.mobility_ind_uni");
			// String device_model = (String)row.get("device.device_model");
			// String port_aid = (String)row.get("pport.port_aid");
			String clli = (String)row.get("evcNode.clli");
			String device_model = nmgr.getNodeValue(deviceNode, "device_model");
			String mobility_ind = nmgr.getNodeValue(pportNode, "mobility_ind_uni");
			String port_aid = nmgr.getNodeValue(pportNode, "port_aid");

			log.info("clli = " + clli + ", device_model = " + device_model + ", mobility_ind = " + mobility_ind + ", port_aid = " + port_aid);

			/* per Shadi email on 6-7-14, 4:08pm.  component is BAU.
			// preprocessor already enriches below plus others.
			// comment below out to avoid overriding pp's enrichment
			if ("N".equals(mobility_ind)||"".equals(mobility_ind)||"Y".equals(mobility_ind))
			{
				alarm.setCustomFieldValue(GFPFields.COMPONENT, "deviceType=<CIENA EMUX> deviceModel=<"+device_model+"> PortAID=<"+port_aid+">");
				// all BAU are set by preprocess
			}
			*/
			if ("N".equals(mobility_ind)||"".equals(mobility_ind))
			{
				if (isInfovistaAlarm == true)
				{
					alarm.setCustomFieldValue(GFPFields.CLASSIFICATION, "EMT-IV-CFO");
					alarm.setCustomFieldValue(GFPFields.DOMAIN, "IPAG");
					alarm.setCustomFieldValue(GFPFields.NODE_NAME, clli);
					alarm.setCustomFieldValue("be_time_stamp", ""+System.currentTimeMillis()/1000);
					alarm.setCustomFieldValue("ticket-number", alarm.getCustomFieldValue("alert-id"));
				}
				else
				{
					alarm.setCustomFieldValue(GFPFields.CLASSIFICATION, "NTE-CFO");
					alarm.setCustomFieldValue(GFPFields.DOMAIN, "IPAG");
					String flags = alarm.getCustomFieldValue("flags");
					if (flags != null && flags.length() > 0)
						flags = flags+ " DeviceSubRole=<EMT>";
					else
						flags = "DeviceSubRole=<EMT>";
					alarm.setCustomFieldValue("flags", flags);
					log.info("flags = " + (String) alarm.getCustomFieldValue("flags"));
				}
			}
			else if ("Y".equals(mobility_ind))
			{
				if (isInfovistaAlarm == true)
				{
					alarm.setCustomFieldValue(GFPFields.CLASSIFICATION, "NFO-MOBILITYUNI");
					alarm.setCustomFieldValue(GFPFields.DOMAIN, "EMUX-PMOS");
				}
				else
				{
					alarm.setCustomFieldValue(GFPFields.CLASSIFICATION, "NFO-MOBILITYUNI");
					alarm.setCustomFieldValue(GFPFields.DOMAIN, "EMT");
				}
			}
		}

/* continue as ciena pport alarm needs enrichment for product type = FBS per
	HLD 257826-110
		if (isInfovistaAlarm == false)
			return;		// Ciean Pport alarm stop here
*/
		if (isproceed == false)
			return;

		// HLD-90(infovista)
/*
		instance=deviceInstance;
		indexName=nmgr.CE_DEVICE_INDEX;
		returnName="device";
		matchClause="(device)-[:Composed_Of_PPort]->(pport)-[:Composed_Of]->(evcNode)";

		whereClause="device.device_sub_role=\"EMT\" and pport.mobility_ind_uni=\"Y\" and pport.key=\""+pportInstance+"\" and pport.device_type=\"CIENA EMUX\" and pport.remote_device_type=\"CIENA NTE\"";
		returnClause="device.device_model,pport.port_aid,evcNode.key";

		row = nmgr.queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
*/
		if (isInfovistaAlarm == true)
		{
			// HLD-90(infovista) for Infovista only
			row = nmgr.getInfovistaInfo(pportInstance, alarm);

			if (row.size() == 0)
				log.info("No infovista enrichment.");
			else
			{
				isproceed = false;
				String evcNodeInstance = (String)row.get("evcNode.key");
				if (evcNodeInstance == null || evcNodeInstance.equals(""))
				{
					String device_model = nmgr.getNodeValue(deviceNode, "device_model");
					String port_aid = nmgr.getNodeValue(pportNode, "port_aid");
					alarm.setCustomFieldValue(GFPFields.CLASSIFICATION, "NFO-MOBILITY");
					alarm.setCustomFieldValue(GFPFields.DOMAIN, "EMUX-PMOS");
					// per Shadi email on 6-7-14, 4:08pm.  
					// component is BAU. preprocessor already 
					// enriches below plus others. comment below 
					// out to avoid overriding pp's enrichment
					// alarm.setCustomFieldValue(GFPFields.COMPONENT, "deviceType=<CIENA EMUX> deviceModel=<"+device_model+"> PortAID=<"+port_aid+">");
				}
			}
		}

		if (isproceed == false)
			return;

		// HLD-40(from Gayathri's email)
/*
		instance=deviceInstance;
		indexName=nmgr.CE_DEVICE_INDEX;
		returnName="device";
		matchClause="(device)-[:Composed_Of_PPort]->(pport)-[:Composed_Of]->(evcNode)";

		whereClause="device.device_sub_role=\"EMT\" and pport.key=\""+pportInstance+"\" and pport.device_type=\"CIENA EMUX\" and pport.remote_pport_key=\"\" and pport.product_type=\"FBS\" and evcNode.key <> \"\"";
		returnClause="evcNode.product_type,evcNode.clli";

		row = nmgr.queryOneRowTopoAttributes(instance, indexName, returnName, matchClause, whereClause, returnClause);
*/
		row = nmgr.getFBSInfo(pportInstance, alarm);
		if (row.size() == 0)
			log.info("No FBS enrichment.");
		else
		{
			isproceed = false;
			String evc_product_type = (String)row.get("evcNode.product_type");
			String evc_name = (String)row.get("evcNode.evc_name");
			String clli = (String)row.get("evcNode.clli");
	
			if (isInfovistaAlarm == true)
			{
	
				alarm.setCustomFieldValue(GFPFields.CLASSIFICATION, "EMT-IV-CFO");
				alarm.setCustomFieldValue(GFPFields.DOMAIN, "IPAG");
	
				if ("FBS".equals(evc_product_type))
				{
					// setFlagsInField(alarm, "reason", "ProductType=<FBS> FBSID=<"+evc_name+">");
					String flags = (String) alarm.getCustomFieldValue("flags");
					if (flags == null) flags = "";

					String pf = "ProductType=<FBS> FBSID=<"+evc_name+">";
					String newflags = getNewFlag(flags, pf);
					log.info("new flags = " + newflags);
					alarm.setCustomFieldValue("flags", newflags);
					log.info("flags = " + (String) alarm.getCustomFieldValue("flags"));
				}
			}
			// HLD 257826-110
			else if ("FBS".equals(evc_product_type))
			{
				String alert_id = alarm.getCustomFieldValue(GFPFields.ALERT_ID);
				String device_type = nmgr.getNodeValue(deviceNode, "device_type");
				String device_model = nmgr.getNodeValue(deviceNode, "device_model");
				String port_aid = nmgr.getNodeValue(pportNode, "port_aid");
				String component = "deviceType=<"+device_type+"> deviceModel=<"+device_model+"> PortAID=<"+port_aid+">";
				log.info("component = " + component);

				alarm.setCustomFieldValue(GFPFields.CLASSIFICATION, "EMT-CFO");
				alarm.setCustomFieldValue(GFPFields.DOMAIN, "IPAG");
				alarm.setCustomFieldValue("node-name", clli);
				alarm.setCustomFieldValue(GFPFields.COMPONENT, component);
				alarm.setCustomFieldValue("ticket-number", alert_id);
				alarm.setCustomFieldValue(GFPFields.CIRCUIT_ID, "");
				String flags = alarm.getCustomFieldValue("flags");
				if (flags == null) flags = "";

				String pf = "ProductType=<FBS> FBSID=<"+evc_name+">";
				String newflags = getNewFlag(flags, pf);
				log.info("new flags = " + newflags);
				alarm.setCustomFieldValue("flags", newflags);
				log.info("flags = " + (String) alarm.getCustomFieldValue("flags"));
			}
		}
		if (isInfovistaAlarm == true)
		{
			// Gamma-300
			row = nmgr.getSDNInfo(pportInstance, alarm);
			if (row.size() > 0)
			{
				String clli = (String)row.get("evcNode.clli"); 
				// String device_model = (String)row.get("device.device_model"); 
				// String port_aid = (String)row.get("pport.port_aid"); 
				String device_model = nmgr.getNodeValue(deviceNode, "device_model");
				String port_aid = nmgr.getNodeValue(pportNode, "port_aid");
	
				alarm.setCustomFieldValue("ticket-number", alarm.getCustomFieldValue("alert-id"));
				alarm.setCustomFieldValue(GFPFields.CLASSIFICATION, "EMT-IV-CFO");
				alarm.setCustomFieldValue(GFPFields.DOMAIN, "IPAG");
				alarm.setCustomFieldValue(GFPFields.NODE_NAME, clli);
				// per Shadi email on 6-7-14, 4:08pm.  
				// component is BAU. preprocessor already 
				// enriches below plus others. comment below 
				// out to avoid overriding pp's enrichment
				// alarm.setCustomFieldValue(GFPFields.COMPONENT, "deviceType=<CIENA EMUX> deviceModel=<"+device_model+"> PortAID=<"+port_aid+">");
			}
		}
	}

	public void setFlagsInField(CienaAlarm alarm, String targetfld, String value)
	{
		String ftokenx = "FLAGS={";
		String ftoken = "FLAGS=\\{";

		String fieldValue = alarm.getCustomFieldValue(targetfld);
		if (fieldValue == null || "".equals(fieldValue))
			alarm.setCustomFieldValue(targetfld, ftokenx+value+"}");
		else
		{
			if (fieldValue.indexOf(ftokenx) == -1)
				alarm.setCustomFieldValue(targetfld, fieldValue+ftokenx+value+"}");
				
			else
			{
				String ary[] = fieldValue.split(ftoken);
				alarm.setCustomFieldValue(targetfld, ary[0]+ftokenx+value+" "+ary[1]);
			}
		}
		
	}

    public static String getNewFlag(String x, String prodTypeFBSID)
    {
	    if ("".equals(x))
		return prodTypeFBSID;
            int idx = x.indexOf("FBSID=<");
            if (idx != -1)
            {
                    String y = x.substring(0, idx);
                    String z = x.substring(idx+7);
                    int idxe = z.indexOf(">");
                    if (idxe != -1)
                    {
			String newflags = y+prodTypeFBSID+z.substring(idxe+1);
                        return newflags;
                    }
            }
            return x+" "+prodTypeFBSID;
    }
}
