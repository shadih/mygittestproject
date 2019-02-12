package com.hp.uca.expert.vp.pd.problem;

import org.mortbay.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.actions.PriSecActionsFactory;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.data.ipagAlarm.Pri_Sec_Alarm;
import com.att.gfp.helper.GFPFields;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;

public class ProcessJuniperLD_LAG {

	private static final Logger LOG = LoggerFactory
			.getLogger(PriSecActionsFactory.class);
	

	/**
	 * 
	 */
	public ProcessJuniperLD_LAG() {

	}

	public void processJuniperLD_OAM_LAG(Group group) {

		if (LOG.isTraceEnabled()) {
			LogHelper.enter(LOG, "processJuniperLD_OAM_LAG()", group.getName());
		}

		boolean have_Primary = false;
		Pri_Sec_Alarm lagAlarm = null; 

		Pri_Sec_Alarm sAlarm = null;
		Pri_Sec_Alarm tAlarm = (Pri_Sec_Alarm) group.getTrigger();

		if(tAlarm.getCanSend()) {
			for( Alarm alarm : group.getAlarmList()) {
				if(alarm != tAlarm) {
					if(((Pri_Sec_Alarm) alarm).getCanSend()) {
						if(alarm.getOriginatingManagedEntity().split(" ")[0].equals("PPORT")) {
							// mark and send the subalarm					
							LOG.info("Setting this PPORT LinkDown alarm Primary to LAG LinkDown (alert-id = " + alarm.getIdentifier() + 
									" & sequence-number = " + alarm.getCustomFieldValue(GFPFields.SEQNUMBER) + ")");

							alarm.setCustomFieldValue("HasSecondary", "true");
							// df gfpc140343 
							alarm.setCustomFieldValue(GFPFields.SECONDARY_ALERT_ID, "1");
							alarm.setCustomFieldValue(GFPFields.SECONDARYTIMESTAMP, "");
							alarm.setCustomFieldValue("sent_to_ruby", "true");
							Util.WhereToSendAndSend((EnrichedAlarm) alarm);
							LOG.trace("The primary alarm was sent: " + alarm.toString());
							sAlarm = (Pri_Sec_Alarm) alarm;
							have_Primary = true;
							break;
						} else {
							lagAlarm = (Pri_Sec_Alarm) alarm;
						}
					}
				}
			}
			// make sure we actually have a primary
			if(have_Primary) {
				// mark and send the trigger
				LOG.info("Setting this LAG LinkDown alarm Secondary to PPORT LinkDown (alert-id = " + tAlarm.getIdentifier() + 
						" & sequence-number = " + tAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + ")");

				tAlarm.setCustomFieldValue(GFPFields.SECONDARY_ALERT_ID, sAlarm.getCustomFieldValue(GFPFields.ALERT_ID));
				tAlarm.setCustomFieldValue(GFPFields.SECONDARYTIMESTAMP, sAlarm.getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP));
				tAlarm.setCustomFieldValue("sent_to_ruby", "true");
                long holdTime = 180000; 
                
                String watchdogDesc = "Watchdog for:" + group.getName();
         
                Util.setGroupWatch(group, holdTime, watchdogDesc);

				//Util.WhereToSendAndSend((EnrichedAlarm) tAlarm);
				if (LOG.isTraceEnabled())
					LOG.trace("The secondary alarm was sent: " + tAlarm.toString());
			} else {
				//if both alarm are LAG LD and neither is a sub interface then suppress the second one (trigger)
				if(tAlarm.getOriginatingManagedEntity().split(" ")[0].equals("DEVICE") && lagAlarm != null &&
						!tAlarm.isLagSubInterfaceLinkDown() && !lagAlarm.isLagSubInterfaceLinkDown() &&
						tAlarm.getIfIndex().equals(lagAlarm.getIfIndex()) &&
						tAlarm.getOriginatingManagedEntity().split(" ")[1].equals(lagAlarm.getOriginatingManagedEntity().split(" ")[1])) {
					tAlarm.setSuppressed(true);
					Log.info("Alarm " + tAlarm.getIdentifier() + " with seq Number " + tAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + " has been suppress by LAG-LAG Local/remote");
				}

			}
		}

		if (LOG.isTraceEnabled()) {
			LogHelper.exit(LOG, "processJuniperLD_OAM_LAG()", " for trigger: " + tAlarm.getIdentifier());
		}

	}
}
