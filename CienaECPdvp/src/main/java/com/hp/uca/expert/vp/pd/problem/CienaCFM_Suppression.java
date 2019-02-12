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

import com.att.gfp.ciena.cienaPD.CienaAlarm;
import com.att.gfp.ciena.cienaPD.Util;
import com.att.gfp.helper.GFPFields;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.vp.pd.core.PD_AlarmRecognition;
import com.hp.uca.expert.vp.pd.core.ProblemDefault;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;
import com.hp.uca.expert.vp.pd.services.PD_Service_Util;

//
public final class CienaCFM_Suppression extends ProblemDefault implements ProblemInterface 
{

	private Logger log = LoggerFactory.getLogger(CienaCFM_Suppression.class);
	public CienaCFM_Suppression() {
		super();

	}

	 @Override
	public List<String> computeProblemEntity(Alarm a) throws Exception
	{
		List<String> problemEntities = new ArrayList<String>();
		CienaAlarm alarm = (CienaAlarm) a;
		String eventKey = a.getCustomFieldValue("EventKey");

		if(eventKey.equals("50002/100/52"))
		{
			String evcNodeInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
			String evcInstance = evcNodeInstance.split("/")[1];
			problemEntities.add(evcInstance);
		}
		else
		{
			HashSet<String> vrfset = alarm.getVrfSet();
			if (vrfset.size() == 0)
				problemEntities.add(a.getIdentifier());
			else
			{
				String vrf;
				Iterator i = vrfset.iterator();
				while(i.hasNext())
				{
					vrf = (String)i.next();
					if (vrf.length() > 0)
						problemEntities.add(vrf);
				}
			}
		}
		if (problemEntities.size() == 0)
			problemEntities.add(a.getIdentifier());

		log.info("alarm = " + a.getIdentifier() + ", computeProblemEntity() --- problemEntities=" + problemEntities);
		return problemEntities;
	 } 

	@Override
	public boolean isMatchingCandidateAlarmCriteria(Alarm a) throws Exception {
		Util.sendNonTrigger((CienaAlarm) a);
		log.info("alarm = " + a.getIdentifier() + ", isMatchingCandidateAlarmCriteria() runs.");
		return true;
	}

	@Override
	public boolean isMatchingTriggerAlarmCriteria(Alarm a) throws Exception {
		boolean ret = false;
		CienaAlarm alarm = (CienaAlarm) a;
		if (!alarm.isSuppressed())
		{
			long after = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger();
			int aging = alarm.getAging();
			long wdtimer = (after>aging)? after: aging;
			String watchdogDesc = "CienaCFM_Suppression watch dog.";
			Util.setTriggerWatch(alarm, CienaAlarm.class, "simpleSendCallBack", wdtimer, watchdogDesc);
			ret = true;
		}
		log.info("alarm = " + a.getIdentifier() + ", isMatchingTriggerAlarmCriteria() " + "[" + ret + "]");

		return ret;
	}

	@Override
	public boolean isMatchingSubAlarmCriteria(Alarm a, Group group) throws Exception {
		boolean ret = false;

                Alarm trigger = group.getTrigger();
                CienaAlarm alarm = (CienaAlarm) a;
		Util.sendNonTrigger(alarm);
		ret = true;
		log.info("trigger identifier = " + trigger.getIdentifier()+
", trigger alarmRaisedTime = " + trigger.getAlarmRaisedTime()+
", trigger be_time_stamp = " + trigger.getCustomFieldValue("be_time_stamp")+
", alarm identifier = " + alarm.getIdentifier()+
", alarm alarmRaisedTime = " + alarm.getAlarmRaisedTime()+
", alarm be_time_stamp = " + alarm.getCustomFieldValue("be_time_stamp"));

		log.info("alarm = " + a.getIdentifier() + ", isMatchingSubAlarmCriteria() " + "[" + ret + "], group name = " + group.getName());
		return ret;
	}

	@Override
	public void whatToDoWhenSubAlarmIsAttachedToGroup(Alarm a, Group group) throws Exception {
                CienaAlarm trigger = (CienaAlarm) group.getTrigger();
                CienaAlarm alarm = (CienaAlarm) a;

		log.info("trigger = " + trigger.getIdentifier() + "alarm = " + a.getIdentifier() + ", group = " + group.getName() + ", group member # = " + group.getNumber() + ", whatToDoWhenSubAlarmIsAttachedToGroup()");
		if (group.getNumber() > 1)
		{
			log.info("alarm = " + trigger.getIdentifier() + " is suppressed due to existence of 50002/100/14 or 50002/100/21 or 50003/100/1.");
			trigger.setSuppressed(true);
		}
	}	
}
