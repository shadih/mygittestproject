package com.att.gfp.ciena.cienaPD;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.ciena.cienaPD.topoModel.IpagCienaTopoAccess;
import com.att.gfp.ciena.cienaPD.topoModel.MptData;
import com.att.gfp.ciena.cienaPD.topoModel.NodeManager;
import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.helper.GFPFields;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.x733alarm.CustomField;
import com.hp.uca.expert.x733alarm.CustomFields;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.scenario.ScenarioThreadLocal;
import com.hp.uca.expert.vp.pd.services.PD_Service_Group;
import org.neo4j.graphdb.Node;

@XmlRootElement
public class CienaAlarm extends EnrichedAlarm {

	private static Logger log = LoggerFactory.getLogger(CienaAlarm.class);
	private static IpagCienaTopoAccess topo = new IpagCienaTopoAccess();
	
	/**
	 * Alarm extension attributes
	 */
	// private AlarmState alarmState;
	// private int severity; 
	
	private boolean CFMPtpMptAlarm = false;

	private Boolean isSent = false;
	private Boolean isClear = false;
	// private Boolean isSuppressed = false;	// already defined in EnrichedAlarm class
	private Boolean inPool = false;   
	private Group groupPtpMpt = null;
	private boolean isSubAlarm = false;
	private Node pportNode;
	private Node deviceNode;
	private Node evcNode;
	private Node faultyEndDeviceNode;
	private Node faultyEndPportNode;
	private Node faultyEndEvcNode;

/*
	private Boolean sentAsSubAlarm = false;
	private Boolean sentAsTriggerAlarm = false;
	private Boolean suppressed = false;   
*/
/*
	static class syntheticState {
		public CienaAlarm syna;
		public boolean isClearSent;
		public syntheticState(CienaAlarm syna)
		{
			this.syna = syna;
			this.isClearSent = false;
		}
	}
*/
	static HashMap<String, HashSet<CienaAlarm>> synaMap = new HashMap<String, HashSet<CienaAlarm>>();

	/**
	 * CienaAlarm Constructor
	 */
	public CienaAlarm() {
		super();
		inPool = false;
		groupPtpMpt = null;
		isSubAlarm = false;
		isSent = false;
		isClear = false;
		// isSuppressed = false;
/*
		suppressed = false;
		sentAsSubAlarm = false;
		sentAsTriggerAlarm = false;
*/
	}

	public CienaAlarm(Alarm alarm) throws Exception {
		super(alarm);
		inPool = false;
		groupPtpMpt = null;
		isSubAlarm = false;
		isSent = false;
		isClear = false;
		// isSuppressed = false;  it is set to false in EnrichedAlarm
/*
		suppressed = false;
		sentAsSubAlarm = false;
		sentAsTriggerAlarm = false;
*/
	}

	/**
	 * CienaAlarm Copy Constructor.
	 * 
	 * This constructor constructs an Extended Alarm and uses the Alarm provided as
	 * parameter to set all the standard alarm fields
	 * @throws Exception 
	 */
	public CienaAlarm(CienaAlarm alarm) throws Exception {
		this ((Alarm) alarm);
		inPool = alarm.inPool;
		groupPtpMpt = alarm.groupPtpMpt;
		isSubAlarm = alarm.isSubAlarm;

		isSent = alarm.isSent;
		isClear = alarm.isClear;
		// isSuppressed = alarm.isSuppressed;
/*
		suppressed = alarm.suppressed;
		sentAsSubAlarm = alarm.sentAsSubAlarm;
		sentAsTriggerAlarm = alarm.sentAsTriggerAlarm;
*/
	}

	public CienaAlarm clone() throws CloneNotSupportedException {
		CienaAlarm newAlarm = (CienaAlarm) super.clone();

		newAlarm.inPool = this.inPool;
		newAlarm.groupPtpMpt = this.groupPtpMpt;
		newAlarm.isSubAlarm = this.isSubAlarm;

		newAlarm.isSent = this.isSent;		
		newAlarm.isClear = this.isClear;
		// newAlarm.isSuppressed = this.isSuppressed;
		
		return newAlarm;
	}
	public void setPportNode(Node pportNode) {
		this.pportNode = pportNode;
	}
	
	public Node getPportNode() {
		return this.pportNode;
	}

	public void setDeviceNode(Node deviceNode) {
		this.deviceNode = deviceNode;
	}
	
	public Node getDeviceNode() {
		return this.deviceNode;
	}

	public void setEvcNode(Node evcNode) {
		this.evcNode = evcNode;
	}
	
	public Node getEvcNode() {
		return this.evcNode;
	}

	public void setFaultyEndDeviceNode(Node faultyEndDeviceNode) {
		this.faultyEndDeviceNode = faultyEndDeviceNode;
	}
	
	public Node getFaultyEndDeviceNode() {
		return this.faultyEndDeviceNode;
	}

	public void setFaultyEndPportNode(Node faultyEndPportNode) {
		this.faultyEndPportNode = faultyEndPportNode;
	}
	
	public Node getFaultyEndPportNode() {
		return this.faultyEndPportNode;
	}

	public void setFaultyEndEvcNode(Node faultyEndEvcNode) {
		this.faultyEndEvcNode = faultyEndEvcNode;
	}
	
	public Node getFaultyEndEvcNode() {
		return this.faultyEndEvcNode;
	}

///
	public Boolean getInPool() {
		return inPool;
	}

	public void setInPool(Boolean inPool) {
		this.inPool = inPool;
	}
///
	public Boolean getIsSent() {
		return isSent;
	}

	public void setGroupPtpMpt(Group groupPtpMpt) {
		this.groupPtpMpt = groupPtpMpt;
	}
	
	public void setIsSubAlarm(boolean isSubAlarm) {
		this.isSubAlarm = isSubAlarm;
	}
	
	public boolean getIsSubAlarm() {
		return this.isSubAlarm;
	}
	public void setIsSent(boolean isSent) {
		this.isSent = isSent;
	}
///
	public Boolean getIsClear() {
		return isClear;
	}

