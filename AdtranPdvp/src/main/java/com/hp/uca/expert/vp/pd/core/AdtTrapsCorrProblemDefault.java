/**
 * 
 */
package com.hp.uca.expert.vp.pd.core;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;  
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.config.AdtranCorrelationConfiguration;
import com.att.gfp.data.ipagAlarm.EnrichedAdtranAlarm;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.helper.FilterTags;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
import com.att.gfp.helper.CorrNames;
import com.hp.uca.common.callback.Callback;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.group.Qualifier;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.scenario.ScenarioThreadLocal;
import com.hp.uca.expert.vp.pd.core.ProblemDefault;
import com.hp.uca.expert.vp.pd.services.PD_Service_Group;
import com.hp.uca.expert.vp.pd.services.PD_Service_Navigation;
import com.hp.uca.expert.vp.pd.services.PD_Service_ProblemAlarm;

/**
 * @author Sruthi 
 * Base class for all Adtran problems  
 * 
 */
public class AdtTrapsCorrProblemDefault extends ProblemDefault {

	private static final int ADT_INTERVAL = 5000;
	private Logger log = LoggerFactory
			.getLogger(AdtTrapsCorrProblemDefault.class);   
	private static final int PRIORITY_FACTOR = 1000;
 
	@Override 
	public boolean isMatchingTriggerAlarmCriteria(Alarm a) throws Exception {
		boolean ret = false;

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "isMatchingTriggerAlarmCriteria()");
		}
		ret = super.isMatchingTriggerAlarmCriteria(a);
		if (a instanceof EnrichedAlarm) {
			EnrichedAlarm alarm = (EnrichedAlarm) a;
			if (alarm.getAlarmTargetExist()) {
				ret = true; 
			} else { 
				ret = false;      
			}
		}
		if (true) {
			/* watch to retract alarm if no subalarms arrive */
			if (a instanceof EnrichedAdtranAlarm) {
				EnrichedAdtranAlarm alarm = (EnrichedAdtranAlarm) a;
				setTriggerWatch(alarm);
				ret = true;
			}
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "isMatchingTriggerAlarmCriteria()");
		}
		return ret;
	}

	@Override
	public boolean isMatchingSubAlarmCriteria(Alarm a, Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "isMatchingSubAlarmCriteria()",
					a.getIdentifier());
		}

		boolean ret = false;
		ret = super.isMatchingSubAlarmCriteria(a, group);
		if (a instanceof EnrichedAlarm) {
			EnrichedAlarm alarm = (EnrichedAlarm) a;  
			if (alarm.getAlarmTargetExist()) {  
				ret = true;       
			} else {  
				ret = false;            
			}  
		}   
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "isMatchingSubAlarmCriteria()",
					a.getIdentifier() + ret);     
		}  
		if (true) {
			/* watch to retract alarm if no subalarms arrive */
			if (a instanceof EnrichedAdtranAlarm) {
				EnrichedAdtranAlarm alarm = (EnrichedAdtranAlarm) a;
				Map<String, String> params = a.getPassingFiltersParams().get(
						getProblemContext().getName());
				String corrName = params.get(FilterTags.CORR_NAME_FILTER_TAG);
				if (!(CorrNames.Correlate_fru_with_childport_events.equals(corrName))) {
				setCandidateAlarmWatch(alarm);
				}
				ret = true;
			}
		} 
