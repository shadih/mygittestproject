package com.hp.uca.expert.vp.pd.core;

import java.lang.reflect.Method;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.data.ipagAlarm.EnrichedJuniperAlarm;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
import com.hp.uca.common.callback.Callback;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.scenario.ScenarioThreadLocal;
import com.hp.uca.expert.vp.pd.problem.Other_LinkDown_AlarmSuppression;
import com.hp.uca.expert.vp.pd.problem.Util;
import com.hp.uca.expert.vp.pd.services.PD_Service_Group;
import com.hp.uca.expert.x733alarm.AttributeChange;

public class JuniperLinkDown_ProblemDefault extends ProblemDefault {

	protected static final String LINKDOWN_KEY = "50003/100/1";
	protected static final String DA = "DA";
	protected static final String AAF_SECONDARY = "AAF-SECONDARY";
	protected static final String AAF_PRIMARY = "AAF-PRIMARY";


	private Logger log = LoggerFactory.getLogger(Other_LinkDown_AlarmSuppression.class);

	@Override
	public boolean isMatchingCandidateAlarmCriteria(Alarm a) throws Exception {
		EnrichedJuniperAlarm ea = (EnrichedJuniperAlarm) a;
		String eventKey = ea.getCustomFieldValue(GFPFields.EVENT_KEY);
		String targetName = null;

		Util.WDPool(ea, 0, getScenario());
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingCandidateAlarmCriteria() runs.");
		return true;
	}

	@Override
	public boolean isMatchingSubAlarmCriteria(Alarm a,Group group) throws Exception {
		boolean ret = true;
		EnrichedJuniperAlarm ea = (EnrichedJuniperAlarm) a;
		String eventKey = ea.getCustomFieldValue(GFPFields.EVENT_KEY);
		String targetName = null;

		Util.WDPool(ea, 0, getScenario());
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingSubAlarmCriteria() " + "[" + ret + "], group name = " + group.getName());
		return ret;
	}

	@Override
	public boolean isAllCriteriaForProblemAlarmCreation(Group group)
			throws Exception {
		boolean ret = false;
		
		if(group.getNumber() > 1)
			ret = true;
		
		return ret;
	}
	

	@Override
	public boolean isMatchingTriggerAlarmCriteria(Alarm a) throws Exception {
		boolean ret = false;

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "isMatchingTriggerAlarmCriteria()");
		}

		if ( a instanceof EnrichedJuniperAlarm)  {
			EnrichedJuniperAlarm alarm = (EnrichedJuniperAlarm) a;	

				setTriggerWatch(alarm);
				ret = true;

		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "isMatchingTriggerAlarmCriteria() " + "[" + ret + "]");
		}

		return ret;
	}
	
	// This is my default behavior which is to suppress the trigger if the group
	// has more than one alarm in it.
	@Override
	public void whatToDoWhenSubAlarmIsAttachedToGroup(Alarm alarm, Group group)
			throws Exception {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "whatToDoWhenSubAlarmIsAttachedToGroup()");
		}
		
		EnrichedJuniperAlarm trigger = (EnrichedJuniperAlarm) group.getTrigger();

		if(alarm == trigger){
			if (log.isTraceEnabled())
				log.trace("This alarm is the trigger for the group: " + alarm.getIdentifier());
			if(group.getNumber() > 1) {
				if (log.isTraceEnabled())
					log.trace("we have more than one alarm so we may suppress this...");
				trigger.setSuppressed(true);
			}
		} else {
			// the trigger should have came in first so when this alarm arrives
			// we have more than one alarm in the group so the trigger is suppressed
			trigger.setSuppressed(true);
			log.info("Alarm " + trigger.getIdentifier() + " sequence:" + trigger.getCustomFieldValue(GFPFields.SEQNUMBER) +
					" is suppressed by " + getProblemPolicy().getName() + " !");
		}		
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenSubAlarmIsAttachedToGroup()");
		}
	}
	
	// This method is used to send alarms that have been attached to a group
