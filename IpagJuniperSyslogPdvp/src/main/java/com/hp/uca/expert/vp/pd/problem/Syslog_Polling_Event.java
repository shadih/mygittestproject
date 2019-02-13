/**
 * This Problem is empty and ready to define methods to customize this problem
 */
package com.hp.uca.expert.vp.pd.problem;

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
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.scenario.ScenarioThreadLocal;
import com.hp.uca.expert.vp.pd.core.JuniperSyslog_ProblemDefault;
import com.hp.uca.expert.vp.pd.core.ProblemDefault;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;
import com.hp.uca.expert.vp.pd.services.PD_Service_ProblemAlarm;

/**
 * @author MASSE
 * 
 */
//public final class Syslog_Polling_Event extends JuniperSyslog_ProblemDefault implements
public final class Syslog_Polling_Event extends JuniperSyslog_ProblemDefault implements
		ProblemInterface {
	private Logger log = LoggerFactory.getLogger(Syslog_Polling_Event.class);
	
	

	public Syslog_Polling_Event() {
		super();
		setLog(LoggerFactory.getLogger(Syslog_Polling_Event.class));
	}

	@Override
	public boolean isMatchingCandidateAlarmCriteria(Alarm a) throws Exception {
		SyslogAlarm ea = (SyslogAlarm) a;
		String eventKey = ea.getCustomFieldValue(GFPFields.EVENT_KEY);
		String targetName = null;
		Util.WDPool(ea, targetName, false, 0, getScenario());
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingCandidateAlarmCriteria() runs.");
		return true;
	}
	
	@Override
	public boolean isMatchingTriggerAlarmCriteria(Alarm a) throws Exception {
		boolean ret = false;

		SyslogAlarm alarm = (SyslogAlarm) a;	
			long after = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger();
			String watchdogDesc = "Syslog_Polling_Event watch dog.";
			Util.setTriggerWatch((SyslogAlarm)a, SyslogAlarm.class, "rpdRsvpByPassDownexpirationCallBack", after, watchdogDesc);
			ret = true; 
					
			if (log.isTraceEnabled())
				LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingTriggerAlarmCriteria() " + "[" + ret + "]"); 
		return ret;
	} 
	

	@Override
	public boolean isMatchingSubAlarmCriteria(Alarm a,Group group) throws Exception {
		boolean ret = true;
		SyslogAlarm ea = (SyslogAlarm) a;
		String eventKey = ea.getCustomFieldValue(GFPFields.EVENT_KEY);
		String targetName = null;
		Util.WDPool(ea, targetName, false, 0, getScenario());
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingSubAlarmCriteria() " + "[" + ret + "], group name = " + group.getName());
		return ret;
	}
	
/*	@Override
	public boolean isAllCriteriaForProblemAlarmCreation(Group group)
			throws Exception {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "isAllCriteriaForProblemAlarmCreation()");
		}

		
		boolean number = false;
		boolean ret = false;
		
		if(group.getNumber() > 1)
			number = true;

		boolean time = PD_Service_ProblemAlarm.isItTimeForProblemAlarmCreation(group);

		ret = number & time;
		
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "isAllCriteriaForProblemAlarmCreation()  ["+ret+"]");
		}

		
		return ret;
	}*/
	
	// This method is used to send alarms that have been attached to a group
	@Override
	public void whatToDoWhenSubAlarmIsAttachedToGroup(Alarm alarm, Group group) throws Exception {

		if (log.isTraceEnabled())
			LogHelper.enter(log, "alarm = " + alarm.getIdentifier() + ", group = " + group.getName() + ", group # = " + group.getNumber() + ", whatToDoWhenSubAlarmIsAttachedToGroup()");
		
		//make sure that the trigger was suppressed and the subalarm was forwarded.
		if(alarm != group.getTrigger()) {
			//service_util.sendAlarm(getScenario(), (SyslogAlarm) alarm);

			String instance = alarm.getOriginatingManagedEntity().split(" ")[1];

			//Correlate with Ciena syslog Unreachable (50002/100/55)
			if(alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50002/100/55"))
				log.info("This event: " + group.getTrigger().getCustomFieldValue(GFPFields.SEQNUMBER) + " cat: 50004/3/2 on " + instance +
						" SUPPRESSED by: SysLog Polling Event | due to active ciena-unreachable (50002/100/55) on same device");
			else
				//Correlate with Adtran syslog nEpingTimedOut (50001/100/52)
				if(alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50001/100/52"))
					log.info("This event: " + group.getTrigger().getCustomFieldValue(GFPFields.SEQNUMBER) + " cat: 50004/3/2 on " + instance +
							" SUPPRESSED by: SysLog Polling Event | due to active adtran-nEpingTimedOut (50001/100/52) on same device");
				else
					//Correlate with Juniper syslog polling event (50004/1/2) 
					//Also correlate from other event and suppress that one if this occurs first.
					if(alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50004/1/2"))
						log.info("This event: " + group.getTrigger().getCustomFieldValue(GFPFields.SEQNUMBER) + " cat: 50004/3/2 on " + instance +
								" SUPPRESSED by: SysLog Polling Event | due to active juniper-syslog-alarm (50004/1/2) on same device");
			
				// send out this alarm
			if (log.isTraceEnabled())
				log.trace("Sending SubAlarm" + alarm.getIdentifier());
				
				Util.whereToSendThenSend((EnrichedAlarm)alarm, false);

				if (log.isTraceEnabled())
					log.trace("Suppressing trigger: " + group.getTrigger().getIdentifier());
				log.info("alarm = " + ((SyslogAlarm) group.getTrigger()).getIdentifier() + " is suppressed.");
				((SyslogAlarm) group.getTrigger()).setSuppressed(true);
			
		}
//		} else{
//			((SyslogAlarm) alarm).setSuppressed(true);
//			if (log.isTraceEnabled()) {
//				LogHelper.enter(log, "Trigger will be suppressed: " + alarm.getIdentifier());}
//		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenSubAlarmIsAttachedToGroup()");
		}
		
	}
}
