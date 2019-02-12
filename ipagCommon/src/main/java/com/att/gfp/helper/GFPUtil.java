package com.att.gfp.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.text.SimpleDateFormat;
import java.util.Date;					

import org.apache.commons.lang.StringUtils;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.config.CascadeAlarmConfiguration;
import com.att.gfp.data.config.CascadePolicies.CascadeTargets.CascadeTarget;
import com.att.gfp.data.config.IpagXmlConfiguration;
import com.att.gfp.data.config.UCAInstancesXmlConfiguration;
import com.att.gfp.data.config.UCAInstnaces;
import com.att.gfp.data.config.UCAInstnaces.UCAInstance;
import com.att.gfp.data.ipag.topoModel.Reason;
import com.att.gfp.data.ipag.topoModel.ReasonCode;
import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.alarm.AlarmCommon;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.group.Qualifier;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.scenario.ScenarioThreadLocal;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;
import com.hp.uca.expert.vp.pd.services.PD_Service_Action;
import com.hp.uca.expert.vp.pd.services.PD_Service_Group;
import com.hp.uca.expert.vp.pd.services.PD_Service_Navigation;
import com.hp.uca.expert.x733alarm.AlarmType;
import com.hp.uca.expert.x733alarm.ClassInstance;
import com.hp.uca.expert.x733alarm.CustomFields; 
import com.hp.uca.expert.x733alarm.CustomField;
import com.hp.uca.expert.x733alarm.NetworkState;
import com.hp.uca.expert.x733alarm.OperatorState;
import com.hp.uca.expert.x733alarm.OriginatingManagedEntityStructure;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;
import com.hp.uca.expert.x733alarm.ProblemState;

/**
 * Utility class that provides functionality for parsing the attributes of an alarm 
 * and also has wrappers  around PD_Servcies _*.
 * Also	provides utilities for forwarding alarms to NOM and Cascading alarms to the specified Scenario.
 * 
 * @author st133d
 * 
 */   
public final class GFPUtil {

	private static Logger log = LoggerFactory.getLogger(GFPUtil.class);

	private GFPUtil(){
		throw new AssertionError("GFPUtil():Not to be instantiated");
	}

