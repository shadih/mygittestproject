package com.hp.uca.expert.vp.pd.problem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class AdtRGPPortCorrelator extends AdtTrapsCorrProblemDefault {

	private Logger log = LoggerFactory
	.getLogger(AdtRGPPortCorrelator.class);  
	public AdtRGPPortCorrelator() {
		super();
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
			//TODO: replace with GFPFields value
			if(group.getNumber() == 2) {
			EnrichedAdtranAlarm enrichedAdtAlarm = null;
			if(alarm instanceof EnrichedAdtranAlarm) {
				enrichedAdtAlarm = (EnrichedAdtranAlarm) alarm;
			}
			List<EnrichedAlarm> decomposedAlarms = Decomposer.decompose(enrichedAdtAlarm);
			if(decomposedAlarms != null && decomposedAlarms.size() > 0) {
				for(EnrichedAlarm decompseAlarm : decomposedAlarms) {  
					log.trace("onAlarmCreationProcess: sending decompsed alarm : " + decompseAlarm.getIdentifier());
					GFPUtil.forwardOrCascadeAlarm(decompseAlarm, AlarmDelegationType.FORWARD, null);
				}     
			}        
			} else {
				return;  
			}
		}
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenSubAlarmIsAttachedToGroup()");
		}
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
				List<String> rgList = new ArrayList<String>();
				for(String rgEntity : rgList) {
					problemEntities.add(rgEntity);
				}
			}
		}
		return problemEntities; 
	}

}
