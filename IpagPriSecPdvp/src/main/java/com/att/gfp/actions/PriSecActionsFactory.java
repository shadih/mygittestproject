/**
 * 
 */
package com.att.gfp.actions;

import java.util.Collection;

import org.mortbay.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.data.ipagAlarm.Pri_Sec_Alarm;
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
import com.hp.uca.expert.vp.pd.problem.ProcessOspfAlarms;
import com.hp.uca.expert.vp.pd.problem.Util;
import com.hp.uca.expert.vp.pd.services.PD_Service_Group;
import com.hp.uca.mediation.action.client.Action;

/**
 * @author MASSE
 * 
 */
public class PriSecActionsFactory extends ActionsFactory implements
		SupportedActions {

	
	private String vpName;
	private String vpVersion;
	private String scenarioName;

	/**
	 * Logger used to trace this component
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(PriSecActionsFactory.class);
	

	/**
	 * 
	 */
	public PriSecActionsFactory() {

	}


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

	@Override
	public final Action setHistoryNavigation(Action action, Scenario scenario,
			Alarm alarm, Qualifier qualifier) throws Exception {

		if (LOG.isTraceEnabled()) {
			LogHelper.enter(LOG, "setHistoryNavigation()",
					alarm.getIdentifier());
		}
		
		if (LOG.isTraceEnabled()) {
			LOG.trace("And navigation history is:" + alarm.getCustomFieldValue(StandardFields.NAVIGATION_FIELD));
			LOG.trace("And the qualifier is:" + qualifier);
		}
		
		switch (qualifier) {
		case No:
                    // When a trigger does not arrive for a candidate alarm then it is an orphan
                    // and we send it along OR if the trigger is cleared before the callback we send the alarms here.
            if(alarm.getCustomFieldValue(StandardFields.NAVIGATION_FIELD) != null) {
			    if((alarm.getCustomFieldValue(StandardFields.NAVIGATION_FIELD).equals("Candidate") || 
                         alarm.getCustomFieldValue(StandardFields.NAVIGATION_FIELD).equals("SubAlarm")))
			    	Util.WhereToSendAndSend((EnrichedAlarm) alarm);
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

/*		// these are the problem policies for pri/sec correlations
		if(group.getName().contains("JnxLacp")) {
			processLACPAlarm(group, scenario);

			// send out all subalarms for this group
			for (Alarm alarm : group.getAlarmList()) {
				Util.WhereToSendAndSend((EnrichedAlarm) alarm);		
			}
		}
		else if (group.getName().contains("JuniperLD_OAM_LAG")) {
			processJuniperLD_OAM_LAG(group);

			// send out all subalarms for this group
			for (Alarm alarm : group.getAlarmList()) {
				Util.WhereToSendAndSend((EnrichedAlarm) alarm);		
			}
		}*/
		
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

	

}
