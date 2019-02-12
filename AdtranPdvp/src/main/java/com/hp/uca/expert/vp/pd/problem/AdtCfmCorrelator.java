package com.hp.uca.expert.vp.pd.problem;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.EnrichedAdtranAlarm;
import com.att.gfp.helper.FilterTags;      
import com.att.gfp.helper.GFPFields;
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
public class AdtCfmCorrelator extends AdtTrapsCorrProblemDefault {

	private Logger log = LoggerFactory
	.getLogger(AdtCfmCorrelator.class);  
	public AdtCfmCorrelator() {     
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
			EnrichedAdtranAlarm enrichedAdtAlarm = null;
			if(alarm instanceof EnrichedAdtranAlarm) {
				enrichedAdtAlarm = (EnrichedAdtranAlarm) alarm;
			}
			group.getTrigger().setCustomFieldValue(GFPFields.ACNABAN, alarm.getCustomFieldValue(GFPFields.ACNABAN)); 
			group.getTrigger().setCustomFieldValue(GFPFields.VRF_NAME, alarm.getCustomFieldValue(GFPFields.VRF_NAME));
			group.getTrigger().setCustomFieldValue(GFPFields.CLCI, alarm.getCustomFieldValue(GFPFields.UNICKT));
			group.getTrigger().setCustomFieldValue(GFPFields.CIRCUIT_ID, alarm.getCustomFieldValue(GFPFields.UNICKT));
			group.getTrigger().setCustomFieldValue(GFPFields.EVC_NAME, alarm.getCustomFieldValue(GFPFields.EVC_NAME));
			group.getTrigger().setCustomFieldValue(GFPFields.REASON, enrichedAdtAlarm.getReasonObj().getReasonTmp() + "EVCID="  + "<" +alarm.getCustomFieldValue(GFPFields.EVC_NAME)
					+ ">" +  "FLAGS={}");   
			//TODO: forward to suppress-on-juniper-lag-linkdown 
			  
			group.getTrigger().setAboutToBeRetracted(true);
			getScenario().getSession().update(group.getTrigger());
			log.trace("whatToDoWhenSubAlarmIsAttachedToGroup : setting Trigger alarm = "
					+ group.getTrigger().getIdentifier()
					+ " setAboutToBeRetracted indicator to TRUE");
		}
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenSubAlarmIsAttachedToGroup()");
		}
	}

}
