/**
 * This Problem is empty and ready to define methods to customize this problem
 */
package com.hp.uca.expert.vp.pd.problem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

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

public final class Ciena_LD_FRU_CARD extends JuniperSyslog_ProblemDefault implements ProblemInterface {

	private Logger log = LoggerFactory.getLogger(Ciena_LD_FRU_CARD.class);
	public Ciena_LD_FRU_CARD() {
		super();

	}

	@Override
	public boolean isMatchingCandidateAlarmCriteria(Alarm a) throws Exception {
		SyslogAlarm ea = (SyslogAlarm) a;
		String eventKey = ea.getCustomFieldValue(GFPFields.EVENT_KEY);
		String targetName = null;
		int extrawait = 0;
		if (!eventKey.equals("50002/100/21"))
			extrawait = 53000;	// aging FRU 55 seconds.
		Util.WDPool(ea, targetName, false, extrawait, getScenario());
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
		int extrawait = 0;
		if (!eventKey.equals("50002/100/21"))
			extrawait = 53000;	// aging FRU 55 seconds.
		Util.WDPool(ea, targetName, false, extrawait, getScenario());
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
		alarm.setIsCiena_LD_FRU_CARD(true);
		if (eventKey.equals("50002/100/21"))
		{
			if ("JUNIPER MX SERIES".equals(alarm.getRemoteDeviceType()))
			{
				// remote ip/slot/card/port
				String remote_pport_key = alarm.getRemotePportInstanceName();
				//String remote_pport_key = alarm.getRemotePportKey();
				int ix = remote_pport_key.lastIndexOf("/");
				// problem entity is remote ip/slot/card
				problemEntities.add(remote_pport_key.substring(0,ix));
			}
			else
			{
				problemEntities.add(alarm.getIdentifier());
				alarm.setIsCiena_LD_FRU_CARD(false);
			}
		}
		else
		{
			// this is FRU
            problemEntities.add(alarm.getOriginatingManagedEntity().split(" ")[1]);
		}
		
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + alarm.getIdentifier() + ", computeProblemEntity() --- problemEntities=" + problemEntities);

		return problemEntities;
	} 

	@Override
	public boolean isMatchingTriggerAlarmCriteria(Alarm a) throws Exception {
		boolean ret = false;

		SyslogAlarm alarm = (SyslogAlarm) a;	
		if (alarm.getIsCiena_LD_FRU_CARD() == true)
		{
			long after = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger();
			String watchdogDesc = "Ciena_LD_FRU_CARD watch dog.";
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
		SyslogAlarm trigger = (SyslogAlarm) group.getTrigger();

		if (group.getNumber() > 1)
		{
			if ("CIENA NTE".equals(trigger.getDeviceType()))
			{
				if (!"Y".equals(trigger.getMobility()))
				{
					log.info("alarm = " + trigger.getIdentifier() + " is suppressed.");
					trigger.setSuppressed(true);
				}
				else
					procChildAlarm(trigger, (SyslogAlarm)alarm);
			}
			else
				procChildAlarm(trigger, (SyslogAlarm)alarm);
		}
	}	
	public void procChildAlarm(SyslogAlarm trigger, SyslogAlarm alarm)
	{
		// remote ip/slot/card/port
		// String remote_pport_key = trigger.getRemotePportKey();
		String remote_pport_key = trigger.getRemotePportInstanceName();
		String[] isc = remote_pport_key.split("/");
		String slot = isc[1];
		String card = isc[2];

		String be_time_stamp = alarm.getCustomFieldValue("be_time_stamp");
		String cst = Util.getCST(be_time_stamp);
		if (log.isTraceEnabled())
			log.trace("slot = " + slot + ", card = " + card + ", cst = " + cst);
		StringBuilder bld = new StringBuilder("Child=<Y> <AlertID><AlertKey> ");
		bld.append(alarm.getCustomFieldValue(GFPFields.ALERT_ID)+"-IPAG01</AlertKey><TimeStamp>"+cst+" CST </TimeStamp></AlertID> DeviceName=<"+alarm.getDeviceName()+"> Slot=<"+slot+"> Card=<"+card+">");
		trigger.setCustomFieldValue("info3", bld.toString());
	}
}
