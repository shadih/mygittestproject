package com.att.gfp.data.ipagAlarm;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.config.AdtranCorrelationConfiguration;
import com.att.gfp.data.config.DecomposeRulesConfiguration;
import com.att.gfp.data.config.DecompseConfig;
import com.att.gfp.data.ipag.decomposition.Decomposer;
import com.att.gfp.data.ipag.topoModel.IpagAdtranTopoAccess;
import com.att.gfp.data.ipag.topoModel.Reason;
import com.att.gfp.data.ipag.topoModel.ReasonCode;
import com.att.gfp.helper.AdtranPportAlarmProcessor;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil; 
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm; 
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.scenario.ScenarioThreadLocal;
import com.hp.uca.expert.vp.pd.services.PD_Service_Group;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;

/**
 * Extends EnrichedAlarm
 * Base alarm class for all the Adtran use cases processing
 * 
 * @author st133d
 *
 */
public class EnrichedAdtranAlarm extends EnrichedAlarm {

	private static final long serialVersionUID = -5734000362965235667L;
	private static Logger log = LoggerFactory.getLogger(EnrichedAdtranAlarm.class);

	private Boolean sentAsSubAlarm;
	private Boolean sentAsTriggerAlarm;
	private Boolean triggerWatchSet;
	private Boolean candidateAlarmWatchSet;
	public Boolean getCandidateAlarmWatchSet() {
		return candidateAlarmWatchSet;
	}


	public void setCandidateAlarmWatchSet(Boolean candidateAlarmWatchSet) {
		this.candidateAlarmWatchSet = candidateAlarmWatchSet;
	}


	private int numberOfWatches;
	private int numberOfExpiredWatches;
	private ReasonCode reasonCodeObj;
	private Reason reasonObj;
	private Boolean sentToNOM;
	private Boolean decomposed;
	private List<EnrichedAlarm> decomposedAlarms;


	public List<EnrichedAlarm> getDecomposedAlarms() {
		return decomposedAlarms;
	}


	public void setDecomposedAlarms(List<EnrichedAlarm> decomposedAlarms) {
		this.decomposedAlarms = decomposedAlarms;
	}


	public Boolean getDecomposed() {
		return decomposed;
	}


	public void setDecomposed(Boolean decomposed) {
		this.decomposed = decomposed;
	}


	public Boolean getSentToNOM() {
		return sentToNOM; 
	}


	public void setSentToNOM(Boolean sentToNOM) {
		this.sentToNOM = sentToNOM;
	}


	public Reason getReasonObj() {
		return reasonObj;
	}


	public void setReasonObj(Reason reason) {
		this.reasonObj = reason;
	}


	public ReasonCode getReasonCodeObj() {
		return reasonCodeObj;
	}


	public void setReasonCodeObj(ReasonCode reasonCodeObj) {
		this.reasonCodeObj = reasonCodeObj;
	}


	public Boolean getTriggerWatchSet() {
		return triggerWatchSet;
	}


	public void setTriggerWatchSet(Boolean triggerWatchSet) {
		this.triggerWatchSet = triggerWatchSet;
	}


	/**
	 * EnrichedAlarm Constructor
	 */
	public EnrichedAdtranAlarm() {
		super();
	}


	/**
	 * EnrichedAlarm Copy Constructor.
	 * 
	 * This constructor constructs an Juniper Alarm and uses the Alarm provided as
	 * parameter to set all the standard alarm fields
	 * @throws Exception 
	 */
	public EnrichedAdtranAlarm(EnrichedAlarm alarm) {
		super(alarm);
		sentAsSubAlarm = false;
		sentAsTriggerAlarm = false;
		triggerWatchSet = false;
		numberOfWatches = 0;
		numberOfExpiredWatches =0;
		reasonCodeObj = null;
		reasonObj = null;
		sentToNOM = false;	
		decomposed = false;
		decomposedAlarms = null;
		candidateAlarmWatchSet = false;
	}

	public EnrichedAdtranAlarm(Alarm alarm) {
		super(alarm);
		sentAsSubAlarm = false;
		sentAsTriggerAlarm = false;
		triggerWatchSet = false;
		numberOfWatches = 0;
		numberOfExpiredWatches =0;
		reasonCodeObj = null;
		reasonObj = null;
		sentToNOM = false;	
		decomposed = false;
		decomposedAlarms = null;
		candidateAlarmWatchSet = false;
	}

