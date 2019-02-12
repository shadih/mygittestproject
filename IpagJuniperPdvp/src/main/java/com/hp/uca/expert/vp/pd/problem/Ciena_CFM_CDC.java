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
import com.hp.uca.expert.x733alarm.PerceivedSeverity;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.vp.pd.core.JuniperLinkDown_ProblemDefault;
import com.hp.uca.expert.vp.pd.core.PD_AlarmRecognition;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;
import com.hp.uca.expert.vp.pd.services.PD_Service_Util;

//
// trigger is enriched by isMatchingSubAlarmCriteria()
// trigger is sent by WD, WD times is long enough to complete
//	enrichment(isAllCriteriaForProblemAlarmCreation() is not needed
// no subalarm
// candidate is sent by WD pool
//
public final class Ciena_CFM_CDC extends JuniperLinkDown_ProblemDefault implements
ProblemInterface {

	private static final String _50002_100_52 = "50002/100/52";
	private Logger log = LoggerFactory.getLogger(Ciena_CFM_CDC.class);
	public Ciena_CFM_CDC() {
		super();

	}
	// page 34 on project 256258a, HLD-500



	@Override
	public List<String> computeProblemEntity(Alarm a) throws Exception {
		List<String> problemEntities = new ArrayList<String>();
		EnrichedJuniperAlarm alarm = (EnrichedJuniperAlarm) a;

		String instance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String eventKey = alarm.getCustomFieldValue(GFPFields.EVENT_KEY);

		// all related EVC's are added for the ld ALARM
		if(eventKey.equals("50003/100/1")) {
			// this is just in case there are no vrfs so we don't want a null problemEntity
			problemEntities.add(instance);	

			HashSet<String> evcSet = alarm.getEVCSet();
			if (evcSet.size() > 0)
			{
				Iterator i = evcSet.iterator();
				while(i.hasNext())
				{
					String evc = (String)i.next();
					if (evc != null && evc.length() > 0)
						// it is used when it's trigger
						problemEntities.add(evc);
				}
			}
		} else {
			// we extract the vrf from the ME string (e.g. 10.144.0.67/VPLS:39367)
			String vfr = null;
			if(instance.contains("/"))
				problemEntities.add(instance.split("/")[1]);
			else
				problemEntities.add(instance);	
		}

		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", computeProblemEntity() --- problemEntities=" + problemEntities);
		return problemEntities;
		
	} 


	@Override
	public void whatToDoWhenSubAlarmIsAttachedToGroup(Alarm a, Group group) throws Exception {
		EnrichedJuniperAlarm trigger = (EnrichedJuniperAlarm) group.getTrigger();
		EnrichedJuniperAlarm alarm = (EnrichedJuniperAlarm) a;
	
		if (log.isTraceEnabled())
			LogHelper.enter(log, "trigger = " + trigger.getIdentifier() + "alarm = " + a.getIdentifier() + ", whatToDoWhenSubAlarmIsAttachedToGroup()");
		
		String mobility_ind = alarm.getCustomFieldValue("mobility_ind");
		
		if(mobility_ind == null || mobility_ind.isEmpty())
			mobility_ind = "N";
		
		// the trigger is the Juniper LD Pport alarm,  the subs will be the ciena cfm 
		// we update the info3 field with the cfm alarm info
		if(alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50002_100_52) &&
				mobility_ind.toUpperCase().equals("Y")) {
			String info3 = trigger.getCustomFieldValue(GFPFields.INFO3);
		
			info3 = info3 + "CFMAlertKey=<" + alarm.getCustomFieldValue(GFPFields.ALERT_ID) + "> CFMTimeStamp =<" + 
					alarm.getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP) + "> ";
			
			trigger.setCustomFieldValue(GFPFields.INFO3, info3);
		}
	}	
}
