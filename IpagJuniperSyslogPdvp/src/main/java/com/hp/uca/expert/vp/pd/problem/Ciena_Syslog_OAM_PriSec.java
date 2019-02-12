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

public final class Ciena_Syslog_OAM_PriSec extends JuniperSyslog_ProblemDefault implements ProblemInterface {

	private Logger log = LoggerFactory.getLogger(Ciena_Syslog_OAM_PriSec.class);
	public Ciena_Syslog_OAM_PriSec() {
		super();

	}
	// Item 6b under Ciena OAM of Ciena Req from Gayathri. The requirement
	// was changed to primary/seconday: item 2c of page 10 on project 200876
	@Override
	public boolean isMatchingCandidateAlarmCriteria(Alarm a) throws Exception {
		SyslogAlarm ea = (SyslogAlarm) a;
		String eventKey = ea.getCustomFieldValue(GFPFields.EVENT_KEY);
		String targetName = null;
		int extrawait = 5000;
		Util.WDPool(ea, targetName, false, extrawait, getScenario());
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingCandidateAlarmCriteria() runs.");
		return true;
	}

	@Override
	public boolean isMatchingSubAlarmCriteria(Alarm a,Group group) throws Exception {
		boolean ret = false;
		SyslogAlarm ea = (SyslogAlarm) a;
		String eventKey = ea.getCustomFieldValue(GFPFields.EVENT_KEY);
		if (eventKey.equals("50004/1/10") || (eventKey.equals("50002/100/19") && "CIENA NTE".equals(ea.getDeviceType())))
		{
			String targetName = null;
			int extrawait = 5000;
			Util.WDPool(ea, targetName, false, extrawait, getScenario());
			ret = true;
		}
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
		String eventKey = alarm.getCustomFieldValue(GFPFields.EVENT_KEY);
		if (eventKey.equals("50004/1/10") || (eventKey.equals("50002/100/19") && "CIENA NTE".equals(alarm.getDeviceType())))
		{
			long after = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger();
			String watchdogDesc = "OAM PriSec watch dog.";
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
			if (log.isTraceEnabled())
				log.trace("OMA primary, secondary.");
			SyslogAlarm sa = (SyslogAlarm) alarm;
			SyslogAlarm ta = (SyslogAlarm) group.getTrigger();
		 	String eventKey_sa = sa.getCustomFieldValue(GFPFields.EVENT_KEY);
		 	String eventKey_ta = ta.getCustomFieldValue(GFPFields.EVENT_KEY);

			if (eventKey_sa.equals("50004/1/10"))	// syslog OAM
			{
				// primary
				sa.setCustomFieldValue(GFPFields.SECONDARY_ALERT_ID, "1");
				sa.setCustomFieldValue(GFPFields.SECONDARYTIMESTAMP, "0");
			}
			else if (eventKey_sa.equals("50002/100/19"))// ciena OAM
			{
				// secondary
				sa.setCustomFieldValue(GFPFields.SECONDARY_ALERT_ID, ta.getCustomFieldValue(GFPFields.ALERT_ID));
				sa.setCustomFieldValue(GFPFields.SECONDARYTIMESTAMP, ta.getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP));
			}

			if (eventKey_ta.equals("50004/1/10"))	// syslog OAM
			{
				// primary
				ta.setCustomFieldValue(GFPFields.SECONDARY_ALERT_ID, "1");
				ta.setCustomFieldValue(GFPFields.SECONDARYTIMESTAMP, "0");
			}
			else if (eventKey_ta.equals("50002/100/19"))// ciena OAM
			{
				// secondary
				ta.setCustomFieldValue(GFPFields.SECONDARY_ALERT_ID, sa.getCustomFieldValue(GFPFields.ALERT_ID));
				ta.setCustomFieldValue(GFPFields.SECONDARYTIMESTAMP, sa.getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP));
			}
		}
	}	
}