	public void setIsClear(Boolean isClear) {
		this.isClear = isClear;
	}
///
	public Boolean getCanSend() {
		log.info("getCanSend(): alarm = " + getIdentifier() + ", isClear = " + isClear+ ", isSent = " + isSent+ ", isSuppressed = " + isSuppressed());
		return (!isClear && !isSent && !isSuppressed());
	}
///
	public Boolean getCanProcess() {
		log.info("getCanProcess(): alarm = " + getIdentifier() + ", isClear = " + isClear+ ", isSent = " + isSent+ ", isSuppressed = " + isSuppressed());
		return (!isClear && !isSuppressed());
	}
///
	public void setCFMPtpMptAlarm(boolean CFMPtpMptAlarm){
		this.CFMPtpMptAlarm =  CFMPtpMptAlarm;
	}

	public boolean isCFMPtpMptAlarm() {
		return CFMPtpMptAlarm;
	}
///

	public void setSeverity(int severity) {
		setCustomFieldValue("severity", Integer.toString(severity));
		switch(severity) {
	    case 0:
		setPerceivedSeverity(PerceivedSeverity.CRITICAL);
	    	break;
	    case 1:
		setPerceivedSeverity(PerceivedSeverity.MAJOR);
	    	break;
	    case 2:
		setPerceivedSeverity(PerceivedSeverity.MINOR);
	    	break;
	    case 3:
		setPerceivedSeverity(PerceivedSeverity.WARNING);
	    	break;
	    case 5:
		setPerceivedSeverity(PerceivedSeverity.INDETERMINATE);
	    	break;
	    case 4:
		setPerceivedSeverity(PerceivedSeverity.CLEAR);
	    	break;
	    default:
		setPerceivedSeverity(PerceivedSeverity.INDETERMINATE);
	    	break;
		}	
	}

	// clearCustomFields() needs to be called to clean up synthetic alarm,
	// otherwise those fields that are not required in the synthetic 
	// alarm will be included in the synthetic alarm with value of the
	// alarming end. it is confusing as the synthetic alarm contains
	// the information of both faulty end and alarming end.
	//
	public static void clearCustomFields(CienaAlarm a)
	{
		CustomFields cflds = a.getCustomFields();
		List<CustomField> customFields = cflds.getCustomField();
		for (CustomField cf:customFields)
			a.setCustomFieldValue(cf.getName(), "");
	}

	public static boolean getIsCC(HashSet<CienaAlarm> cset)
	{
		boolean isCC = true;

		Iterator i = cset.iterator();
		while(i.hasNext())
		{
			CienaAlarm ca = (CienaAlarm)i.next();
			String eventKey = ca.getCustomFieldValue(GFPFields.EVENT_KEY);
			// ciena CFM can be sent to RUBY-TELCO or RUBY-CC
			// adtran is always sent to RUBY-CC
			// tj: isCC = true when at least one alarm's 
			//	isMobility() is false or at lease one alarm
			//	is adtran
			if (eventKey.equals("50002/100/52"))
			{
				String evcnodeInstance = ca.getOriginatingManagedEntity().split(" ")[1];
				String deviceInstance = evcnodeInstance.split("/")[0];
				log.info("check if it is mobility.");
				if (NodeManager.isMobility(ca))
				{
					log.info("it is mobility");
					isCC = false; // send to RUBY-TELCO
				}
				else
				{
					isCC = true;
					log.info("isCC = " + isCC);
					return isCC; // send to RUBY-CC
				}
			}
			else
			{
				isCC = true;
				log.info("isCC = " + isCC);
				return isCC;
			}
		}
		log.info("isCC = " + isCC);
		return isCC;
	}

