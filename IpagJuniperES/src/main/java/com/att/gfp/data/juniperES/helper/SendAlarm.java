package com.att.gfp.data.juniperES.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.data.ipagAlarm.AlarmState;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;

public class SendAlarm {

	public static final String COMPLETION = "completion";
	public static final String CFM = "cfm";
	public static final String NTD = "ntd";
	public static final String VFR = "vfr";

	private static Logger log = LoggerFactory.getLogger(SendAlarm.class);


	/*	public SendAlarm(EnrichedAlarm newAlarm) {

	}*/

	public static void send(EnrichedAlarm alarm, String scenarioName) {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "send() SendAlarm class", alarm.toXMLString());  
		}

		// we do not forward UCA generated clears
		if("YES".equalsIgnoreCase(alarm.getCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA)) &&
				alarm.getPerceivedSeverity().equals(PerceivedSeverity.CLEAR) &&  !alarm.isSendToCdc()) {
			return;
		}

		// Cascading routes...
		//NTD -> CFM -> completion -> NOM
		//VFR -> PriSec
		//
		if(alarm.isSuppressed()) {
			log.info("Alarm [" + alarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "] has been suppressed by Juniper Completion : " + alarm.getIdentifier());
		} else {
			if(scenarioName == null) {
				
				alarm.SetAccumulativeTime();
				
				// forward alarm to NOM by default
				if (log.isTraceEnabled())
					log.trace("Forwarding alarm to NOM : " + alarm.getIdentifier()); 
				GFPUtil.forwardOrCascadeAlarm(alarm, AlarmDelegationType.FORWARD, null);
			} else
				if(scenarioName.equals(COMPLETION)) {
					// we are cascading the alarm to NOM if we are done with Completion
					GFPUtil.forwardOrCascadeAlarm(alarm, AlarmDelegationType.FORWARD, null);

					if (log.isTraceEnabled())
						log.trace("Cascading alarm to ... : " + alarm.getIdentifier());		
				}
				else if(scenarioName.equals(NTD)){
					if(alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50002/100/21") ||
							alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50001/100/7")) {  
						if (log.isTraceEnabled())
							log.trace("Forwarding alarm to NOM " + alarm.getIdentifier());  
						alarm.setAlarmState(AlarmState.ntdcorrelated);
						GFPUtil.forwardOrCascadeAlarm(alarm, AlarmDelegationType.FORWARD, null);     
					} else {
						if (log.isTraceEnabled())
							log.trace("Cascading alarm to Juniper Completion : " + alarm.getIdentifier());
						GFPUtil.forwardOrCascadeAlarm(alarm, AlarmDelegationType.CASCADE, "JUNIPER_COMPLETION");
					}
				}else if(scenarioName.equals(VFR)) {
					if (log.isTraceEnabled())
						log.trace("Cascading alarm to Juniper Pri/Sec Processing : " + alarm.getIdentifier());
					GFPUtil.forwardOrCascadeAlarm(alarm, AlarmDelegationType.CASCADE, "PRIMARYSECONDARY_CORRELATION");						
				}
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "send()");  
		}  
	}
}
