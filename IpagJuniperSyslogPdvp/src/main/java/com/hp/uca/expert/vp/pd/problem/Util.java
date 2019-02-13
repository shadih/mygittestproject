package com.hp.uca.expert.vp.pd.problem;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

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
import com.hp.uca.expert.x733alarm.PerceivedSeverity;
import com.att.gfp.data.ipag.decomposition.Decomposer;
import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.data.ipagAlarm.SyslogAlarm;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;


public class Util {
	private static Logger log = LoggerFactory.getLogger(Util.class);

	
	
	public static void whereToSendThenSend(EnrichedAlarm alarm, boolean decomp) {
		
		String eventKey = alarm.getCustomFieldValue(GFPFields.EVENT_KEY);
		String moClass = alarm.getOriginatingManagedEntity().split(" ")[0];
		PerceivedSeverity severity = alarm.getPerceivedSeverity();
		
		AlarmDelegationType delegateTo = AlarmDelegationType.FORWARD;
		String target = null;
				
		// turing off decomp globally here
		decomp = false;

		log.info("event Key = " + eventKey + ", isPtpMpt = " + ((SyslogAlarm)alarm).getIsPtpMpt());

		if(eventKey.equals("50003/100/1")) {
			if(moClass.equals("PPORT") || moClass.equals("DEVICE"))  {
				delegateTo = AlarmDelegationType.CASCADE;
				target = "PRIMARYSECONDARY_CORRELATION";
			}			
		} else if(eventKey.equals("50003/100/16") || eventKey.equals("50003/100/13") ||
				eventKey.equals("50003/100/12")) {
			delegateTo = AlarmDelegationType.CASCADE;
			target = "JUNIPER_JNXFRUOFFLINE";			
		} else if(eventKey.equals("50004/1/10")) {
				delegateTo = AlarmDelegationType.CASCADE;
				target = "PRIMARYSECONDARY_CORRELATION"; 
				//decomp = true;
		} else if(eventKey.equals("50002/100/21")  || eventKey.equals("50002/100/19")) { 
			delegateTo = AlarmDelegationType.CASCADE;     
			target = "PRIMARYSECONDARY_CORRELATION";  
		} else if (((SyslogAlarm)alarm).getIsPtpMpt() == true) {		
			delegateTo = AlarmDelegationType.CASCADE;
			target = "PTP_MPT";
			log.info("send it to CienaPdvp.");
		} else if (eventKey.equals("50002/100/55")) {
			String deviceType = alarm.getDeviceType();
			if (log.isTraceEnabled())
				log.trace("device type = " + deviceType);
			if ("CIENA EMUX".equals(deviceType))
			{
				if (log.isTraceEnabled())
					log.trace("Send Unreachable synthetic alarm.");

				SyslogAlarm syntheticA = null;
				try {
					syntheticA = new SyslogAlarm(alarm);
				} catch (Exception e) {
					log.trace("whereToSendThenSend: ERROR:"
							+ Arrays.toString(e.getStackTrace()));
					return;
				}
				syntheticA.setIsSent(false);
				syntheticA.setIdentifier(syntheticA.getIdentifier()+"EMUX");
				syntheticA.setCustomFieldValue(GFPFields.ALERT_ID, syntheticA.getCustomFieldValue(GFPFields.ALERT_ID)+"EMUX");
				syntheticA.setCustomFieldValue(GFPFields.CLASSIFICATION, "EMUX-CFO");
				syntheticA.setCustomFieldValue(GFPFields.DOMAIN, "IPAG");
				// send synthetic to AM
				Util.sendAlarm(syntheticA, delegateTo, target, decomp);		
			}
		}

		// if this is a UCA generated synthetic clear then we don't send it on to NOM.
		if("YES".equalsIgnoreCase(alarm.getCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA)) &&
				severity.equals(PerceivedSeverity.CLEAR) && target == null) 
			return;

