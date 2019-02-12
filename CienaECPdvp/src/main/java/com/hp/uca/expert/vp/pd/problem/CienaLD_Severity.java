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
public final class CienaLD_Severity extends ProblemDefault implements ProblemInterface 
{

	private Logger log = LoggerFactory.getLogger(CienaLD_Severity.class);
	public CienaLD_Severity() {
		super();

	}

	 @Override
	public List<String> computeProblemEntity(Alarm a) throws Exception
	{
		List<String> problemEntities = new ArrayList<String>();
		CienaAlarm alarm = (CienaAlarm) a;
		String eventKey = alarm.getCustomFieldValue(GFPFields.EVENT_KEY);
		problemEntities.add(eventKey);

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
		CienaAlarm alarm = (CienaAlarm) a;
		if (alarm.getUpdateSeverity() == true)
		{
			long after = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger();
			int aging = alarm.getAging();
			long wdtimer = (after>aging)? after: aging;
			String watchdogDesc = "CienaLD_Severity watch dog.";
			Util.setTriggerWatch(alarm, CienaAlarm.class, "simpleSendCallBack", wdtimer, watchdogDesc);
			ret = true;
		}
		log.info("alarm = " + a.getIdentifier() + ", isMatchingTriggerAlarmCriteria() " + "[" + ret + "]");

		return ret;
	}

	@Override
	public boolean isMatchingSubAlarmCriteria(Alarm a, Group group) throws Exception {
		boolean ret = false;

                Alarm trigger = group.getTrigger();
                CienaAlarm alarm = (CienaAlarm) a;
		Util.sendNonTrigger(alarm);
		ret = true;
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
		if (group.getNumber() > 1 && !alarm.getInstance().equals(trigger.getInstance()))
		{
			if (trigger.getRemoteDeviceIpaddr().equals(alarm.getSm_element()) &&
			!trigger.getRemote_pport_key().equals(alarm.getInstance()) && 
			("Y".equals(alarm.getSlavlan_nmvlan()) || "CIENA EMUX".equals(alarm.getRemoteDeviceType())) &&
			"Y".equals(alarm.getMultiNni()))
			{
				log.info("Update severity to 0 for alarm = " + trigger.getIdentifier());
				trigger.setSeverity(0);
			}
		}
	}	
}
