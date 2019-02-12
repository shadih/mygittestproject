package com.att.gfp.data.ipagPreprocess.preprocess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.ArrayIndexOutOfBoundsException;

import org.drools.command.GetSessionClockCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.config.IpagPolicies.SuperEmuxDevices.SuperEmux;
import com.att.gfp.data.config.IpagPolicies.SuperEmuxDevices.SuperEmux.portAidMapping;
import com.att.gfp.data.config.IpagXmlConfiguration;
import com.att.gfp.data.ipag.topoModel.IpagTopoAccess;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.data.ipagPreprocess.externdata.DevTypeAndSeveritySuppressionProperties;
import com.att.gfp.data.ipagPreprocess.externdata.OIDToEname;
import com.att.gfp.data.ipagPreprocess.externdata.PurgeInterval;
import com.att.gfp.data.ipagPreprocess.externdata.PurgeIntervalProperties;
import com.att.gfp.data.ipagPreprocess.externdata.TunableProperties;
import com.att.gfp.data.util.NetcoolFields;
import com.att.gfp.data.util.PreprocessHelper;
import com.att.gfp.data.util.StandardFields;
import com.att.gfp.data.util.SuppressionBean;
import com.att.gfp.data.util.service_util;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.alarm.AlarmCommon;
import com.hp.uca.expert.alarm.AlarmDeletion;
import com.hp.uca.expert.alarm.AlarmUpdater;  
import com.hp.uca.expert.alarm.AlarmUpdater.UsualVar;
import com.hp.uca.expert.engine.Bootstrap;
import com.hp.uca.expert.lifecycle.LifeCycleAnalysis;
import com.hp.uca.expert.localvariable.LocalVariable;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.x733alarm.AlarmDeletionInterface;
import com.hp.uca.expert.x733alarm.AttributeChange;
import com.hp.uca.expert.x733alarm.NetworkState;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;
import com.hp.uca.mediation.action.client.Action;


// Just an example of extension of analyzer
public class ExtendedLifeCycle extends LifeCycleAnalysis {

	private static final String IPAG_PMOS = "IPAG-PMOS";
	private static final String PPORT = "PPORT";
	private static final String LPORT = "LPORT";
	private static final String ETHERNET_NFO = "ETHERNET-NFO";
	private static final String DEVICE = "DEVICE";
	private static final String EVC = "EVCNODE";
	private static final String DEFAULT_EVENT_KEY = "10000/500/1";
	private static final String LINKDOWN_KEY = "50003/100/1";
	protected static final String TUNABLE_BEAN_NAME = "oid2eName";
	protected static final String OID2EN_CONFIG_XML = "OID-KE.xml";
	private static TunableProperties tunableProperties = null;

	protected static final String DEVSEV_BEAN_NAME = "devTypeSeveritySuppression";
	protected static final String PURGEINTERVAL_BEAN_NAME = "purgeInterval";
	//private static final String IPAG_CONFIGURATON_FILE  = "IPAGConfiguration.xml";
	private static DevTypeAndSeveritySuppressionProperties devSevProperties = null;
	private static PurgeIntervalProperties purgeIntervalProperties = null;

	private static Logger log = LoggerFactory.getLogger(ExtendedLifeCycle.class);

	public ExtendedLifeCycle(Scenario scenario) {   
		super(scenario);

		/*
		 * If needed more configuration, use the context.xml to define any beans
		 * that will be available here using scenario.getGlobals()
		 */
		scenario.getGlobals();

	}

	@Override
	public void processIncomingData(AlarmCommon inData)
	{
		if (log.isTraceEnabled()) { 
			LogHelper.enter(log, "processIncomingData()");
		}
		EnrichedPreprocessAlarm enrichedAlarm = null;
		com.att.gfp.data.ipagAlarm.EnrichedAlarm enrichedAlm = null;
		try {
			enrichedAlm = new com.att.gfp.data.ipagAlarm.EnrichedAlarm(
					(Alarm)inData);
			enrichedAlarm = new EnrichedPreprocessAlarm(enrichedAlm);

			if (log.isInfoEnabled()) {
				String axml = enrichedAlarm.toXMLString();
				axml = axml.replaceAll("\\n", " ");

				log.info("Incoming alarm: " + axml);
			}
			enrichedAlarm.setCustomFieldValue("SuppressPport", "false");
			enrichedAlarm.setCustomFieldValue("SuppressDevice", "false");
			enrichedAlarm.setCustomFieldValue("SuppressLport", "false"); 


			if ((enrichedAlarm.getPerceivedSeverity() != PerceivedSeverity.CLEAR)) {
				if (!enrichedAlarm.isSuppressed()) {
					if (!SuppressByDeviceAndSeverity(enrichedAlarm)) {  
						extractOIDAndTunableLookup(enrichedAlarm); 
					}
				}
			}
			IpagXmlConfiguration ipagXmlConfig = (IpagXmlConfiguration) service_util.retrieveBeanFromContextXml(getScenario(), "ipagXmlConfig");
			if(ipagXmlConfig.getIpagPolicies().getHealthTraps().getEventNames().getEventName().contains(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
				if (log.isTraceEnabled()) {
					log.trace("is a health trap");  
				}
				enrichedAlarm.setProbableCause(enrichedAlarm.getProbableCause() + ";CDM_IN=" + GFPUtil.getCurrentTimeInSecond() + ";"); 
				service_util.sendAlarm(getScenario(), enrichedAlarm, enrichedAlarm.getCustomFieldValue(NetcoolFields.EVENT_KEY)); 
				return;
			}

			if ((enrichedAlarm.getPerceivedSeverity() != PerceivedSeverity.CLEAR)) {
				if(!("ALL".equalsIgnoreCase(enrichedAlarm.getOriginatingManagedEntity().split(" ")[1]))) {
					enrichTopologyInfo(enrichedAlarm);
				}
				if ( !enrichedAlarm.isSuppressed() ) {
					enrichedAlarm = enrichCommonAlarmInfo(enrichedAlarm);
				}
				else {
					enrichedAlarm = null;
				}
			}


			//		if(!(GFPUtil.getConditionalDecomposedTraps().contains(enrichedAlarm.getCustomFieldValue(NetcoolFields.EVENT_KEY)))) {
			//			if("IPAG01".equalsIgnoreCase(enrichedAlarm.getCustomFieldValue(GFPFields.G2SUPPRESS))) { 
			//				if(GFPUtil.getCienaDecompositionAlarms().contains(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) { 
			//				GFPUtil.forwardAlarmToDecomposerInstance(enrichedAlarm, "CIENA_DECOMPOSER");
			//				} else if(GFPUtil.getJuniperDecompositionAlarms().contains(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
			//				GFPUtil.forwardAlarmToDecomposerInstance(enrichedAlarm, "JUNIPER_DECOMPOSER");
			//				} else if(GFPUtil.getAdtranDecompositionAlarms().contains(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
			//				GFPUtil.forwardAlarmToDecomposerInstance(enrichedAlarm, "ADTRAN_DECOMPOSER");
			//				} 
			//				enrichedAlarm = null; 
			//			} 
			//		} 

			if(enrichedAlarm != null) {

				super.processIncomingData(enrichedAlarm);
			} 
		} catch(Exception e) {
			if (log.isTraceEnabled()) {
				log.trace("processIncomingData: ERROR:"
						+ Arrays.toString(e.getStackTrace()));
			}
			e.printStackTrace();
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "processIncomingData()");
		}
	}