	public static CienaAlarm createRubyOneAlarmPtp(boolean isCC, CienaAlarm syntheticA, CienaAlarm ta)
	{
		log.info("createRubyOneAlarmPtp runs.");
		String alert_id = syntheticA.getCustomFieldValue(GFPFields.ALERT_ID);
		String purgeIntvl = syntheticA.getCustomFieldValue("purge-interval");
		String fe_time_stamp = syntheticA.getCustomFieldValue(GFPFields.FE_TIME_STAMP);
		String be_time_stamp = syntheticA.getCustomFieldValue("be_time_stamp");
		String eventKey = syntheticA.getCustomFieldValue(GFPFields.EVENT_KEY);
		String eventName = syntheticA.getCustomFieldValue("EventName");
		// String info1 = syntheticA.getCustomFieldValue(GFPFields.INFO1);
		String info1 = "";
		String reason = syntheticA.getCustomFieldValue(GFPFields.REASON);
              	String sequenceNumber = syntheticA.getCustomFieldValue(GFPFields.SEQNUMBER);
		String classification = syntheticA.getCustomFieldValue(GFPFields.CLASSIFICATION);
		String G2Suppress = syntheticA.getCustomFieldValue("G2Suppress");
		String cdcSptType = syntheticA.getCustomFieldValue("cdc-subscription-type");
		String alarmObjectType = syntheticA.getCustomFieldValue("sm-class");
		clearCustomFields(syntheticA);
		syntheticA.setCustomFieldValue(GFPFields.CLASSIFICATION, classification);
		if (alarmObjectType != null)
			syntheticA.setCustomFieldValue("sm-class", alarmObjectType);
		syntheticA.setIdentifier(syntheticA.getIdentifier()+"_Synthetic");
		syntheticA.setCustomFieldValue(GFPFields.ALERT_ID, alert_id+"_Synthetic");
		syntheticA.setCustomFieldValue("purge-interval", purgeIntvl);

		// find faulty end
		String alarmEvcnodeInstance = syntheticA.getOriginatingManagedEntity().split(" ")[1];
		String vrfName = alarmEvcnodeInstance.split("/")[1]; // vrfname
		String faultyEvcnodeInstance = NodeManager.getFaultyend(ta, alarmEvcnodeInstance, vrfName);

		// String ip = alarmEvcnodeInstance.split("/")[0];
		// String vrfName = alarmEvcnodeInstance.split("/")[1];

		if (faultyEvcnodeInstance == null)
		{
			log.info("No faulty end.  Drop it.");
			return null;	// no faulty end, drop it
		}

		// String evcName = NodeManager.queryEVCName(faultyEvcnodeInstance);
		syntheticA.setPerceivedSeverity(PerceivedSeverity.MAJOR);
		syntheticA.setCustomFieldValue("severity", "1");
		// String clli = NodeManager.queryClli(faultyEvcnodeInstance);
		// clli is same as device_name.  clli can be empty. 
		// use device_name instead of clli per email from Dickey date
		// 6/24/2014 1:42 PM
		Node dn = ta.getFaultyEndDeviceNode();
		Node en = ta.getFaultyEndEvcNode();

		String clli = NodeManager.getNodeValue(dn, "device_name");
		String evcName = NodeManager.getNodeValue(en, "evc_name");
		String prodType = NodeManager.getNodeValue(en, "product_type");
		String device_type = NodeManager.getNodeValue(dn, "device_type");
		String device_sub_role = NodeManager.getNodeValue(dn, "device_sub_role");
		log.info("faulty end product type = " + prodType + ", device_type = " + device_type+", device_sub_role = "+device_sub_role);

		boolean isAllSDN = false;
		if (prodType.contains("SDN") && 
			device_type.equals("CIENA EMUX") && 
			device_sub_role.equals("EMT"))
			isAllSDN = true;
		if (isCC)
		{
			if (isAllSDN)
			{
				log.info("set classification to SDN-CFM-CFO");
				classification = "SDN-CFM-CFO";
			}
			else
			{
				log.info("set classification to NTE-CFM-CFO");
				classification = "NTE-CFM-CFO";
			}
			syntheticA.setCustomFieldValue(GFPFields.CLASSIFICATION, classification);

			syntheticA.setCustomFieldValue(GFPFields.DOMAIN, "IPAG");
			syntheticA.setCustomFieldValue(GFPFields.NODE_NAME, clli);
			syntheticA.setCustomFieldValue("be_time_stamp", be_time_stamp);
			syntheticA.setCustomFieldValue(GFPFields.FE_TIME_STAMP, fe_time_stamp);
			syntheticA.setCustomFieldValue(GFPFields.EVENT_KEY, eventKey);
			syntheticA.setCustomFieldValue("EventName", eventName);
			syntheticA.setCustomFieldValue("ticket-number", alert_id);
			syntheticA.setCustomFieldValue("evcid", evcName);
			syntheticA.setCustomFieldValue("vrf", vrfName);
		}
		else
		{
			if (isAllSDN)
			{
				log.info("set classification to SDN-CFM-CFO");
				classification = "SDN-CFM-CFO";
			}
			else
			{
				log.info("set classification to NFO-MOBILITY-CFM");
				classification = "NFO-MOBILITY-CFM";
			}
			syntheticA.setCustomFieldValue(GFPFields.CLASSIFICATION, classification);
			syntheticA.setCustomFieldValue(GFPFields.DOMAIN, "NTE");
			syntheticA.setCustomFieldValue("node-name", clli);
			syntheticA.setCustomFieldValue("be_time_stamp", be_time_stamp);
			syntheticA.setCustomFieldValue(GFPFields.FE_TIME_STAMP, fe_time_stamp);
			syntheticA.setCustomFieldValue(GFPFields.EVENT_KEY, eventKey);
			syntheticA.setCustomFieldValue("EventName", eventName);
		}
              	syntheticA.setCustomFieldValue(GFPFields.SEQNUMBER, sequenceNumber);
		if (G2Suppress != null)
              		syntheticA.setCustomFieldValue("G2Suppress", G2Suppress);
		if (cdcSptType != null)
              		syntheticA.setCustomFieldValue("cdc-subscription-type", cdcSptType);
		topo.enrichComponent(syntheticA, ta);
		if (isCC)
		{
			// per req, CLCI is BAU, ie, it is from the alarming end
			// since CLCI will be populated to RUBY CFO file and
			// it required to be the faulty end.  it is also 
			// enriched.
			topo.enrichCircuitID_CLCI(syntheticA, ta);
			// DeviceSubRole is needed when it is in flags
			String flags = topo.createFlags(ta, device_sub_role, false, classification, true);
			syntheticA.setCustomFieldValue("flags", flags);
			syntheticA.setCustomFieldValue(GFPFields.REASON, reason);
		}
		else
		{
			// telco's component = cc's component + circuitID
			// telco's info1 = cc's flags
			// below component is NOT from the trap. it is enriched
			// in topo.enrichComponent().
			String component = syntheticA.getCustomFieldValue(GFPFields.COMPONENT);
			component += " evcid=<"+evcName+"> VRFName=<"+vrfName+">";
			syntheticA.setCustomFieldValue(GFPFields.COMPONENT, component);
			topo.enrichClci_Clfi(syntheticA, ta);
			// SDN is always CC ==> no need to get prodType here 
			// ==> set "" for classification argument
			String flags = topo.createFlags(ta, device_sub_role, false, "", true);
			syntheticA.setCustomFieldValue(GFPFields.INFO1, info1+" "+flags);
			// per email from Shadi dated 4-1-2014, 3:59pm. "VRF=.."
			// needs to be removed from reason as it is already in
			// component field
			syntheticA.setCustomFieldValue(GFPFields.REASON, removeVRF(reason));
		}
		return syntheticA;
	}

