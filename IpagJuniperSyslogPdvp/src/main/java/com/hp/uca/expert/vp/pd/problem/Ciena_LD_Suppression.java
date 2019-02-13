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
import com.att.gfp.data.ipagAlarm.SyslogAlarm;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.vp.pd.core.JuniperSyslog_ProblemDefault;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;
import com.hp.uca.expert.vp.pd.services.PD_Service_Group;

public final class Ciena_LD_Suppression extends JuniperSyslog_ProblemDefault implements ProblemInterface {

	private Logger log = LoggerFactory.getLogger(Ciena_LD_Suppression.class);
	public Ciena_LD_Suppression() {
		super();

	}

	// item 2a(2b and 2a are same) of page 9 on project 200876
	@Override
	public boolean isMatchingCandidateAlarmCriteria(Alarm a) throws Exception {
		SyslogAlarm ea = (SyslogAlarm) a;
		String eventKey = ea.getCustomFieldValue(GFPFields.EVENT_KEY);
		String targetName = null;
		if (eventKey.equals("50002/100/21"))
			targetName = "NTDTICKET_CORRELATION";
		Util.WDPool(ea, targetName, false, 0, getScenario());
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingCandidateAlarmCriteria() runs.");
		return true;
	}

	@Override
	public boolean isMatchingSubAlarmCriteria(Alarm a,Group group) throws Exception {
		boolean ret = true;
		SyslogAlarm ea = (SyslogAlarm) a;
		String eventKey = ea.getCustomFieldValue(GFPFields.EVENT_KEY);
		String targetName = null;
		if (eventKey.equals("50002/100/21"))
			targetName = "NTDTICKET_CORRELATION";
		Util.WDPool(ea, targetName, false, 0, getScenario());
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingSubAlarmCriteria() " + "[" + ret + "], group name = " + group.getName());
		return ret;
	}
	
	@Override
	public List<String> computeProblemEntity(Alarm a) throws Exception
	{

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "computeProblemEntity()");
		}

		List<String> problemEntities = new ArrayList<String>();
		SyslogAlarm alarm = (SyslogAlarm) a;

		String eventKey = alarm.getCustomFieldValue(GFPFields.EVENT_KEY);
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

		SyslogAlarm alarm = (SyslogAlarm) a;	
		if ("CIENA NTE".equals(alarm.getDeviceType()))
		{
			long after = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger();
			String watchdogDesc = "Ciena LD watch dog.";
			Util.setTriggerWatch((SyslogAlarm)a, SyslogAlarm.class, "simpleSendCallBack", after, watchdogDesc);
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
		if (group.getNumber() > 1)
		{
			SyslogAlarm trigger = (SyslogAlarm) group.getTrigger();
			log.info("alarm = " + trigger.getIdentifier() + " is suppressed.");
			trigger.setSuppressed(true);
		}
	}	
}