	@Override
	public AlarmCommon onAlarmCreationProcess(Alarm alarm) {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "onAlarmCreationProcess()");
		}
		EnrichedPreprocessAlarm enrichedAlarm = null;
		try {
			if (alarm.getPerceivedSeverity() == PerceivedSeverity.CLEAR) {
				if (log.isTraceEnabled()) {
					log.trace("onAlarmCreationProcess: Dropping the alarm as the severity is CLEAR "
							+ alarm.getIdentifier()
							+ "|{"
							+ alarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}");
				}
				return null;
			}
			// setPurgeInterval(enrichedAlarm);
			if(alarm instanceof EnrichedPreprocessAlarm) {
				enrichedAlarm = (EnrichedPreprocessAlarm) alarm;
			}
			if (log.isTraceEnabled()) {
				log.trace("Is DUPLICATE = " + enrichedAlarm.isDuplicate());
			}

			enrichedAlarm.getVar().put("PurgingOnGoing", false);
			if((enrichedAlarm.getAlarmState() == com.att.gfp.data.ipagAlarm.AlarmState.pending) && (enrichedAlarm.getPerceivedSeverity() != PerceivedSeverity.CLEAR)) {
				service_util.sendAlarm(getScenario(), enrichedAlarm);
				enrichedAlarm.setAlarmState(com.att.gfp.data.ipagAlarm.AlarmState.sent);  
			}

		} catch (Exception e) { 
			if (log.isTraceEnabled()) {
				log.trace("onAlarmCreationProcess: ERROR:"
						+ Arrays.toString(e.getStackTrace()));
			}
			e.printStackTrace();
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "onAlarmCreationProcess()");
		}

		return enrichedAlarm;
	}

	private void enrichTopologyInfo(EnrichedPreprocessAlarm enrichedAlarm) {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "enrichTopologyInfo()");
		}

		processCDCSubscription(enrichedAlarm);
		try {
			getAllTopologyInformation(enrichedAlarm);  
		} catch (Exception e) {
			if (log.isTraceEnabled()) {
				log.trace("enrichTopologyInfo: Topology ERROR:" + Arrays.toString(e.getStackTrace()));
				log.trace("Discarding the alarm " + enrichedAlarm.getIdentifier() + "|{" + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}" + " because of the exception thrown by topology");
			}
			enrichedAlarm.setSuppressed(true); 
		} 
		if(!enrichedAlarm.getAlarmTargetExist() && !enrichedAlarm.isSuppressed()) {
			if(GFPUtil.getAdtranJuniperTraps().contains(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY)) ||
					enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50003/100/33") ) {
				if ( enrichedAlarm.getDeviceLevelExist() ) {
					if (log.isTraceEnabled()) {
						log.trace("Setting the alarm " + enrichedAlarm.getIdentifier() + "|{" + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}" + " as pass through since alarm target does not exist but target Device exists");
					}
					enrichedAlarm.setPassthru(true);
				} else {
					if (log.isInfoEnabled()) {
						log.info("Discarding the alarm " + enrichedAlarm.getIdentifier() + "|{" + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}" + " because alarm target Device does not exist in topology");
					}
					enrichedAlarm.setSuppressed(true);
				}
			} 
			else {
				if(!("ALL".equalsIgnoreCase(enrichedAlarm.getOriginatingManagedEntity().split(" ")[1]))) {
					if (log.isInfoEnabled()) {
						log.info("Discarding the alarm " + enrichedAlarm.getIdentifier() + "|{" + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}" + " because alarm target does not exist in topology");
					}
					enrichedAlarm.setSuppressed(true); 
				} 
			}
		}
		if ( !enrichedAlarm.isSuppressed() ) {
			if("PPORT".equalsIgnoreCase(enrichedAlarm.getOrigMEClass())) {
				doPportSuppression(enrichedAlarm);
				if ( enrichedAlarm.isSuppressed() ) {
					if (log.isTraceEnabled()) {
						LogHelper.exit(log, "enrichTopologyInfo()");
					}
					return;
				}
				if ( null != enrichedAlarm.getRemotePtnii()  && !enrichedAlarm.getRemotePtnii().isEmpty()) {
					if( enrichedAlarm.getCustomFieldValue(GFPFields.INFO2) == null || enrichedAlarm.getCustomFieldValue(GFPFields.INFO2).isEmpty() || 
							enrichedAlarm.getCustomFieldValue(GFPFields.INFO2).startsWith("null") ) {
						enrichedAlarm.setCustomFieldValue(GFPFields.INFO2,  
								"RemoteDevicePTNII=<" + enrichedAlarm.getRemotePtnii() + ">");
					} else {
						enrichedAlarm.setCustomFieldValue(GFPFields.INFO2, enrichedAlarm.getCustomFieldValue(GFPFields.INFO2) + 
								" RemoteDevicePTNII=<" + enrichedAlarm.getRemotePtnii() + ">");
					}

				}
				
				if ( null != enrichedAlarm.getRemoteDeviceModel()  && !enrichedAlarm.getRemoteDeviceModel().isEmpty()) {
					if( enrichedAlarm.getCustomFieldValue(GFPFields.INFO2) == null || enrichedAlarm.getCustomFieldValue(GFPFields.INFO2).isEmpty() || 
							enrichedAlarm.getCustomFieldValue(GFPFields.INFO2).startsWith("null") ) {
						enrichedAlarm.setCustomFieldValue(GFPFields.INFO2,  
								"RemoteDeviceModel=<" + enrichedAlarm.getRemoteDeviceModel() + ">");
					} else {
						enrichedAlarm.setCustomFieldValue(GFPFields.INFO2, enrichedAlarm.getCustomFieldValue(GFPFields.INFO2) + 
								" RemoteDeviceModel=<" + enrichedAlarm.getRemoteDeviceModel() + ">");
					}
					
				}
				
				if ( null != enrichedAlarm.getRemotePortAid() && !enrichedAlarm.getRemotePortAid().isEmpty()) {
					if( enrichedAlarm.getCustomFieldValue(GFPFields.INFO2) == null || enrichedAlarm.getCustomFieldValue(GFPFields.INFO2).isEmpty() || 
							enrichedAlarm.getCustomFieldValue(GFPFields.INFO2).startsWith("null") ) {
						enrichedAlarm.setCustomFieldValue(GFPFields.INFO2,  
								"RemotePortAID=<" + enrichedAlarm.getRemotePortAid() + ">");
					} else {
						enrichedAlarm.setCustomFieldValue(GFPFields.INFO2, enrichedAlarm.getCustomFieldValue(GFPFields.INFO2) + 
								" RemotePortAID=<" + enrichedAlarm.getRemotePortAid() + ">");
					}
				}
			}
			if("LPORT".equalsIgnoreCase(enrichedAlarm.getOrigMEClass())) {
				doLportSuppression(enrichedAlarm);
			} 
			if("DEVICE".equalsIgnoreCase(enrichedAlarm.getOrigMEClass())) {
				doDeviceSuppression(enrichedAlarm);
			}
			checkIfsubscribedToCDC(enrichedAlarm);
			// if we are suppressing this alarms because of severity and device type, then
			// do not bother looking up the tunable
			setHairPinIndicator(enrichedAlarm);
			setPtnii(enrichedAlarm);

			if ( enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50005/6/3303") ) {
				if (log.isTraceEnabled()) {
					log.trace("Updating reason/info2/info3 for alarm with event key 50005/6/3303. " + enrichedAlarm.getIdentifier() + "|{" + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}");
				}
				updateBgpMonAlarm(enrichedAlarm);
			}
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "enrichTopologyInfo()");
		}
	}

	private void checkIfsubscribedToCDC(EnrichedPreprocessAlarm enrichedAlarm) {
		if("IPAG".equalsIgnoreCase(enrichedAlarm.getSourceIdentifier()) || 
				"adtran".equalsIgnoreCase(enrichedAlarm.getSourceIdentifier()) || 
				"ciena".equalsIgnoreCase(enrichedAlarm.getSourceIdentifier()) || 
				"infovista".equalsIgnoreCase(enrichedAlarm.getSourceIdentifier()) || 
				"cisco".equalsIgnoreCase(enrichedAlarm.getSourceIdentifier()) || 
				"pmoss".equalsIgnoreCase(enrichedAlarm.getSourceIdentifier()) ) {
			if("PPORT".equalsIgnoreCase(enrichedAlarm.getOrigMEClass()) || 
					"CARD".equalsIgnoreCase(enrichedAlarm.getOrigMEClass()) ||
					"SLOT".equalsIgnoreCase(enrichedAlarm.getOrigMEClass())) {
				if("L2-7450-IPAG".equalsIgnoreCase(enrichedAlarm.getCdcSubscriptionType()) ||
						"L2-7750-IPAG".equalsIgnoreCase(enrichedAlarm.getCdcSubscriptionType()) ) {
					enrichedAlarm.setCustomFieldValue("cdc-subscription-type", enrichedAlarm.getCdcSubscriptionType()); 
					enrichedAlarm.setCustomFieldValue("RealTimeFlag", "R");    
					enrichedAlarm.setSendToCdc(true);      
					//               PreprocessHelper.updateCdcInfoForCienaCFM(enrichedAlarm);
				}
			}
			if(EVC.equalsIgnoreCase(enrichedAlarm.getOrigMEClass())) {
				if("VLXP".equalsIgnoreCase(enrichedAlarm.getCdcSubscriptionType())) {
					enrichedAlarm.setCustomFieldValue("cdc-subscription-type", enrichedAlarm.getCdcSubscriptionType()); 
					enrichedAlarm.setCustomFieldValue("RealTimeFlag", "R");    
					enrichedAlarm.setSendToCdc(true);      
					PreprocessHelper.updateCdcInfoForCienaCFM(enrichedAlarm);
				}

			}
		}
	}

	private void doDeviceSuppression(EnrichedPreprocessAlarm enrichedAlarm) {
		if(enrichedAlarm.getAlarmTargetExist()) {
			if("N".equalsIgnoreCase(enrichedAlarm.getInEffect())) {
				if (log.isTraceEnabled()) {
					log.trace("SUPRESSED DUE TO INEFFECT BEING N");
				}
				//enrichedAlarm.setCustomFieldValue("SuppressDevice", "true");
				enrichedAlarm.setSuppressed(true);
			}
		}
	}

	private void doPportSuppression(EnrichedPreprocessAlarm enrichedAlarm) {
		if(enrichedAlarm.getAlarmTargetExist()) {
			if("N".equalsIgnoreCase(enrichedAlarm.getInEffect())) {
				if (log.isTraceEnabled()) {
					log.trace("SUPRESSED DUE TO INEFFECT BEING N");
				}
				//enrichedAlarm.setCustomFieldValue("SuppressPport", "true");
				enrichedAlarm.setSuppressed(true);
			}
			else if("JUNIPER MX SERIES".equalsIgnoreCase(enrichedAlarm.getDeviceType()) && 
					null !=  enrichedAlarm.getRemoteDeviceTypeFromLocalPort() &&    
					!(enrichedAlarm.getRemoteDeviceTypeFromLocalPort().contains("ALCATEL")) && 
					(enrichedAlarm.getRemotePportInstanceName() == null || (enrichedAlarm.getRemotePportInstanceName().isEmpty()))) {

				if (log.isTraceEnabled()) { 
					log.trace("SUPRESSED due to NO Remote Pport " + enrichedAlarm.getIdentifier() +
							" SeqNum = " + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER));
				} 
				enrichedAlarm.setSuppressed(true);
			}
			else if( null != enrichedAlarm.getDeviceRole() && !enrichedAlarm.getDeviceRole().isEmpty() 
					&& enrichedAlarm.getDeviceRole().equals("NM-HUB") ) {
				if ( null == enrichedAlarm.getRemotePportKey() || enrichedAlarm.getRemotePportKey().isEmpty() ) { 
					if (log.isTraceEnabled()) { 
						log.trace("SUPRESSED due to NM-HUB device role with NO Remote Pport " + enrichedAlarm.getIdentifier() +
								" SeqNum = " + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER));
					} 
					enrichedAlarm.setSuppressed(true);
				}
			}
			else if( null != enrichedAlarm.getDeviceType() && !enrichedAlarm.getDeviceType().isEmpty() && 
					(enrichedAlarm.getDeviceType().equals("HU4") || enrichedAlarm.getDeviceType().equals("HU6")) ) {
				if ( null == enrichedAlarm.getRemotePportKey() || enrichedAlarm.getRemotePportKey().isEmpty() ) { 
					if (log.isTraceEnabled()) { 
						log.trace("SUPRESSED due to HU4/HU6 device type with NO Remote Pport " + enrichedAlarm.getIdentifier() +
								" SeqNum = " + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER));
					} 
					enrichedAlarm.setSuppressed(true);
				}
			}
		}
	}

	private void doLportSuppression(EnrichedPreprocessAlarm enrichedAlarm) {

		if(enrichedAlarm.getAlarmTargetExist()) {
			if("JUNIPER MX SERIES".equalsIgnoreCase(enrichedAlarm.getDeviceType()) && 
					null != enrichedAlarm.getContaiiningPportremDevType() &&
					!enrichedAlarm.getContaiiningPportremDevType().isEmpty() &&
					(enrichedAlarm.getContaiiningPportremDevType()).contains("ALCATEL") ) {
				return; 
			}
			if("JUNIPER MX SERIES".equalsIgnoreCase(enrichedAlarm.getDeviceType()) && 
					( null == enrichedAlarm.getContainingPportRemotePPortInstance() || enrichedAlarm.getContainingPportRemotePPortInstance().isEmpty() ) ) {
				if (log.isTraceEnabled()) { 
					log.trace("SUPRESSED due to Lport with NO Remote Pport "  + enrichedAlarm.getIdentifier() +
							" SeqNum = " + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER)); 
				} 
				//enrichedAlarm.setCustomFieldValue("SuppressLport", "true");
				enrichedAlarm.setSuppressed(true);
			}
		}  
	}

	private CDCAlarm processCDCSubscription(
			EnrichedPreprocessAlarm enrichedAlarm) {
		if ("CDC".equals(enrichedAlarm.getSourceIdentifier())) {
			// Note: Do not insert the CDC Subscription Alarm in UCA-EBC
			// ipagPreProcess Value Pack Working Memory.
			CDCAlarm cdcAlarm = new CDCAlarm(enrichedAlarm);
			cdcAlarm.setOperation(enrichedAlarm
					.getCustomFieldValue(GFPFields.CDC_OPERATION));
			cdcAlarm.setSubscriptionId(enrichedAlarm
					.getCustomFieldValue(GFPFields.CDC_SUBSCRIPTIONID));
			cdcAlarm.setSubscriptionType(enrichedAlarm
					.getCustomFieldValue(GFPFields.CDC_SUBSCRIPTIONTYPE));
			cdcAlarm.setFromAppId(enrichedAlarm
					.getCustomFieldValue(GFPFields.CDC_FROMAPPID));
			cdcAlarm.setPubEventType(enrichedAlarm
					.getCustomFieldValue(GFPFields.CDC_PUBEVENTTYPE));
			cdcAlarm.setInitialize(enrichedAlarm
					.getCustomFieldValue(GFPFields.CDC_INITIALIZE));
			cdcAlarm.setInitializeTimeStamp(enrichedAlarm
					.getCustomFieldValue(GFPFields.CDC_INITIALIZETIMESTAMP));
			cdcAlarm.setCustomFieldValue(
					com.att.gfp.data.util.NetcoolFields.PURGE_INTERVAL, "3600");
			if ("add".equals(cdcAlarm.getOperation())) {
				if (!IpagTopoAccess.getInstance()
						.searchNUpdateCDCNode(cdcAlarm)) {
					if (log.isTraceEnabled()) {
						log.trace("onAlarmCreationProcess : cdc node does not exists in topology");
					}
					IpagTopoAccess.getInstance().createCdcNode(cdcAlarm);
				}
				return cdcAlarm;
			}
			if ("delete".equals(cdcAlarm.getOperation())) {
				IpagTopoAccess.getInstance().deleteCdcNode(cdcAlarm);
			}
		}
		return null;
	}

	private EnrichedPreprocessAlarm enrichCommonAlarmInfo(
			EnrichedPreprocessAlarm enrichedAlarm) {
		IpagXmlConfiguration ipagXmlConfig = (IpagXmlConfiguration) service_util.retrieveBeanFromContextXml(getScenario(), "ipagXmlConfig");
		List<String> cpeCdcEvents = ipagXmlConfig.getIpagPolicies().getCpeCdcAlarms().getEventNames().getEventName();
		if(cpeCdcEvents.contains(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
			enrichedAlarm.setIsCpeCdcEvent(true);
		} 

		if(GFPUtil.getCienaClciTrapsList().contains((enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY)))) {
			IpagTopoAccess.getInstance().findClciForCienaAlarms(enrichedAlarm);
		} 

		if(GFPUtil.getPmossTrapsList().contains((enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY)))) {
			setClassificationAndDomainForPmossTraps(findOrigNetworkFromComponent(enrichedAlarm.getCustomFieldValue(GFPFields.COMPONENT)),enrichedAlarm);
		}
		if(GFPUtil.getPmossCompassTrapsList().contains(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
			setClassificationAndDomainForPmossCompassTraps(findV9FromComponent(enrichedAlarm.getCustomFieldValue(GFPFields.COMPONENT)),enrichedAlarm);
		}

		if(enrichedAlarm == null) {   
			return null;  
		}
		setAsPassthru(enrichedAlarm);    

		suppressPportBasedOnRemoteDevicedType(enrichedAlarm);
		suppressBasedOnNFOorCFOClassification(enrichedAlarm);


		// are we suppressing yet?
		if(!enrichedAlarm.isSuppressed()) {			 
			// alarm classification and domain suppression
			if(!SuppressByClassificationAndDomainAndOperational(enrichedAlarm)) {

				// save the tunable in a field we can see from the rules
				enrichedAlarm.setTunable(enrichedAlarm.getCustomFieldValue(NetcoolFields.EVENT_KEY));
				setAlarmDomainForInfoVistaAlarms(enrichedAlarm);
				if(enrichedAlarm.isSuppressed()) {
					return null;
				}

				// set all of the custom fields in the alarm
				if(enrichedAlarm.getLegacyOrgInd() == null) {
					enrichedAlarm.setLegacyOrgInd(""); 
				}
				if(enrichedAlarm.getCustomFieldValue(GFPFields.REASON) == null) {
					enrichedAlarm.setCustomFieldValue(GFPFields.REASON, "" + " Region=<" + enrichedAlarm.getLegacyOrgInd() + ">");
				} else {
					enrichedAlarm.setCustomFieldValue(GFPFields.REASON, enrichedAlarm.getCustomFieldValue(GFPFields.REASON) + " Region=<" + enrichedAlarm.getLegacyOrgInd() + ">");
				}   
				setInfo1ForCienaAlarms(enrichedAlarm);  
				addDevSubRoleToInfo1ForJuniperAlarms(enrichedAlarm);
				enrichedAlarm.createMissingCustomFields();
				String classification = enrichedAlarm.getCustomFieldValue(GFPFields.CLASSIFICATION);
				if (classification.contains ("CFO") && !("CNI-CFO".equalsIgnoreCase(classification)))
				{
					String objectType = "AlarmObjectType=<" + enrichedAlarm.getCustomFieldValue(NetcoolFields.SM_CLASS)+">";
					if((enrichedAlarm.getCustomFieldValue("info") != null) && !(enrichedAlarm.getCustomFieldValue("info").isEmpty())) {
						enrichedAlarm.setCustomFieldValue("info", enrichedAlarm.getCustomFieldValue("info") + objectType);
					} else {
						enrichedAlarm.setCustomFieldValue("info", objectType); 
					}
				}
				SuppressionBean suppresionBean = (SuppressionBean) service_util.retrieveBeanFromContextXml(getScenario(), "suppressionBean"); 

				//				if(ipagXmlConfig.getSuppresionMap().containsKey(GFPFields.NFO_MOBILITYUNI)) {
				//			    	if("Y".equalsIgnoreCase(ipagXmlConfig.getSuppresionMap().get(GFPFields.NFO_MOBILITYUNI).getValue())) {
				if(suppresionBean != null && suppresionBean.isNfomobilityuniSuppression()) {
					if("NFO-MOBILITYUNI".equalsIgnoreCase(enrichedAlarm.getCustomFieldValue(GFPFields.CLASSIFICATION)) && 
							"NTE".equalsIgnoreCase(enrichedAlarm.getCustomFieldValue(GFPFields.DOMAIN))	) {
						if(log.isTraceEnabled()) {
							log.trace("Dropping the alarm because the classification is NFO-MOBILITYUNI "
									+ enrichedAlarm.getIdentifier()
									+ "|{"
									+ enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}");
						}
						return null; 
					}   
				}
				//				    }
				//			    }

				if(enrichedAlarm.getIsCpeCdcEvent()) { 
					if(log.isTraceEnabled()) {  
						log.trace("is a Cpe CDC ALARM " + enrichedAlarm.getIdentifier());
					}
					PreprocessHelper.processCpeCdcAlarms(enrichedAlarm);
				}


			} else {
				// we have suppress this alarm so we can just not insert it into WM
				if (log.isTraceEnabled()) {
					log.trace("Alarm " + enrichedAlarm.getIdentifier() + " suppress by PreProcessing");
				}
				enrichedAlarm = null;
			}
		} else {
			// this has been suppressed
			enrichedAlarm = null;
		}  
		return enrichedAlarm;
	}

	private void setPtnii(EnrichedPreprocessAlarm enrichedAlarm) {
		/*if(  "JUNIPER MX SERIES".equalsIgnoreCase(enrichedAlarm.getDeviceType()) ||
			 "NV1".equalsIgnoreCase(enrichedAlarm.getDeviceType()) ||
			 "NV2".equalsIgnoreCase(enrichedAlarm.getDeviceType()) 
				) {*/
		if( null != enrichedAlarm.getPtnii() && !(enrichedAlarm.getPtnii().isEmpty())) {
			if(enrichedAlarm.getCustomFieldValue(GFPFields.INFO3) == null) {
				enrichedAlarm.setCustomFieldValue(GFPFields.INFO3, ""); 
			}
			if(enrichedAlarm.getCustomFieldValue(GFPFields.INFO3).isEmpty()) {
				enrichedAlarm.setCustomFieldValue(GFPFields.INFO3, "PTNII=<" + enrichedAlarm.getPtnii() + ">");
			} else {
				enrichedAlarm.setCustomFieldValue(GFPFields.INFO3, enrichedAlarm.getCustomFieldValue(GFPFields.INFO3) + " PTNII=<" + enrichedAlarm.getPtnii() + ">");
			}
		} 
		//}
	} 

	private void setHairPinIndicator(EnrichedPreprocessAlarm enrichedAlarm) {
		if("PPORT".equalsIgnoreCase(enrichedAlarm.getOrigMEClass()) && 
				("JUNIPER MX SERIES".equalsIgnoreCase(enrichedAlarm.getDeviceType()) ||
						"NV1".equalsIgnoreCase(enrichedAlarm.getDeviceType()) ||
						"NV2".equalsIgnoreCase(enrichedAlarm.getDeviceType()) ||
						"NV3".equalsIgnoreCase(enrichedAlarm.getDeviceType()) ) &&
				"Y".equalsIgnoreCase(enrichedAlarm.getHairPinIndicator())) {
			if (log.isTraceEnabled()) {
				log.trace("setting HairPin Indicator for alarm " + enrichedAlarm.getIdentifier());
			}
			String info1 = enrichedAlarm.getCustomFieldValue(GFPFields.INFO1);
			if(info1 != null && !(info1.isEmpty())) {
				enrichedAlarm.setCustomFieldValue(GFPFields.INFO1, info1 + "Hairpin=<Y>");
			} else {
				enrichedAlarm.setCustomFieldValue(GFPFields.INFO1, "Hairpin=<Y>");
			}
		} 

	}

	private void suppressPportBasedOnRemoteDevicedType(
			EnrichedPreprocessAlarm enrichedAlarm) {
		if(GFPUtil.getPportRemoteDeciveSuppressionTrapsList().contains(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
			if(enrichedAlarm.getRemoteDeviceType() != null && "CIENA EMUX".equalsIgnoreCase(enrichedAlarm.getRemoteDeviceType())) {
				if (log.isInfoEnabled()) {
					log.info("Suppressing this pport " + enrichedAlarm.getIdentifier() + "|{" + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}" + " because remote device type is CIENA EMUX");
				}
				enrichedAlarm.setSuppressed(true);
			}
		}

	} 

	private void suppressBasedOnNFOorCFOClassification(
			EnrichedPreprocessAlarm enrichedAlarm) {
		if("IPAG01".equalsIgnoreCase(enrichedAlarm.getCustomFieldValue(GFPFields.G2SUPPRESS))) {
			if("NFO".equalsIgnoreCase(enrichedAlarm.getCustomFieldValue(NetcoolFields.CLASSIFICATION)) ||
					"CNI-CFO".equalsIgnoreCase(enrichedAlarm.getCustomFieldValue(NetcoolFields.CLASSIFICATION))) {
				enrichedAlarm.setSuppressed(true); 			
			}
		}		
	} 	

	private void setClassificationAndDomainForPmossCompassTraps(
			String findV9FromComponent, EnrichedPreprocessAlarm enrichedAlarm) {
		if("VPE".equalsIgnoreCase(findV9FromComponent)) {
			enrichedAlarm.setCustomFieldValue(NetcoolFields.CLASSIFICATION, "NFO2");
			enrichedAlarm.setCustomFieldValue(NetcoolFields.DOMAIN, "VPLS-PE-PMOS"); 
		} else {
			enrichedAlarm.setCustomFieldValue(NetcoolFields.CLASSIFICATION, "ETHERNET-NFO");
			enrichedAlarm.setCustomFieldValue(NetcoolFields.DOMAIN, "IPAG-PMOS"); 
		} 
		if("50005/6/9208".equalsIgnoreCase(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
			removeInfo1FromReasonField(enrichedAlarm);
		}
	}


	private void removeInfo1FromReasonField( 
			EnrichedPreprocessAlarm enrichedAlarm) {
		String probableCause = enrichedAlarm.getCustomFieldValue(GFPFields.REASON);
		if(probableCause != null) {
			if(probableCause.contains("INFO1=")) {
				Pattern pattern = Pattern.compile("INFO1=<\\w*>");
				Matcher mather = pattern.matcher(probableCause); 
				if(mather.find()) {
					if(mather.group() != null) {
						enrichedAlarm.setCustomFieldValue(GFPFields.REASON, probableCause.replace(mather.group(), ""));
					}
				} 
			}
		}
	}


	private String findV9FromComponent(String component) {
		String v9Value = "";
		if(component.contains("Network=")) {
			Pattern pattern = Pattern.compile("Network=\\w*");
			Matcher mather = pattern.matcher(component);
			if(mather.find()) {
				if(mather.group() != null) {
					String[] origNets = mather.group().split("\\=");
					if(origNets.length > 1) {
						if (log.isTraceEnabled()) {
							log.trace(" V9 from component =  " + origNets[1]);
						}
						v9Value = origNets[1];
					} 
				}
			}
		}
		return v9Value; 	
	}

	private void setClassificationAndDomainForPmossTraps(
			String origNetworkFromComponent,
			EnrichedPreprocessAlarm enrichedAlarm) {
		if(origNetworkFromComponent.isEmpty()) {
			if (log.isTraceEnabled()) {
				log.trace("onAlarmCreationProcess:setClassificationAndDomainForPmossTraps Dropping the alarm becuase No Network value found in component " + enrichedAlarm.getIdentifier() + "|{" + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}");
			}
			enrichedAlarm = null;
			return;
		} else if("Metro-NPE".equalsIgnoreCase(origNetworkFromComponent) || "Heartbeat".equalsIgnoreCase(origNetworkFromComponent)) {
			enrichedAlarm.setCustomFieldValue(NetcoolFields.CLASSIFICATION, "ETHERNET-NFO");
			enrichedAlarm.setCustomFieldValue(NetcoolFields.DOMAIN, "IPAG-PMOS"); 
		} else if("NTE-NPE".equalsIgnoreCase(origNetworkFromComponent)) {
			enrichedAlarm.setCustomFieldValue(NetcoolFields.CLASSIFICATION, "NFO-MOBILITY");
			enrichedAlarm.setCustomFieldValue(NetcoolFields.DOMAIN, "NTE"); 
			setFlagsInReason(enrichedAlarm);
		} else if("VPLS-NPE".equalsIgnoreCase(origNetworkFromComponent)) {
			enrichedAlarm.setCustomFieldValue(NetcoolFields.CLASSIFICATION, "NFO2");
			enrichedAlarm.setCustomFieldValue(NetcoolFields.DOMAIN, "VPLS-PE");  
		} else {
			if (log.isTraceEnabled()) {
				log.trace("onAlarmCreationProcess:setClassificationAndDomainForPmossTraps Alarm Dropped as Network: orig-network is Unknown " + enrichedAlarm.getIdentifier() + "|{" + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}");
			}
			enrichedAlarm = null;
			return;
		}
		//commented out as per MR gfpc150089
		//		 if(!("NTE-NPE".equalsIgnoreCase(origNetworkFromComponent))) {
		//			 setReasonandInfoFieldsForPmossTraps(enrichedAlarm); 
		//		 }
	}


	private void setReasonandInfoFieldsForPmossTraps(
			EnrichedPreprocessAlarm enrichedAlarm) {
		String probableCause = enrichedAlarm.getCustomFieldValue(GFPFields.REASON); 
		if(probableCause != null) {
			String[] times  = probableCause.split("Time=");
			if(times.length > 1) {
				int infoLen = times[0].trim().length();
				String infos = times[0].trim(); 
				if(infoLen <= 250) {
					enrichedAlarm.setCustomFieldValue(GFPFields.INFO1, infos);
				} else if(infoLen > 250 && infoLen <= 500) {
					enrichedAlarm.setCustomFieldValue(GFPFields.INFO1, infos.substring(0, 250));
					enrichedAlarm.setCustomFieldValue(GFPFields.INFO2, infos.substring(250, infos.length()));
				} else if(infoLen > 500) {
					enrichedAlarm.setCustomFieldValue(GFPFields.INFO1,infos.substring(0, 250)); 
					enrichedAlarm.setCustomFieldValue(GFPFields.INFO2,infos.substring(250, 500)); 
					enrichedAlarm.setCustomFieldValue(GFPFields.INFO3,infos.substring(500, infos.length())); 
				}
				enrichedAlarm.setCustomFieldValue(GFPFields.REASON, "Time=" + times[1]);
			} else {
				enrichedAlarm.setCustomFieldValue(GFPFields.REASON, probableCause); 
			}
		} 

	} 


	private void setFlagsInReason(EnrichedPreprocessAlarm enrichedAlarm) {
		enrichedAlarm.setCustomFieldValue(GFPFields.REASON, enrichedAlarm.getCustomFieldValue(GFPFields.REASON) + " FLAGS={" + "Region=<" + enrichedAlarm.getLegacyOrgInd() + ">}"); 
	}  


	private String findOrigNetworkFromComponent(String component) {
		String origNetwork = "";
		if(component.contains("Network=")) {
			Pattern pattern = Pattern.compile("Network=\\w*-\\w*");
			Matcher mather = pattern.matcher(component);
			if(mather.find()) {
				if(mather.group() != null) {
					String[] origNets = mather.group().split("\\=");
					if(origNets.length > 1) {
						if (log.isTraceEnabled()) {
							log.trace(" Network from component =  " + origNets[1]);
						}
						origNetwork = origNets[1];
					}
				}
			}
		}
		else if(component.toLowerCase().contains("heartbeat")) {
			origNetwork = component;
		}
		return origNetwork; 	
	}


	private void setAsPassthru(EnrichedPreprocessAlarm enrichedAlarm) {
		if("50004/1/10".equalsIgnoreCase(enrichedAlarm.getCustomFieldValue(NetcoolFields.EVENT_KEY)) && "DEVICE".equalsIgnoreCase(enrichedAlarm.getOrigMEClass())) {
			if (log.isTraceEnabled()) {
				log.trace("setting 50004/1/10 DEVICE level alarm as passthru" + enrichedAlarm.getIdentifier());
			}
			enrichedAlarm.setPassthru(true); 
		}  
		if(("50001/100/50".equalsIgnoreCase(enrichedAlarm.getCustomFieldValue(NetcoolFields.EVENT_KEY))
				|| "50001/100/46".equalsIgnoreCase(enrichedAlarm.getCustomFieldValue(NetcoolFields.EVENT_KEY))
				|| "50001/100/47".equalsIgnoreCase(enrichedAlarm.getCustomFieldValue(NetcoolFields.EVENT_KEY))
				|| "50001/100/49".equalsIgnoreCase(enrichedAlarm.getCustomFieldValue(NetcoolFields.EVENT_KEY))		
				) && "DEVICE".equalsIgnoreCase(enrichedAlarm.getOrigMEClass())) {
			if (log.isTraceEnabled()) {
				log.trace("setting "+enrichedAlarm.getCustomFieldValue(NetcoolFields.EVENT_KEY)+ " DEVICE level alarm as passthru" + enrichedAlarm.getIdentifier());
			}
			enrichedAlarm.setPassthru(true); 
		}    
		//		if(enrichedAlarm.getCustomFieldValue("reason_code") != null && enrichedAlarm.getCustomFieldValue("reason_code").contains("Chronic")) {
		//			if (log.isTraceEnabled()) {
		//				log.trace("This is a Chronic alarm and setting it as a pass thru alarm " + enrichedAlarm.getIdentifier());
		//			}
		//			enrichedAlarm.setPassthru(true);  
		//		}  
	}


	private void setAlarmDomainForInfoVistaAlarms(
			EnrichedPreprocessAlarm enrichedAlarm) {
		if("50004/1/4".equals(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY)) || 
				"50004/1/22".equals(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
			if("NFO-EMUX".equalsIgnoreCase(enrichedAlarm.getCustomFieldValue(GFPFields.CLASSIFICATION))) {
				//			enrichedAlarm.setCustomFieldValue(GFPFields.DOMAIN, "EMUX-PMOS"); 
				if("EMT".equalsIgnoreCase(enrichedAlarm.getDeviceSubRole())) {   
					if (log.isTraceEnabled()) {
						log.trace("Setting 50004/1/4,50004/1/22 Alarm info1 filed to deviceSubRole");
					}
					if("50004/1/4".equals(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
						enrichedAlarm.setCustomFieldValue(GFPFields.DOMAIN, "EMUX-PMOS");   
					}
					enrichedAlarm.setCustomFieldValue(GFPFields.INFO1, "DeviceSubRole=<EMT>");  
				} 
			}   
			if("50004/1/22".equals(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
				if(enrichedAlarm.getCustomFieldValue(GFPFields.INFO1) == null || enrichedAlarm.getCustomFieldValue(GFPFields.INFO1).isEmpty() || 
						!("EMT".equalsIgnoreCase(enrichedAlarm.getDeviceSubRole()))) {
					if (log.isTraceEnabled()) {
						log.trace("Suppressing the alarm " + enrichedAlarm.getIdentifier() + "|{" + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}" + " because device sub role is null");
					}
					enrichedAlarm.setSuppressed(true);  
				} 
			}

		} 
	}  

	private void setInfo1ForCienaAlarms(
			EnrichedPreprocessAlarm enrichedAlarm) {
		if("50002/100/59".equals(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
			if("EMT".equalsIgnoreCase(enrichedAlarm.getDeviceSubRole())) {   
				if (log.isTraceEnabled()) {
					log.trace("Setting 50002/100/23 Alarm info1 filed to deviceSubRole");
				}
				enrichedAlarm.setCustomFieldValue(GFPFields.INFO1, "DeviceSubRole=<EMT>");  
			} 
		}    
	}  

	private void addDevSubRoleToInfo1ForJuniperAlarms(
			EnrichedPreprocessAlarm enrichedAlarm) {
		if( ( "JUNIPER MX SERIES".equals(enrichedAlarm.getDeviceType()) ||
				"NV1".equals(enrichedAlarm.getDeviceType()) ||
				"NV2".equals(enrichedAlarm.getDeviceType()) ||
				"NV3".equals(enrichedAlarm.getDeviceType()) ) && 
				("ETHERNET-NFO".equalsIgnoreCase(enrichedAlarm.getCustomFieldValue(GFPFields.CLASSIFICATION)))) {
			if((enrichedAlarm.getDeviceSubRole() != null) && !(enrichedAlarm.getDeviceSubRole().isEmpty())) {   
				if (log.isTraceEnabled()) {
					log.trace("Adding device subrole to INFO1 for Juniper alarms");
				}
				String info1 = enrichedAlarm.getCustomFieldValue(GFPFields.INFO1);
				if(info1 == null || info1.isEmpty()) {
					enrichedAlarm.setCustomFieldValue(GFPFields.INFO1, "DeviceSubRole=<"+ enrichedAlarm.getDeviceSubRole() +">");
				} else {
					enrichedAlarm.setCustomFieldValue(GFPFields.INFO1, info1 + "DeviceSubRole=<"+ enrichedAlarm.getDeviceSubRole() +">");
				}
			} 
		}    
	}  


	/**
	 * sets the purge-interval field for alarms
	 * @param enrichedAlarm
	 */
	private void setPurgeInterval(EnrichedPreprocessAlarm enrichedAlarm) {
		PurgeInterval purgeInterval = lookupPurgeInterval();
		if(enrichedAlarm.getPerceivedSeverity() == PerceivedSeverity.CLEAR) {
			enrichedAlarm.setCustomFieldValue(NetcoolFields.PURGE_INTERVAL, purgeInterval.getClearPurgeInterval());
		} else {
			enrichedAlarm.setCustomFieldValue(NetcoolFields.PURGE_INTERVAL, purgeInterval.getActivePurgeInterval());
		} 
	}


	private boolean managedClassIsEVC(String string) {
		if(EVC.equalsIgnoreCase(string)) {
			return true;
		}
		return false;
	}


	private boolean SuppressByClassificationAndDomainAndOperational(EnrichedPreprocessAlarm alarm) {

		boolean ret = false;

		String ems = null;
		try {
			ems = alarm.getCustomFieldValue("ems").toLowerCase();
		} catch (Exception e) {
			ems = "unknown";
		}

		// set classification based on the ems custom field
		if(ems.equals("pmoss")){
			alarm.setCustomFieldValue(NetcoolFields.CLASSIFICATION, ETHERNET_NFO);
			alarm.setCustomFieldValue(NetcoolFields.DOMAIN, IPAG_PMOS);	
		} else {
			// if the alarm target is INTERFACE or TUNNEL then we set the classification
			// and domain to predetermined values.   
			if(((alarm.getOriginatingManagedEntity().split(" ")[0]).equals("TUNNEL"))) {  
				alarm.setCustomFieldValue(NetcoolFields.CLASSIFICATION, ETHERNET_NFO);
				alarm.setCustomFieldValue(NetcoolFields.DOMAIN, "IPAG"); 
			} 
			// if the classification has not been set to this point, meaning it is
			// not available in topo, then we suppress this alarm.
			//if(alarm.getAlarmClassification() == null) {
			if(alarm.getCustomFieldValue(NetcoolFields.CLASSIFICATION) == null) {
				LogHelper.method(log, "SuppressByClassificationAndDomainAndOperational()", "Classification Missing, Suppressing Alarm " +
						alarm.getIdentifier());
				ret = true;
			}
			//			}
		}

		// if the object is not operational then suppress the alarm
		if(!alarm.isOperational()) {
			ret=true;

			if (log.isTraceEnabled()) { 
				LogHelper.method(log, "SuppressByClassificationAndDomainAndOperational()", "Not operational, Suppressing Alarm " +
						alarm.getIdentifier());
			}

		}

		// TODO: because the topo doesn't have the classification yet
		ret=false;

		return ret;
	}


	private void getAllTopologyInformation(EnrichedPreprocessAlarm alarm) {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "getAllTopologyInformation()");
		}

		// Find the device-name, device-model, and device-type of the target object.
		// Find the port_aid IF the target is a PPORT.
		// Find is_operational for the device

		String device = null;
		Boolean isDeviceLevel=false;

		String managedObjectInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String managedObjectClass = alarm.getOriginatingManagedEntity().split(" ")[0];
		if("INTERFACE".equalsIgnoreCase(managedObjectClass)) {
			managedObjectClass = PPORT;
		} 

		// Get the port_aid if this is a pport
		if(managedObjectClass.equals(PPORT)) {
			// make the query to topology
			IpagTopoAccess.getInstance().fetchPPortLevelInformation(managedObjectInstance, alarm); 
			// if it's a PPORT level alarm, suppress if local device type is JUNIPER MX SERIES and remote device type ADTRAN 5000 SERIES
			// and classification is ETHERNET-NFO

			if ( (null != alarm.getDeviceType()) && alarm.getDeviceType().equals("JUNIPER MX SERIES") && 
					alarm.getCustomFieldValue(NetcoolFields.CLASSIFICATION).equals("ETHERNET-NFO")) {
				if ( null != alarm.getRemoteDeviceTypeFromLocalPort() && !alarm.getRemoteDeviceTypeFromLocalPort().isEmpty() ) {
					if ( alarm.getRemoteDeviceTypeFromLocalPort().equals("CIENA EMUX") ) {
						if (alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(LINKDOWN_KEY)) {
							if (log.isTraceEnabled()) {
								log.trace("Continue processing... JUNIPER MX SERIES PPORT LinkDown Alarm where Remote is CIENA EMUX "
										+ alarm.getIdentifier()
										+ " SeqNum = "
										+ alarm.getCustomFieldValue(GFPFields.SEQNUMBER));
							}
						}
						else {
							if (log.isTraceEnabled()) {
								log.trace("SUPPRESS JUNIPER MX SERIES PPORT Alarm where Remote is CIENA EMUX "
										+ alarm.getIdentifier()
										+ " SeqNum = "
										+ alarm.getCustomFieldValue(GFPFields.SEQNUMBER));
							}
							alarm.setSuppressed(true);
						}
					} else if (alarm.getRemoteDeviceTypeFromLocalPort().equals("ADTRAN 5000 SERIES")) {
						if (log.isTraceEnabled()) {
							log.trace("SUPPRESS JUNIPER MX SERIES PPORT Alarm where Remote is ADTRAN 5000 SERIES "
									+ alarm.getIdentifier()
									+ " SeqNum = "
									+ alarm.getCustomFieldValue(GFPFields.SEQNUMBER));
						}
						alarm.setSuppressed(true);
					}
				}
			}
			if(!alarm.isSuppressed()) {
				if(!("ALCATEL".equalsIgnoreCase(alarm.getRemotePportKey()))) {
					IpagTopoAccess.getInstance().fetchRemotePPortLevelInformation(managedObjectInstance, alarm); 
					if("PE".equalsIgnoreCase(alarm.getNodeType())) {
						if(alarm.getRemoteDeviceTypeFromLocalPort() != null && !(alarm.getRemoteDeviceTypeFromLocalPort().contains("CIENA")) && !(alarm.getRemoteDeviceTypeFromLocalPort().contains("ADTRAN")))	{
							IpagTopoAccess.getInstance().fetchRemotePePPortLevelInformation(alarm.getRemotePportKey(), alarm);
						}
					}
				}
			}  
		}else {
			// anything but device level get just get the alarm classification and alarm domain
			if(!managedObjectClass.equals(DEVICE)) {
				IpagTopoAccess.getInstance().fetchSubclassLevelInformation(managedObjectInstance, 
						managedObjectClass, alarm);
				// if it's a LPORT level alarm, suppress if remote device type is CIENA EMUX or ADTRAN 5000 SERIES
				if ( managedObjectClass.equals(LPORT) && (null != alarm.getDeviceType()) && alarm.getDeviceType().equals("JUNIPER MX SERIES") ) {
					if ( null != alarm.getContaiiningPportremDevType() && !alarm.getContaiiningPportremDevType().isEmpty() ) {
						if ( alarm.getContaiiningPportremDevType().equals("CIENA EMUX") || alarm.getContaiiningPportremDevType().equals("ADTRAN 5000 SERIES") ) {
							if (log.isTraceEnabled()) {
								log.trace("SUPPRESS JUNIPER MX SERIES LPORT Alarm where Remote is EMUX " + alarm.getIdentifier() + 
										" SeqNum = " + alarm.getCustomFieldValue(GFPFields.SEQNUMBER));
							}
							alarm.setSuppressed(true);
						}
					}
				}

				if ( alarm.getCustomFieldValue(NetcoolFields.EVENT_KEY).equals("50002/100/52") ||
						alarm.getCustomFieldValue(NetcoolFields.EVENT_KEY).equals("50002/100/47") ||
						alarm.getCustomFieldValue(NetcoolFields.EVENT_KEY).equals("50002/100/48") ||
						alarm.getCustomFieldValue(NetcoolFields.EVENT_KEY).equals("50002/100/49") ||
						alarm.getCustomFieldValue(NetcoolFields.EVENT_KEY).equals("50002/100/50") ) {

					if ( null != alarm.getEvcName() && alarm.getEvcName().contains("VLXU") ) {
						if( null == alarm.getCustomFieldValue("flags") || alarm.getCustomFieldValue("flags").isEmpty() ) {
							alarm.setCustomFieldValue("flags", "ENNI=<Y>"); 
						}
						else {
							alarm.setCustomFieldValue("flags", alarm.getCustomFieldValue("flags") + " ENNI=<Y>");
						}
					}
				}

			} else {
				isDeviceLevel = true;
				if(alarm.getCustomFieldValue(NetcoolFields.EVENT_KEY).equals(LINKDOWN_KEY)) {
					String reasonCodeLagId = getLagIdFromReasonCode(alarm.getCustomFieldValue(NetcoolFields.REASON_CODE), alarm);
					if (log.isTraceEnabled()) {
						log.trace("LagId extracted from the reason_code field: " + reasonCodeLagId);
					}
					if( null != reasonCodeLagId  && !reasonCodeLagId.isEmpty()) {
						IpagTopoAccess.getInstance().getPportSetMatchedPortLagID(managedObjectInstance, reasonCodeLagId, alarm);
						// if it's a LAG linkdown, suppress if remote device type is CIENA EMUX or ADTRAN 5000 SERIES 
						if ( null != alarm.getRemoteDeviceType() && !alarm.getRemoteDeviceType().isEmpty() && 
								(null != alarm.getDeviceType()) && alarm.getDeviceType().equals("JUNIPER MX SERIES") ) {
							if ( alarm.getRemoteDeviceType().equals("CIENA EMUX") || alarm.getRemoteDeviceType().equals("ADTRAN 5000 SERIES") ) {
								if (log.isTraceEnabled()) {
									log.trace("SUPPRESS JUNIPER MX SERIES LAG/DEVICE Linkdown Alarm where Remote is EMUX " + alarm.getIdentifier() + 
											" SeqNum = " + alarm.getCustomFieldValue(GFPFields.SEQNUMBER));
								}
								alarm.setSuppressed(true);
							}
						}
					}
				}
			}
		}

		if(!alarm.isSuppressed()) {
			// strip off the garbage at the end so we can get the device level
			// stuff from topology
			device = managedObjectInstance.split("/")[0];
			// get device level info
			IpagTopoAccess.getInstance().fetchDeviceLevelInformation(device, alarm, isDeviceLevel, alarm.getNodeType());


			//checking both CE and PE for pmoss device level alarms
			if(managedObjectClass.equals(DEVICE) && "pmoss".equalsIgnoreCase(alarm.getSourceIdentifier()) && !(alarm.getAlarmTargetExist())) {
				if("PE".equalsIgnoreCase(alarm.getNodeType())) {
					IpagTopoAccess.getInstance().fetchDeviceLevelInformation(device, alarm, isDeviceLevel, "CE");
				} else {
					IpagTopoAccess.getInstance().fetchDeviceLevelInformation(device, alarm, isDeviceLevel, "PE");
				}
			} 

			IpagXmlConfiguration ipagXmlConfig = (IpagXmlConfiguration) service_util.retrieveBeanFromContextXml(getScenario(), "ipagXmlConfig");
			List<SuperEmux> superEmuxList = ipagXmlConfig.getIpagPolicies().getSuperEmuxDevices().getSuperEmux();
			for(SuperEmux superEmux : superEmuxList) {
				if(superEmux.getDeviceModel().equalsIgnoreCase(alarm.getDeviceModel())) {
					if (log.isTraceEnabled()) {
						log.trace("Alarm is a superEmux device Model = " +alarm.getDeviceModel());
					}
					//				if(isDeviceLevel) {
					//					alarm.setCustomFieldValue(GFPFields.CLASSIFICATION, "NFO-EMUX");
					//					alarm.setCustomFieldValue(GFPFields.DOMAIN, "EMUX");
					//				}
					if(!(alarm.getAlarmTargetExist()) && managedObjectClass.equals(PPORT)) {   
						List<portAidMapping> portAidMappingList = superEmux.getPortAidMapping();
						String[] deviceAndportAid = managedObjectInstance.split("/");  
						String portAid = "";
						if(deviceAndportAid.length == 2) {  
							portAid = deviceAndportAid[1];   
							boolean isMappingFound = false;
							try {
								if(Integer.parseInt(portAid) <= 48) {
									isMappingFound = true;
									alarm.setOriginatingManagedEntity(managedObjectClass + " " + deviceAndportAid[0] + "/1." + portAid);
								} else {
									for(portAidMapping portMapping : portAidMappingList) {
										if(portAid.equalsIgnoreCase(portMapping.getPortAid())) {
											isMappingFound = true;
											alarm.setOriginatingManagedEntity(managedObjectClass  + " " + deviceAndportAid[0] + "/" + portMapping.getGcpPortAid());
										}
									}
								}
							} catch(NumberFormatException e) { 
								if (log.isTraceEnabled()) {
									log.trace("potAid is not Number");
								}
								return;   
							}
							if(isMappingFound) { 
								String managedObjectInstancePport = alarm.getOriginatingManagedEntity().split(" ")[1];
								IpagTopoAccess.getInstance().fetchPPortLevelInformation(managedObjectInstancePport, alarm); 
								if(!alarm.isSuppressed()) {
									IpagTopoAccess.getInstance().fetchRemotePPortLevelInformation(managedObjectInstancePport, alarm); 
									populateInfo2ForCienaEmuxAlarms(alarm);
								} 
							}
						} 
					}
				} 
			}
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "getAllTopologyInformation()");
		}

	}

	private void populateInfo2ForCienaEmuxAlarms(EnrichedPreprocessAlarm alarm) {
		if("NFO-MOBILITY".equalsIgnoreCase(alarm.getCustomFieldValue(NetcoolFields.CLASSIFICATION)) || 
				"NFO-MOBILITYUNI".equalsIgnoreCase(alarm.getCustomFieldValue(NetcoolFields.CLASSIFICATION))) {
			String info2 = alarm.getCustomFieldValue(GFPFields.INFO2);
			if(info2 == null || info2.isEmpty()) {
				info2 = "";
			} 
			if(!(info2.contains("MultiUNI"))) { 
				String multiUni = alarm.getMultiUni();
				if("Y".equalsIgnoreCase(multiUni)) { 
					alarm.setCustomFieldValue(GFPFields.INFO2, info2 + "MultiUNI=<Y>"); 
				}  
			}
		}			 

	}


	/* 
	 * Methods regarding Tunable lookup
	 */
	private void extractOIDAndTunableLookup(EnrichedPreprocessAlarm alarm) {

		String oid = "unknown";

		// look for the EventKey alarm field, if not there then try to assign the default
		// tunable
		if(alarm.getCustomFieldValue(NetcoolFields.EVENT_KEY)!=null) {
			if (log.isTraceEnabled()) {
				log.trace("### EventKey :" + alarm.getCustomFieldValue(NetcoolFields.EVENT_KEY));
			}
			oid = alarm.getCustomFieldValue(NetcoolFields.EVENT_KEY);
		} else {
			if (log.isTraceEnabled()) {
				log.trace("Invalid Alarm recieved, EventKey is not set: [A=" +
						alarm.getIdentifier() + "][OID=" + oid + "]");
			}

			// assign a default key (10000/500/1) if device is in the topology  
			AssignDefaultTunableOrSuppress(alarm);
		}

		// lookup tunable
		if( !oid.equals("unknown")) {
			// lookup the oid and set the tunable
			String tunable = null;
			String sendToRuby = "true";
			String purgeInterval = "604800";
			if(lookupOIDRetieveEname(oid) != null) {
				tunable = lookupOIDRetieveEname(oid).getEname();
				sendToRuby = lookupOIDRetieveEname(oid).getSendToRuby();
				purgeInterval = lookupOIDRetieveEname(oid).getPurgeInterval();
			}   
			//alarm.setTunable(lookupOIDRetieveEname(oid));

			if(sendToRuby != null) {  
				if (log.isTraceEnabled()) {
					log.trace("extractOIDAndTunableLookup() [SEND_TO_RUBY for OID =" + oid + " is ]=" + sendToRuby);
				}
				alarm.setCustomFieldValue(NetcoolFields.SEND_TO_RUBY, sendToRuby); 
			}
			if (tunable != null) {
				alarm.setCustomFieldValue(NetcoolFields.EVENT_KEY, tunable);
				if (log.isTraceEnabled()) {
					log.trace("extractOIDAndTunableLookup() [OID=" + oid + "]=" + alarm.getCustomFieldValue(NetcoolFields.EVENT_KEY));
				}
			} else {
				if (log.isTraceEnabled()) {
					log.trace("extractOIDAndTunableLookup() [F=" + OID2EN_CONFIG_XML +
							"][OID=" + oid + "*not retrieved*!");
				}
				// assign a default key (10000/500/1) if device or ME level (PPORT) is in the topology 
				// ATT-UVERSE-DEVICE
				AssignDefaultTunableOrSuppress(alarm);
			}
			if(purgeInterval != null) {
				alarm.setCustomFieldValue(NetcoolFields.PURGE_INTERVAL, String.valueOf(Long.valueOf(purgeInterval)*1000));       
				if (log.isTraceEnabled()) {
					log.trace("extractOIDAndTunableLookup() [purgetInterval =" + purgeInterval + "]=" + alarm.getCustomFieldValue(NetcoolFields.PURGE_INTERVAL));
				}
			} 
		}  
	}

	private void AssignDefaultTunableOrSuppress(EnrichedPreprocessAlarm alarm) {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "AssignDefaultTunableOrSuppress()");
		}

		String managedObjectInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String managedObjectClass = alarm.getOriginatingManagedEntity().split(" ")[0];

		//String objectType = null;
		//any subclass PPORT, LPORT, CARD, or SLOT 
		if(!managedObjectClass.equals(DEVICE)) {
			// if found then assign the default tunable
			if(alarm.getAlarmTargetExist()) {
				if (log.isTraceEnabled()) {
					log.trace("Subclass was found, assign the default tunable: " + managedObjectClass);
				}
				//alarm.setTunable(DEFAULT_EVENT_KEY);
				alarm.setCustomFieldValue(NetcoolFields.EVENT_KEY, DEFAULT_EVENT_KEY);
				//alarm.setNodeType(objectType);
			} else {
				if (log.isTraceEnabled()) {
					log.trace("Subclass was not found, now check device level.");
				}
				// if the device level node was found assign the default tunable and change the 
				//MO classes to ATT-UVERSE-DEVICE, set operations as true
				if(alarm.getDeviceLevelExist()) {
					if (log.isTraceEnabled()) {
						log.trace("Device was found, assign the default tunable, and change ME.");
					}
					//alarm.setTunable(DEFAULT_EVENT_KEY);
					alarm.setCustomFieldValue(NetcoolFields.EVENT_KEY, DEFAULT_EVENT_KEY);
					alarm.setOperational(true); 
					alarm.setOriginatingManagedEntity("ATT-UVERSE-DEVICE " + managedObjectInstance);
				} else { 
					if (log.isTraceEnabled()) {
						log.trace("Device was not found, suppress alarm");
					}
					// the device was not found, then suppress
					alarm.setSuppressed(true);
				}
			}
		} else {
			log.trace("This is a device level alarm...");
			// if found in topo assign the default tunable and change the MO classes to 
			//ATT-UVERSE-DEVICE
			if(alarm.getDeviceLevelExist()) {
				if (log.isTraceEnabled()) {
					log.trace("Device was found, assign the default tunable");
				}
				//alarm.setTunable(DEFAULT_EVENT_KEY);
				alarm.setCustomFieldValue(NetcoolFields.EVENT_KEY, DEFAULT_EVENT_KEY);
			} else {
				if (log.isTraceEnabled()) {
					log.trace("Device was not found, suppress alarm.");
				}
				// the device was not found, then suppress
				alarm.setSuppressed(true);
			}
		}
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "AssignDefaultTunableOrSuppress()");
		}

	}

	public OIDToEname lookupOIDRetieveEname(String oid) {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "lookupOIDRetieveEname() Enter :");
		}

		String result = null;
		OIDToEname oidToEname = new OIDToEname();

		if( tunableProperties == null) { 

			//        	tunableProperties = (TunableProperties) getScenario().getGlobals().get(TUNABLE_BEAN_NAME);
			tunableProperties = (TunableProperties) 
					service_util.retrieveBeanFromContextXml(getScenario(), TUNABLE_BEAN_NAME);
			//        	tunableProperties = (TunableProperties) getScenario()
			//            .getValuePackImpl().getApplicationContext()   
			//            .getBean(TUNABLE_BEAN_NAME); 
		}
		if (tunableProperties != null)
			oidToEname = tunableProperties.getHashOIDToEname().get(oid);  

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "lookupOIDRetieveEname() - Lookup result:" + result);
		}
		return oidToEname; 
	}

	public PurgeInterval lookupPurgeInterval() {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "lookupPurgeInterval() Enter :");
		}
		PurgeInterval purgeInterval  = new PurgeInterval();
		if( purgeIntervalProperties == null) {
			purgeIntervalProperties = (PurgeIntervalProperties)service_util.retrieveBeanFromContextXml(getScenario(), PURGEINTERVAL_BEAN_NAME);
			//        	purgeIntervalProperties = (PurgeIntervalProperties) 
			//                      getScenario().getProblems().getMyApplicationContext().getBean(PURGEINTERVAL_BEAN_NAME);
		}
		if(purgeIntervalProperties != null)
			purgeInterval  = purgeIntervalProperties.getHashPurgeInterval().get("ipagPreprocess");

		if (log.isTraceEnabled()) { 
			LogHelper.exit(log, "lookupPurgeInterval() - Lookup clear-purge-interval:" + purgeInterval.getClearPurgeInterval());
		}
		return purgeInterval;     
	}

	/*
	 * Methods regarding suppression by device and severity
	 */
	private boolean SuppressByDeviceAndSeverity(EnrichedPreprocessAlarm alarm) {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "SuppressByDeviceAndSeverity()");
		}

		Boolean res = false;

		if (alarm.getDeviceType() != null) {

			// create the string to search xml file devicetype/severity (CIENA NTE/0)
			String searchString = alarm.getDeviceType() + "/" + alarm.getSeverity();

			if (log.isTraceEnabled()) {
				log.trace("Search string=" + searchString + "#");
			}

			// if found in the xml, set alarm to suppress and return true
			// if not found then return false
			res = lookupDeviceSeveritySuppression(searchString);
			alarm.setSuppressed(res);

		}
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "SuppressByDeviceAndSeverity()");
		}

		return res;
	}

	public Boolean lookupDeviceSeveritySuppression(String searchString) {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "lookupDeviceSeveritySuppression()");
		}

		Boolean bresult = false;
		String sresult = null;
		if( devSevProperties == null) {    
			//        	devSevProperties = (DevTypeAndSeveritySuppressionProperties) getScenario().getGlobals().get(DEVSEV_BEAN_NAME);
			devSevProperties = (DevTypeAndSeveritySuppressionProperties) service_util.retrieveBeanFromContextXml(getScenario(), DEVSEV_BEAN_NAME);
		}		    
		if (devSevProperties != null) 
			sresult = devSevProperties.getHashDevTypeAndSeverityCheck().get(searchString);

		if(sresult != null) {
			if(sresult.equals("true"))
				bresult=true;
		}

		if (log.isTraceEnabled()) {  
			LogHelper.exit(log, "lookupDeviceSeveritySuppression() - Lookup result:" + sresult);
		}
		return bresult;
	}

	public String getLagIdFromReasonCode(String reasonCode, EnrichedPreprocessAlarm alarm) {
		String lagId = null;

		if(reasonCode.startsWith("ae"))
			lagId = reasonCode;

		// does the lagId contain the index, if so drop it off
		if(lagId != null && lagId.contains("_"))
			lagId = lagId.substring(0, lagId.indexOf("_"));

		// this lag is on a sub interface so drop everything after the .
		if(lagId != null && lagId.contains(".")) {
			lagId = lagId.substring(0, lagId.indexOf("."));
		}
		return lagId;
	}


	public void updateBgpMonAlarm (EnrichedPreprocessAlarm enrichedAlarm) {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "updateBgpMonAlarm()");
		}

		String origReason = enrichedAlarm.getCustomFieldValue(NetcoolFields.REASON);

		String[] reasonInfo2And3 = origReason.split("\\;");

		String numberRoutes = ""; String addInfo2 = ""; String addInfo3 = "";

		try {
			numberRoutes = reasonInfo2And3[3];
			addInfo2 = reasonInfo2And3[4];
			addInfo3 = reasonInfo2And3[5];

		} catch (ArrayIndexOutOfBoundsException e) {
			if (log.isTraceEnabled()) {
				log.trace("ArrayIndexOutOfBoundsException Error while parsing out received reason text: " + origReason + "|" +
						enrichedAlarm.getIdentifier() + "|{" + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}");
				log.trace("updateBgpMonAlarm: ArrayIndexOutOfBoundsException ERROR:"
						+ Arrays.toString(e.getStackTrace()));
			}
			e.printStackTrace();
			return;
		} catch (Exception e) {
			if (log.isTraceEnabled()) {
				log.trace("Error while parsing out received reason text: " + origReason + "|" +
						enrichedAlarm.getIdentifier() + "|{" + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}");
				log.trace("updateBgpMonAlarm: Exception ERROR:"
						+ Arrays.toString(e.getStackTrace()));
			}
			e.printStackTrace();
			return;
		}

		if (log.isTraceEnabled()) {
			log.trace("field 4 from reason field: " + numberRoutes + "|" +
					enrichedAlarm.getIdentifier() + "|{" + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}");
		}

		Matcher m;

		m = Pattern.compile ( "NumberRoutes=(\\d+).*" ).matcher ( numberRoutes ); 

		if ( m.find() ) { 
			enrichedAlarm.setCustomFieldValue(NetcoolFields.REASON, "BGPRoute "+ m.group(1) + " " + enrichedAlarm.getCustomFieldValue(NetcoolFields.REASON_CODE));
			if (log.isTraceEnabled()) {
				log.trace("number of routes -> " + m.group(1) + "|" +
						enrichedAlarm.getIdentifier() + "|{" + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}");
			}
		} 

		if (log.isTraceEnabled()) {
			log.trace("field 5 from reason field " + addInfo2 + "|" +
					enrichedAlarm.getIdentifier() + "|{" + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}");
		}

		String[] info2Fields = addInfo2.split("\\s");

		try {

			for (String infoField : info2Fields ) {
				m = Pattern.compile ( "(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}).*" ).matcher ( infoField );
				if ( m.find() ) { 
					String ipToReplace = m.group(1);
					String ptnii = IpagTopoAccess.getInstance().getPtniiByLB10(ipToReplace, "PE");

					if ( null == ptnii || ptnii.isEmpty() ) {
						if (log.isTraceEnabled()) {
							log.trace("IP address: " + ipToReplace + " cannot be resolved to ptnii|" +
									enrichedAlarm.getIdentifier() + "|{" + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}");
						}
					} else {
						if (log.isTraceEnabled()) {
							log.trace("IP address: " + ipToReplace + " resolved to ptnii: " + ptnii + "|" +
									enrichedAlarm.getIdentifier() + "|{" + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}");
						}
						addInfo2 = addInfo2.replaceAll(ipToReplace, ptnii);
					}
				} 
			}
			
			if( null == enrichedAlarm.getCustomFieldValue(GFPFields.INFO2) || enrichedAlarm.getCustomFieldValue(GFPFields.INFO2).isEmpty()) {
				enrichedAlarm.setCustomFieldValue(GFPFields.INFO2, addInfo2);
			} else {
				enrichedAlarm.setCustomFieldValue(GFPFields.INFO2, enrichedAlarm.getCustomFieldValue(GFPFields.INFO2) + " " + addInfo2);
			}

		} catch (ArrayIndexOutOfBoundsException e) {
			if (log.isTraceEnabled()) {
				log.trace("ArrayIndexOutOfBoundsException Error while parsing out BGPLabel/LDPLoopback from reason text: " + origReason + "|" +
						enrichedAlarm.getIdentifier() + "|{" + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}");
				log.trace("updateBgpMonAlarm: ArrayIndexOutOfBoundsException ERROR:"
						+ Arrays.toString(e.getStackTrace()));
			}
			e.printStackTrace();
			return;
		} catch (Exception e) {
			if (log.isTraceEnabled()) {
				log.trace("Error while parsing out BGPLabel/LDPLoopback from reason text: " + origReason + "|" +
						enrichedAlarm.getIdentifier() + "|{" + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}");
				log.trace("updateBgpMonAlarm: Exception ERROR:"
						+ Arrays.toString(e.getStackTrace()));
			}
			e.printStackTrace();
			return;
		}
		
		if (log.isTraceEnabled()) {
			log.trace("field 6 from reason field " + addInfo3 + "|" +
					enrichedAlarm.getIdentifier() + "|{" + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}");
		}

		String[] info3Fields = addInfo3.split("\\,");

		try {

			for (String infoField : info3Fields ) {
				m = Pattern.compile ( "(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}).*" ).matcher ( infoField );
				
				if ( m.find() ) { 
					String ipToReplace = m.group(1);
					String ptnii = IpagTopoAccess.getInstance().getPtnii(ipToReplace, "PE");

					if ( null == ptnii || ptnii.isEmpty() ) {
						if (log.isTraceEnabled()) {
							log.trace("IP address: " + ipToReplace + " cannot be resolved to ptnii|" +
									enrichedAlarm.getIdentifier() + "|{" + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}");
						}
					} else {
						if (log.isTraceEnabled()) {
							log.trace("IP address: " + ipToReplace + " resolved to ptnii: " + ptnii + "|" +
									enrichedAlarm.getIdentifier() + "|{" + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}");
						}
						addInfo3 = addInfo3.replaceAll(ipToReplace, ptnii);
					}
				} 
			}
			
			if( null == enrichedAlarm.getCustomFieldValue(GFPFields.INFO3) || enrichedAlarm.getCustomFieldValue(GFPFields.INFO3).isEmpty()) {
				enrichedAlarm.setCustomFieldValue(GFPFields.INFO3, addInfo3);
			} else {
				enrichedAlarm.setCustomFieldValue(GFPFields.INFO3, enrichedAlarm.getCustomFieldValue(GFPFields.INFO3) + " " + addInfo3);
			}


		} catch (ArrayIndexOutOfBoundsException e) {
			if (log.isTraceEnabled()) {
				log.trace("ArrayIndexOutOfBoundsException Error while parsing out NextHop info from reason text: " + origReason + "|" +
						enrichedAlarm.getIdentifier() + "|{" + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}");
				log.trace("updateBgpMonAlarm: ArrayIndexOutOfBoundsException ERROR:"
						+ Arrays.toString(e.getStackTrace()));
			}
			e.printStackTrace();
			return;
		} catch (Exception e) {
			if (log.isTraceEnabled()) {
				log.trace("Error while parsing out NextHop info from reason text: " + origReason + "|" +
						enrichedAlarm.getIdentifier() + "|{" + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}");
				log.trace("updateBgpMonAlarm: Exception ERROR:"
						+ Arrays.toString(e.getStackTrace()));
			}
			e.printStackTrace();
			return;
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "updateBgpMonAlarm()");
		}
	}





	/**
	 * The Netcool system will never send AVC & SC. So we need to simulate the
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
	public boolean onUpdateSpecificFieldsFromAlarm(Alarm newIncAlarm,
			AlarmCommon alarmInWorkingMemory) { 

		EnrichedAlarm newAlarm = new EnrichedAlarm(newIncAlarm);

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "onUpdateSpecificFieldsFromAlarm()", newAlarm.getIdentifier());
		}  
		if (log.isInfoEnabled()) {
			log.info("New ALARM : " + newAlarm.toXMLString());			 
			log.info("ALARM in WM : " + alarmInWorkingMemory.toXMLString()); 			 
		} 
		boolean ret = false;
		if (alarmInWorkingMemory instanceof EnrichedPreprocessAlarm) {
			log.trace("onUpdateSpecificFieldsFromAlarm alarm in WM is instance of EnrichedAlarm");
			EnrichedPreprocessAlarm alarmInWM = (EnrichedPreprocessAlarm) alarmInWorkingMemory;

			List<AttributeChange> attributeChangesSC = new ArrayList<AttributeChange>(); 
			List<AttributeChange> attributeChangesAVC = new ArrayList<AttributeChange>();
			List<AttributeChange> attributeChangesNewSC = new ArrayList<AttributeChange>();
			AttributeChange attributeChange = null;
			if(newAlarm.getCustomFieldValue(NetcoolFields.FE_TIME) == null) {
				if (log.isTraceEnabled()) {
					log.trace("onUpdateSpecificFieldsFromAlarm FE_TIME of new Alarm is NULL setting to current time");
				}
				newAlarm.setCustomFieldValue(NetcoolFields.FE_TIME, String.valueOf(System.currentTimeMillis()/1000));   
			}
			long feTimeOfNewAlarm = Long.parseLong(newAlarm.getCustomFieldValue(NetcoolFields.FE_TIME));
			if(alarmInWM.getCustomFieldValue(NetcoolFields.FE_TIME) == null) {
				if (log.isTraceEnabled()) {
					log.trace("onUpdateSpecificFieldsFromAlarm FE_TIME of alarmInWM is NULL setting to 0");
				}
				alarmInWM.setCustomFieldValue(NetcoolFields.FE_TIME, "0"); 
			}    
			long feTimeOfAlarminWM = Long.parseLong(alarmInWM.getCustomFieldValue(NetcoolFields.FE_TIME));  

			/*  
			 * Updating the Perceived Severity of the alarm in Working memory
			 * only if the Alarm received is different.
			 */
			if(newAlarm.getPerceivedSeverity() == PerceivedSeverity.CLEAR) {
				if (log.isTraceEnabled()) {
					log.trace("onUpdateSpecificFieldsFromAlarm alarm severity clear");
				}
				if ( alarmInWM.isPassthru() ) {
					newAlarm.setPassthru(true);
				}

				if((feTimeOfNewAlarm >= feTimeOfAlarminWM)) {
					if (log.isTraceEnabled()) {
						log.trace("onUpdateSpecificFieldsFromAlarm alarm fetime is greater clear");
					}
					String tunable = alarmInWM.getCustomFieldValue(NetcoolFields.EVENT_KEY);
					String sendToRuby = alarmInWM.getCustomFieldValue(NetcoolFields.SEND_TO_RUBY);
					if(tunable == null)
						tunable = DEFAULT_EVENT_KEY;
					if(sendToRuby == null)   
						sendToRuby = "true";
					//TODO:  If the alarm in WM is not state=sent or is suppressed
					if(alarmInWM.getNetworkState() == NetworkState.NOT_CLEARED) { 
						//alarmInWM.get
						//Drop, do not process the clear alarm
						if (log.isTraceEnabled()) {
							log.trace("onUpdateSpecificFieldsFromAlarm alarm state is not cleared clear");
						}
						if(!(alarmInWM.getAlarmState().equals(com.att.gfp.data.ipagAlarm.AlarmState.sent)) ||  
								alarmInWM.isSuppressed()) {  
							if (log.isTraceEnabled()) {
								log.trace("onUpdateSpecificFieldsFromAlarm alarm state is not sent or suppressed clear");
							}
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
							newAlarm.setCustomFieldValue(NetcoolFields.LAST_CLEAR_TIME, String.valueOf(System.currentTimeMillis()/1000));
							newAlarm.setCustomFieldValue(NetcoolFields.BE_ALARM_TIME_STAMP, alarmInWM.getCustomFieldValue(NetcoolFields.BE_ALARM_TIME_STAMP));
							newAlarm.setCustomFieldValue(NetcoolFields.PURGE_INTERVAL, alarmInWM.getCustomFieldValue(NetcoolFields.PURGE_INTERVAL));
							newAlarm.setCustomFieldValue(NetcoolFields.CLASSIFICATION, alarmInWM.getCustomFieldValue(NetcoolFields.CLASSIFICATION));
							newAlarm.setCustomFieldValue(NetcoolFields.DOMAIN, alarmInWM.getCustomFieldValue(NetcoolFields.DOMAIN));
							newAlarm.setCustomFieldValue(NetcoolFields.CLCI, alarmInWM.getCustomFieldValue(NetcoolFields.CLCI));     
							newAlarm.setCustomFieldValue(NetcoolFields.CLFI, alarmInWM.getCustomFieldValue(NetcoolFields.CLFI));
							newAlarm.setCustomFieldValue(NetcoolFields.CLLI, alarmInWM.getCustomFieldValue(NetcoolFields.CLLI));
							newAlarm.setCustomFieldValue(NetcoolFields.SEND_TO_RUBY, alarmInWM.getCustomFieldValue(NetcoolFields.SEND_TO_RUBY));
							newAlarm.setCustomFieldValue(NetcoolFields.AGING, alarmInWM.getCustomFieldValue(NetcoolFields.AGING));
							newAlarm.setCustomFieldValue(NetcoolFields.NODE_NAME, alarmInWM.getCustomFieldValue(NetcoolFields.NODE_NAME));
							newAlarm.setCustomFieldValue(NetcoolFields.SM_SOURCEDOMAIN, alarmInWM.getCustomFieldValue(NetcoolFields.SM_SOURCEDOMAIN));
							newAlarm.setCustomFieldValue(NetcoolFields.SM_CLASS, alarmInWM.getCustomFieldValue(NetcoolFields.SM_CLASS));
							newAlarm.setCustomFieldValue(NetcoolFields.BMP_CLCI, alarmInWM.getCustomFieldValue(NetcoolFields.BMP_CLCI));
							newAlarm.setCustomFieldValue(NetcoolFields.INFO1, alarmInWM.getCustomFieldValue(NetcoolFields.INFO1));
							newAlarm.setCustomFieldValue(NetcoolFields.INFO2, alarmInWM.getCustomFieldValue(NetcoolFields.INFO2));
							newAlarm.setCustomFieldValue(NetcoolFields.INFO3, alarmInWM.getCustomFieldValue(NetcoolFields.INFO3));
							newAlarm.setCustomFieldValue(NetcoolFields.INFO, alarmInWM.getCustomFieldValue(NetcoolFields.INFO));
							newAlarm.setCustomFieldValue(NetcoolFields.EVC_NAME, alarmInWM.getCustomFieldValue(NetcoolFields.EVC_NAME));
							newAlarm.setCustomFieldValue(NetcoolFields.CIRCUIT_ID, alarmInWM.getCustomFieldValue(NetcoolFields.CIRCUIT_ID));
							newAlarm.setCustomFieldValue(NetcoolFields.ACNABAN, alarmInWM.getCustomFieldValue(NetcoolFields.ACNABAN));
							newAlarm.setCustomFieldValue(NetcoolFields.SEC_ALARM_TIME, alarmInWM.getCustomFieldValue(NetcoolFields.SEC_ALARM_TIME));
							newAlarm.setCustomFieldValue(NetcoolFields.ALERT_ID, alarmInWM.getCustomFieldValue(NetcoolFields.ALERT_ID));
							newAlarm.setCustomFieldValue(NetcoolFields.SM_EVENT, alarmInWM.getCustomFieldValue(NetcoolFields.SM_EVENT)); 
							newAlarm.setCustomFieldValue(NetcoolFields.MCN, alarmInWM.getCustomFieldValue(NetcoolFields.MCN));
							newAlarm.setCustomFieldValue(NetcoolFields.SM_EVENT_TEXT, alarmInWM.getCustomFieldValue(NetcoolFields.SM_EVENT_TEXT));
							newAlarm.setCustomFieldValue(NetcoolFields.EVENT_KEY, alarmInWM.getCustomFieldValue(NetcoolFields.EVENT_KEY));
							newAlarm.setCustomFieldValue(NetcoolFields.COMPONENT, alarmInWM.getCustomFieldValue(NetcoolFields.COMPONENT)); 
							newAlarm.setCustomFieldValue(NetcoolFields.REASON, alarmInWM.getCustomFieldValue(NetcoolFields.REASON));
							newAlarm.setCustomFieldValue(NetcoolFields.REASON_CODE, alarmInWM.getCustomFieldValue(NetcoolFields.REASON_CODE));
							if (log.isTraceEnabled()) {
								log.trace("onUpdateSpecificFieldsFromAlarm setting the probableCause");
							}
							newAlarm.setProbableCause(alarmInWM.getProbableCause());      
							if (log.isTraceEnabled()) {
								log.trace("onUpdateSpecificFieldsFromAlarm sending new alarm clear");
							}
							//service_util.sendAlarm(getScenario(), new EnrichedAlarm(newAlarm), alarmInWM.getCustomFieldValue(NetcoolFields.EVENT_KEY)); 
							service_util.sendAlarm(getScenario(), newAlarm, alarmInWM.getCustomFieldValue(NetcoolFields.EVENT_KEY)); 
							attributeChange = new AttributeChange();
							attributeChange.setName(StandardFields.NETWORK_STATE); 
							attributeChange
							.setNewValue(NetworkState.CLEARED.toString());  
							attributeChange.setOldValue(alarmInWM.getNetworkState()
									.toString());
							attributeChangesSC.add(attributeChange);   
						}

					} else {
						if (log.isTraceEnabled()) {
							log.trace("onUpdateSpecificFieldsFromAlarm RETRACTING new alarm CLEAR");
						}

						attributeChange = new AttributeChange();
						attributeChange.setName(StandardFields.NETWORK_STATE);
						attributeChange
						.setNewValue(NetworkState.CLEARED.toString());
						attributeChange.setOldValue(newAlarm.getNetworkState()
								.toString());    
						attributeChangesNewSC.add(attributeChange);     
					}
				} else {
					//suppress clear.
					if (log.isTraceEnabled()) {
						log.trace("onUpdateSpecificFieldsFromAlarm RETRACTING new alarm CLEAR");
					}

					attributeChange = new AttributeChange();
					attributeChange.setName(StandardFields.NETWORK_STATE);
					attributeChange
					.setNewValue(NetworkState.CLEARED.toString());
					attributeChange.setOldValue(newAlarm.getNetworkState()
							.toString());    
					attributeChangesNewSC.add(attributeChange);     
				}  
			}else {

				/*
				 * 
				 */
				if (log.isTraceEnabled()) {
					log.trace("onUpdateSpecificFieldsFromAlarm alarm severity NOT clear");
				}

				if(newAlarm.getCustomFieldValue(NetcoolFields.FE_TIME).equals(alarmInWM.getCustomFieldValue(NetcoolFields.FE_TIME))
						&&	newAlarm.getPerceivedSeverity().equals(alarmInWM.getPerceivedSeverity())) {
					//log this alarm
					//TODO: drop alarm do not process.
					// code below does not retract the newAlarm from WM since newAlarm has not been added yet to WM
					// code below effectively doesn't do much; alarm is dropped
					if (log.isTraceEnabled()) {
						log.trace("onUpdateSpecificFieldsFromAlarm fetime equals DUPLICATE");
						log.trace("onUpdateSpecificFieldsFromAlarm DROPPING new alarm DUPLICATE");
					}
					attributeChange = new AttributeChange();
					attributeChange.setName(StandardFields.NETWORK_STATE);
					attributeChange
					.setNewValue(NetworkState.CLEARED.toString());
					attributeChange.setOldValue(newAlarm.getNetworkState()
							.toString());    
					attributeChangesNewSC.add(attributeChange);     
				} else {
					if((feTimeOfNewAlarm > feTimeOfAlarminWM))  {
						if (log.isTraceEnabled()) {
							log.trace("onUpdateSpecificFieldsFromAlarm fetime is greater than DUPLICATE");
							log.trace("onUpdateSpecificFieldsFromAlarm Processing the new Duplicate alarm");
						}

						if ( (alarmInWM.getCustomFieldValue(NetcoolFields.EVENT_KEY).equals("50004/1/34")) &&
								(feTimeOfNewAlarm <= (feTimeOfAlarminWM + 3600)) ) {

							log.trace("onUpdateSpecificFieldsFromAlarm DROPPING 50004/1/34 since it occured within 60 minutes of a pre-existing occurence. "
									+ "Seq Num : " + newAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + " Seq Num of pre-existing alarm: " +
									alarmInWM.getCustomFieldValue(GFPFields.SEQNUMBER));

						} else {
							EnrichedPreprocessAlarm duplicateAlarm = new EnrichedPreprocessAlarm(alarmInWM);
							duplicateAlarm.setCustomFieldValue(NetcoolFields.FE_TIME, newAlarm.getCustomFieldValue(NetcoolFields.FE_TIME)); 
							duplicateAlarm.setCustomFieldValue(NetcoolFields.BE_ALARM_TIME_STAMP, newAlarm.getCustomFieldValue(NetcoolFields.BE_ALARM_TIME_STAMP)); 
							duplicateAlarm.setPerceivedSeverity(newAlarm.getPerceivedSeverity());
							duplicateAlarm.setAlarmRaisedTime(newAlarm.getAlarmRaisedTime());
							duplicateAlarm.setCustomFieldValue(GFPFields.SEQNUMBER, newAlarm.getCustomFieldValue(GFPFields.SEQNUMBER)); 
							duplicateAlarm.setCustomFieldValue(GFPFields.COMPONENT, newAlarm.getCustomFieldValue(GFPFields.COMPONENT));
							duplicateAlarm.setCustomFieldValue(GFPFields.REASON, newAlarm.getCustomFieldValue(GFPFields.REASON));
							duplicateAlarm.setCustomFieldValue(NetcoolFields.REASON_CODE, newAlarm.getCustomFieldValue(NetcoolFields.REASON_CODE));
							String managedObjectInstance = newAlarm.getOriginatingManagedEntity().split(" ")[1];
							duplicateAlarm.setCustomFieldValue(NetcoolFields.ALERT_ID, newAlarm.getCustomFieldValue(GFPFields.EVENT_KEY) + "-" 
									+ managedObjectInstance + "-" + newAlarm.getCustomFieldValue(GFPFields.REASON_CODE));
							//						enrichedNewAlarm.setIdentifier(newAlarm.getIdentifier() + "-DuplicateID-" + String.valueOf(new Random().nextInt())); 
							alarmInWM.setCustomFieldValue(NetcoolFields.BE_ALARM_TIME_STAMP, String.valueOf(System.currentTimeMillis()/1000));
							alarmInWM.setCustomFieldValue(NetcoolFields.FE_TIME, String.valueOf(System.currentTimeMillis()/1000));
							alarmInWM.setPerceivedSeverity(PerceivedSeverity.CLEAR);
							alarmInWM.setCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA, "YES");  
							service_util.sendAlarm(getScenario(), new EnrichedAlarm(alarmInWM), alarmInWM.getCustomFieldValue(NetcoolFields.EVENT_KEY));						
							service_util.sendAlarm(getScenario(), duplicateAlarm, alarmInWM.getCustomFieldValue(NetcoolFields.EVENT_KEY));
							alarmInWM.setCustomFieldValue(NetcoolFields.FE_TIME, newAlarm.getCustomFieldValue(NetcoolFields.FE_TIME)); 
							alarmInWM.setCustomFieldValue(NetcoolFields.BE_ALARM_TIME_STAMP, newAlarm.getCustomFieldValue(NetcoolFields.BE_ALARM_TIME_STAMP)); 
							alarmInWM.setPerceivedSeverity(newAlarm.getPerceivedSeverity());
							alarmInWM.setAlarmRaisedTime(newAlarm.getAlarmRaisedTime());
							alarmInWM.setCustomFieldValue(GFPFields.SEQNUMBER, newAlarm.getCustomFieldValue(GFPFields.SEQNUMBER)); 
							alarmInWM.setCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA, "");    

							//						getScenario().getSession().insert(duplicateAlarm);  
							//						attributeChange = new AttributeChange();
							//						attributeChange.setName(StandardFields.NETWORK_STATE); 
							//						attributeChange
							//								.setNewValue(NetworkState.CLEARED.toString());  
							//						attributeChange.setOldValue(alarmInWM.getNetworkState()
							//								.toString()); 
							//						attributeChangesSC.add(attributeChange);   
						}
					}
				}
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
	}  

}