	public static CienaAlarm createRubyTwoAlarmsPtp(boolean isCC, CienaAlarm syntheticA, CienaAlarm sa, CienaAlarm ta)
	{
		log.info("createRubyTwoAlarmsPtp runs.");

		String alert_id = syntheticA.getCustomFieldValue(GFPFields.ALERT_ID);
		String purgeIntvl = syntheticA.getCustomFieldValue("purge-interval");
		String fe_time_stamp = syntheticA.getCustomFieldValue(GFPFields.FE_TIME_STAMP);
		String be_time_stamp = syntheticA.getCustomFieldValue("be_time_stamp");
		String eventKey = syntheticA.getCustomFieldValue(GFPFields.EVENT_KEY);
		String eventName = syntheticA.getCustomFieldValue("EventName");
		// String info1 = syntheticA.getCustomFieldValue(GFPFields.INFO1);
		// String info = syntheticA.getCustomFieldValue(GFPFields.INFO);
		String info1 = "";
		String info = "";
		String reason = syntheticA.getCustomFieldValue(GFPFields.REASON);
              	String sequenceNumber = syntheticA.getCustomFieldValue(GFPFields.SEQNUMBER);
		String classification = syntheticA.getCustomFieldValue(GFPFields.CLASSIFICATION);
		String alarmObjectType = syntheticA.getCustomFieldValue("sm-class");
		String G2Suppress = syntheticA.getCustomFieldValue("G2Suppress");
		String cdcSptType = syntheticA.getCustomFieldValue("cdc-subscription-type");
		clearCustomFields(syntheticA);
		syntheticA.setCustomFieldValue(GFPFields.CLASSIFICATION, classification);
		if (alarmObjectType != null)
			syntheticA.setCustomFieldValue("sm-class", alarmObjectType);
		syntheticA.setIdentifier(syntheticA.getIdentifier()+"_Synthetic");
		syntheticA.setCustomFieldValue(GFPFields.ALERT_ID, alert_id+"_Synthetic");
		syntheticA.setCustomFieldValue("purge-interval", purgeIntvl);
		String trigEvcnodeInstance = syntheticA.getOriginatingManagedEntity().split(" ")[1];
		String subAlarmEvcnodeInstance = sa.getOriginatingManagedEntity().split(" ")[1];
		// String deviceInstance = trigEvcnodeInstance.split("/")[0];
		String vrfName = trigEvcnodeInstance.split("/")[1];

		syntheticA.setPerceivedSeverity(PerceivedSeverity.MAJOR);
		syntheticA.setCustomFieldValue("severity", "1");

		// tj: do we need node for two alarms?
		// if yes, which alarm should to used for clli
		// for two alarms(A, B): both alarms are alarming/faulty ends
		// ie, A is B's faulty end.  B is A's faulty end
		// ==> pick any one is fine
		// String clli = NodeManager.queryClli(trigEvcnodeInstance);
		// String evcName = NodeManager.queryEVCName(trigEvcnodeInstance);

		Node dn_t = ta.getDeviceNode();
		Node en_t = ta.getEvcNode();
		Node dn_s = sa.getDeviceNode();
		Node en_s = sa.getEvcNode();

		String evcName = NodeManager.getNodeValue(en_t, "evc_name");
		String clli = NodeManager.getNodeValue(dn_t, "device_name");

		String prodType_t = NodeManager.getNodeValue(en_t, "product_type");
		String device_type_t = NodeManager.getNodeValue(dn_t, "device_type");
		String device_sub_role_t = NodeManager.getNodeValue(dn_t, "device_sub_role");
		String prodType_s = NodeManager.getNodeValue(en_s, "product_type");
		String device_type_s = NodeManager.getNodeValue(dn_s, "device_type");
		String device_sub_role_s = NodeManager.getNodeValue(dn_s, "device_sub_role");

		log.info("trigger product type = " + prodType_t + ", trigger device_type = " + device_type_t + "trigger device_sub_role = " + device_sub_role_t+", subalarm product type = " + prodType_s + ", subalarm device_type = " + device_type_s + "subalarm device_sub_role = " + device_sub_role_s);

		boolean isAllSDN = false;
		if (prodType_t.contains("SDN") && 
			device_type_t.equals("CIENA EMUX") && 
			device_sub_role_t.equals("EMT") &&
			prodType_s.contains("SDN") && 
			device_type_s.equals("CIENA EMUX") && 
			device_sub_role_s.equals("EMT"))
			isAllSDN = true;

		if (isCC)
		{
			if (isAllSDN)
			{
				log.info("set classification to SDN-CFM-CFO");
				classification = "SDN-CFM-CFO";
			}
			else
			{
				log.info("set classification to NTE-CFM-CFO");
				classification = "NTE-CFM-CFO";
			}
			syntheticA.setCustomFieldValue(GFPFields.CLASSIFICATION, classification);
			syntheticA.setCustomFieldValue(GFPFields.DOMAIN, "IPAG");
			syntheticA.setCustomFieldValue(GFPFields.NODE_NAME, clli);
			syntheticA.setCustomFieldValue("be_time_stamp", be_time_stamp);
			syntheticA.setCustomFieldValue(GFPFields.FE_TIME_STAMP, fe_time_stamp);
			syntheticA.setCustomFieldValue(GFPFields.EVENT_KEY, eventKey);
			syntheticA.setCustomFieldValue("EventName", eventName);
			// no node, circuit-id, component as it applies to 
			// faulty end(ie, PTP one alarm case) only
			// tj: trig or 'a'	==> either one is fine
			syntheticA.setCustomFieldValue("ticket-number", alert_id);
		}
		else
		{
			if (isAllSDN)
			{
				log.info("set classification to SDN-CFM-CFO");
				classification = "SDN-CFM-CFO";
			}
			else
			{
				log.info("set classification to NFO-MOBILITY-CFM");
				classification = "NFO-MOBILITY-CFM";
			}
			syntheticA.setCustomFieldValue(GFPFields.CLASSIFICATION, classification);
			syntheticA.setCustomFieldValue(GFPFields.DOMAIN, "NTE");
			syntheticA.setCustomFieldValue("node-name", clli);
			syntheticA.setCustomFieldValue("be_time_stamp", be_time_stamp);
			syntheticA.setCustomFieldValue(GFPFields.FE_TIME_STAMP, fe_time_stamp);
			syntheticA.setCustomFieldValue(GFPFields.EVENT_KEY, eventKey);
			syntheticA.setCustomFieldValue("EventName", eventName);
		}
              	syntheticA.setCustomFieldValue(GFPFields.SEQNUMBER, sequenceNumber);
		if (G2Suppress != null)
              		syntheticA.setCustomFieldValue("G2Suppress", G2Suppress);
		if (cdcSptType != null)
              		syntheticA.setCustomFieldValue("cdc-subscription-type", cdcSptType);

		if (isCC)
		{
			// tj: trig or 'a'
			syntheticA.setCustomFieldValue("evcid", evcName);
			syntheticA.setCustomFieldValue("vrf", vrfName);
			syntheticA.setCustomFieldValue(GFPFields.REASON, reason);
		}
		else
		{
			// tj: trig or 'a'
			String component = "evcid=<"+evcName+"> VRFName=<"+vrfName+">";
			syntheticA.setCustomFieldValue(GFPFields.COMPONENT, component);
			// per email from Shadi dated 4-1-2014, 3:59pm. "VRF=.."
			// needs to be removed from reason as it is already in
			// component field
			syntheticA.setCustomFieldValue(GFPFields.REASON, removeVRF(reason));
		}

		if (isCC)
		{
			// flags contains only trigger info. since PtpCFM=<BOTH>
			// is set in flags ==> RUBY knows to look under info
			// which contains info of both faulty ends
			String flags = topo.createFlags(ta, null, true, classification, false);
			syntheticA.setCustomFieldValue("flags", flags);
			String infosyntheticA = topo.createInfo(ta, isCC);
			String infoa = topo.createInfo(sa, isCC);
			syntheticA.setCustomFieldValue(GFPFields.INFO, info+" "+infosyntheticA+" "+infoa+" AlarmObjectType=<"+alarmObjectType+">");
		}
		else
		{
			// SDN is always CC ==> no need to get prodType here 
			// ==> set "" for classification argument
			String flags = topo.createFlags(ta, null, true, "", false);
			String infosyntheticA = topo.createInfo(ta, isCC);
			String infoa = topo.createInfo(sa, isCC);
			syntheticA.setCustomFieldValue(GFPFields.INFO1, info1+" "+flags+" "+infosyntheticA+" "+infoa);
		}
		return syntheticA;
	}

