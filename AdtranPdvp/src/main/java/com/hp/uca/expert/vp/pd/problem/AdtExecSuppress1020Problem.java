package com.hp.uca.expert.vp.pd.problem;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.hp.uca.expert.vp.pd.core.PD_AlarmRecognition;
import com.hp.uca.expert.vp.pd.services.PD_Service_Group;
import com.hp.uca.expert.vp.pd.services.PD_Service_Navigation;

/**
 * Implementation class for the following Adtran correlations
 * Execute suppress_10_20(M,T)  
 * 
 * @author st133d
 *
 */
public class AdtExecSuppress1020Problem extends AdtTrapsCorrProblemDefault {
 
	private Logger log = LoggerFactory.getLogger(AdtExecSuppress1020Problem.class);

	public AdtExecSuppress1020Problem() {
		super(); 
	}
	    
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
		}
			 			
		return matches;    
	}
	
	@Override
	public boolean isMatchingTriggerAlarmCriteria(Alarm a) throws Exception {

		boolean matches = false;
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "AdtExecSuppress1020Problem isMatchingTriggerAlarmCriteria()");
		}
		matches = super.isMatchingTriggerAlarmCriteria(a);
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "AdtExecSuppress1020Problem isMatchingTriggerAlarmCriteria() " + "[" + matches
					+ "]");
		}

		return matches;

	}    
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
			group.getTrigger().setAboutToBeRetracted(true);
			log.trace("##### Suppression : Alarm  " + group.getTrigger().getIdentifier() + "|{" + group.getTrigger().getCustomFieldValue(GFPFields.SEQNUMBER) + "} event key: " + group.getTrigger().getCustomFieldValue(GFPFields.EVENT_KEY) + " is Suppressed by the Adtran correlation {" +corrName + "} and the alarm that triggered this suppression is " + alarm.getIdentifier() + "|{"+ alarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "} event key: " + alarm.getCustomFieldValue(GFPFields.EVENT_KEY));  
			 
			getScenario().getSession().update(alarm);    
			log.trace("whatToDoWhenSubAlarmIsAttachedToGroup : setting alarm's = "
					+ alarm.getIdentifier()  
					+ " setAboutToBeRetracted indicator to TRUE"); 
		}
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenSubAlarmIsAttachedToGroup()");
		}  
	}
}
