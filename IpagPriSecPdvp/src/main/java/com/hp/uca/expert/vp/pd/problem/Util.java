package com.hp.uca.expert.vp.pd.problem;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.data.ipagAlarm.Pri_Sec_Alarm;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
import com.hp.uca.common.callback.Callback;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.scenario.ScenarioThreadLocal;
import com.hp.uca.expert.vp.pd.services.PD_Service_Navigation;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;


public class Util {
	private static Logger log = LoggerFactory.getLogger(Util.class);

	public static void WhereToSendAndSend(EnrichedAlarm alarm) {
		String eventKey = alarm.getCustomFieldValue(GFPFields.EVENT_KEY);
		String meClass = alarm.getOriginatingManagedEntity().split(" ")[0];
		PerceivedSeverity severity = alarm.getPerceivedSeverity();
		Boolean decomp = false;
		
		AlarmDelegationType delegateTo = AlarmDelegationType.FORWARD;
		String target = null;

		if(eventKey.equals("50003/100/24")) {
			delegateTo = AlarmDelegationType.CASCADE;
			target = "JUNIPER_COMPLETION";
		} else if(eventKey.equals("50002/100/21")) {
            		delegateTo = AlarmDelegationType.CASCADE;
            		target = "NTDTICKET_CORRELATION";
		}  else if(eventKey.equals("50003/100/1")) {
			delegateTo = AlarmDelegationType.CASCADE;
			if(meClass.equals("LPORT") || meClass.equals("PPORT"))
				target = "NTDTICKET_CORRELATION";
			else if(meClass.equals("DEVICE"))
				target = "JUNIPER_COMPLETION";
		} else if(eventKey.equals("50004/1/18") ||
				eventKey.equals("50004/1/19") ||
				eventKey.equals("50004/1/20") ||
				eventKey.equals("50004/1/21")) {
			decomp = true;
		}

		if (log.isTraceEnabled())
			log.trace("Sending " + eventKey + " to " + target);
		
		Util.sendAlarm(alarm, delegateTo, target, decomp);		

	}
	
