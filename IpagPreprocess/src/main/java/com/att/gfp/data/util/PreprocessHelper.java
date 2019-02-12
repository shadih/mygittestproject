package com.att.gfp.data.util;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.config.IpagXmlConfiguration;
import com.att.gfp.data.config.IpagPolicies.CpeCdcAlarms.CpeCdcAlarm;
import com.att.gfp.data.config.IpagPolicies.SuperEmuxDevices.SuperEmux;
import com.att.gfp.data.ipag.topoModel.IpagTopoAccess;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.data.ipagPreprocess.preprocess.EnrichedPreprocessAlarm;
import com.att.gfp.helper.GFPFields;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.scenario.ScenarioThreadLocal;



public final class PreprocessHelper {
	private static Logger log = LoggerFactory.getLogger(PreprocessHelper.class);
	 
	private PreprocessHelper(){
		throw new AssertionError("PreprocessHelper():Not to be instantiated");
	} 

	public static void processCpeCdcAlarms(EnrichedPreprocessAlarm alarm) {
		if (log.isTraceEnabled()) {
			log.trace("processCpeCdcAlarm enter : " + alarm.getIdentifier());
		}
		String processingType = "";
		Scenario scenario = ScenarioThreadLocal.getScenario();
		IpagXmlConfiguration ipagXmlConfig = (IpagXmlConfiguration) service_util.retrieveBeanFromContextXml(scenario, "ipagXmlConfig");
		List<String> cpeCdcEvents = ipagXmlConfig.getIpagPolicies().getCpeCdcAlarms().getEventNames().getEventName();
		if(cpeCdcEvents.contains(alarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
			CpeCdcAlarm cpeCdcAlarm = ipagXmlConfig.getCpeCdeAlarmMap().get(alarm.getCustomFieldValue(GFPFields.EVENT_KEY));
			if(cpeCdcAlarm != null) {
				processingType = cpeCdcAlarm.getProcessingType();
			}
		}   
		if("CienaPortAlarm".equalsIgnoreCase(processingType)) {
			processCienaPortAlarms(alarm);
		}
		if("CienaCFMAlarm".equalsIgnoreCase(processingType)) {
			processCienaCFMAlarms(alarm);
		}
		if("CienaDeviceAlarm".equalsIgnoreCase(processingType)) {
			processCienaDeviceAlarms(alarm);
		}
		if("AdtranPortAlarm".equalsIgnoreCase(processingType)) {
		processAdtranPortAlarms(alarm);
		}
		if("AdtranCFMAlarm".equalsIgnoreCase(processingType)) {
		processAdtranCFMAlarms(alarm);
		}
		if("AdtranDeviceAlarm".equalsIgnoreCase(processingType)) {
		processAdtranDeviceAlarms(alarm);
		}
		if("JuniperPortAlarm".equalsIgnoreCase(processingType)) {
		processJuniperPortAlarms(alarm);
		}
		if("JuniperCardOrSlotAlarm".equalsIgnoreCase(processingType)) {
			if("CARD".equalsIgnoreCase(alarm.getOrigMEClass())) {
				processJuniperCardAlarms(alarm); 
			}
			if("SLOT".equalsIgnoreCase(alarm.getOrigMEClass())) {
				processJuniperSlotAlarms(alarm);
			}
		}
		if("JuniperDeviceAlarm".equalsIgnoreCase(processingType)) {
			processJuniperDeviceAlarms(alarm);
		}
		if("CiscoEGSPortAlarm".equalsIgnoreCase(processingType)) {
			processJCiscoEGSPortAlarm(alarm);
		} 
		
	}

	private static void processJCiscoEGSPortAlarm(EnrichedPreprocessAlarm alarm) {
			if(alarm.getHasMis()) { 
				alarm.setIssendToCpeCdc(true);
			} 
	}
	
	private static void processJuniperDeviceAlarms(EnrichedPreprocessAlarm alarm) { 
		if("DEVICE".equalsIgnoreCase(alarm.getOrigMEClass())) {
			if("JUNIPER MX SERIES".equalsIgnoreCase(alarm.getDeviceType()) || 
			   "VR1".equalsIgnoreCase(alarm.getDeviceType()) ||
			   "NV1".equalsIgnoreCase(alarm.getDeviceType()) ||
			   "NV2".equalsIgnoreCase(alarm.getDeviceType()) ||
			   "NV3".equalsIgnoreCase(alarm.getDeviceType())
			   ) { 
				IpagTopoAccess.getInstance().findAdditionalJuniperDeviceClfis(alarm);
			}	
		}
		
	}
	
	private static void processJuniperSlotAlarms(EnrichedPreprocessAlarm alarm) {
		if("JUNIPER MX SERIES".equalsIgnoreCase(alarm.getDeviceType()) || 
		   "VR1".equalsIgnoreCase(alarm.getDeviceType()) ||
		   "NV1".equalsIgnoreCase(alarm.getDeviceType()) ||
		   "NV2".equalsIgnoreCase(alarm.getDeviceType()) ||
		   "NV3".equalsIgnoreCase(alarm.getDeviceType())
		   ) { 
			IpagTopoAccess.getInstance().findAdditionalJuniperSlotClfis(alarm);
			 
		}		
	}

	private static void processJuniperCardAlarms(EnrichedPreprocessAlarm alarm) {
		if("JUNIPER MX SERIES".equalsIgnoreCase(alarm.getDeviceType()) || 
		   "VR1".equalsIgnoreCase(alarm.getDeviceType()) ||
		   "NV1".equalsIgnoreCase(alarm.getDeviceType()) ||
		   "NV2".equalsIgnoreCase(alarm.getDeviceType()) ||
		   "NV3".equalsIgnoreCase(alarm.getDeviceType())
		   ) { 
			IpagTopoAccess.getInstance().findAdditionalJuniperCardClfis(alarm);
			
		}		
	}

	private static void processJuniperPortAlarms(EnrichedPreprocessAlarm alarm) {
		if("JUNIPER MX SERIES".equalsIgnoreCase(alarm.getDeviceType())) {
			if("PPORT".equalsIgnoreCase(alarm.getOrigMEClass())) {
				if("CIENA NTE".equalsIgnoreCase(alarm.getRemoteDeviceType()) && alarm.getHasMis()) {
					alarm.setIssendToCpeCdc(true);
				} 
			}
			if("LPORT".equalsIgnoreCase(alarm.getOrigMEClass())) {
				if("MIS".equalsIgnoreCase(alarm.getlPortScpService()) && "CIENA NTE".equalsIgnoreCase(alarm.getCorrespondingPPortRemoteDeviceType())) {
					alarm.setIssendToCpeCdc(true); 
				}
			}
		}
		if("VR1".equalsIgnoreCase(alarm.getDeviceType())) {
			if("PPORT".equalsIgnoreCase(alarm.getOrigMEClass())) {
				if(alarm.getHasMis()) {
					alarm.setIssendToCpeCdc(true);
				}
 			}			
			if("LPORT".equalsIgnoreCase(alarm.getOrigMEClass())) {
				if("MIS".equalsIgnoreCase(alarm.getLportServiceName())) {
					alarm.setIssendToCpeCdc(true);
				}
			}
		}
		
	}

	private static void processAdtranDeviceAlarms(EnrichedPreprocessAlarm alarm) {
		if("DEVICE".equalsIgnoreCase(alarm.getOrigMEClass())) {
			if(alarm.getHasMis()) {
				alarm.setIssendToCpeCdc(true);
			}
			if("ADTRAN 800 SERIES".equalsIgnoreCase(alarm.getDeviceType())) {
				if(alarm.getHasMis()) {  
					findAdditionalCienaClfiInfo(alarm);
				}
			}
			if("ADTRAN 5000 SERIES".equalsIgnoreCase(alarm.getDeviceType())) {
					IpagTopoAccess.getInstance().findAdditionalCienaEmuxClfis(alarm);
			}
		}
	}

	private static void processAdtranCFMAlarms(EnrichedPreprocessAlarm alarm) {
		if(alarm.getHasMis()) {
			alarm.setIssendToCpeCdc(true); 
		}
	
	}

	private static void processAdtranPortAlarms(EnrichedPreprocessAlarm alarm) {
		log.trace("processAdtranPortalarms enter " + alarm.getIdentifier());
		if("PPORT".equalsIgnoreCase(alarm.getOrigMEClass())) {
			if("ADTRAN 800 SERIES".equalsIgnoreCase(alarm.getDeviceType())) {
				if(alarm.getHasMis()) {
					alarm.setIssendToCpeCdc(true);
				}
			}
			if("ADTRAN 5000 SERIES".equalsIgnoreCase(alarm.getDeviceType())
					&& (!"JUNIPER MX SERIES".equalsIgnoreCase(alarm.getRemoteDeviceType()))
					&& alarm.getRemotePPortHasMis()) { 
				alarm.setIssendToCpeCdc(true);
			}
		}
	}

	private static void processCienaDeviceAlarms(EnrichedPreprocessAlarm alarm) {	
		if("DEVICE".equalsIgnoreCase(alarm.getOrigMEClass())) {
			if(alarm.getHasMis()) {
				alarm.setIssendToCpeCdc(true);
			}
			if("CIENA NTE".equalsIgnoreCase(alarm.getDeviceType())) {
				findAdditionalCienaClfiInfo(alarm);
			}
			if("CIENA EMUX".equalsIgnoreCase(alarm.getDeviceType())) {
				IpagTopoAccess.getInstance().findAdditionalCienaEmuxClfis(alarm); 
			}
		}
	}

	private static void findAdditionalCienaClfiInfo(EnrichedPreprocessAlarm alarm) {
		IpagTopoAccess.getInstance().findAdditionalCienaClfis(alarm);
	}

	private static void processCienaCFMAlarms(EnrichedPreprocessAlarm alarm) {
		if(alarm.getHasMis()) {
			alarm.setIssendToCpeCdc(true);
		}
	}
 
	private static void processCienaPortAlarms(EnrichedAlarm alarm) {
		if("PPORT".equalsIgnoreCase(alarm.getOrigMEClass())) { 
			if("CIENA NTE".equalsIgnoreCase(alarm.getDeviceType()) && alarm.getHasMis()) {
				alarm.setIssendToCpeCdc(true);
			}
			if("CIENA EMUX".equalsIgnoreCase(alarm.getDeviceType()) && !("JUNIPER MX SERIES".equalsIgnoreCase(alarm.getRemoteDeviceType())) &&
					(alarm.getHasMis() || alarm.getRemotePPortHasMis())) {
				alarm.setIssendToCpeCdc(true);
			}  
		}
	}
	
	public static void updateCdcInfoForCienaCFM(EnrichedPreprocessAlarm alarm) {
		if("50002/100/52".equalsIgnoreCase(alarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
			String managedObjectInstance =alarm.getOriginatingManagedEntity().split(" ")[1];
			alarm.setCustomFieldValue(NetcoolFields.ALERT_ID, alarm.getCustomFieldValue(GFPFields.EVENT_KEY) + "-" + managedObjectInstance + "-" + alarm.getCustomFieldValue(GFPFields.REASON_CODE));
	        String CDCInfo = "CFMAlertKey=<"+alarm.getCustomFieldValue(GFPFields.ALERT_ID)+"-IPAG01> CFMTimeStamp=<"+alarm.getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP)+">";
	        log.info("CDCInfo = " + CDCInfo); 
	        alarm.setCustomFieldValue("cdc-info", CDCInfo);	  
	        IpagTopoAccess.getInstance().updateCDCInfo(alarm.getOriginatingManagedEntity().split(" ")[1], CDCInfo);	 
		}
	}
 		
}
