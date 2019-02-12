/**
 * This Problem is empty and ready to define methods to customize this problem
 */
package com.hp.uca.expert.vp.pd.problem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.EnrichedJuniperAlarm;
import com.att.gfp.helper.GFPFields;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.vp.pd.core.JuniperLinkDown_ProblemDefault;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;


// ******************************************************
//
//
// THIS HAS BEEN MOVED TO THE IpagPriSecPdvp ValuePack
//
//
// *******************************************************





/**
 * @author df
 * 
 */
public final class OAM_LAG_Suppression extends JuniperLinkDown_ProblemDefault implements
		ProblemInterface {

	
	private Logger log = LoggerFactory.getLogger(OAM_LAG_Suppression.class);

	public OAM_LAG_Suppression() {
		super();

	}
	//
	// subalarm arrives, wait 3 min(from xml file)
	// if (trigger arrives in 3 min)
	//	enriched subalarm
	// else
	//	send subalarm;

	//@Override
/*	public boolean isMatchingCandidateAlarmCriteria(Alarm a) throws Exception {

		long after = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger();
		EnrichedJuniperAlarm ea = (EnrichedJuniperAlarm) a;
		String targetName = null;
		Util.WDPool(ea, targetName, false, after);
		LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingCandidateAlarmCriteria() runs.");
		
		return true;
	}*/

	// tj: athough subalarm of problem "OAM_LAG_Suppression" is also
	// enriched, it can still be sent by WD pool as its enrichment 
	// should be completed before WD pool timeout(2 seconds)
/*	@Override
	public boolean isMatchingSubAlarmCriteria(Alarm a, Group group) throws Exception {
		boolean ret = true;
		EnrichedJuniperAlarm ea = (EnrichedJuniperAlarm) a;
		String targetName = null;
		Util.WDPool(ea, targetName, false, 0);
		LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingSubAlarmCriteria() " + "[" + ret + "], group name = " + group.getName());
		return ret;
	}*/
		 
	@Override
	public List<String> computeProblemEntity(Alarm a) throws Exception {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "computeProblemEntity()");
		}

		String meInstance = a.getOriginatingManagedEntity().split(" ")[1];
		String meClass = a.getOriginatingManagedEntity().split(" ")[0];

		List<String> problemEntities = new ArrayList<String>();
		EnrichedJuniperAlarm alarm = (EnrichedJuniperAlarm) a;

		// the problem entity is the containing pport if an LPORT alarm 
		if(alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(LINKDOWN_KEY)) {
			if(meClass.equals("LPORT")) {
				if(alarm.getContainingPPort()!=null){
					problemEntities.add(alarm.getContainingPPort());
					log.trace("### Containing port="+ alarm.getContainingPPort());
				}
			} else {
				// its the lagid if pport
				if(meClass.equals("PPORT")) {
					if(alarm.getPortLagId() != null && !alarm.getPortLagId().isEmpty()) 
						problemEntities.add(alarm.getPortLagId());
					/*if(alarm.getlagIdFromAlarm() != null)
						problemEntities.add(alarm.getlagIdFromAlarm());*/
					if(alarm.getDeviceInstance() != null && !alarm.getDeviceInstance().isEmpty())
						problemEntities.add(alarm.getDeviceInstance());					
				} else {
					// if a device then we need the lag
					if(meClass.equals("DEVICE")) {
						if(alarm.getlagIdFromAlarm() != null)
							problemEntities.add(alarm.getlagIdFromAlarm());
					}
				}
			}
		} else {
			// if the component contains the lagid
			if(alarm.getlagIdFromAlarm() != null)
				problemEntities.add(alarm.getlagIdFromAlarm());
		}

		// in any case we put the ME instance 
		problemEntities.add(meInstance);

		LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", computeProblemEntity() --- problemEntities=" + problemEntities);

		return problemEntities;
	} 

	@Override
	public void whatToDoWhenSubAlarmIsAttachedToGroup(Alarm alarm, Group group) throws Exception {
		// we don't want the default behavior, just want to do nothing

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "whatToDoWhenSubAlarmIsAttachedToGroup()");
		}
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenSubAlarmIsAttachedToGroup()");
		}
	
	}
	
	@Override
	public boolean isAllCriteriaForProblemAlarmCreation(Group group)
			throws Exception {
		

		if (log.isTraceEnabled()) {
			log.trace(
					"ENTER:  isAllCriteriaForProblemAlarmCreation() for group ["
							+ group.getTrigger().getIdentifier(), group.getName());
		}

		boolean ret = false;
		/*
		 * Checking that there are at least two alarms in the group before to
		 * create the Problem Alarm
		 */
		Collection<Alarm> alarmsInGroup = group.getAlarmList();
		int numberOfAlarmsInGroup = alarmsInGroup.size();

		if (log.isDebugEnabled()) {
			log.debug("numberOfAlarmsInGroup = " + numberOfAlarmsInGroup);
		}

		if (numberOfAlarmsInGroup > 1 ) {
			ret = true;
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "isAllCriteriaForProblemAlarmCreation()",
					String.valueOf(ret) + " -- " + group.getName());
		}
		return ret;
	}
	
/*	@Override
	public void whatToDoWhenSubAlarmIsAttachedToGroup(Alarm alarm, Group group) throws Exception {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "whatToDoWhenSubAlarmIsAttachedToGroup()");
		}
		
		EnrichedAlarm tAlarm = (EnrichedAlarm) group.getTrigger();
		
		// ## DF - here is where I want to assign the primary/secondary stuff
		if(alarm != group.getTrigger()) {
			
			// mark and send the trigger
			log.info("Setting this PPORT LinkDown alarm Primary to LAG LinkDown (alert-id = " + tAlarm.getIdentifier() + 
					" & sequence-number = " + tAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + ")");

			tAlarm.setCustomFieldValue(GFPFields.SECONDARY_ALERT_ID, "1");
			tAlarm.setCustomFieldValue(GFPFields.SECONDARYTIMESTAMP, "0");
			tAlarm.setCustomFieldValue("sent_to_ruby", "true");
			Util.sendAlarm((EnrichedAlarm) tAlarm, AlarmDelegationType.CASCADE, "JUNIPER_VRFPROCESSING", false);
			log.trace("The primary alarm was sent: " + tAlarm.toString());

			// mark and send the subalarm
			log.info("Setting this LAG LinkDown alarm Secondary to PPORT LinkDown (alert-id = " + alarm.getIdentifier() + 
					" & sequence-number = " + alarm.getCustomFieldValue(GFPFields.SEQNUMBER) + ")");
			
			alarm.setCustomFieldValue(GFPFields.SECONDARY_ALERT_ID, group.getTrigger().getCustomFieldValue(GFPFields.ALERT_ID));
			alarm.setCustomFieldValue(GFPFields.SECONDARYTIMESTAMP, group.getTrigger().getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP));
			alarm.setCustomFieldValue("sent_to_ruby", "true");
			Util.sendAlarm((EnrichedAlarm) alarm, AlarmDelegationType.CASCADE, "JUNIPER_VRFPROCESSING", false);
			log.trace("The secondary alarm was sent: " + alarm.toString());
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenSubAlarmIsAttachedToGroup()");
		}

	}*/
}
