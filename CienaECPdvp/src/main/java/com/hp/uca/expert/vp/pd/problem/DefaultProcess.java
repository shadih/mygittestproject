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
// Process all events that don't meet the filter criteria of problem groups
// other than DefaultProcess group.  Otherwise, they will be filtered out and
// never get delivered to this Pdvp, such as pass through alarm, 
// they are sent by WD. 
// 
// health traps don't belong to DefaultProcess group as helath traps are sent
// without delay, but alarms in DefaultProcess group are sent by WD which has
// at least 2 sec delay
// 
public final class DefaultProcess extends ProblemDefault implements ProblemInterface 
{
	private Logger log = LoggerFactory.getLogger(DefaultProcess.class);
	public DefaultProcess() {
		super();

	}

	 @Override
	public List<String> computeProblemEntity(Alarm a) throws Exception
	{
		List<String> problemEntities = new ArrayList<String>();
		CienaAlarm alarm = (CienaAlarm) a;

		problemEntities.add(alarm.getIdentifier()+"_"+alarm.getCustomFieldValue("be_time_stamp"));

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
	// below doesn't get called as they are tagged as Subalarm in this 
	// problem group. there is NO Trigger alarm in this problem group
	public boolean isMatchingTriggerAlarmCriteria(Alarm a) throws Exception {
		boolean ret = false;
		CienaAlarm alarm = (CienaAlarm) a;
		log.info("alarm = " + a.getIdentifier() + ", isMatchingTriggerAlarmCriteria() " + "[" + ret + "]");

		return ret;
	}

	@Override
	// below doesn't get called as they are tagged as Subalarm in this 
	// problem group. there is NO Trigger alarm in this problem group
	// ie, they are candiate alarm
	public boolean isMatchingSubAlarmCriteria(Alarm a, Group group) throws Exception {
		boolean ret = false;

                Alarm trigger = group.getTrigger();
                CienaAlarm alarm = (CienaAlarm) a;

		log.info("trigger identifier = " + trigger.getIdentifier()+
", trigger alarmRaisedTime = " + trigger.getAlarmRaisedTime()+
", trigger be_time_stamp = " + trigger.getCustomFieldValue("be_time_stamp")+
", alarm identifier = " + alarm.getIdentifier()+
", alarm alarmRaisedTime = " + alarm.getAlarmRaisedTime()+
", alarm be_time_stamp = " + alarm.getCustomFieldValue("be_time_stamp"));

		log.info("alarm = " + a.getIdentifier() + ", isMatchingSubAlarmCriteria() " + "[" + ret + "], group name = " + group.getName());
		return ret;
	}

}
