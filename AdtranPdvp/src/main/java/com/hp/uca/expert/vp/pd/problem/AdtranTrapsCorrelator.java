package com.hp.uca.expert.vp.pd.problem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.config.AdtranCorrelationConfiguration;
import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.data.ipagAlarm.EnrichedAdtranAlarm;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.helper.CorrNames;
import com.att.gfp.helper.FilterTags;
import com.att.gfp.helper.GFPFields;  
import com.att.gfp.helper.GFPUtil;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.group.Qualifier;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.scenario.ScenarioThreadLocal;
import com.hp.uca.expert.vp.pd.core.AdtTrapsCorrProblemDefault;
import com.hp.uca.expert.vp.pd.services.PD_Service_Group;
import com.hp.uca.expert.x733alarm.AttributeChange;

/**
 * Implementation class for the following Adtran correlations adt-corr-two-traps
 * correlate-adtran-linkdown Adt-corr-one-trap
 * 
 * @author st133d
 * 
 */
public final class AdtranTrapsCorrelator extends AdtTrapsCorrProblemDefault {

	private Logger log = LoggerFactory.getLogger(AdtranTrapsCorrelator.class);

	public AdtranTrapsCorrelator() {
		super();
	}

	@Override
	public boolean isMatchingTriggerAlarmCriteria(Alarm a) throws Exception {

		boolean matches = false;
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "isMatchingTriggerAlarmCriteria()");
		}
		matches = super.isMatchingTriggerAlarmCriteria(a);
		Map<String, String> params = a.getPassingFiltersParams().get(
				getProblemContext().getName());
		String corrName = params.get(FilterTags.CORR_NAME_FILTER_TAG);
		if (matches) {
			if (CorrNames.Corr_Adt_two_traps.equals(corrName)
					|| CorrNames.Corr_Adt_one_trap.equals(corrName)) {
				if (a instanceof EnrichedAlarm) {
					EnrichedAlarm alarm = (EnrichedAlarm) a;
					log.trace("isMatchingTriggerAlarmCriteria trigget alarm = "
							+ alarm.getOriginatingManagedEntity());
					log.trace("Remote PPOrt info exists = "
							+ alarm.isRemotePPortInfoExists());
					if (alarm.isRemotePPortInfoExists()) {
						matches = true;
					} else {  
						matches = false;
					}
				}  
			}
		}
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "isMatchingTriggerAlarmCriteria() " + "[" + matches
					+ "]");
		}

		return matches;

	}
	
	@Override
	public boolean isMatchingCandidateAlarmCriteria(Alarm a) throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "isMatchingCandidateAlarmCriteria()",
					a.getIdentifier());
		}

		boolean ret = true;
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "isMatchingCandidateAlarmCriteria()",
					a.getIdentifier() + ret);     
		}  
		if (true) { 
			/* watch to retract alarm if no subalarms arrive */
			if (a instanceof EnrichedAdtranAlarm) {
				EnrichedAdtranAlarm alarm = (EnrichedAdtranAlarm) a;
				AdtranCorrelationConfiguration adtCorrelationConfig = new AdtranCorrelationConfiguration();
				if(!(adtCorrelationConfig.getAdtranCorrelationPolicies().getPportProcessing().getEventsWithNoCorrelation().getEventNames().getEventName().contains(alarm.getCustomFieldValue(GFPFields.EVENT_KEY))) &&
						!(adtCorrelationConfig.getAdtranCorrelationPolicies().getPportProcessing().getEventsWithCorrelation().getEventNames().getEventName().contains(alarm.getCustomFieldValue(GFPFields.EVENT_KEY)))) {  
				setCandidateAlarmWatch(alarm); 
				}
			} 
		} 
		return ret;        
	}
	

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.uca.expert.vp.pd.interfaces.AlarmRecognition#
	 * isMatchingSubAlarmCriteria(com.hp.uca.expert.alarm.Alarm,
	 * com.hp.uca.expert.group.Group)
	 */
	@Override
	public boolean isMatchingSubAlarmCriteria(Alarm a, Group group)
			throws Exception {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "isMatchingSubAlarmCriteria()",
					a.getIdentifier());
		}
		boolean matches = true;
		matches = super.isMatchingSubAlarmCriteria(a, group);  
		if (matches) {
			log.trace("isMatchingSubAlarmCriteria sub alarm = "
					+ a.getOriginatingManagedEntity());
			String managedInstnace = a.getOriginatingManagedEntity().split(" ")[1];
			Scenario scenario = ScenarioThreadLocal.getScenario();
			Long currentTriggerFirstOccurrence = GFPUtil
					.retrieveFeTimeFromCustomFields(group.getTrigger());
			Long currentAlarmFirstOccurrence = GFPUtil
					.retrieveFeTimeFromCustomFields(a);   
			if (currentAlarmFirstOccurrence != null
					&& currentTriggerFirstOccurrence != null
					&& currentAlarmFirstOccurrence < currentTriggerFirstOccurrence) {

				if (PD_Service_Group.isThereOnlyTrigger(group.getAlarmList())
						&& PD_Service_Group.isMatchingTriggerAlarm(a, group)) {
					log.trace("isMatchingSubAlarmCriteria isMatchingTriggerAlarm = "
							+ a.getOriginatingManagedEntity());
					PD_Service_Group.recreateGroupWithAnotherTrigger(group, a);
					// TODO Use internals
					PD_Service_Group.forceRole(group, a, Qualifier.Candidate);
					scenario.getGroups().removeGroup(group);
					scenario.getSession().retract(group);
					matches = true;
				}
			}
			if (group.getTrigger() != null) {
				log.trace("isMatchingSubAlarmCriteria Trigger is NOT NULL");
				Map<String, String> params = a.getPassingFiltersParams().get(
						getProblemContext().getName());

				String corrName = params.get(FilterTags.CORR_NAME_FILTER_TAG);
				log.trace("isMatchingSubAlarmCriteria corrName = " + corrName);
				if (CorrNames.Corr_Adt_LinkDown.equals(corrName)) {
					matches = isMatchingAdtLinkDownCorrSubAlarmCriteria(group, a);
				}
				if(CorrNames.Corr_Adt_LPort.equals(corrName)) {
					matches = isMatchingAdtLPortCorrSubAlarmCriteria(group, a);
				}    
				if(CorrNames.Corr_Adt_LPort_ReasonCode.equals(corrName)) {
					matches = isMatchingAdtLportReasonCodeSubAlarmCriteria(group,a);
				}
				if ("Corr_Adt_two_traps".equals(corrName)
						|| CorrNames.Corr_Adt_one_trap.equals(corrName)) {
					matches = isMatchingAdtCorrTrapsSubAlarmCriteria(group,
							managedInstnace);  
				}
			}
	  

	}
		if (log.isTraceEnabled()) { 
			LogHelper.exit(log, "isMatchingSubAlarmCriteria() " + "[" + matches
					+ "]");
		}
		return matches; 
	} 

	private boolean isMatchingAdtLportReasonCodeSubAlarmCriteria(Group group,
			Alarm a) {
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "isMatchingAdtLportReasonCodeSubAlarmCriteria() "
					+ "[" + "]");
		}
		String remotePortKey = "";
		boolean matches = true;
		if (group.getTrigger() instanceof EnrichedAdtranAlarm) {
			EnrichedAdtranAlarm trigger = (EnrichedAdtranAlarm) group.getTrigger();
			if( a instanceof EnrichedAlarm) {
				EnrichedAlarm subAlarm = (EnrichedAlarm) a;
				if(trigger.getReasonCodeObj().getrDevName().equals((subAlarm.getDeviceName())) &&
						trigger.getReasonCodeObj().getrNvlanIdTop().equals(subAlarm.getnVlanIdTop()) &&
						trigger.getReasonCodeObj().getrVlanId().equals(subAlarm.getVlanId())) {
						trigger.setCustomFieldValue(GFPFields.EVC_NAME, subAlarm.getCustomFieldValue(GFPFields.EVC_NAME));
						trigger.setCustomFieldValue(GFPFields.CLCI, subAlarm.getCustomFieldValue(GFPFields.CLCI));
						//TODO: set the reason interface-ip-addr of LP
						trigger.setCustomFieldValue(GFPFields.REASON_CODE, trigger.getReasonCodeObj().getrDevName() + "_" + trigger.getReasonCodeObj().getrNvlanIdTop()
								+ "_" + trigger.getReasonCodeObj().getrVlanId());  
						
				}
			}
			log.trace("isMatchingAdtCorrTrapsSubAlarmCriteria sub alarm found trigger remote port key = "
					+ trigger.getRemotePportInstanceName());
			remotePortKey = trigger.getRemotePportInstanceName();
		}
		String managedInstnace = a.getOriginatingManagedEntity().split(" ")[1];
		if (managedInstnace.equalsIgnoreCase(remotePortKey)) {
			log.trace("isMatchingAdtCorrTrapsSubAlarmCriteria sub alarm crieteria matched");
			matches = true;
		} else {
			matches = false;
		}
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "isMatchingAdtCorrTrapsSubAlarmCriteria() "
					+ "[" + matches + "]");
		}
		return matches;		
	}

	/**
	 * Checks if AdtCorrTraps correlation sub alarm criteria matches
	 * 
	 * @param group
	 * @param managedInstnace
	 * @return
	 */
	private boolean isMatchingAdtCorrTrapsSubAlarmCriteria(Group group,
			String managedInstnace) {
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "isMatchingAdtCorrTrapsSubAlarmCriteria() "
					+ "[" + "]");
		}
		String remotePortKey = "";
		boolean matches = true;
		if (group.getTrigger() instanceof EnrichedAlarm) {
			EnrichedAlarm trigger = (EnrichedAlarm) group.getTrigger();
			log.trace("isMatchingAdtCorrTrapsSubAlarmCriteria sub alarm found trigger remote port key = "
					+ trigger.getRemotePportInstanceName());
			remotePortKey = trigger.getRemotePportInstanceName();
		}
		if (managedInstnace.equalsIgnoreCase(remotePortKey)) {
			log.trace("isMatchingAdtCorrTrapsSubAlarmCriteria sub alarm crieteria matched");
			matches = true;
		} else {
			matches = false;
		}
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "isMatchingAdtCorrTrapsSubAlarmCriteria() "
					+ "[" + matches + "]");
		}
		return matches;
	}

	/**
	 * Checks if AdtLinkDown correlation sub alarm criteria matches
	 * 
	 * @param group
	 * @param a
	 * @return
	 */
	private boolean isMatchingAdtLinkDownCorrSubAlarmCriteria(Group group,
			Alarm a) {
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "isMatchingAdtLinkDownCorrSubAlarmCriteria() "
					+ "[" + "]");
		}
		boolean matches = true;
			String slotForTrigger = GFPUtil.parseSpecifiedValueFromField(group
					.getTrigger().getCustomFieldValue(GFPFields.COMPONENT),
					"slot", " ");
			log.trace("slot for trigget = " + slotForTrigger);
			String slotForSubAlarm = GFPUtil.parseSpecifiedValueFromField(
					a.getCustomFieldValue(GFPFields.COMPONENT), "slot", " ");
			log.trace("slot for subAlarm = " + slotForSubAlarm);  
			if (("252".equalsIgnoreCase(slotForTrigger) && "253"   
					.equalsIgnoreCase(slotForSubAlarm))
					|| ("253".equalsIgnoreCase(slotForTrigger) && "252"
							.equalsIgnoreCase(slotForSubAlarm))) {
				log.trace("isMatchingAdtLinkDownCorrSubAlarmCriteria met slot252/253 correlation criteria");
				log.trace("isMatchingAdtLinkDownCorrSubAlarmCriteria TRIGGER Event key "
						+ group.getTrigger().getCustomFieldValue(
								GFPFields.EVENT_KEY));
				AdtranCorrelationConfiguration adtCorrelationConfig = new AdtranCorrelationConfiguration();
				if (adtCorrelationConfig
						.getAdtranCorrelationPolicies()
						.getAdtranLinkDownCorrelation()
						.getAdtLinkDownCorrKeys()
						.getEventNames()
						.getEventName()
						.contains(
								group.getTrigger().getCustomFieldValue(
										GFPFields.EVENT_KEY))) {
					log.trace("isMatchingAdtLinkDownCorrSubAlarmCriteria trigger is one of the corr keys");
					if (adtCorrelationConfig
							.getAdtranCorrelationPolicies()
							.getAdtranLinkDownCorrelation()
							.getAdtLinkDownAlarm()
							.getEventNames()
							.getEventName()
							.contains(
									a.getCustomFieldValue(GFPFields.EVENT_KEY))) {
						log.trace("isMatchingAdtLinkDownCorrSubAlarmCriteria TRIGGER Event key "
								+ group.getTrigger().getCustomFieldValue(
										GFPFields.EVENT_KEY));
						matches = true;
					} else {
						matches = false;
					}
				} else if (adtCorrelationConfig
						.getAdtranCorrelationPolicies()
						.getAdtranLinkDownCorrelation()
						.getAdtLinkDownAlarm()
						.getEventNames()
						.getEventName()
						.contains(
								group.getTrigger().getCustomFieldValue(
										GFPFields.EVENT_KEY))) {
					log.trace("isMatchingAdtLinkDownCorrSubAlarmCriteria trigger is link down");
					if (adtCorrelationConfig
							.getAdtranCorrelationPolicies()
							.getAdtranLinkDownCorrelation()
							.getAdtLinkDownCorrKeys()
							.getEventNames()
							.getEventName()
							.contains(
									a.getCustomFieldValue(GFPFields.EVENT_KEY))) {
						log.trace("isMatchingAdtLinkDownCorrSubAlarmCriteria TRIGGER Event key "
								+ group.getTrigger().getCustomFieldValue(
										GFPFields.EVENT_KEY));
						matches = true;
					} else {
						matches = false;
					}

				}
			} else {
				matches = false;
			}
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "isMatchingAdtLinkDownCorrSubAlarmCriteria() "
					+ "[" + matches + "]");
		}
		return matches;
	}

	/**
	 * Checks if AdtLinkDown correlation sub alarm criteria matches
	 * 
	 * @param group
	 * @param a
	 * @return
	 */
	private boolean isMatchingAdtLPortCorrSubAlarmCriteria(Group group,
			Alarm a) {
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "isMatchingAdtLPortCorrSubAlarmCriteria() "
					+ "[" + "]");
		}
		boolean matches = true;
		if ((GFPUtil.retrieveFeTimeFromCustomFields(group.getTrigger()) - GFPUtil
				.retrieveFeTimeFromCustomFields(a)) < 180000) {
			log.trace("isMatchingAdtLPortCorrSubAlarmCriteria FE time of trigger minus SUB Alarm is less than ADT LINKDOWN INTERVAL");
				log.trace("isMatchingAdtLPortCorrSubAlarmCriteria TRIGGER Event key "
						+ group.getTrigger().getCustomFieldValue(
								GFPFields.EVENT_KEY));
				AdtranCorrelationConfiguration adtCorrelationConfig = new AdtranCorrelationConfiguration();
				if (adtCorrelationConfig
						.getAdtranCorrelationPolicies()
						.getAdtranLPortCorrelation()
						.getAdtLPortCorrKeys()
						.getEventNames()
						.getEventName()
						.contains(
								group.getTrigger().getCustomFieldValue(
										GFPFields.EVENT_KEY))) {
					log.trace("isMatchingAdtLPortCorrSubAlarmCriteria trigger is one of the corr keys");
					if (adtCorrelationConfig
							.getAdtranCorrelationPolicies()
							.getAdtranLPortCorrelation()
							.getAdtLPortPPPAlarm()
							.getEventNames()
							.getEventName()
							.contains(
									a.getCustomFieldValue(GFPFields.EVENT_KEY))) {
						log.trace("isMatchingAdtLPortCorrSubAlarmCriteria TRIGGER Event key "
								+ group.getTrigger().getCustomFieldValue(
										GFPFields.EVENT_KEY));
						matches = true;
					} else {
						matches = false;
					}
				} else if (adtCorrelationConfig
						.getAdtranCorrelationPolicies()
						.getAdtranLPortCorrelation()
						.getAdtLPortPPPAlarm()
						.getEventNames()
						.getEventName()
						.contains(
								group.getTrigger().getCustomFieldValue(
										GFPFields.EVENT_KEY))) {
					log.trace("isMatchingAdtLPortCorrSubAlarmCriteria trigger is link down");
					if (adtCorrelationConfig
							.getAdtranCorrelationPolicies()
							.getAdtranLPortCorrelation()
							.getAdtLPortCorrKeys()
							.getEventNames()
							.getEventName()
							.contains(
									a.getCustomFieldValue(GFPFields.EVENT_KEY))) {
						log.trace("isMatchingAdtLPortCorrSubAlarmCriteria TRIGGER Event key "
								+ group.getTrigger().getCustomFieldValue(
										GFPFields.EVENT_KEY));
						matches = true;
					} else {
						matches = false;
					}
  
				}
			} else {  
				matches = false;
			}  
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "isMatchingAdtLPortCorrSubAlarmCriteria() "
					+ "[" + matches + "]");
		}
		return matches;
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
			} else if (corrkey.equalsIgnoreCase("Device")) {
				log.trace("problems for group2");
				String managedInstance = a.getOriginatingManagedEntity().split(
						" ")[1];
				pbEntity = managedInstance.split("/")[0];
				problemEntities.add(pbEntity);
			}
		}
		String corrkey2 = params.get(FilterTags.CORR_KEY2_FILTER_PARAM);
		log.trace("corrkey2 = " + corrkey2);
		if (corrkey2 != null && !corrkey2.equals("null")) {
			if (a instanceof EnrichedAlarm) {
				EnrichedAlarm alarm = (EnrichedAlarm) a;
				pbEntity = alarm.getRemotePportInstanceName();
				if(pbEntity != null) {
					problemEntities.add(pbEntity);  
				}
			}
		}
		for (String problem : problemEntities) {
			log.trace("problems added = " + problem);
		}
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "computeProblemEntity() --- ProblemEntity="
					+ pbEntity);
		}

		return problemEntities;

	}

	/*
	 * Sends alarms if the an orphan alarm attribute has changed (non-Javadoc)
	 * 
	 * @see com.hp.uca.expert.vp.pd.core.ProblemDefault#
	 * whatToDoWhenOrphanAlarmAttributeHasChanged(com.hp.uca.expert.alarm.Alarm,
	 * com.hp.uca.expert.x733alarm.AttributeChange)
	 */
	@Override
	public void whatToDoWhenOrphanAlarmAttributeHasChanged(Alarm alarm,
			AttributeChange attributeChange) throws Exception {

		if (log.isTraceEnabled()) {
			LogHelper
					.enter(log, "whatToDoWhenOrphanAlarmAttributeHasChanged()");
		}

		// if we went from Candidate to alarm then the problem time window has
		// expired
		// and we can disregard this alarm and forward it
		if (attributeChange.getName().equalsIgnoreCase("pd")
				&& attributeChange.getOldValue().equalsIgnoreCase("Candidate")) {
			// TODO: test for SubAlarm may be redundant
			GFPUtil.forwardOrCascadeAlarm((EnrichedAlarm) alarm,
					AlarmDelegationType.FORWARD, null);
		}

		// sending all candidate alarms that were not attached to a group
		GFPUtil.forwardOrCascadeAlarm((EnrichedAlarm) alarm,
				AlarmDelegationType.FORWARD, null);

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenOrphanAlarmAttributeHasChanged()");
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.hp.uca.expert.vp.pd.interfaces.GroupLifecycle#
	 * whatToDoWhenProblemAlarmIsAttachedToGroup( com.hp.uca.expert.group.Group)
	 */
	@Override
	public void whatToDoWhenProblemAlarmIsAttachedToGroup(Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.method(log,
					"whatToDoWhenProblemAlarmIsAttachedToGroup()", group
							.getProblemAlarm().getIdentifier());
		}  

	}

	@Override
	public boolean calculateIfProblemAlarmhasToBeCleared(Group arg0)
			throws Exception {
		/*
		 * ProblemAlarm Clearance is manually managed through this Customization
		 */
		return false;

	}

}
