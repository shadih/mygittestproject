/**
 * This Problem is empty and ready to define methods to customize this problem
 */
package com.hp.uca.expert.vp.pd.problem;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.Pri_Sec_Alarm;
import com.att.gfp.helper.GFPFields; 
import com.att.gfp.helper.GFPUtil;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.vp.pd.config.LongItem;
import com.hp.uca.expert.vp.pd.core.PriSec_ProblemDefault;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;
import com.hp.uca.expert.vp.pd.services.PD_Service_Group;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;

/**
 * @author df
 * 
 */
public final class AAF_DA_AlarmProcessing extends PriSec_ProblemDefault implements
ProblemInterface {

	//private static final String CE_NODETYPE = "CE";
	//private static final String PE_NODETYPE = "PE";
	protected static final String LINKDOWN_KEY = "50003/100/1";
	protected static final String DA = "DA";
	protected static final String AAF_SECONDARY = "AAF-SECONDARY";
	protected static final String AAF_PRIMARY = "AAF-PRIMARY";


	private Logger log = LoggerFactory.getLogger(AAF_DA_AlarmProcessing.class);

	public AAF_DA_AlarmProcessing() {
		super();

	}


	@Override
	public boolean isMatchingSubAlarmCriteria(Alarm a, Group group) throws Exception {
		boolean ret = true;
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "isMatchingSubAlarmCriteria()");
		}

		if ( a instanceof Pri_Sec_Alarm)  {
			Pri_Sec_Alarm alarm = (Pri_Sec_Alarm) a;	
			String aafDa = alarm.getRemotePportAafdaRole();

			if (log.isTraceEnabled())
				log.trace("AAF_DA role for remote device is: " + aafDa);
			if(alarm.getRemoteDeviceType() != null && alarm.getDeviceType() != null) {
				if((alarm.getRemoteDeviceType().toUpperCase().contains("CIENA NTE") &&
						(alarm.getDeviceType().toUpperCase().contains("JUNIPER MX")))) {
					if(aafDa != null && !(aafDa.isEmpty())) {
						if(aafDa.equals(AAF_PRIMARY) || aafDa.equals(AAF_SECONDARY) || aafDa.equals(DA)) {
							if(a.getPerceivedSeverity().equals(PerceivedSeverity.CRITICAL))
								a.setPerceivedSeverity(PerceivedSeverity.MAJOR);
							setInfo1FieldForJuniperAlarms(alarm); 
							ret = true; 
						}
					}
				} else if((alarm.getRemoteDeviceType().toUpperCase().contains("JUNIPER MX") &&
						(alarm.getDeviceType().toUpperCase().contains("CIENA NTE")))) {
					String aafDaCiena = alarm.getAafDaRole();			
					if(aafDaCiena != null && !(aafDaCiena.isEmpty())) {
						if(aafDaCiena.equals(AAF_PRIMARY) || aafDaCiena.equals(AAF_SECONDARY) || aafDaCiena.equals(DA)) {
							if(a.getPerceivedSeverity().equals(PerceivedSeverity.CRITICAL))
								a.setPerceivedSeverity(PerceivedSeverity.MAJOR);
							setInfo1FieldForCienaAlarms(alarm);
							ret = true; 
						}
					}
				}
			}			
		} 
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingSubAlarmCriteria() " + "[" + ret + "], group name = " + group.getName());
		return ret;
	}

	private void setInfo1FieldForCienaAlarms(Pri_Sec_Alarm alarm) {
		StringBuilder info1s = new StringBuilder();
		if(alarm.getCustomFieldValue(GFPFields.INFO1) != null && !(alarm.getCustomFieldValue(GFPFields.INFO1).isEmpty())) {
			info1s.append(alarm.getCustomFieldValue(GFPFields.INFO1));
		} else {
			alarm.setCustomFieldValue(GFPFields.INFO1,"");
		}
		if(alarm.getCustomFieldValue(GFPFields.INFO1) != null && !(alarm.getCustomFieldValue(GFPFields.INFO1).contains("AAFDARole=<"))) {
			info1s.append(" AAFDARole=<" + alarm.getAafDaRole() + ">" );
		}
		if(alarm.getCustomFieldValue(GFPFields.INFO1) != null && !(alarm.getCustomFieldValue(GFPFields.INFO1).contains("DiverseCircuitID=<"))) {
			info1s.append(" DiverseCircuitID=<" + alarm.getDiverseCircuitID() + ">");
		}
		if(alarm.getRelatedCLLI() != null && !(alarm.getRelatedCLLI().isEmpty())) { 
			if(alarm.getCustomFieldValue(GFPFields.INFO1) != null && !(alarm.getCustomFieldValue(GFPFields.INFO1).contains("RelatedCLLI=<"))) {
				info1s.append(" RelatedCLLI=<" + alarm.getRelatedCLLI() + ">");
			}
		}
		if(alarm.getRelatedPortAID() != null && !(alarm.getRelatedPortAID().isEmpty())) {
			if(alarm.getCustomFieldValue(GFPFields.INFO1) != null && !(alarm.getCustomFieldValue(GFPFields.INFO1).contains("RelatedPortAID=<"))) {
				info1s.append(" RelatedPortAID=<" + alarm.getRelatedPortAID() + "> ");
			}
		}
		alarm.setCustomFieldValue(GFPFields.INFO1, info1s.toString()); 

	}

	private void setInfo1FieldForJuniperAlarms(Pri_Sec_Alarm alarm) {
		StringBuilder info1s = new StringBuilder();
		if(alarm.getCustomFieldValue(GFPFields.INFO1) != null && !(alarm.getCustomFieldValue(GFPFields.INFO1).isEmpty())) {
			info1s.append(alarm.getCustomFieldValue(GFPFields.INFO1));
		} else {
			alarm.setCustomFieldValue(GFPFields.INFO1,"");
		}
		if(alarm.getCustomFieldValue(GFPFields.INFO1) != null && !(alarm.getCustomFieldValue(GFPFields.INFO1).contains("AAFDARole=<"))) {
			info1s.append(" AAFDARole=<" + alarm.getRemotePportAafdaRole() + ">" );
		}
		if(alarm.getCustomFieldValue(GFPFields.INFO1) != null && !(alarm.getCustomFieldValue(GFPFields.INFO1).contains("DiverseCircuitID=<"))) {
			info1s.append(" DiverseCircuitID=<" + alarm.getRemotePportDiverseCktId() + ">");
		}
		if(alarm.getRelatedCLLI() != null && !(alarm.getRelatedCLLI().isEmpty())) {
			if(alarm.getCustomFieldValue(GFPFields.INFO1) != null && !(alarm.getCustomFieldValue(GFPFields.INFO1).contains("RelatedCLLI=<"))) {
				info1s.append(" RelatedCLLI=<" + alarm.getRelatedCLLI() + ">");
			}
		} 
		if(alarm.getRelatedPortAID() != null && !(alarm.getRelatedPortAID().isEmpty())) {
			if(alarm.getCustomFieldValue(GFPFields.INFO1) != null && !(alarm.getCustomFieldValue(GFPFields.INFO1).contains("RelatedPortAID=<"))) {
				info1s.append(" RelatedPortAID=<" + alarm.getRelatedPortAID() + "> "); 
			} 
		}
		alarm.setCustomFieldValue(GFPFields.INFO1, info1s.toString()); 

	}

	@Override
	public boolean isMatchingTriggerAlarmCriteria(Alarm a) throws Exception {
		boolean ret = false;

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "isMatchingTriggerAlarmCriteria()");
		}

		if ( a instanceof Pri_Sec_Alarm)  {
			Pri_Sec_Alarm alarm = (Pri_Sec_Alarm) a;	
			String aafDa = alarm.getRemotePportAafdaRole();
			String remoteDeviceType = "";
			String deviceType = "";
			if(alarm.getRemoteDeviceType() != null) {
				remoteDeviceType = alarm.getRemoteDeviceType().toUpperCase();
			}
			if(alarm.getDeviceType() != null) {
				deviceType = alarm.getDeviceType().toUpperCase();
			}

			if (log.isTraceEnabled())
				log.trace("AAF_DA role for remote device is: " + aafDa);

			if(remoteDeviceType != null && deviceType != null &&
					remoteDeviceType.contains("CIENA NTE") &&
					deviceType.contains("JUNIPER MX")) {

				if(aafDa != null && !(aafDa.isEmpty())) {
					if(aafDa.equals(AAF_PRIMARY) || aafDa.equals(AAF_SECONDARY) || aafDa.equals(DA)) {
						if(a.getPerceivedSeverity().equals(PerceivedSeverity.CRITICAL))
							a.setPerceivedSeverity(PerceivedSeverity.MAJOR);
						setInfo1FieldForJuniperAlarms(alarm);
						ret = true; 
					}
				}
			} else if(remoteDeviceType != null && deviceType != null &&
					remoteDeviceType.contains("JUNIPER MX") &&
					deviceType.contains("CIENA NTE")) {
				String aafDaCiena = alarm.getAafDaRole();			
				if(aafDaCiena != null && !(aafDaCiena.isEmpty())) {
					if(aafDaCiena.equals(AAF_PRIMARY) || aafDaCiena.equals(AAF_SECONDARY) || aafDaCiena.equals(DA)) {
						if(a.getPerceivedSeverity().equals(PerceivedSeverity.CRITICAL))
							a.setPerceivedSeverity(PerceivedSeverity.MAJOR);
						setInfo1FieldForCienaAlarms(alarm);
						ret = true;  
					}
				}
			}

			if ( ret ) {
				log.info("Alarm " + a.getIdentifier() + " Sequence # " + a.getCustomFieldValue(GFPFields.SEQNUMBER) + " will be held up to 3 minutes waiting for Primary Secondary Correlation.");

				// this is what we use to determine how long to hold the trigger before we let it go
				long afterTrigger = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger();
				long ageLeft = alarm.TimeRemaining(afterTrigger); 
				long holdTime = 0;

				if(maxDelayedEvent != null && alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(maxDelayedEvent))
					holdTime = ageLeft;
				else
					holdTime = ageLeft + maxSAge; 

				String watchdogDesc = "Creating watchdog for:" + getProblemPolicy().getName();

				Util.setTriggerWatch((Pri_Sec_Alarm)alarm, Pri_Sec_Alarm.class, "simpleSendCallBack", holdTime, watchdogDesc);
			}
		}
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingTriggerAlarmCriteria() " + "[" + ret + "]");

		return ret;
	}

	@Override
	public List<String> computeProblemEntity(Alarm a) throws Exception {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "computeProblemEntity()");
		}

		List<String> problemEntities = new ArrayList<String>();
		Pri_Sec_Alarm alarm = (Pri_Sec_Alarm) a;

		String problemEntity;

		if(alarm.getLocalPeeringPort() != null) {
			problemEntities.add(alarm.getLocalPeeringPort()); 
		} else if(alarm.getRemotePeeringPort() != null) {
			problemEntities.add(alarm.getRemotePeeringPort());
		}
		problemEntity = a.getOriginatingManagedEntity().split(" ")[1];	 
		problemEntities.add(problemEntity);
		// the problem entity is the remote port to the juniper port that has the 
		// link down alarm
		//		if(alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(LINKDOWN_KEY)) {
		//			// this is the link down alarm so use the peering port
		//			if(alarm.getLocalPeeringPort() != null) {
		//				problemEntities.add(alarm.getLocalPeeringPort()); 
		//			} else if(alarm.getRemotePeeringPort() != null) {
		//				problemEntities.add(alarm.getRemotePeeringPort());
		//			}
		//			if(problemEntities.size() == 0) {
		//				problemEntity = a.getOriginatingManagedEntity().split(" ")[1];
		//			}   
		//		} else {
		//			// this is not the link down alarm so add the port instance
		//			problemEntity = a.getOriginatingManagedEntity().split(" ")[1];	 
		//
		//			problemEntities.add(problemEntity);
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
		Pri_Sec_Alarm trigger = (Pri_Sec_Alarm) group.getTrigger();
		Pri_Sec_Alarm a = (Pri_Sec_Alarm) alarm;

		// test to see if this is the trigger.   If no then cascade the alarm, if yes
		// do the suppression checks.
		//		if(a == trigger)			
		//			AlarmIsTrigger(trigger);
		if (group != null && alarm != group.getTrigger()) { 

			if (log.isTraceEnabled())
				log.trace("This alarm is not the trigger: " + alarm.getIdentifier());

			if(GFPUtil.retrieveFeTimeFromCustomFields(alarm) > GFPUtil.retrieveFeTimeFromCustomFields(trigger)) {
				alarm.setPerceivedSeverity(PerceivedSeverity.CRITICAL);
				if(!alarm.getCustomFieldValue(GFPFields.G2SUPPRESS).equals("IPAG02")){
					if("50003/100/1".equalsIgnoreCase(alarm.getCustomFieldValue(GFPFields.EVENT_KEY)) ||
							"50002/100/21".equalsIgnoreCase(alarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
						GFPUtil.forwardAlarmToDecomposerInstance(a, "JUNIPER_DECOMPOSER");
						a.setDecomposed(true);
					} else {
						GFPUtil.forwardAlarmToDecomposerInstance(a, "CIENA_DECOMPOSER");
						a.setDecomposed(true);
					}
				}
			} else {
				trigger.setPerceivedSeverity(PerceivedSeverity.CRITICAL);
				if(!alarm.getCustomFieldValue(GFPFields.G2SUPPRESS).equals("IPAG02")) {
					if("50003/100/1".equalsIgnoreCase(trigger.getCustomFieldValue(GFPFields.EVENT_KEY)) ||
							"50002/100/21".equalsIgnoreCase(trigger.getCustomFieldValue(GFPFields.EVENT_KEY))) {
						GFPUtil.forwardAlarmToDecomposerInstance(trigger, "JUNIPER_DECOMPOSER"); 
						trigger.setDecomposed(true);
					} else {  
						GFPUtil.forwardAlarmToDecomposerInstance(trigger, "CIENA_DECOMPOSER");
						trigger.setDecomposed(true);
					}
				}
			} 		



			//		AlarmIsNotTrigger(trigger, a);

			// is this subalarm a trigger for any group?   If it is then we don't want
			// to send the alarm here.   We will wait till all the triggers have been
			// evaluated then send it (watch callback).
			for (Group possibleGroup : PD_Service_Group.getGroupsOfAnAlarm(alarm, null)) {
				if(possibleGroup.getTrigger() == alarm) {
					isTriggerForOtherGroup = true;
					if (log.isTraceEnabled())
						log.trace("This is a trigger for another group: " + alarm.getIdentifier());
					break;
				}
			}

			if(!isTriggerForOtherGroup) {
				Util.WhereToSendAndSend(a); 

				// decompose the ciena alarms
			}
		}	
	} 


	private void AlarmIsNotTrigger(Pri_Sec_Alarm trigger, Pri_Sec_Alarm a) {

		if(a.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50002/100/21") ||
				a.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50003/100/1")) {

			String info1 = a.getCustomFieldValue(GFPFields.INFO1); 
			//String aafdaRole = "AAFDARole=<" + trigger.getRemotePportAafdaRole() + "> ";
			//String diverseCktId = "DiverseCircuitId=<" + trigger.getRemotePportDiverseCktId() + ">";
			//String pairClfi = "PairCLFI=<" + trigger.getRemotePportDiverseCktId() + ">";
			info1 = info1 + " AAFDARole=<" + a.getAafDaRole() + "> " +
					" DiverseCircuitID=<" + a.getDiverseCircuitID() + "> "; 
			Pattern pattern = Pattern.compile("RelatedCLLI=<\\w*\\S*>");
			Matcher matcher = pattern.matcher(info1);  
			if(matcher.find()) {
				info1 = info1.replace(matcher.group(), "RelatedCLLI=<" + trigger.getDeviceName() + ">");
			} else {
				info1 = info1 + "RelatedCLLI=<" + trigger.getDeviceName() + ">";
			}
			Pattern pattern2 = Pattern.compile("RelatedPortAID=<\\w*\\S*>");
			Matcher matcher2 = pattern2.matcher(info1); 
			if(matcher2.find()) {
				info1 = info1.replace(matcher2.group(), "RelatedPortAID=<" + trigger.getPortAid() + ">");
			} else {
				info1 = info1 + "RelatedPortAID=<" + trigger.getPortAid() + ">";
			}
			a.setCustomFieldValue(GFPFields.INFO1, info1); 
			//			a.setCustomFieldValue(GFPFields.INFO1, info1 +
			//					" AAFDARole=<" + a.getRemotePportAafdaRole() + "> " +
			//					" DiverseCircutId=<" + a.getRemotePportDiverseCktId() + "> " +
			//					" RelatedCLLI=<" + a.getRemoteDeviceName() + "> " +  
			//					" RelatedPortAID=<" + a.getRemotePortAid() + ">"); 
			String triggerInfo1 = trigger.getCustomFieldValue(GFPFields.INFO1);
			Pattern pattern3 = Pattern.compile("RelatedCLLI=<\\w*\\S*>");
			Matcher matcher3 = pattern3.matcher(triggerInfo1);    
			if(matcher3.find()) {
				triggerInfo1 = triggerInfo1.replace(matcher3.group(), "RelatedCLLI=<" + a.getRemoteDeviceName() + ">");
			} else {
				triggerInfo1 = triggerInfo1 + "RelatedCLLI=<" + a.getRemoteDeviceName() + ">";
			}
			Pattern pattern4 = Pattern.compile("RelatedPortAID=<\\w*\\S*>");
			Matcher matcher4 = pattern4.matcher(triggerInfo1); 
			if(matcher4.find()) {
				triggerInfo1 = triggerInfo1.replace(matcher4.group(), "RelatedPortAID=<" + a.getRemotePortAid() + ">");
			} else { 
				triggerInfo1 = triggerInfo1 + "RelatedPortAID=<" + a.getRemotePortAid() + ">";
			} 
			trigger.setCustomFieldValue(GFPFields.INFO1, triggerInfo1);  
			//			if(trigger.getCustomFieldValue(GFPFields.CLASSIFICATION).contains("CFO")) {
			//				a.setCustomFieldValue(GFPFields.INFO1, aafdaRole + " " + diverseCktId + " "+ info1);
			//			} else if(trigger.getCustomFieldValue(GFPFields.CLASSIFICATION).contains("NFO")) {
			//				a.setCustomFieldValue(GFPFields.INFO1, pairClfi + " " + info1);
			//			}
			trigger.setPerceivedSeverity(PerceivedSeverity.CRITICAL);	  	 

		}
	}	


	//Sample alarm data populated in info2 info1 for RUBY-Telco:
	//	AAFDARole=<value> DiverseCircutID=<value> RelatedCLLI=<value> RelatedPortAID=<value>
	//	Notes:
	//	RelatedCLLI=<value> and RelatedPortAID=<value> will be populated if related link has any alarm listed above 

	private void AlarmIsTrigger(Pri_Sec_Alarm trigger) {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "AlarmIsTrigger()");
		}

		String classification = trigger.getCustomFieldValue(GFPFields.CLASSIFICATION);
		if (log.isTraceEnabled())
			log.trace("The classification is " + classification);

		if(classification != null) {
			//if the classification contains NFO (is_Telco)
			if(classification.contains("NFO")) {

				// update the CLFI field with the remote port's CLFI
				trigger.setCustomFieldValue(GFPFields.CLFI, trigger.getRemotePportClfi());

				String info1 = trigger.getCustomFieldValue(GFPFields.INFO1); 
				trigger.setCustomFieldValue(GFPFields.INFO1, info1 +
						" AAFDARole=<" + trigger.getRemotePportAafdaRole() + "> " +
						" DiverseCircuitID=<" + trigger.getRemotePportDiverseCktId() + "> " +
						" RelatedCLLI=<" + trigger.getRemoteDeviceName() + "> " +
						" RelatedPortAID=<" + trigger.getRemotePortAid() + ">");
			}

			// update the component with the remote port CLFI if the classification
			// contains CFO (is_Rrc)
			if(classification.contains("CFO")) {

				String newComponent = null;

				newComponent = "CLFI=<" + trigger.getRemotePportClfi() + "> " + 
						trigger.getCustomFieldValue(GFPFields.COMPONENT);

				trigger.setCustomFieldValue(GFPFields.COMPONENT, newComponent);	

				//Include DiverseCircutID in the beginning of reason field as PairCLFI=<DiverseCircutID  value>
				trigger.setCustomFieldValue(GFPFields.REASON, "PairCLFI=<" + trigger.getRemotePportDiverseCktId() + "> " + 
						trigger.getCustomFieldValue(GFPFields.REASON) +
						" RelatedPortAID=<" + trigger.getRemotePortAid() +
						"> deviceType=<" + trigger.getDeviceType() +
						"> DeviceModel=<" + trigger.getDeviceModel() +
						"> PortAID=<" + trigger.getPortAid() +
						"> Port=<" + trigger.getOriginatingManagedEntity().split(" ")[1] +
						"> Slot=<" +
						"> Card=<" +
						"> AAFDARole=<" + trigger.getRemotePportAafdaRole() + ">");				
			}

			//			trigger.setPerceivedSeverity(PerceivedSeverity.CRITICAL);		
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "AlarmIsTrigger()");
		}
	}


}