	/**
	 * Clones the provided Extended Alarm object
	 * @return a copy of the current object
	 * @throws CloneNotSupportedException
	 */
	@Override
	public EnrichedAlarm clone() throws CloneNotSupportedException {
		EnrichedAdtranAlarm newAlarm = (EnrichedAdtranAlarm) super.clone();
		newAlarm.sentAsSubAlarm = this.sentAsSubAlarm;		
		newAlarm.sentAsTriggerAlarm = this.sentAsTriggerAlarm;
		newAlarm.triggerWatchSet = this.triggerWatchSet;
		newAlarm.numberOfWatches = this.numberOfWatches;
		newAlarm.numberOfExpiredWatches = this.numberOfExpiredWatches;
		newAlarm.reasonCodeObj = this.reasonCodeObj;
		newAlarm.reasonObj = this.reasonObj;
		newAlarm.sentToNOM = this.sentToNOM;
		newAlarm.decomposed = this.decomposed; 
		newAlarm.decomposedAlarms = this.decomposedAlarms;
		newAlarm.candidateAlarmWatchSet = this.candidateAlarmWatchSet;
		return newAlarm;
	}



	/**
	 * 
	 */
	public void expirationCallBack() {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "expirationCallBack()", getIdentifier());
		}
		boolean noSend = false;
		Scenario scenario = ScenarioThreadLocal.getScenario();

		// if we already sent the trigger, then we are done here
		if(!getSentAsTriggerAlarm() && !getSentAsSubAlarm() && !isSuppressed() && !getSentToNOM()) {  
			// all of this code is here just to make sure...   
			// probably not needed unless something weird happens    
			// unless the time windows are different then we need this
			// code  

			// increment the number of watch expirations
			setNumberOfExpiredWatches(getNumberOfExpiredWatches() + 1);

			// get the groups where this alarm is present
			Collection<Group> groups = PD_Service_Group.getGroupsOfAnAlarm(scenario, this);

			// if there is only one group that has this alarm 
			if(groups.size() == 1) {
				// there is only one group so this is the last watch to expire
				setNumberOfExpiredWatches(0);
				setNumberOfWatches(0);

				// is there more than one alarm in this group
				for (Group group : groups) {
					if(group.getNumber() > 1 && group.getTrigger() == this) {
						// don't send out the alarm here cuz there are more alarms in the group
						// this code should never execute because the time window should have 
						// expired and the alarm would have already been sent by createProblemAlarm
						// in the actions factory
						noSend = true;
					}
				}						
			} else {
				// there are more than one group with this alarm
				// we have to go thru the groups to see what is up

				// if this is the last watch to expire
				if(getNumberOfExpiredWatches() == getNumberOfWatches()) {
					setNumberOfExpiredWatches(0);
					setNumberOfWatches(0);

					// if there are any groups with more than one alarm in it 
					// don't send out the trigger here
					for (Group group : groups) {
						if(group.getNumber() > 1 && group.getTrigger() == this)
							noSend = true;
					}				
				} else {
					// this is not the last watch to expire so don't send it out
					// yet
					noSend = true;
				}
			}

			if (noSend == false && isSuppressed() == false) {
				// cascade alarm
				log.trace("EnrichedAdtranAlarm:expirationCallBack: met the sent critiria");
				DecomposeRulesConfiguration decomposeConfig = new DecomposeRulesConfiguration();
				List<DecompseConfig.DecomposeRules.DecomposeRule> decomposeRules = decomposeConfig.getDecompseConfig().getDecomposeRules().getDecomposeRule();
				if(decomposeRules != null && decomposeRules.size() > 0) {	
					for(DecompseConfig.DecomposeRules.DecomposeRule decomposeRule : decomposeRules) {   
						log.trace("EnrichedAdtranAlarm:expirationCallBack: Found decompose Rules");
						log.trace("EnrichedAdtranAlarm:expirationCallBack: Type = " + decomposeRule.getType());   
						log.trace("EnrichedAdtranAlarm:expirationCallBack: Order = " + decomposeRule.getOrder()); 
						if(decomposeRule.getEventNames().getEventName().contains(getCustomFieldValue(GFPFields.EVENT_KEY))) { 
							if("whileSendingTheEvent".equalsIgnoreCase(decomposeRule.getOrder())) {
								GFPUtil.forwardAlarmToDecomposerInstance(this, "ADTRAN_DECOMPOSER");								
								//								List<EnrichedAlarm> decomposedAlarms = null;
								//								try {
								//									decomposedAlarms = Decomposer.decompose(this);
								//								} catch (Exception e) {
								//									e.printStackTrace();               
								//								}
								//								if(decomposedAlarms != null && decomposedAlarms.size() > 0) {
								//									for(EnrichedAlarm enrichedAlrm : decomposedAlarms) {
								//										if(enrichedAlrm.getPerceivedSeverity() != PerceivedSeverity.CLEAR) {
								//											enrichedAlrm.setPerceivedSeverity(PerceivedSeverity.CRITICAL);       
								//										}
								//										log.trace("EnrichedAdtranAlarm:expirationCallBack: sending decomposed alarm : " + enrichedAlrm.getIdentifier()); 
								//										GFPUtil.forwardOrCascadeAlarm(enrichedAlrm, AlarmDelegationType.FORWARD, null);   
								//									}     
								//								}    
							} 
						}   
					}  
				}
				AdtranCorrelationConfiguration adtCorrelationConfig = new AdtranCorrelationConfiguration();
				if(adtCorrelationConfig.getAdtranCorrelationPolicies().getPportProcessing().getEventsWithCorrelation().getEventNames().getEventName().contains(getCustomFieldValue(GFPFields.EVENT_KEY))) {
					if(adtCorrelationConfig.getAdtranCorrelationPolicies().getVrfUpdate().getEventNames().getEventName().contains(getCustomFieldValue(GFPFields.EVENT_KEY))) {
						log.trace("EnrichedAdtranAlarm:expirationCallBack: executing the shdsl alarm logic");
						String localDeviceType = this.getDeviceType();
						String remoteDeviceType = this.getRemoteDeviceType();
						IpagAdtranTopoAccess topoAccess = IpagAdtranTopoAccess.getInstance();
						if("ADTRAN 800 SERIES".equalsIgnoreCase(localDeviceType)) {  
							if(this.getPerceivedSeverity() == PerceivedSeverity.CLEAR) {
								topoAccess.updateAdtLinkDownStatusByPport(GFPUtil.getManagedInstanceFromMangaedEntity(this.getOriginatingManagedEntity()), "false", false);
							} else {
								topoAccess.updateAdtLinkDownStatusByPport(GFPUtil.getManagedInstanceFromMangaedEntity(this.getOriginatingManagedEntity()), "true", true);
							}
						} 
						if("ADTRAN 5000 SERIES".equalsIgnoreCase(localDeviceType)) {
							if(!("JUNIPER MX SERIES".equalsIgnoreCase(remoteDeviceType))) {
								if(this.getRemotePportInstanceName() != null) {
									if(this.getPerceivedSeverity() == PerceivedSeverity.CLEAR) {
										topoAccess.updateAdtLinkDownStatusByPport(this.getRemotePportInstanceName(), "false", false);
									} else {
										topoAccess.updateAdtLinkDownStatusByPport(this.getRemotePportInstanceName(), "true", true);
									}
								}
							}
						}
					}
					AdtranPportAlarmProcessor pportProcessor = new AdtranPportAlarmProcessor();
					EnrichedAdtranLinkDownAlarm enrichedAdtranLinkDownAlarm = null;
					enrichedAdtranLinkDownAlarm = new EnrichedAdtranLinkDownAlarm(this);
					log.trace("EnrichedAdtranAlarm:expirationCallBack: calling the pport processor after correlation");
					pportProcessor.processAdtranPportAlarm(enrichedAdtranLinkDownAlarm);   

				}  else {
					log.trace("EnrichedAlarm:expirationCallBack: sending the EnrichedAlarm"); 
					GFPUtil.forwardOrCascadeAlarm(this, AlarmDelegationType.FORWARD, null);
					setSentAsTriggerAlarm(true);
				}
			}
		} else {
			if (log.isTraceEnabled()) {
				LogHelper.method(log, "expirationCallBack()", " We already sent the trigger, we are done.");
			}
			setNumberOfExpiredWatches(0);
			setNumberOfWatches(0);
		}
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "expirationCallBack()");
		}
	}

	/**
	 * 
	 */
	public void candidateAlarmexpirationCallBack() {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "candidateAlarmexpirationCallBack()", getIdentifier());
		}

		// if we already sent the trigger, then we are done here
		if(!getSentAsTriggerAlarm() && !getSentAsSubAlarm() && !isAboutToBeRetracted() && !isSuppressed() && !getSentToNOM()) {  
			if( !GFPUtil.getAdtranJuniperTraps().contains(getCustomFieldValue(GFPFields.EVENT_KEY)) && 
					!getCustomFieldValue(GFPFields.EVENT_KEY).equals("50001/100/7") ) {
				GFPUtil.forwardOrCascadeAlarm(this, AlarmDelegationType.FORWARD, null);
				setSentToNOM(true);
			}   
		} 
		if (log.isTraceEnabled()) {   
			LogHelper.exit(log, "candidateAlarmexpirationCallBack()");
		}
	}


	public Boolean getSentAsSubAlarm() {
		return sentAsSubAlarm;
	}


	public void setSentAsSubAlarm(Boolean sentAsSubAlarm) {
		this.sentAsSubAlarm = sentAsSubAlarm;
	}


	public Boolean getSentAsTriggerAlarm() {
		return sentAsTriggerAlarm;  
	}


	public void setSentAsTriggerAlarm(Boolean sentAsTriggerAlarm) {
		this.sentAsTriggerAlarm = sentAsTriggerAlarm;
	}




	public int getNumberOfWatches() {
		return numberOfWatches;
	}


	public void setNumberOfWatches(int numberOfWatches) {
		this.numberOfWatches = numberOfWatches;
	}


	public int getNumberOfExpiredWatches() {
		return numberOfExpiredWatches;
	}


	public void setNumberOfExpiredWatches(int numberOfExpiredWatches) {
		this.numberOfExpiredWatches = numberOfExpiredWatches;
	}




}