/*		@Override
		public void whatToDoWhenSubAlarmIsAttachedToGroup(Alarm alarm, Group group)
				throws Exception {

			if (log.isTraceEnabled()) {
				LogHelper.enter(log, "whatToDoWhenSubAlarmIsAttachedToGroup()");
			}

			boolean isTriggerForOtherGroup = false;
			EnrichedJuniperAlarm trigger = (EnrichedJuniperAlarm) group.getTrigger();

			// is this subalarm a trigger for any group?   If it is then we don't want
			// to send the alarm here.   We will wait till all the triggers have been
			// evaluated then send it (watch callback).
			for (Group possibleGroup : PD_Service_Group.getGroupsOfAnAlarm(alarm, null)) {
				if(possibleGroup.getTrigger() == alarm) {
					isTriggerForOtherGroup = true;
					log.trace("This is a trigger for another group: " + alarm.getIdentifier());
					break;
				}
			}

			// check to make sure this alarm has not been sent out yet at all
			if(alarm != trigger && !((EnrichedJuniperAlarm) alarm).getSentAsSubAlarm() &&
					!isTriggerForOtherGroup ) {
				Util.sendAlarm(alarm, AlarmDelegationType.FORWARD, null, false);
			}	

			// this group has more than one alarm and this alarm is the trigger so suppress
			if(alarm == trigger){
				log.trace("This alarm is the trigger for the group: " + alarm.getIdentifier());
				if(group.getNumber() > 1) {
					log.trace("we have more than one alarm so suppress");
					trigger.setSuppressed(true);
					//trigger.setCustomFieldValue(GFPFields.SEND_TO_RUBY, "false");
				}				
			} else {
				// this is not the trigger but the trigger should be suppressed
				trigger.setSuppressed(true);
				//trigger.setCustomFieldValue(GFPFields.SEND_TO_RUBY, "false");			
			}
			
			if (log.isTraceEnabled()) {
				LogHelper.exit(log, "whatToDoWhenSubAlarmIsAttachedToGroup()");
			}
		}*/
		
	// this method is used to send alarms that have not been attached to a group
	@Override
	public void whatToDoWhenOrphanAlarmAttributeHasChanged(Alarm alarm,
			AttributeChange attributeChange) throws Exception {
		
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "whatToDoWhenOrphanAlarmAttributeHasChanged()");
		}
		
		// if we went from Candidate to alarm then the problem time window has expired
		// and we can disregard this alarm and forward it
		if ( attributeChange.getName().equalsIgnoreCase("pd") && 
				attributeChange.getOldValue().equalsIgnoreCase("Candidate") &&
				!attributeChange.getNewValue().equalsIgnoreCase("SubAlarm")) {
			// TODO: test for SubAlarm may be redundant
		}
		
		// sending all candidate alarms that were not attached to a group
		Util.whereToSendThenSend((EnrichedAlarm)alarm);
		
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenOrphanAlarmAttributeHasChanged()");
		}

	}
	
	public void setTriggerWatch(EnrichedJuniperAlarm alarm) {

		//if(!alarm.getTriggerWatchSet()) {

			// watch to send the trigger alarm if no subalarms arrive
			Class<?> partypes[] = new Class[0];
			Object arglist[] = new Object[0];
			Method method = null;
			
			try {
				method = EnrichedJuniperAlarm.class.getMethod("expirationCallBack",
						partypes);
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			}
			
			Callback callback = new Callback(method, alarm, arglist);

			// get the after trigger time and set this watchdog with that value
			// if no subalarms are added before this, we know that the group will 
			// not form and we can cascade this alarm with no change.
			long after = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger();

			// to make sure...
			if(after < 2000 )
				after = after + 2000;
			
			alarm.setNumberOfWatches(alarm.getNumberOfWatches() + 1);
			String watchdogDesc = "Expiration WatchdogItem:" + String.valueOf(alarm.getNumberOfWatches());
			Scenario scenario = ScenarioThreadLocal.getScenario();
			scenario.addCallbackWatchdogItem(after, callback, false, watchdogDesc, true, alarm);

			if (log.isTraceEnabled()) {
				LogHelper.exit(log, "isMatchingTriggerAlarmCriteria() " + "Watchdog has been set to:" + after);
			}
			alarm.setTriggerWatchSet(true);
			
		//}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "setTriggerWatch()");
		}
	}
}	
	
	

