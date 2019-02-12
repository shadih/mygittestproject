/**
 * This Problem is empty and ready to define methods to customize this problem
 */
package com.hp.uca.expert.vp.pd.problem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.EnrichedJuniperAlarm;
import com.att.gfp.helper.GFPFields;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.vp.pd.core.JuniperLinkDown_ProblemDefault;
import com.hp.uca.expert.vp.pd.core.PD_AlarmRecognition;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;
import com.hp.uca.expert.vp.pd.services.PD_Service_Util;

//
// trigger is enriched by isMatchingSubAlarmCriteria()
// trigger is sent by WD, WD times is long enough to complete
//	enrichment(isAllCriteriaForProblemAlarmCreation() is not needed
// no subalarm
// candidate is sent by WD pool
//
public final class Severity_Correlation extends JuniperLinkDown_ProblemDefault implements
ProblemInterface {

	private Logger log = LoggerFactory.getLogger(Severity_Correlation.class);
	public Severity_Correlation() {
		super();

	}
	// page 34 on project 256258a, HLD-500


	@Override
	public boolean isMatchingTriggerAlarmCriteria(Alarm a) throws Exception {
		boolean ret = false;
		EnrichedJuniperAlarm alarm = (EnrichedJuniperAlarm) a;
		HashSet<String> peerset = alarm.getPeerSet();
		if (peerset.size() > 0)
		{
			log.info("Set severity to Major.  It will be set to Critical later if all its remote peer alarms exist.");
			alarm.setSeverity(1);
			setTriggerWatch((EnrichedJuniperAlarm)a);
			ret = true;
		}
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingTriggerAlarmCriteria() " + "[" + ret + "]");

		return ret;
	}

	@Override
	public List<String> computeProblemEntity(Alarm a) throws Exception {
		List<String> problemEntities = new ArrayList<String>();
		EnrichedJuniperAlarm alarm = (EnrichedJuniperAlarm) a;

		String instance = alarm.getOriginatingManagedEntity().split(" ")[1];
		problemEntities.add(instance);	// it is used when it is the
		// subalarm
		HashSet<String> peerset = alarm.getPeerSet();
		if (peerset.size() > 0)
		{
			Iterator i = peerset.iterator();
			while(i.hasNext())
			{
				String peer = (String)i.next();
				if (peer != null && peer.length() > 0)
					// it is used when it's trigger
					problemEntities.add(peer);
			}
		}

		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", computeProblemEntity() --- problemEntities=" + problemEntities);
		return problemEntities;
	} 

	@Override
	public boolean isMatchingSubAlarmCriteria(Alarm a, Group group) throws Exception {
		boolean ret = true;

		Alarm trigger = group.getTrigger();
		EnrichedJuniperAlarm alarm = (EnrichedJuniperAlarm) a;
		if (log.isTraceEnabled())
			log.trace("trigger identifier = " + trigger.getIdentifier()+
			", trigger alarmRaisedTime = " + trigger.getAlarmRaisedTime()+
			", trigger be_time_stamp = " + trigger.getCustomFieldValue("be_time_stamp")+
			", alarm identifier = " + alarm.getIdentifier()+
			", alarm alarmRaisedTime = " + alarm.getAlarmRaisedTime()+
			", alarm be_time_stamp = " + alarm.getCustomFieldValue("be_time_stamp"));

		if (alarm.isSuppressed())
		{
			log.info("isSuppressed = " + alarm.isSuppressed());
			ret = false;
		}
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingSubAlarmCriteria() " + "[" + ret + "], group name = " + group.getName());
		return ret;
	}

	@Override
	public void whatToDoWhenSubAlarmIsAttachedToGroup(Alarm a, Group group) throws Exception {
		EnrichedJuniperAlarm trigger = (EnrichedJuniperAlarm) group.getTrigger();
		EnrichedJuniperAlarm alarm = (EnrichedJuniperAlarm) a;
		String instance = alarm.getOriginatingManagedEntity().split(" ")[1];
		HashSet<String> peerset = trigger.getPeerSet();

		if (log.isTraceEnabled())
			LogHelper.enter(log, "trigger = " + trigger.getIdentifier() + "alarm = " + a.getIdentifier() + ", group = " + group.getName() + ", group member # = " + group.getNumber() + ", subalarm instance = " + instance + ", whatToDoWhenSubAlarmIsAttachedToGroup()");
		if (group.getNumber() > 1)
		{
			if (peerset.size() > 0 && peerset.contains(instance))
			{
				peerset.remove(instance);
				if (peerset.size() == 0)
				{
					log.info("Set severity to Critical.");
					trigger.setSeverity(0);
				}
			}
		}
	}	
}
