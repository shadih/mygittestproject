package com.hp.uca.expert.vp.pd.problem;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.data.ipagAlarm.SyslogAlarm;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
import com.hp.uca.common.callback.Callback;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.localvariable.LocalVariable;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.scenario.ScenarioThreadLocal;
import com.hp.uca.expert.vp.pd.core.ProblemDefault;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;
import com.hp.uca.expert.vp.pd.services.PD_Service_ProblemAlarm;


/** 
 * @author MASSE
 * 
 */
public final class RpdMplsLspProblem extends ProblemDefault implements
		ProblemInterface {
	
	private Logger log = LoggerFactory.getLogger(RpdMplsLspProblem.class);

	public RpdMplsLspProblem() {
		super();
	}
	
	@Override
	public boolean isMatchingTriggerAlarmCriteria(Alarm a) throws Exception {
		boolean ret = false;

		SyslogAlarm alarm = (SyslogAlarm) a;	
		long after = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger();
		String watchdogDesc = "RpdMplsLspProblem watch dog.";
		Util.setTriggerWatch((SyslogAlarm)a, SyslogAlarm.class, "rpdRsvpByPassDownexpirationCallBack", after, watchdogDesc);
		ret = true; 

		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingTriggerAlarmCriteria() " + "[" + ret + "]");
		return ret;
	} 
	 

	@Override
	public boolean isMatchingCandidateAlarmCriteria(Alarm a) throws Exception {
		SyslogAlarm ea = (SyslogAlarm) a;
		String eventKey = ea.getCustomFieldValue(GFPFields.EVENT_KEY);
		String targetName = null;
		Util.WDPool(ea, targetName, false, 0, getScenario());
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingCandidateAlarmCriteria() runs.");
		return true;
	}

	@Override
	public boolean isMatchingSubAlarmCriteria(Alarm a,Group group) throws Exception {
		boolean ret = true;
		SyslogAlarm ea = (SyslogAlarm) a;
		String eventKey = ea.getCustomFieldValue(GFPFields.EVENT_KEY);
		String targetName = null;
		Util.WDPool(ea, targetName, false, 0, getScenario());
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingSubAlarmCriteria() " + "[" + ret + "], group name = " + group.getName());
		return ret;
	}
	
	 @Override
	 public List<String> computeProblemEntity(Alarm alarm) throws Exception {

		List<String> problemEntities = new ArrayList<String>();
		if (alarm.getCustomFieldValue("tail-end-router") != null) {
			problemEntities.add(alarm.getCustomFieldValue("tail-end-router"));
		} else
			problemEntities.add(alarm.getOriginatingManagedEntity());
		
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + alarm.getIdentifier() + ", computeProblemEntity() --- problemEntities=" + problemEntities);
		return problemEntities;
	} 
	 
	 @Override
	 public boolean isAllCriteriaForProblemAlarmCreation(Group group)
		throws Exception {
		return false;
	}

	 @Override
	 public boolean isInformationNeededAvailable(Alarm alarm) throws Exception {
		 
		 boolean ret = false;
		 
		if ((alarm instanceof SyslogAlarm)) {
			SyslogAlarm a = (SyslogAlarm) alarm;
			String tunnelKey = a.getOriginatingManagedEntity().split(" ")[1];
			String ipAddr = tunnelKey.split("/")[0];
			
			if (!a.getDeviceLevelExist()) {
				log.info(a.getIdentifier() + ": no parent device named " + ipAddr +
					" found for tunnel.");
			}
			
			if (a.getCustomFieldValue("tail-end-router") == null) {
				a.setOriginatingManagedEntity("DEVICE " + ipAddr);
				if (log.isTraceEnabled()) {
					log.trace("isInformationNeededAvailable(): This event: " + a.getIdentifier() +
							" | Tunnel could not be accessed.  Changing to send against device: " + ipAddr);
					log.trace("isInformationNeededAvailable() ==> Sending Tunnel Alarm: " + 
							alarm.toString());
				}
				
				//Scenario scenario = ScenarioThreadLocal.getScenario();
				Util.whereToSendThenSend((EnrichedAlarm) a, false);
			 } else {
				 ret = true;
			 }
		}
		return ret;
	 }
	  
	@Override
	public void whatToDoWhenSubAlarmIsAttachedToGroup(Alarm alarm, Group group) throws Exception {
		if (log.isTraceEnabled())
			LogHelper.enter(log, "alarm = " + alarm.getIdentifier() + ", group = " + group.getName() + ", group # = " + group.getNumber() + ", whatToDoWhenSubAlarmIsAttachedToGroup()");
		
		if (alarm == group.getTrigger()) {
			checkLocalVariable(group);
		}
		
		if ((alarm instanceof SyslogAlarm)) {
			SyslogAlarm a = (SyslogAlarm) alarm;
              long aggrAlarmCorrWindowStartTime = 
        		  a.getTimeInMilliseconds() - SyslogAlarm.AGGR_ALARM_CORR_WINDOW;
              if (a.checkForAggregateAlarm(group, aggrAlarmCorrWindowStartTime)) {
            	  return;
              }
              
              // watch to send the trigger alarm if no subalarms arrive
              //Class<?> partypes[] = new Class[0];
              Class<?>[] partypes = {Long.class}; 
              //Object arglist[] = new Object[0];
              Object[] arglist = {new Long(alarm.getTimeInMilliseconds())};
              Method method = 
            		  SyslogAlarm.class.getMethod("rpdMplsLspCallback", 
    				  partypes);
              Callback callback = new Callback(method, a, arglist);

              long agingTime = SyslogAlarm.TUNNEL_ALARM_AGING_WINDOW;
              Scenario scenario = ScenarioThreadLocal.getScenario();
              scenario.addCallbackWatchdogItem(agingTime, callback, false,
        		  "Expiration WatchdogItem", true, a);
              
              log.info("whatToDoWhenSubAlarmIsAttachedToGroup():  This event: " + 
        		  a.getIdentifier() + " | waiting / aging for " + agingTime / 1000 + " secs");
       }
     }
	
	@Override
	public void whatToDoWhenSubAlarmIsCleared(Alarm alarm, Group group)
			throws Exception {
		
		if ((alarm instanceof SyslogAlarm)) {
			SyslogAlarm a = (SyslogAlarm) alarm;    
            long aggrAlarmCorrWindowStartTime = 
        		System.currentTimeMillis() - SyslogAlarm.AGGR_ALARM_CORR_WINDOW;
			a.clearAggregateAlarm(group, aggrAlarmCorrWindowStartTime);
		}
	}
	
	private void checkLocalVariable(Group group) {

		LocalVariable var = group.getVar();
		synchronized (var) {
			// Not expecting var to ever be null, but just in case...
			if (var == null) {
				if (log.isTraceEnabled())
					log.trace("checkLocalVariable(): Creating LocalVariable for group " + 
					group.getName());
				var = new LocalVariable();
				group.setVar(var);
			}		
			if (!var.containsKey("AGGR_ALARM_ID")) {
				var.put("AGGR_ALARM_ID", "");			
			}
			if (!var.containsKey("AGGR_ALARM_LAST_NOTIFIED_AT")) {
				var.put("AGGR_ALARM_LAST_NOTIFIED_AT", 0L);			
			}
			if (!var.containsKey("AGGR_ALARM_COMPONENT")) {
				var.put("AGGR_ALARM_COMPONENT", "");			
			}
			if (!var.containsKey("AGGR_ALARM_REASON")) {
				var.put("AGGR_ALARM_REASON", "");			
			}
			if (!var.containsKey("TUNNEL_CLEAR_ID")) {
				var.put("TUNNEL_CLEAR_ID", "");			
			}
			if (!var.containsKey("TUNNEL_ALARM_LAST_CLEARED_AT")) {
				var.put("TUNNEL_ALARM_LAST_CLEARED_AT", 0L);			
			}
		}
	}
}
