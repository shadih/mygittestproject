package com.att.gfp.ciena.cienaPD;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Method;
import com.hp.uca.common.callback.Callback;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.scenario.ScenarioThreadLocal;
import com.hp.uca.expert.vp.pd.services.PD_Service_Group;
import com.hp.uca.expert.vp.pd.services.PD_Service_Navigation;
import com.att.gfp.actions.GFPActionsFactory;
import com.att.gfp.data.ipag.decomposition.Decomposer;
import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;


public class Util {
	private static Logger log = LoggerFactory.getLogger(Util.class);
	private static boolean Junit = false;

	public static void init(boolean Junitx)
	{
		setJunit(Junitx);
	}
	public static void setJunit(boolean Junitx)
	{
		Junit = Junitx;
	}
	public static boolean getJunit()
	{
		return Junit;
	}
	// isDecomposed is true ==> cascadeTargetName must be null a ==>
	// alarmDelegationType must be FORWARD as the the alarm is never
	// decomposed when cadcading to other VP 
	public static void sendAlarm(EnrichedAlarm alarm, AlarmDelegationType alarmDelegationType, String cascadeTargetName, boolean isDecomposed) 

	{
		try {

			if ( alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50002/100/52") ) {
				if ( null != alarm.getEvcName() && alarm.getEvcName().contains("VLXU") ) {
					if( null == alarm.getCustomFieldValue("flags") || alarm.getCustomFieldValue("flags").isEmpty() ) {
						alarm.setCustomFieldValue("flags", "ENNI=<Y>"); 
					}
					else if ( !alarm.getCustomFieldValue("flags").contains("ENNI=<Y>") ) {
						alarm.setCustomFieldValue("flags", alarm.getCustomFieldValue("flags") + " ENNI=<Y>");
					}
				}
			}

			CienaAlarm ca = (CienaAlarm) alarm;

			if (!ca.getCanSend())
				return;



			String axml = alarm.toXMLString();
			axml = axml.replaceAll("\\n", " ");

			String targetApp = (cascadeTargetName == null)? "Alarm Manager": cascadeTargetName;
			log.info("Sending alarm to " + targetApp + ", isDecomposed = " + isDecomposed + ", alarm = " + axml);
			if (isDecomposed)
			{
				if (!getJunit())
				{
					List<EnrichedAlarm> decomposedAlarms = null;
					try {
						decomposedAlarms = Decomposer.decompose(alarm);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						log.error("Failed to decompose: ", e);
						e.printStackTrace();
						return;
					}
					log.info("Sent decomposed alarms to the Alarm Manger:");
					for (EnrichedAlarm decomposedAlarm : decomposedAlarms)
					{
						String daxml = decomposedAlarm.toXMLString();
						daxml = daxml.replaceAll("\\n", " ");
						log.info("Alarm = " + daxml);
						if (!getJunit())
							GFPUtil.forwardOrCascadeAlarm(decomposedAlarm, alarmDelegationType, cascadeTargetName);
					}
				}
			}
			else
			{
				if (!getJunit())
				{
					// to work around the UCA issue that stops processing alarm
					// after computeProbelmEntity()
					EnrichedAlarm alarmToCascade = alarm.clone();
					alarmToCascade.setJustInserted(true);
					alarmToCascade.setAboutToBeRetracted(false);
					alarmToCascade.getVar().remove(PD_Service_Navigation.NEED_NAVIGATION_UPDATE);
					GFPUtil.forwardOrCascadeAlarm(alarmToCascade, alarmDelegationType, cascadeTargetName);
				}
				ca.setIsSent(true);
			}
			log.info("Sent successfully.");
		} catch (Exception e) {
			log.error("Failed to send.", e);
		}
	}

	public static void setTriggerWatch(CienaAlarm alarm, Class ac, String cbName, long after, String watchdogDesc) {

		Class<?> partypes[] = new Class[0];
		Object arglist[] = new Object[0];
		Method method = null;

		try {
			method = ac.getMethod(cbName, partypes);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}

		Callback callback = new Callback(method, alarm, arglist);
		// to make sure...
		if(after == 0 )
			after = 2000;
		Scenario scenario = ScenarioThreadLocal.getScenario();
		scenario.addCallbackWatchdogItem(after, callback, false, watchdogDesc, true, alarm);
		log.info("cretae WD for trigger alarm = " + alarm.getIdentifier() + ", description = " + watchdogDesc);

	}

	public static void WDPool(Scenario scenario, CienaAlarm a, String targetName, boolean isDecompose) 
	{
		if (!a.getInPool() && a.getCanSend())
		{
			log.info("Put alarm Identifier = " + a.getIdentifier() + " to watchdog pool.");
			a.setInPool(true);
			CandidateAlarmProc task = null;
			if (targetName == null)
				// send to AM
				task = new CandidateAlarmProc(scenario, a, AlarmDelegationType.FORWARD, null, false);
			else
				task = new CandidateAlarmProc(scenario, a, AlarmDelegationType.CASCADE, targetName, isDecompose);

			// wait till this alarm's correlation in ALL groups are
			// completed.  2 sec should suffice .
			CandidateAlarmProc.watchdog.schedule(task, 2000, TimeUnit.MILLISECONDS);
		}
	}

	public static boolean isTriggerAlarm(Scenario scenario, Alarm alarm)
	{
		Collection<Group> groups = PD_Service_Group.getGroupsOfAnAlarm(scenario, alarm);
		for (Group group : groups)
		{
			if (group.getTrigger() == alarm)
			{
				// it is a trigger in other group.
				log.info(alarm.getIdentifier()+" is the trigger for group = " + group.getName());
				return true;
			}
		}
		return false;
	}
}
