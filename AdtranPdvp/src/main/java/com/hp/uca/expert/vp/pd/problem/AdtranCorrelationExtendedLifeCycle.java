package com.hp.uca.expert.vp.pd.problem;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Arrays;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.config.AdtranCorrelationConfiguration;
import com.att.gfp.data.config.DecomposeRulesConfiguration;
import com.att.gfp.data.config.DecompseConfig;
import com.att.gfp.data.config.IpagXmlConfiguration;
import com.att.gfp.data.ipag.decomposition.Decomposer;
import com.att.gfp.data.ipag.topoModel.IpagAdtranTopoAccess;
import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.data.ipagAlarm.EnrichedAdtranAlarm;
import com.att.gfp.data.ipagAlarm.EnrichedAdtranLinkDownAlarm;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.helper.AdtranPportAlarmProcessor;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
import com.att.gfp.helper.StandardFields;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.alarm.AlarmCommon;
import com.hp.uca.expert.alarm.AlarmUpdater;
import com.hp.uca.expert.alarm.AlarmUpdater.UsualVar;
import com.hp.uca.expert.engine.Bootstrap;
import com.hp.uca.expert.lifecycle.LifeCycleAnalysis;
import com.hp.uca.expert.localvariable.LocalVariable;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.vp.pd.services.PD_Service_Navigation;
import com.hp.uca.expert.x733alarm.AttributeChange;
import com.hp.uca.expert.x733alarm.NetworkState;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;



public class AdtranCorrelationExtendedLifeCycle extends LifeCycleAnalysis {

	private static Logger log = LoggerFactory.getLogger(AdtranCorrelationExtendedLifeCycle.class);
	
