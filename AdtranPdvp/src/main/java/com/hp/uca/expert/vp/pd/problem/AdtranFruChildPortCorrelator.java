package com.hp.uca.expert.vp.pd.problem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.config.AdtranCorrelationConfiguration;
import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.data.ipagAlarm.EnrichedAdtranLinkDownAlarm;
import com.att.gfp.data.ipagAlarm.EnrichedAdtranAlarm;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.helper.CorrNames;
import com.att.gfp.helper.FilterTags;
import com.att.gfp.helper.GFPFields;  
import com.att.gfp.helper.GFPUtil;
import com.att.gfp.helper.AdtranPportAlarmProcessor;
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
public final class AdtranFruChildPortCorrelator extends AdtTrapsCorrProblemDefault {

	private Logger log = LoggerFactory.getLogger(AdtranFruChildPortCorrelator.class);

	public AdtranFruChildPortCorrelator() {
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
			if (CorrNames.Correlate_fru_with_childport_events.equals(corrName)) {
				if (a instanceof EnrichedAlarm) {
					EnrichedAlarm alarm = (EnrichedAlarm) a;
					log.trace("isMatchingTriggerAlarmCriteria trigget alarm = "
							+ alarm.getOriginatingManagedEntity());
					log.trace("Remote PPOrt info exists = "
							+ alarm.isRemotePPortInfoExists());
					if (alarm.isRemotePPortInfoExists()) {
						if("ADTRAN 5000 SERIES".equalsIgnoreCase(alarm.getDeviceType()) && "JUNIPER MX SERIES".equalsIgnoreCase(alarm.getRemoteDeviceType())) {
							matches = true;
						} else {
							log.trace("Device type is not ADTRAN 5000 SERIES or Remote Device Type is not JUNIPER MX SERIES"); 
						}
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
				if(CorrNames.Correlate_fru_with_childport_events.equals(corrName)) {
					if(alarm.getRemotePportInstanceName() != null && !(alarm.getRemotePportInstanceName().isEmpty())) {
						String[] cardslot = alarm.getRemotePportInstanceName().split("/");
						if(cardslot.length > 3) {
							problemEntities.add(cardslot[0] + "/" + cardslot[1] + "/" + cardslot[2]);
							problemEntities.add(cardslot[0] + "/" + cardslot[1]);
						}
						if(cardslot.length == 3) { 
							problemEntities.add(cardslot[0] + "/" + cardslot[1]);
						}		
					}
				} else {
				pbEntity = alarm.getRemotePportInstanceName(); 
				}
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
			if (CorrNames.Correlate_fru_with_childport_events.equals(corrName))
					 {
				if(group.getTrigger() instanceof EnrichedAlarm) {
					EnrichedAlarm enrichedAlarm = (EnrichedAlarm) group.getTrigger();
					String[] slotCard = alarm.getOriginatingManagedEntity().split(" ")[1].split("/");
					if(slotCard.length == 3) { 
						enrichedAlarm.setCustomFieldValue(GFPFields.INFO3, "Child=<Y> <AlertID><AlertKey>" + enrichedAlarm.getCustomFieldValue(GFPFields.ALERT_ID) + "</AlertKey><TimeStamp>" + enrichedAlarm.getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP) + "</TimeStamp></AlertID> DeviceName=" + slotCard[0] + "Slot=" + slotCard[1] + "Card=" + slotCard[2]);
					} else if(slotCard.length == 2) {
						enrichedAlarm.setCustomFieldValue(GFPFields.INFO3, "Child=<Y> <AlertID><AlertKey>" + enrichedAlarm.getCustomFieldValue(GFPFields.ALERT_ID) + "</AlertKey><TimeStamp>" + enrichedAlarm.getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP) + "</TimeStamp></AlertID> DeviceName=" + slotCard[0] + "Slot=" + slotCard[1]);  
					} 
					log.trace("whatToDoWhenSubAlarmIsAttachedToGroup one of the pport processing evenr keys ");     
					AdtranPportAlarmProcessor pportProcessor = new AdtranPportAlarmProcessor();
					EnrichedAdtranLinkDownAlarm enrichedAdtLinkDownAlarm = new EnrichedAdtranLinkDownAlarm(enrichedAlarm);
					pportProcessor.processAdtranPportAlarm(enrichedAdtLinkDownAlarm);      
				}
				log.trace("##### Updated INFO3 of Alarm : Alarm  " + group.getTrigger().getIdentifier() + "|{" + group.getTrigger().getCustomFieldValue(GFPFields.SEQNUMBER) + "} event key: " + group.getTrigger().getCustomFieldValue(GFPFields.EVENT_KEY) + " is Correlated by the Adtran correlation {" +corrName + "} and the alarm that triggered this correlation is " + alarm.getIdentifier() + "|{"+ alarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "} event key: " + alarm.getCustomFieldValue(GFPFields.EVENT_KEY));  
			} else {
			//alarm.setAboutToBeRetracted(true);
			//log.trace("##### Suppression : Alarm  " + alarm.getIdentifier() + "|{" + alarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "} event key: " + alarm.getCustomFieldValue(GFPFields.EVENT_KEY) + " is Suppressed by the Adtran correlation {" +corrName + "} and the alarm that triggered this suppression is " + group.getTrigger().getIdentifier() + "|{"+ group.getTrigger().getCustomFieldValue(GFPFields.SEQNUMBER) + "} event key: " + group.getTrigger().getCustomFieldValue(GFPFields.EVENT_KEY));  
			}
		//	getScenario().getSession().update(alarm);     
			log.trace("whatToDoWhenSubAlarmIsAttachedToGroup : setting alarm's = " 
					+ alarm.getIdentifier()   
					+ " Updated INFO3"); 
		}  
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenSubAlarmIsAttachedToGroup()");
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