//		if(true) {  
//			Long currentTriggerFirstOccurrence = GFPUtil
//			.retrieveFeTimeFromCustomFields(group.getTrigger());
//			Long currentAlarmFirstOccurrence = GFPUtil
//					.retrieveFeTimeFromCustomFields(a);
//			if (currentAlarmFirstOccurrence != null   
//					&& currentTriggerFirstOccurrence != null
//					&& currentAlarmFirstOccurrence < currentTriggerFirstOccurrence) {
//				log.trace("isMatchingSubAlarmCriteria() : life cycle methods added  " + Qualifier.Candidate);
//			log.trace("isMatchingSubAlarmCriteria() : alarm attached is  " + Qualifier.Candidate);
////			PD_Service_Group.forceRole(group, a, Qualifier.Candidate);    
//			Scenario scenario = ScenarioThreadLocal.getScenario();
//			group.addAlarm(a); 
//			   whatToDoWhenSubAlarmIsAttachedToGroup(a, group); 
//			    PD_Service_Navigation.needProblemAlarmNavigationUpdate(scenario, group);
//			    scenario.getSession().update(group);  
//		}     
//		}  
		return ret;        
	}    
  
	/*
	 * (non-Javadoc) 
	 * @see com.hp.uca.expert.vp.pd.core.ProblemDefault#isAllCriteriaForProblemAlarmCreation(com.hp.uca.expert.group.Group)
	 */
	@Override
	public boolean isAllCriteriaForProblemAlarmCreation(Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "isAllCriteriaForProblemAlarmCreation()",
					group.getName());
		}  
		boolean ret = true;  
  
		/*  
		 * Checking that there are at least two alarms in the group before to
		 * create the Problem Alarm
		 */
		Collection<Alarm> alarmsInGroup = group.getAlarmList();
		int numberOfAlarmsInGroup = alarmsInGroup.size();

		if (log.isDebugEnabled()) {
			log.debug("numberOfAlarmsInGroup = " + numberOfAlarmsInGroup);
		}

		boolean isNumberOfAlarmsInGroupSufficient = false;
		// TODO simplify ret
		if (numberOfAlarmsInGroup > 1) {
			isNumberOfAlarmsInGroupSufficient = true;
		}

		ret = ret && isNumberOfAlarmsInGroupSufficient;
		if (ret == true) {
			Collection<Group> subGroups = PD_Service_Group
					.getGroupsWhereAlarmSetAsCandidateSubAlarmOrTrigger(
							group.getTrigger(), group);
			int subGrCnt = 0;
			for (Group subGr : subGroups) {
				subGrCnt = subGr.getNumber();
			}
			if ((!PD_Service_Group.isLeadGroup(group, subGroups))
					&& subGrCnt > 1) {
				/*
				 * The current Group is not the leaderGroup (lower priority)
				 */
				ret = false;
			}
		}
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "isAllCriteriaForProblemAlarmCreation()",
					String.valueOf(ret));
		}
		return ret;
	}

	@Override
	public List<String> computeProblemEntity(Alarm a) throws Exception {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "computeProblemEntity()");
		}
		List<String> problemEntities = new ArrayList<String>();
		String pbEntity = "";
		Map<String, String> params = a.getPassingFiltersParams().get(
				getProblemContext().getName());
		String corrkey = params.get(FilterTags.CORR_KEY_FILTER_PARAM);
		String corrName = params.get(FilterTags.CORR_NAME_FILTER_TAG);
		log.trace("corrName = " + corrName);
		if (corrkey != null && !corrkey.equals("null")) {
			log.trace("corrkey = " + corrkey);
			if (corrkey.equalsIgnoreCase("ManagedInstnace")) {
				log.trace("problems for group1");
				pbEntity = a.getOriginatingManagedEntity().split(" ")[1];
				problemEntities.add(pbEntity);
				log.trace("problems entity = " + pbEntity);  
			}
		}
		return problemEntities;
	}

	/*
	 * (non-Javadoc) Invoked when a alarm is attached to the group
	 * @see com.hp.uca.expert.vp.pd.core.ProblemDefault#whatToDoWhenSubAlarmIsAttachedToGroup(com.hp.uca.expert.alarm.Alarm, com.hp.uca.expert.group.Group)
	 */
	@Override
	public void whatToDoWhenSubAlarmIsAttachedToGroup(Alarm alarm, Group group)
			throws Exception {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "whatToDoWhenSubAlarmIsAttachedToGroup()");
		}
		if (alarm != null) {     
			log.trace("whatToDoWhenSubAlarmIsAttachedToGroup : sub alarm = "
					+ alarm.getIdentifier());
		}
		if (alarm == group.getTrigger()) {
			log.trace("whatToDoWhenSubAlarmIsAttachedToGroup : alarm attached is Trigger ");
			PD_Service_Group.forceRole(group, alarm, Qualifier.Trigger);
		}
		if (group != null) {
			if (group.getTrigger() != null) {
				log.trace("whatToDoWhenSubAlarmIsAttachedToGroup : Trigger alarm = "
						+ group.getTrigger().getIdentifier());
			}
			if (group.getNbAlarmsSinceCreation() != group
					.getNbNotAcknowledgedAlarms()) {
				log.trace("whatToDoWhenSubAlarmIsAttachedToGroup : Numbers are not equal = "
						+ group.getNbNotAcknowledgedAlarms());
				log.trace("whatToDoWhenSubAlarmIsAttachedToGroup : Numbers of acknowledged alarms = "
						+ group.getNbAcknowledgedAlarms());
				group.addAlarm(group.getTrigger());
			}
			log.trace("whatToDoWhenSubAlarmIsAttachedToGroup : group count = "
					+ group.getNumber());
		}
		if (group != null && alarm != group.getTrigger()) {
			Map<String, String> params = alarm.getPassingFiltersParams().get(
					getProblemContext().getName());
			String corrName = params.get(FilterTags.CORR_NAME_FILTER_TAG);
			log.trace("whatToDoWhenSubAlarmIsAttachedToGroup : CorrName = "
					+ corrName);
			log.trace("correlate existing " + group.getTrigger().getIdentifier() + " with " + alarm.getIdentifier() + ";{" + alarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}");
//			if (CorrNames.Corr_Adt_two_traps.equals(corrName)
//					|| CorrNames.Corr_Adt_one_trap.equals(corrName)) {
//				group.getTrigger().setAboutToBeRetracted(true);
//				log.trace("##### Suppression : Alarm  " + group.getTrigger().getIdentifier() + "|{" + group.getTrigger().getCustomFieldValue(GFPFields.SEQNUMBER) + "} event key: " + group.getTrigger().getCustomFieldValue(GFPFields.EVENT_KEY) + " is Suppressed by the Adtran correlation {" +corrName + "} and the alarm that triggered this suppression is " + alarm.getIdentifier() + "|{"+ alarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "} event key: " + alarm.getCustomFieldValue(GFPFields.EVENT_KEY));  
//			} else {
			if (CorrNames.Corr_Adt_two_traps.equals(corrName)) {
				Long currentTriggerFirstOccurrence = GFPUtil 
				.retrieveFeTimeFromCustomFields(group.getTrigger());
				Long currentAlarmFirstOccurrence = GFPUtil
						.retrieveFeTimeFromCustomFields(alarm);   
					if (currentAlarmFirstOccurrence != null
						&& currentTriggerFirstOccurrence != null
						&& currentAlarmFirstOccurrence < currentTriggerFirstOccurrence) {
						group.getTrigger().setAboutToBeRetracted(true);  
							log.trace("##### Suppression : Alarm  " + group.getTrigger().getIdentifier() + "|{" + group.getTrigger().getCustomFieldValue(GFPFields.SEQNUMBER) + "} event key: " + group.getTrigger().getCustomFieldValue(GFPFields.EVENT_KEY) + " is Suppressed by the Adtran correlation {" +corrName + "} and the alarm that triggered this suppression is " + alarm.getIdentifier() + "|{"+ alarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "} event key: " + alarm.getCustomFieldValue(GFPFields.EVENT_KEY));  
						} 
			}
			if (CorrNames.Corr_Adt_one_trap.equals(corrName)
					|| CorrNames.Corr_Adt_LPort.equals(corrName)) {
				Long currentTriggerFirstOccurrence = GFPUtil 
				.retrieveFeTimeFromCustomFields(group.getTrigger()); 
				Long currentAlarmFirstOccurrence = GFPUtil
						.retrieveFeTimeFromCustomFields(alarm);   
					if (currentAlarmFirstOccurrence != null
						&& currentTriggerFirstOccurrence != null
						&& currentAlarmFirstOccurrence > currentTriggerFirstOccurrence) {
							alarm.setAboutToBeRetracted(true);  
							log.trace("##### Suppression : Alarm  " + alarm.getIdentifier() + "|{" + alarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "} event key: " + alarm.getCustomFieldValue(GFPFields.EVENT_KEY) + " is Suppressed by the Adtran correlation {" +corrName + "} and the alarm that triggered this suppression is " + group.getTrigger().getIdentifier() + "|{"+ group.getTrigger().getCustomFieldValue(GFPFields.SEQNUMBER) + "} event key: " + group.getTrigger().getCustomFieldValue(GFPFields.EVENT_KEY));  
						}
			} else {
			alarm.setAboutToBeRetracted(true);  
			log.trace("##### Suppression : Alarm  " + alarm.getIdentifier() + "|{" + alarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "} event key: " + alarm.getCustomFieldValue(GFPFields.EVENT_KEY) + " is Suppressed by the Adtran correlation {" +corrName + "} and the alarm that triggered this suppression is " + group.getTrigger().getIdentifier() + "|{"+ group.getTrigger().getCustomFieldValue(GFPFields.SEQNUMBER) + "} event key: " + group.getTrigger().getCustomFieldValue(GFPFields.EVENT_KEY));  
			}
//			} 
		//	getScenario().getSession().update(alarm);      
//			log.trace("whatToDoWhenSubAlarmIsAttachedToGroup : setting alarm's = " 
//					+ alarm.getIdentifier()   
//					+ " setAboutToBeRetracted indicator to TRUE"); 
		}  
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenSubAlarmIsAttachedToGroup()");
		}  
	}

	/**
	 * Computes Group Priority
	 */
	@Override
	public Long computeGroupPriority(Alarm alarm) {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "computeGroupPriority()",
					alarm.getIdentifier());
		}
		Long priority = null;
		Map<String, String> params = alarm.getPassingFiltersParams().get(
				getProblemContext().getName());
		String corrPriString = params
				.get(FilterTags.CORR_PRIORITY_FILTER_PARAM);
		String trigPriString = params
				.get(FilterTags.TRIGGER_PRIORITY_FILTER_PARAM);
		Long corrPriority = null;
		Long trigPriority = null;
		try {
			corrPriority = Long.valueOf(corrPriString);
			trigPriority = Long.valueOf(trigPriString);
			priority = corrPriority * PRIORITY_FACTOR + trigPriority;
		} catch (Exception e) {
			priority = Group.LOWEST_PRIORITY;
		}
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "computeGroupPriority()",
					String.valueOf(priority));
		}

		return priority;
	}

	/**
	 * Sets the callback method to forward the trigger alarm if no Subalarm arrives. 
	 * @param alarm
	 */
	public void setTriggerWatch(EnrichedAdtranAlarm alarm) {

		if (!alarm.getTriggerWatchSet()) {

			// watch to send the trigger alarm if no subalarms arrive
			Class<?> partypes[] = new Class[0];
			Object arglist[] = new Object[0];
			Method method = null;

			try {
				method = EnrichedAdtranAlarm.class.getMethod(
						"expirationCallBack", partypes);
			} catch (SecurityException e) {
				log.trace("setTriggerWatch: ERROR: "+ e.toString() + " Trace = " + Arrays.toString(e.getStackTrace()));
				//e.printStackTrace();
			} catch (NoSuchMethodException e) {
				log.trace("setTriggerWatch: ERROR: "+ e.toString() + " Trace = " + Arrays.toString(e.getStackTrace()));
				//e.printStackTrace();
			}

			Callback callback = new Callback(method, alarm, arglist);

			// get the after trigger time and set this watchdog with that value
			// if no subalarms are added before this, we know that the group
			// will
			// not form and we can cascade this alarm with no change.
			long after = 0;
			AdtranCorrelationConfiguration adtCorrelationConfig = new AdtranCorrelationConfiguration();

			// to make sure...
			if (after == 0) {
				if (adtCorrelationConfig
						.getAdtranCorrelationPolicies()
						.getAdtranAgingEvents()
						.getEventNames()
						.getEventName()
						.contains(
								alarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
					after = after
							+ Long.valueOf(adtCorrelationConfig
									.getAdtranCorrelationPolicies()
									.getAdtranAgingEvents().getAgingInterval());
					log.trace("Aging the alarm " + alarm.getIdentifier() + "|{" + alarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "} event key: " + alarm.getCustomFieldValue(GFPFields.EVENT_KEY) + " for " + after + " milliseconds" );
				} else { 
					after = after + ADT_INTERVAL;  
				}
			}
			Scenario scenario = ScenarioThreadLocal.getScenario(); 
			scenario.addCallbackWatchdogItem(after, callback, false,
					"Expiration WatchdogItem", true, alarm);

			if (log.isTraceEnabled()) {
				LogHelper.exit(log, "isMatchingTriggerAlarmCriteria() "
						+ "Watchdog has been set to:" + after);
			}
			alarm.setTriggerWatchSet(true);
			alarm.setNumberOfWatches(alarm.getNumberOfWatches() + 1);
		}
  
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "setTriggerWatch()");
		}
	}  
	
	/**
	 * Sets the callback method to forward the trigger alarm if no Subalarm arrives. 
	 * @param alarm
	 */
	public void setCandidateAlarmWatch(EnrichedAdtranAlarm alarm) {

		if (!alarm.getCandidateAlarmWatchSet()) {

			// watch to send the trigger alarm if no subalarms arrive
			Class<?> partypes[] = new Class[0];
			Object arglist[] = new Object[0];
			Method method = null;

			try {
				method = EnrichedAdtranAlarm.class.getMethod(  
						"candidateAlarmexpirationCallBack", partypes);
			} catch (SecurityException e) {
				log.trace("setCandidateAlarmWatch: ERROR: "+ e.toString() + " Trace = " + Arrays.toString(e.getStackTrace()));
				//e.printStackTrace();
			} catch (NoSuchMethodException e) {
				log.trace("setCandidateAlarmWatch: ERROR: "+ e.toString() + " Trace = " + Arrays.toString(e.getStackTrace()));
				//e.printStackTrace();
			} 

			Callback callback = new Callback(method, alarm, arglist);

			// get the after trigger time and set this watchdog with that value
			// if no subalarms are added before this, we know that the group
			// will
			// not form and we can cascade this alarm with no change.
			long after = 0;
			AdtranCorrelationConfiguration adtCorrelationConfig = new AdtranCorrelationConfiguration();

			// to make sure...
			if (after == 0) {
				if (adtCorrelationConfig
						.getAdtranCorrelationPolicies() 
						.getAdtranAgingEvents()
						.getEventNames()
						.getEventName()
						.contains(
								alarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
					after = after
							+ Long.valueOf(adtCorrelationConfig
									.getAdtranCorrelationPolicies()
									.getAdtranAgingEvents().getAgingInterval());
				} else { 
					after = after + ADT_INTERVAL;  
				} 
			}
			log.trace("Aging the alarm " + alarm.getIdentifier() + "|{" + alarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "} event key: " + alarm.getCustomFieldValue(GFPFields.EVENT_KEY) + " for " + after + " milliseconds" );
			Scenario scenario = ScenarioThreadLocal.getScenario(); 
			scenario.addCallbackWatchdogItem(after, callback, false,
					"Expiration WatchdogItem", true, alarm);

			if (log.isTraceEnabled()) {
				LogHelper.exit(log, "isMatchingSubAlarmCriteria() "
						+ "Watchdog has been set to:" + after);
			}
			alarm.setCandidateAlarmWatchSet(true);
		} 
  
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "setCandidateAlarmWatch()");
		}
	}  
	
	@Override
	public boolean isMatchingCandidateAlarmCriteria(Alarm a) throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "isMatchingCandidateAlarmCriteria()",
					a.getIdentifier());
		}
			/* watch to retract alarm if no subalarms arrive */
			if (a instanceof EnrichedAdtranAlarm) {
				EnrichedAdtranAlarm alarm = (EnrichedAdtranAlarm) a;
				setCandidateAlarmWatch(alarm); 
			} 
		
		return true;        
	}
	
//	@Override
//	public void whatToDoWhenOrphanAlarmIsCleared(Alarm alarm) throws Exception {
//
//		if (log.isTraceEnabled()) {
//			LogHelper.enter(log, "whatToDoWhenOrphanAlarmIsCleared()",
//					alarm.getIdentifier());
//		}
//		alarm.setAboutToBeRetracted(true);
//		getScenario().getSession().retract(alarm);   
//		if (log.isTraceEnabled()) {  
//			LogHelper.exit(log, "whatToDoWhenOrphanAlarmIsCleared()");
//		}
//		
//	}

}