		Util.sendAlarm(alarm, delegateTo, target, decomp);		
		
	}
	

	private static boolean sendAlarm(EnrichedAlarm alarm, AlarmDelegationType alarmDelegationType, String cascadeTargetName, boolean isDecomposed) 
	{
		try {
			SyslogAlarm ca = (SyslogAlarm) alarm;
			if (!ca.getCanSend())
				return false;
			if (ca.getIsFBSPtp() && !ca.getIdentifier().contains("Synthetic"))
			{
				// only synthetic alarm is sent
				if (log.isTraceEnabled())
					log.trace("Don't send the FBSPtp real alarm: "+ ca.getIdentifier());
				return false;
			}

			String targetApp = (cascadeTargetName == null)? "Alarm Manager": cascadeTargetName;

			if (log.isDebugEnabled()) {
				String axml = alarm.toXMLString();
				axml = axml.replaceAll("\\n", " ");
				log.debug("Sending alarm to " + targetApp + ", isDecomposed = " + isDecomposed + ", alarm = " + axml);
			}

			if (isDecomposed  && !alarm.getCustomFieldValue(GFPFields.G2SUPPRESS).equals("IPAG02"))
			{
				if (log.isDebugEnabled())
					log.debug("Decomposing alarm = " + ca.getIdentifier());

				GFPUtil.forwardAlarmToDecomposerInstance(alarm, "JUNIPER_DECOMPOSER");
				alarm.setDecomposed(true);
			}

			EnrichedAlarm alarmToCascade = alarm.clone();
			alarmToCascade.setJustInserted(true);
			alarmToCascade.setAboutToBeRetracted(false);
			alarmToCascade.getVar().remove(PD_Service_Navigation.NEED_NAVIGATION_UPDATE);
			
			alarmToCascade.SetAccumulativeTime();

			GFPUtil.forwardOrCascadeAlarm(alarmToCascade, alarmDelegationType, cascadeTargetName);
			ca.setIsSent(true);

			if (log.isTraceEnabled())
				log.trace("Sent successfully.");
			return true;
		} catch (Exception e) {
			log.error("Failed to send.", e);
			return false;
		}
	}

	public static void setTriggerWatch(SyslogAlarm alarm, Class ac, String cbName, long after, String watchdogDesc) {

		Class<?> partypes[] = new Class[0];
		Object arglist[] = new Object[0];
		Method method = null;
		
		try {
			method = ac.getMethod(cbName, partypes);
		} catch (SecurityException e) {
			log.trace("setTriggerWatch: Security Exception ERROR:"
					+ Arrays.toString(e.getStackTrace()));
		} catch (NoSuchMethodException e) {
			log.trace("setTriggerWatch: NoSuchMethod Exception ERROR:"
					+ Arrays.toString(e.getStackTrace()));		}
		
		Callback callback = new Callback(method, alarm, arglist);
		// to make sure...
		if(after < 2000 )
			after = 2000;
		alarm.setNumberOfWatches(alarm.getNumberOfWatches() + 1);
		Scenario scenario = ScenarioThreadLocal.getScenario();
		scenario.addCallbackWatchdogItem(after, callback, false, watchdogDesc, true, alarm);
		if (log.isTraceEnabled())
			log.trace("create WD for trigger alarm = " + alarm.getIdentifier() + ", description = " + watchdogDesc);
	}

	public static void WDPool(SyslogAlarm a, String targetName, boolean isDecompose, long extraWait,
			Scenario scenario) 
	{
		if (!a.getInPool() && a.getCanSend())
		{
			if (log.isTraceEnabled())
				log.trace("Put alarm Identifier = " + a.getIdentifier() + " to watchdog pool.");
			a.setInPool(true);
			CandidateAlarmProc task = null;
			if (targetName == null)
				// send to AM
				task = new CandidateAlarmProc(a, AlarmDelegationType.FORWARD, null, isDecompose, scenario);
			else
				task = new CandidateAlarmProc(a, AlarmDelegationType.CASCADE, targetName, false, scenario);

			// wait till this alarm's correlation in ALL groups are
			// completed.  2 sec should suffice .
			CandidateAlarmProc.watchdog.schedule(task, 2000+extraWait, TimeUnit.MILLISECONDS);
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
				if (log.isTraceEnabled())
					log.trace(alarm.getIdentifier()+" is the trigger for group = " + group.getName());
				return true;
			}
		}
		return false;
	}
    public static String getCST(String be_time_stamp)
    {
	// java supports EST, EDT(use "US/Eastern"), CDT but not CST
	// note that it returns CDT even you set to CST
	// i use EST to get around the issue
	long millis = (Long.parseLong(be_time_stamp)-3600)*1000;
		
	Date date = new Date(millis);
	SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.ENGLISH);
	sdf.setTimeZone(TimeZone.getTimeZone("EST"));
	String formattedDate = sdf.format(date);
	return formattedDate;
    }
}
