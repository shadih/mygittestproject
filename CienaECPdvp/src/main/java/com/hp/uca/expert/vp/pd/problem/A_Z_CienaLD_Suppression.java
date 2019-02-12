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
public final class A_Z_CienaLD_Suppression extends ProblemDefault implements ProblemInterface 
{

	private Logger log = LoggerFactory.getLogger(A_Z_CienaLD_Suppression.class);
	public A_Z_CienaLD_Suppression() {
		super();

	}

	 @Override
	public List<String> computeProblemEntity(Alarm a) throws Exception
	{
		List<String> problemEntities = new ArrayList<String>();
		CienaAlarm alarm = (CienaAlarm) a;

		problemEntities.add(alarm.getInstance());
		String remote_pport_key = alarm.getRemote_pport_key();
		
		log.info("remote_pport_key = " + remote_pport_key);
		if (remote_pport_key != null && remote_pport_key.length() > 0)
			problemEntities.add(remote_pport_key);

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

		// if requirement : A exists, suppress B.
		// ==> if A is suppressed, B is suppressed
		// 
		// if the trigger alarm is suppressed (in other problem 
		// group), the method still returns true and a group is
		// formed 
		//

		long after = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger();
		int aging = alarm.getAging();
		long wdtimer = (after>aging)? after: aging;
		String watchdogDesc = "A_Z_CienaLD watch dog.";
		Util.setTriggerWatch(alarm, CienaAlarm.class, "simpleSendCallBack", wdtimer, watchdogDesc);
		ret = true;

		log.info("alarm = " + a.getIdentifier() + ", isMatchingTriggerAlarmCriteria() " + "[" + ret + "]");

		return ret;
	}

	@Override
	public boolean isMatchingSubAlarmCriteria(Alarm a, Group group) throws Exception {
		boolean ret = false;

                CienaAlarm trigger = (CienaAlarm)group.getTrigger();
                CienaAlarm alarm = (CienaAlarm) a;
		// prombem entity:    instance   remote_pport_key
		//                   --------------------------------
		// trigger              X            Y
		// alarm_a              X            Y
		// alarm_b              Y            Z
		// ==> alamr_a is NOT qualified to be in the group, alarm_b is
		// 
		if (!alarm.getInstance().equals(trigger.getInstance()))
		{
			Util.sendNonTrigger(alarm);
			ret = true;
		}
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
    			// in the case of a CIENA EMUX and CIENA NTE, CIENA EMUX
			// is suppressed
    			if(trigger.getDeviceType().equals("CIENA EMUX"))
			{
    				log.info("Suppressed Ciena Link Down alarm(A-Z): " + trigger.getIdentifier() + " due to A_Z correlation.");
				trigger.setSuppressed(true);
  			}
			else 
			{
				if (alarm.getIsSent() == true)
				{
    					log.info("Suppressed Ciena Link Down alarm(A-Z): " + trigger.getIdentifier() + " due to A_Z correlation.");
					trigger.setSuppressed(true);
				}
				else
				{
					if (alarm.getDeviceType().equals("CIENA EMUX"))
					{
    						log.info("Suppressed Ciena Link Down alarm(A-Z): " + alarm.getIdentifier()  + " due to A_Z correlation.");
						alarm.setSuppressed(true);
					}
					else
					{
    						log.info("Suppressed Ciena Link Down alarm(A-Z): " + trigger.getIdentifier() + " due to A_Z correlation.");
						trigger.setSuppressed(true);
					}
				}
  			}
		}
	}	
}