	public AdtranCorrelationExtendedLifeCycle(Scenario scenario) {
		super(scenario);

		/*
		 * If needed more configuration, use the context.xml to define any beans
		 * that will be available here using scenario.getGlobals() 
		 */
		scenario.getGlobals();
	}

	
	@Override
	public AlarmCommon onAlarmCreationProcess(Alarm alarm) {

		  
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "onAlarmCreationProcess()");
		}
		if(alarm.getPerceivedSeverity() == PerceivedSeverity.CLEAR) {
			log.trace("onAlarmCreationProcess: Dropping the alarm as the severity is CLEAR " + alarm.getIdentifier() + "|{" + alarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}");
			return null;  
		}  
		
		EnrichedAlarm enrichedAlrm = null;
		EnrichedAdtranAlarm enrichedAlarm = null;
		try {       
			//set the following fields for decomposable alarms        
			log.trace("onAlarmCreationProcess : alarm class name " + alarm.getClass().getName());
			
			if(alarm instanceof com.att.gfp.data.ipagAlarm.EnrichedAlarm) {
				log.trace("onAlarmCreationProcess : EnrichedAlarm object received");
				enrichedAlrm = (EnrichedAlarm) alarm;
				log.trace("enrichedAlarm toString() result : " + enrichedAlrm.toString());  
				log.trace("device type = " + enrichedAlrm.getDeviceType());        
				log.trace("device model = " + enrichedAlrm.getDeviceModel());   
				enrichedAlarm = new EnrichedAdtranAlarm(enrichedAlrm);     
				log.trace("device type = " + enrichedAlarm.getDeviceType());        
				log.trace("device name = " + enrichedAlarm.getDeviceName());  
			} else {  
				log.trace("onAlarmCreationProcess : Alarm object received");
				enrichedAlrm = new EnrichedAlarm(alarm); 
				enrichedAlrm.setNodeType("CE"); 
				enrichedAlrm.setDeviceType("ADTRAN 5000 SERIES"); 
				enrichedAlarm = new EnrichedAdtranAlarm(enrichedAlrm); 
				enrichedAlarm = new EnrichedAdtranAlarm(enrichedAlrm);    
//				setAlarmAttributesForUNITTest(enrichedAlarm);  
			}  
            String axml = enrichedAlarm.toXMLString();
            axml = axml.replaceAll("\\n", " ");
            log.info("Incoming alarm: "+axml);
            log.info("Enrichment: "+(enrichedAlarm.toString()));  
			IpagXmlConfiguration ipagXmlConfig = new IpagXmlConfiguration();
			if(ipagXmlConfig.getIpagPolicies().getHealthTraps().getEventNames().getEventName().contains(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
				log.trace("AdtranPdvp is a health check alarm ");
				GFPUtil.forwardOrCascadeAlarm(enrichedAlarm, AlarmDelegationType.FORWARD, null);  
				enrichedAlarm.setSentToNOM(true);  
				log.trace("AdtranPdvp retracting health check alarm ");
				return null;              
			}      
			enrichedAlarm.setReasonObj(GFPUtil.getReasonObj(enrichedAlarm.getCustomFieldValue(GFPFields.REASON)));  
			AdtranCorrelationConfiguration adtCorrelationConfig = new AdtranCorrelationConfiguration();
			if(adtCorrelationConfig.getAdtranCorrelationPolicies().getEnrichLportAlarm().getEventNames().getEventName().contains(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
				enrichLportAlarm(enrichedAlarm);
			}
			if(adtCorrelationConfig.getAdtranCorrelationPolicies().getAdtranCfmAlarm().getEventNames().getEventName().contains(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
				enrichedAlarm.setReasonCodeObj(GFPUtil.parseReasonCode(enrichedAlarm));
				log.trace("Cascading to a PD vp so changing NeedNavigationUpdate field");   
				enrichedAlarm.setJustInserted(true);
				enrichedAlarm.setAboutToBeRetracted(false);
				enrichedAlarm.getVar().remove(PD_Service_Navigation.NEED_NAVIGATION_UPDATE);   
				GFPUtil.forwardOrCascadeAlarm(enrichedAlarm, AlarmDelegationType.CASCADE, "JUNIPER_SYSLOG"); 
				enrichedAlarm.setSentToNOM(true);
			}
			DecomposeRulesConfiguration decomposeConfig = new DecomposeRulesConfiguration();
			if(adtCorrelationConfig.getAdtranCorrelationPolicies().getLportDomainClassification().getNTEDomainKeys().getEventNames().getEventName().contains(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
				enrichedAlarm.setCustomFieldValue(GFPFields.CLASSIFICATION, "NFO-MOBILITY");
				enrichedAlarm.setCustomFieldValue(GFPFields.DOMAIN, "NTE");
			} else if(adtCorrelationConfig.getAdtranCorrelationPolicies().getLportDomainClassification().getIPAGDomainAlarms().getEventNames().getEventName().contains(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
				enrichedAlarm.setCustomFieldValue(GFPFields.CLASSIFICATION, "PIVOT-CFO");
				enrichedAlarm.setCustomFieldValue(GFPFields.DOMAIN, "IPAG");
				//TODO: remove the hardcoded value
			} else if("50001/100/82".equals(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
				enrichedAlarm.setCustomFieldValue(GFPFields.CLASSIFICATION, "PIVOT-CFO");
				enrichedAlarm.setCustomFieldValue(GFPFields.DOMAIN, "IPAG");
			}
			List<DecompseConfig.DecomposeRules.DecomposeRule> decomposeRules = decomposeConfig.getDecompseConfig().getDecomposeRules().getDecomposeRule();
			if(decomposeRules != null && decomposeRules.size() > 0) {
				for(DecompseConfig.DecomposeRules.DecomposeRule decomposeRule : decomposeRules) {   
					log.trace("onAlarmCreationProcess: Found decompose Rules");
					log.trace("onAlarmCreationProcess Type = " + decomposeRule.getType());   
					if(decomposeRule.getEventNames().getEventName().contains(alarm.getCustomFieldValue(GFPFields.EVENT_KEY)) && "whenTheEventArrived".equalsIgnoreCase(decomposeRule.getOrder())) {
						GFPUtil.forwardAlarmToDecomposerInstance(enrichedAlarm, "ADTRAN_DECOMPOSER");						
//						List<EnrichedAlarm> decomposedAlarms = Decomposer.decompose(enrichedAlarm);
//						if(decomposedAlarms != null && decomposedAlarms.size() > 0) {
//							for(EnrichedAlarm decompseAlarm : decomposedAlarms) {  
//								EnrichedAdtranAlarm enrichedDecomposeAlarm = new EnrichedAdtranAlarm(decompseAlarm);
//								log.trace("onAlarmCreationProcess: sending decompsed alarm : " + enrichedDecomposeAlarm.getIdentifier());
//								if(enrichedDecomposeAlarm.getPerceivedSeverity() != PerceivedSeverity.CLEAR) {
//									enrichedDecomposeAlarm.setPerceivedSeverity(PerceivedSeverity.CRITICAL);   
//								}
//								GFPUtil.forwardOrCascadeAlarm(enrichedDecomposeAlarm, AlarmDelegationType.FORWARD, null);
//								enrichedDecomposeAlarm.setSentToNOM(true);   
//								enrichedAlarm.setDecomposed(true); 
//							}     
//							log.trace("onAlarmCreationProcess: setting deomposed alarms List in EnrichedAdtranAlarm object" + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + " size = " + decomposedAlarms.size()); 
//							enrichedAlarm.setDecomposedAlarms(decomposedAlarms);   
//						}  
//						executeAdtranDeviceAlarm(enrichedAlarm);  
					}               
				} 
			}
			if(adtCorrelationConfig.getAdtranCorrelationPolicies().getLportDomainClassification().getNTEDomainKeys().getEventNames().getEventName().contains(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
				executeAdtranLportAlarm(enrichedAlarm);
			} 
			if(isSuppressedByRequiredCriteria(enrichedAlarm)) {
				log.trace("onAlarmCreationProcess: setting suppreseed to true");
				enrichedAlarm.setSuppressed(true);     
			}  
			if(adtCorrelationConfig.getAdtranCorrelationPolicies().getPportProcessing().getEventsWithNoCorrelation().getEventNames().getEventName().contains(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {  
				log.trace("onAlarmCreationProcess one of the pport processing evenr keys ");     
				AdtranPportAlarmProcessor pportProcessor = new AdtranPportAlarmProcessor();
				EnrichedAdtranLinkDownAlarm enrichedAdtLinkDownAlarm = new EnrichedAdtranLinkDownAlarm(enrichedAlrm);
				pportProcessor.processAdtranPportAlarm(enrichedAdtLinkDownAlarm);     
			}     
   
		} catch (Exception e) {
			log.trace("onAlarmCreationProcess: ERROR: "+ e.toString() + " Trace = " + Arrays.toString(e.getStackTrace()));
			//e.printStackTrace();  
		}
		
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "onAlarmCreationProcess()");
		}
		
		return enrichedAlarm;  
	}

	private void enrichLportAlarm(EnrichedAdtranAlarm enrichedAlarm) {
		String flagsTxt = "";
		if("CHANDS3_3".equalsIgnoreCase(enrichedAlarm.getCardType())) {
			flagsTxt = "FLAGS={PIVOT=<Y>}";
		} else {
			flagsTxt = "FLAGS={PIVOT=<N>}"; 
		}
		enrichedAlarm.setCustomFieldValue(GFPFields.REASON,enrichedAlarm.getCustomFieldValue(GFPFields.REASON) + "<" + enrichedAlarm.getEvcName() + ">" + flagsTxt);
		enrichedAlarm.setCustomFieldValue(GFPFields.CLCI, enrichedAlarm.getDs1CktId());
		enrichedAlarm.setCustomFieldValue(GFPFields.CIRCUIT_ID, enrichedAlarm.getDs1CktId());
		enrichedAlarm.setCustomFieldValue(GFPFields.EVC_NAME, enrichedAlarm.getEvcName());
	}


	private void executeAdtranLportAlarm(EnrichedAdtranAlarm enrichedAlarm) {
		if((enrichedAlarm.getCustomFieldValue(GFPFields.CLFI) == null) || enrichedAlarm.getCustomFieldValue(GFPFields.CLFI).isEmpty()) {
		enrichedAlarm
		.setCustomFieldValue(
				GFPFields.CLFI,   
				StringUtils
						.defaultString(getClfiFromTology(GFPUtil
								.getManagedInstanceFromMangaedEntity(enrichedAlarm
										.getOriginatingManagedEntity()))));
		}
		if(enrichedAlarm.getCustomFieldValue(GFPFields.CLFI) == null || enrichedAlarm.getCustomFieldValue(GFPFields.CLFI).isEmpty()) {
			enrichedAlarm.setCustomFieldValue(GFPFields.CLFI, "CLFI-UNKNOWN");
		}  
		GFPUtil.forwardOrCascadeAlarm(enrichedAlarm, AlarmDelegationType.FORWARD, null); 
	}

	private String getClfiFromTology(String lportInstance) {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "getClfiFromTology()");
		}
		String clfi = null;
		IpagAdtranTopoAccess topoAccess = IpagAdtranTopoAccess.getInstance();
		clfi = topoAccess.fetchClfiOfPportFromLportInstance(lportInstance);
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "getClfiFromTology()");
		}
		return clfi;
	}

	private void setAlarmAttributesForUNITTest(EnrichedAdtranAlarm enrichedAlarm) {
		enrichedAlarm.setAlarmTargetExists(true); 
		if("50001/100/1".equalsIgnoreCase(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
			log.trace("setting fields for JUNIT test DECOMPOSE"); 
		enrichedAlarm.setTunable(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY));    
		enrichedAlarm.setDeviceName(enrichedAlarm.getCustomFieldValue("DeviceName"));
		enrichedAlarm.setDeviceType(enrichedAlarm.getCustomFieldValue("DeviceType"));   
		enrichedAlarm.setDeviceModel(enrichedAlarm.getCustomFieldValue("DeviceModel"));  
		enrichedAlarm.setNodeType("CE");    
			enrichedAlarm.setDeviceLevelExists(true);    
		enrichedAlarm.setRemoteDeviceName(enrichedAlarm.getCustomFieldValue("RemoteDeviceName"));
		enrichedAlarm.setRemoteDeviceModel(enrichedAlarm.getCustomFieldValue("RemoteDeviceModel"));
		enrichedAlarm.setRemoteDeviceType(enrichedAlarm.getCustomFieldValue("RemoteDeviceType"));
		enrichedAlarm.setRemotePportInstanceName(
			enrichedAlarm.getCustomFieldValue("RemotePportInstanceName"));
		}
		if(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50001/100/3") ||
				enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50001/100/1")	) {
			// Get all information from topology that is needed    
			log.trace("onAlarmCreationProcess: setAlarmAttributesForUNITTest event key is 50001/100/3 so fecting the remote port info");  
			enrichedAlarm.setRemotePportInstanceName(enrichedAlarm.getCustomFieldValue("remoteportkey"));     
		} 
		  
	}


	private void executeAdtranDeviceAlarm(EnrichedAlarm enrichedAlarm) {
		if(enrichedAlarm.getAlarmTargetExist()) {
			if("50001/100/39".equals(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY)))
			GFPUtil.forwardOrCascadeAlarm(enrichedAlarm, AlarmDelegationType.FORWARD, null); 
			
		}  
	}


	/**
	 * We do not receive AVC (attribute value chang) & SC (state change) events. So we need to 
	 * simulate the
	 * generation of this information by manually updates the Attributes Changes
	 * of the alarm in the Working Memory.
	 * <p>
	 * <u>List of attributes managed:</u>
	 * <li>perceivedSeverity (impacting the networkState severity is CLEAR)</li>
	 * <li>serverSerial</li>
	 * </p>
	 * <hr>
	 * 
	 * @see com.hp.uca.expert.lifecycle.CommonLifeCycle#onUpdateSpecificFieldsFromAlarm
	 *      (com.hp.uca.expert.alarm.Alarm, com.hp.uca.expert.alarm.AlarmCommon)
	 */
	@Override
	public boolean onUpdateSpecificFieldsFromAlarm(Alarm newAlarm,
			AlarmCommon alarmInWorkingMemory) {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "onUpdateSpecificFieldsFromAlarm()", newAlarm.getIdentifier());
		}  
		log.trace("New Incoming alarm : " + newAlarm.toXMLString());			 
		log.trace("Alarm in WM : " + alarmInWorkingMemory.toXMLString()); 			 
  
		boolean ret = false;
		if (alarmInWorkingMemory instanceof EnrichedAlarm) {
			log.trace("onUpdateSpecificFieldsFromAlarm alarm in WM is instance of EnrichedAlarm");
			EnrichedAlarm alarmInWM = (EnrichedAlarm) alarmInWorkingMemory;
			  
			List<AttributeChange> attributeChangesSC = new ArrayList<AttributeChange>(); 
			List<AttributeChange> attributeChangesAVC = new ArrayList<AttributeChange>();
			List<AttributeChange> attributeChangesNewSC = new ArrayList<AttributeChange>();
			AttributeChange attributeChange = null;
			long feTimeOfNewAlarm = Long.parseLong(newAlarm.getCustomFieldValue(GFPFields.FE_TIME_STAMP));
			long feTimeOfAlarminWM = Long.parseLong(alarmInWM.getCustomFieldValue(GFPFields.FE_TIME_STAMP));  
 
			/*  
			 * Updating the Perceived Severity of the alarm in Working memory
			 * only if the Alarm received is different.
			 */
			if(newAlarm.getPerceivedSeverity() == PerceivedSeverity.CLEAR) {
				log.trace("onUpdateSpecificFieldsFromAlarm new alarm severity is CLEAR");
			    //if((feTimeOfNewAlarm >= feTimeOfAlarminWM)) {
				//log.trace("onUpdateSpecificFieldsFromAlarm fetime timestamp of new alarm is greater than the original clear alarm");
				//if(alarmInWM.getNetworkState() == NetworkState.NOT_CLEARED) {
					//Drop, do not process the clear alarm
					//log.trace("onUpdateSpecificFieldsFromAlarm alarm network state is not CLAEARED");
					if(alarmInWM.isSuppressed()) {  
						log.trace("onUpdateSpecificFieldsFromAlarm alarminWM is suppressed");
						attributeChange = new AttributeChange();
						attributeChange.setName(StandardFields.NETWORK_STATE);
						attributeChange
								.setNewValue(NetworkState.CLEARED.toString());
						attributeChange.setOldValue(newAlarm.getNetworkState()
								.toString());
						attributeChangesNewSC.add(attributeChange);    
						attributeChange = new AttributeChange();
						attributeChange.setName(StandardFields.NETWORK_STATE);
						attributeChange
								.setNewValue(NetworkState.CLEARED.toString());
						attributeChange.setOldValue(alarmInWM.getNetworkState()
								.toString());
						attributeChangesSC.add(attributeChange); 
				}
				 else {
					log.trace("onUpdateSpecificFieldsFromAlarm sending new CLEAR alarm");
					//either cascade CLEAR to downstream or forward to NOM based on the regular flow of the alarm
					AdtranCorrelationConfiguration adtCorrelationConfig = new AdtranCorrelationConfiguration();
					if(adtCorrelationConfig.getAdtranCorrelationPolicies().getAdtranCfmAlarm().getEventNames().getEventName().contains(newAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
						log.trace("Cascading to a PD vp so changing NeedNavigationUpdate field");   
						newAlarm.setJustInserted(true);
						newAlarm.setAboutToBeRetracted(false);
						newAlarm.getVar().remove(PD_Service_Navigation.NEED_NAVIGATION_UPDATE); 
						GFPUtil.forwardOrCascadeAlarm(newAlarm, AlarmDelegationType.CASCADE, "JUNIPER_SYSLOG"); 
					} else if(!("50003/100/12".equalsIgnoreCase(newAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) &&
							!("50003/100/13".equalsIgnoreCase(newAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) &&
							!("50003/100/14".equalsIgnoreCase(newAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) &&
							!("50003/100/15".equalsIgnoreCase(newAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) &&
							!("50003/100/16".equalsIgnoreCase(newAlarm.getCustomFieldValue(GFPFields.EVENT_KEY)))){
					log.trace("Forwarding the CLEAR");   
					EnrichedAdtranAlarm enrichedAdtranClear = new EnrichedAdtranAlarm(newAlarm);
					if(adtCorrelationConfig.getAdtranCorrelationPolicies().getLportDomainClassification().getNTEDomainKeys().getEventNames().getEventName().contains(enrichedAdtranClear.getCustomFieldValue(GFPFields.EVENT_KEY))) {
						enrichedAdtranClear.setCustomFieldValue(GFPFields.CLASSIFICATION, "NFO-MOBILITY");
						enrichedAdtranClear.setCustomFieldValue(GFPFields.DOMAIN, "NTE");
					} else if(adtCorrelationConfig.getAdtranCorrelationPolicies().getLportDomainClassification().getIPAGDomainAlarms().getEventNames().getEventName().contains(enrichedAdtranClear.getCustomFieldValue(GFPFields.EVENT_KEY))) {
						enrichedAdtranClear.setCustomFieldValue(GFPFields.CLASSIFICATION, "PIVOT-CFO");
						enrichedAdtranClear.setCustomFieldValue(GFPFields.DOMAIN, "IPAG");
					} else if("50001/100/82".equals(enrichedAdtranClear.getCustomFieldValue(GFPFields.EVENT_KEY))) {
						enrichedAdtranClear.setCustomFieldValue(GFPFields.CLASSIFICATION, "PIVOT-CFO");
						enrichedAdtranClear.setCustomFieldValue(GFPFields.DOMAIN, "IPAG");  
					}
					 
					if("50001/100/7".equalsIgnoreCase(newAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
						GFPUtil.forwardOrCascadeAlarm(newAlarm, AlarmDelegationType.CASCADE, "NTDTICKET_CORRELATION"); 
					} else {
						GFPUtil.forwardOrCascadeAlarm(enrichedAdtranClear, AlarmDelegationType.FORWARD, null);
					}
 					enrichedAdtranClear.setSentToNOM(true);    
 					DecomposeRulesConfiguration decomposeConfig = new DecomposeRulesConfiguration();
 					List<DecompseConfig.DecomposeRules.DecomposeRule> decomposeRules = decomposeConfig.getDecompseConfig().getDecomposeRules().getDecomposeRule();
 					if(decomposeRules != null && decomposeRules.size() > 0) {
 						for(DecompseConfig.DecomposeRules.DecomposeRule decomposeRule : decomposeRules) {   
 							log.trace("onAlarmCreationProcess Type = " + decomposeRule.getType());   
 							if(decomposeRule.getEventNames().getEventName().contains(enrichedAdtranClear.getCustomFieldValue(GFPFields.EVENT_KEY))) {
 								GFPUtil.forwardAlarmToDecomposerInstance(enrichedAdtranClear, "ADTRAN_DECOMPOSER"); 
 							}               
 						}  
 					} 
					if(alarmInWM instanceof EnrichedAdtranAlarm) {
						EnrichedAdtranAlarm enrichedAdtranalarmInWm = (EnrichedAdtranAlarm) alarmInWM;
						if(enrichedAdtranalarmInWm.getDecomposed()) {
							List<EnrichedAlarm> decmoposedAlarms = enrichedAdtranalarmInWm.getDecomposedAlarms();
							if(decmoposedAlarms != null && decmoposedAlarms.size() > 0) {
								log.trace("Creating Synthetic CLEARS for decomposed alarms size = " + decmoposedAlarms.size());   
								for(EnrichedAlarm decomposedAlarm : decmoposedAlarms) { 
									log.trace("Creating Synthetic CLEARS for decomposed alarms");  
									EnrichedAdtranAlarm enrichedAdtranSytheticClear = new EnrichedAdtranAlarm(decomposedAlarm);
									GregorianCalendar gCalendar = new GregorianCalendar();
							        gCalendar.setTime(new java.util.Date(System.currentTimeMillis()));	 
									XMLGregorianCalendar alarmraisedtime = null; 
									try {
										alarmraisedtime = DatatypeFactory.newInstance().newXMLGregorianCalendar(gCalendar);
									} catch (DatatypeConfigurationException e) {
										log.trace("onUpdateSpecificFieldsFromAlarm: ERROR: "+ e.toString() + " Trace = " + Arrays.toString(e.getStackTrace()));
										//e.printStackTrace();
									} 
									enrichedAdtranSytheticClear.setAlarmRaisedTime(alarmraisedtime);
									enrichedAdtranSytheticClear.setCustomFieldValue(GFPFields.REASON, "CLEAR"); 
									enrichedAdtranSytheticClear.setCustomFieldValue(GFPFields.LAST_CLEAR_TIME, String.valueOf(System.currentTimeMillis()/1000));
									enrichedAdtranSytheticClear.setCustomFieldValue(GFPFields.FE_TIME_STAMP,String.valueOf(System.currentTimeMillis()/1000));
									enrichedAdtranSytheticClear.setPerceivedSeverity(PerceivedSeverity.CLEAR); 
									enrichedAdtranSytheticClear.setCustomFieldValue(GFPFields.SEQNUMBER, enrichedAdtranClear.getCustomFieldValue(GFPFields.SEQNUMBER));
									log.trace("Forwarding sythetic CLEAR for decomposed alarm to NOM"); 
				 					GFPUtil.forwardOrCascadeAlarm(enrichedAdtranSytheticClear, AlarmDelegationType.FORWARD, null);
				 					enrichedAdtranSytheticClear.setSentToNOM(true);     
								}  
							} 
						}     
					}
//					getScenario().getSession().retract(alarmInWM);
//					getScenario().getSession().update(alarmInWM);   
					}
					attributeChange = new AttributeChange();
					attributeChange.setName(StandardFields.NETWORK_STATE); 
					attributeChange
							.setNewValue(NetworkState.CLEARED.toString());  
					attributeChange.setOldValue(alarmInWM.getNetworkState()
							.toString());
					attributeChangesSC.add(attributeChange);   
				//}
				/*
				} else {
					log.trace("onUpdateSpecificFieldsFromAlarm RETRACTING the new CLEAR alarm ");
					attributeChange = new AttributeChange();
					attributeChange.setName(StandardFields.NETWORK_STATE);
					attributeChange
							.setNewValue(NetworkState.CLEARED.toString());
					attributeChange.setOldValue(newAlarm.getNetworkState()
							.toString());    
					attributeChangesNewSC.add(attributeChange);     
				}
				*/
			}
			/*
			else {
				//suppress clear.
				log.trace("onUpdateSpecificFieldsFromAlarm RETRACTING new alarm CLEAR");
				
				attributeChange = new AttributeChange();
				attributeChange.setName(StandardFields.NETWORK_STATE);
				attributeChange
						.setNewValue(NetworkState.CLEARED.toString());
				attributeChange.setOldValue(newAlarm.getNetworkState()
						.toString());    
				attributeChangesNewSC.add(attributeChange);     
			}
			*/  
			}else {
				
				log.trace("onUpdateSpecificFieldsFromAlarm: Received a duplicate... ignoring");
				
				/* old code trying to handle duplicates... replace by code above
				log.trace("onUpdateSpecificFieldsFromAlarm alarm severity is NOT CLEAR");

				if(newAlarm.getCustomFieldValue(GFPFields.FE_TIME_STAMP).equals(alarmInWM.getCustomFieldValue(GFPFields.FE_TIME_STAMP))
					&&	newAlarm.getPerceivedSeverity().equals(alarmInWM.getPerceivedSeverity())) {
					//log this alarm
					//TODO: drop alarm do not process.
					log.trace("onUpdateSpecificFieldsFromAlarm new alarm is a duplicate alarm");
					log.trace("onUpdateSpecificFieldsFromAlarm fetime stamp of new alarm is equals to the original alarm");
					log.trace("onUpdateSpecificFieldsFromAlarm RETRACTING new alarm");
					attributeChange = new AttributeChange(); 
					attributeChange.setName(StandardFields.NETWORK_STATE);
					attributeChange
							.setNewValue(NetworkState.CLEARED.toString());
					attributeChange.setOldValue(newAlarm.getNetworkState()
							.toString());    
					attributeChangesNewSC.add(attributeChange);     
				} else {
					if((feTimeOfNewAlarm > feTimeOfAlarminWM))  {
						log.trace("onUpdateSpecificFieldsFromAlarm fetime stamp of the new alarm is greater than the original alarm");
						log.trace("onUpdateSpecificFieldsFromAlarm Processing the new Duplicate alarm");
						newAlarm.setIdentifier(newAlarm.getIdentifier() + "_DuplicateID"); 
						Bootstrap.getInstance().getDispatcher().enqueueAlarm(newAlarm);     
					}
					
				}
			*/	 
			} 

			if (!attributeChangesSC.isEmpty()) {
				AlarmUpdater.updateAlarmFromAttributesChanges(alarmInWM,
						UsualVar.StateChange, attributeChangesSC,
						System.currentTimeMillis());
				alarmInWM.setHasStateChanged(true);
				ret = true;
			}

			if (!attributeChangesAVC.isEmpty()) {
				AlarmUpdater.updateAlarmFromAttributesChanges(alarmInWM,
						UsualVar.AVCChange, attributeChangesAVC,
						System.currentTimeMillis());
				alarmInWM.setHasAVCChanged(true);
				ret = true; 
			}

			if (!attributeChangesNewSC.isEmpty()) {
				AlarmUpdater.updateAlarmFromAttributesChanges(newAlarm,
						UsualVar.StateChange, attributeChangesNewSC,   
						System.currentTimeMillis());
				newAlarm.setHasStateChanged(true);    
				ret = true; 
			}   
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "onUpdateSpecificFieldsFromAlarm()",  
					String.valueOf(ret));
		}
		return ret;
		// for now just handling the severity
	}
	
	public boolean isSuppressedByRequiredCriteria(EnrichedAlarm enrichedAlarm) {
		if("50001/100/7".equalsIgnoreCase(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
			if(GFPFields.DEVICE_MODEL_NETVANTA838.equalsIgnoreCase(enrichedAlarm.getDeviceModel())) {
				return true;
			}
		}
		return false;
	}
	
	public static void testOnAlarm(Alarm alarm) {
		if(alarm instanceof EnrichedAlarm) {
			
		}
		EnrichedAdtranAlarm enrichedAdt = new EnrichedAdtranAlarm(alarm);
		System.out.println("deive type" + enrichedAdt.getDeviceType());
		System.out.println("remote deive type" + enrichedAdt.getRemoteDeviceType());
	}
	public static void main(String[] args) {
		EnrichedAlarm alarm = new EnrichedAlarm();
		alarm.setDeviceType("deviceType");
		alarm.setRemoteDeviceType("remoteDeviceType");
		AdtranCorrelationExtendedLifeCycle.testOnAlarm(alarm);  
	}
}
