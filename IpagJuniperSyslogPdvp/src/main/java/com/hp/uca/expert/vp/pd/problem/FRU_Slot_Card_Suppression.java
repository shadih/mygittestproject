/**
 * This Problem is empty and ready to define methods to customize this problem
 */
package com.hp.uca.expert.vp.pd.problem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
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
import com.hp.uca.expert.vp.pd.services.PD_Service_ProblemAlarm;

/**
 * @author MASSE
 * 
 */
public final class FRU_Slot_Card_Suppression extends ProblemDefault implements
		ProblemInterface {
	private static Logger log = LoggerFactory.getLogger(FRU_Slot_Card_Suppression.class);

	public FRU_Slot_Card_Suppression() {
		super();
	}

	// page 39 on project 263779
	@Override
	public boolean isMatchingCandidateAlarmCriteria(Alarm a) throws Exception {
		SyslogAlarm ca = (SyslogAlarm) a;
		String eventKey = ca.getCustomFieldValue("EventKey");
		String targetName = null;
		Util.WDPool(ca, targetName, false, 0, getScenario());
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingCandidateAlarmCriteria() runs.");
		return true;
	}

	@Override
	public List<String> computeProblemEntity(Alarm a) throws Exception {
		List<String> problemEntities = new ArrayList<String>();
		SyslogAlarm sa = (SyslogAlarm)a;
		String eventKey = sa.getCustomFieldValue("EventKey");
		if(eventKey.equals("50003/100/12") || eventKey.equals("50003/100/13") || eventKey.equals("50003/100/14") || eventKey.equals("50003/100/15") || eventKey.equals("50003/100/16"))
		{
			HashSet<String> vrfset = sa.getVRFSet();
			if (vrfset.size() == 0)
				problemEntities.add(sa.getIdentifier());
			else
			{
				Iterator i = vrfset.iterator();
				while(i.hasNext())
				{
					String vrf = (String)i.next();
					if (vrf.length() > 0)
						problemEntities.add(vrf);
				}
			}
		}
		else
		{
			String vrf = (String) a.getCustomFieldValue("vrf-name");
			if (log.isTraceEnabled())
				log.trace("computeProblemEntity runs.  Identifier = " + a.getIdentifier() + ", vrf name = " + vrf);
			if (vrf == null || vrf.equals(""))
				vrf = a.getIdentifier();
			problemEntities.add(vrf);
		}
		if (problemEntities.size() == 0)
			problemEntities.add(sa.getIdentifier());

		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", computeProblemEntity() --- problemEntities=" + problemEntities);

		return problemEntities;
	} 

	@Override
	public boolean isMatchingSubAlarmCriteria(Alarm a, Group group) throws Exception {
		boolean ret = true;
		SyslogAlarm sa = (SyslogAlarm)a;
		String targetName = null;
		Util.WDPool(sa, targetName, false, 0, getScenario());
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingSubAlarmCriteria() " + "[" + ret + "], group name = " + group.getName());
		return ret;
	}

	@Override
	public boolean isMatchingTriggerAlarmCriteria(Alarm a) throws Exception 
	{
		boolean ret = true;
		long after = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger();
		String watchdogDesc = "FRU watch dog.";
		Util.setTriggerWatch((SyslogAlarm)a, SyslogAlarm.class, "FRUColdStartCallBack", after, watchdogDesc);
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingTriggerAlarmCriteria() " + "[" + ret + "]");
		return ret;
	}

	@Override
	public void whatToDoWhenSubAlarmIsAttachedToGroup(Alarm alarm, Group group) throws Exception {
		if (log.isTraceEnabled())
			LogHelper.enter(log, "alarm = " + alarm.getIdentifier() + ", group = " + group.getName() + ", group # = " + group.getNumber() + ", whatToDoWhenSubAlarmIsAttachedToGroup()");
		if (group.getNumber() > 1)
		{
			SyslogAlarm trigger = (SyslogAlarm) group.getTrigger();
			log.info("alarm = " + trigger.getIdentifier() + " is suppressed.");
			trigger.setSuppressed(true);
		}
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
}
