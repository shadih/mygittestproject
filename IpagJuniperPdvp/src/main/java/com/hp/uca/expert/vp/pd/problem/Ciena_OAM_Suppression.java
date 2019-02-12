/**
 * This Problem is empty and ready to define methods to customize this problem
 */
package com.hp.uca.expert.vp.pd.problem;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.data.ipagAlarm.EnrichedJuniperAlarm;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.vp.pd.core.JuniperLinkDown_ProblemDefault;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;
import com.hp.uca.expert.vp.pd.services.PD_Service_Group;

public final class Ciena_OAM_Suppression extends JuniperLinkDown_ProblemDefault implements ProblemInterface {

	private Logger log = LoggerFactory.getLogger(Ciena_OAM_Suppression.class);
	public Ciena_OAM_Suppression() {
		super();

	}

	
	@Override
	public List<String> computeProblemEntity(Alarm a) throws Exception
	{

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "computeProblemEntity()");
		}

		List<String> problemEntities = new ArrayList<String>();
		EnrichedJuniperAlarm alarm = (EnrichedJuniperAlarm) a;

		String eventKey = alarm.getCustomFieldValue(GFPFields.EVENT_KEY);
             	String problemEntity = a.getOriginatingManagedEntity().split(" ")[1];
             	problemEntities.add(problemEntity);
		
		String remotePportKey = alarm.getRemotePportInstanceName();
		if (remotePportKey != null)
             		problemEntities.add(remotePportKey);

		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", computeProblemEntity() --- problemEntities=" + problemEntities);

		return problemEntities;
	} 

	@Override
	public boolean isMatchingTriggerAlarmCriteria(Alarm a) throws Exception {
		boolean ret = false;

		EnrichedJuniperAlarm alarm = (EnrichedJuniperAlarm) a;	
		if ("CIENA NTE".equals(alarm.getDeviceType()))
		{
			long after = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger();
			String watchdogDesc = "Ciena OAM watch dog.";
			Util.setTriggerWatch((EnrichedJuniperAlarm)a, EnrichedJuniperAlarm.class, "simpleSendCallBack", after, watchdogDesc);
			ret = true;
		}
					
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingTriggerAlarmCriteria() " + "[" + ret + "]");
		return ret;
	}
		 
	
	@Override
	public void whatToDoWhenSubAlarmIsAttachedToGroup(Alarm alarm, Group group) throws Exception
	{
		if (log.isTraceEnabled())
			LogHelper.enter(log, "alarm = " + alarm.getIdentifier() + ", group = " + group.getName() + ", group # = " + group.getNumber() + ", whatToDoWhenSubAlarmIsAttachedToGroup()");

		if (group.getNumber() > 1)
		{
			EnrichedJuniperAlarm trigger = (EnrichedJuniperAlarm) group.getTrigger();
			trigger.setSuppressed(true);
			log.info("Alarm " + trigger.getIdentifier() + " sequence:" + trigger.getCustomFieldValue(GFPFields.SEQNUMBER) +
					" is suppressed by Ciena OAM suppression !");
		}
	}	
}
