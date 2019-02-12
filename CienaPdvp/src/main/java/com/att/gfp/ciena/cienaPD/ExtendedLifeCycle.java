package com.att.gfp.ciena.cienaPD;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.att.gfp.ciena.cienaPD.bean.CienaBean;
import com.att.gfp.ciena.cienaPD.topoModel.IpagCienaTopoAccess;
import com.att.gfp.ciena.cienaPD.topoModel.NodeManager;
import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.StandardFields;
import com.att.gfp.helper.GFPUtil;
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

public class ExtendedLifeCycle extends LifeCycleAnalysis {
	private IpagCienaTopoAccess topo;
	private static Logger log = LoggerFactory.getLogger(ExtendedLifeCycle.class);
	private static ApplicationContext context = null;
	public ExtendedLifeCycle(Scenario scenario) {
		super(scenario);
		if (context == null)
		{
			log.info("Get Application Context.");
			// its location is defined in classpath
			// ApplicationContext context = new ClassPathXmlApplicationContext("context.xml");
			context = scenario.getVPApplicationContext();
			// context = new ClassPathXmlApplicationContext("context.xml");
			CienaBean cb = (CienaBean) context.getBean("cienaBean");
			boolean Junit = (cb.getJunit().equals("true"))? true: false;
			log.info("Junit = " + Junit);
			// Util.setJunit(Junit);
			Util.init(Junit);
			log.info("Get Application Context is completed.");
		}

		/*
		 * If needed more configuration, use the context.xml to define
		 * any beans that will be available here using 
		 * scenario.getGlobals()
		 */
		scenario.getGlobals();
		// topo = new IpagCienaTopoAccess();
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

		CienaAlarm cienaAlarm = null;

		try {
			cienaAlarm = new CienaAlarm(alarm);
		} catch (Exception e) {
			e.printStackTrace();
			log.info("Failed to create CienaAlarm.  Drop it."+ e);
			return null;
		}
		String eventKey = cienaAlarm.getCustomFieldValue("EventKey");
		log.info("EventKey = " + eventKey);
		int severity = cienaAlarm.getSeverity();
		if (severity == 4)
		{
			log.info("There is no corresponding active alarm.  Drop the clear alarm.");
			return null;
		}

		// health trap.  CienaPdvp doesn't receive health trap
/*
		if (eventKey.equals("50002/100/58916873") ||
			eventKey.equals("50004/2/58916876"))
		{
			log.info("Health Trap. Send it to AM.");
			Util.sendAlarm(cienaAlarm, AlarmDelegationType.FORWARD, null, false);
			return null;
		}
*/
		if (eventKey.equals("50002/100/52") ||
			eventKey.equals("50001/100/61") ||
			eventKey.equals("50001/100/62") ||
			eventKey.equals("50001/100/63") ||
			eventKey.equals("50001/100/64") ||
			eventKey.equals("50001/100/65"))
		{
			String vrfName = cienaAlarm.getCustomFieldValue("vrf-name");
			if (vrfName.equals("VPLS:INFRA_NTE_IPAG1") ||
			    vrfName.equals("VPLS:INFRA_NTE_IPACDM"))
			{
				log.info("Drop the CFM alarms as vfrName = " + vrfName);
				return null;
			}
			NodeManager.setDevicePportEvcNode(cienaAlarm);

			// DF - pull the mepid from the reason field
                     	if(cienaAlarm.getCustomFieldValue(GFPFields.REASON_CODE) !=null && cienaAlarm.getCustomFieldValue(GFPFields.REASON_CODE).contains("MID="))
			{
                           String mePid = cienaAlarm.getCustomFieldValue(GFPFields.REASON_CODE).split("MID=")[1];
                           cienaAlarm.setCustomFieldValue("mepid", mePid);
                           log.info("Pulled the mepid from the reason code field: " + mePid);
                     	}

			// this Pdvp should send the CFM alarm if the device
			// is NOT NTE/EMT.  it is done in setHistoryNavigation()
			// String evcnodeInstance = cienaAlarm.getOriginatingManagedEntity().split(" ")[1];
			// String deviceInstance = evcnodeInstance.split("/")[0];
			cienaAlarm.setCFMPtpMptAlarm(NodeManager.isDevice_NTE_EMT(cienaAlarm));
		}

		if (cienaAlarm == null)
			log.info("This alarm is not put to WM.");
		else
			log.info("return to rule engine for ptp/mpt processing.");
		return cienaAlarm;
	   } catch (Exception e) {
		log.info("Dropped this alarm as enrichment failed. "+ e);
		return null;
	   }
	}

	@Override
	public boolean onUpdateSpecificFieldsFromAlarm(Alarm newAlarm,
			AlarmCommon alarmInWorkingMemory) {

		String axml = newAlarm.toXMLString();
		axml = axml.replaceAll("\\n", " ");
		log.info("Incoming updated alarm: "+axml);

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
			String eventKey = a.getCustomFieldValue(GFPFields.EVENT_KEY);
			a.setSeverity(4);
			a.setCustomFieldValue(GFPFields.LAST_CLEAR_TIME, ""+System.currentTimeMillis()/1000);
                     	// DF - we also have to set the sequence number to 
			// that of the clear alarm
                     	a.setCustomFieldValue(GFPFields.SEQNUMBER, newAlarm.getCustomFieldValue(GFPFields.SEQNUMBER));
			boolean isGenByUCA = false;
			boolean isPurgeItvlExp = false;
			if("YES".equalsIgnoreCase(newAlarm.getCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA)))
			{
				a.setCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA, "YES"); 
				isGenByUCA = true;
			}
			if("YES".equalsIgnoreCase(newAlarm.getCustomFieldValue(GFPFields.IS_PURGE_INTERVAL_EXPIRED)))
			{
				a.setCustomFieldValue(GFPFields.IS_PURGE_INTERVAL_EXPIRED, "YES"); 
				isPurgeItvlExp = true;
			}
                     
                     	// DF - setting this here will prevent the clear from 
			// being sent because of the canBeSent() check in 
			// Util.sendAlarm()
                     	// a.setIsClear(true);
			log.info("attempt to clear the synthetic alarm.");
			a.clearSyntheticAlarm(a.getCustomFieldValue("vrf-name"), newAlarm.getCustomFieldValue(GFPFields.SEQNUMBER), isGenByUCA, isPurgeItvlExp);

			// a.getIsSent() is always false here
			if (ret == true && a.getIsSent()) 
			{
			       	// DF - again here, because the alarm was 
				// already sent (as tested above)
                           	// the clear alarm will not be sent because 
				// of the canBeSent() check in Util.sendAlarm()
                           	// so I will set IsSent temporarily to false 
				// to fool the sendAlarm method
                           	a.setIsSent(false);
/*
				if (eventKey.contains("50002"))
					// send ciena clear to Juniper VP
					Util.sendAlarm(a, AlarmDelegationType.CASCADE, "JUNIPER_LINKDOWN", false);
				else
				// send adtran clear to AM
*/
				CienaAlarm ax = a;
				if (!(a instanceof EnrichedAlarm))
					ax = (CienaAlarm)GFPUtil.populateEnrichedAlarmObj(a);
				Util.sendAlarm(ax, AlarmDelegationType.FORWARD, null, false);
				// DF - and now I will put it back in case 
				// this value is used in later processing
                           	ax.setIsSent(true);
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
				log.info("retract alarm in WM: " + alarmInWorkingMemory.getIdentifier());
	                 	getScenario().getSession().retract(alarmInWorkingMemory);
			}
*/
		}
		return ret;
	}
}
