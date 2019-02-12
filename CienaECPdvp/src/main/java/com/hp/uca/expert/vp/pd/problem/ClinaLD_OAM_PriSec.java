/**
 * This Problem is empty and ready to define methods to customize this problem
 */
package com.hp.uca.expert.vp.pd.problem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.ciena.cienaPD.CienaAlarm;
import com.att.gfp.ciena.cienaPD.Util;
import com.att.gfp.helper.GFPFields;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.vp.pd.core.PD_AlarmRecognition;
import com.hp.uca.expert.vp.pd.core.ProblemDefault;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;
import com.hp.uca.expert.vp.pd.services.PD_Service_Util;

//
public final class ClinaLD_OAM_PriSec extends ProblemDefault implements ProblemInterface 
{

	private Logger log = LoggerFactory.getLogger(ClinaLD_OAM_PriSec.class);
	public ClinaLD_OAM_PriSec() {
		super();

	}

	 @Override
	public List<String> computeProblemEntity(Alarm a) throws Exception
	{
		List<String> problemEntities = new ArrayList<String>();
		CienaAlarm alarm = (CienaAlarm) a;

		problemEntities.add(alarm.getInstance());

		log.info("alarm = " + a.getIdentifier() + ", computeProblemEntity() --- problemEntities=" + problemEntities);
		return problemEntities;
	 } 

	@Override
	public boolean isMatchingCandidateAlarmCriteria(Alarm a) throws Exception {
		Util.sendNonTrigger((CienaAlarm) a);
		log.info("alarm = " + a.getIdentifier() + ", isMatchingCandidateAlarmCriteria() runs.");
		return true;
	}

	@Override
	public boolean isMatchingTriggerAlarmCriteria(Alarm a) throws Exception {
		boolean ret = false;
		CienaAlarm alarm = (CienaAlarm)a;
		if (!alarm.isSuppressed())
		{
			long after = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger();
			int aging = alarm.getAging();
			long wdtimer = (after>aging)? after: aging;
			String watchdogDesc = "CienaOAM watch dog.";
			Util.setTriggerWatch(alarm, CienaAlarm.class, "simpleSendCallBack", wdtimer, watchdogDesc);
			ret = true;
		}
		log.info("alarm = " + a.getIdentifier() + ", isMatchingTriggerAlarmCriteria() " + "[" + ret + "]");

		return ret;
	}

	@Override
	public boolean isMatchingSubAlarmCriteria(Alarm a, Group group) throws Exception {
		boolean ret = true;
                Alarm trigger = group.getTrigger();
                CienaAlarm alarm = (CienaAlarm) a;
		Util.sendNonTrigger(alarm);

		log.info("trigger identifier = " + trigger.getIdentifier()+
", trigger alarmRaisedTime = " + trigger.getAlarmRaisedTime()+
", trigger be_time_stamp = " + trigger.getCustomFieldValue("be_time_stamp")+
", alarm identifier = " + alarm.getIdentifier()+
", alarm alarmRaisedTime = " + alarm.getAlarmRaisedTime()+
", alarm be_time_stamp = " + alarm.getCustomFieldValue("be_time_stamp"));

		log.info("alarm = " + a.getIdentifier() + ", isMatchingSubAlarmCriteria() " + "[" + ret + "], group name = " + group.getName());
		return ret;
	}

	@Override
	public void whatToDoWhenSubAlarmIsAttachedToGroup(Alarm a, Group group) throws Exception {
                CienaAlarm trigger = (CienaAlarm) group.getTrigger();
                CienaAlarm alarm = (CienaAlarm) a;

		log.info("trigger = " + trigger.getIdentifier() + "alarm = " + a.getIdentifier() + ", group = " + group.getName() + ", group member # = " + group.getNumber() + ", whatToDoWhenSubAlarmIsAttachedToGroup()");
		if (group.getNumber() > 1)
		{
			// if primary is suppressed ==> suppress secondary
			if (alarm.isSuppressed())
			{
				log.info("Secondary alarm = " + trigger.getIdentifier() + " is suppressed as the primary alarm = " + alarm.getIdentifier() + " is suppressed.");
				trigger.setSuppressed(true);
			}
			else
			{
				log.info("CienaLD = " + alarm.getIdentifier() + " is the primary. OAM = " + trigger.getIdentifier() + " is the secondary.");
				trigger.setCustomFieldValue(GFPFields.SECONDARY_ALERT_ID, alarm.getCustomFieldValue(GFPFields.ALERT_ID));
				trigger.setCustomFieldValue(GFPFields.SECONDARYTIMESTAMP, alarm.getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP));
	        		alarm.setCustomFieldValue(GFPFields.SECONDARY_ALERT_ID, "1");
	        		alarm.setCustomFieldValue(GFPFields.SECONDARYTIMESTAMP, "0");
			}
		}
	}	
}
