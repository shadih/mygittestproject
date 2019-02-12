/**
 * This Problem is empty and ready to define methods to customize this problem
 */
package com.hp.uca.expert.vp.pd.problem;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.data.ipagAlarm.EnrichedJuniperAlarm;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.vp.pd.core.JuniperLinkDown_ProblemDefault;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;
import com.hp.uca.expert.vp.pd.services.PD_Service_Group;

/**
 * @author df
 * 
 */
public final class A_Z_AlarmProcessing extends JuniperLinkDown_ProblemDefault implements
		ProblemInterface {

	private static final String LINKDOWN_KEY = "50003/100/1";
	
	private Logger log = LoggerFactory.getLogger(A_Z_AlarmProcessing.class);

	public A_Z_AlarmProcessing() {
		super();

	}

	
	@Override
	public boolean isMatchingTriggerAlarmCriteria(Alarm a) throws Exception {
		boolean ret = false;

		if ( a instanceof EnrichedJuniperAlarm)  {
			EnrichedJuniperAlarm alarm = (EnrichedJuniperAlarm) a;	

			if(alarm.getRemoteDeviceType() != null && alarm.getDeviceType() != null) {
				//				if((alarm.getRemoteDeviceType().equals("CIENA NTE")) || (alarm.getRemoteDeviceType().contains("CISCO")) ||
				//						(alarm.getDeviceType().contains("CISCO") && alarm.getRemoteDeviceType().equals("JUNIPER MX SERIES")) ||
				//						(alarm.getDeviceType().equals("JUNIPER MX SERIES") && alarm.getRemoteDeviceType().contains("CISCO")) ||
				//						(alarm.getDeviceType().equals("JUNIPER MX SERIES") && alarm.getRemoteDeviceType().equals("JUNIPER MX SERIES")) ||
				//						(alarm.getDeviceType().equals("VR1") && alarm.getRemoteDeviceType().equals("ME3")) ||
				//						(alarm.getDeviceType().equals("VR1") && (alarm.getRemoteDeviceType().contains("hu4") || alarm.getRemoteDeviceType().contains("nm9"))))
				//				{
				//					
				if (    alarm.getRemoteDeviceType().contains("CISCO") ||
						alarm.getRemoteDeviceType().equals("JUNIPER MX SERIES") ||
						alarm.getRemoteDeviceType().equals("VR1") ||
						alarm.getRemoteDeviceType().equals("ME3") ||
						alarm.getRemoteDeviceType().contains("hu4") || 
						alarm.getRemoteDeviceType().contains("nm9") ||
						alarm.getRemoteDeviceType().equals("NV1") ||
						alarm.getRemoteDeviceType().equals("NV2") 
					)
				{		

					setTriggerWatch(alarm);
					ret = true;
				}  
			}
			
			// sending the alarm here if not a valid trigger
			if(!ret) 
				Util.WDPool(alarm, 0, getScenario());
			
		}

		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingTriggerAlarmCriteria() " + "[" + ret + "]");

		return ret;

	}
		 
	@Override
	public List<String> computeProblemEntity(Alarm a) throws Exception {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "computeProblemEntity()");
		}

		List<String> problemEntities = new ArrayList<String>();
		EnrichedJuniperAlarm alarm = (EnrichedJuniperAlarm) a;

		String problemEntity;

		// the problem entity is the remote port to the juniper port that has the 
		// link down alarm
		if(alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(LINKDOWN_KEY) || "50003/100/23".equals(alarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
			// this is the link down alarm so use the remote port
			// problemEntity = alarm.getRemotePortKey();
			problemEntity = alarm.getRemotePportInstanceName();
			if (problemEntity != null)
				problemEntities.add(problemEntity); 
		}
		// add the instance
		problemEntity = a.getOriginatingManagedEntity().split(" ")[1];
		problemEntities.add(problemEntity);

		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", computeProblemEntity() --- problemEntities=" + problemEntities);

		return problemEntities;
	} 

	
	// This method is used to send alarms that have been attached to a group and
	// here is where I'm putting the suppression of the link down alarm based on the 
	// criteria listed in the requirements.   
	@Override
	public void whatToDoWhenSubAlarmIsAttachedToGroup(Alarm alarm, Group group) throws Exception {
		if (log.isTraceEnabled())
			LogHelper.enter(log, "alarm = " + alarm.getIdentifier() + ", group = " + group.getName() + ", group # = " + group.getNumber() + ", whatToDoWhenSubAlarmIsAttachedToGroup()");

		boolean isTriggerForOtherGroup = false;
		EnrichedAlarm trigger = (EnrichedAlarm) group.getTrigger();
		EnrichedAlarm eAlarm = (EnrichedAlarm) alarm;

		// is this subalarm a trigger for any group?   If it is then we don't want
		// to send the alarm here.   We will wait till all the triggers have been
		// evaluated then send it (watch callback).
		for (Group possibleGroup : PD_Service_Group.getGroupsOfAnAlarm(alarm, null)) {
			if(possibleGroup.getTrigger() == alarm) {
				isTriggerForOtherGroup = true;
				break;
			}
		}
	
		// test to see if this is the trigger.  If the link down alarm is to be
		// suppressed or cascade the alarm
		if(alarm != trigger && !isTriggerForOtherGroup) {			
			Util.whereToSendThenSend(eAlarm);
		}


				
				
//		if (alarm != trigger && ((trigger.getRemoteDeviceType().contains("CISCO") &&
//				eAlarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50003/100/23"))
//				||
//				(trigger.getDeviceType().contains("CISCO") && trigger.getRemoteDeviceType().equals("JUNIPER MX SERIES") &&
//						eAlarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50003/100/1"))
//						|| 
//				(trigger.getDeviceType().equals("JUNIPER MX SERIES") && trigger.getRemoteDeviceType().contains("CISCO") &&
//						eAlarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50003/100/23"))
//						|| 
//				(trigger.getDeviceType().equals("JUNIPER MX SERIES") && 
//						trigger.getRemoteDeviceType().equals("JUNIPER MX SERIES") && 
//						eAlarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50003/100/1"))
//						||
//						(trigger.getDeviceType().equals("VR1") && 
//								trigger.getRemoteDeviceType().equals("ME3") &&
//								eAlarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50003/100/1"))
//								||
//								(trigger.getDeviceType().equals("VR1") && 
//										(trigger.getRemoteDeviceType().contains("hu4") || 
//												trigger.getRemoteDeviceType().contains("nm9")) &&
//												eAlarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50003/100/1")))) {
		
		
		if ( alarm != trigger && ( trigger.getRemoteDeviceType().contains("CISCO") ||
				trigger.getRemoteDeviceType().equals("JUNIPER MX SERIES") ||
				trigger.getRemoteDeviceType().equals("VR1") ||
				trigger.getRemoteDeviceType().equals("ME3") ||
				trigger.getRemoteDeviceType().contains("hu4") || 
				trigger.getRemoteDeviceType().contains("nm9") ||
				trigger.getRemoteDeviceType().equals("NV1") ||
				trigger.getRemoteDeviceType().equals("NV2") )) {

			log.info("Alarm " + trigger.getIdentifier() + " sequence:" + trigger.getCustomFieldValue(GFPFields.SEQNUMBER)
					+ " has been suppressed by A_Z_AlarmProcessing and the alarm that triggered this suppression is " + alarm.getIdentifier() + " sequence:" + alarm.getCustomFieldValue(GFPFields.SEQNUMBER));

			trigger.setSuppressed(true); 
		}
 
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + alarm.getIdentifier() + ", group = " + group.getName() + ", group # = " + group.getNumber() + ", whatToDoWhenSubAlarmIsAttachedToGroup()");
	}	
}
