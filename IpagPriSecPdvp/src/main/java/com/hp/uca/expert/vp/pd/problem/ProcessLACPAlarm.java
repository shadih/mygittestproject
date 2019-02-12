package com.hp.uca.expert.vp.pd.problem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.actions.PriSecActionsFactory;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.helper.GFPFields;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;

public class ProcessLACPAlarm {

	private static final String _50003_100_24 = "50003/100/24";
	private static final String _50004_1_24 = "50004/1/24";
	private static final String _50003_100_4 = "50003/100/4";
	private static final String _50003_100_3 = "50003/100/3";
	private static final String _50003_100_1 = "50003/100/1";
	private static final String _50004_1_21 = "50004/1/21";
	private static final String _50004_1_39 = "50004/1/39";
	private static final String _50004_1_20 = "50004/1/20";
	private static final String _50004_1_19 = "50004/1/19";
	private static final String _50004_1_18 = "50004/1/18";
	private static final String _50004_1_10 = "50004/1/10";
	private static final String _50003_100_21 = "50003/100/21";

	/**
	 * Logger used to trace this component
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(PriSecActionsFactory.class);


	/**
	 * 
	 */
	public ProcessLACPAlarm() {

	}

	public void processLACPAlarm(Group group) {

		if (LOG.isTraceEnabled()) {
			LogHelper.enter(LOG, "processLACPAlarm()", group.getName());
		}

		Alarm alarm_50003_100_1 = null;
		Alarm alarm_50004_1_10 = null;
		Alarm other_alarm_50004_1_10 = null;
		Alarm alarm_50004_1_18 = null;
		Alarm alarm_50004_1_19 = null;
		Alarm alarm_50004_1_20 = null;
		Alarm alarm_50004_1_21 = null;
		Alarm alarm_50004_1_39 = null;
		Alarm alarm_50004_1_24 = null;
		Alarm primaryAlarm =  null;

		// this is done so eventually we can find out which _50004_1_10 alarm is remote
		// we will treat it as remote if it doesn't have the same MO as the other alarms
		// in the group/s.  the remote alarm will be secondary and not sent to ruby
		String mo = null;

		if (LOG.isTraceEnabled())
			LOG.trace("Processing Group: " + group.getName());

		// find out what we have in this group
		for (Alarm alarm : group.getAlarmList()) {	
			if (LOG.isTraceEnabled())
				LOG.trace("   Processing alarm: " + alarm.getIdentifier());

			if (alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50003_100_1)) {
				alarm_50003_100_1 = alarm;
				mo = alarm.getOriginatingManagedEntity();
			} else if (alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50004_1_10)) {
				if (alarm_50004_1_10 != null) {
					// here we have the situation where this alarm is on the
					// local and
					// remote sides.
					other_alarm_50004_1_10 = alarm;
				} else {
					alarm_50004_1_10 = alarm;
				}
			} else if (alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50004_1_19)) {
				mo = alarm.getOriginatingManagedEntity();
				alarm_50004_1_19 = alarm;
			} else if (alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50004_1_18)) {
				mo = alarm.getOriginatingManagedEntity();
				alarm_50004_1_18 = alarm;
			} else if (alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50004_1_20)) {
				mo = alarm.getOriginatingManagedEntity();
				alarm_50004_1_20 = alarm;
			} else if (alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50004_1_21)) {
				mo = alarm.getOriginatingManagedEntity();
				alarm_50004_1_21 = alarm;
			} else if (alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50004_1_39)) {
				mo = alarm.getOriginatingManagedEntity();
				alarm_50004_1_39 = alarm;
			} else if (alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50003_100_24)) {
				LOG.trace("Found a 50003/1/24 alarm.");
				mo = alarm.getOriginatingManagedEntity();
				alarm_50004_1_24 = alarm;
			}
		}

		// here is the case where we have only local and remote 50004/1/10 alarms
		// so we pick the first to arrive as the primary and the second to arrive as the 
		// secondary
		if(mo==null && alarm_50004_1_10 !=null && other_alarm_50004_1_10 !=null) {
			if(Long.parseLong(alarm_50004_1_10.getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP)) >
			Long.parseLong(other_alarm_50004_1_10.getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP))) {
				mo = other_alarm_50004_1_10.getOriginatingManagedEntity();
				primaryAlarm = other_alarm_50004_1_10;
			} else {
				primaryAlarm = alarm_50004_1_10;
				mo = alarm_50004_1_10.getOriginatingManagedEntity();
			}			
		}

		// find the primary alarm
		if(primaryAlarm == null) {				
			// first find the primary alarm based on the priority or as listed in the requirements
			//	50003/100/1 - this is already assigned from above
			//	50004/1/10
			//	50004/1/19 or 50004/1/21 or 50004/1/39 - the first to arrive
			//	50004/1/18 or 50004/1/20 - the first to arrive				
			if (alarm_50003_100_1 != null)
				primaryAlarm = alarm_50003_100_1;
			else
				// here the 50004_1_10 is the primary if it has the same mo as the
				// others
				// in the group/s/
				if (alarm_50004_1_10 != null && alarm_50004_1_10.getOriginatingManagedEntity().equals(mo))
					primaryAlarm = alarm_50004_1_10;
				else if (other_alarm_50004_1_10 != null && other_alarm_50004_1_10.getOriginatingManagedEntity().equals(mo))
					primaryAlarm = other_alarm_50004_1_10;
				else if (alarm_50004_1_19 != null || alarm_50004_1_21 != null || alarm_50004_1_39 != null ) {
					long alarm_50004_1_19_bets = 9999999999L;
					long alarm_50004_1_21_bets = 9999999999L;
					long alarm_50004_1_39_bets = 9999999999L;

					if (alarm_50004_1_19 != null ) {
						alarm_50004_1_19_bets = Long.parseLong(alarm_50004_1_19.getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP));
					}
					if (alarm_50004_1_21 != null ) {
						alarm_50004_1_21_bets = Long.parseLong(alarm_50004_1_21.getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP));
					}
					if (alarm_50004_1_39 != null ) {
						alarm_50004_1_39_bets = Long.parseLong(alarm_50004_1_39.getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP));
					}

					if(alarm_50004_1_19_bets < alarm_50004_1_21_bets && alarm_50004_1_19_bets < alarm_50004_1_39_bets) {
						primaryAlarm = alarm_50004_1_19;
					} else if(alarm_50004_1_21_bets < alarm_50004_1_39_bets && alarm_50004_1_21_bets < alarm_50004_1_19_bets) {
						primaryAlarm = alarm_50004_1_21;
					} else {
						primaryAlarm = alarm_50004_1_39;
					}

				}
			/*				else if (alarm_50004_1_19 != null && alarm_50004_1_21 != null) {
					if (Long.parseLong(alarm_50004_1_19.getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP)) > Long
							.parseLong(alarm_50004_1_21.getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP)))
						primaryAlarm = alarm_50004_1_21;
					else
						primaryAlarm = alarm_50004_1_19;
				} else if (alarm_50004_1_19 != null)
					primaryAlarm = alarm_50004_1_19;
				else if (alarm_50004_1_21 != null)
					primaryAlarm = alarm_50004_1_21;*/
				else if (alarm_50004_1_18 != null && alarm_50004_1_20 != null) {
					if (Long.parseLong(alarm_50004_1_18.getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP)) > Long
							.parseLong(alarm_50004_1_20.getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP)))
						primaryAlarm = alarm_50004_1_20;
					else
						primaryAlarm = alarm_50004_1_18;
				} else if (alarm_50004_1_18 != null)
					primaryAlarm = alarm_50004_1_18;
				else if (alarm_50004_1_20 != null)
					primaryAlarm = alarm_50004_1_20;
		}				

		if(primaryAlarm == null){
			if (LOG.isTraceEnabled()) {
				LOG.trace("Could not find the primary alarm !!!");

				LOG.trace("This group name:" + group.getName());
			}

			// send out all of the alarm in the group
			for (Alarm alarm : group.getAlarmList()) {
				if (LOG.isTraceEnabled())
					LOG.trace("Alarm in group:" + alarm.getIdentifier() + " event key:" + alarm.getCustomFieldValue(GFPFields.EVENT_KEY));

				if(alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50003_100_24))
					Util.WhereToSendAndSend((EnrichedAlarm)alarm);
				else
					Util.WhereToSendAndSend((EnrichedAlarm)alarm);
			}
			return;
		}	

		if (LOG.isTraceEnabled())
			LOG.trace("Found primary alarm: " + primaryAlarm.getIdentifier());

		// was the primary sent already
		// 	no, update the primary and send it
		// here I am using the SentAsTriggerAlarm field for the primary instead of inventing another
		primaryAlarm.setCustomFieldValue("HasSecondary", "true");
		// df gfpc140343 
		primaryAlarm.setCustomFieldValue(GFPFields.SECONDARY_ALERT_ID, "1");
		primaryAlarm.setCustomFieldValue(GFPFields.SECONDARYTIMESTAMP, "");
		if (LOG.isTraceEnabled())
			LOG.trace("The primary alarm was sent: " + primaryAlarm.toString());

		LOG.info("Alarm " + primaryAlarm.getIdentifier() + " Sequence # " + primaryAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) +
				" is a primary alarm.");

		// check if _50004_1_39 is a secondary to any primary; if so, set primary's secondaryAlarmName to name of _50004_1_39
		// from _50004_1_39's reason_code

		EnrichedAlarm priEA = (EnrichedAlarm) primaryAlarm;
		if ( priEA.isSendToCdc() ) {
			priEA.setCustomFieldValue("secondaryAlarmName", "");
			for (Alarm subAlarm : group.getAlarmList()) {
				if(subAlarm != primaryAlarm && subAlarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50004_1_39) ) {

					priEA.setCustomFieldValue("secondaryAlarmName", subAlarm.getCustomFieldValue(GFPFields.REASON_CODE));	

					LOG.info("Found 50004/1/39 Secondary alarm: " + subAlarm.getIdentifier() + " Sequence # " + 
							subAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + " to primary: " + primaryAlarm.getIdentifier() + 
							" with Sequence # " + primaryAlarm.getCustomFieldValue(GFPFields.SEQNUMBER)
							+ ". Setting primary's secondaryAlarmName to: " + subAlarm.getCustomFieldValue(GFPFields.REASON_CODE));
					break;
				}
			}
		}

		// send out the primary
		Util.WhereToSendAndSend(priEA);

		if (LOG.isTraceEnabled())
			LOG.trace("Processing secondary alarm/s. " );
		// Send out the secondaries
		if (primaryAlarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50003_100_3) && 
				primaryAlarm.getOriginatingManagedEntity().split(" ")[0].equals("PPORT")  ) {

			long holdTime = 180000; 
			String watchdogDesc = "Watchdog for:" + group.getName();
			Util.setGroupWatch(group, holdTime, watchdogDesc);
		}
		else {
			for (Alarm subAlarm : group.getAlarmList()) {
				if(subAlarm != primaryAlarm) {
					subAlarm.setCustomFieldValue(GFPFields.SECONDARY_ALERT_ID, primaryAlarm.getCustomFieldValue(GFPFields.ALERT_ID));
					subAlarm.setCustomFieldValue(GFPFields.SECONDARYTIMESTAMP, primaryAlarm.getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP));
					// do not send _50004_1_39 to CDC when it's a secondary and not clear
					if ( subAlarm.getPerceivedSeverity() != PerceivedSeverity.CLEAR && subAlarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50004_1_39) ) {
						EnrichedAlarm secEA = (EnrichedAlarm) subAlarm;
						secEA.setSendToCdc(false);
						Util.WhereToSendAndSend(secEA);
					} else {
						Util.WhereToSendAndSend((EnrichedAlarm)subAlarm);	
					}

					LOG.info("Alarm " + subAlarm.getIdentifier() + " Sequence # " + subAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) +
							" is a secondary to " + primaryAlarm.getIdentifier() + ".");

				}
			}
		}

		if (LOG.isTraceEnabled()) {
			LogHelper.exit(LOG, "processLACPAlarm()", group.getName());
		}

	}


}

