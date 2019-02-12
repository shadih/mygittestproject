package com.hp.uca.expert.vp.pd.problem;

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
import com.att.gfp.data.ipagAlarm.EnrichedJuniperAlarm;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;


public class Util {
	private static Logger log = LoggerFactory.getLogger(Util.class);
	
	
	public static void whereToSendThenSend(EnrichedAlarm alarm) {
		if (log.isTraceEnabled())
			log.trace("whereToSendThenSend() : [enter}");

		boolean decomp = false;
		String eventKey = alarm.getCustomFieldValue(GFPFields.EVENT_KEY);
		String moClass = alarm.getOriginatingManagedEntity().split(" ")[0];
		PerceivedSeverity severity = alarm.getPerceivedSeverity();
		EnrichedJuniperAlarm eAlarm = (EnrichedJuniperAlarm) alarm;
		
		AlarmDelegationType delegateTo = AlarmDelegationType.FORWARD;
		String target = null;

		if(eventKey.equals("50003/100/1")) {
			
			if (alarm.getDeviceLevelExist() && alarm.getRemoteDeviceType() != null && 
					alarm.getRemoteDeviceType().equals("CIENA NTE") && !(GFPUtil.isAafDaAlarm(alarm))) 
				decomp = true; 

			if( moClass.equals("DEVICE")) {
				delegateTo = AlarmDelegationType.CASCADE;
				target = "JUNIPER_SYSLOG";
			} else if(moClass.equals("LPORT")) {
				delegateTo = AlarmDelegationType.CASCADE;
				target = "PRIMARYSECONDARY_CORRELATION";
			} else if(moClass.equals("PPORT")) {
				// send a reference alarm to CienaECPdvp
				GFPUtil.forwardOrCascadeAlarm(alarm, AlarmDelegationType.CASCADE, "CIENA");
				//Util.sendAlarm(alarm, AlarmDelegationType.CASCADE, "CIENA", false);

				delegateTo = AlarmDelegationType.CASCADE;
				target = "PRIMARYSECONDARY_CORRELATION";				
			}
		} else if(eventKey.equals("50002/100/19")|| eventKey.equals("50002/100/21")) {
			delegateTo = AlarmDelegationType.CASCADE;
			target = "JUNIPER_SYSLOG";	
			
			if((eventKey.equals("50002/100/19")) && !(GFPUtil.isAafDaAlarm(alarm))) { 
				decomp = true;
			}
			if((eventKey.equals("50002/100/21")) && !(GFPUtil.isAafDaAlarm(alarm)) && !("CIENA EMUX".equalsIgnoreCase(alarm.getDeviceType()))) {
				decomp = true;
			}  
		} else if(eventKey.equals("50003/100/6") || eventKey.equals("50003/100/7")) {
			decomp = true;
			delegateTo = AlarmDelegationType.CASCADE;
			target = "JUNIPER_COMPLETION";						
		} else if (eventKey.equals("50004/2/3") ||
				eventKey.equals("50004/2/58916875") ||
				eventKey.equals("50004/2/58916876") ||
				eventKey.equals("50004/2/58916877")) {
			delegateTo = AlarmDelegationType.CASCADE;
			target = "JUNIPER_COMPLETION";						
		//} else if(eventKey.equals("50003/100/19")) {
			//decomp = true;
		} else if (eventKey.equals("50002/100/55") || eventKey.equals("50004/1/2")) {
			delegateTo = AlarmDelegationType.CASCADE;
			target = "JUNIPER_SYSLOG";	
		} if(eventKey.equals("50003/100/23")) {
			if(moClass.equals("DEVICE")) {
				delegateTo = AlarmDelegationType.CASCADE;
				target = "JUNIPER_SYSLOG";
			}
		}else if (eventKey.equals("50002/100/52"))
				return;   // we don't send this alarm out anywhere, a copy is being sent by CIENA directly to syslog
		
		// if this is a UCA generated synthetic clear then we don't send it on to NOM.
		if("YES".equalsIgnoreCase(alarm.getCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA)) &&
				severity.equals(PerceivedSeverity.CLEAR) && target == null) 
			return;

