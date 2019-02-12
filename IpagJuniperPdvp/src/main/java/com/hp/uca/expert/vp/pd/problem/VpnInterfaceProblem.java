package com.hp.uca.expert.vp.pd.problem;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.data.ipagAlarm.EnrichedJuniperAlarm;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
import com.hp.uca.common.callback.Callback;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.scenario.ScenarioThreadLocal;
import com.hp.uca.expert.vp.pd.core.ProblemDefault;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;

/**
 * @author MASSE
 * 
 */
public final class VpnInterfaceProblem extends ProblemDefault implements
		ProblemInterface {
	
	private static final String _50003_100_7 = "50003/100/7";
	private static final String _50003_100_6 = "50003/100/6";
	private Logger log = LoggerFactory.getLogger(VpnInterfaceProblem.class);

	public VpnInterfaceProblem() {
		super();
	}
	
	@Override
	public boolean isMatchingCandidateAlarmCriteria(Alarm a) throws Exception {
		EnrichedJuniperAlarm ea = (EnrichedJuniperAlarm) a;
		// we will suppress the subalarms and send the trigger; don't need WDPool
		//Util.WDPool(ea, 0, getScenario());
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingCandidateAlarmCriteria() runs.");
		return true;
	}

	@Override
	public void whatToDoWhenSubAlarmIsAttachedToGroup(Alarm a, Group group)
			throws Exception {
		
		EnrichedJuniperAlarm ea = (EnrichedJuniperAlarm) a;
		EnrichedJuniperAlarm trigger = (EnrichedJuniperAlarm) group.getTrigger();
		
		// this is not the trigger, meaning we have a group created with at least two alarms
		// we suppress the trigger in this case
		if( ea != trigger) {
		//	if (! trigger.isSuppressed()) {
				log.info("VPN Alarm " + ea.getIdentifier() + " sequence # " + ea.getCustomFieldValue(GFPFields.SEQNUMBER) +
					" has been suppressed due to a related VPN alarm, sequence # " + trigger.getCustomFieldValue(GFPFields.SEQNUMBER) +
					" with the same VRF!!");
				ea.setSuppressed(true);
		//	}
			//Util.WDPool(ea, 0, getScenario());
		}
		
		Util.whereToSendThenSend(trigger);
		
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingSubAlarmCriteria()");
		
	}


	@Override
	public boolean isAllCriteriaForProblemAlarmCreation(Group group)
			throws Exception {
		return false;
	}

	/*
	@Override
	public boolean isInformationNeededAvailable(Alarm alarm) throws Exception {
		boolean ret = false;
		
		if(alarm.getCustomFieldValue(GFPFields.REASON_CODE) != null &&
			!alarm.getCustomFieldValue(GFPFields.REASON_CODE).isEmpty())
			ret=true;
		
		return ret;
	}
	*/
	
	 @Override
	 public List<String> computeProblemEntity(Alarm a) throws Exception {

		 List<String> problemEntities = new ArrayList<String>();
		 		 
		 problemEntities.add(a.getCustomFieldValue("vrf-name"));
		 
		 if (log.isTraceEnabled())
			 LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", computeProblemEntity() --- problemEntities=" + problemEntities);
		 
		 return problemEntities;
	 } 
	
 /*
	 @Override
     public boolean isMatchingTriggerAlarmCriteria(Alarm a) throws Exception {
    	 boolean ret = false;

    	 if (log.isTraceEnabled())
    		 log.trace("isMatchingTriggerAlarmCriteria(): " + a.getIdentifier());
    	 if ((a instanceof EnrichedJuniperAlarm)) {
    		 if (log.isTraceEnabled())
    			 log.trace("isMatchingTriggerAlarmCriteria(): creating callback");
    		 EnrichedJuniperAlarm alarm = (EnrichedJuniperAlarm) a;    

    		 // watch to send the trigger alarm if no subalarms arrive
    		 Class<?> partypes[] = new Class[0];
    		 Object arglist[] = new Object[0];
    		 Method method = EnrichedJuniperAlarm.class.getMethod("vpnInterfaceProblemCallback",
    				 partypes);
    		 Callback callback = new Callback(method, alarm, arglist);

    		 // get the after trigger time and set this watchdog with that value
    		 // if no subalarms are added before this, we know that the group will 
    		 // not form and we can cascade this alarm with no change.
    		 long after = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger();

    		 // to make sure...
    		 if(after == 0 )
    			 after = after + 1000;

    		 Scenario scenario = ScenarioThreadLocal.getScenario();
    		 scenario.addCallbackWatchdogItem(after, callback, false,
    				 "Expiration WatchdogItem", true, alarm);
    		 if (log.isTraceEnabled())
    			 log.trace("isMatchingTriggerAlarmCriteria(): created callback with delay of " +
    					 after);
    		 ret = true;
    	 }
    	 if (log.isTraceEnabled())
    		 LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingTriggerAlarmCriteria() " + "[" + ret + "]");
    	 return ret;
     }
*/
 
/*	 
    @Override
 	public void whatToDoWhenSubAlarmIsCleared(Alarm a, Group group) throws Exception {
    	if (log.isTraceEnabled())
    		LogHelper.enter(log, "alarm = " + a.getIdentifier() + ", group = " + group.getName() + ", group # = " + group.getNumber() + ", whatToDoWhenSubAlarmIsAttachedToGroup()");

 		if (a instanceof EnrichedJuniperAlarm) {
 			EnrichedJuniperAlarm alarm = (EnrichedJuniperAlarm) a;
 			if (alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50003_100_6) ||
 				alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50003_100_7)) {
  				if (alarm.isSuppressed()) {
 					log.info("Alarm " + alarm.getIdentifier() + 
						" was suppressed; not cascading clear");
 				} else {
 					if (log.isTraceEnabled())
 						log.trace("Cascading Clear to Juniper Completion: " + alarm.toString());
					Util.whereToSendThenSend((EnrichedAlarm)alarm);
 				}
 			}
 		}
 	}
 */
	 
}
