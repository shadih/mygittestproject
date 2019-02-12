/**
 * This Problem is empty and ready to define methods to customize this problem
 */
package com.hp.uca.expert.vp.pd.problem;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.EnrichedJuniperAlarm;
import com.att.gfp.helper.GFPFields;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.vp.pd.core.JuniperLinkDown_ProblemDefault;
import com.hp.uca.expert.vp.pd.core.PD_AlarmRecognition;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;
import com.hp.uca.expert.vp.pd.services.PD_Service_Util;

/**
 * @author df
 * 
 */
public final class Device_LAG_Suppression extends JuniperLinkDown_ProblemDefault implements
		ProblemInterface {

	private Logger log = LoggerFactory.getLogger(Device_LAG_Suppression.class);

	public Device_LAG_Suppression() {
		super();

	}
		 
	@Override
	public boolean isMatchingCandidateAlarmCriteria(Alarm a) throws Exception {
		EnrichedJuniperAlarm ea = (EnrichedJuniperAlarm) a;

		Util.WDPool(ea, 0, getScenario());
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingCandidateAlarmCriteria() runs.");

		// we reject any subinterface lag alarms here
		if(((EnrichedJuniperAlarm)a).IsSubInterface())
			return false;
		else 
			return true;
	}

	@Override
	public boolean isMatchingSubAlarmCriteria(Alarm a, Group group) throws Exception {
		boolean ret = true;
		EnrichedJuniperAlarm ea = (EnrichedJuniperAlarm) a;
		Util.WDPool(ea, 0, getScenario());

		// we reject any subinterface lag alarms here
		if(((EnrichedJuniperAlarm)a).IsSubInterface())
			ret = false;
		
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingSubAlarmCriteria() " + "[" + ret + "], group name = " + group.getName());
		return ret;
	}

	@Override
	public boolean isMatchingTriggerAlarmCriteria(Alarm a) throws Exception {
		// we reject any subinterface lag alarms here
		if(((EnrichedJuniperAlarm)a).IsSubInterface())
			return false;
		else 
			return true;
	}

	 @Override
	 public List<String> computeProblemEntity(Alarm a) throws Exception {

		 if (log.isTraceEnabled()) {
			 LogHelper.enter(log, "######### computeProblemEntity()");
		 }
		 
		 List<String> problemEntities = new ArrayList<String>();
		 EnrichedJuniperAlarm alarm = (EnrichedJuniperAlarm) a;

		 String problemEntity = null;
			 
		 // one is the remote device instance that may have the alarm
		 if(alarm.getRemoteDevice() != null ) {
			 problemEntity = alarm.getRemoteDevice() + "-" + alarm.getlagIdFromAlarm();
			 problemEntities.add(problemEntity);
		 } 
		 
		 //  the other is the device instance for the alarm
		 problemEntity = a.getOriginatingManagedEntity().split(" ")[1] + "-" + alarm.getlagIdFromAlarm();
		 problemEntities.add(problemEntity);

		 if (log.isTraceEnabled())
			 LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", computeProblemEntity() --- problemEntities=" + problemEntities);

		 return problemEntities;

	 } 
}