	public static void procPtpOneAlarm(CienaAlarm ta)
	{
		log.info("procPtpOneAlarm runs.");
		CienaAlarm syntheticA = null;
		try {
			syntheticA = ta.clone();
		} catch (CloneNotSupportedException e1) {
			// TODO Auto-generated catch block
			log.info("Failed to clone.");
			e1.printStackTrace();
			return;
		}
		if (syntheticA != null)
			log.info("synthetic alarm id = " + syntheticA.getIdentifier());
		HashSet<CienaAlarm> cset = new HashSet<CienaAlarm>();
		cset.add(syntheticA);
		log.info("size = " + cset.size());
		boolean isCC = getIsCC(cset);

		try {
			// try block is to catch the cypher exception such as
			// invalid node name, eg 
			// 	evc=node:EVCZZZ(key="VPWS:117280") match ...
			// EVCZZZ doesn't exist
			CienaAlarm ea = createRubyOneAlarmPtp(isCC, syntheticA, ta);
			if (ea != null)
			{
				Util.sendAlarm(ea, AlarmDelegationType.FORWARD, null, false);
				String vrfname = ta.getCustomFieldValue("vrf-name");
				addSyntheticA2map(vrfname, ea);
			}
		} catch (Exception e)
		{
			log.error("Failed to process ptp one alarm.  Drop it.", e);
		}
	}
	public static void procPtpTwoAlarms(CienaAlarm ta, CienaAlarm sa)
	{
		CienaAlarm syntheticA = null;
		try {
			syntheticA = ta.clone();
		} catch (CloneNotSupportedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
		HashSet<CienaAlarm> cset = new HashSet<CienaAlarm>();
		cset.add(syntheticA);
		cset.add(sa);
		boolean isCC = getIsCC(cset);

		try {
			// try block is to catch the cypher exception such as
			// invalid node name, eg 
			// 	evc=node:EVCZZZ(key="VPWS:117280") match ...
			// EVCZZZ doesn't exist
			CienaAlarm ea = createRubyTwoAlarmsPtp(isCC, syntheticA, sa, ta);
			if (ea != null)
			{
				Util.sendAlarm(ea, AlarmDelegationType.FORWARD, null, false);
				String vrfname = ta.getCustomFieldValue("vrf-name");
				addSyntheticA2map(vrfname, ea);
			}
		} catch (Exception e)
		{
			log.error("Failed to process ptp two alarms.  Drop it.", e);
		}
	}

	public void cfmPtpCallBack() {
	   try {
		CienaAlarm ta = (CienaAlarm) this;
		// Scenario scenario = ScenarioThreadLocal.getScenario();
		// get the groups where this alarm is present
		// Collection<Group> groups = PD_Service_Group.getGroupsOfAnAlarm(scenario, this);
		log.info("cfmPtpCallBack() run. trigger id = " + ta.getIdentifier());
		if (!ta.getCanProcess())
		{
			log.info("Stop processing Ptp as trigger alarm cannot be prcessed(ie, clear or suppressed)");
			return;
		}
		if (ta.getIsSubAlarm())
		{
			log.info("Stop processing Ptp as trigger alarm is the subalarm in other Ptp group.");
			return;
		}

		Group group = groupPtpMpt;
		log.info("group name = " + group.getName());
		CienaAlarm sa = null;
/*
		for (Group groupx : groups) {
			// this trigger should belong to only one Correlation group
			if (groupx.getTrigger() == this)
			{
				group = groupx;
				break;
			}
		}
*/
		if (group == null)
		{
			log.info("The Trigger alarm doesn't belong to any group");
			return;
		}
		log.info("continue processing... ");
                for (Alarm ax : group.getAlarmList())
                {
                        log.info("member ID = " + ax.getIdentifier());
                }

		for (Alarm alarm : group.getAlarmList())
		{
			if ((CienaAlarm)alarm != this)	// 'this' is trigger
			{
				sa = (CienaAlarm)alarm;
				if (sa.getCanProcess())
					// at most one subalarm
					break;
				else
				{
					sa = null;
					log.info("Stop processing Ptp as one alarm cannot be prcessed(ie, clear or suppressed)");
					return;
				}
			}
		}

		if (sa != null)
		{
			log.info("TWO alarms.");
			procPtpTwoAlarms(ta, sa);
		}
		else
		{
			log.info("ONE alarms.");
			procPtpOneAlarm(ta);
		}
           } catch (Exception e) {
                log.info("cfmPtpCallBack() failed. ", e);
           }
	}

////////////////////////////////////////////////////////////////////////////////

	// tj: I randomly pick one alarm as the outgoing synthetic alarm
	public static CienaAlarm createRubyAlarmsMpt(boolean isCC, CienaAlarm syntheticA, HashSet<MptData> dataset, boolean isAll, boolean isMPA, String faultyend)
	{
		log.info("createRubyAlarmsMpt runs.");
		String alert_id = syntheticA.getCustomFieldValue(GFPFields.ALERT_ID);
		String purgeIntvl = syntheticA.getCustomFieldValue("purge-interval");
		String fe_time_stamp = syntheticA.getCustomFieldValue(GFPFields.FE_TIME_STAMP);
		String be_time_stamp = syntheticA.getCustomFieldValue("be_time_stamp");
		String eventKey = syntheticA.getCustomFieldValue(GFPFields.EVENT_KEY);
		String eventName = syntheticA.getCustomFieldValue("EventName");
		// String info_orig = syntheticA.getCustomFieldValue(GFPFields.INFO);
		String info_orig = "";
		String reason = syntheticA.getCustomFieldValue(GFPFields.REASON);
              	String sequenceNumber = syntheticA.getCustomFieldValue(GFPFields.SEQNUMBER);
		String classification = syntheticA.getCustomFieldValue(GFPFields.CLASSIFICATION);
		String alarmObjectType = syntheticA.getCustomFieldValue("sm-class");
		String G2Suppress = syntheticA.getCustomFieldValue("G2Suppress");
		String cdcSptType = syntheticA.getCustomFieldValue("cdc-subscription-type");
		clearCustomFields(syntheticA);
		syntheticA.setCustomFieldValue(GFPFields.CLASSIFICATION, classification);
		if (alarmObjectType != null)
			syntheticA.setCustomFieldValue("sm-class", alarmObjectType);
		syntheticA.setIdentifier(syntheticA.getIdentifier()+"_Synthetic");
		syntheticA.setCustomFieldValue(GFPFields.ALERT_ID, alert_id+"_Synthetic");
		syntheticA.setCustomFieldValue("purge-interval", purgeIntvl);

		String trigEvcnodeInstance = syntheticA.getOriginatingManagedEntity().split(" ")[1];
		// String deviceInstance = trigEvcnodeInstance.split("/")[0];
		String vrfName = trigEvcnodeInstance.split("/")[1];

		syntheticA.setPerceivedSeverity(PerceivedSeverity.MAJOR);
		syntheticA.setCustomFieldValue("severity", "1");
              	syntheticA.setCustomFieldValue(GFPFields.SEQNUMBER, sequenceNumber);
		if (G2Suppress != null)
              		syntheticA.setCustomFieldValue("G2Suppress", G2Suppress);
		if (cdcSptType != null)
              		syntheticA.setCustomFieldValue("cdc-subscription-type", cdcSptType);
		// tj: which alarm's clli should be used?
		// String clli = NodeManager.queryClli(faultyend);
		// String evcName = NodeManager.queryEVCName(trigEvcnodeInstance);

		String clli = "";
		String evcName = "";
		Iterator iy = dataset.iterator();
		while(iy.hasNext())
		{
			MptData m = (MptData)iy.next();
			clli = m.device_name;
			evcName = m.evc_name;
			break;	// get the first clli
		}

		boolean isAllSDN = true;
		Iterator ix = dataset.iterator();
		while(ix.hasNext())
		{
			MptData m = (MptData)ix.next();
			if (!m.prodType.contains("SDN") ||
				!m.device_type.equals("CIENA EMUX") ||
				!m.device_sub_role.equals("EMT"))
			{
				isAllSDN = false;
				break;
			}
		}

		if (isCC)
		{
			if (isAllSDN)
			{
				log.info("set classification to SDN-CFM-CFO");
				classification = "SDN-CFM-CFO";
			}
			else
			{
				log.info("set classification to NTE-CFM-CFO");
				classification = "NTE-CFM-CFO";
			}
			syntheticA.setCustomFieldValue(GFPFields.CLASSIFICATION, classification);
			syntheticA.setCustomFieldValue(GFPFields.DOMAIN, "IPAG");
			syntheticA.setCustomFieldValue(GFPFields.NODE_NAME, clli);
			syntheticA.setCustomFieldValue("be_time_stamp", be_time_stamp);
			syntheticA.setCustomFieldValue(GFPFields.FE_TIME_STAMP, fe_time_stamp);
			syntheticA.setCustomFieldValue(GFPFields.EVENT_KEY, eventKey);
			syntheticA.setCustomFieldValue("EventName", eventName);
			syntheticA.setCustomFieldValue("ticket-number", alert_id);
			syntheticA.setCustomFieldValue(GFPFields.COMPONENT, "");
			syntheticA.setCustomFieldValue(GFPFields.CIRCUIT_ID, "");
			// since circuit id is empty per requirement and its
			// value is from clci (see alarm manager's RUBYCFO file)
			// clci has to be set to empty per Shadi'e email
			// dated Tue 7/29/2014 3:42 PM
			syntheticA.setCustomFieldValue(GFPFields.CLCI, "");
		}
		else
		{
			if (isAllSDN)
			{
				log.info("set classification to SDN-CFM-CFO");
				classification = "SDN-CFM-CFO";
			}
			else
			{
				log.info("set classification to NFO-MOBILITY-CFM");
				classification = "NFO-MOBILITY-CFM";
			}
			syntheticA.setCustomFieldValue(GFPFields.CLASSIFICATION, classification);
			syntheticA.setCustomFieldValue(GFPFields.DOMAIN, "NTE");
			syntheticA.setCustomFieldValue("node-name", clli);
			syntheticA.setCustomFieldValue("be_time_stamp", be_time_stamp);
			syntheticA.setCustomFieldValue(GFPFields.FE_TIME_STAMP, fe_time_stamp);
			syntheticA.setCustomFieldValue(GFPFields.EVENT_KEY, eventKey);
			syntheticA.setCustomFieldValue("EventName", eventName);
		}

		if (isCC)
		{
			// tj: trig or 'a'
			syntheticA.setCustomFieldValue("evcid", evcName);
			syntheticA.setCustomFieldValue("vrf", vrfName);
			syntheticA.setCustomFieldValue(GFPFields.REASON, reason);
		}
		else
		{
			// tj: which alarm should be used for evcid?
			// tj: trig or 'a'
			String component = "evcid=<"+evcName+"> VRFName=<"+vrfName+">";
			syntheticA.setCustomFieldValue(GFPFields.COMPONENT, component);
			syntheticA.setCustomFieldValue(GFPFields.CLCI, "");
			syntheticA.setCustomFieldValue(GFPFields.CLFI, "");
			// per email from Shadi dated 4-1-2014, 3:59pm. "VRF=.."
			// needs to be removed from reason as it is already in
			// component field
			syntheticA.setCustomFieldValue(GFPFields.REASON, removeVRF(reason));
		}

		if (isCC)
		{
			String info = info_orig;
			String prodTypex = "";

			Iterator ii = dataset.iterator();
			while(ii.hasNext())
			{
				MptData m = (MptData)ii.next();
				prodTypex = m.prodType;

				info += " CLLI=<"+m.clli+"> PortAID=<"+m.port_aid+">";
				if ("GE TRUNK".equals(m.mpaType))
					info += " CLFI=<"+m.clfi+">";
				else
					info += " CLCI=<"+m.clci+">";
				info += " MPAType=<"+m.mpaType+">";
				//
				// per email from 
				// Nga dated Tue 5/27/2014 11:32 AM
				// Nga dated Thu 11/6/2014 10:10 AM
				//
				// DeviceSubRole is NOT need for CC(including
				// 	classification is SDN-ETHERNET for
				//	MPT) for both PTP and MPT.  
				//
				// DeviceSubRole is need for TELCO for
				// 	both PTP and MPT.  
				//
/*
				if (classification.contains("SDN"))
				{
					if ("EMT".equals(m.device_sub_role))
						info += " DeviceSubRole=<EMT>";
				}
*/
			}
			syntheticA.setCustomFieldValue(GFPFields.INFO, info+" AlarmObjectType=<"+alarmObjectType+">");

			String flags = (isAll==true)? "MptCFM=<ALL>":"MptCFM=<Y>";
			if (isMPA == true)
				flags += " MPA=<Y>";
			//
			// note : below is NOT true for hybrid EVC:
			// if the MPT group has n alarms and one is SDN
			// ==> the rest must be SDN and they all have the
			// same prodType;
			if (classification.contains("SDN"))
				// below prodTypex MUST be SDN-ETHERNET
				flags += " ProductType=<"+prodTypex+">";

			syntheticA.setCustomFieldValue("flags", flags);
		}
		else
		{
			int infoFld = 1;
			int dataSet = 1;
			String info = (isAll==true)? "MptCFM=<ALL>":"MptCFM=<Y>";
			if (isMPA == true)
				info += " MPA=<Y>";
			
			boolean isDone = false;
			Iterator ii = dataset.iterator();
			while(ii.hasNext())
			{
				MptData m = (MptData)ii.next();
				info += " CLLI=<"+m.clli+"> PortAID=<"+m.port_aid+">";
				if ("GE TRUNK".equals(m.mpaType))
					info += " CLFI=<"+m.clfi+">";
				else
					info += " CLCI=<"+m.clci+">";
				info += " MPAType=<"+m.mpaType+">";
				if ("EMT".equals(m.device_sub_role))
					info += " DeviceSubRole=<EMT>";
				dataSet++;
				if (dataSet == 3)
				{
					syntheticA.setCustomFieldValue("info"+infoFld, info);
					dataSet = 1;
					info = "";
					infoFld++;
					if (infoFld >=4)
					{
						isDone = true;
						break;
					}
				}
			}
			if (isDone == false && dataSet == 2)
				syntheticA.setCustomFieldValue("info"+infoFld, info);
		}
		return syntheticA;
	}

	// at least one alarm is the Ciena alarm.
	// synthetic alarm is modeled from the FIRST alarm
	public static void procMptAlarmCase1(Group group)
	{
		log.info("procMptAlarmCase1() runs.");
		String eventKey = null;

		HashSet<CienaAlarm> cset = new HashSet<CienaAlarm>();
		HashSet<String> endset = null;
		HashSet<MptData> dataset = new HashSet<MptData>();
		boolean isAll = true;
		boolean isMPA = false;
		CienaAlarm syntheticA = null;
		String vrfname = "";
		String faultyend = "";

		for (Alarm am : group.getAlarmList())
		{
			// CienaAlarm ca = (CienaAlarm) me.getValue();
			CienaAlarm ca = (CienaAlarm) am;
			if (!ca.getCanProcess())
			{
				log.info("Stop processing MPT Case1 as one alarm cannot be prcessed(ie, clear or suppressed)");
				// continue;
				return;
			}
			cset.add(ca);
			eventKey = ca.getCustomFieldValue(GFPFields.EVENT_KEY);
			log.info("event key = " + eventKey + ", identifier = " + ca.getIdentifier()); 
			if (!eventKey.equals("50002/100/52"))
			{
				log.info("Adtran CFM " + ca.getIdentifier() + " is suppresesed by Ciena CFM in Mpt process.");
				continue;	// drop adtran alarms
			}
			// tj: check field name 
			String mepid = ca.getCustomFieldValue("mepid");
			vrfname = ca.getCustomFieldValue("vrf-name");
			boolean isCienaAlarm = true;

			MptData mdata = NodeManager.getMptData(mepid, vrfname, isCienaAlarm);
			if (mdata == null)
				continue;
			dataset.add(mdata);
			faultyend = mdata.faultyend;	// pick last faulty end

			if (endset == null)
				// do it once as all alarm has same vrfname
				endset = NodeManager.getAllEnds(vrfname);

			if (syntheticA == null)
			{
				try {
					// trigger can be adtran which
					// can be dropped.  assume
					// the first alarm on AlarmList
					// arrived first
					syntheticA = ca.clone();
				} catch (CloneNotSupportedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return;
				}
			}
			// if any faulty end's mpa_ind == 'Y' ==>
			// set isMPA to true
			if ("Y".equals(mdata.mpa_ind))
				isMPA = true;
		}
		if (dataset.size() == 0 || endset.size() == 0)
		{
			log.info("No entry found in mepidmapping table.  suppress it.");
			return;
		}
		if (syntheticA == null)
		{
			log.info("Failed to clone the ALL alarms. Drop the group.");
			return;
		}
		isAll = true;
		log.info("alarm count = " + dataset.size() + ", faulty end count = " + endset.size());
		if (dataset.size() != endset.size())
			isAll = false;
		else
		{
			Iterator i = dataset.iterator();
			while(i.hasNext())
			{
				MptData m = (MptData)i.next();
				log.info("faulty end = " + m.faultyend);
				if (!endset.contains(m.faultyend))
				{
					isAll = false;
					break;
				}
			}
		}

		boolean isCC = getIsCC(cset);
		
		CienaAlarm ea = createRubyAlarmsMpt(isCC, syntheticA, dataset, isAll, isMPA, faultyend);
		if (ea != null)
		{
			Util.sendAlarm(ea, AlarmDelegationType.FORWARD, null, false);
			addSyntheticA2map(vrfname, ea);
		}
	}

	// all adtran alarms
	// synthetic alarm is modeled from the FIRST alarm
	public static void procMptAlarmCase2(Group group)
	{
		log.info("procMptAlarmCase2 runs.");
		HashSet<CienaAlarm> cset = new HashSet<CienaAlarm>();
		HashSet<MptData> dataset = new HashSet<MptData>();

		boolean isAll = false;	// always false for Case2.
		boolean isMPA = false;
		CienaAlarm syntheticA = null;
		String vrfname = "";
		String faultyend = "";

		for (Alarm am : group.getAlarmList())
		{
			// CienaAlarm ca = (CienaAlarm) me.getValue();
			CienaAlarm ca = (CienaAlarm) am;
			if (!ca.getCanProcess())
			{
				log.info("Stop processing MPT Case2 as one alarm cannot be prcessed(ie, clear or suppressed)");
				// continue;
				return;
			}
			cset.add(ca);
			String mepid = ca.getCustomFieldValue("mepid");
			vrfname = ca.getCustomFieldValue("vrf-name");
			boolean isCienaAlarm = false;

			MptData mdata = NodeManager.getMptData(mepid, vrfname, isCienaAlarm);
			if (mdata == null)
				continue;
			dataset.add(mdata);
			faultyend = mdata.faultyend;	// pick last faulty end

			if (syntheticA == null)
			{
				try {
					// trigger is always the first
					// alarm
					if (ca == group.getTrigger())
						syntheticA = ca.clone();
				} catch (CloneNotSupportedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return;
				}
			}
			// if any faulty end's mpa_ind == 'Y' ==>
			// set isMPA to true
			if ("Y".equals(mdata.mpa_ind))
				isMPA = true;
		}
		if (dataset.size() == 0)
		{
			log.info("No entry found in mepidmapping table.  suppress it.");
			return;
		}
		if (syntheticA == null)
		{
			log.info("Failed to clone the ALL alarms. Drop the group.");
			return;
		}
		boolean isCC = getIsCC(cset);
		CienaAlarm ea = createRubyAlarmsMpt(isCC, syntheticA, dataset, isAll, isMPA, faultyend);
		if (ea != null)
		{
			Util.sendAlarm(ea, AlarmDelegationType.FORWARD, null, false);
			addSyntheticA2map(vrfname, ea);
		}
	}
	
	public static void addSyntheticA2map(String vrfname, CienaAlarm syna)
	{
		HashSet<CienaAlarm> as = null;
		log.info("add synthetic alarm = " + syna.getIdentifier() +" to map for vrf name = " + vrfname);
		if (synaMap.containsKey(vrfname))
		{
			as = synaMap.get(vrfname);
			as.add(syna);
		}
		else
		{
			as = new HashSet<CienaAlarm>(); 
			as.add(syna);
			synaMap.put(vrfname, as);
		}
	}

	public void clearSyntheticAlarm(String vrfname, String seqNumber, boolean isGenByUCA, boolean isPurgeItvlExp)
	{
		if (synaMap.containsKey(vrfname))
		{
			HashSet<CienaAlarm> as = synaMap.get(vrfname);
			Iterator i = as.iterator();
			while(i.hasNext())
			{
				CienaAlarm syna = (CienaAlarm)i.next();
				syna.setSeverity(4);
				syna.setCustomFieldValue(GFPFields.LAST_CLEAR_TIME, ""+System.currentTimeMillis()/1000);
                     		syna.setCustomFieldValue(GFPFields.SEQNUMBER, seqNumber);
				if (isGenByUCA == true)
					syna.setCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA, "YES"); 
				if (isPurgeItvlExp == true)
					syna.setCustomFieldValue(GFPFields.IS_PURGE_INTERVAL_EXPIRED, "YES"); 
				syna.setIsSent(false); // so it can be sent
				// no need to call GFPUtil.populateEnrichedAlarmObj as syna is from the Hash. it is type EnrichedAlarm
				log.info("Send clear for synthetic alarm = " + syna.getIdentifier());
				Util.sendAlarm(syna, AlarmDelegationType.FORWARD, null, false);
			}
			log.info("remove vrfname = " + vrfname + " from synthetic alarm map");
			synaMap.remove(vrfname);
		}
		else
			log.info("No active synthetic alarms with vrf name = " + vrfname);
	}

