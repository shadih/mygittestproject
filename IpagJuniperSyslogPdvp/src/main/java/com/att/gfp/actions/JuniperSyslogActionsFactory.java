/**
 * 
 */
package com.att.gfp.actions;

import java.util.Collection;

import org.mortbay.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.data.ipagAlarm.SyslogAlarm;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.StandardFields;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.group.Qualifier;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.vp.pd.actions.ActionsFactory;
import com.hp.uca.expert.vp.pd.config.Longs;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;
import com.hp.uca.expert.vp.pd.interfaces.SupportedActions;
import com.hp.uca.expert.vp.pd.problem.Util;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;
import com.hp.uca.mediation.action.client.Action;

/**
 * @author MASSE
 * 
 */
public class JuniperSyslogActionsFactory extends ActionsFactory implements
		SupportedActions {

	private static final String _50003_100_1 = "50003/100/1";
	private static final String _50004_1_21 = "50004/1/21";
	private static final String _50004_1_20 = "50004/1/20";
	private static final String _50004_1_19 = "50004/1/19";
	private static final String _50004_1_18 = "50004/1/18";
	private static final String _50004_1_10 = "50004/1/10";
	private static final String _50003_100_24 = "50003/100/24";
	private static final String _50004_1_12 = "50004/1/12";

	private static final String THRESHOLD_KEY = "SAVPNSITE-AGGREGATION-COUNT-THRESHOLD";

	private String vpName;
	private String vpVersion;
	private String scenarioName;

	/**
	 * Logger used to trace this component
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(JuniperSyslogActionsFactory.class);
	private static final Object _50003_1_24 = null;

	/**
	 * 
	 */
	public JuniperSyslogActionsFactory() {

	}

	/**
	 * TODO
	 */
	@Override
	public final Action clearAlarm(Action action, Scenario scenario,
			Alarm alarm, ProblemInterface problem) throws Exception {

		if (LOG.isTraceEnabled()) {
			LogHelper.enter(LOG, "clearAlarm()",
					alarm.getIdentifier());
		}

		if (LOG.isTraceEnabled()) {
			LogHelper.exit(LOG, "clearAlarm()",
					alarm.getIdentifier());
		}

		return null;
	}


	/**
	 * TODO
	 */
	@Override
	public final Action setHistoryNavigation(Action action, Scenario scenario,
			Alarm alarm, Qualifier qualifier) throws Exception {

		if (LOG.isTraceEnabled()) {
			LogHelper.enter(LOG, "setHistoryNavigation()",
					alarm.getIdentifier());
		}
		
		switch (qualifier) {
		case No:
                    // When a trigger does not arrive for a candidate alarm then it is an orphan
                    // and we send it along OR if the trigger is cleared before the callback we send the alarms here.
            if(alarm.getCustomFieldValue(StandardFields.NAVIGATION_FIELD) != null) {
			    if((alarm.getCustomFieldValue(StandardFields.NAVIGATION_FIELD).equals("Candidate") || 
                         alarm.getCustomFieldValue(StandardFields.NAVIGATION_FIELD).equals("SubAlarm")))
			    	Util.whereToSendThenSend((EnrichedAlarm) alarm, false); 
			
		    }
			alarm.setCustomFieldValue(StandardFields.NAVIGATION_FIELD,
					StandardFields.EMPTY_CUSTOM_FIELD);
			break;
		case Candidate:
		case ProblemAlarm:
		case SubProblemAlarm:
			alarm.setCustomFieldValue(StandardFields.NAVIGATION_FIELD,
					qualifier.toString());
			break;
		default:
			alarm.setCustomFieldValue(StandardFields.NAVIGATION_FIELD,
					qualifier.toString());
			break;
		}	
		
		if (LOG.isTraceEnabled()) {
			LogHelper.method(LOG, "setHistoryNavigation() -- " + qualifier.toString());
		}

		if (LOG.isTraceEnabled()) {
			LogHelper.exit(LOG, "setHistoryNavigation()",
					alarm.getIdentifier());
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.hp.uca.expert.vp.pd.interfaces.SupportedActions#terminateAlarm(com
	 * .hp.uca.expert.scenario.Scenario, com.hp.uca.expert.alarm.Alarm,
	 * com.hp.uca.expert.vp.pd.interfaces.CustomInterface)
	 */
	@Override
	public final Action terminateAlarm(Action action, Scenario scenario,
			Alarm alarm, ProblemInterface problem) throws Exception {
		/*
		 * Not used in the customization
		 */
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.hp.uca.expert.vp.pd.interfaces.SupportedActions#acknowledgeAlarm(
	 * com.hp.uca.expert.scenario.Scenario, com.hp.uca.expert.alarm.Alarm,
	 * com.hp.uca.expert.vp.pd.interfaces.CustomInterface)
	 */
	@Override
	public final Action acknowledgeAlarm(Action action, Scenario scenario,
			Alarm alarm, ProblemInterface problem) throws Exception {
		/*
		 * Not used in the customization
		 */
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.hp.uca.expert.vp.pd.interfaces.SupportedActions#unacknowledgeAlarm
	 * (com.hp.uca.expert.scenario.Scenario, com.hp.uca.expert.alarm.Alarm,
	 * com.hp.uca.expert.vp.pd.interfaces.CustomInterface)
	 */
	@Override
	public final Action unacknowledgeAlarm(Action action, Scenario scenario,
			Alarm alarm, ProblemInterface problem) throws Exception {
		/*
		 * Not used in the customization
		 */
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.uca.expert.vp.pd.interfaces.SupportedActions#
	 * dissociateAlarmsForHistoryNavigation(com.hp.uca.expert.scenario.Scenario,
	 * com.hp.uca.expert.group.Group, java.util.Collection,
	 * com.hp.uca.expert.vp.pd.interfaces.CustomInterface)
	 */
	@Override
	public final Action dissociateAlarmsForHistoryNavigation(Action action,
			Scenario scenario, Group group, Collection<Alarm> children,
			ProblemInterface problem) throws Exception {
		/*
		 * Not used in the customization
		 */
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.uca.expert.vp.pd.actions.ActionsFactory#computeLongs()
	 */
	@Override
	public void computeLongs() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.uca.expert.vp.pd.actions.ActionsFactory#computeBooleans()
	 */
	@Override
	public void computeBooleans() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.uca.expert.vp.pd.actions.ActionsFactory#computeStrings()
	 */
	@Override
	public void computeStrings() {
	}

	/**
	 * @return the vpName
	 */
	public final String getVpName() {
		return vpName;
	}

	/**
	 * @return the vpVersion
	 */
	public final String getVpVersion() {
		return vpVersion;
	}

	/**
	 * @return the scenarioName
	 */
	public final String getScenarioName() {
		return scenarioName;
	}

	@Override
	public Action associateAlarmsForHistoryNavigation(Action action,
			Scenario scenario, Group group, Collection<Alarm> children,
			ProblemInterface problem) throws Exception {
		// TODO Auto-generated method stub
		
		if (LOG.isTraceEnabled()) {
			LogHelper.enter(LOG, "associateAlarmsForHistoryNavigation()");
		}

	

		if (LOG.isTraceEnabled()) {
			LogHelper.exit(LOG, "associateAlarmsForHistoryNavigation()");
		}

		return null;
	}

	@Override
	public Action createProblemAlarm(Action action, Scenario scenario,
			Group group, ProblemInterface problem, Alarm referenceAlarm)
					throws Exception {

		if (LOG.isTraceEnabled()) {
			LogHelper.enter(LOG, "createProblemAlarm()", group.getName());
		}

//		if(isLACPAlarm(group.getTrigger().getCustomFieldValue(GFPFields.EVENT_KEY))) {
//			processLACPAlarm(group);			
//		} else {
//			if(group.getTrigger().getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50004_1_12))
//				processBFDownAlarm(group, referenceAlarm);
//			else {

				// Clone the Trigger Alarm to build the new alarm to send 
				SyslogAlarm a =(SyslogAlarm) referenceAlarm.clone();

				// if the trigger is not to be suppressed and if we already haven't sent it
				// then send it.
				Util.whereToSendThenSend(a, false);
//			}
		//}
		if (LOG.isTraceEnabled()) {
			LogHelper.exit(LOG, "createProblemAlarm()",
					group.getTrigger().getIdentifier());
		}

		return null;
	}

	
	
	//convert to an Int
	public int longToInt(Longs myThreshold)
	{ 
		Integer x=0;
		try { 
			x = Integer.valueOf(myThreshold.toString()); 
		} catch(IllegalArgumentException e) { 
			LOG.trace("Long to Interger Error");
		}
		return x;
	}

/*	private void processLACPAlarm(Group group) {

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

				if(alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50003_100_1)) {
					alarm_50003_100_1 = alarm;
					mo = alarm.getOriginatingManagedEntity();
				} else
					if(alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50004_1_10)) {
						if(alarm_50004_1_10 != null) {
							// here we have the situation where this alarm is on the local and
							// remote sides.
							other_alarm_50004_1_10 = alarm;						
						} else
							alarm_50004_1_10 = alarm;
					} else
						if(alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50004_1_19)) {
							mo = alarm.getOriginatingManagedEntity();
							alarm_50004_1_19 = alarm;
						} else
							if(alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50004_1_18)) {
								mo = alarm.getOriginatingManagedEntity();
								alarm_50004_1_18 = alarm;
							} else
								if(alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50004_1_20)) {
									mo = alarm.getOriginatingManagedEntity();
									alarm_50004_1_20 = alarm;
								} else
									if(alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50004_1_21)) {
										mo = alarm.getOriginatingManagedEntity();
										alarm_50004_1_21 = alarm;
									}
		}

		// here is the case where we have only local and remote 50004/1/10 alarms
		// so we pick one mo
		if(mo==null && alarm_50004_1_10!=null && other_alarm_50004_1_10!=null)
			mo = alarm_50004_1_10.getOriginatingManagedEntity();
		
		// find the primary alarm
		if(primaryAlarm == null) {				
			// first find the primary alarm based on the priority or as listed in the requirements
			//	50003/100/1 - this is already assigned from above
			//	50004/1/10
			//	50004/1/19 or 50004/1/21 - the first to arrive
			//	50004/1/18 or 50004/1/20 - the first to arrive				
			if(alarm_50003_100_1 != null)
				primaryAlarm = alarm_50003_100_1;
			else
				// here the 50004_1_10 is the primary if it has the same mo as the others 
				// in the group/s/
				if(alarm_50004_1_10 != null && alarm_50004_1_10.getOriginatingManagedEntity().equals(mo))
						primaryAlarm = alarm_50004_1_10;
				else
					if(other_alarm_50004_1_10 != null && other_alarm_50004_1_10.getOriginatingManagedEntity().equals(mo))
							primaryAlarm = other_alarm_50004_1_10;
					else
						if(alarm_50004_1_19 != null && alarm_50004_1_21 != null) {
							if(Long.parseLong(alarm_50004_1_19.getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP)) >
							Long.parseLong(alarm_50004_1_21.getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP)))
								primaryAlarm = alarm_50004_1_21;
							else
								primaryAlarm = alarm_50004_1_19;
						} else
							if(alarm_50004_1_19 != null)
								primaryAlarm = alarm_50004_1_19;
							else
								if(alarm_50004_1_21 != null)
									primaryAlarm = alarm_50004_1_21;
								else
									if(alarm_50004_1_18 != null && alarm_50004_1_20 != null) {
										if(Long.parseLong(alarm_50004_1_18.getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP)) >
										Long.parseLong(alarm_50004_1_20.getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP)))
											primaryAlarm = alarm_50004_1_20;
										else
											primaryAlarm = alarm_50004_1_18;
									} else
										if(alarm_50004_1_18 != null)
											primaryAlarm = alarm_50004_1_18;
										else
											if(alarm_50004_1_20 != null)
												primaryAlarm = alarm_50004_1_20;
		}				

		if(primaryAlarm == null){
			if (LOG.isDebugEnabled())
				LOG.debug("Could not find the primary alarm !!!");
			
			if (LOG.isDebugEnabled())
				LOG.debug("This group name:" + group.getName());
			
			// send out all of the alarm in the group
			for (Alarm alarm : group.getAlarmList()) {
				if (LOG.isTraceEnabled())
					LOG.trace("Alarm in group:" + alarm.getIdentifier() + " event key:" + alarm.getCustomFieldValue(GFPFields.EVENT_KEY));

				if(alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50003/100/24"))
					Util.whereToSendThenSend((EnrichedAlarm)alarm, false);
				else
					Util.whereToSendThenSend((EnrichedAlarm)alarm, false);
			}
			return;
		}	
		
		if (LOG.isTraceEnabled())
			LOG.trace("Found primary alarm: " + primaryAlarm.getIdentifier());

		// was the primary sent already
		// 	no, update the primary and send it
		// here I am using the SentAsTriggerAlarm field for the primary instead of inventing another
			primaryAlarm.setCustomFieldValue(GFPFields.SECONDARY_ALERT_ID, "1");
			primaryAlarm.setCustomFieldValue(GFPFields.SECONDARYTIMESTAMP, "0");
			if (LOG.isDebugEnabled())
				LOG.debug("The primary alarm was sent: " + primaryAlarm.toString());

			// send out the primary
			Util.whereToSendThenSend((EnrichedAlarm) primaryAlarm, false);

		// see which alarms arrived after the primary, mark them, send them if not already sent	
		// here we are using SentAsSubAlarm for the secondary instead of inventing another 
		boolean oneWasSent = false;
		if (LOG.isTraceEnabled())
			LOG.trace("Processing secondary alarms. " );

		for (Alarm alarm : group.getAlarmList()) {
			// if any in this group were sent then we know that the correlation is done and
			// we can ignore the second case below
			if (LOG.isTraceEnabled())
				LOG.trace("Processing secondary alarms: " + alarm.getIdentifier());

			oneWasSent = ((SyslogAlarm) alarm).getIsSent() | oneWasSent; 
			
			if(alarm != primaryAlarm &&
					Long.parseLong(primaryAlarm.getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP)) <
			Long.parseLong(alarm.getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP)))  {	

				alarm.setCustomFieldValue(GFPFields.SECONDARY_ALERT_ID, primaryAlarm.getCustomFieldValue(GFPFields.ALERT_ID));
				alarm.setCustomFieldValue(GFPFields.SECONDARYTIMESTAMP, primaryAlarm.getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP));
				Util.whereToSendThenSend((EnrichedAlarm)alarm, false);
				oneWasSent = true;
			}			
		}
		
		// here we have the second case where there was no primary alarm before the (secondary) trigger and then finally
		// within one minute after the trigger (secondary) there came the primary.
		// so here is where we are covering that case
		if(!oneWasSent) {
			if (LOG.isTraceEnabled())
				LOG.trace("here we have the second case where there was no primary alarm before the (secondary) trigger " );

			for (Alarm alarm : group.getAlarmList()) {	
				if (LOG.isTraceEnabled())
					LOG.trace("Processing alarm: " + alarm.getIdentifier());
				
				if(alarm != primaryAlarm) {	
					alarm.setCustomFieldValue(GFPFields.SECONDARY_ALERT_ID, primaryAlarm.getCustomFieldValue(GFPFields.ALERT_ID));
					alarm.setCustomFieldValue(GFPFields.SECONDARYTIMESTAMP, primaryAlarm.getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP));
					Util.whereToSendThenSend((EnrichedAlarm)alarm, false);
				}			
			}		
		}

		if (LOG.isTraceEnabled()) {
			LogHelper.exit(LOG, "processLACPAlarm()", group.getName());
		}

	}*/

	/*public static boolean isLACPAlarm(String key)
	{
	    String[] LACPkeys = {_50003_100_24,_50004_1_10,_50004_1_18,_50004_1_19,_50004_1_20,_50004_1_21};  
	    return (Arrays.asList(LACPkeys).contains(key));
	}*/
	
}