	/**
	 * Parses specified value form a field, The value to be retrieved and valueToBeParsed are seperated by =
	 * @param attribute
	 * @param valueToBeParsed
	 * @param delimiter  
	 * @return
	 */
	public static String parseSpecifiedValueFromField(String attribute, String valueToBeParsed, String delimiter) {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "GFPUtil.parseSpecifiedValueFromField()");
		}
		String value = null;
		if(delimiter == null) {
			delimiter = " "; 
		}
		if(attribute != null && valueToBeParsed != null) {
			String[] fields = attribute.split(delimiter);
			if(fields != null) {  
				for(String field : fields) {
					log.trace("GFPUtil.parseSpecifiedValueFromField(): Found fields seperated by " + delimiter);
					String[] keyValues = field.split("\\=");
					if(keyValues != null && keyValues.length > 1) {
						if(valueToBeParsed.equalsIgnoreCase(keyValues[0])) {
							if(keyValues[1].startsWith("<") && keyValues[1].endsWith(">")) {
								value = keyValues[1].replace("<", "").replace(">", "");
							}  
							log.trace("GFPUtil.parseSpecifiedValueFromField(): Found " +  valueToBeParsed + "name field seperated by = " + value );
						}    
					}  
				}  
			}
		}
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "GFPUtil.parseComponentField()");
		}
		return value;    
	}  

	/**
	 * Fetches managed instance from originatedManagedEntity 
	 * @param originatedManagedEntity
	 * @return
	 */
	public static String getManagedInstanceFromMangaedEntity(String originatedManagedEntity) {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "GFPUtil.getManagedInstanceFromMangaedEntity()");
		}
		String managedInstance = null; 
		if(originatedManagedEntity != null) {
			String [] values = originatedManagedEntity.split(" ");
			if(values != null && values.length > 1) {
				managedInstance = values[1];
				log.trace("GFPUtil.getManagedInstanceFromMangaedEntity() managed instance = " + managedInstance);
			} 
		}
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "GFPUtil.getManagedInstanceFromMangaedEntity()");
		}
		return managedInstance;  
	}

	/**
	 * Retrieves fe_time_stamp custom field value from alarm
	 * @param alarm
	 * @return
	 */
	public static Long retrieveFeTimeFromCustomFields(Alarm alarm) {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "GFPUtil.retrieveFeTimeFromCustomFields()");
		}
		Long feTime = null;
		String feTimeStr = alarm
				.getCustomFieldValue(GFPFields.FE_TIME_STAMP); 
		try {  
			if (feTimeStr != null) { 
				feTime = Long.valueOf(feTimeStr); 
			}
		} catch (Exception e) {
			log.trace("GFPUtil.retrieveFeTimeFromCustomFields() Exception Unable to retrieve feTimeStamp field"); 
		}
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "GFPUtil.retrieveFeTimeFromCustomFields()");
		}

		return feTime; 
	}

	/**
	 * Recreates group with another alarm as trigger whose fe_time_stamp is less than the original trigger   
	 * @param a
	 * @param group
	 */
	public static void recreateGroupWithAnotherAlarmAsTrigger(Alarm a, Group group) {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "GFPUtil.recreateGroupWithAntherAlarmAsTrigger()");
		}
		Scenario scenario = ScenarioThreadLocal.getScenario();
		Long currentTriggerFeTimeStamp = 
				retrieveFeTimeFromCustomFields(group.getTrigger());
		Long currentAlarmFeTimeStamp = 
				retrieveFeTimeFromCustomFields(a);
		if (currentAlarmFeTimeStamp != null 
				&& currentTriggerFeTimeStamp != null
				&& currentAlarmFeTimeStamp < currentTriggerFeTimeStamp) {
			log.trace("GFPUtil.recreateGroupWithAntherAlarmAsTrigger(): Recreating group");
			PD_Service_Group.recreateGroupWithAnotherTrigger(group, a);
			PD_Service_Group.forceRole(group, a, Qualifier.Candidate); 
			scenario.getGroups().removeGroup(group);
			scenario.getSession().retract(group);
		}
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "GFPUtil.recreateGroupWithAntherAlarmAsTrigger()");
		}
	} 

	/**
	 * Returns the lead trigger group for the input alarm 
	 * @param alarm
	 * @param groupToIgnore
	 * @return
	 */
	public static Group calculateLeadTriggerGroup(Alarm alarm,
			Group groupToIgnore) {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "GFPUtil.calculateLeadTriggerGroup()");
		}
		Scenario scenario = ScenarioThreadLocal.getScenario();
		Set<Group> groupsWhereAlarmIsTrigger = new HashSet<Group>();
		for (Group group : scenario.getGroups().getGroupsOfAnAlarm(alarm)) {
			if (group != groupToIgnore && alarm == group.getTrigger()) {
				groupsWhereAlarmIsTrigger.add(group);
			}
		}
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "GFPUtil.calculateLeadTriggerGroup()");
		}

		return PD_Service_Group.calculateLeadGroup(groupsWhereAlarmIsTrigger);
	}

	/**
	 * Returns the lead subAlarmgroup for the input alarm
	 * @param alarm
	 * @param groupToIgnore
	 * @return
	 */
	public static Group calculateLeadSubAlarmGroup(Alarm alarm,
			Group groupToIgnore) {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "GFPUtil.calculateLeadSubAlarmGroup()");
		}
		Scenario scenario = ScenarioThreadLocal.getScenario();
		Set<Group> groupsWhereAlarmIsSubAlarm = new HashSet<Group>();
		for (Group group : scenario.getGroups().getGroupsOfAnAlarm(alarm)) {
			if (group != groupToIgnore && alarm != group.getTrigger()
					&& alarm != group.getProblemAlarm()) { 
				groupsWhereAlarmIsSubAlarm.add(group);
			}
		}
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "GFPUtil.calculateLeadSubAlarmGroup()");
		}

		return PD_Service_Group.calculateLeadGroup(groupsWhereAlarmIsSubAlarm);
	}

	/**
	 * @param subAlarms
	 * @param groupToIgnore
	 * @return
	 */
	public static Collection<Group> calculateGroupsToUpdateAfterRecorrelation(
			Collection<Alarm> subAlarms, Group groupToIgnore, Collection<Alarm> subAlarmsWhichAreNotTriggerOfAnyGroup) {

		Set<Group> groupsToUpdate = new HashSet<Group>();

		for (Alarm alarm : subAlarms) {

			if (log.isTraceEnabled()) {
				log.trace("==> Analysing subAlarm[" + alarm.getIdentifier()
						+ "]");
			}

			Long leadTriggerGroupPriority = null;
			Long leadSubAlarmGroupPriority = null;

			/*  
			 * Groups where alarm is Trigger
			 */
			Group leadTriggerGroup = calculateLeadTriggerGroup(
					alarm, groupToIgnore);

			if (leadTriggerGroup != null) {
				leadTriggerGroupPriority = leadTriggerGroup.getPriority();

				if (log.isTraceEnabled()) {
					log.trace("SubAlarm [" + alarm.getIdentifier()
							+ "] is Trigger. Leader group is ["
							+ leadTriggerGroup.getName() + "][Priority:"
							+ leadTriggerGroupPriority + "]");
				}

				/*
				 * Calculate the Leader of Groups where this Alarm is SubAlarm
				 */
				Group leadSubAlarmGroup = 
						calculateLeadSubAlarmGroup(alarm, groupToIgnore);

				if (leadSubAlarmGroup != null) {
					leadSubAlarmGroupPriority = leadSubAlarmGroup.getPriority();

					if (log.isTraceEnabled()) {
						log.trace("SubAlarm [" + alarm.getIdentifier()
								+ "] is Subalarm. SubAlarm Leader group is ["
								+ leadSubAlarmGroup.getName() + "][Priority:"
								+ leadSubAlarmGroupPriority + "]");
					}

					if (leadTriggerGroupPriority <= leadSubAlarmGroupPriority) {

						if (log.isTraceEnabled()) {
							log.trace("SubAlarm ["
									+ alarm.getIdentifier()
									+ "] is Trigger with High Priority, need group update");
						}

						groupsToUpdate.add(leadTriggerGroup);
					} else {
						if (log.isTraceEnabled()) {
							log.trace("SubAlarm [" + alarm.getIdentifier()
									+ "] is kept as SubAlarm, no group Update");
						}

					}

				} else {

					if (log.isTraceEnabled()) {
						log.trace("SubAlarm ["
								+ alarm.getIdentifier()
								+ "] is Trigger without SubAlarm, need group update");
					}
					groupsToUpdate.add(leadTriggerGroup);

				}

			} else {
				if (log.isTraceEnabled()) {
					log.trace("SubAlarm [" + alarm.getIdentifier()
							+ "] is not Trigger for any group");
				}
				subAlarmsWhichAreNotTriggerOfAnyGroup.add(alarm);
			}

		}

		return groupsToUpdate;
	}

	/**
	 * <pre>
	 * After recorrelation - when the problem alarm of a group of higher priority is no longer eligible -,
	 * there is the need to decide what to do for groups whose alarms used to have that problem alarm  as parent alarm
	 * - If there is no problem alarm for the group to update, then simply update the group, and count on rules execution
	 * - it there is already a problem alarm for the group to update, then declare that this  alarm have the  effective role of "problem alarm" 
	 * </pre>
	 * 
	 * @param groupsToUpdate
	 * @throws Exception
	 */
	public static void decideGroupUpdateStrategyAfterRecorrelation(
			Collection<Group> groupsToUpdate) throws Exception {

		Scenario scenario = ScenarioThreadLocal.getScenario();

		for (Group groupToUpdate : groupsToUpdate) {

			if (log.isTraceEnabled()) {
				log.trace("==> Group [" + groupToUpdate
						+ "] decide how to update");
			}

			ProblemInterface problemInterface = PD_Service_Group
					.retrieveProblemFromGroup(groupToUpdate); 

			if (groupToUpdate.getProblemAlarm() == null) {
				if (log.isTraceEnabled()) {
					log.trace("PbAlarm of group ["
							+ groupToUpdate.getName()
							+ "] does not exist, update it to create the PbAlarm");
				}
				// TODO Use internals
				scenario.getSession().update(groupToUpdate);

				if (!problemInterface
						.isAllCriteriaForProblemAlarmCreation(groupToUpdate)) {
					/*
					 * If the PbAlarm is still null after the Group update,
					 * reset all children with ParentIdentifier=""
					 */
					PD_Service_Action.associateAlarmsForHistoryNavigation(
							scenario, groupToUpdate,
							groupToUpdate.getAlarmList(), problemInterface);
				}
			} else {

				if (log.isTraceEnabled()) {
					log.trace("PbAlarm of group [" + groupToUpdate.getName()
							+ "] already exists, just navigation request");
				}

				PD_Service_Group
				.forceRole(groupToUpdate,
						groupToUpdate.getProblemAlarm(),
						Qualifier.ProblemAlarm);
				PD_Service_Navigation.needProblemAlarmNavigationUpdate(
						scenario, groupToUpdate);

				PD_Service_Action.associateAlarmsForHistoryNavigation(scenario,
						groupToUpdate, groupToUpdate.getAlarmList(),
						problemInterface);

			}

		}
	}

	/**
	 * Returns current time in Seconds
	 * @return
	 */
	public static String getCurrentTimeInSecond() {
		return String.valueOf(TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS));
	} 

	/**  
	 * Converts given time in seconds to MilliSeconds 
	 * @param seconds 
	 * @return 
	 */
	public static Long convertToMilliseconds(long seconds) {
		return TimeUnit.MILLISECONDS.convert(System.currentTimeMillis(), TimeUnit.SECONDS);
	}
	/**
	 * @param group
	 * @param subAlarms
	 * @return
	 * @throws Exception
	 */
	public static Group checkIfAnotherAlarmCanBeTriggerAndSameGroupShouldbeCreated(
			Group group, Collection<Alarm> subAlarms) throws Exception {
		if (log.isTraceEnabled()) {    
			LogHelper
			.enter(log,
					"checkIfAnotherAlarmCanBeTriggerAndSameGroupShouldbeCreated()",
					String.format("[%s][%s]", group.getName(),
							subAlarms.toString()));
		}  

		Scenario scenario = ScenarioThreadLocal.getScenario();

		Group newGroup = null;

		for (Alarm potentialTrigger : subAlarms) {
			if (potentialTrigger != group.getTrigger()
					&& PD_Service_Group.isMatchingTriggerAlarm(
							potentialTrigger, group)) {

				newGroup = PD_Service_Group.recreateGroupWithAnotherTrigger(
						group, potentialTrigger);

				if (group.getProblemAlarm() != null) {
					scenario.getGroups()
					.setGroupMatching(newGroup,
							group.getProblemAlarm(),
							Qualifier.Candidate, false);
					scenario.getGroups().setGroupMatching(newGroup,
							group.getProblemAlarm(), Qualifier.SubAlarm, false);
					scenario.getGroups().setGroupMatching(newGroup,
							group.getProblemAlarm(), Qualifier.Trigger, false);
					scenario.getGroups().setGroupMatching(newGroup,
							group.getProblemAlarm(), Qualifier.ProblemAlarm,
							false);
				}
				break;
			}  
		}

		return newGroup;
	}

	/**
	 * Forwards the alarm to NOM or Cascades the alarm to a specified Scenario
	 * @param enrichedAlarm
	 * @param alarmDelegationType
	 * @param cascadeTargetName is the target name specified in the conf xml file
	 */
	public static void forwardOrCascadeAlarm(Alarm alarm, AlarmDelegationType alarmDelegationType, String cascadeTargetName) {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "GFPUtil.forwardOrCascadeAlarm()");  
		}    
		String axml = alarm.toXMLString();
		axml = axml.replaceAll("\\n", " ");
		Scenario scenario = ScenarioThreadLocal.getScenario();    
		if(scenario == null) {
			log.trace("GFPUtil.forwardOrCascadeAlarm() Scenario cannot be NULL");  
		}
		IpagXmlConfiguration ipagXmlConfig = (IpagXmlConfiguration) service_util.retrieveBeanFromContextXml(scenario, "ipagXmlConfig");
		EnrichedAlarm enrichedAlarm= null;
		if(alarm != null) {   
			//			if(alarm instanceof com.att.gfp.data.ipagAlarm.EnrichedAlarm) {
			//				log.trace("GFPUtil.forwardOrCascadeAlarm() Received alarm is instanceOf EnrichedAlarm"); 
			//			enrichedAlarm = (com.att.gfp.data.ipagAlarm.EnrichedAlarm) alarm;
			//			} else {
			populateInfoField(alarm);
			enrichedAlarm = new com.att.gfp.data.ipagAlarm.EnrichedAlarm(alarm); 
			//			}
			if(alarmDelegationType.equals(AlarmDelegationType.CASCADE)) {
				cascadeTargetName = changeCascadeTargetForJuniperESScenarios(cascadeTargetName);  
				CascadeAlarmConfiguration cascadeConf = (CascadeAlarmConfiguration)service_util.retrieveBeanFromContextXml(scenario, "cascadeConfig");   
				CascadeTarget cascadeTarget = cascadeConf.getCascadePolicies().getCascadeTargets().getCascadeTargetByName(cascadeTargetName);
				if(cascadeTarget != null) {
					//		log.trace("GFPUtil.forwardOrCascadeAlarm() Cascading the alarm : toString() " + enrichedAlarm.toString()); 
					log.info("GFPUtil.forwardOrCascadeAlarm() Cascading alarm  = " + axml); 
					log.info("GFPUtil.forwardOrCascadeAlarm() Cascading alarm to VP Name= " + cascadeTarget.getVpName() + " Senario = " + cascadeTarget.getScenarioName());       

					if(ipagXmlConfig.getIpagPolicies().getHealthTraps().getEventNames().getEventName().contains(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
						log.info("GFPUtil.forwardOrCascadeAlarm() Cascading the health check alarm with Seq Number = " + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER));   
						enrichedAlarm.setProbableCause(enrichedAlarm.getProbableCause() + cascadeTarget.getVpName() + "_IN=" + getCurrentTimeInSecond() + ";");
					}
					if(getTargetUCAInstanceVPs(scenario, cascadeTarget.getVpName()).contains(getValuePackName(scenario.getValuePack().getNameVersion()))) {
						scenario.copyAlarmToScenario(
								cascadeTarget.getVpName(),      
								String.valueOf(cascadeTarget.getVersion()),             
								cascadeTarget.getScenarioName(), enrichedAlarm);
					} else { 
						enrichedAlarm.setTargetValuePack(cascadeTarget.getVpName() + "-" + cascadeTarget.getVersion());  
						setAdditionalCustomFieldsForEnrichedAlarm(enrichedAlarm);
						if(Arrays.toString(AdtranInstanceVPs.values()).contains(cascadeTarget.getVpName())) {  
							log.trace("GFPUtil.forwardOrCascadeAlarm() forwarding to Adtran isntnance " + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER));
							service_util.forwardAlarmToAdtranInstance(scenario, enrichedAlarm);
						} else if(Arrays.toString(CienaInstanceVPs.values()).contains(cascadeTarget.getVpName())) { 
							log.trace("GFPUtil.forwardOrCascadeAlarm() forwarding to Ciena isntnance " + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER));
							service_util.forwardAlarmToCienaInstance(scenario, enrichedAlarm); 
						} else if(Arrays.toString(JuniperInstanceVPs.values()).contains(cascadeTarget.getVpName())) {
							log.trace("GFPUtil.forwardOrCascadeAlarm() forwarding to Juniper isntnance " + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER));
							service_util.forwardAlarmToJuniperInstance(scenario, enrichedAlarm);    
						}  
					}

				}    
			}
			if(alarmDelegationType.equals(AlarmDelegationType.FORWARD)) {      
				log.trace("GFPUtil.forwardOrCascadeAlarm() Forwarding alarm to NOM " + axml);    
				removeExtraCustomFields(enrichedAlarm);

				if (enrichedAlarm.getRemoteDeviceType() == null ){					
					enrichedAlarm.setCustomFieldValue(GFPFields.REMOTE_DEVICE_TYPE, "");
				} else {
					enrichedAlarm.setCustomFieldValue(GFPFields.REMOTE_DEVICE_TYPE, enrichedAlarm.getRemoteDeviceType());
				}
				if (enrichedAlarm.getRemoteDeviceModel() == null ){					
					enrichedAlarm.setCustomFieldValue(GFPFields.REMOTE_DEVICE_MODEL, "");
				} else {
					enrichedAlarm.setCustomFieldValue(GFPFields.REMOTE_DEVICE_MODEL, enrichedAlarm.getRemoteDeviceModel());
				}
				if (enrichedAlarm.getRemoteDeviceName() == null ){					
					enrichedAlarm.setCustomFieldValue(GFPFields.REMOTE_DEVICE_NAME, "");
				} else {
					enrichedAlarm.setCustomFieldValue(GFPFields.REMOTE_DEVICE_NAME, enrichedAlarm.getRemoteDeviceName());
				}
				if (enrichedAlarm.getRemoteDeviceIpaddr() == null ){					
					enrichedAlarm.setCustomFieldValue(GFPFields.REMOTE_DEVICE_IPADDR, "");
				} else {
					enrichedAlarm.setCustomFieldValue(GFPFields.REMOTE_DEVICE_IPADDR, enrichedAlarm.getRemoteDeviceIpaddr());
				}
				if (enrichedAlarm.getRemotePportInstanceName() == null ){					
					enrichedAlarm.setCustomFieldValue(GFPFields.REMOTE_PPORT_INSTANCE_NAME, "");
				} else {
					enrichedAlarm.setCustomFieldValue(GFPFields.REMOTE_PPORT_INSTANCE_NAME, enrichedAlarm.getRemotePportInstanceName());
				}
				if (enrichedAlarm.getRemotePortAid() == null ){					
					enrichedAlarm.setCustomFieldValue(GFPFields.REMOTE_PORT_AID, "");
				} else {
					enrichedAlarm.setCustomFieldValue(GFPFields.REMOTE_PORT_AID, enrichedAlarm.getRemotePortAid());
				}

				if(ipagXmlConfig.getIpagPolicies().getHealthTraps().getEventNames().getEventName().contains(alarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
					log.trace("GFPUtil.forwardOrCascadeAlarm() Forwarding alarm to NOM, is health check alarm with  Seq Number = " + alarm.getCustomFieldValue(GFPFields.SEQNUMBER)); 
					alarm.setProbableCause(alarm.getProbableCause() + "CDM_OUT=" + getCurrentTimeInSecond()); 
					if(getJuniperHealthTraps().contains(alarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
						enrichedAlarm.setIssendToCpeCdc(true); 
					}

				}

				if(enrichedAlarm.isSendToCdc()) { 
					log.trace("GFPUtil.forwardOrCascadeAlarm() alarm " + alarm.getIdentifier() + "{|" + alarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}" + " is subscribed to CDC");
					if(ipagXmlConfig.getIpagPolicies().getCdcAlarms().getEventNames().getEventName().contains(alarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
						log.trace("Alarm " + alarm.getIdentifier() + "|{" + alarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "} is configured to send to Mobility CDC");

						if((alarm.getCustomFieldValue(GFPFields.SECONDARYTIMESTAMP) != null) && 
								!(alarm.getCustomFieldValue(GFPFields.SECONDARYTIMESTAMP).isEmpty()) &&
								!(alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50004/1/39")) ) {

							log.trace("Alarm " + alarm.getIdentifier() + "|{" + alarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "} is a Secondary alarm, Not sending to CDC");
						}  
						else {
							log.trace("GFPUtil.forwardOrCascadeAlarm() Fowarding alarm to CDC");   
							//TODO: remove this if condition

							enrichedAlarm.setCustomFieldValue("RealTimeFlag", "R");
							enrichedAlarm.setCustomFieldValue("sm-sourcedomain","IPAG"); 
							boolean fwdToCDC= false;

							log.trace("checking for cdc-subscription-type ... " + alarm.getCustomFieldValue("cdc-subscription-type") + ".");

							if("L2-7450-IPAG".equalsIgnoreCase(alarm.getCustomFieldValue("cdc-subscription-type")) ||
									"L2-7750-IPAG".equalsIgnoreCase(alarm.getCustomFieldValue("cdc-subscription-type"))	) {
								log.trace("cdc-subscription-type is L2-7450-IPAG/L2-7750-IPAG");
								if(enrichedAlarm.getCdcPportClfi() != null && !(enrichedAlarm.getCdcPportClfi().isEmpty())) {
									enrichedAlarm.setCustomFieldValue(GFPFields.CIRCUIT_ID, enrichedAlarm.getCdcPportClfi()); 
									enrichedAlarm.setCustomFieldValue("circuitId", enrichedAlarm.getCdcPportClfi());   
									log.trace("cdc-subscription-type is L2-7450-IPAG/L2-7750-IPAG. " + GFPFields.CIRCUIT_ID + " is " + enrichedAlarm.getCustomFieldValue(GFPFields.CIRCUIT_ID) + "." );
								}
								if(     (enrichedAlarm.getCustomFieldValue(GFPFields.CIRCUIT_ID) != null) &&
										!(enrichedAlarm.getCustomFieldValue(GFPFields.CIRCUIT_ID).isEmpty()))
								{
									fwdToCDC = true;
								}
							}
							if("VLXP".equalsIgnoreCase(alarm.getCustomFieldValue("cdc-subscription-type"))) {
								if((enrichedAlarm.getEvcName() != null) && 
										!(enrichedAlarm.getEvcName().isEmpty())) {
									enrichedAlarm.setCustomFieldValue(GFPFields.CIRCUIT_ID, enrichedAlarm.getEvcName());  
									enrichedAlarm.setCustomFieldValue(GFPFields.EVC_NAME, enrichedAlarm.getEvcName());  
									//enrichedAlarm.setCustomFieldValue("circuitId", enrichedAlarm.getCdcPportClfi());   
								} 
								if(     (enrichedAlarm.getCustomFieldValue(GFPFields.EVC_NAME) != null) && 
										!(enrichedAlarm.getCustomFieldValue(GFPFields.EVC_NAME).isEmpty()) && 
										(enrichedAlarm.getCustomFieldValue(GFPFields.CIRCUIT_ID) != null) &&
										!(enrichedAlarm.getCustomFieldValue(GFPFields.CIRCUIT_ID).isEmpty()))
								{
									fwdToCDC = true;
								}
							}

							if( fwdToCDC )
							{
								log.trace("GFPUtil.forwardOrCascadeAlarm() fwdToCDC is true. Fowarding alarm to CDC");    
								service_util.forwardAlarmToMobilityCDC(scenario, enrichedAlarm);     
								if(!("VLXP".equalsIgnoreCase(alarm.getCustomFieldValue("cdc-subscription-type")))) {
									if ( enrichedAlarm.getCustomFieldValue("cdc-subscription-type").equalsIgnoreCase("L2-7450-IPAG")) {
										enrichedAlarm.setCustomFieldValue(GFPFields.CIRCUIT_TYPE, "L2-FBS-IPAG");
									} else if ( enrichedAlarm.getCustomFieldValue("cdc-subscription-type").equalsIgnoreCase("L2-7750-IPAG")) {
										enrichedAlarm.setCustomFieldValue(GFPFields.CIRCUIT_TYPE, "L2-ASE-IPAG");
									} else {
										enrichedAlarm.setCustomFieldValue(GFPFields.CIRCUIT_TYPE, "");
									}
									service_util.forwardAlarmToUverseCDC(scenario, enrichedAlarm);  
								}
							}
							else {
								log.info("GFPUtil.forwardOrCascadeAlarm() Not Fowarding alarm to CDC because EVC_NAME/CIRCUIT_ID is null");
							}
						}
					}
				}

				if("YES".equalsIgnoreCase(alarm.getCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA))) {
					log.trace("GFPUtil.forwardOrCascadeAlarm() Discarding the alarm as it is generated by UCA");   
					return;  
				}   
				if(alarm.getPerceivedSeverity() != PerceivedSeverity.WARNING) {
					if(!("YES".equalsIgnoreCase(enrichedAlarm.getCustomFieldValue(GFPFields.IS_PURGE_INTERVAL_EXPIRED)))) {
						if("true".equalsIgnoreCase(alarm.getCustomFieldValue(GFPFields.IS_GENERATED_BY_DECOMPOSITION))) {
							service_util.forwardAlarmToDecompostion(scenario, enrichedAlarm);
						} else { 
							EnrichedAlarm alarmToForward = null;  
							try { 
								alarmToForward = enrichedAlarm.clone();						
								if("CE".equalsIgnoreCase(alarmToForward.getNodeType())) {
									updateComponentField(alarmToForward);
								} 
							} catch (CloneNotSupportedException e) {
								e.printStackTrace(); 
							}	 
							service_util.forwardAlarmToNOM(scenario, alarmToForward);
							generateAlarmForRubyCNI(scenario, alarmToForward);						
							generateDecompostionAlarmForCienaCFM(scenario, enrichedAlarm);
						}  
					}
				} 				
				else {
					log.info("GFPUtil.forwardOrCascadeAlarm() alarm " + alarm.getIdentifier() + "{" + alarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}" + 
							" has WARNING severity; Stop Forwarding to NOM... Continue...");
				} 

				if(enrichedAlarm.getIssendToCpeCdc() && (enrichedAlarm.getPerceivedSeverity() != PerceivedSeverity.CLEAR)) {
					log.trace("GFPUtil.forwardOrCascadeAlarm() alarm " + alarm.getIdentifier() + "{|" + alarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}" + " is send  to CPE CDC");
					if((enrichedAlarm.getCustomFieldValue(GFPFields.SECONDARYTIMESTAMP) != null) && !(enrichedAlarm.getCustomFieldValue(GFPFields.SECONDARYTIMESTAMP).isEmpty())) {
						log.trace("Alarm " + enrichedAlarm.getIdentifier() + "|{" + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "} is a Secondary alarm, Not sending to CPE CDC");
						return; 
					}  
					Alarm cpeCdcAlarm = new Alarm();
					cpeCdcAlarm.setIdentifier(enrichedAlarm.getCustomFieldValue(GFPFields.ALERT_ID) + "," +enrichedAlarm.getAlarmRaisedTime());
					cpeCdcAlarm.setSourceIdentifier("GFP-Data");
					cpeCdcAlarm.setAlarmRaisedTime(enrichedAlarm.getAlarmRaisedTime());
					SimpleDateFormat isoFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
					Date date = new Date();
					if(enrichedAlarm.getAlarmRaisedTime() != null) {
						date = new Date(enrichedAlarm.getAlarmRaisedTime().toGregorianCalendar().getTimeInMillis());
					}
					isoFormat.setTimeZone(TimeZone.getTimeZone("CST"));
					String networkTimestamp = isoFormat.format(date) + " CST";  
					SimpleDateFormat iFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
					iFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
					String normalzedTimestamp = iFormat.format(date) + "-00:00";  

					cpeCdcAlarm.setOriginatingManagedEntity(enrichedAlarm.getCustomFieldValue(GFPFields.ALERT_ID));
					OriginatingManagedEntityStructure orig = new OriginatingManagedEntityStructure();
					ClassInstance cl = new ClassInstance();
					cl.setClazz("AlarmObjectID");
					cl.setInstance(alarm.getCustomFieldValue(GFPFields.ALERT_ID));
					orig.getClassInstance().add(cl);
					cpeCdcAlarm.setOriginatingManagedEntityStructure(orig);
					cpeCdcAlarm.setProbableCause(enrichedAlarm.getCustomFieldValue(GFPFields.REASON));
					cpeCdcAlarm.setAlarmType(AlarmType.COMMUNICATIONS_ALARM);
					cpeCdcAlarm.setNetworkState(NetworkState.NOT_CLEARED);
					cpeCdcAlarm.setPerceivedSeverity(PerceivedSeverity.INDETERMINATE);
					cpeCdcAlarm.setOperatorState(OperatorState.NOT_ACKNOWLEDGED);
					cpeCdcAlarm.setProblemState(ProblemState.NOT_HANDLED);
					cpeCdcAlarm.setCustomFieldValue("AlarmObjectType", enrichedAlarm.getOrigMEClass()); 
					cpeCdcAlarm.setCustomFieldValue("EquipmentHierarchyType", enrichedAlarm.getOrigMEClass());
					cpeCdcAlarm.setCustomFieldValue("CLCI", enrichedAlarm.getCustomFieldValue(GFPFields.CLCI));
					if("LPORT".equalsIgnoreCase(enrichedAlarm.getOrigMEClass())) {
						cpeCdcAlarm.setCustomFieldValue("CLCI", enrichedAlarm.getLportClci()); 
					}   
					cpeCdcAlarm.setCustomFieldValue("CLFI", enrichedAlarm.getCustomFieldValue(GFPFields.CLFI));
					cpeCdcAlarm.setCustomFieldValue("NetworkTimeStamp", networkTimestamp); 
					cpeCdcAlarm.setCustomFieldValue("NormalizedTimeStamp", normalzedTimestamp);
					cpeCdcAlarm.setCustomFieldValue("FMSLayer", "L2");
					cpeCdcAlarm.setCustomFieldValue("TID", enrichedAlarm.getDeviceName());    
					cpeCdcAlarm.setCustomFieldValue("AssetID", enrichedAlarm.getDeviceName());
					cpeCdcAlarm.setCustomFieldValue("AdditionalCLCIInfo", "");
					cpeCdcAlarm.setCustomFieldValue("AdditionalCLFIInfo", enrichedAlarm.getAdditionalCLFIInfo());
					cpeCdcAlarm.setCustomFieldValue("NodeType", getCpeCdcNodeType(enrichedAlarm));
					cpeCdcAlarm.setCustomFieldValue("AlarmClass", "Network Alarm");
					cpeCdcAlarm.setCustomFieldValue("isHealthAlarm", "false");
					if("true".equalsIgnoreCase(enrichedAlarm.getCustomFieldValue("isHealthCheckAlarm"))) {
						cpeCdcAlarm.setCustomFieldValue("isHealthAlarm", "true"); 
					}
					service_util.forwardAlarmToCpeCDC(scenario, cpeCdcAlarm); 
				}
			}  
		} else {   
			log.trace("GFPUtil.forwardOrCascadeAlarm(): INPUT Alarm cannot be NULL");     
		}
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "GFPUtil.forwardOrCascadeAlarm()");
		}    

	}

	private static void generateAlarmForRubyCNI(Scenario scenario, EnrichedAlarm alarmToForward) {
		if(("10000/500/2".equalsIgnoreCase(alarmToForward.getCustomFieldValue(GFPFields.EVENT_KEY)) || 
				"10000/500/3".equalsIgnoreCase(alarmToForward.getCustomFieldValue(GFPFields.EVENT_KEY))) && ("INTERFACE".equalsIgnoreCase(alarmToForward.getOriginatingManagedEntity().split(" ")[0]))) {
			EnrichedAlarm enrichedAlarmForRubyCNI = null;
			try {
				enrichedAlarmForRubyCNI = alarmToForward.clone();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
			enrichedAlarmForRubyCNI.setCustomFieldValue(GFPFields.CLASSIFICATION, "CNI-CFO");
			enrichedAlarmForRubyCNI.setCustomFieldValue(GFPFields.DOMAIN, "EGS");  
			enrichedAlarmForRubyCNI.setCustomFieldValue(GFPFields.ALERT_ID, (enrichedAlarmForRubyCNI.getCustomFieldValue(GFPFields.ALERT_ID)) + "CNI-CFO");
			service_util.forwardAlarmToNOM(scenario, enrichedAlarmForRubyCNI); 
		}  
	} 

	private static void updateComponentField(EnrichedAlarm enrichedAlarm) {
		String component = enrichedAlarm.getCustomFieldValue(GFPFields.COMPONENT);
		if(component != null) { 
			if(!("CIENA EMUX".equalsIgnoreCase(enrichedAlarm.getDeviceType()))) {
				if(enrichedAlarm.getGcpDeviceType() != null && !(enrichedAlarm.getGcpDeviceType().isEmpty())){
					if(component.contains("deviceType")) { 
						enrichedAlarm.setCustomFieldValue(GFPFields.COMPONENT, component.replaceAll("deviceType=<.*?>", "deviceType=<" + enrichedAlarm.getGcpDeviceType() + ">"));
					} 
				}
			}  
		} 
	}

	private static String getValuePackName(String vpName) {
		if(vpName != null && vpName.contains("-")) {
			return vpName.substring(0, vpName.indexOf("-")); 
		} else { 
			return vpName;
		}
	}

	private static List<String> getTargetUCAInstanceVPs(Scenario scenario, String vpName) {
		List<String> vpNames = new ArrayList<String>();
		UCAInstancesXmlConfiguration ucaInstanceConfiguration = (UCAInstancesXmlConfiguration) service_util.retrieveBeanFromContextXml(scenario, "UCAInstances");
		for (UCAInstance ucaInstnace : ucaInstanceConfiguration.getUCAInstnaces().getUCAInstance()) {
			if(ucaInstnace.getVpNames().getVpName().contains(vpName)) {
				vpNames =  ucaInstnace.getVpNames().getVpName();
			}
		}
		return vpNames;
	}

	private static void populateInfoField(Alarm alarm) {
		String info = alarm.getCustomFieldValue(GFPFields.INFO);
		if(info == null) {
			info = "";
		} 
		if(!(info.contains("AlarmObjectType"))) {
			alarm.setCustomFieldValue(GFPFields.INFO, info + "AlarmObjectType=<" + alarm.getOriginatingManagedEntity().split(" ")[0] + ">"); 
		}
	} 

	private static void generateDecompostionAlarmForCienaCFM(Scenario scenario, EnrichedAlarm alarm) {
		if("50002/100/52".equalsIgnoreCase(alarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
			long now = System.currentTimeMillis();
			String nowStr = Long.toString(now / 1000); 
			String instanceName = alarm.getOriginatingManagedEntity().split(" ")[1];
			EnrichedAlarm enrichedDecompositionAlarm = new EnrichedAlarm(alarm);
			enrichedDecompositionAlarm.setCustomFieldValue(GFPFields.REASON, "PORT_DOWN");
			enrichedDecompositionAlarm.setCustomFieldValue("last-clear-time", "0.0");
			enrichedDecompositionAlarm.setCustomFieldValue("last-update", nowStr);  
			enrichedDecompositionAlarm.setCustomFieldValue("destination", "CFM");
			enrichedDecompositionAlarm.setSourceIdentifier(enrichedDecompositionAlarm.getCustomFieldValue("domain"));
			enrichedDecompositionAlarm.setCustomFieldValue("component", "deviceType=<" + enrichedDecompositionAlarm.getDeviceType() + 
					"> deviceModel=<" + enrichedDecompositionAlarm.getDeviceModel() + 
					"> EVC " + instanceName);
			enrichedDecompositionAlarm.setCustomFieldValue("alert-id", enrichedDecompositionAlarm.getCustomFieldValue(GFPFields.EVENT_KEY) + 
					"-" + instanceName); 
			enrichedDecompositionAlarm.setCustomFieldValue("sm-source-domain", enrichedDecompositionAlarm.getEvcNodeAlarmSource());
			enrichedDecompositionAlarm.setCustomFieldValue("managed-object-class", "EVC");
			enrichedDecompositionAlarm.setCustomFieldValue("managed-object-instance", instanceName); 
			enrichedDecompositionAlarm.setCustomFieldValue("sm-class", "EVC");
			enrichedDecompositionAlarm.setCustomFieldValue("sm-instance", instanceName);
			enrichedDecompositionAlarm.setCustomFieldValue("source", enrichedDecompositionAlarm.getEvcNodeAlarmSource());
			enrichedDecompositionAlarm.setCustomFieldValue("acnaban", enrichedDecompositionAlarm.getEvcNodeAcnaban());
			enrichedDecompositionAlarm.setCustomFieldValue("vrf-name", enrichedDecompositionAlarm.getEvcNodeVrfName());
			enrichedDecompositionAlarm.setCustomFieldValue("evc-name", enrichedDecompositionAlarm.getCustomFieldValue("CircuitId"));
			enrichedDecompositionAlarm.setCustomFieldValue("clci", enrichedDecompositionAlarm.getCustomFieldValue(GFPFields.UNICKT));
			enrichedDecompositionAlarm.setCustomFieldValue("node-name", enrichedDecompositionAlarm.getDeviceName());
			enrichedDecompositionAlarm.setCustomFieldValue("secondary-alert-id", ""); 
			enrichedDecompositionAlarm.setCustomFieldValue(GFPFields.IS_GENERATED_BY_DECOMPOSITION, "true");  
			if (alarm.getPerceivedSeverity() == PerceivedSeverity.CLEAR) {
				enrichedDecompositionAlarm.setCustomFieldValue("reason", "CLEAR");
				enrichedDecompositionAlarm.setCustomFieldValue("last-clear-time", nowStr);
			}
			service_util.forwardAlarmToDecompostion(scenario, enrichedDecompositionAlarm);
		}

	}


	private static String changeCascadeTargetForJuniperESScenarios(
			String cascadeTargetName) {
		String juniperCompletionCascadingTarget = cascadeTargetName;  
		if(cascadeTargetName != null && 
				("JUNIPER_CFMPROCESSING".equalsIgnoreCase(cascadeTargetName) ||
						"JUNIPER_JNXFRUOFFLINE".equalsIgnoreCase(cascadeTargetName) ||
						"LACPTIMEOUT_ENRICHMENT".equalsIgnoreCase(cascadeTargetName))) {
			juniperCompletionCascadingTarget = "JUNIPER_COMPLETION";
		}
		return juniperCompletionCascadingTarget;
	}

	public static void forwardAlarmToDecomposerInstance(EnrichedAlarm enrichedAlarm, String cascadeTargetName) {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "GFPUtil.forwardAlarmToDecomposerInstance()");  
		}    
		Scenario scenario = ScenarioThreadLocal.getScenario();  
		if(enrichedAlarm != null) {
			if(!("IPAG02".equalsIgnoreCase(enrichedAlarm.getCustomFieldValue(GFPFields.G2SUPPRESS)))) {
				CascadeAlarmConfiguration cascadeConf = (CascadeAlarmConfiguration)service_util.retrieveBeanFromContextXml(scenario, "cascadeConfig");   
				CascadeTarget cascadeTarget = cascadeConf.getCascadePolicies().getCascadeTargets().getCascadeTargetByName(cascadeTargetName);
				if(cascadeTarget != null) {
					enrichedAlarm.setTargetValuePack(cascadeTarget.getVpName());  
					enrichedAlarm.setCustomFieldValue(GFPFields.DEVICE_TYPE, enrichedAlarm.getDeviceType());
					enrichedAlarm.setCustomFieldValue(GFPFields.REMOTE_DEVICE_TYPE, enrichedAlarm.getRemoteDeviceType());
					enrichedAlarm.setCustomFieldValue(GFPFields.REMOTE_DEVICE_NAME, enrichedAlarm.getRemoteDeviceName());
					enrichedAlarm.setCustomFieldValue(GFPFields.REMOTE_DEVICE_MODEL, enrichedAlarm.getRemoteDeviceModel());
					enrichedAlarm.setCustomFieldValue(GFPFields.REMOTE_PPORT_INSTANCE_NAME, enrichedAlarm.getRemotePportInstanceName()); 
					enrichedAlarm.setCustomFieldValue(GFPFields.DEVICE_NAME, enrichedAlarm.getDeviceName());    
					enrichedAlarm.setCustomFieldValue(GFPFields.DEVICE_MODEL, enrichedAlarm.getDeviceModel());   

					service_util.forwardAlarmToDecomposerInstance(scenario, enrichedAlarm); 
				}
			} 
		} else {
			if(log.isTraceEnabled())
				log.trace("GFPUtil.forwardAlarmToDecomposeInstance(): INPUT Alarm cannot be NULL");     
		}
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "GFPUtil.forwardOrCascadeAlarm()");  
		}    
	} 

	private static String getCpeCdcNodeType(EnrichedAlarm enrichedAlarm) {
		String nodetype = "";
		if("JUNIPER MX SERIES".equalsIgnoreCase(enrichedAlarm.getDeviceType())) {
			nodetype = "IPAG";
		} else if("ADTRAN 5000 SERIES".equalsIgnoreCase(enrichedAlarm.getDeviceType()) || 
				"CIENA EMUX".equalsIgnoreCase(enrichedAlarm.getDeviceType())) {
			nodetype = "EMUX";
		} else if("ADTRAN 800 SERIES".equalsIgnoreCase(enrichedAlarm.getDeviceType()) ||
				"CIENA NTE".equalsIgnoreCase(enrichedAlarm.getDeviceType())) {
			nodetype = "NTE";
		} else if("VR1".equalsIgnoreCase(enrichedAlarm.getDeviceType())) {
			nodetype = "VPLS";
		} else if("EGS-CFO".equalsIgnoreCase(enrichedAlarm.getCustomFieldValue(GFPFields.CLASSIFICATION))||
				"EGS-NFO".equalsIgnoreCase(enrichedAlarm.getCustomFieldValue(GFPFields.CLASSIFICATION))) {
			nodetype = "EGS";
		}
		return nodetype;
	}

	/**
	 * Parses the reasonCode field to get the following attributes
	 * Sample is: ccgcil401me3-1234:1001:ge-1/0/5.2-4
	 *	•	r_dev_name (L3 Router "DNS Name) ccgcil401me3
	 *	•	r_nvlan_idtop (SVLAN) 1234
	 *	•	 r_vlan_id (CVLAN) 1001
	 *	•	 r_aid (AID) ge-1/0/5.2
	 *	•	 r_md_level (MD Level) 4
	 * @param enrichedAlarm 
	 */
	public static ReasonCode parseReasonCode(Alarm enrichedAlarm) {
		String reasonCode = enrichedAlarm.getCustomFieldValue(GFPFields.REASON_CODE);
		ReasonCode  reasonCodeObj = new ReasonCode();
		if(reasonCode != null && !reasonCode.isEmpty()) {
			String[] values = reasonCode.split("\\:");
			if(values != null && values.length > 0) {  
				if(values.length > 2) {  
					String devNameSvVan = values[0];
					reasonCodeObj.setrVlanId(values[1]);  
					String rAidMdLevel = values[2]; 
					String[] values1 = devNameSvVan.split("\\-");
					if(values1 != null && values1.length > 1) {
						reasonCodeObj.setrDevName(values1[0]);

						reasonCodeObj.setrNvlanIdTop(values1[1]);
					} 
					String[] values3 = rAidMdLevel.split("\\-");
					if(values3 != null && values3.length > 1) {
						reasonCodeObj.setrAid(values3[0]);
						reasonCodeObj.setrMdLevel(values3[1]); 
					}
				}
			}
		} 
		return reasonCodeObj; 
	}

	/**
	 * Parses the reason of the alarm to get the vrf name
	 * @param reason
	 * @return
	 */
	public static Reason getReasonObj(String reason) {
		Reason reasonObj = new Reason();
		if(reason != null && !reason.isEmpty()) {
			if(reason.contains("VRF=")) {
				String vrfString = reason.substring(reason.indexOf("VRF="));
				String[] vrfs = vrfString.split("\\=");
				reasonObj.setReasonTmp(vrfs[0]);
				reasonObj.setVrfInAlarm(vrfs[1]);   
			}
		}  
		return reasonObj;       
	}

	/**
	 * gets list of pmoss traps
	 * @return
	 */
	public static List<String> getPmossTrapsList() {
		List<String> pmossTraps = new ArrayList<String>();
		pmossTraps.add("50005/6/7001");
		pmossTraps.add("50005/6/7003");
		pmossTraps.add("50005/6/7005");
		pmossTraps.add("50005/6/7007");
		pmossTraps.add("50005/6/7013");
		pmossTraps.add("50005/6/7015"); 
		pmossTraps.add("50005/6/6001");

		return pmossTraps;
	}
	/**
	 * gets list of pmoss traps
	 * @return
	 */
	public static List<String> getPmossCompassTrapsList() {
		List<String> pmossCompassTraps = new ArrayList<String>();
		pmossCompassTraps.add("50005/6/9130"); 
		pmossCompassTraps.add("50005/6/9200");
		pmossCompassTraps.add("50005/6/9208");
		pmossCompassTraps.add("50005/6/9202");

		return pmossCompassTraps; 
	}

	/**
	 * gets list of ciena clci traps
	 * @return
	 */
	public static List<String> getCienaClciTrapsList() {
		List<String> pmossTraps = new ArrayList<String>();
		pmossTraps.add("50002/100/3");
		pmossTraps.add("50002/100/4");
		pmossTraps.add("50002/100/5");
		pmossTraps.add("50002/100/6");
		pmossTraps.add("50002/100/7");
		pmossTraps.add("50002/100/8"); 
		pmossTraps.add("50002/100/10"); 
		pmossTraps.add("50002/100/12"); 
		pmossTraps.add("50002/100/13"); 
		pmossTraps.add("50002/100/17"); 
		pmossTraps.add("50002/100/18"); 
		pmossTraps.add("50002/100/25"); 
		pmossTraps.add("50002/100/26"); 
		pmossTraps.add("50002/100/41"); 
		pmossTraps.add("50002/100/42"); 
		pmossTraps.add("50002/100/43"); 
		pmossTraps.add("50002/100/44"); 
		pmossTraps.add("50002/100/45"); 
		pmossTraps.add("50002/100/46"); 
		pmossTraps.add("50002/100/50"); 
		pmossTraps.add("50002/100/53"); 
		pmossTraps.add("50002/100/54"); 
		pmossTraps.add("50002/100/56"); 
		pmossTraps.add("50002/100/57"); 

		return pmossTraps;
	}	

	/**
	 * gets list of pport suppression traps
	 * @return
	 */
	public static List<String> getPportRemoteDeciveSuppressionTrapsList() {
		List<String> pportSuppresionTraps = new ArrayList<String>();
		pportSuppresionTraps.add("50004/1/10");
		pportSuppresionTraps.add("50004/1/18");
		pportSuppresionTraps.add("50004/1/19");
		pportSuppresionTraps.add("50004/1/20");
		pportSuppresionTraps.add("50004/1/21");

		return pportSuppresionTraps;
	} 

	public static List<String> getCienaDecompositionAlarms() {
		List<String> pmossCompassTraps = new ArrayList<String>();
		pmossCompassTraps.add("50002/100/14"); 
		pmossCompassTraps.add("50002/100/19");
		pmossCompassTraps.add("50002/100/22");
		pmossCompassTraps.add("50004/1/10");

		return pmossCompassTraps; 
	}
	public static List<String> getAdtranDecompositionAlarms() {
		List<String> pmossCompassTraps = new ArrayList<String>();
		pmossCompassTraps.add("50001/100/1"); 
		pmossCompassTraps.add("50001/100/7");
		pmossCompassTraps.add("50001/100/42");
		pmossCompassTraps.add("50001/100/43");
		pmossCompassTraps.add("50001/100/48"); 
		pmossCompassTraps.add("50001/100/20");
		pmossCompassTraps.add("50001/100/39");
		pmossCompassTraps.add("50001/100/45");
		pmossCompassTraps.add("50001/100/46"); 
		pmossCompassTraps.add("50001/100/47");
		pmossCompassTraps.add("50001/100/48");
		pmossCompassTraps.add("50001/100/49");
		pmossCompassTraps.add("50001/100/50"); 
		pmossCompassTraps.add("50001/100/78");
		pmossCompassTraps.add("50001/100/79");
		pmossCompassTraps.add("50001/100/80");
		pmossCompassTraps.add("50001/100/81");
		return pmossCompassTraps; 
	}
	public static List<String> getJuniperDecompositionAlarms() {
		List<String> pmossCompassTraps = new ArrayList<String>();
		pmossCompassTraps.add("50002/100/21"); 
		pmossCompassTraps.add("50003/100/19");
		pmossCompassTraps.add("50003/100/1");
		pmossCompassTraps.add("50003/100/6");
		pmossCompassTraps.add("50003/100/7"); 
		pmossCompassTraps.add("50003/100/23");

		return pmossCompassTraps; 
	}

	public static List<String> getJuniperHealthTraps() {
		List<String> juniperHealthTraps = new ArrayList<String>();
		juniperHealthTraps.add("50003/100/58916874"); 
		juniperHealthTraps.add("50005/100/58916875");
		juniperHealthTraps.add("50004/2/58916876");
		juniperHealthTraps.add("50004/2/58916877");

		return juniperHealthTraps; 
	}	

	public static List<String> getAdtranCFMTraps() {
		List<String> juniperHealthTraps = new ArrayList<String>();
		juniperHealthTraps.add("50001/100/61"); 
		juniperHealthTraps.add("50001/100/62");
		juniperHealthTraps.add("50001/100/63");
		juniperHealthTraps.add("50001/100/64");
		juniperHealthTraps.add("50001/100/65");

		return juniperHealthTraps; 
	}	

	public static List<String> getAdtranJuniperTraps() {
		List<String> adtranJuniperTraps = new ArrayList<String>();
		adtranJuniperTraps.add("50003/100/12"); 
		adtranJuniperTraps.add("50003/100/13"); 
		adtranJuniperTraps.add("50003/100/14");
		adtranJuniperTraps.add("50003/100/15");
		adtranJuniperTraps.add("50003/100/16"); 

		return adtranJuniperTraps; 
	}

	public static List<String> getConditionalDecomposedTraps() {
		List<String> juniperHealthTraps = new ArrayList<String>();
		juniperHealthTraps.add("50003/100/1"); 
		juniperHealthTraps.add("50002/100/19");
		juniperHealthTraps.add("50002/100/21");
		juniperHealthTraps.add("50004/1/10");  

		return juniperHealthTraps; 
	}

	public static ExecutionEngine getCypherEngine() {
		Scenario scenario = ScenarioThreadLocal.getScenario();
		CypherEngine cypherEngine = (CypherEngine)service_util.retrieveBeanFromContextXml(scenario, "CypherEngine");
		return cypherEngine.getEngine(); 
	}

	public static boolean isAafDaAlarm(EnrichedAlarm enrichedAlarm) {
		String aafDa = "";
		if(enrichedAlarm.getRemoteDeviceType() != null && enrichedAlarm.getDeviceType() != null) {
			if((enrichedAlarm.getRemoteDeviceType().toUpperCase().contains("CIENA NTE") &&
					(enrichedAlarm.getDeviceType().toUpperCase().contains("JUNIPER MX")))) {
				aafDa = enrichedAlarm.getRemotePportAafdaRole();

			}
			else if((enrichedAlarm.getRemoteDeviceType().toUpperCase().contains("JUNIPER MX") &&
					(enrichedAlarm.getDeviceType().toUpperCase().contains("CIENA NTE")))) {
				aafDa = enrichedAlarm.getAafDaRole();			
			} 
		} 
		boolean ret = false;
		if(aafDa != null && !(aafDa.isEmpty())) {
			if(aafDa.equals(GFPFields.AAF_PRIMARY) || aafDa.equals(GFPFields.AAF_SECONDARY) || aafDa.equals(GFPFields.DA)) {
				ret = true;   
			}
		}
		return ret;  
	}

	public static void forwardAlarmtoAdtranInstnance(Scenario scenario, EnrichedAlarm enrichedAlarm) {
		log.trace("GFPUtil.forwardOrCascadeAlarm() forwarding to Adtran isntnance " + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER));
		setAdditionalCustomFieldsForEnrichedAlarm(enrichedAlarm);
		service_util.forwardAlarmToAdtranInstance(scenario, enrichedAlarm);

	}

	public static void forwardAlarmtoCienaInstnance(Scenario scenario, EnrichedAlarm enrichedAlarm) {
		log.trace("GFPUtil.forwardOrCascadeAlarm() forwarding to Ciena isntnance " + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER));
		setAdditionalCustomFieldsForEnrichedAlarm(enrichedAlarm);
		service_util.forwardAlarmToCienaInstance(scenario, enrichedAlarm);

	}

	public static void forwardAlarmtoJuniperInstnance(Scenario scenario, EnrichedAlarm enrichedAlarm) {
		log.trace("GFPUtil.forwardOrCascadeAlarm() forwarding to Juniper isntnance " + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER));
		setAdditionalCustomFieldsForEnrichedAlarm(enrichedAlarm);
		service_util.forwardAlarmToJuniperInstance(scenario, enrichedAlarm);

	}	
	public static EnrichedAlarm populateEnrichedAlarmObj(Alarm alarm) {
		EnrichedAlarm enrichedAlarm = new EnrichedAlarm(alarm);
		enrichedAlarm.setDeviceType(enrichedAlarm.getCustomFieldValue(GFPFields.DEVICE_TYPE));
		enrichedAlarm.setDeviceName(enrichedAlarm.getCustomFieldValue(GFPFields.DEVICE_NAME));
		enrichedAlarm.setDeviceModel(enrichedAlarm.getCustomFieldValue(GFPFields.DEVICE_MODEL));
		enrichedAlarm.setRemoteDeviceType(enrichedAlarm.getCustomFieldValue(GFPFields.REMOTE_DEVICE_TYPE));
		enrichedAlarm.setRemoteDeviceModel(enrichedAlarm.getCustomFieldValue(GFPFields.REMOTE_DEVICE_MODEL));
		enrichedAlarm.setRemoteDeviceName(enrichedAlarm.getCustomFieldValue(GFPFields.REMOTE_DEVICE_NAME));
		enrichedAlarm.setRemoteDeviceIpaddr(enrichedAlarm.getCustomFieldValue(GFPFields.REMOTE_DEVICE_IPADDR));
		enrichedAlarm.setRemotePportInstanceName(enrichedAlarm.getCustomFieldValue(GFPFields.REMOTE_PPORT_INSTANCE_NAME));
		enrichedAlarm.setRemotePportAafdaRole(enrichedAlarm.getCustomFieldValue(GFPFields.REMOTE_PPORT_AAFDAROLE));
		enrichedAlarm.setAafDaRole(enrichedAlarm.getCustomFieldValue(GFPFields.AAFDAROLE));
		enrichedAlarm.setDiverseCircuitID(enrichedAlarm.getCustomFieldValue(GFPFields.DIVERSECIRCUITID));
		enrichedAlarm.setRelatedCLLI(enrichedAlarm.getCustomFieldValue(GFPFields.RELATEDCLLI));
		enrichedAlarm.setRelatedPortAID(enrichedAlarm.getCustomFieldValue(GFPFields.RELATEDPORTAID)); 
		enrichedAlarm.setLocalPeeringPort(enrichedAlarm.getCustomFieldValue(GFPFields.LOCALPEERINGPORT));
		enrichedAlarm.setRemotePeeringPort(enrichedAlarm.getCustomFieldValue(GFPFields.REMOTEPEERINGPORT));
		enrichedAlarm.setRemotePortId(enrichedAlarm.getCustomFieldValue(GFPFields.REMOTE_PORT_AID)); 
		enrichedAlarm.setGcpDeviceType(enrichedAlarm.getCustomFieldValue(GFPFields.GCP_DEVICE_TYPE)); 
		enrichedAlarm.setDeviceRole(enrichedAlarm.getCustomFieldValue(GFPFields.DEVICEROLE));
		enrichedAlarm.setDeviceSubRole(enrichedAlarm.getCustomFieldValue(GFPFields.DEVICESUBROLE));
		enrichedAlarm.setSendToCdc(Boolean.valueOf(enrichedAlarm.getCustomFieldValue(GFPFields.ISSENDTOCDC))); 
		enrichedAlarm.setEvcName(enrichedAlarm.getCustomFieldValue(GFPFields.EVC_NAME));  
		enrichedAlarm.setIssendToCpeCdc(Boolean.valueOf(enrichedAlarm.getCustomFieldValue(GFPFields.ISSENDTOCPECDC)));  
		if(enrichedAlarm.getCustomFieldValue(GFPFields.REDUNDANTNNIPORTS) != null && !(enrichedAlarm.getCustomFieldValue(GFPFields.REDUNDANTNNIPORTS).isEmpty())) {
			enrichedAlarm.setRedundantNNIPorts(Arrays.asList(enrichedAlarm.getCustomFieldValue(GFPFields.REDUNDANTNNIPORTS).substring(1, enrichedAlarm.getCustomFieldValue(GFPFields.REDUNDANTNNIPORTS).length()-1).split(",\\s*")));
		} 

		return enrichedAlarm;  
	}

	private static void setAdditionalCustomFieldsForEnrichedAlarm(
			EnrichedAlarm enrichedAlarm) {
		enrichedAlarm.setCustomFieldValue(GFPFields.DEVICE_TYPE, enrichedAlarm.getDeviceType());
		enrichedAlarm.setCustomFieldValue(GFPFields.DEVICE_MODEL, enrichedAlarm.getDeviceModel());
		enrichedAlarm.setCustomFieldValue(GFPFields.DEVICE_NAME, enrichedAlarm.getDeviceName());
		enrichedAlarm.setCustomFieldValue(GFPFields.REMOTE_DEVICE_TYPE, enrichedAlarm.getRemoteDeviceType());
		enrichedAlarm.setCustomFieldValue(GFPFields.REMOTE_DEVICE_MODEL, enrichedAlarm.getRemoteDeviceModel());
		enrichedAlarm.setCustomFieldValue(GFPFields.REMOTE_DEVICE_NAME, enrichedAlarm.getRemoteDeviceName());
		enrichedAlarm.setCustomFieldValue(GFPFields.REMOTE_PPORT_INSTANCE_NAME, enrichedAlarm.getRemotePportInstanceName());
		if(enrichedAlarm.getRedundantNNIPorts() != null && !(enrichedAlarm.getRedundantNNIPorts().isEmpty())) {
			enrichedAlarm.setCustomFieldValue(GFPFields.REDUNDANTNNIPORTS, enrichedAlarm.getRedundantNNIPorts().toString());
		}
		enrichedAlarm.setCustomFieldValue(GFPFields.REMOTE_DEVICE_IPADDR, enrichedAlarm.getRemoteDeviceIpaddr());
		enrichedAlarm.setCustomFieldValue(GFPFields.REMOTE_PPORT_AAFDAROLE, enrichedAlarm.getRemotePportAafdaRole());
		enrichedAlarm.setCustomFieldValue(GFPFields.AAFDAROLE, enrichedAlarm.getAafDaRole());
		enrichedAlarm.setCustomFieldValue(GFPFields.DIVERSECIRCUITID, enrichedAlarm.getDiverseCircuitID());
		enrichedAlarm.setCustomFieldValue(GFPFields.RELATEDCLLI, enrichedAlarm.getRelatedCLLI());
		enrichedAlarm.setCustomFieldValue(GFPFields.RELATEDPORTAID, enrichedAlarm.getRelatedPortAID());
		enrichedAlarm.setCustomFieldValue(GFPFields.LOCALPEERINGPORT, enrichedAlarm.getLocalPeeringPort());
		enrichedAlarm.setCustomFieldValue(GFPFields.REMOTEPEERINGPORT, enrichedAlarm.getRemotePeeringPort());
		enrichedAlarm.setCustomFieldValue(GFPFields.REMOTE_PORT_AID, enrichedAlarm.getRemotePortAid());
		enrichedAlarm.setCustomFieldValue(GFPFields.GCP_DEVICE_TYPE, enrichedAlarm.getGcpDeviceType()); 
		enrichedAlarm.setCustomFieldValue(GFPFields.DEVICEROLE, enrichedAlarm.getDeviceRole()); 
		enrichedAlarm.setCustomFieldValue(GFPFields.DEVICESUBROLE, enrichedAlarm.getDeviceSubRole());
		enrichedAlarm.setCustomFieldValue(GFPFields.ISSENDTOCDC, String.valueOf(enrichedAlarm.isSendToCdc()));
		enrichedAlarm.setCustomFieldValue(GFPFields.ISSENDTOCPECDC, String.valueOf(enrichedAlarm.getIssendToCpeCdc()));  
		enrichedAlarm.setCustomFieldValue(GFPFields.EVC_NAME, String.valueOf(enrichedAlarm.getEvcName())); 
		enrichedAlarm.setCustomFieldValue(GFPFields.REMOVE_ADDITIONAL_CUSTOM_FIELDS, "YES");      
	}

	private static void removeExtraCustomFields(EnrichedAlarm enrichedAlarm) {
		//if("YES".equalsIgnoreCase(enrichedAlarm.getCustomFieldValue(GFPFields.REMOVE_ADDITIONAL_CUSTOM_FIELDS))) {
		for(CustomField customFiled : enrichedAlarm.getCustomFields().getCustomField()) {
			if(customFiled.getName().equalsIgnoreCase(GFPFields.DEVICE_TYPE) || 
					customFiled.getName().equalsIgnoreCase(GFPFields.DEVICE_NAME) ||
					customFiled.getName().equalsIgnoreCase(GFPFields.DEVICE_MODEL) ||
					//	customFiled.getName().equalsIgnoreCase(GFPFields.REMOTE_DEVICE_TYPE) ||
					//	customFiled.getName().equalsIgnoreCase(GFPFields.REMOTE_DEVICE_MODEL) ||
					//	customFiled.getName().equalsIgnoreCase(GFPFields.REMOTE_DEVICE_NAME) ||
					//	customFiled.getName().equalsIgnoreCase(GFPFields.REMOTE_DEVICE_IPADDR) ||
					//	customFiled.getName().equalsIgnoreCase(GFPFields.REMOTE_PPORT_INSTANCE_NAME) ||
					customFiled.getName().equalsIgnoreCase(GFPFields.REMOTE_PPORT_AAFDAROLE) ||
					customFiled.getName().equalsIgnoreCase(GFPFields.DIVERSECIRCUITID) ||
					customFiled.getName().equalsIgnoreCase(GFPFields.RELATEDCLLI) ||
					customFiled.getName().equalsIgnoreCase(GFPFields.RELATEDPORTAID) ||
					customFiled.getName().equalsIgnoreCase(GFPFields.LOCALPEERINGPORT) ||
					customFiled.getName().equalsIgnoreCase(GFPFields.REMOTEPEERINGPORT) || 
					//	customFiled.getName().equalsIgnoreCase(GFPFields.REMOTE_PORT_AID) ||
					customFiled.getName().equalsIgnoreCase(GFPFields.GCP_DEVICE_TYPE) || 
					customFiled.getName().equalsIgnoreCase(GFPFields.DEVICEROLE) || 
					customFiled.getName().equalsIgnoreCase(GFPFields.DEVICESUBROLE) || 
					customFiled.getName().equalsIgnoreCase(GFPFields.ISSENDTOCDC) || 
					customFiled.getName().equalsIgnoreCase(GFPFields.ISSENDTOCPECDC) ||
					customFiled.getName().equalsIgnoreCase(GFPFields.EVC_NAME) ||
					customFiled.getName().equalsIgnoreCase(GFPFields.REDUNDANTNNIPORTS)) {  
				enrichedAlarm.getCustomFields().getCustomField().remove(customFiled);  
			}
		}
		//}
	}


}
