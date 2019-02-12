package com.hp.uca.expert.vp.pd.problem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.config.AdtranCorrelationConfiguration;
import com.att.gfp.data.ipag.decomposition.Decomposer;
import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.data.ipagAlarm.EnrichedAdtranAlarm;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.helper.FilterTags;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.group.Qualifier;
import com.hp.uca.expert.vp.pd.core.AdtTrapsCorrProblemDefault;
import com.hp.uca.expert.vp.pd.services.PD_Service_Group;

/**
 * Implementation class for the following Adtran correlations
 * CFM Correlation  
 * 
 * @author st133d
 *  
 */
public class AdtLportPppLinkLcpCorrelator extends AdtTrapsCorrProblemDefault {

	private Logger log = LoggerFactory
	.getLogger(AdtLportPppLinkLcpCorrelator.class);  
	public AdtLportPppLinkLcpCorrelator() {
		super();
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
				setCandidateAlarmWatch(alarm); 
			} 
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
				if("LPORT".equalsIgnoreCase(a.getOriginatingManagedEntity().split(" ")[0])) {
					String lportInstance = a.getOriginatingManagedEntity().split(" ")[1];
					pbEntity = lportInstance .substring(0, lportInstance.lastIndexOf("/")); 
				} else {
				pbEntity = a.getOriginatingManagedEntity().split(" ")[1];
				} 
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
			group.getTrigger().setAboutToBeRetracted(true);  
			log.trace("##### Suppression : Alarm  " + group.getTrigger().getIdentifier() + "|{" + group.getTrigger().getCustomFieldValue(GFPFields.SEQNUMBER) + "} event key: " + group.getTrigger().getCustomFieldValue(GFPFields.EVENT_KEY) + " is Suppressed by the Adtran correlation {" +corrName + "} and the alarm that triggered this suppression is " + alarm.getIdentifier() + "|{"+ alarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "} event key: " + alarm.getCustomFieldValue(GFPFields.EVENT_KEY));  
		}  
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenSubAlarmIsAttachedToGroup()");
		}  
	}
 
	
}