		Util.sendAlarm(alarm, delegateTo, target, decomp);

		if (log.isTraceEnabled())
			log.trace("whereToSendThenSend() : [exit]");

	}

	// isDecomposed is true ==> cascadeTargetName must be null a ==>
	// alarmDelegationType must be FORWARD as the the alarm is never
	// decomposed when cadcading to other VP 
	private static void sendAlarm(EnrichedAlarm alarm, AlarmDelegationType alarmDelegationType, String cascadeTargetName, boolean isDecomposed) 

	{
		try {
			EnrichedJuniperAlarm ca = (EnrichedJuniperAlarm) alarm;

			// we do decomp always unless the g2_suppression is set
			String targetApp = (cascadeTargetName == null)? "Alarm Manager": cascadeTargetName;
			if (isDecomposed && !alarm.getCustomFieldValue(GFPFields.G2SUPPRESS).equals("IPAG02"))
			{
				if (log.isDebugEnabled())
					log.debug("Decomposing alarm = " + ca.getIdentifier());
				
				if("50002/100/19".equalsIgnoreCase(alarm.getCustomFieldValue(GFPFields.EVENT_KEY)) ||
						alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50002/100/21") ) {
					GFPUtil.forwardAlarmToDecomposerInstance(alarm, "CIENA_DECOMPOSER");
					alarm.setDecomposed(true);
				} else {
					GFPUtil.forwardAlarmToDecomposerInstance(alarm, "JUNIPER_DECOMPOSER");
					alarm.setDecomposed(true);
				}
			} 

			// maybe we sent this alarm already
			if (!ca.getCanSend())
				return;

			// we don't want to send this alarm if the g2_suppress is set to ipag01
//			if(alarm.getCustomFieldValue(GFPFields.G2SUPPRESS).toUpperCase().equals("IPAG01")) {
//				log.info("Event: " + alarm.getCustomFieldValue(GFPFields.SEQNUMBER) + " ID: " + alarm.getIdentifier() + " was suppressed because the G2_suppress field is set for IPAG01 !!");
//			} else {

				EnrichedAlarm alarmToCascade = alarm.clone();
				alarmToCascade.setJustInserted(true);
				alarmToCascade.setAboutToBeRetracted(false);
				alarmToCascade.getVar().remove(PD_Service_Navigation.NEED_NAVIGATION_UPDATE);

				if (log.isDebugEnabled())
					log.debug("Sending alarm to " + targetApp + ", alarm = " + alarmToCascade.getIdentifier());

				alarmToCascade.SetAccumulativeTime();
				
				GFPUtil.forwardOrCascadeAlarm(alarmToCascade, alarmDelegationType, cascadeTargetName);
				ca.setIsSent(true);

				if (log.isTraceEnabled())
					log.trace("Sent successfully.");
			//}
		} catch (Exception e) {
			log.error("Failed to send.", e);
		}
	}

	public static void setTriggerWatch(EnrichedJuniperAlarm alarm, Class ac, String cbName, long after, String watchdogDesc) {

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
		if(after < 2000 )
			after = 2000;
		alarm.setNumberOfWatches(alarm.getNumberOfWatches() + 1);
		Scenario scenario = ScenarioThreadLocal.getScenario();
		scenario.addCallbackWatchdogItem(after, callback, false, watchdogDesc, true, alarm);
		if (log.isTraceEnabled())
			log.trace("create WD for trigger alarm = " + alarm.getIdentifier() + ", description = " + watchdogDesc);

	}

	public static void WDPool(EnrichedJuniperAlarm a, long extraWait, Scenario scenario) 
	{
		if (!a.getInPool() && a.getCanSend())
		{
			if (log.isTraceEnabled())
				log.trace("Put alarm Identifier = " + a.getIdentifier() + " to watchdog pool.");
			a.setInPool(true);
			CandidateAlarmProc task = null;
			
			task = new CandidateAlarmProc(a, scenario);

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
}
