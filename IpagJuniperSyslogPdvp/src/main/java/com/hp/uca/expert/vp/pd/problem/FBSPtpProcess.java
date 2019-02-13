/**
 * This Problem is empty and ready to define methods to customize this problem
 */
package com.hp.uca.expert.vp.pd.problem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.lang.reflect.Method;
import com.hp.uca.common.callback.Callback;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.scenario.ScenarioThreadLocal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.SyslogAlarm;
import com.hp.uca.common.trace.LogHelper;

import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.vp.pd.core.ProblemDefault;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;

/**
 * @author MASSE
 * 
 */
public final class FBSPtpProcess extends ProblemDefault implements
		ProblemInterface {
	private static Logger log = LoggerFactory.getLogger(FBSPtpProcess.class);

	public FBSPtpProcess() {
		super();
	}

	// page 51 on project 257826
	@Override
	public boolean isMatchingCandidateAlarmCriteria(Alarm a) throws Exception {
		SyslogAlarm ca = (SyslogAlarm) a;
		// String eventKey = ca.getCustomFieldValue("EventKey");
		// if alarms arrive at this problem group and don't belong to
		// any group ==> its isPtpMpt MUST be false ==> send it to AM
		String targetName = null;
		Util.WDPool(ca, targetName, false, 0, getScenario());
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingCandidateAlarmCriteria() runs.");
		return true;
	}

	@Override
	public List<String> computeProblemEntity(Alarm a) throws Exception {
		SyslogAlarm alarm = (SyslogAlarm) a;
		String vrf = (String) a.getCustomFieldValue("vrf-name");
		if (log.isTraceEnabled())
			log.trace("computeProblemEntity runs.  Identifier = " + a.getIdentifier() + ", vrf name = " + vrf);
		List<String> problemEntities = new ArrayList<String>();
		if(vrf == null || vrf.isEmpty())
			vrf = a.getIdentifier();
		else if (alarm.getIsFBSPtp() == true && !vrf.contains("FBS"))
			// vrf doesn't contain FBS with roduct type being FBS
			vrf = vrf+":FBS";
		problemEntities.add(vrf);
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", computeProblemEntity() --- problemEntities=" + problemEntities);
		return problemEntities;
	} 

	@Override
	public boolean isMatchingSubAlarmCriteria(Alarm a, Group group) throws Exception {
		boolean ret = true;

		if (log.isTraceEnabled())
			log.trace("isMatchingSubAlarmCriteria runs.");
                Alarm trigger = group.getTrigger();
		SyslogAlarm alarm = (SyslogAlarm) a;	
		if (log.isTraceEnabled())
			log.trace("trigger identifier = " + trigger.getIdentifier()+
", trigger alarmRaisedTime = " + trigger.getAlarmRaisedTime()+
", trigger be_time_stamp = " + trigger.getCustomFieldValue("be_time_stamp")+
", alarm identifier = " + alarm.getIdentifier()+
", alarm alarmRaisedTime = " + alarm.getAlarmRaisedTime()+
", alarm be_time_stamp = " + alarm.getCustomFieldValue("be_time_stamp"));

		if (alarm.getIsFBSPtp() == false || alarm.isSuppressed())
		{
			log.info("IsFBSPtp = " + alarm.getIsFBSPtp() + ", isSuppressed = " + alarm.isSuppressed());
			ret = false;
		}
		else
		{
			// don't suppress it here as return true doesn't mean
			// alarm 'a' will be in this group. UCA will check
			// correlation window to see if 'a' will be in this 
			// group.  it is a bad design in UCA.  HP should check
			// the correlation window of before calling this method.
			ret = true;
		}
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingSubAlarmCriteria() " + "[" + ret + "], group name = " + group.getName());
		return ret;
	}

	@Override
	public boolean isMatchingTriggerAlarmCriteria(Alarm a) throws Exception 
	{
		boolean ret = false;

		SyslogAlarm alarm = (SyslogAlarm) a;	
		// when an alarm (tagged as trigger and subalarm) arrives
		// isMatchingSubAlarmCriteria gets called, which can suppress
		// it(see above) ==> it cannot be a trigger
		//
		// it can be suppressed by ColdStart
		if (alarm.getIsFBSPtp() == false || alarm.isSuppressed())
		{
			log.info("IsFBSPtp = " + alarm.getIsFBSPtp() + ", isSuppressed = " + alarm.isSuppressed());
			ret = false;
		}
		else
		{

			long after = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger();
			String watchdogDesc = "FBSPtp watch dog.";
			Util.setTriggerWatch(alarm, SyslogAlarm.class, "fbsPtpCallBack", after, watchdogDesc);
			ret = true;
		}
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingTriggerAlarmCriteria() " + "[" + ret + "]");
		return ret;
	}

	@Override
	public void whatToDoWhenSubAlarmIsAttachedToGroup(Alarm a, Group group) throws Exception {
		Alarm trigger = group.getTrigger();
		SyslogAlarm alarm = (SyslogAlarm) a;
		if (log.isTraceEnabled())
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
			alarm.setGroupFBS(group);
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
