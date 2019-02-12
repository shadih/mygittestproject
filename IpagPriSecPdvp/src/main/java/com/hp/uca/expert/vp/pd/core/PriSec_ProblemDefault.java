package com.hp.uca.expert.vp.pd.core;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.Int;
import com.att.gfp.helper.GFPFields;

import com.att.gfp.data.ipagAlarm.Pri_Sec_Alarm;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.vp.pd.config.LongItem;
import com.hp.uca.expert.vp.pd.config.StringItem;
import com.hp.uca.expert.vp.pd.problem.Util;
import com.hp.uca.expert.vp.pd.services.PD_Service_ProblemAlarm;

public class PriSec_ProblemDefault extends ProblemDefault {

	private static final String TRIGGER_AGE_KEY = "MaxTAge";
	private static final String SUBALARM_AGE_KEY = "MaxSAge";
	private static final String MAX_DELAYED_EVENT = "MaxDelayedEvent";
	
	protected long maxTAge = 0;
	protected long maxSAge = 0;
	//protected long maxPAwait = 0;
	protected String maxDelayedEvent = null;

	//protected static final String LINKDOWN_KEY = "50003/100/1";
	//private static final int PRIORITY_FACTOR = 1000;
	private static Logger log = LoggerFactory.getLogger(PriSec_ProblemDefault.class);

	
	@Override
	public boolean isAllCriteriaForProblemAlarmCreation(Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			log.trace(
					"ENTER:  isAllCriteriaForProblemAlarmCreation() for group ["
							+ group.getTrigger().getIdentifier(), group.getName());
		}
		
		/*
		 * Checking that there are at least two alarms in the group before to
		 * create the Problem Alarm
		 */
/*		Collection<Alarm> alarmsInGroup = group.getAlarmList();
		int numberOfAlarmsInGroup = alarmsInGroup.size();

		if (log.isDebugEnabled()) {
			log.debug("numberOfAlarmsInGroup = " + numberOfAlarmsInGroup);
		}
		
		// is it time to stop the correlation and do the pri/sec enrichment
		// if the max delayed event has arrived the we don't have to wait any longer
		// Send out the subalarms
		if(maxDelayedEvent != null) {
			for (Alarm alarm : alarmsInGroup) {	
				if (log.isTraceEnabled())
					log.trace("Alarm in group: " + alarm.getCustomFieldValue(GFPFields.EVENT_KEY));

				// Check to see if the subAlarm is a candidate for another problem, if it is then don't send it.
				if(alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(maxDelayedEvent) && alarm != group.getTrigger())
				{
					isItTime = true;

					if (log.isTraceEnabled())
						log.trace("The max delayed event has arrived: " + maxDelayedEvent);
					break;
				}
			}
		}
		
		if(!isItTime) {
			Pri_Sec_Alarm alarm = (Pri_Sec_Alarm) group.getTrigger();
			
			long paWait = this.getProblemPolicy().getProblemAlarm().getDelayForProblemAlarmCreation();
			
			isItTime = alarm.isItTimeToCreateProblemAlarm(paWait);	
		}

		log.trace("Is it time now:" + isItTime);
		
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "isAllCriteriaForProblemAlarmCreation()",
					String.valueOf(isItTime) + " -- " + group.getName());
		}*/
		
