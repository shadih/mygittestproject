/**
 * This Problem is empty and ready to define methods to customize this problem
 */
package com.hp.uca.expert.vp.pd.problem;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.data.ipagAlarm.Pri_Sec_Alarm;
import com.att.gfp.helper.GFPFields;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.vp.pd.core.PriSec_ProblemDefault;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;

public final class Ciena_Syslog_OAM_PriSec extends PriSec_ProblemDefault implements ProblemInterface {

	private Logger log = LoggerFactory.getLogger(Ciena_Syslog_OAM_PriSec.class);
	public Ciena_Syslog_OAM_PriSec() {
		super();

	}
	// Item 6b under Ciena OAM of Ciena Req from Gayathri. The requirement
	// was changed to primary/seconday: item 2c of page 10 on project 200876

	
	@Override
	public List<String> computeProblemEntity(Alarm a) throws Exception
	{

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "computeProblemEntity()");
		}

		List<String> problemEntities = new ArrayList<String>();
		Pri_Sec_Alarm alarm = (Pri_Sec_Alarm) a;

        String problemEntity = a.getOriginatingManagedEntity().split(" ")[1];
        problemEntities.add(problemEntity);
		
		String remotePportKey = alarm.getRemotePportInstanceName();
		if (remotePportKey != null && remotePportKey.length() > 0)
             		problemEntities.add(remotePportKey);

		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + alarm.getIdentifier() + ", computeProblemEntity() --- problemEntities=" + problemEntities);

		return problemEntities;
	} 

	@Override
	public boolean isMatchingTriggerAlarmCriteria(Alarm a) throws Exception {
		boolean ret = false;

		Pri_Sec_Alarm alarm = (Pri_Sec_Alarm) a;
		
		if ( alarm.getRemoteDeviceType() != null &&
				!alarm.getRemoteDeviceType().isEmpty() &&
				alarm.getRemoteDeviceType().equals("CIENA NTE"))
		{
			// this is what we use to determine how long to hold the trigger before we let it go
			long afterTrigger = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger();
			long ageLeft = alarm.TimeRemaining(afterTrigger); 
			long holdTime = ageLeft + maxSAge; 

			log.trace("The time of the trigger callback is " + holdTime);
			
			String watchdogDesc = "Ciena_OAM PriSec watch dog.";
			Util.setTriggerWatch(alarm, Pri_Sec_Alarm.class, "simpleSendCallBack", holdTime, watchdogDesc);
			ret = true;
		}
					
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingTriggerAlarmCriteria() " + "[" + ret + "]");
		return ret;
	}
		 
	@Override
	public void whatToDoWhenSubAlarmIsAttachedToGroup(Alarm alarm, Group group) throws Exception
	{
		if (log.isTraceEnabled())
			LogHelper.enter(log, "alarm = " + alarm.getIdentifier() + ", group = " + group.getName() + ", group # = " + group.getNumber() + ", whatToDoWhenSubAlarmIsAttachedToGroup()");

		Alarm ta = group.getTrigger();

		if(alarm != ta) {
			// secondary
			alarm.setCustomFieldValue(GFPFields.SECONDARY_ALERT_ID, ta.getCustomFieldValue(GFPFields.ALERT_ID));
			alarm.setCustomFieldValue(GFPFields.SECONDARYTIMESTAMP, ta.getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP));
			alarm.setCustomFieldValue(GFPFields.HASSECONDARY,"false");

			log.info("Setting this CIENA OAM alarm Secondary to ActionSyslog_Adjacency_Lost (alert-id = " + alarm.getIdentifier() + 
					" & sequence-number = " + alarm.getCustomFieldValue(GFPFields.SEQNUMBER) + ")");
			
			// primary
			ta.setCustomFieldValue(GFPFields.SECONDARY_ALERT_ID, "1");
			ta.setCustomFieldValue(GFPFields.SECONDARYTIMESTAMP, "0");
			ta.setCustomFieldValue(GFPFields.HASSECONDARY,"true");

			log.info("Setting this ActionSyslog_Adjacency_Lost as primary to CIENA OAM (alert-id = " + ta.getIdentifier() + 
					" & sequence-number = " + ta.getCustomFieldValue(GFPFields.SEQNUMBER) + ") .");

			
			Util.WhereToSendAndSend((EnrichedAlarm) ta);
			Util.WhereToSendAndSend((EnrichedAlarm) alarm);
		}			

		if (log.isTraceEnabled())
			LogHelper.exit(log, "whatToDoWhenSubAlarmIsAttachedToGroup()");

	
	}
}
