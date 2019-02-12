/**
 * 
 */
package com.att.gfp.actions;

import java.util.Arrays;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.att.gfp.ciena.cienaPD.CienaAlarm;
import com.att.gfp.ciena.cienaPD.Util;

import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
import com.att.gfp.helper.StandardFields;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.group.Qualifier;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.vp.pd.actions.ActionsFactory;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;
import com.hp.uca.expert.vp.pd.interfaces.SupportedActions;
import com.hp.uca.mediation.action.client.Action;

/**
 * @author MASSE
 * 
 */
public class CienaActionsFactory extends ActionsFactory implements
		SupportedActions {

/*
	private static final String _50003_100_1 = "50003/100/1";
	private static final String _50004_1_21 = "50004/1/21";
	private static final String _50004_1_20 = "50004/1/20";
	private static final String _50004_1_19 = "50004/1/19";
	private static final String _50004_1_18 = "50004/1/18";
	private static final String _50004_1_10 = "50004/1/10";
	private static final String _50003_100_24 = "50003/100/24";
	private String vpName;
	private String vpVersion;
	private String scenarioName;
*/

	/**
	 * Logger used to trace this component
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(CienaActionsFactory.class);

	/**
	 * 
	 */
	public CienaActionsFactory() {

	}

	/**
	 * TODO
	 */
	@Override
	public final Action clearAlarm(Action action, Scenario scenario,
			Alarm alarm, ProblemInterface problem) throws Exception {

		return null;
	}


	/**
	 * TODO
	 */
	@Override
	public final Action setHistoryNavigation(Action action, Scenario scenario,
			Alarm alarm, Qualifier qualifier) throws Exception {

		if (LOG.isTraceEnabled()) {
			LogHelper.enter(LOG, "setHistoryNavigation()",
					alarm.getIdentifier());
		}
		
		switch (qualifier) {
		case No:
/*
                    // When a trigger does not arrive for a candidate alarm then it is an orphan
                    // and we send it along OR if the trigger is cleared before the callback we send the alarms here.
*/
		// below got called when
		// (1) an alarm arrives.  it doesn't belong to any group ==>
		// 	its state is Candiate.  when timeout(ie, its trigger 
		//	doesn't arrives), it state changes from Candiate to 'No'
		// (2) when a trigger is cleared, state of subalarms in the 
		//	group change from SubAlarm to 'No'
                    if(alarm.getCustomFieldValue(StandardFields.NAVIGATION_FIELD) != null) {
                           if((alarm.getCustomFieldValue(StandardFields.NAVIGATION_FIELD).equals("Candidate") || alarm.getCustomFieldValue(StandardFields.NAVIGATION_FIELD).equals("SubAlarm"))) {
				// Util.sendAlarm((CienaAlarm)alarm, AlarmDelegationType.CASCADE, "JUNIPER_LINKDOWN", false);
				Util.sendAlarm((CienaAlarm)alarm, false);
                           }
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

	@Override
	public Action associateAlarmsForHistoryNavigation(Action action,
			Scenario scenario, Group group, Collection<Alarm> children,
			ProblemInterface problem) throws Exception {
		// TODO Auto-generated method stub
		
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
	Group group, ProblemInterface problem, Alarm referenceAlarm) throws Exception {

		if (LOG.isTraceEnabled()) {
			LogHelper.enter(LOG, "createProblemAlarm()", group.getName());
		}

		if (LOG.isTraceEnabled()) {
			LogHelper.exit(LOG, "createProblemAlarm()",
					group.getTrigger().getIdentifier());
		}

		return null;
	}
}