		return false;
	}
	
	@Override
	public boolean isMatchingTriggerAlarmCriteria(Alarm a) throws Exception {
		boolean ret = false;

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "isMatchingTriggerAlarmCriteria()" + a.getIdentifier() + " Sequence # " + a.getCustomFieldValue(GFPFields.SEQNUMBER));
		}

		if ( a instanceof Pri_Sec_Alarm)  {
			Pri_Sec_Alarm alarm = (Pri_Sec_Alarm) a;	

			log.info("Alarm " + a.getIdentifier() + " Sequence # " + a.getCustomFieldValue(GFPFields.SEQNUMBER) + " will be held up to 3 minutes waiting for Primary Secondary Correlation.");

			// this is what we use to determine how long to hold the trigger before we let it go
			long afterTrigger = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger();
			long ageLeft = alarm.TimeRemaining(afterTrigger); 
			long holdTime = ageLeft + maxSAge; 
					
			String watchdogDesc = "Creating watchdog for:" + getProblemPolicy().getName();
		
			Util.setTriggerWatch((Pri_Sec_Alarm)alarm, Pri_Sec_Alarm.class, "simpleSendCallBack", holdTime, watchdogDesc);
				ret = true;

		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "isMatchingTriggerAlarmCriteria() " + "[" + ret + "]");
		}

		return ret;
	}	
	
	@Override
	public void whatToDoWhenSubAlarmIsAttachedToGroup(Alarm alarm, Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "whatToDoWhenSubAlarmIsAttachedToGroup()" + alarm.getIdentifier() + " Sequence # " + alarm.getCustomFieldValue(GFPFields.SEQNUMBER));
		}
		
		// we have to know when to send out an alarm so we keep track of how
		// many problems this alarm is a candidate for.
		if(alarm != group.getTrigger()) {
			Pri_Sec_Alarm a = (Pri_Sec_Alarm) alarm;
			int num = a.getGroupsAsCandidate();
		
			if(num > 0) {
				a.setGroupsAsCandidate(num-1);
				log.trace("Decremented number of groups, now is "+ a.getGroupsAsCandidate());
			}
		}
		
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenSubAlarmIsAttachedToGroup() ");
		}
	}
	
	@Override
	public boolean isMatchingCandidateAlarmCriteria(Alarm a) {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "isMatchingCandidateAlarmCriteria()" + a.getIdentifier() + " Sequence # " + a.getCustomFieldValue(GFPFields.SEQNUMBER));
		}
		// we have to know when to send out an alarm so we keep track of how
		// many problems this alarm is a candidate for.

		Pri_Sec_Alarm alarm = (Pri_Sec_Alarm) a;
		int num = alarm.getGroupsAsCandidate();
		alarm.setGroupsAsCandidate(num+1);
		
		if (log.isTraceEnabled())
			log.trace("Incremented number of groups, now is "+ alarm.getGroupsAsCandidate());
		
		log.info("Alarm " + a.getIdentifier() + " Sequence # " + a.getCustomFieldValue(GFPFields.SEQNUMBER) + " will be held up to 3 minutes waiting for Primary Secondary Correlation.");

		String watchdogDesc = "Candidate watchdog for:" + getProblemPolicy().getName();
		
		// this is what we use to determine how long to hold the candidate before we let it go
		long beforeTrigger = getProblemPolicy().getTimeWindow().getTimeWindowBeforeTrigger();
		long ageLeft = alarm.TimeRemaining(beforeTrigger); 
		long holdTime = ageLeft + maxTAge; 

		// To try and reduce the number of candidate watches, if there is one already set that has a greater wait time than
		// this one, then we don't have to set it.
		if(alarm.getHighestCandidateAgeingValue() < holdTime) {
			alarm.setHighestCandidateAgeingValue(holdTime);

			if (log.isTraceEnabled())
				log.trace("The candidate callback time is " + holdTime);
		
			Util.setCandidateWatch((Pri_Sec_Alarm)alarm, Pri_Sec_Alarm.class, "candidateSendCallBack", holdTime, watchdogDesc);
		}
		
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "isMatchingCandidateAlarmCriteria() ");
		}
		
		return true;
	}
	
	@Override
	public void computeLongs() 
	{ 
		if (getLongs() != null && getLongs().getLong() != null) {
			for (LongItem longItem : getLongs().getLong()) {
				if (longItem.getKey().equals(TRIGGER_AGE_KEY)) {
					maxTAge = longItem.getValue();
					
					if (log.isTraceEnabled())
						log.trace("Max possible age of trigger: = "+ maxTAge);
				} else if (longItem.getKey().equals(SUBALARM_AGE_KEY)) {
					maxSAge = longItem.getValue();
					
					if (log.isTraceEnabled())
						log.trace("Max possible age of Subalamr: = "+ maxSAge);
				}
			}
		}		 
	}
	
	@Override
	public void computeStrings() {
		if (getStrings() != null && getStrings().getString() != null) {
			for (StringItem stringItem : getStrings().getString()) {
				if (stringItem.getKey().equals(MAX_DELAYED_EVENT)) {
					maxDelayedEvent = stringItem.getValue();
					
					if (log.isTraceEnabled())
						log.trace("Max delayed event: = "+ maxDelayedEvent);
				}
			}
		}		 
	}
	
	@Override
	public Long computeGroupPriority(Alarm alarm) {
		if (log.isTraceEnabled()) {
			getLog().trace("ENTER:  computeGroupPriority()",
					alarm.getIdentifier());
		}

		Long priority = null;
		priority = Int.int2long(100);
		return priority;
	}	

}	
	
	

