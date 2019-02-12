package com.att.gfp.ciena.cienaPD;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.att.gfp.ciena.cienaPD.topoModel.IpagCienaTopoAccess;
import com.att.gfp.ciena.cienaPD.topoModel.NodeManager;
import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.data.ipagAlarm.AlarmState;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.StandardFields;
import com.att.gfp.helper.GFPUtil;
//import com.att.gfp.ciena.externdata.DevTypeAndSeveritySuppressionProperties;
// import com.att.gfp.ciena.externdata.TunableProperties;
// import com.att.gfp.ciena.cienaScenario.util.service_util;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.alarm.AlarmCommon;
import com.hp.uca.expert.alarm.AlarmUpdater;
import com.hp.uca.expert.alarm.AlarmUpdater.UsualVar;
import com.hp.uca.expert.lifecycle.LifeCycleAnalysis;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.scenario.ScenarioStatus;
import com.hp.uca.expert.x733alarm.AttributeChange;
import com.hp.uca.expert.x733alarm.CustomField;
import com.hp.uca.expert.x733alarm.NetworkState;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;

public class CienaExtendedLifeCycle extends LifeCycleAnalysis {
	private IpagCienaTopoAccess topo;
	private static Logger log = LoggerFactory.getLogger(CienaExtendedLifeCycle.class);
	private static NodeManager nmgr;

	private static ApplicationContext context = null;
	public CienaExtendedLifeCycle(Scenario scenario) {
		super(scenario);
		if (context == null)
		{
			if (log.isInfoEnabled())
				log.info("Get Application Context.");
			// its location is defined in classpath
			// ApplicationContext context = new ClassPathXmlApplicationContext("context.xml");
			context = scenario.getVPApplicationContext();
			nmgr = new NodeManager();
		}

		/*
		CienaBean cb = (CienaBean)
		getScenario().getProblems().getMyApplicationContext().getBean("cienaBean");
		 */

		/*
		 * If needed more configuration, use the context.xml to define
		 * any beans that will be available here using 
		 * scenario.getGlobals()
		 */
		scenario.getGlobals();
		// topo = new IpagCienaTopoAccess();
		// slog = log;
		// Util.setLogger(slog);
		// Util.setLogger(log);
	}

