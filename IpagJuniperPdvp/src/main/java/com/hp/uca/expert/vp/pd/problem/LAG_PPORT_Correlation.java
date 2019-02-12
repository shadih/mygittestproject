/**
 * This Problem is empty and ready to define methods to customize this problem
 */
package com.hp.uca.expert.vp.pd.problem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.EnrichedJuniperAlarm;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.vp.pd.core.JuniperLinkDown_ProblemDefault;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;

//
// trigger is enriched by createProblemAlarm()
// trigger is sent by WD set in JuniperLinkDown_ProblemDefault ==>
//	its timer must > delayForProblemAlarmCreation
// no subalarm
// candidate is sent by WD pool
//
public final class LAG_PPORT_Correlation extends JuniperLinkDown_ProblemDefault implements
		ProblemInterface {

	private Logger log = LoggerFactory.getLogger(LAG_PPORT_Correlation.class);

	public LAG_PPORT_Correlation() {
		super();

	}
		 
	 @Override
	 public List<String> computeProblemEntity(Alarm a) throws Exception {

		 if (log.isTraceEnabled()) {
			 LogHelper.enter(log, "######### computeProblemEntity()");
		 }
		 
		 List<String> problemEntities = new ArrayList<String>();
		 EnrichedJuniperAlarm alarm = (EnrichedJuniperAlarm) a;

		 String problemEntity = alarm.getDeviceInstance();
			 
		 problemEntities.add(problemEntity);

		 if (log.isTraceEnabled())
			 LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", computeProblemEntity() --- problemEntities=" + problemEntities);

		 return problemEntities;

	 } 

	@Override
	public boolean isMatchingSubAlarmCriteria(Alarm alarm, Group group) throws Exception {
		boolean ret = false;
		EnrichedJuniperAlarm pportAlarm = (EnrichedJuniperAlarm) alarm;
		EnrichedJuniperAlarm triggerAlarm = (EnrichedJuniperAlarm) group.getTrigger();
		HashSet<String> lagIdPportset = triggerAlarm.getLagIdPportset();
		HashSet<String> pportset = triggerAlarm.getPportset();
		String pport = pportAlarm.getOriginatingManagedEntity().split(" ")[1];
		if (lagIdPportset.contains(pport))
		{
			pportset.add(pport);
		}
		// just add subalarm pport to Trigger's pportset if it is in
		// lagIdPportset.
		// don't add subalarm to group ==> always return false
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + alarm.getIdentifier() + ", isMatchingSubAlarmCriteria() " + "[" + ret + "], group name = " + group.getName());
		return ret;
	}
}
