package com.att.gfp.data.ipagAlarm;

/**
 * 
 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.mortbay.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipag.topoModel.NodeManager;
import com.att.gfp.data.ipag.topoModel.FBSPtpData;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.helper.GFPFields;
import com.hp.uca.common.trace.LogHelper;
import com.att.gfp.helper.GFPUtil;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.engine.Bootstrap;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.localvariable.LocalVariable;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.scenario.ScenarioThreadLocal;
import com.hp.uca.expert.vp.pd.problem.Util;
import com.hp.uca.expert.vp.pd.services.PD_Service_Group;
import com.hp.uca.expert.x733alarm.CustomField;
import com.hp.uca.expert.x733alarm.CustomFields;
import com.hp.uca.expert.x733alarm.NetworkState;
import com.hp.uca.expert.x733alarm.OperatorState;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;
import com.hp.uca.expert.x733alarm.ProblemState;


// @XmlRootElement
public class SyslogAlarm extends EnrichedAlarm {	

	public static final long AGGR_ALARM_CORR_WINDOW = 120 * 1000;
	public static final long TUNNEL_ALARM_AGING_WINDOW = 60 * 1000;

	private int bfDownThreshold;
	private String tunnelLoopBackIP;
	private String startLagId;
	private String startLagIp;
	private String remoteDevice_SavpnSiteID;
	private String localDevice_SavpnSiteID;
	private String mobility = "";
	// private boolean FBSPtpAlarm = false;

	private Boolean isSent = false;
	private Boolean isClear = false;
	private Boolean inInfoList = false;
	private Boolean inPool = false;   
	private HashSet<String> vrfset = new HashSet<String>();
	private boolean isPtpMpt = false;
	private boolean isFBSPtp = false;
	private Group groupFBS = null;
	private boolean isSubAlarm = false;
	private boolean isCiena_LD_FRU_CARD = false;
	private boolean isCiena_LD_FRU_SLOT = false;
	private int numberOfWatches = 0;
	private int numberOfExpiredWatches = 0;
	List<String> PPHIps;
	List<String> hopStartLagIds; 
	List<String> hopStartLagIPs;

	static HashMap<String, HashSet<SyslogAlarm>> synaMap = new HashMap<String, HashSet<SyslogAlarm>>();

	/**
	 * 
	 */
	private static final long serialVersionUID = -184871297978421735L;


	private static Logger log = LoggerFactory.getLogger(SyslogAlarm.class);


	/**
	 * EnrichedAlarm Constructor
	 */
	public SyslogAlarm() {
		super();

		// initialize the suppress value to not suppress for now 
		inPool = false;
		isSent = false;
		isClear = false;
		inInfoList = false;
		isPtpMpt = false;
		isFBSPtp = false;
		groupFBS = null;
		isSubAlarm = false;
		mobility = "";
		isCiena_LD_FRU_CARD = false;
		isCiena_LD_FRU_SLOT = false;
		vrfset = new HashSet<String>();
		numberOfWatches = 0;
		numberOfExpiredWatches = 0;

		bfDownThreshold = 0;
		tunnelLoopBackIP = null;
		startLagId = null;
		startLagIp = null;
		remoteDevice_SavpnSiteID = null;
		localDevice_SavpnSiteID = null;
		PPHIps = new ArrayList<String>();
		hopStartLagIds = new ArrayList<String>();
		hopStartLagIPs = new ArrayList<String>();



	}

	/**
	 * EnrichedAlarm Copy Constructor.
	 * 
	 * This constructor constructs an Extended Alarm and uses the Alarm provided as
	 * parameter to set all the standard alarm fields
	 * @throws Exception 
	 */
	public SyslogAlarm(Alarm alarm) throws Exception {
		super(alarm);

		// initialize the suppress value to not suppress for now 
		inPool = false;
		isSent = false;
		isClear = false;
		inInfoList = false;
		isPtpMpt = false;
		isFBSPtp = false;
		groupFBS = null;
		isSubAlarm = false;
		mobility = "";
		isCiena_LD_FRU_CARD = false;
		isCiena_LD_FRU_SLOT = false;
		vrfset = new HashSet<String>();
		numberOfWatches = 0;
		numberOfExpiredWatches = 0;

		bfDownThreshold = 0;
		tunnelLoopBackIP = null;
		startLagId = null;
		startLagIp = null;
		remoteDevice_SavpnSiteID = null;
		localDevice_SavpnSiteID = null;
		PPHIps = new ArrayList<String>();
		hopStartLagIds = new ArrayList<String>();
		hopStartLagIPs = new ArrayList<String>();


	}


	public SyslogAlarm( SyslogAlarm alarm) throws Exception {
		this ( (Alarm) alarm );	

		inPool = alarm.inPool;
		isSent = alarm.isSent;
		isClear = alarm.isClear;
		inInfoList = alarm.inInfoList;
		isPtpMpt = alarm.isPtpMpt;
		isFBSPtp = alarm.isFBSPtp;
		groupFBS = alarm.groupFBS;
		isSubAlarm = alarm.isSubAlarm;
		mobility = alarm.mobility;
		isCiena_LD_FRU_CARD = alarm.isCiena_LD_FRU_CARD;
		isCiena_LD_FRU_SLOT = alarm.isCiena_LD_FRU_SLOT;
		vrfset = alarm.vrfset;
		numberOfWatches = alarm.numberOfWatches;
		numberOfExpiredWatches = alarm.numberOfExpiredWatches;

		bfDownThreshold = alarm.bfDownThreshold;
		tunnelLoopBackIP = alarm.tunnelLoopBackIP;
		startLagId = alarm.startLagId;
		startLagIp = alarm.startLagIp;
		remoteDevice_SavpnSiteID = alarm.remoteDevice_SavpnSiteID;
		localDevice_SavpnSiteID = alarm.localDevice_SavpnSiteID;
		PPHIps = alarm.PPHIps;
		hopStartLagIds = alarm.hopStartLagIds;
		hopStartLagIPs = alarm.hopStartLagIPs;

	}


	/**
	 * Clones the provided Extended Alarm object
	 * @return a copy of the current object
	 * @throws CloneNotSupportedException
	 */
	@Override
	public SyslogAlarm clone() throws CloneNotSupportedException {
		SyslogAlarm newAlarm = (SyslogAlarm) super.clone();

		newAlarm.inPool = this.inPool;		
		newAlarm.isSent = this.isSent;
		newAlarm.isClear = this.isClear;
		newAlarm.inInfoList = this.inInfoList;
		newAlarm.isPtpMpt = this.isPtpMpt;
		newAlarm.isFBSPtp = this.isFBSPtp;
		newAlarm.groupFBS = this.groupFBS;
		newAlarm.isSubAlarm = this.isSubAlarm;
		newAlarm.mobility = this.mobility;
		newAlarm.isCiena_LD_FRU_CARD = this.isCiena_LD_FRU_CARD;
		newAlarm.isCiena_LD_FRU_SLOT = this.isCiena_LD_FRU_SLOT;
		newAlarm.vrfset = this.vrfset;
		newAlarm.numberOfWatches = this.numberOfWatches;
		newAlarm.numberOfExpiredWatches = this.numberOfExpiredWatches;

		newAlarm.bfDownThreshold = this.bfDownThreshold;
		newAlarm.startLagId = this.startLagId;
		newAlarm.startLagIp = this.startLagIp;
		newAlarm.tunnelLoopBackIP = this.tunnelLoopBackIP;
		newAlarm.remoteDevice_SavpnSiteID = this.remoteDevice_SavpnSiteID;
		newAlarm.localDevice_SavpnSiteID = this.localDevice_SavpnSiteID;
		newAlarm.PPHIps =  this.PPHIps;
		newAlarm.hopStartLagIds = this.hopStartLagIds;
		newAlarm.hopStartLagIPs = this.hopStartLagIPs;

		return newAlarm;
	}


	/**
	 * 
	 */

	public void clearAggregateAlarm(Group group, long aggrAlarmCorrWindowStartTime) {
		LocalVariable var = group.getVar();
		synchronized (var) {
			Long aggrAlarmLastNofitiedAt = (Long) var.get("AGGR_ALARM_LAST_NOTIFIED_AT");
			long currentTime = System.currentTimeMillis();
			if (aggrAlarmLastNofitiedAt >= aggrAlarmCorrWindowStartTime) {
				String aggrAlarmId = (String) var.get("AGGR_ALARM_ID");
				log.info("clearAggregateAlarm():" +
						" AGGREGATE-ALARM-FOUND-CLEAR-AGGREGATE|" +
						" This event: " + this.getIdentifier() +
						" | Aggregate alarm created less than " +
						AGGR_ALARM_CORR_WINDOW / 60000 +
						" mins ago already exists -" +
						" Alert-id of Aggregate: " + aggrAlarmId +
						" and was created " + (currentTime - aggrAlarmLastNofitiedAt) / 1000 +
						" secs ago | Clear the aggregate alarm as syslog event is a clear. Clear Aggregate");
				sendAggregateAlarm(PerceivedSeverity.CLEAR, var, null, null);
			}
			var.put("TUNNEL_CLEAR_ID", this.getIdentifier());
			var.put("TUNNEL_ALARM_LAST_CLEARED_AT", new Long(currentTime));
			var.put("AGGR_ALARM_ID", "");
			var.put("AGGR_ALARM_LAST_NOTIFIED_AT", new Long(0));
			var.put("AGGR_ALARM_COMPONENT", "");
			var.put("AGGR_ALARM_REASON", "");
		}
	}	

	private void sendAggregateAlarm(PerceivedSeverity sev, LocalVariable var, String component,
			String reason) {
		long now = System.currentTimeMillis();
		String nowStr = Long.toString(now / 1000);

		try {
			synchronized (var) {
				String tailEndRtr = this.getCustomFieldValue("tail-end-router");
				SyslogAlarm ea = new SyslogAlarm(this);
				ea.setIdentifier("50004/1/11-" + tailEndRtr); 
				ea.setOriginatingManagedEntity("DEVICE " + tailEndRtr);
				GregorianCalendar gCalendar = new GregorianCalendar();
				gCalendar.setTime(new java.util.Date(now));	 
				XMLGregorianCalendar alarmraisedtime = 
						DatatypeFactory.newInstance().newXMLGregorianCalendar(gCalendar);
				ea.setAlarmRaisedTime(alarmraisedtime);
				ea.setPerceivedSeverity(sev);
				if (sev == PerceivedSeverity.CLEAR) {
					ea.setCustomFieldValue("component", var.getString("AGGR_ALARM_COMPONENT"));
					ea.setCustomFieldValue("reason", "CLEAR");
					ea.setCustomFieldValue("last-clear-time", nowStr);

				} else {
					ea.setCustomFieldValue("component", component);
					ea.setCustomFieldValue("reason", getCustomFieldValue("reason") + "-" + reason);
					ea.setCustomFieldValue("last-clear-time", "0.0");
				}
				ea.setCustomFieldValue("fe-alarm-time", nowStr);
				ea.setCustomFieldValue("be-alarm-time", nowStr);
				ea.setCustomFieldValue("last-update", nowStr);
				ea.setCustomFieldValue("event-key", "50004/1/11");
				ea.setCustomFieldValue("alert-id", ea.getIdentifier());
				ea.setCustomFieldValue("sm-class", "TUNNEL");
				ea.setCustomFieldValue("managed-object-class", "TUNNEL");
				ea.setCustomFieldValue("managed-object-instance", tailEndRtr);

				log.info("createAggregateAlarm() ==> Sending Aggregate Alarm: " + ea.toString());
				Util.whereToSendThenSend(ea, false);

				if (sev != PerceivedSeverity.CLEAR) {
					var.put("AGGR_ALARM_ID", ea.getIdentifier());
					var.put("AGGR_ALARM_LAST_NOTIFIED_AT", new Long(now));
					var.put("AGGR_ALARM_COMPONENT", component);
					var.put("AGGR_ALARM_REASON", reason);
					var.put("TUNNEL_CLEAR_ID", "");
					var.put("TUNNEL_ALARM_LAST_CLEARED_AT", new Long(0));
				}
			}
		} catch (Exception e) {
			log.info("Exception: ", e);
			log.trace("sendAggregateAlarm: ERROR:"
					+ Arrays.toString(e.getStackTrace()));
		}	
	}

	private boolean checkForTunnelClear(Group group, long agingStartTime) {

		boolean ret = false;
		LocalVariable var = group.getVar();
		synchronized (var) {
			Long tunnelAlarmLastClearedAt = (Long) var.get("TUNNEL_ALARM_LAST_CLEARED_AT");
			if (tunnelAlarmLastClearedAt >= agingStartTime) {
				String tunnelClearId = (String) var.get("TUNNEL_CLEAR_ID");
				log.info("checkForTunnelClear():  This event: " + 
						this.getIdentifier() + 
						" | Received clear for alarm with same tail-end-router" +
						" while waiting for " + TUNNEL_ALARM_AGING_WINDOW / 1000 + 
						" secs - the alarm is- " + tunnelClearId + 
						" | Aggregate alarm will not be created.");
				ret = true;
			}
		}
		return ret;
	}

	public boolean checkForAggregateAlarm(Group group, long aggrAlarmCorrWindowStartTime) {

		boolean ret = false;

		LocalVariable var = group.getVar();
		synchronized (var) {
			Long aggrAlarmLastNofitiedAt = (Long) var.get("AGGR_ALARM_LAST_NOTIFIED_AT");
			if (aggrAlarmLastNofitiedAt.longValue() >= aggrAlarmCorrWindowStartTime) {
				String aggrAlarmId = (String) var.get("AGGR_ALARM_ID");
				long currentTime = System.currentTimeMillis();
				log.info("checkForAggregateAlarm(): " +
						"AGGREGATE-ALARM-FOUND-SUPRESS-ALARM| This event: " + this.getIdentifier() +
						"| Aggregate alarm created less than " +
						AGGR_ALARM_CORR_WINDOW / 60000 +
						" mins ago already exists - " +
						"id of Aggregate: " +  aggrAlarmId + 
						" and was created " + (currentTime - aggrAlarmLastNofitiedAt) / 1000 + 
						" secs ago. Suppress Alarm");
				ret = true;
			}
		}
		return ret;
	}

	public void rpdMplsLspCallback(Long agingStartTime) {
		log.info("rpdMplsLspCallback(): " + getIdentifier());

		Scenario scenario = ScenarioThreadLocal.getScenario();

		if (getPerceivedSeverity() == PerceivedSeverity.CLEAR) {
			log.info("rpdMplsLspCallback():  This event: " + this.getIdentifier() + 
					" | alarm cleared while waiting for " + TUNNEL_ALARM_AGING_WINDOW / 1000 + 
					" secs | Aggregate alarm will not be created.");
			return;

		}

		for (Group group : PD_Service_Group.getGroupsOfAnAlarm(scenario, this)) {
			if (checkForTunnelClear(group, agingStartTime.longValue()) || 
					checkForAggregateAlarm(group, agingStartTime.longValue())) {
				return;
			}
			int alarmCount = 0;
			String comp = this.getCustomFieldValue("component");
			for (Alarm a : group.getAlarmList()) {
				if (a.getTimeInMilliseconds() >= agingStartTime.longValue()) {
					String tunnelName = 
							a.getOriginatingManagedEntity().split(" ")[1];
					log.info("rpdMplsLspCallback():  This event: " + 
							this.getIdentifier() +
							" | Found Rpd-mpls-lsp-down alarm on Tunnel " +
							tunnelName + " created " + 
							a.getAlarmRaisedTime().toString());
					alarmCount++;
					if (comp != null) {
						comp += "; ";
					}
					comp += alarmCount + "<" + tunnelName + ">";
				}
			}
			if (alarmCount > 1) {
				log.info("rpdMplsLspCallback():  This event: " + 
						this.getIdentifier() +
						" | Found " + alarmCount + 
						" Rpd-mpls-lsp-down alarms. Creating Aggregate alarm.");
				String reason = "Possible FRR-Tunnel Tail-End Failure - Router <" +
						this.getCustomFieldValue("tail-end-router") + 
						"> - <" + alarmCount + "> tunnel failures rcvd.";
				sendAggregateAlarm(PerceivedSeverity.CRITICAL, group.getVar(), comp, reason);
			}
		}		
	}

	public String getTunnelLoopBackIP() {
		return tunnelLoopBackIP;
	}

	public void setTunnelLoopBackIP(String tunnelLoopBackIP) {
		this.tunnelLoopBackIP = tunnelLoopBackIP;
	}

	public String getStartLagId() {
		return startLagId;
	}

	public void setStartLagId(String startLagId) {
		this.startLagId = startLagId;
	}

	public String getStartLagIp() {
		return startLagIp;
	}

	public void setStartLagIp(String startLagIp) {
		this.startLagIp = startLagIp;
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
	public Boolean getInInfoList() {
		return inInfoList;
	}

	public void setInInfoList(Boolean inInfoList) {
		this.inInfoList = inInfoList;
	}
	///
	public Boolean getCanSend() {
		if (log.isTraceEnabled())
			log.trace("getCanSend(): alarm = " + getIdentifier() + ", isClear = " + isClear+ ", isSent = " + isSent+ ", isSuppressed = " + isSuppressed());
		return (!isClear && !isSent && !isSuppressed());
	}
	///
	public Boolean getCanProcess() {
		if (log.isTraceEnabled())
			log.trace("getCanProcess(): alarm = " + getIdentifier() + ", isClear = " + isClear+ ", isSent = " + isSent+ ", isSuppressed = " + isSuppressed());
		return (!isClear && !isSuppressed());
	}
	///
	/*
	public void setFBSPtpAlarm(boolean FBSPtpAlarm){
		this.FBSPtpAlarm =  FBSPtpAlarm;
	}
	public boolean isFBSPtpAlarm() {
		return FBSPtpAlarm;
	}
	 */

	/*
	public void setVRFSet(HashSet<String> vrfset)
	{
		this.vrfset = vrfset;
	}
	 */
	public void addVRFName(String vrf_name)
	{
		vrfset.add(vrf_name);
	}

	public HashSet<String> getVRFSet()
	{
		return vrfset;
	}

	private boolean checkIfStringExists(List<String> sList, String newString) {
		boolean ret = false;
		for (String storedString : sList) {
			if(storedString.equals(newString)) {
				ret = true;
				break;
			}
		}
		return ret;
	}

	public void addHopStartLagIP(String ip)
	{
		if(!checkIfStringExists(hopStartLagIPs, ip))
			hopStartLagIPs.add(ip);
	}

	public List<String> getHopStartLagIp()
	{
		return hopStartLagIPs;
	}

	public void addHopStartLagId(String id)
	{
		if(!checkIfStringExists(hopStartLagIds, id))
			hopStartLagIds.add(id);
	}

	public List<String> getHopStartLagId()
	{
		return hopStartLagIds;
	}


	public void addPPHip(String ip)
	{
		if(!checkIfStringExists(PPHIps, ip))
			PPHIps.add(ip);
	}

	public List<String> getPPHIps()
	{
		return PPHIps;
	}

	public void setIsPtpMpt(boolean isPtpMpt) {
		this.isPtpMpt = isPtpMpt;
	}

	public boolean getIsPtpMpt() {
		return this.isPtpMpt;
	}

	public void setIsFBSPtp(boolean isFBSPtp) {
		this.isFBSPtp = isFBSPtp;
	}

	public boolean getIsFBSPtp() {
		return this.isFBSPtp;
	}

	public void setGroupFBS(Group groupFBS) {
		this.groupFBS = groupFBS;
	}

	public void setIsSubAlarm(boolean isSubAlarm) {
		this.isSubAlarm = isSubAlarm;
	}

	public boolean getIsSubAlarm() {
		return this.isSubAlarm;
	}

	public void setMobility(String mobility) {
		this.mobility = mobility;
	}

	public String getMobility() {
		return this.mobility;
	}

	public void setIsCiena_LD_FRU_SLOT(boolean isCiena_LD_FRU_SLOT) {
		this.isCiena_LD_FRU_SLOT = isCiena_LD_FRU_SLOT;
	}

	public boolean getIsCiena_LD_FRU_SLOT() {
		return this.isCiena_LD_FRU_SLOT;
	}

	public void setIsCiena_LD_FRU_CARD(boolean isCiena_LD_FRU_CARD) {
		this.isCiena_LD_FRU_CARD = isCiena_LD_FRU_CARD;
	}

	public boolean getIsCiena_LD_FRU_CARD() {
		return this.isCiena_LD_FRU_CARD;
	}

	public int getNumberOfWatches() {
		return numberOfWatches;
	}

	public void setNumberOfWatches(int numberOfWatches) {
		this.numberOfWatches = numberOfWatches;
	}

	public int getNumberOfExpiredWatches() {
		return numberOfExpiredWatches;
	}

	public void setNumberOfExpiredWatches(int numberOfExpiredWatches) {
		this.numberOfExpiredWatches = numberOfExpiredWatches;
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
	///

	public String getRemoteDevice_SavpnSiteID() {
		return remoteDevice_SavpnSiteID;
	}

	public void setRemoteDevice_SavpnSiteID(String remoteDevice_SavpnSiteID) {
		this.remoteDevice_SavpnSiteID = remoteDevice_SavpnSiteID;
	}

	public String getLocalDevice_SavpnSiteID() {
		return localDevice_SavpnSiteID;
	}

	public void setLocalDevice_SavpnSiteID(String localDevice_SavpnSiteID) {
		this.localDevice_SavpnSiteID = localDevice_SavpnSiteID;
	}

	public int getBfDownThreshold() {
		return bfDownThreshold;
	}

	public void setBfDownThreshold(int bfDownThreshold) {
		this.bfDownThreshold = bfDownThreshold;
	}


	public void clearCustomFields(SyslogAlarm a)
	{
		CustomFields cflds = a.getCustomFields();
		List<CustomField> customFields = cflds.getCustomField();
		for (CustomField cf:customFields)
			a.setCustomFieldValue(cf.getName(), "");
	}

	public String getSyslogReason(SyslogAlarm a)
	{
		/*
		messages = "Jul 11 10:07:19  volt cfmd[5033]: CFMD_CCM_DEFECT_RMEP: CFM defect: Remote CCM timeout detected by MEP on Level: 3 MD: VPWS:26983:FBS-3 MA: serv Interface: ge-2/2/0.1100"
		reaspn = "CFMD_CCM_DEFECT_RMEP: CFM defect: Remote CCM timeout detected by MEP on Level: 3 MD: VPWS:26983:FBS-3 MA: serv Interface: ge-2/2/0.1100"
		 */
		String reason = a.getCustomFieldValue("messages");
		if (log.isTraceEnabled())
			log.trace("sys log messages = " + reason);
		for (int i = 0; i < 3; i++)
		{
			if (reason.contains(":"))
			{
				int idx = reason.indexOf(":");
				if (idx != -1)
					reason = reason.substring(idx+1);
				else
				{
					reason = "";
					break;
				}
			}
			else
			{
				reason = "";
				break;
			}
		}
		return reason;
	}

	protected void procPtpOneAlarm(SyslogAlarm ta, String f_evcNodeInstance, FBSPtpData fp, String reason)
	{
		if (log.isTraceEnabled())
			log.trace("procPtpOneAlarm runs.");
		SyslogAlarm syntheticA = null;
		try {
			syntheticA = ta.clone();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.trace("procPtpOneAlarm: ERROR:"
					+ Arrays.toString(e.getStackTrace()));
			return;
		}
		String alert_id = syntheticA.getCustomFieldValue("alert-id");
		String purgeIntvl = syntheticA.getCustomFieldValue("purge-interval");
		String fe_time_stamp = syntheticA.getCustomFieldValue("fe_time_stamp");
		String be_time_stamp = syntheticA.getCustomFieldValue("be_time_stamp");
		String eventKey = syntheticA.getCustomFieldValue(GFPFields.EVENT_KEY);
		String sequenceNumber = syntheticA.getCustomFieldValue(GFPFields.SEQNUMBER);
		String G2Suppress = syntheticA.getCustomFieldValue("G2Suppress");
		String cdcSptType = syntheticA.getCustomFieldValue("cdc-subscription-type");
		String alarmObjectType = syntheticA.getCustomFieldValue("sm-class");
		clearCustomFields(syntheticA);
		syntheticA.setIdentifier(syntheticA.getIdentifier()+"_Synthetic");
		if (alarmObjectType != null)
			syntheticA.setCustomFieldValue("sm-class", alarmObjectType);
		syntheticA.setCustomFieldValue(GFPFields.ALERT_ID, alert_id+"_Synthetic");
		syntheticA.setCustomFieldValue(GFPFields.SEQNUMBER, sequenceNumber);
		if (G2Suppress != null)
			syntheticA.setCustomFieldValue("G2Suppress", G2Suppress);
		if (cdcSptType != null)
			syntheticA.setCustomFieldValue("cdc-subscription-type", cdcSptType);
		syntheticA.setCustomFieldValue("purge-interval", purgeIntvl);
		String originatingManagedEntity = syntheticA.getOriginatingManagedEntity();
		String evcnodeInstance = originatingManagedEntity.split(" ")[1];
		String evcInstance = evcnodeInstance.split("/")[1];

		originatingManagedEntity = originatingManagedEntity.replace(evcnodeInstance, f_evcNodeInstance);
		syntheticA.setOriginatingManagedEntity(originatingManagedEntity);
		syntheticA.setCustomFieldValue("classification", "EMT-CFM-CFO");
		syntheticA.setPerceivedSeverity(PerceivedSeverity.MAJOR);
		syntheticA.setCustomFieldValue("domain", "IPAG");
		syntheticA.setCustomFieldValue("node-name", fp.clli);
		syntheticA.setCustomFieldValue("fe_time_stamp", fe_time_stamp);
		syntheticA.setCustomFieldValue("be_time_stamp", be_time_stamp);
		syntheticA.setCustomFieldValue(GFPFields.EVENT_KEY, eventKey);

		String component = "deviceType=<"+fp.device_type+"> deviceModel=<"+fp.device_model+"> PortAID=<"+fp.port_aid+">";
		syntheticA.setCustomFieldValue("component", component);

		syntheticA.setCustomFieldValue("reason", reason);
		syntheticA.setCustomFieldValue("ticketNumber", alert_id);

		syntheticA.setCustomFieldValue("vrf", evcInstance);

		String flags = "MptCFM=<N> MPAType=<> ProductType=<FBS> FBSID=<"+fp.evc_name+">";
		syntheticA.setCustomFieldValue("flags", flags);
		syntheticA.setCustomFieldValue("evcid", fp.evc_name);

		Util.whereToSendThenSend((EnrichedAlarm) syntheticA, false);
		String vrfname = ta.getCustomFieldValue("vrf-name");
		addSyntheticA2map(vrfname, syntheticA);
	}

	protected void procPtpTwoAlarms(SyslogAlarm ta, FBSPtpData p, FBSPtpData sp, String reason)
	{
		if (log.isTraceEnabled())
			log.trace("procPtpTwoAlarms runs.");
		SyslogAlarm syntheticA = null;
		try {
			syntheticA = ta.clone();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.trace("procPtpTwoAlarms: ERROR:"
					+ Arrays.toString(e.getStackTrace()));
			return;
		}
		String alert_id = syntheticA.getCustomFieldValue("alert-id");
		String purgeIntvl = syntheticA.getCustomFieldValue("purge-interval");
		String fe_time_stamp = syntheticA.getCustomFieldValue("fe_time_stamp");
		String be_time_stamp = syntheticA.getCustomFieldValue("be_time_stamp");
		String eventKey = syntheticA.getCustomFieldValue(GFPFields.EVENT_KEY);
		String sequenceNumber = syntheticA.getCustomFieldValue(GFPFields.SEQNUMBER);
		String G2Suppress = syntheticA.getCustomFieldValue("G2Suppress");
		String cdcSptType = syntheticA.getCustomFieldValue("cdc-subscription-type");
		String alarmObjectType = syntheticA.getCustomFieldValue("sm-class");
		clearCustomFields(syntheticA);
		syntheticA.setIdentifier(syntheticA.getIdentifier()+"_Synthetic");
		if (alarmObjectType != null)
			syntheticA.setCustomFieldValue("sm-class", alarmObjectType);
		syntheticA.setCustomFieldValue(GFPFields.ALERT_ID, alert_id+"_Synthetic");
		syntheticA.setCustomFieldValue(GFPFields.SEQNUMBER, sequenceNumber);
		if (G2Suppress != null)
			syntheticA.setCustomFieldValue("G2Suppress", G2Suppress);
		if (cdcSptType != null)
			syntheticA.setCustomFieldValue("cdc-subscription-type", cdcSptType);
		syntheticA.setCustomFieldValue("purge-interval", purgeIntvl);
		String evcnodeInstance = syntheticA.getOriginatingManagedEntity().split(" ")[1];
		String evcInstance = evcnodeInstance.split("/")[1];


		syntheticA.setCustomFieldValue("classification", "EMT-CFM-CFO");
		syntheticA.setPerceivedSeverity(PerceivedSeverity.MAJOR);
		syntheticA.setCustomFieldValue("domain", "IPAG");
		syntheticA.setCustomFieldValue("node-name", p.clli);
		syntheticA.setCustomFieldValue("fe_time_stamp", fe_time_stamp);
		syntheticA.setCustomFieldValue("be_time_stamp", be_time_stamp);
		syntheticA.setCustomFieldValue(GFPFields.EVENT_KEY, eventKey);
		syntheticA.setCustomFieldValue("reason", reason);
		syntheticA.setCustomFieldValue("ticketNumber", alert_id);

		syntheticA.setCustomFieldValue("vrf", evcInstance);
		String flags = "MptCFM=<N> MPAType=<> ProductType=<FBS> FBSID=<"+p.evc_name+"> PtpCFM=<BOTH>";
		syntheticA.setCustomFieldValue("flags", flags);
		syntheticA.setCustomFieldValue("evcid", p.evc_name);

		String info = "";
		String infosa = "";
		if (p != null)
		{
			// info = "CLLI=<"+p.device_name+"> PortAID=<"+p.port_aid+"> FBSID=<"+p.evc_name+"> MPAType=<"+p.mpa_connect_type+">";
			info = "CLLI=<"+p.device_name+"> PortAID=<"+p.port_aid+"> FBSID=<"+p.evc_name+"> MPAType=<>";
		}
		if (sp != null)
		{
			// infosa = "CLLI=<"+sp.device_name+"> PortAID=<"+sp.port_aid+"> FBSID=<"+sp.evc_name+"> MPAType=<"+sp.mpa_connect_type+">";
			infosa = "CLLI=<"+sp.device_name+"> PortAID=<"+sp.port_aid+"> FBSID=<"+sp.evc_name+"> MPAType=<>";
		}

		syntheticA.setCustomFieldValue("info", info+" "+infosa);
		Util.whereToSendThenSend((EnrichedAlarm) syntheticA, false);
		String vrfname = ta.getCustomFieldValue("vrf-name");
		addSyntheticA2map(vrfname, syntheticA);
	}

	public static void addSyntheticA2map(String vrfname, SyslogAlarm syna)
	{
		HashSet<SyslogAlarm> as = null;
		if (log.isTraceEnabled())
			log.trace("add synthetic alarm = " + syna.getIdentifier() +" to map for vrf name = " + vrfname);
		if (synaMap.containsKey(vrfname))
		{
			as = synaMap.get(vrfname);
			as.add(syna);
		}
		else
		{
			as = new HashSet<SyslogAlarm>(); 
			as.add(syna);
			synaMap.put(vrfname, as);
		}
	}

	public void clearSyntheticAlarm(String vrfname, String seqNumber, boolean isGenByUCA, boolean isPurgeItvlExp)
	{
		if (synaMap.containsKey(vrfname))
		{
			HashSet<SyslogAlarm> as = synaMap.get(vrfname);
			Iterator i = as.iterator();
			while(i.hasNext())
			{
				SyslogAlarm syna = (SyslogAlarm)i.next();
				syna.setSeverity(4);
				syna.setCustomFieldValue(GFPFields.LAST_CLEAR_TIME, ""+System.currentTimeMillis()/1000);
				syna.setCustomFieldValue(GFPFields.SEQNUMBER, seqNumber);
				if (isGenByUCA == true)
					syna.setCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA, "YES"); 
				if (isPurgeItvlExp == true)
					syna.setCustomFieldValue(GFPFields.IS_PURGE_INTERVAL_EXPIRED, "YES"); 
				syna.setIsSent(false); // so it can be sent
				log.info("Send clear for synthetic alarm = " + syna.getIdentifier());
				Util.whereToSendThenSend(syna, false);
			}
			if (log.isTraceEnabled())
				log.trace("remove vrfname = " + vrfname + " from synthetic alarm map");
			synaMap.remove(vrfname);
		}
		else
			if (log.isTraceEnabled())
				log.trace("No active synthetic alarms with vrf name = " + vrfname);
	}

	public void fbsPtpCallBack() {
		try {
			SyslogAlarm ta = (SyslogAlarm) this;
			if (log.isTraceEnabled())
				log.trace("fbsPtpCallBack() run. trigger id = " + ta.getIdentifier());
			if (!ta.getCanProcess())
			{
				log.info("Stop processing FBS Ptp as trigger alarm cannot be prcessed(ie, clear or suppressed)");
				return;
			}
			if (ta.getIsSubAlarm())
			{
				log.info("Stop processing FBS Ptp as trigger alarm is the subalarm in other FBSPtp group.");
				return;
			}
			// Scenario scenario = ScenarioThreadLocal.getScenario();
			// get the groups where this alarm is present
			// Collection<Group> groups = PD_Service_Group.getGroupsOfAnAlarm(scenario, this);
			Group group = groupFBS;
			if (log.isTraceEnabled())
				log.trace("group name = " + group.getName());
			SyslogAlarm sa = null;
			/*
		for (Group groupx : groups)
		{
			log.info("group name = " + groupx.getName());
			// this trigger should belong to one Correlation group
			if (groupx.getTrigger() == this)
			{
				group = groupx;
				break;
			}
		}
			 */
			if (group == null)
			{
				if (log.isTraceEnabled())
					log.trace("The Trigger alarm doesn't belong to any group");
				return;
			}
			if (log.isTraceEnabled())
				log.trace("continue processing... ");

			for (Alarm ax : group.getAlarmList())
			{
				if (log.isTraceEnabled())    
					log.trace("member ID = " + ax.getIdentifier());
			}
			for (Alarm alarm : group.getAlarmList())
			{
				if (log.isTraceEnabled())
					log.trace("member id = " + alarm.getIdentifier());
				if ((SyslogAlarm)alarm != this)   // 'this' is trigger
				{
					sa = (SyslogAlarm)alarm;
					if (sa.getCanProcess())
						// at most one subalarm
						break;
					else
					{
						sa = null;
						if (log.isTraceEnabled())
							log.trace("Stop processing FBS Ptp as one alarm cannot be prcessed(ie, clear or suppressed)");

						return;
					}
				}
			}
			/*
		if (!ta.getCanProcess())
		{
			if (sa == null)
			{
				log.info("The Trigger alarm is cleared/suppressed. stop FBS PTP process as the group doesn't exist.");
				return;
			}
			else
			{
				log.info("The Trigger alarm is cleared/suppressed. subalarm becomes trigger alarm.");
				ta = sa;   // subalarm becomes trigger alarm
				sa = null;
			}
			log.info("Stop processing FBS Ptp as trigger alarm cannot be prcessed(ie, clear or suppressed)");
			return;
		}
			 */
			if (sa != null)
				if (log.isTraceEnabled())
					log.trace("TWO alarms.");

			String eventKey = ta.getCustomFieldValue(GFPFields.EVENT_KEY);
			if (log.isTraceEnabled())
				log.trace("fbsptt trigger event key = " + eventKey);
			if (eventKey.equals("50004/1/11"))
			{
				if (sa != null)
				{
					if (log.isTraceEnabled())
						log.trace("fbsptp two alarms.");
					FBSPtpData p = NodeManager.FetchFBSSyslogPtpDataTwoAlarms(ta);
					FBSPtpData sp = NodeManager.FetchFBSCienaPtpDataTwoAlarms(sa);
					// String reason = getSyslogReason(ta);
					String reason = ta.getCustomFieldValue("reason");
					procPtpTwoAlarms(ta, p, sp, reason);
				}
				else
				{
					if (log.isTraceEnabled())
						log.trace("fbsptp one alarm.");
					String f_evcNodeInstance = NodeManager.getFBSSyslogFaultyend(ta);
					if (f_evcNodeInstance == null)
					{
						if (log.isTraceEnabled())
							log.trace("Drop FBS Syslog CFM alarm.");
						return;
					}
					// syslog's faulty end is ciena
					FBSPtpData fp = NodeManager.FetchFBSCienaPtpDataOneAlarm(f_evcNodeInstance);
					// String reason = getSyslogReason(ta);
					String reason = ta.getCustomFieldValue("reason");
					procPtpOneAlarm(ta, f_evcNodeInstance, fp, reason);
				}
			}
			else if (eventKey.equals("50002/100/52"))
			{
				if (sa != null)
				{
					if (log.isTraceEnabled())
						log.trace("fbsptp two alarms.");
					FBSPtpData p = NodeManager.FetchFBSCienaPtpDataTwoAlarms(ta);
					FBSPtpData sp = NodeManager.FetchFBSSyslogPtpDataTwoAlarms(sa);
					String reason = ta.getCustomFieldValue("reason");
					procPtpTwoAlarms(ta, p, sp, reason);
				}
				else
				{
					if (log.isTraceEnabled())
						log.trace("fbsptp one alarm.");
					String f_evcNodeInstance = NodeManager.getFBSCienaFaultyend(ta);
					if (f_evcNodeInstance == null)
					{
						if (log.isTraceEnabled())
							log.trace("Drop FBS Ciena CFM alarm.");
						return;
					}
					// ciena's faulty end is syslog
					FBSPtpData fp = NodeManager.FetchFBSSyslogPtpDataOneAlarm(f_evcNodeInstance);
					String reason = ta.getCustomFieldValue("reason");
					procPtpOneAlarm(ta, f_evcNodeInstance, fp, reason);
				}
			}

		} catch (Exception e) {
			log.error("fbsPtpCallBack() failed. ", e);
		}
	}

	public void FRUColdStartCallBack() {
		// when the callback is called by more than one problem groups
		// blow checking is required
		if (log.isTraceEnabled())
			log.trace("FRUColdStartCallBack() runs.");

		// increment the number of watch expirations
		setNumberOfExpiredWatches(getNumberOfExpiredWatches() + 1);
		if (log.isTraceEnabled())
			log.trace("WATCHES ARE:" + getNumberOfExpiredWatches() + " - " + getNumberOfWatches());
		// if this is the last watch to expire
		if(getNumberOfExpiredWatches() != getNumberOfWatches())
			return;
		setNumberOfExpiredWatches(0);
		setNumberOfWatches(0);
		if (!getCanProcess())
			return;
		if (getIsPtpMpt())
			Util.whereToSendThenSend(this, false);
		else
			// if this alarm's getIsFBSPtp is true, it will be
			// blocked in Util.sendAlarm()
			Util.whereToSendThenSend(this, false);
	}


	public void simpleSendCallBack() {
		// when the callback is called by more than one problem groups
		// blow checking is required
		if (log.isTraceEnabled())
			log.trace("simpleSendCallBack() runs.");

		// increment the number of watch expirations
		setNumberOfExpiredWatches(getNumberOfExpiredWatches() + 1);
		if (log.isTraceEnabled())
			log.trace("WATCHES ARE:" + getNumberOfExpiredWatches() + " - " + getNumberOfWatches());
		// if this is the last watch to expire
		if(getNumberOfExpiredWatches() != getNumberOfWatches())
			return;
		setNumberOfExpiredWatches(0);
		setNumberOfWatches(0);

		Util.whereToSendThenSend(this, false);

	}

	/**
	 * 
	 */
	public void rpdRsvpByPassDownexpirationCallBack() {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "rpdRsvpByPassDownexpirationCallBack()", getIdentifier());
		}
		boolean noSend = false;
		Scenario scenario = ScenarioThreadLocal.getScenario();

		// if we already sent the trigger, then we are done here
		if(getCanSend()) {  
			// all of this code is here just to make sure...   
			// probably not needed unless something weird happens    
			// unless the time windows are different then we need this 
			// code  

			// increment the number of watch expirations
			setNumberOfExpiredWatches(getNumberOfExpiredWatches() + 1);

			// get the groups where this alarm is present
			Collection<Group> groups = PD_Service_Group.getGroupsOfAnAlarm(scenario, this);

			// if there is only one group that has this alarm 
			if(groups.size() == 1) {
				// there is only one group so this is the last watch to expire
				setNumberOfExpiredWatches(0);
				setNumberOfWatches(0);

				// is there more than one alarm in this group
				for (Group group : groups) {
					if(group.getNumber() > 1 && group.getTrigger() == this) {
						// don't send out the alarm here cuz there are more alarms in the group
						// this code should never execute because the time window should have 
						// expired and the alarm would have already been sent by createProblemAlarm
						// in the actions factory
						noSend = true;
					}
				}						
			} else {
				// there are more than one group with this alarm
				// we have to go thru the groups to see what is up

				// if this is the last watch to expire
				if(getNumberOfExpiredWatches() == getNumberOfWatches()) {
					setNumberOfExpiredWatches(0);
					setNumberOfWatches(0);

					// if there are any groups with more than one alarm in it 
					// don't send out the trigger here
					for (Group group : groups) {
						if(group.getNumber() > 1 && group.getTrigger() == this)
							noSend = true;
					}				
				} else {
					// this is not the last watch to expire so don't send it out
					// yet
					noSend = true;
				}
			}

			if (noSend == false && isSuppressed() == false) {
				// cascade alarm
				if (log.isTraceEnabled())
					log.trace("rpdRsvpByPassDownexpirationCallBack: met the sent critiria");
				Util.whereToSendThenSend(this, false);
			}
		} else {
			if (log.isTraceEnabled()) { 
				LogHelper.method(log, "rpdRsvpByPassDownexpirationCallBack()", " The trigger alarm "+getIdentifier() + "is suppressed");
			}
			setNumberOfExpiredWatches(0);
			setNumberOfWatches(0);
		}
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "rpdRsvpByPassDownexpirationCallBack()"); 
		}
	}

	public void bfdownCallBack() {
		// when the callback is called by more than one problem groups
		// below checking is required
		if (log.isTraceEnabled())
			log.trace("bfdownCallBack() runs.");


		// increment the number of watch expirations
		setNumberOfExpiredWatches(getNumberOfExpiredWatches() + 1);
		if (log.isTraceEnabled())
			log.trace("WATCHES ARE:" + getNumberOfExpiredWatches() + " - " + getNumberOfWatches());
		// if this is the last watch to expire
		if(getNumberOfExpiredWatches() != getNumberOfWatches())
			return;
		setNumberOfExpiredWatches(0);
		setNumberOfWatches(0);

		// This alarm comes in is only associated with BFDown.
		// It can only be a trigger for this group
		boolean aggregate = false;
		Group lower_suppress = null;

		Collection<Group> groups = PD_Service_Group.getGroupsWhereAlarmSetAsCandidateSubAlarmOrTrigger(this, null);
		for (Group group : groups) {

			log.trace(group.getName() + " # of alarms" + group.getNumber());
			if(group.getTrigger() == this ){
				//			group.getName().contains("Supress_Lower_SiteID")
				if ( (group.getName().contains("Remote") ||  group.getName().contains("Local"))
						&& (group.getNumber() >= ((SyslogAlarm) this).getBfDownThreshold()) ) {
					aggregate = true;
					processBFDownAggregate(group);
				}

				if (group.getName().contains("Supress_Lower_SiteID"))
					lower_suppress = group;
			}
		}

		/*
		 * 
		 */
		if (!aggregate	&& lower_suppress != null)	
			processBFDownLowerSuppress(lower_suppress);
		else

			Util.whereToSendThenSend(this, false);

	}

	private void processBFDownLowerSuppress(Group group) {

		if (log.isTraceEnabled()) { 
			LogHelper.enter(log, "processBFDownLowerSuppress()", group.getName());
		}

		/*
		 * if there is less than threshold apply the check siteId to see which is greater. suppress the lower savid
		 * if greater than the threshold you supply the aggregate. suppress everything
		 * */

		if (log.isTraceEnabled())
			log.trace("There are only "+ group.getNumber() +"alarms in group "+ group.getName() );
		if (group.getNumber() > 1){
			for(Alarm alarm:group.getAlarmList())
			{
				if(group.getTrigger()!= alarm)
				{
					// forward the alarm with the highest site id
					if(Integer.parseInt(((SyslogAlarm) group.getTrigger()).getRemoteDevice_SavpnSiteID()) >= 
							Integer.parseInt(((SyslogAlarm) group.getTrigger()).getLocalDevice_SavpnSiteID())) {

						// this is just for Junit testing so we can tell if this worked or not
						if (log.isTraceEnabled())
							log.trace("Suppressing subalarm: " + alarm.getIdentifier());

						((SyslogAlarm) alarm).setSuppressed(true);

						Util.whereToSendThenSend((EnrichedAlarm) group.getTrigger(), false);
					}
					else {
						// this is just for Junit testing so we can tell if this worked or not
						if (log.isTraceEnabled())
							log.trace("Suppressing trigger: " + group.getTrigger().getIdentifier());

						((SyslogAlarm) group.getTrigger()).setSuppressed(true);

						Util.whereToSendThenSend((EnrichedAlarm) alarm, false);	
					}
				}
			}
		}
		else {
			if (log.isTraceEnabled())
				log.trace("Only Single Alarm received...");

			// Single Alarm, send out the trigger
			Util.whereToSendThenSend((EnrichedAlarm) group.getTrigger(), false);

		}

	}

	private void processBFDownAggregate(Group group) {
		boolean found = false;

		if (log.isTraceEnabled()) { 
			LogHelper.enter(log, "processBFDownAlarmAggregated()", group.getName());
		}

		//reference Alarm is the trigger for the group
		//		If more alarms than the threshold meet criteria then we need to generate a NEW, CORRelated event against the remote siteId object.  
		//Note if there already an existing-active-corr-event then 
		//			Log  Active CORRelation event [the gevm-category of M]-[the key of remoteDev]-CORR is already posted to Ruby.  No need to publish another one."
		//			Done

		/*
		 * if there is less than threshold apply the check siteId to see which is greater. suppress the lower savid
		 * if greater than the threshold you supply the aggregate. suppress everything
		 * */

		if (log.isTraceEnabled())
			log.trace("The threshold for this alarm is " + ((SyslogAlarm) this).getBfDownThreshold() + " !!");


		if (log.isTraceEnabled())
			log.trace("The number of alarms exceeds the threshold...");

		for(Alarm alarm:group.getAlarmList())
		{
			if(alarm.getCustomFieldValue(GFPFields.REASON_CODE).contains("CORR"))
			{
				log.info("Active CORRelation event " + this.getCustomFieldValue(GFPFields.EVENT_KEY) +
						"-" + alarm.getCustomFieldValue(GFPFields.EVENT_KEY) + "-CORR is already posted to Ruby.  No need to publish another one.");
				found =true;
				break;
			}
		}
		if (log.isTraceEnabled())
			log.trace("Found an existing correlated alarm? : "+ found);

		// we don't have a correlated alarm so we generate one
		if(!found)
		{
			// Set the info fields with PTNII info
			getPtniiInfo (group, this);

			// Clone the Trigger Alarm to build the new alarm to send 
			SyslogAlarm a = null;
			try {
				a = ((SyslogAlarm) this).clone();
			} catch (CloneNotSupportedException e) {
				log.trace("processBFDownAggregate: ERROR:"+ Arrays.toString(e.getStackTrace()));
			}



			//we need to generate a NEW, CORRelated event against the remote siteId object.
			//set variables
			//reason-text = "RPD_LAYER2_VC_BFD_DOWN (dataplane), aggregate";
			a.setOriginatingManagedEntity("DEVICE " + this.getRemoteDeviceIpaddr());
			a.setCustomFieldValue(GFPFields.REASON,  "RPD_LAYER2_VC_BFD_DOWN (dataplane), aggregate");
			a.setCustomFieldValue(GFPFields.REMOTEDEVICENAME, this.getRemoteDeviceIpaddr());
			a.setCustomFieldValue(GFPFields.REASON_CODE, "CORR");
			a.setPerceivedSeverity(PerceivedSeverity.CRITICAL);
			a.setSuppressed(false);
			
			//the component / reason for a one-to-many:
			// Component:           LocalSite=<local device name>, RemoteSite=<trap cnt> devices 
			boolean local=false;
			if (group.getName().contains("Local")){
				a.setCustomFieldValue(GFPFields.COMPONENT, "LocalSite=[" + this.getPtnii() +"]"+  
						", RemoteSite=["+group.getNumber() +"] devices");
				a.setIdentifier(a.getIdentifier() + "-LOCAL-CORR");

				local=true;

				log.info("Suppressing raw events because agg_cnt correlation criteria were " +
						"met for local site: " + this.getLocalDevice_SavpnSiteID() +
						"| Needed more than [" + this.getBfDownThreshold() + "] within corr-window " + 
						"[3 minutes].  Had [" + group.getNumber()
						+"] match.");
			}
			else{
				// The component / reason for  a many-to-one:
				//Component:            LocalSite=<local device name>|<trap cnt> devices, RemoteSite=<remote device name>
				String component = "";
				component = this.getCustomFieldValue(GFPFields.COMPONENT);
				String remoteSite = component.substring(component.lastIndexOf("=")+1,component.length());
						
				a.setCustomFieldValue(GFPFields.COMPONENT, "LocalSite=[" + group.getNumber()+"]"+  
						"devices, RemoteSite=["+remoteSite +"]");
				a.setIdentifier(a.getIdentifier() + "-REMOTE-CORR");

				log.info("Suppressing raw events because agg_cnt correlation criteria were " +
						"met for remote site: " + this.getRemoteDevice_SavpnSiteID() +
						"| Needed more than [" + this.getBfDownThreshold() + "] within corr-window " + 
						"[3 minutes].  Had [" + group.getNumber()
						+"] match.");
			}

			a.setCustomFieldValue(GFPFields.EMS, "syslog");


			// Sent the Corr alarm  out to AM.
			a.setTargetValuePack("IpagJuniperSyslogPdvp-0.3");
			SyslogAlarm b = (SyslogAlarm) a;
			Util.whereToSendThenSend(b, false);

			// Sent the CORR Alarm back to dispatcher so it stays in WM
			a.setCustomFieldValue("SavpnSiteID_Remote", this.getRemoteDevice_SavpnSiteID());
			a.setCustomFieldValue("SavpnSiteID_Local",this.getLocalDevice_SavpnSiteID());

			//PD sets fields in the alarm that identify the grouping, scenario, and etc.   These fields have to be cleared
			// if the alarm is sent back to the same PD value back because having these fields set in an incoming alarm will confuse PD
			resetAllInternalFields(a);
			if (local)
				a.setCustomFieldValue("AlarmGrouping", "LOCAL");
			else
				a.setCustomFieldValue("AlarmGrouping", "REMOTE");

			Bootstrap.getInstance().getDispatcher().enqueueAlarm(a);

		}

		// here we mark all alarms suppressed because the threshold was reached and send out the correlated alarm
		// if one is there
		for(Alarm alarm:group.getAlarmList())
		{
			if(alarm instanceof SyslogAlarm) {
				SyslogAlarm ea = (SyslogAlarm) alarm;

				if(!ea.getCustomFieldValue(GFPFields.REASON_CODE).contains("CORR"))
					ea.setSuppressed(true);
				else 
					Util.whereToSendThenSend(ea, false);
			}
		}
	}


	/**
	 * @param alarmToReset
	 */
	private void resetAllInternalFields(Alarm alarmToReset) {

		alarmToReset.getPassingFilters().clear();
		alarmToReset.getPassingFiltersTags().clear();
		alarmToReset.getPassingFiltersParams().clear();
		alarmToReset.getSourceScenarios().clear();

		alarmToReset.setOperatorState(OperatorState.NOT_ACKNOWLEDGED);
		alarmToReset.setNetworkState(NetworkState.NOT_CLEARED);
		alarmToReset.setProblemState(ProblemState.NOT_HANDLED);

		alarmToReset.getStateChanges().clearAndResetFlag();
		alarmToReset.getAttributeValueChanges().clearAndResetFlag();

		alarmToReset.getVar().clear();
	}

	private  int getPtniiInfo(Group passedGroup, SyslogAlarm cloneAlarm) {
		// Build the info field for the CORR alarm

		String info = null;
		int cnt = 0;
		// Initialize to empty values for 3 info fields
		// Put 21 PTNII on each info lines. Each PTNII has 11 characters
		cloneAlarm.setCustomFieldValue(GFPFields.INFO1,"");
		cloneAlarm.setCustomFieldValue(GFPFields.INFO2,"");
		cloneAlarm.setCustomFieldValue(GFPFields.INFO3,"");

		boolean local=false;
		if (passedGroup.getName().contains("Local"))
			local=true;
		

		for(Alarm alarm:passedGroup.getAlarmList())
		{
			String ptnii = null;
			SyslogAlarm sa = (SyslogAlarm) alarm;

			if (local && null != sa.getRemoteDeviceIpaddr())
				ptnii = NodeManager.getRemotePtnii(sa);
			else if (!local)
				ptnii = sa.getPtnii();


			if ( null != ptnii  && !ptnii.isEmpty() && !sa.getInInfoList())
			{
				// Set variable in Alarm that his alarm was counted
				
				if (cnt < 21) 
					info = GFPFields.INFO1;
				else if (cnt >= 21 && cnt < 42)
					info = GFPFields.INFO2;
				else if (cnt >= 42 && cnt < 63)
					info = GFPFields.INFO3;

				if (cnt == 0 || cnt == 21 || cnt == 42)
					cloneAlarm.setCustomFieldValue(info, ptnii);
				else if (cnt == 63){
					cloneAlarm.setCustomFieldValue(GFPFields.INFO3,cloneAlarm.getCustomFieldValue(GFPFields.INFO3) + ",");
					break;
				}
				else 
					cloneAlarm.setCustomFieldValue(info,cloneAlarm.getCustomFieldValue(info) +"," + ptnii);


				if (cnt == 21 )
					cloneAlarm.setCustomFieldValue(GFPFields.INFO1,cloneAlarm.getCustomFieldValue(GFPFields.INFO1) + ",");
				if (cnt == 42 )
					cloneAlarm.setCustomFieldValue(GFPFields.INFO2,cloneAlarm.getCustomFieldValue(GFPFields.INFO2) + ",");
				
				sa.setInInfoList(true);
				cnt++;
			}
		}
		return cnt;
	}

}
