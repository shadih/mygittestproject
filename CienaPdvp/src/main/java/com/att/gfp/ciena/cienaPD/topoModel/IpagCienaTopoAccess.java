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
import com.hp.uca.expert.topology.TopoAccess;

public class IpagCienaTopoAccess extends TopoAccess 
{
	private static Logger log = LoggerFactory.getLogger(IpagCienaTopoAccess.class);
	private static NodeManager nmgr;

	public IpagCienaTopoAccess()
	{
		nmgr = new NodeManager();
	}

	public void enrichComponent(CienaAlarm syntheticA, CienaAlarm ta)
	{
		Node dn = ta.getFaultyEndDeviceNode();
		Node en = ta.getFaultyEndEvcNode();

		String paid = NodeManager.getNodeValue(en, "port_aid");
		String device_type = NodeManager.getNodeValue(dn, "device_type");
		String device_model = NodeManager.getNodeValue(dn, "device_model");
		String component = "deviceType=<"+device_type+"> deviceModel=<"+device_model+"> PortAID=<"+paid+">";
		syntheticA.setCustomFieldValue(GFPFields.COMPONENT, component);
	}

	public void enrichCircuitID_CLCI(CienaAlarm syntheticA, CienaAlarm ta)
	{
		Node pn = ta.getFaultyEndPportNode();

		String clci = NodeManager.getNodeValue(pn, "clci");
		String clfi = NodeManager.getNodeValue(pn, "clfi");
		String mpa_connect_type = NodeManager.getNodeValue(pn, "mpa_connect_type");
		if ("GE TRUNK".equals(mpa_connect_type))
			syntheticA.setCustomFieldValue(GFPFields.CIRCUIT_ID, clfi);
		else
			syntheticA.setCustomFieldValue(GFPFields.CIRCUIT_ID, clci);
		syntheticA.setCustomFieldValue(GFPFields.CLCI, clci);
	}

	// alarm is the alarming end
	public String createFlags(CienaAlarm alarm, String device_sub_role, boolean bothends, String classification, boolean isOneAlarm)
	{
		String flags = "MptCFM=<N>";;

		Node dn=null;
		Node pn=null;
		Node en=null;
		if (isOneAlarm)
		{
			dn = alarm.getFaultyEndDeviceNode();
			pn = alarm.getFaultyEndPportNode();
			en = alarm.getFaultyEndEvcNode();
		}
		else
		{
			// for PTP two alarms(A, B): 
			// B is A's faulty end. A is B's faulty end
			// ==> below is also faulty end information
			dn = alarm.getDeviceNode();
			pn = alarm.getPportNode();
			en = alarm.getEvcNode();
		}

		String mpa_ind = NodeManager.getNodeValue(dn, "mpa_ind");
		String mpa_connect_type = NodeManager.getNodeValue(pn, "mpa_connect_type");
		String prodType = NodeManager.getNodeValue(en, "product_type");

		if ("Y".equals(mpa_ind))
			flags += " MPA=<Y>";
		if (classification.contains("SDN"))
			flags += " ProductType=<"+prodType+">";
		if (bothends)
			flags += " PtpCFM=<BOTH>";
		else
		{
			flags += " MPAType=<"+mpa_connect_type+">";
			if ("EMT".equals(device_sub_role))
				flags += " DeviceSubRole=<EMT>";
		}
		return flags;
	}

	// called by ONE alarm for Telco only
	public void enrichClci_Clfi(CienaAlarm syntheticA, CienaAlarm ta)
	{

		Node pn = ta.getFaultyEndPportNode();

		String clci = NodeManager.getNodeValue(pn, "clci");
		String clfi = NodeManager.getNodeValue(pn, "clfi");
		String mpa_connect_type = NodeManager.getNodeValue(pn, "mpa_connect_type");

		// email from Rick Mon 9/29/2014 7:36 PM
		if ("DIRECT LEC".equals(mpa_connect_type) || "".equals(mpa_connect_type) || mpa_connect_type == null)
			syntheticA.setCustomFieldValue(GFPFields.CLCI, clci);
		else if ("GE TRUNK".equals(mpa_connect_type))
			syntheticA.setCustomFieldValue(GFPFields.CLFI, clfi);
	}

	// called by PTP Two alarm only
	// alarm is the alarming end
	public String createInfo(CienaAlarm alarm, boolean isCC)
	{
		// for PTP two alarms(A, B): 
		// B is A's faulty end. A is B's faulty end
		// ==> below is also faulty end information
		Node dn = alarm.getDeviceNode();
		Node pn = alarm.getPportNode();
		Node en = alarm.getEvcNode();

		String device_sub_role = NodeManager.getNodeValue(dn,"device_sub_role");
		String clli = NodeManager.getNodeValue(en,"clli");
		String port_aid = NodeManager.getNodeValue(en,"port_aid");
		String info = "CLLI=<"+clli+"> PortAID=<"+port_aid+">";

		String clci = NodeManager.getNodeValue(pn,"clci");
		String clfi = NodeManager.getNodeValue(pn,"clfi");
		String mpa_connect_type = NodeManager.getNodeValue(pn,"mpa_connect_type");

		if ("GE TRUNK".equals(mpa_connect_type))
			info += " CLFI=<"+clfi+">";
		else
			info += " CLCI=<"+clci+">";
		info += " MPAType=<"+mpa_connect_type+">";
		if (!isCC)
		{
			if ("EMT".equals(device_sub_role))
				info += " DeviceSubRole=<EMT>";
		}
		return info;
	}
}
