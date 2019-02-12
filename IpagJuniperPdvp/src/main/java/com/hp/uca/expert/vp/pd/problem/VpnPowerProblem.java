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
import com.hp.uca.expert.vp.pd.core.ProblemDefault;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;

/**
 * @author MASSE
 * 
 */
public final class VpnPowerProblem extends ProblemDefault implements
		ProblemInterface {
	
	private static final String _50003_100_7 = "50003/100/7";
	private static final String _50003_100_6 = "50003/100/6";
	private Logger log = LoggerFactory.getLogger(VpnPowerProblem.class);

	public VpnPowerProblem() {
		super();
	}
	
	@Override
	public boolean isMatchingCandidateAlarmCriteria(Alarm a) throws Exception {
		EnrichedJuniperAlarm ea = (EnrichedJuniperAlarm) a;
		Util.WDPool(ea, 0, getScenario());
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingCandidateAlarmCriteria() runs.");
		return true;
	}

	@Override
	public boolean isMatchingSubAlarmCriteria(Alarm a, Group group) throws Exception {
		boolean ret = true;
		EnrichedJuniperAlarm ea = (EnrichedJuniperAlarm) a;
		Util.WDPool(ea, 0, getScenario());
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingSubAlarmCriteria() " + "[" + ret + "], group name = " + group.getName());
		return ret;
	}
	@Override
	public boolean isAllCriteriaForProblemAlarmCreation(Group group)
			throws Exception {
		return false;
	}
	
	@Override
	public boolean isInformationNeededAvailable(Alarm alarm) throws Exception {
		boolean ret = false;
		
		if(alarm.getCustomFieldValue(GFPFields.REASON_CODE) != null &&
			!alarm.getCustomFieldValue(GFPFields.REASON_CODE).isEmpty())
			ret=true;
		
		return ret;
	}
	
	 @Override
	 public List<String> computeProblemEntity(Alarm a) throws Exception {

		List<String> problemEntities = new ArrayList<String>();

		problemEntities.add(a.getCustomFieldValue(GFPFields.REASON_CODE));
		
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", computeProblemEntity() --- problemEntities=" + problemEntities);
		 return problemEntities;
	 } 


	@Override
	public void whatToDoWhenSubAlarmIsAttachedToGroup(Alarm alarm, Group group) throws Exception {
		LogHelper.enter(log, "alarm = " + alarm.getIdentifier() + ", group = " + group.getName() + ", group # = " + group.getNumber() + ", whatToDoWhenSubAlarmIsAttachedToGroup()");
		
		Alarm triggerAlarm = group.getTrigger();
		
		if (alarm == triggerAlarm) {
			Util.whereToSendThenSend((EnrichedAlarm) alarm);
		} else {
			log.info("This event: " + alarm.getIdentifier() + "|" +
				alarm.getCustomFieldValue("reason") + "|" +
				triggerAlarm.getCustomFieldValue("reason") + 
				" - alarm suppressed due to " + triggerAlarm.getIdentifier() +
				" created " + triggerAlarm.getAlarmRaisedTime().toString());
			if (alarm instanceof EnrichedAlarm) {
				EnrichedAlarm ea = (EnrichedAlarm) alarm;
				log.info("alarm = " + ea.getIdentifier() + " is suppressed.");
				ea.setSuppressed(true);
			}
		}
	}

	
	@Override
	public void whatToDoWhenSubAlarmIsCleared(Alarm a, Group group)
		throws Exception {

		if (a instanceof EnrichedJuniperAlarm) {
			EnrichedJuniperAlarm alarm = (EnrichedJuniperAlarm) a;
			if (alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50003_100_6) ||
				alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50003_100_7)) {
				if (alarm.isSuppressed()) {
					log.info("Alarm " + alarm.getIdentifier() + " was suppressed; not sending clear");
				} else {
					Util.whereToSendThenSend((EnrichedAlarm) alarm);
				}
			}
		}
	}
}
