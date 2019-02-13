package com.hp.uca.expert.vp.pd.core;

import java.util.Collection;

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
import com.hp.uca.expert.vp.pd.problem.Util;
import com.hp.uca.expert.vp.pd.services.PD_Service_ProblemAlarm;

public class JuniperSyslog_ProblemDefault extends ProblemDefault {

	//protected static final String LINKDOWN_KEY = "50003/100/1";
	//private static final int PRIORITY_FACTOR = 1000;
	private static Logger log = LoggerFactory.getLogger(JuniperSyslog_ProblemDefault.class);

	
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
	
	// This method is used to send alarms that have been attached to a group
	@Override
	public void whatToDoWhenSubAlarmIsAttachedToGroup(Alarm alarm, Group group)
			throws Exception {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "whatToDoWhenSubAlarmIsAttachedToGroup()");
		}
		
		//make sure that the trigger is suppressed and the subalarm is forwarded.
		if(alarm != group.getTrigger()) {
			Util.whereToSendThenSend((EnrichedAlarm)alarm, false);
			
			log.info("Alarm was forwarded: " + alarm.getIdentifier() + " Sequence number: " +
					alarm.getCustomFieldValue(GFPFields.SEQNUMBER));
			
			log.info("Alarm " + group.getTrigger().getIdentifier() + " Sequence number: " +
					group.getTrigger().getCustomFieldValue(GFPFields.SEQNUMBER) + " will be suppressed by " +
					getProblemPolicy().getName());

			// suppress the trigger
			((SyslogAlarm) group.getTrigger()).setSuppressed(true);
		} 
		
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenSubAlarmIsAttachedToGroup()");
		}
		
	}

}	
	
	

