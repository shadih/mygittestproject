/**
 * This Problem is empty and ready to define methods to customize this problem
 */
package com.hp.uca.expert.vp.pd.problem;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipag.decomposition.Decomposer;
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
import com.hp.uca.expert.x733alarm.PerceivedSeverity;

/**
 * @author df
 * 
 */
public final class CienaLinkdownRedundantPPortCorrelation extends JuniperLinkDown_ProblemDefault implements
		ProblemInterface {

	private static final String LINKDOWN_KEY = "50003/100/1";
	
	private Logger log = LoggerFactory.getLogger(CienaLinkdownRedundantPPortCorrelation.class);

	public CienaLinkdownRedundantPPortCorrelation() {
		super();

	}

		 
	@Override
	public List<String> computeProblemEntity(Alarm a) throws Exception {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "computeProblemEntity()");
		}

		List<String> problemEntities = new ArrayList<String>();
		EnrichedJuniperAlarm alarm = (EnrichedJuniperAlarm) a;

		String problemEntity;

		// the problem entity is the remote port to the juniper port that has the 
		// link down alarm
		if(alarm.getRedundantNNIPorts() != null && !(alarm.getRedundantNNIPorts().isEmpty())) {
			for(String redundantPort : alarm.getRedundantNNIPorts()) {
				if(redundantPort != null && !redundantPort.isEmpty()) {
					problemEntities.add(redundantPort);
				}
			}
		}
//		if(problemEntities.isEmpty()) {
//	     	problemEntity = a.getOriginatingManagedEntity().split(" ")[1];
//	     	problemEntities.add(problemEntity);
//		} 

		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", computeProblemEntity() --- problemEntities=" + problemEntities);
		return problemEntities;
	} 

	
	// This method is used to send alarms that have been attached to a group and
	// here is where I'm putting the suppression of the link down alarm based on the 
	// criteria listed in the requirements.   
	@Override
	public void whatToDoWhenSubAlarmIsAttachedToGroup(Alarm alarm, Group group) throws Exception {
		if (log.isTraceEnabled())
			LogHelper.enter(log, "alarm = " + alarm.getIdentifier() + ", group = " + group.getName() + ", group # = " + group.getNumber() + ", whatToDoWhenSubAlarmIsAttachedToGroup()");

		boolean isTriggerForOtherGroup = false;
		EnrichedJuniperAlarm trigger = (EnrichedJuniperAlarm) group.getTrigger();
		EnrichedJuniperAlarm eAlarm = (EnrichedJuniperAlarm) alarm;

		// is this subalarm a trigger for any group?   If it is then we don't want
		// to send the alarm here.   We will wait till all the triggers have been
		// evaluated then send it (watch callback).
		for (Group possibleGroup : PD_Service_Group.getGroupsOfAnAlarm(alarm, null)) {
			if(possibleGroup.getTrigger() == alarm) {
				isTriggerForOtherGroup = true;
				break;
			}
		}
		
		if (group != null && eAlarm != group.getTrigger()) { 
			
			if (log.isTraceEnabled())
				log.trace("This alarm is not the trigger: " + eAlarm.getIdentifier());
			
			eAlarm.setIsRedundantPPort(true);
			
			GFPUtil.forwardAlarmToDecomposerInstance(trigger, "JUNIPER_DECOMPOSER"); 
			trigger.setDecomposed(true); 
			
/*			List<EnrichedAlarm> decomposedAlarms = Decomposer.decompose(trigger);
			if(decomposedAlarms != null && decomposedAlarms.size() > 0) {
				for(EnrichedAlarm decompseAlarm : decomposedAlarms) {  
					log.trace("whatToDoWhenSubAlarmIsAttachedToGroup: sending decompsed alarm : " + decompseAlarm.getIdentifier());
					if(decompseAlarm.getPerceivedSeverity() != PerceivedSeverity.CLEAR) {
						decompseAlarm.setPerceivedSeverity(PerceivedSeverity.CRITICAL);   
					}
					GFPUtil.forwardOrCascadeAlarm(decompseAlarm, AlarmDelegationType.FORWARD, null);
	//				decompseAlarm.setSentToNOM(true);   
					trigger.setDecomposed(true); 
				}     
				if (log.isTraceEnabled())
					log.trace("whatToDoWhenSubAlarmIsAttachedToGroup: setting deomposed alarms List " + trigger.getCustomFieldValue(GFPFields.SEQNUMBER) + " size = " + decomposedAlarms.size()); 
				trigger.setDecomposedAlarms(decomposedAlarms);    
			} */ 
			
		}	
		// test to see if this is the trigger.  If the link down alarm is to be
		// suppressed or cascade the alarm
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + alarm.getIdentifier() + ", group = " + group.getName() + ", group # = " + group.getNumber() + ", whatToDoWhenSubAlarmIsAttachedToGroup()");
	}	
}
