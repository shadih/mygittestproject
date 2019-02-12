/**
 * This Problem is empty and ready to define methods to customize this problem
 */
package com.hp.uca.expert.vp.pd.problem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.EnrichedJuniperAlarm;
import com.att.gfp.helper.GFPFields;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.vp.pd.core.JuniperLinkDown_ProblemDefault;
// import com.hp.uca.expert.vp.pd.core.JuniperLinkDown_ProblemDefault;
import com.hp.uca.expert.vp.pd.core.ProblemDefault;
import com.hp.uca.expert.vp.pd.core.PD_AlarmRecognition;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;
import com.hp.uca.expert.vp.pd.services.PD_Service_ProblemAlarm;
import com.hp.uca.expert.vp.pd.services.PD_Service_Util;

//
// trigger is enriched by createProblemAlarm()
// trigger is sent by WD pool (NOT WD(as it is not in other problem group) nor 
//	createProblemAlarm(as it never gets called since 
//	isAllCriteriaForProblemAlarmCreation() never returns true as the 
//	group # never > 1))
// no subalarm
// candidate is sent by WD pool
//
public final class Unreachable_PPORT_Correlation extends JuniperLinkDown_ProblemDefault implements ProblemInterface {

	private Logger log = LoggerFactory.getLogger(Unreachable_PPORT_Correlation.class);

	public Unreachable_PPORT_Correlation() {
		super();

	}

	// page 33 on project 256258a, HLD-500

	@Override
	public boolean isMatchingTriggerAlarmCriteria(Alarm a) throws Exception {
		boolean ret = true;

		EnrichedJuniperAlarm alarm = (EnrichedJuniperAlarm) a;	

		long after = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger();
		String watchdogDesc = "Unreachable alarm watch dog.";
		Util.setTriggerWatch(alarm, EnrichedJuniperAlarm.class, "simpleSendCallBack", after, watchdogDesc);

		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingTriggerAlarmCriteria() " + "[" + ret + "]");

		return ret;
	}
		 
	 @Override
	 public List<String> computeProblemEntity(Alarm a) throws Exception {

		 if (log.isTraceEnabled()) {
			 LogHelper.enter(log, "######### computeProblemEntity()");
		 }
		 EnrichedJuniperAlarm ja = (EnrichedJuniperAlarm) a;
		 String eventKey = ja.getCustomFieldValue(GFPFields.EVENT_KEY);
		 List<String> problemEntities = new ArrayList<String>();
		 if(eventKey.equals("50002/100/55"))
		 {
			String pe = ja.getRemoteDeviceIpaddr();
			HashSet<String> remotePportset = ja.getRemotePportset();
			if (log.isTraceEnabled())
				log.trace("remotePportset = " + remotePportset);
			if (remotePportset.size() > 0)
			{
				Iterator i = remotePportset.iterator();
				while(i.hasNext())
				{
					// (String)i.next()(ie, remote_pport_key) 
					// already contains IP address
					// however, this IP address is different from
					// remote_device_ipaddr
					// problemEntities.add(pe+"_"+(String)i.next());
					String remotePport = (String)i.next();
					problemEntities.add(remotePport);
				}
			}
			else
				problemEntities.add(ja.getIdentifier());
		 }
		 else
		 {
             		String pport = a.getOriginatingManagedEntity().split(" ")[1];
             		// problemEntities.add(ja.getDeviceIpAddr()+"_"+pport);
			// pport already contains IP address
             		problemEntities.add(pport);
		 }
		 if (log.isTraceEnabled())
			 LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", computeProblemEntity() --- problemEntities=" + problemEntities);
		
		 return problemEntities;

	 } 

	@Override
	public boolean isMatchingSubAlarmCriteria(Alarm alarm, Group group) throws Exception {
		boolean ret = false;
		
		EnrichedJuniperAlarm pportAlarm = (EnrichedJuniperAlarm) alarm;
		EnrichedJuniperAlarm triggerAlarm = (EnrichedJuniperAlarm) group.getTrigger();
		if (triggerAlarm != pportAlarm)
		{
			log.info("Unreachable is the secondary.");
			triggerAlarm.setCustomFieldValue(GFPFields.SECONDARY_ALERT_ID, pportAlarm.getCustomFieldValue(GFPFields.ALERT_ID));
			triggerAlarm.setCustomFieldValue(GFPFields.SECONDARYTIMESTAMP, pportAlarm.getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP));
		}
		// don't add subalarm to group ==> always return false
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + alarm.getIdentifier() + ", isMatchingSubAlarmCriteria() " + "[" + ret + "], group name = " + group.getName());
		return ret;
	}
}