	public static void procMptAlarm(Group group)
	{
		String eventKey = null;
		// Map<String, Alarm> map = group.getAlarmsMap();
		try {
			// try block is to catch the cypher exception such as
			// invalid node name, eg 
			// 	evc=node:EVCZZZ(key="VPWS:117280") match ...
			// EVCZZZ doesn't exist
			// for (Map.Entry<String, Alarm> me: map.entrySet())
			for (Alarm am : group.getAlarmList())
			{
				// CienaAlarm ca = (CienaAlarm) me.getValue();
				CienaAlarm ca = (CienaAlarm) am;
				if (!ca.getCanProcess())
				{
					log.info("Stop processing MPT as one alarm cannot be prcessed(ie, clear or suppressed)");
					// continue;
					return;
				}
				eventKey = ca.getCustomFieldValue(GFPFields.EVENT_KEY);
				if (eventKey.equals("50002/100/52"))
				{
					procMptAlarmCase1(group);
					return;
				}
			}
			procMptAlarmCase2(group);
		} catch (Exception e)
		{
			log.error("Failed to process mpt alarm.  Drop it.", e);
		}
	}

	public void cfmMptCallBack() {
	   try {
		CienaAlarm ta = (CienaAlarm) this;
		log.info("cfmMptCallBack() run. trigger id = " + ta.getIdentifier());
		if (!ta.getCanProcess())
		{
			log.info("Stop processing Mpt as trigger alarm cannot be prcessed(ie, clear or suppressed)");
			return;
		}
		if (ta.getIsSubAlarm())
		{
			log.info("Stop processing Mpt as trigger alarm is the subalarm in other Ptp group.");
			return;
		}

		Group group = groupPtpMpt;
		log.info("group name = " + group.getName());
		// Scenario scenario = ScenarioThreadLocal.getScenario();
		// Collection<Group> groups = PD_Service_Group.getGroupsOfAnAlarm(scenario, this);
		if (group == null)
		{
			log.info("The Trigger alarm doesn't belong to any group");
			return;
		}
                for (Alarm ax : group.getAlarmList())
                {
                        log.info("member ID = " + ax.getIdentifier());
                }
		procMptAlarm(group);
           } catch (Exception e) {
                log.info("cfmMptCallBack() failed. ", e);
           }
	}

	// x = "CFM Fault is detected, Admin Status disabled, Oper Status disabled VRF=<L2CKT:34426> Region=<S>"
	// ret = "CFM Fault is detected, Admin Status disabled, Oper Status disabled  Region=<S>"
	public static String removeVRF(String x)
	{
		log.info("original reason = " + x);
		int idx = x.indexOf("VRF=");
		if (idx != -1)
		{
			log.info("VRF= is found");
			String y = x.substring(0, idx);
			String z = x.substring(idx+4);
			int idxe = z.indexOf("> ");
			if (idxe != -1)
			{
				String newReason = y + z.substring(idxe+2);
				log.info("new reason = " + newReason);
				return newReason;
			}
		}
		return x;
		
	}
}
