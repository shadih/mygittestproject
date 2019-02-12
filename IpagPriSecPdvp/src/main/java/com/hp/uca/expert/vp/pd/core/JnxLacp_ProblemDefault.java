package com.hp.uca.expert.vp.pd.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.Int;

import com.att.gfp.data.ipagAlarm.Pri_Sec_Alarm;
import com.att.gfp.helper.GFPFields;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.vp.pd.config.LongItem;
import com.hp.uca.expert.vp.pd.config.StringItem;
import com.hp.uca.expert.vp.pd.problem.Util;
import com.hp.uca.expert.vp.pd.services.PD_Service_Group;

public class JnxLacp_ProblemDefault extends ProblemDefault {

	//protected static final String LINKDOWN_KEY = "50003/100/1";
	//private static final int PRIORITY_FACTOR = 1000;
	private static Logger log = LoggerFactory.getLogger(JnxLacp_ProblemDefault.class);

	private static final String TRIGGER_AGE_KEY = "MaxTAge";
	private static final String SUBALARM_AGE_KEY = "MaxSAge";
	private static final String MAX_DELAYED_EVENT = "MaxDelayedEvent";

	protected long maxTAge = 0;
	protected long maxSAge = 0;
	//protected long maxPAwait = 0;
	protected String maxDelayedEvent = null;


