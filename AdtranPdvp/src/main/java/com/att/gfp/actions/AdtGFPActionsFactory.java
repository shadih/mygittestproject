/**
 * 
 */
package com.att.gfp.actions;

import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.config.AdtranCorrelationConfiguration;
import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.data.ipagAlarm.EnrichedAdtranAlarm;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.helper.FilterTags;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
import com.att.gfp.helper.StandardFields;
import com.att.gfp.helper.service_util;
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
public class AdtGFPActionsFactory extends ActionsFactory implements
		SupportedActions {

	private String vpName;
	private String vpVersion;
	private String scenarioName;

	/**
	 * Logger used to trace this component
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(AdtGFPActionsFactory.class);

	/**
	 * 
	 */
	public AdtGFPActionsFactory() {

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
        String axml = alarm.toXMLString();
        axml = axml.replaceAll("\\n", " ");
		
		LOG.trace("setHistoryNavigation Qualifier " + qualifier.toString());  
		AdtranCorrelationConfiguration adtCorrelationConfig = new AdtranCorrelationConfiguration();

		switch (qualifier) {  
		case No:
			// When a trigger does not arrive for a candidate alarm then it is an orphan
			// and we send it along.
//			LogHelper.method(LOG,"CASE NO :" + alarm.getCustomFieldValue(StandardFields.NAVIGATION_FIELD));
//			LogHelper.method(LOG,"CASE NO :" + ((EnrichedAdtranAlarm)alarm).getSentAsSubAlarm());
//			if(qualifier.toString().equalsIgnoreCase("Candidate")
//					&& !((EnrichedAdtranAlarm)alarm).getSentAsSubAlarm() && !((EnrichedAdtranAlarm)alarm).getSentToNOM()) { 
//				if(!(adtCorrelationConfig.getAdtranCorrelationPolicies().getAdtranCfmAlarm().getEventNames().getEventName().contains(alarm.getCustomFieldValue(GFPFields.EVENT_KEY))) &&
//						(adtCorrelationConfig.getAdtranCorrelationPolicies().getAdtranAgingEvents().getEventNames().getEventName().contains(alarm.getCustomFieldValue(GFPFields.EVENT_KEY)))	) {
//					LogHelper.method(LOG,"CASE NO : sending sub alarm " + axml);
//					GFPUtil.forwardOrCascadeAlarm((EnrichedAlarm) alarm, AlarmDelegationType.FORWARD, null); 
//					((EnrichedAdtranAlarm)alarm).setSentAsSubAlarm(true);  
//				} 
//			}           
//			
			alarm.setCustomFieldValue(StandardFields.NAVIGATION_FIELD,  
					StandardFields.EMPTY_CUSTOM_FIELD);   
			break; 
		case Candidate:
			LOG.trace("CANDIDATE");   
//			LogHelper.method(LOG,"CASE CANDIDATE :" + alarm.getCustomFieldValue(StandardFields.NAVIGATION_FIELD)); 
//			LogHelper.method(LOG,"CASE CANDIDATE :" + ((EnrichedAdtranAlarm)alarm).getSentAsSubAlarm());
//			if(qualifier.toString().equalsIgnoreCase("Candidate")
//					&& !((EnrichedAdtranAlarm)alarm).getSentAsSubAlarm() && !((EnrichedAdtranAlarm)alarm).getSentToNOM()) {
//				if(!(adtCorrelationConfig.getAdtranCorrelationPolicies().getAdtranCfmAlarm().getEventNames().getEventName().contains(alarm.getCustomFieldValue(GFPFields.EVENT_KEY))) &&
//						!(adtCorrelationConfig.getAdtranCorrelationPolicies().getAdtranAgingEvents().getEventNames().getEventName().contains(alarm.getCustomFieldValue(GFPFields.EVENT_KEY)))	) {
//					LogHelper.method(LOG,"CASE NO : sending sub alarm " + axml);
//					GFPUtil.forwardOrCascadeAlarm((EnrichedAlarm) alarm, AlarmDelegationType.FORWARD, null);  
//					((EnrichedAdtranAlarm)alarm).setSentAsSubAlarm(true);  
//				} 
//			}      
			
			alarm.setCustomFieldValue(StandardFields.NAVIGATION_FIELD,
					StandardFields.EMPTY_CUSTOM_FIELD);  
			break;    
		case ProblemAlarm:
		case SubAlarm:
		case SubProblemAlarm:    
//			alarm.setCustomFieldValue(StandardFields.NAVIGATION_FIELD,
//					qualifier.toString());
//			break;
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

	/**
	 * TODO
	 */
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
			Group group, ProblemInterface problem, Alarm referenceAlarm)
					throws Exception {

		if (LOG.isTraceEnabled()) {
			LogHelper.enter(LOG, "createProblemAlarm()", group.getName());
		}
                 
            String axml = group.getTrigger().toXMLString();
            axml = axml.replaceAll("\\n", " ");

			//LogHelper.method(LOG, "############# SUPPRESSING the Alarm after correlation " + axml);
			Map<String, String> params = referenceAlarm.getPassingFiltersParams().get(
					problem.getProblemContext().getName());
			String corrName = params.get(FilterTags.CORR_NAME_FILTER_TAG);
			Collection<Alarm> alarms = group.getAlarmList();   
			Alarm subAlarm = null;
			if(group.getNumber() > 1) {
				for(Alarm alarm : alarms) {   
					 
					if(alarm != group.getTrigger()) {
						subAlarm = alarm;  
					}
				} 
			}
		if(subAlarm.isAboutToBeRetracted()) { 
			LogHelper.method(LOG, "##### Suppression : Alarm  " + subAlarm.getIdentifier() + "|{" + subAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "} event key: " + subAlarm.getCustomFieldValue(GFPFields.EVENT_KEY) + " is Suppressed by the Adtran correlation {" +corrName + "} and the alarm that triggered this suppression is " + group.getTrigger().getIdentifier() + "|{"+ group.getTrigger().getCustomFieldValue(GFPFields.SEQNUMBER) + "} event key: " + group.getTrigger().getCustomFieldValue(GFPFields.EVENT_KEY));  
			LogHelper.method(LOG, "##### This event " + subAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + " | "+ subAlarm.getIdentifier() + " cat: " + subAlarm.getCustomFieldValue(GFPFields.EVENT_KEY) + " processed by: AdtranPdvp | Alarm suppressed; " +"{" + subAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}");  
			LogHelper.method(LOG, "#############  RETRACTING the Alarm  " + subAlarm.getIdentifier() + "(SeqNumber=" + subAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + ")");
			group.removeAlarm(subAlarm);
			scenario.getSession().retract(subAlarm);  
			scenario.getSession().update(group);     
		}  else if(group.getTrigger().isAboutToBeRetracted()) {
			LogHelper.method(LOG, "##### Suppression : Alarm  " + group.getTrigger().getIdentifier() + "|{" + group.getTrigger().getCustomFieldValue(GFPFields.SEQNUMBER) + "} event key: " + group.getTrigger().getCustomFieldValue(GFPFields.EVENT_KEY) + " is Suppressed by the Adtran correlation {" +corrName + "} and the alarm that triggered this suppression is " + subAlarm.getIdentifier() + "|{"+ subAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "} event key: " + subAlarm.getCustomFieldValue(GFPFields.EVENT_KEY));  
			LogHelper.method(LOG, "##### This event " + group.getTrigger().getCustomFieldValue(GFPFields.SEQNUMBER) + " | "+ group.getTrigger().getIdentifier() + " cat: " + group.getTrigger().getCustomFieldValue(GFPFields.EVENT_KEY) + " processed by: AdtranPdvp | Alarm suppressed; " +"{" + group.getTrigger().getCustomFieldValue(GFPFields.SEQNUMBER) + "}");  
			LogHelper.method(LOG, "#############  RETRACTING the Alarm  " + group.getTrigger().getIdentifier() + "(SeqNumber=" + group.getTrigger().getCustomFieldValue(GFPFields.SEQNUMBER) + ")");
			scenario.getSession().retract(group.getTrigger()); 
//			if(subAlarm instanceof EnrichedAdtranAlarm) {  
//				EnrichedAdtranAlarm enrichedSubAlarm = (EnrichedAdtranAlarm) subAlarm;
//				if(!enrichedSubAlarm.getSentToNOM()) {
//					GFPUtil.forwardOrCascadeAlarm(subAlarm, AlarmDelegationType.FORWARD, null);  
//					enrichedSubAlarm.setSentToNOM(true);    
//				}
//			} 
			     
		}
		LogHelper.method(LOG, "#############   group count = " + group.getNumber());     
		     
  
		if (LOG.isTraceEnabled()) {  
			LogHelper.enter(LOG, "createProblemAlarm()",
					referenceAlarm.getIdentifier());  
		}     

		return null;
	}

}