	@Override
	// if two alarms has the same <identifier> in xml files ==> the second
	// alarm will be dropped by UCA
	public AlarmCommon onAlarmCreationProcess(Alarm alarmx) {
		Alarm alarm = alarmx; 
		try {
			if (topo == null)
				topo = new IpagCienaTopoAccess();
			if (!(alarmx instanceof EnrichedAlarm))
				alarm = GFPUtil.populateEnrichedAlarmObj(alarmx);
			String axml = alarm.toXMLString();
			axml = axml.replaceAll("\\n", " ");
			log.info("Incoming alarm: "+axml);
			log.info("Enrichment: "+((EnrichedAlarm)alarm).toString());

			CienaAlarm cienaAlarm=null;

			try {
				cienaAlarm = new CienaAlarm(alarm);
			} catch (Exception e) {
				// this.getScenario().setStatusAndLogAndUpdateVPStatus(e.getMessage(), ScenarioStatus.Degraded);
				if (log.isInfoEnabled())
					log.info("Dropped the alarm: "+ e);
				return null;
			}
			
			if (cienaAlarm.getSeverity() == 4)
			{
				if (log.isInfoEnabled())
					log.info(alarm.getIdentifier() + ": Dropping the clear alarm as there is no corresponding active alarm.");
				return null;
			}
			
			String eventKey = cienaAlarm.getCustomFieldValue(GFPFields.EVENT_KEY);
			String reasonCode = cienaAlarm.getCustomFieldValue("reason_code");
			boolean isChronic = false;
			if (reasonCode != null && reasonCode.contains("Chronic"))
				isChronic = true;
			String moClass = alarm.getOriginatingManagedEntity().split(" ")[0];
			if ("PPORT".equals(moClass))
				topo.setDevicePportNode(cienaAlarm);
			else if ("DEVICE".equals(moClass))
				topo.setDeviceNode(cienaAlarm);
			else if (eventKey.equals("50002/100/52"))
				topo.setDeviceEvcNode(cienaAlarm);

			String G2sps = cienaAlarm.getCustomFieldValue("G2Suppress");
			////////////////////////////////////////////////////////////////
			// rule for G2Suppress:
			// We process alarms normally including decompositions 
			// except when G2Suppression = IPAG02 we dont send decomposed 
			// alarms to IPAGDEC.  The rule is the same as below:
			//
			// If suppress is IPAG02:
			// only  Suppress decomposed alarm in UCA
			// let original alarm go to AM

			// If suppress is IPAG01:
			// let original+decomposed alarm go to AM

			// if suppress is BOTH:
			// dont send any alarm to AM

			// if suppress is NULL:
			// let original+decomposed alarm go to AM
			////////////////////////////////////////////////////////////////


			boolean sendDecomposed = true;
			if ("IPAG02".equals(G2sps)) 
				sendDecomposed = false;

			log.info("EventKey = " + eventKey + ", G2Suppress = " + G2sps + ", isChronic = " + isChronic);

			boolean isFBS_PtpMpt = false;
			if (eventKey.equals("50002/100/52"))
			{
				isFBS_PtpMpt = NodeManager.isFBS_PtpMpt(cienaAlarm);
				cienaAlarm.setIsFBS_PtpMpt(isFBS_PtpMpt);
			}
			log.info("isFBS_PtpMpt = " + isFBS_PtpMpt);
			cienaAlarm.setIsFBS_PtpMpt(isFBS_PtpMpt);

			// chronic alarm doesn't have decomposed alarm, ie, chronic
			// alarm with G2Suppress=IPAG01 will be dropped by PP
			// ==> you don't need to check/drop it here
			//
			if (sendDecomposed == true && isChronic == false)
			{
				////////////////////////////////////////////////////////
				// below is the completed list for event keys that need
				// decomposed:
				// 50002/100/1,50002/100/21,50002/100/22,50002/100/52,
				// 50002/100/14, 50002/100/46, 50002/100/19,
				//
				// "50002/100/19" and "50002/100/21" are conditionally
				// decomposed, ie, their decomposed alarms are sent only
				// when its is not aaf_da. they are sent by JuniperPdvp
				// after aaf_da correlation
				// the others are unconditionaly decomposed.  they are
				// sent here
				// (1) unconditional decomposed alarm
				//		(eg, 50002/100/14)
				//	send to IPAGDEC	==> isSent2DEC is set 
				//		after send
				//	send to proper VP/AM ==> isSent is set
				//		after send
				// (2) conditional decomposed alarm
				//		(eg, 50002/100/19,21)
				//	send to proper VP  ==> isSent is set 
				//		after send
				////////////////////////////////////////////////////////
				//
				// you don't need to send conditional decomposed alarm
				// to IPAGDEC.  instead you send it to JUNIPER which
				// is responsible to send it out after aaf_da 
				// correlation
				// 
				// to make it simple, you can think this way:
				// (1) CIENA has only below 5 alarms that require
				//	decomposition
				// (2) 50002/100/19,21 does NOT need decomposition
				//
				////////////////////////////////////////////////////////
				if (eventKey.equals("50002/100/1") ||
						eventKey.equals("50002/100/22") ||
						eventKey.equals("50002/100/52") ||
						eventKey.equals("50002/100/14") ||
						eventKey.equals("50002/100/46"))
				{
					log.info("Send unconditional decomposed alarms to IPAGDEC: " + alarm.getIdentifier());
					Util.sendAlarm(cienaAlarm, true);
				}
			}

			// below is not in Gayathri requirement.  it is in email
			// dated 5-1-2014, 4:16 from Tom and 
			// 4-15-2014, 5:48pm from Gayathri
			String deviceSubRole = cienaAlarm.getDeviceSubRole();
			String domain = cienaAlarm.getCustomFieldValue(GFPFields.DOMAIN);
			String device_type = cienaAlarm.getDeviceType();
			String classification = cienaAlarm.getCustomFieldValue(GFPFields.CLASSIFICATION);

			if ("EMT".equals(deviceSubRole) && (
					("CIENA EMUX".equals(device_type) && "NFO-MOBILITY".equals(classification) && "NTE".equals(domain)) || 
					("NFO-EMUX".equals(classification)&& "EMUX".equals(domain)) || 
					("NFO-EMUX".equals(classification)&& "EMUX-PMOS".equals(domain))
					))
			{
				String info1 = (String) cienaAlarm.getCustomFieldValue(GFPFields.INFO1);
				cienaAlarm.setCustomFieldValue(GFPFields.INFO1, info1+" DeviceSubRole=<EMT>");
				if (log.isInfoEnabled())
					log.info("info1 = "+ cienaAlarm.getCustomFieldValue(GFPFields.INFO1));
			}
			// below are pport alarms:
			// 50002/100/23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35,
			// 36, 37, 38, 39,40
			if ("PPORT".equals(moClass) && eventKey.contains("50002"))
				topo.preEnrichPPortAlarm(cienaAlarm);

			if (eventKey.equals("50002/100/55"))
			{
				// System.out.println("Processing unreachable alarm.");

				String info= ""; 
				log.info("Checking if alarm should be processed as MIS-NOD-CFO." + cienaAlarm.getIdentifier() + " SeqNum = " + cienaAlarm.getCustomFieldValue(GFPFields.SEQNUMBER));
				log.info("Device Type = " + device_type + " Device SubRole = " + deviceSubRole + ". " + cienaAlarm.getIdentifier() + " SeqNum = " + cienaAlarm.getCustomFieldValue(GFPFields.SEQNUMBER));
				if ( ("EMT".equals(deviceSubRole) && "CIENA EMUX".equals(device_type)) || "CIENA NTE".equals(device_type) ) {
					info = nmgr.getMatchingPportsEvcs(cienaAlarm, "SDN-ETHERNET-INTERNET");
					if ( info.isEmpty() ) {
						log.info("No Matching Pport/EVC; No need to process alarm as MIS-NOD-CFO." + cienaAlarm.getIdentifier() + " SeqNum = " + cienaAlarm.getCustomFieldValue(GFPFields.SEQNUMBER));
					}
					else {
						log.info("Creating duplicate alarm as MIS-NOD-CFO." + cienaAlarm.getIdentifier() + " SeqNum = " + cienaAlarm.getCustomFieldValue(GFPFields.SEQNUMBER));
						EnrichedAlarm cienaUnreachMISNODCFO = new EnrichedAlarm(alarmx);
						cienaUnreachMISNODCFO.setCustomFieldValue(GFPFields.ALERT_ID, cienaUnreachMISNODCFO.getCustomFieldValue(GFPFields.ALERT_ID) + "-MIS-NOD-CFO");
						cienaUnreachMISNODCFO.setIdentifier(cienaUnreachMISNODCFO.getIdentifier() + "-duplicate");
						cienaUnreachMISNODCFO.setCustomFieldValue(GFPFields.CLASSIFICATION, "MIS-NOD-CFO");  
						cienaUnreachMISNODCFO.setCustomFieldValue(GFPFields.DOMAIN, "IPAG");
						cienaUnreachMISNODCFO.setCustomFieldValue(GFPFields.INFO, info);
						if ( "EMT".equals(deviceSubRole) && "CIENA EMUX".equals(device_type) ) {
							cienaUnreachMISNODCFO.setCustomFieldValue("flags", "DeviceSubRole=<EMT>");
						}
						GFPUtil.forwardOrCascadeAlarm(cienaUnreachMISNODCFO, AlarmDelegationType.FORWARD, "Alarm Manager");
					}
					cienaAlarm.setCustomFieldValue("dupInfo", info);
				}

				if (log.isInfoEnabled())
					log.info("Processing unreachable alarm.");
				topo.enrichUnreachableAlarm(cienaAlarm);
				
				// 2 lines below are only needed if we don't insert ciena unreach. to WM and we will have to send it out from here
				/*Util.sendAlarm(cienaAlarm, false);
				cienaAlarm = null;*/
			}
			else if (eventKey.equals("50003/100/1"))
			{
				log.info("Processing Juniper Link Down.");
				topo.enrichVRFStatusJuniperLD(cienaAlarm);

			}
			else if (eventKey.equals("50002/100/14"))
			{
				log.info("Processing dyinggasp alarm.");
				// below updates DB for both active/clear alarms.  
				// as VRF status needs to be set for both active and
				// clear alarms
				// Alarm is not inserted to WM yet
				//

				topo.enrichDeviceIP(cienaAlarm);
				topo.enrichVRFStatusDyingGaspAlarm(cienaAlarm);
				// topo.enrichCFM_CDCInfoDyingGaspAlarm(cienaAlarm);
			}
			else if (eventKey.equals("50004/1/3") || eventKey.equals("50004/1/4"))
			{
				// 50004/1/4 never reaches here as it is a DEVICE level
				// infovista alarm. its enrichment is only
				// info1: DeviceSubRole=<EMT>
				// which is done by preprocess
				//
				// 50004/1/3 is PORT level infovista alarm and is
				// enriched here
				//
				if ("PPORT".equals(moClass))
				{
					if (log.isInfoEnabled())
						log.info("Processing Infovista alarm.");
					topo.enrichInfovista(cienaAlarm, true);
				}
			}
			else if (eventKey.equals("50002/100/21"))
			{
				// System.out.println("Processing link down alarm.");
				if (log.isInfoEnabled())
					log.info("Processing link down alarm.");
				// don't send decompostion here.  it will be sent by
				// JuniperPdvp.
				// Util.sendAlarm(cienaAlarm, AlarmDelegationType.FORWARD, null, true);

				// below updates DB.  Alarm is not inserted to WM yet
				// it applies to both active/clear alarms
				topo.enrichVRFStatusLinkDownAlarm(cienaAlarm);
				// below does
				// (1) enrich alarm 
				// then does the following in WM
				// (1) update severity
				// (2) suppression
				topo.enrichLinkDownAlarm(cienaAlarm);
				// topo.preEnrichPPortAlarm(cienaAlarm);
				// topo.enrichCFM_CDCInfoLinkDownAlarm(cienaAlarm);
			}
			else if (eventKey.equals("50002/100/19"))
			{
				// only enrich device ip
				// System.out.println("Processing OAM alarm.");
				if (log.isInfoEnabled())
					log.info("Processing OAM alarm.");
				// the decomposed is sent based on aaf_da correlation
				// it will be sent by PriSecPdvp when needed.
				// Util.sendAlarm(cienaAlarm, AlarmDelegationType.FORWARD, null, true);
				topo.enrichOAMAlarm(cienaAlarm);
			}
			else if (eventKey.equals("50002/100/9") ||
					eventKey.equals("50002/100/11"))
			{
				// System.out.println("Processing alarm correlated to OAM/link down.");
				if (log.isInfoEnabled())
					log.info("Processing alarm correlated to OAM/link down.");
				// only enrich device ip
				topo.enrichDeviceIP(cienaAlarm);
			}
			else if (eventKey.equals("50002/100/52"))
			{
				if (log.isInfoEnabled())
					log.info("Processing CFM alarm.");


				// it return false when the CFM alarm is 
				// suppressed
				if(topo.enrichCFMAlarm(cienaAlarm) == false)
					cienaAlarm = null; // suppress CFM
			}
			else if (eventKey.equals("50002/100/58"))
			{
				if (log.isInfoEnabled())
					log.info("Processing ColdStart alarm.");
				// just send it to Syslog
				// topo.enrichColdStart(cienaAlarm);
			}
			// health trap
			// else if (eventKey.equals("50002/100/58916873") ||
			//	eventKey.equals("50004/2/58916876"))
			// 50004/2/2: ciena health trap
			// 50004/2/58916876: infovista health trap
			else if (eventKey.equals("50004/2/2") || 
					eventKey.equals("50004/2/58916876"))

			{
				if (log.isInfoEnabled())
					log.info("Health Trap. Send it to NOM.");
				Util.sendAlarm(cienaAlarm, false);
				cienaAlarm = null;
			}
			else
			{
				if (log.isInfoEnabled())
					log.info("It is a pass through alarm.");
			}

			if (cienaAlarm == null)
			{
				if (log.isInfoEnabled())
					log.info("This alarm is not put to WM.");
			}
			else
			{
				//			if (isChronic == true)
				//			{
				//				// send the chronic alarm to NOM.
				//				log.info("Send the chronic alarm to NOM.  It is not put to WM: " + cienaAlarm.getIdentifier());
				//				Util.sendAlarm(cienaAlarm, null);
				//				// it won't go to WM.  hence no clear, 
				//				// no correlation will take place
				//				return null;
				//			}
				if (log.isInfoEnabled())
					log.info("Put the alarm to WM.");
			}
			return cienaAlarm;
		} catch (Exception e) {
			if (log.isInfoEnabled())
				log.info("Dropped this alarm = " + alarm.getIdentifier() + ",  as enrichment failed. ", e);
			return null;
		}
	}