	@Override
	public Long computeGroupPriority(Alarm alarm) {
		if (log.isTraceEnabled()) {
			getLog().trace("ENTER:  computeGroupPriority()",
					alarm.getIdentifier());
		}

		Long priority = null;
		String EventKey = alarm.getCustomFieldValue(GFPFields.EVENT_KEY);

		if(EventKey.equals("50003/100/24"))
			priority = Int.int2long(1);
		else if(EventKey.equals("50004/1/18") || EventKey.equals("50004/1/20"))
			priority = Int.int2long(2);
		else if(EventKey.equals("50004/1/19") || EventKey.equals("50004/1/21") || EventKey.equals("50004/1/39"))
			priority = Int.int2long(3);
		else if(EventKey.equals("50004/1/10"))
			priority = Int.int2long(4);
		else 
			priority = Group.LOWEST_PRIORITY;

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "computeGroupPriority()",
					String.valueOf(priority));
		}
		return priority;
	}	

	@Override
	public boolean isAllCriteriaForProblemAlarmCreation(Group group)
			throws Exception {

		if (log.isTraceEnabled()) {
			log.trace(
					"ENTER:  isAllCriteriaForProblemAlarmCreation() for group ["
							+ group.getTrigger().getIdentifier() + "]" +
							group.getName() + "policy: " + this.getProblemPolicy().getName());
		}

		boolean ret = false;

		/*	
		 * Checking that there are at least two alarms in the group before to
		 * create the Problem Alarm

		Collection<Alarm> alarmsInGroup = group.getAlarmList();
		int numberOfAlarmsInGroup = alarmsInGroup.size();

		if (log.isTraceEnabled()) {
			log.trace("numberOfAlarmsInGroup = " + numberOfAlarmsInGroup);
		}

		boolean isItTime = false;

		// is it time to stop the correlation and do the pri/sec enrichment
		// if the max delayed event has arrived the we don't have to wait any longer
		// Send out the subalarms
		for (Alarm alarm : alarmsInGroup) {	
			if (log.isTraceEnabled())
				log.trace("Alarm in group: " + alarm.getCustomFieldValue(GFPFields.EVENT_KEY));

			if(maxDelayedEvent != null && alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(maxDelayedEvent))
			{
				isItTime = true;

				if (log.isTraceEnabled())
					log.trace("The max delayed event has arrived: " + maxDelayedEvent);
			}
		}

		if(!isItTime) {
			Pri_Sec_Alarm alarm = (Pri_Sec_Alarm) group.getTrigger();

			long paWait = this.getProblemPolicy().getProblemAlarm().getDelayForProblemAlarmCreation();

			isItTime = alarm.isItTimeToCreateProblemAlarm(paWait);	
		}

		log.trace("Is it time now:" + isItTime);

		if (numberOfAlarmsInGroup > 1 && isItTime) {

			if (log.isTraceEnabled()) {
				log.trace("The number of alarms is sufficient:" + numberOfAlarmsInGroup);
				log.trace("It time to create the problem alarm.");
			}

			try {
				Collection<Group> subGroups = PD_Service_Group
						.getGroupsWhereAlarmSetAsCandidateSubAlarmOrTrigger(
								group.getTrigger(), group);

				if (subGroups != null && !subGroups.isEmpty() && !PD_Service_Group.isLeadGroup(group, subGroups)) {

		 * The current Group is not the leaderGroup (lower priority)

					log.info("Group [" + group.getName()
							+ "] is not the lead group ");
				} else 
					ret = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}*/
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "isAllCriteriaForProblemAlarmCreation()",
					String.valueOf(ret));
		}

		return ret;
	}

	@Override
	public List<String> computeProblemEntity(Alarm a) throws Exception {
		/*
		 * Default behavior: get first level of managing entity
		 */
		String pbEntity = null;
		List<String> problemEntities = new ArrayList<String>();

		// instance only for the PE
		pbEntity = a.getOriginatingManagedEntity().split(" ")[1];

		problemEntities.add(pbEntity);

		// if this is the 50004/1/10 then we add the remote entity
		// this is for the A to Z correlation
		if(a.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50004/1/10")) {
			if(((Pri_Sec_Alarm) a).getRemotePportInstanceName() != null)
				problemEntities.add(((Pri_Sec_Alarm) a).getRemotePportInstanceName());
		}

		LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", computeProblemEntity() --- problemEntities=" + problemEntities);
		return problemEntities;
	}

	@Override
	public boolean isMatchingTriggerAlarmCriteria(Alarm a) throws Exception {
		boolean ret = false;

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "isMatchingTriggerAlarmCriteria()");
		}

		if ( a instanceof Pri_Sec_Alarm)  {
			Pri_Sec_Alarm alarm = (Pri_Sec_Alarm) a;	

			log.info("Alarm " + a.getIdentifier() + " Sequence # " + a.getCustomFieldValue(GFPFields.SEQNUMBER) + " will be held up to 3 minutes waiting for Primary Secondary Correlation.");

			// this is what we use to determine how long to hold the trigger before we let it go
			long afterTrigger = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger();
			long ageLeft = alarm.TimeRemaining(afterTrigger); 
			long holdTime = ageLeft + maxSAge; 

			if (log.isTraceEnabled()) {
				log.trace("Creating Trigger watch, hold time is: " + holdTime);
			}

			String watchdogDesc = "Creating watchdog for:" + getProblemPolicy().getName();

			Util.setTriggerWatch((Pri_Sec_Alarm)alarm, Pri_Sec_Alarm.class, "simpleSendCallBack", holdTime, watchdogDesc);
			ret = true;
		}
		return ret;
	}

	@Override
	public void whatToDoWhenSubAlarmIsAttachedToGroup(Alarm alarm, Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "whatToDoWhenSubAlarmIsAttachedToGroup()");
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
			LogHelper.exit(log, "isMatchingTriggerAlarmCriteria() ");
		}
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
				//				} else if (longItem.getKey().equals(MAX_PA_WAIT_KEY)) {
				//					maxPAwait = longItem.getValue();
				//					
				//					if (log.isTraceEnabled())
				//						log.trace("Max wait for PA: = "+ maxPAwait);			
				//				}
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
	public boolean isMatchingCandidateAlarmCriteria(Alarm a) {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "isMatchingCandidateAlarmCriteria()");
		}

		Pri_Sec_Alarm alarm = (Pri_Sec_Alarm) a;
		int num = alarm.getGroupsAsCandidate();

		//log.info("Alarm " + a.getIdentifier() + " Sequence # " + a.getCustomFieldValue(GFPFields.SEQNUMBER) + " will be held up to 3 minutes waiting for Primary Secondary Correlation.");

		String watchdogDesc = "Creating Candidate watchdog for:" + getProblemPolicy().getName();

		// this is what we use to determine how long to hold the candidate before we let it go
		long beforeTrigger = getProblemPolicy().getTimeWindow().getTimeWindowBeforeTrigger();

		// the calculation for the time remaining based on the hold time in precious VPs is dropped.
		// too many possible variations.   We will hold for the max time...
		//long ageLeft = alarm.TimeRemaining(beforeTrigger); 
		//long holdTime = ageLeft + maxTAge; 
		long holdTime = beforeTrigger + maxTAge;

		// To try and reduce the number of candidate watches, if there is one already set that has a greater wait time than
		// this one, then we don't have to set it.
		if(alarm.getHighestCandidateAgeingValue() < holdTime) {
			alarm.setGroupsAsCandidate(num+1);
			log.trace("Incremented number of groups, now is "+ alarm.getGroupsAsCandidate());

			alarm.setHighestCandidateAgeingValue(holdTime);
			log.trace("The time of the candidate callback is " + holdTime);

			Util.setCandidateWatch((Pri_Sec_Alarm)alarm, Pri_Sec_Alarm.class, "candidateSendCallBack", holdTime, watchdogDesc);
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "isMatchingCandidateAlarmCriteria() ");
		}

		return true;
	}
}
