/**
 * This Problem is empty and ready to define methods to customize this problem
 */
package com.hp.uca.expert.vp.pd.problem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.ciena.cienaPD.CandidateAlarmProc;
import com.att.gfp.ciena.cienaPD.CienaAlarm;
import com.att.gfp.ciena.cienaPD.Util;
import com.att.gfp.data.ipagAlarm.AlarmDelegationType;

import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.vp.pd.core.ProblemDefault;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;

/**
 * @author MASSE
 * 
 */
public final class MptProcess extends ProblemDefault implements
		ProblemInterface {
	private static Logger log = LoggerFactory.getLogger(MptProcess.class);

	public MptProcess() {
		super();
	}

	@Override
	public boolean isMatchingCandidateAlarmCriteria(Alarm a) throws Exception {
		CienaAlarm ca = (CienaAlarm) a;
		String eventKey = ca.getCustomFieldValue("EventKey");
		String targetName = null;	// default to AM for adtran
/*
		if (eventKey.contains("50002"))
			targetName = "JUNIPER_LINKDOWN";
*/
		Util.WDPool(getScenario(), ca, targetName, false);
		LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingCandidateAlarmCriteria() runs.");
		return true;
	}

	@Override
	public List<String> computeProblemEntity(Alarm a) throws Exception {
		String vrf = (String) a.getCustomFieldValue("vrf-name");
		log.info("computeProblemEntity runs.  Identifier = " + a.getIdentifier() + ", vrf name = " + vrf);
		List<String> problemEntities = new ArrayList<String>();
		if(vrf == null || vrf.isEmpty())
			vrf = a.getIdentifier();
		problemEntities.add(vrf);
		log.info("alarm = " + a.getIdentifier() + ", computeProblemEntity() --- problemEntities=" + problemEntities);
		return problemEntities;
	} 
		 
	@Override
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

		if (alarm.isCFMPtpMptAlarm() == false)
			ret = false;
		else
			ret = true;
		LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingSubAlarmCriteria() " + "[" + ret + "], group name = " + group.getName());
		return ret;
	}
		 
	@Override
	public boolean isMatchingTriggerAlarmCriteria(Alarm a) throws Exception {
		boolean ret = false;

		CienaAlarm alarm = (CienaAlarm) a;	
		if (alarm.isCFMPtpMptAlarm() == false || alarm.isSuppressed())
		{
			log.info("isCFMPtpMptAlarm = " + alarm.isCFMPtpMptAlarm() + ", isSuppressed = " + alarm.isSuppressed());
			ret = false;
		}
		else
		{
			long after = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger();
			String watchdogDesc = "CFMMpt watch dog.";
			Util.setTriggerWatch(alarm, CienaAlarm.class, "cfmMptCallBack", after, watchdogDesc);
			ret = true;
		}
		LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingTriggerAlarmCriteria() " + "[" + ret + "]");
		return ret;
	}

	@Override
	public void whatToDoWhenSubAlarmIsAttachedToGroup(Alarm a, Group group) throws Exception {
                Alarm trigger = group.getTrigger();
                CienaAlarm alarm = (CienaAlarm) a;
		LogHelper.enter(log, "trigger = " + trigger.getIdentifier() + "alarm = " + a.getIdentifier() + ", group = " + group.getName() + ", group member # = " + group.getNumber() + ", whatToDoWhenSubAlarmIsAttachedToGroup()");
		//
		// alarm A (alarmRaisedTime = 10), alarm B (alarmRaisedTime = 8)
		// A arrives	==> create a group GA containing A
		// B arrives (it cannot be in GA as 8 < 10)	
		//		==> create a group GB containing A, B
		// ==> two synthetic alarms will be sent from GA and GB after 
		//	3 min ==> wrong
		// solution: set isSubAlarm of A to true in GB processing
		// GA will be dropped as isSubAlarm of A (trigger) is true
		//
		if (alarm == trigger)
			alarm.setGroupPtpMpt(group);
		else
			alarm.setIsSubAlarm(true);
/*
                for (Alarm ax : group.getAlarmList())
                {
                        boolean isTrig = false;
                        if (ax == trigger)
                                isTrig = true;
                        log.info("PTP identifier = " + ax.getIdentifier() + ", alarm time = " + ax.getAlarmRaisedTime() + ", ManagedEntity = " + ax.getOriginatingManagedEntity() + ", isTrig = " + isTrig);
                }
*/
	}	
}