	@Override
	public boolean onUpdateSpecificFieldsFromAlarm(Alarm newAlarm,
			AlarmCommon alarmInWorkingMemory) {

		String axml = newAlarm.toXMLString();
		axml = axml.replaceAll("\\n", " ");
		if (log.isInfoEnabled())
			log.info("Incoming updated alarm: "+axml);

		String eventKey = newAlarm.getCustomFieldValue(GFPFields.EVENT_KEY);
		String seqNum = newAlarm.getCustomFieldValue(GFPFields.SEQNUMBER);

		CienaAlarm cienaAlarm = null;

		try {
			cienaAlarm = new CienaAlarm(newAlarm);
		} catch (Exception e) {
			// this.getScenario().setStatusAndLogAndUpdateVPStatus(e.getMessage(), ScenarioStatus.Degraded);
			if (log.isInfoEnabled())
				log.info("failed to create CienaAlarm: "+ e);
			return true;
		}
		CienaAlarm ca = (CienaAlarm) alarmInWorkingMemory;

		boolean isFBS_PtpMpt = ca.getIsFBS_PtpMpt();
		log.info("isFBS_PtpMpt = " + isFBS_PtpMpt);
		int severity = cienaAlarm.getSeverity();
		if (severity == 4)
			log.info("Received a clear alarm.");
		// for now just handling the severity
		boolean ret = false;
		boolean isClear = false;
		Alarm alarmInWM = (Alarm) alarmInWorkingMemory;

		List<AttributeChange> attributeChangesSC = new ArrayList<AttributeChange>();
		List<AttributeChange> attributeChangesAVC = new ArrayList<AttributeChange>();
		AttributeChange attributeChange = null;

		if (alarmInWorkingMemory instanceof Alarm) {

			// Updating the Perceived Severity of the alarm in Working memory
			// only if the Alarm received is different.
			if (newAlarm.getPerceivedSeverity() != alarmInWM
					.getPerceivedSeverity()) {

				if (newAlarm.getPerceivedSeverity() == PerceivedSeverity.CLEAR
						&& alarmInWM.getNetworkState() == NetworkState.NOT_CLEARED) {

					if (eventKey.equals("50002/100/55") && 
							!"YES".equalsIgnoreCase(newAlarm.getCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA)) && 
							!"YES".equalsIgnoreCase(newAlarm.getCustomFieldValue(GFPFields.IS_PURGE_INTERVAL_EXPIRED)) )
					{
						clearMISNODCFODupAlarm(ca, seqNum);
					}
					isClear = true;
					attributeChange = new AttributeChange();
					attributeChange.setName(StandardFields.NETWORK_STATE);
					attributeChange.setNewValue(NetworkState.CLEARED.toString());
					attributeChange.setOldValue(alarmInWM.getNetworkState().toString());
					attributeChangesSC.add(attributeChange);

					attributeChange = new AttributeChange();
					attributeChange.setName(StandardFields.PERCEIVED_SEVERITY);
					attributeChange.setNewValue(PerceivedSeverity.CLEAR.toString());
					attributeChange.setOldValue(alarmInWM.getPerceivedSeverity().toString());
					attributeChangesAVC.add(attributeChange);

					// change clearence time of the alarm.
					attributeChange = new AttributeChange();
					attributeChange.setName(GFPFields.LAST_CLEAR_TIME);
					attributeChange.setNewValue(String.valueOf(System.currentTimeMillis()/1000));
					attributeChange.setOldValue(alarmInWM.getCustomFieldValue(GFPFields.LAST_CLEAR_TIME));
					attributeChangesAVC.add(attributeChange);
				} else {

					attributeChange = new AttributeChange();
					attributeChange.setName(StandardFields.PERCEIVED_SEVERITY);
					attributeChange.setNewValue(newAlarm.getPerceivedSeverity().toString());
					attributeChange.setOldValue(alarmInWM.getPerceivedSeverity().toString());
					attributeChangesAVC.add(attributeChange);
				}
			}

			if (!attributeChangesSC.isEmpty()) {
				ret = true;
			}

			if (!attributeChangesAVC.isEmpty()) {
				ret = true;
			}

		}

