/**
 * This Problem is empty and ready to define methods to customize this problem
 */
package com.hp.uca.expert.vp.pd.problem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.SyslogAlarm;
import com.att.gfp.helper.GFPFields;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.vp.pd.core.ProblemDefault;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;
import com.hp.uca.expert.vp.pd.services.PD_Service_ProblemAlarm;


/**
 * @author DF
 * 
 */


public class JnxLacp extends ProblemDefault implements ProblemInterface {
	private Logger log = LoggerFactory.getLogger(JnxLacp.class);
	
	

	public JnxLacp() {
		super();
		setLog(LoggerFactory.getLogger(JnxLacp.class));
	}

	@Override
	public boolean isMatchingCandidateAlarmCriteria(Alarm a) throws Exception {
		SyslogAlarm ea = (SyslogAlarm) a;
		String eventKey = ea.getCustomFieldValue(GFPFields.EVENT_KEY);
		String targetName = null;
		Util.WDPool(ea, targetName, false, 0, getScenario());
		LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingCandidateAlarmCriteria() runs.");
		return true;
	}

	@Override
	public boolean isMatchingSubAlarmCriteria(Alarm a,Group group) throws Exception {
		boolean ret = true;
		SyslogAlarm ea = (SyslogAlarm) a;
		String eventKey = ea.getCustomFieldValue(GFPFields.EVENT_KEY);
		String targetName = null;
		Util.WDPool(ea, targetName, false, 0, getScenario());
		LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingSubAlarmCriteria() " + "[" + ret + "], group name = " + group.getName());
		return ret;
	}

	@Override
	public List<String> computeProblemEntity(Alarm a) throws Exception {
		/*
		 * Default behavior: get first level of managing entity
		 */
		String pbEntity = null;
		List<String> problemEntities = new ArrayList<String>();

		// instance only for the PE
		pbEntity = a.getOriginatingManagedEntity().split(" ")[1];

		problemEntities.add(pbEntity);

		// if this is the 50004/1/10 then we add the remote entity
		// this is for the A to Z correlation
		if(a.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50004/1/10")) {
			if(((SyslogAlarm) a).getRemotePportInstanceName() != null)
				problemEntities.add(((SyslogAlarm) a).getRemotePportInstanceName());
		}
		
		LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", computeProblemEntity() --- problemEntities=" + problemEntities);
		return problemEntities;
	}

	@Override
	public boolean isAllCriteriaForProblemAlarmCreation(Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			log.trace(
					"ENTER:  isAllCriteriaForProblemAlarmCreation() for group ["
							+ group.getTrigger().getIdentifier(), group.getName());
		}

		boolean ret = false;
		/*
		 * Checking that there are at least two alarms in the group before to
		 * create the Problem Alarm
		 */
		Collection<Alarm> alarmsInGroup = group.getAlarmList();
		int numberOfAlarmsInGroup = alarmsInGroup.size();

		if (log.isDebugEnabled()) {
			log.debug("numberOfAlarmsInGroup = " + numberOfAlarmsInGroup);
		}

		if (numberOfAlarmsInGroup > 1 && PD_Service_ProblemAlarm.isItTimeForProblemAlarmCreation(group)) {
			ret = true;
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "isAllCriteriaForProblemAlarmCreation()",
					String.valueOf(ret) + " -- " + group.getName());
		}
		return ret;
	}

	@Override
	public boolean isMatchingTriggerAlarmCriteria(Alarm a) throws Exception {
		boolean ret = false;
		SyslogAlarm alarm = (SyslogAlarm) a;	

		long after = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger();
		String watchdogDesc = "JnxLacp watch dog.";
		Util.setTriggerWatch((SyslogAlarm)alarm, SyslogAlarm.class, "simpleSendCallBack", after, watchdogDesc);
					
		LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingTriggerAlarmCriteria() " + "[" + ret + "]");
		return true;
	}
}