	// isDecomposed is true ==> cascadeTargetName must be null a ==>
	// alarmDelegationType must be FORWARD as the the alarm is never
	// decomposed when cadcading to other VP 
	private static boolean sendAlarm(EnrichedAlarm alarm, AlarmDelegationType alarmDelegationType, String cascadeTargetName, boolean isDecomposed) 
	{
		if (log.isTraceEnabled())
			log.trace("Enter: sendAlarm()");
		Pri_Sec_Alarm ca = (Pri_Sec_Alarm) alarm;
		
		
		ca.timeBackToNormal();
		
		boolean ret = false;

		// this code is duplicated from LC because of unexplained null exception
		if(ca.getCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA) == null || 
				ca.getCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA).isEmpty())
			ca.setCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA, "NO");

		
		// if this is a UCA generated synthetic clear then we don't send it on except to NTD Ticket correlation.
		// We throw it out here because the Juniper completeion scenario do not store alams in WM
		// so there is no need to send this clear there.
		if("YES".equalsIgnoreCase(ca.getCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA)) &&
				ca.getPerceivedSeverity().equals(PerceivedSeverity.CLEAR) ) {
			if(cascadeTargetName != null)
				if(!cascadeTargetName.equals("NTDTICKET_CORRELATION")) {
					return false;
				}
		}

		// we don't decompose UCA generated clears
		if(!("YES".equalsIgnoreCase(alarm.getCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA)) &&
				ca.getPerceivedSeverity().equals(PerceivedSeverity.CLEAR))) {

			// we do decomp always unless the g2_suppression is set
			if (isDecomposed && !ca.getCustomFieldValue(GFPFields.G2SUPPRESS).equals("IPAG02"))
			{
				if (log.isDebugEnabled())
					log.debug("Decomposing alarm = " + ca.getIdentifier());

				GFPUtil.forwardAlarmToDecomposerInstance((EnrichedAlarm) ca, "JUNIPER_DECOMPOSER");
			} 

			//from the post-oct13 Adders doc from p8;
			//If there is no primary/standlone pport linkdown alarm, Suppress LAG alarm
			//this is the most convenient place to do this as all alarms come thru here
			if(ca.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50003/100/1") &&
					ca.getCustomFieldValue("hasRemoteEmux") != null &&
					!ca.getCustomFieldValue("hasRemoteEmux").isEmpty() &&
					ca.getCustomFieldValue("hasRemoteEmux").equals("true") &&
					(ca.getCustomFieldValue(GFPFields.SECONDARY_ALERT_ID) == null ||
					ca.getCustomFieldValue(GFPFields.SECONDARY_ALERT_ID).isEmpty())) {
				ca.setSuppressed(true);
				log.info("Alarm [" + ca.getCustomFieldValue(GFPFields.SEQNUMBER) + "] has been suppressed as a standalone Juniper LAG alarm : " +
						ca.getIdentifier());
			}
		}
		
		if(ca.getCanSend()) {			
			EnrichedAlarm alarmToCascade = null;
			
			// this is the first VP in the series so we only have to calculate how log the alarm was aged.
			alarm.SetAccumulativeTime();
			
			try {
				alarmToCascade = alarm.clone();

				alarmToCascade.setJustInserted(true);
				alarmToCascade.setAboutToBeRetracted(false);
				alarmToCascade.getVar().remove(PD_Service_Navigation.NEED_NAVIGATION_UPDATE);

				if (log.isTraceEnabled())
					log.trace("Sending alarm: " + alarm.getIdentifier());
				
				GFPUtil.forwardOrCascadeAlarm(alarmToCascade, alarmDelegationType, cascadeTargetName);
				ca.setIsSent(true);
				ret = true;
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
		}
		
		if (log.isTraceEnabled())
			log.trace("Exit: sendAlarm()");
		return ret;
	}

	public static void setTriggerWatch(Pri_Sec_Alarm alarm, Class<Pri_Sec_Alarm> ac, String cbName, long after, String watchdogDesc) {

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

		alarm.setNumberOfWatches(alarm.getNumberOfWatches() + 1);
		Scenario scenario = ScenarioThreadLocal.getScenario();
		scenario.addCallbackWatchdogItem(after, callback, false, watchdogDesc, true, alarm);
		
		if (log.isTraceEnabled())
			log.trace("create watchdog for trigger alarm = " + alarm.getIdentifier() + ", description = " + watchdogDesc + " for " + after);
	}
	
	public static void setCandidateWatch(Pri_Sec_Alarm alarm, Class<Pri_Sec_Alarm> ac, String cbName, long after, String watchdogDesc) {

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

		alarm.setNumberOfCandidateWatches(alarm.getNumberOfCandidateWatches() + 1);
		Scenario scenario = ScenarioThreadLocal.getScenario();
		scenario.addCallbackWatchdogItem(after, callback, false, watchdogDesc, true, alarm);
		
		if (log.isTraceEnabled())
			log.trace("create watchdog for candidate alarm = " + alarm.getIdentifier() + ", description = " + watchdogDesc + " for " + after);
	}
	
    public static void setGroupWatch(Group group, long after, String watchdogDesc) {

        Class<?> partypes[] = new Class[1];
        partypes[0] = Group.class;

        Object arglist[] = new Object[1];
        arglist[0] = group;
        Method method = null;
        
        try {
               method = Util.class.getMethod("groupCallBack", partypes);
        } catch (SecurityException e) {
               e.printStackTrace();
        } catch (NoSuchMethodException e) {
               e.printStackTrace();
        }
        
        Callback callback = new Callback(method, group, arglist);

        Scenario scenario = ScenarioThreadLocal.getScenario();
        scenario.addCallbackWatchdogItem(after, callback, false, watchdogDesc, true, group);
        
        if (log.isTraceEnabled())
               log.trace("create watchdog for group = " + group.getName() + ", description = " + watchdogDesc + " for " + after);
    }
    
    public static void groupCallBack(Group group) {
        if (log.isTraceEnabled())
               log.trace("groupCallBack() Enter:");

        for( Alarm alarm : group.getAlarmList()) {
        	if ( alarm.getCustomFieldValue("HasSecondary") != null && ! alarm.getCustomFieldValue("HasSecondary").equals("true") ) {
        		WhereToSendAndSend((EnrichedAlarm) alarm);
        	}
        }
    }

}