		if (isClear == true)
		{
			CienaAlarm a = (CienaAlarm) alarmInWorkingMemory;
			eventKey = a.getCustomFieldValue(GFPFields.EVENT_KEY);
			a.setSeverity(4);
			a.setCustomFieldValue(GFPFields.LAST_CLEAR_TIME, ""+System.currentTimeMillis()/1000);
			a.setCustomFieldValue(GFPFields.SEQNUMBER, newAlarm.getCustomFieldValue(GFPFields.SEQNUMBER));
			if("YES".equalsIgnoreCase(newAlarm.getCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA)))
				a.setCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA, "YES"); 
			if("YES".equalsIgnoreCase(newAlarm.getCustomFieldValue(GFPFields.IS_PURGE_INTERVAL_EXPIRED)))
				a.setCustomFieldValue(GFPFields.IS_PURGE_INTERVAL_EXPIRED, "YES"); 
			CienaAlarm ax = a;
			if (!(a instanceof EnrichedAlarm))
				// i assume a and ax are the same reference
				ax = (CienaAlarm)GFPUtil.populateEnrichedAlarmObj(a);
			if (ret == true)
			{
				log.info("Send the clear alarm.");
				if (ax.getIsSent2DEC())
					Util.sendAlarm(ax, true);
				if (ax.getIsSent())
					Util.sendAlarm(ax, false);
			}

			a.setIsClear(true);
			if (!attributeChangesSC.isEmpty()) {
				AlarmUpdater.updateAlarmFromAttributesChanges(alarmInWM, UsualVar.StateChange, attributeChangesSC, System.currentTimeMillis());
				alarmInWM.setHasStateChanged(true);
				ret = true;
			}

			if (!attributeChangesAVC.isEmpty()) {
				AlarmUpdater.updateAlarmFromAttributesChanges(alarmInWM, UsualVar.AVCChange, attributeChangesAVC, System.currentTimeMillis());
				alarmInWM.setHasAVCChanged(true);
				ret = true;
			}
			/*
below makes WM_CurrentNumberOfFacts stay high (as the # of group in WM are never
retracted) and makes MW_InsertUpdateRetractRate low
			if(ret)
			{
				if (log.isInfoEnabled())
				log.info("retract alarm in WM: " + alarmInWorkingMemory.getIdentifier());
                 		getScenario().getSession().retract(alarmInWorkingMemory);
			}
			 */
		}

		return ret;
	}

	public void clearMISNODCFODupAlarm (CienaAlarm ca, String seqNum) {

		try {
			CienaAlarm cienaUnreachMISNODCFO = new CienaAlarm(ca);

			String info= "";
			String deviceSubRole = cienaUnreachMISNODCFO.getDeviceSubRole();
			String device_type = cienaUnreachMISNODCFO.getDeviceType();

			log.trace("Checking if a clear MIS-NOD-CFO should be processed " + cienaUnreachMISNODCFO.getIdentifier() + " SeqNum = " + seqNum);
			log.trace("Device Type = " + device_type + " Device SubRole = " + deviceSubRole + ". " + cienaUnreachMISNODCFO.getIdentifier() + " SeqNum = " + seqNum);

			if ( ("EMT".equals(deviceSubRole) && "CIENA EMUX".equals(device_type)) || "CIENA NTE".equals(device_type) ) {
				info = cienaUnreachMISNODCFO.getCustomFieldValue("dupInfo");
				if ( null == info || info.isEmpty() ) {
					log.trace("dupInfo custom field is null/empty; No need to process clear alarm as MIS-NOD-CFO." + cienaUnreachMISNODCFO.getIdentifier() + " SeqNum = " + seqNum);
				}
				else {
					log.info("Creating duplicate clear alarm as MIS-NOD-CFO." + cienaUnreachMISNODCFO.getIdentifier() + " SeqNum = " + seqNum);
					cienaUnreachMISNODCFO.setCustomFieldValue(GFPFields.ALERT_ID, cienaUnreachMISNODCFO.getCustomFieldValue(GFPFields.ALERT_ID) + "-MIS-NOD-CFO");
					cienaUnreachMISNODCFO.setIdentifier(cienaUnreachMISNODCFO.getIdentifier() + "-duplicate");
					cienaUnreachMISNODCFO.setCustomFieldValue(GFPFields.CLASSIFICATION, "MIS-NOD-CFO");  
					cienaUnreachMISNODCFO.setCustomFieldValue(GFPFields.DOMAIN, "IPAG");
					cienaUnreachMISNODCFO.setCustomFieldValue(GFPFields.INFO, info);
					cienaUnreachMISNODCFO.setCustomFieldValue(GFPFields.SEQNUMBER, seqNum);
					cienaUnreachMISNODCFO.setPerceivedSeverity(PerceivedSeverity.CLEAR);
					if ( "EMT".equals(deviceSubRole) && "CIENA EMUX".equals(device_type) ) {
						cienaUnreachMISNODCFO.setCustomFieldValue("flags", "DeviceSubRole=<EMT>");
					}
					GFPUtil.forwardOrCascadeAlarm(cienaUnreachMISNODCFO, AlarmDelegationType.FORWARD, "Alarm Manager");
				}
			}

		}
		catch (Exception e) {
			log.error("Exception processing clear for MIS-NOD-CFO alarm" + ca.getIdentifier() + " SeqNum = " + seqNum
			           + " Exeption = " + e.toString() + " Trace = "  + e.getStackTrace());
		}
	}
}
