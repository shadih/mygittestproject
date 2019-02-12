package com.att.gfp.ciena.cienaPD;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.hp.uca.expert.vp.pd.services.PD_Service_Group;
import com.hp.uca.expert.vp.pd.services.PD_Service_Navigation;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Method;
import com.hp.uca.common.callback.Callback;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.scenario.ScenarioThreadLocal;
import com.att.gfp.data.ipag.decomposition.Decomposer;
import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;


public class Util {
	private static Logger log = LoggerFactory.getLogger(Util.class);

	// isDecomposed is true ==> cascadeTargetName must be null a ==>
	// alarmDelegationType must be FORWARD as the the alarm is never
	// decomposed when cadcading to other VP 
	public static void sendAlarm(EnrichedAlarm alarm, boolean isDecomposed)
	{
		try {
			CienaAlarm ca = (CienaAlarm) alarm;
			// below cannot be used as it is the method of EnrichedAlarm
			// cannot call below(severity is stored in EnrichedAlarm
			// domain) as setSeverity() is called for clear, which 
			// doesn't update the severity of EnrichedAlarm 
			// int severity = ca.getSeverity();

			String severity = ca.getCustomFieldValue("severity");
			log.info("sendAlarm() severity = " + severity + ", isDecomposed = " + isDecomposed);
			String eventKey = alarm.getCustomFieldValue(GFPFields.EVENT_KEY);
			// no need to check getCanSend() for clear as
			// its isSent is from active alarm and is always true
			// 
			// "50003/100/1" is for reference only.  don't send it
			//
			if ((!"4".equals(severity) && !isDecomposed && !ca.getCanSend()) || eventKey.equals("50003/100/1"))
				return;
			String targetName = null;
			if (isDecomposed)
				targetName = "CIENA_DECOMPOSER";
			else if (eventKey.equals("50002/100/21"))
				targetName = "JUNIPER_LINKDOWN";
			else if (eventKey.equals("50002/100/19"))
				targetName = "JUNIPER_LINKDOWN";
			else if (eventKey.equals("50002/100/55"))
			{
				if (ca.getUnreachableAlarm2JuniperVP()) {
					// un-comment line below in order to send ciena unreach. to JUNIPER instance when it should be sent for further correlation
					// targetName = "JUNIPER_LINKDOWN";
					targetName = null;
				}
				else
					targetName = null;
			}
			else if (eventKey.equals("50002/100/52"))
				targetName = "JUNIPER_LINKDOWN";
			else if (eventKey.equals("50002/100/58"))
				targetName = "JUNIPER_SYSLOG";
			else
				// health trap go to NOM
				// CienaPdvp doesn't receive traps
				targetName = null;
			sendAlarm(alarm, targetName);

			if (!isDecomposed)
			{
				// send "50002/100/52" to "JUNIPER_LINKDOWN" and
				// "JUNIPER_SYSLOG"
				if (eventKey.equals("50002/100/52"))
				{
					targetName = "JUNIPER_SYSLOG";
					sendAlarm(alarm, targetName);
				}
	
				// send synthetic alarm when it is NOT forwarded				// to Syslog for correlation
				else if (eventKey.equals("50002/100/55") &&
					// un-comment line below when sending ciena unreach. to JUNIPER instance when it should be sent for further correlation	
					// !ca.getUnreachableAlarm2JuniperVP() &&
					"CIENA EMUX".equals(ca.getDeviceType()))
				{
					CienaAlarm syntheticA = null;
					try {
						syntheticA = new CienaAlarm(alarm);
						syntheticA.setIdentifier(syntheticA.getIdentifier()+"EMUX");
						syntheticA.setCustomFieldValue(GFPFields.ALERT_ID, syntheticA.getCustomFieldValue(GFPFields.ALERT_ID)+"EMUX");
						syntheticA.setCustomFieldValue(GFPFields.CLASSIFICATION, "EMUX-CFO");
						syntheticA.setCustomFieldValue(GFPFields.DOMAIN, "IPAG");

						targetName = null;
						// send synthetic to AM
						sendAlarm(syntheticA, targetName);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			log.error("Failed to send.", e);
		}
	}

	public static void sendAlarm(EnrichedAlarm alarm, String targetName)
	{
		try {
			String axml = alarm.toXMLString();
			axml = axml.replaceAll("\\n", " ");			

			CienaAlarm ca = (CienaAlarm) alarm;

			String targetApp = (targetName == null)? "Alarm Manager": targetName;
			AlarmDelegationType delegationType = (targetName == null)? AlarmDelegationType.FORWARD: AlarmDelegationType.CASCADE;
			log.info("Sending alarm to " + targetApp + ", alarm = " + axml);
			if ("CIENA_DECOMPOSER".equals(targetName))
			{
				// do not call setIsSent(true) for decomposed
				// alarm
				GFPUtil.forwardAlarmToDecomposerInstance(alarm, "CIENA_DECOMPOSER");
				ca.setIsSent2DEC(true);
			}
			else 
			{
				// to work around the UCA issue that stops processing 
				// alarm after computeProbelmEntity()
				EnrichedAlarm alarmToCascade = alarm.clone();
				alarmToCascade.setJustInserted(true);
				alarmToCascade.setAboutToBeRetracted(false);
				alarmToCascade.getVar().remove(PD_Service_Navigation.NEED_NAVIGATION_UPDATE);
				GFPUtil.forwardOrCascadeAlarm(alarmToCascade, delegationType, targetName);
				ca.setIsSent(true);
			}

			log.info("Sent successfully.");
		} catch (Exception e) {
			log.error("Failed to send.", e);
		}
	}

	public static void setTriggerWatch(CienaAlarm alarm, Class ac, String cbName, long wdtimer, String watchdogDesc) {

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
		if(wdtimer < 2000 )
			wdtimer = 2000;

		alarm.setNumberOfWatches(alarm.getNumberOfWatches() + 1);
		Scenario scenario = ScenarioThreadLocal.getScenario();
		scenario.addCallbackWatchdogItem(wdtimer, callback, false, watchdogDesc, true, alarm);
		log.info("create WD for trigger alarm = " + alarm.getIdentifier() + ", description = " + watchdogDesc);

	}

	public static void WDPool(Scenario scenario, CienaAlarm a, long aging)
	{
		if (!a.getInPool() && a.getCanSend())
		{
			log.info("Put alarm Identifier = " + a.getIdentifier() + " to watchdog pool.");
			a.setInPool(true);
			CandidateAlarmProc task = null;
			
			task = new CandidateAlarmProc(scenario, a);

			// wait till this alarm's correlation in ALL groups are
			// completed.  2 sec should suffice .
			CandidateAlarmProc.watchdog.schedule(task, 2000+aging, TimeUnit.MILLISECONDS);
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
	// send alarm which is not trigger, is subalarm and candiate
	public static void sendNonTrigger(CienaAlarm alarm)
	{
		Util.WDPool(ScenarioThreadLocal.getScenario(), alarm, alarm.getAging());
	}
}
