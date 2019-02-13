package com.att.gfp.helper;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm; 
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.x733alarm.PerceivedSeverity; 
import com.hp.uca.expert.alarm.FileAlarmForwarder;
import com.hp.uca.expert.alarm.JMSAlarmForwarder;
import com.hp.uca.expert.alarm.OpenMediationAlarmForwarder;
import com.hp.uca.expert.alarm.AlarmForwarder;
import com.hp.uca.expert.alarm.exception.AlarmForwarderException;

/**
 * Utility class that provides methods for forwarding alarms to NOM.
 * 
 * @author st133d
 *
 */ 
public class service_util {

	private static AlarmForwarder fileAlarmForwarder;
	private static OpenMediationAlarmForwarder nomAlarmForwarder;
	private static OpenMediationAlarmForwarder mobilityCdcAlarmForwarder;
	private static OpenMediationAlarmForwarder decompsedAlarmForwarder;
	private static OpenMediationAlarmForwarder cpeCdcAlarmForwarder;
	private static OpenMediationAlarmForwarder uverseCdcAlarmForwarder;
	private static JMSAlarmForwarder decomposerJmsAlarmForwarder; 
	private static JMSAlarmForwarder juniperJmsAlarmForwarder; 
	private static JMSAlarmForwarder adtranJmsAlarmForwarder; 
	private static JMSAlarmForwarder cienaJmsAlarmForwarder; 
	private static File alarmFile; 
	
	private static Logger log = LoggerFactory.getLogger(service_util.class);
	
	public service_util() {
		
	}
	
	public static File getfileFromResourceName(String fileName) {
		File configFile = null;
		configFile = new File(fileName);
		return configFile;
	}
	
	public static Object retrieveBeanFromContextXml(Scenario scenario,String beanName) {
		if(scenario == null) {
			log.trace("service_util.retrieveBeanFromContextXml() Scenario is NULL"); 
		}
		ApplicationContext context =   
			scenario.getVPApplicationContext();
	      return context.getBean(beanName);
	}
 
	/**
	 * Forwards the alarm to NOM
	 * @param scenario  
	 * @param alarm    
	 */
	public static void forwardAlarmToNOM(Scenario scenario,Alarm alarm) { 
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "service_util.forwardAlarmToNOM()");
		}
		writeToAlarmForwarderFile(scenario, alarm);
		if(nomAlarmForwarder == null) {
			if(retrieveBeanFromContextXml(scenario, "nomAlarmForwarder") != null) {
				nomAlarmForwarder = (OpenMediationAlarmForwarder) retrieveBeanFromContextXml(scenario, "nomAlarmForwarder");
				log.trace("service_util.forwardAlarmToNOM(): Retrieved nomAlarmForwarder from spring bean : ");
			} 
		}  else {
			log.trace("nomAlarmForwarder exists alredy toString(): " + nomAlarmForwarder.toString());
		}
		  
		if(nomAlarmForwarder != null) {   
				try {   
					// can't do this in JUNIT..sure
					Alarm newAlarm = (Alarm) alarm;
					nomAlarmForwarder.write(alarm);
					log.trace("service_util.forwardAlarmToNOM(): Forwarded alarm to NOM: " + newAlarm.getIdentifier()+ " having Seq Number = " + alarm.getCustomFieldValue(GFPFields.SEQNUMBER));
					log.trace("service_util.forwardAlarmToNOM(): classLoader = " + nomAlarmForwarder.getClass().getClassLoader()); 
				} catch (AlarmForwarderException e) {
					log.trace("service_util.forwardAlarmToNOM(): Exception when forwarding to NOM "); 
					e.printStackTrace();    
					log.error(e.getMessage());   
				}  
		} else {         
			log.trace("service_util.forwardAlarmToNOM(): nomAlarmForwarder is still NULL.");
		}			
		if (log.isTraceEnabled()) { 
			LogHelper.exit(log, "service_util.forwardAlarmToNOM(): forwardAlarmToNOM()");
		}  
		  
	}
	
	/**
	 * Forwards the alarm to NOM
	 * @param scenario  
	 * @param alarm    
	 */
	public static void forwardAlarmToDecompostion(Scenario scenario,Alarm alarm) { 
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "service_util.forwardAlarmToDecompostion()");
		}
		writeToAlarmForwarderFile(scenario,alarm);
		if(decompsedAlarmForwarder == null) { 
			if(retrieveBeanFromContextXml(scenario, GFPFields.DECOMPOSED_ALARMS_BEAN) != null) {
				decompsedAlarmForwarder = (OpenMediationAlarmForwarder) retrieveBeanFromContextXml(scenario, GFPFields.DECOMPOSED_ALARMS_BEAN);
				log.trace("service_util.forwardAlarmToDecompostion(): Retrieved decompsedAlarmForwarder from spring bean : ");
			} 
		}  else {
			log.trace("decompsedAlarmForwarder exists alredy toString(): " + decompsedAlarmForwarder.toString());
		}
		  
		if(decompsedAlarmForwarder != null) {    
				try {   
					Alarm newAlarm = (Alarm) alarm; 
					decompsedAlarmForwarder.write(alarm);
					log.trace("service_util.forwardAlarmToDecompostion(): Forwarded alarm to NOM: " + newAlarm.getIdentifier()+ " having Seq Number = " + alarm.getCustomFieldValue(GFPFields.SEQNUMBER));
					log.trace("service_util.forwardAlarmToDecompostion(): classLoader = " + decompsedAlarmForwarder.getClass().getClassLoader()); 
				} catch (AlarmForwarderException e) {
					log.trace("service_util.forwardAlarmToDecompostion(): Exception when forwarding to decompsedAlarmForwarder "); 
					e.printStackTrace();    
					log.error(e.getMessage());   
				}  
		} else {         
			log.trace("service_util.forwardAlarmToDecompostion(): decompsedAlarmForwarder is still NULL.");
		}			 
		if (log.isTraceEnabled()) { 
			LogHelper.exit(log, "service_util.forwardAlarmToDecompostion(): decompsedAlarmForwarder()");
		}  
		  
	}

	/**
	 * Forwards the alarm to NOM
	 * @param scenario  
	 * @param alarm    
	 */
	public static void forwardAlarmToCpeCDC(Scenario scenario,Alarm alarm) { 
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "service_util.forwardAlarmToCpeCDC()");
		}
		writeToAlarmForwarderFile(scenario,alarm);
		if(cpeCdcAlarmForwarder == null) { 
			if(retrieveBeanFromContextXml(scenario, GFPFields.CPE_CDC_ALARM_FORWARDER_BEAN) != null) {
				cpeCdcAlarmForwarder = (OpenMediationAlarmForwarder) retrieveBeanFromContextXml(scenario, GFPFields.CPE_CDC_ALARM_FORWARDER_BEAN);
				log.trace("service_util.forwardAlarmToCpeCDC(): Retrieved cpeCdcAlarmForwarder from spring bean : ");
			} 
		}  else {
			log.trace("cpeCdcAlarmForwarder exists alredy toString(): " + cpeCdcAlarmForwarder.toString());
		}
		   
		if(cpeCdcAlarmForwarder != null) {    
				try {   
					Alarm newAlarm = (Alarm) alarm; 
					cpeCdcAlarmForwarder.write(alarm);
					log.trace("service_util.forwardAlarmToCpeCDC(): Forwarded alarm to NOM: " + newAlarm.getIdentifier()+ " having Seq Number = " + alarm.getCustomFieldValue(GFPFields.SEQNUMBER));
					log.trace("service_util.forwardAlarmToCpeCDC(): classLoader = " + cpeCdcAlarmForwarder.getClass().getClassLoader()); 
				} catch (AlarmForwarderException e) {
					log.trace("service_util.forwardAlarmToCpeCDC(): Exception when forwarding to decompsedAlarmForwarder "); 
					e.printStackTrace();    
					log.error(e.getMessage());   
				}  
		} else {         
			log.trace("service_util.forwardAlarmToCpeCDC(): cpeCdcAlarmForwarder is still NULL.");
		}			 
		if (log.isTraceEnabled()) { 
			LogHelper.exit(log, "service_util.forwardAlarmToCpeCDC(): cpeCdcAlarmForwarder()");
		}  
		  
	}
	
	/**
	 * Forwards the alarm to Uverse CDC 
	 * @param scenario  
	 * @param alarm    
	 */
	public static void forwardAlarmToUverseCDC(Scenario scenario,Alarm alarm) { 
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "service_util.forwardAlarmToUverseCDC()");
		}
		writeToAlarmForwarderFile(scenario,alarm);
		if(uverseCdcAlarmForwarder == null) { 
			if(retrieveBeanFromContextXml(scenario, GFPFields.UVERSE_CDC_ALARM_FORWARDER_BEAN) != null) {
				uverseCdcAlarmForwarder = (OpenMediationAlarmForwarder) retrieveBeanFromContextXml(scenario, GFPFields.UVERSE_CDC_ALARM_FORWARDER_BEAN);
				log.trace("service_util.forwardAlarmToUverseCDC(): Retrieved uverseCdcAlarmForwarder from spring bean : ");
			} 
		}  else {
			log.trace("cpeCdcAlarmForwarder exists alredy toString(): " + uverseCdcAlarmForwarder.toString());
		}
		   
		if(uverseCdcAlarmForwarder != null) {     
				try {    
					Alarm newAlarm = (Alarm) alarm; 
					uverseCdcAlarmForwarder.write(alarm);
					log.trace("service_util.forwardAlarmToUverseCDC(): Forwarded alarm to NOM: " + newAlarm.getIdentifier()+ " having Seq Number = " + alarm.getCustomFieldValue(GFPFields.SEQNUMBER));
					log.trace("service_util.forwardAlarmToUverseCDC(): classLoader = " + uverseCdcAlarmForwarder.getClass().getClassLoader()); 
				} catch (AlarmForwarderException e) {
					log.trace("service_util.forwardAlarmToUverseCDC(): Exception when forwarding to uverseCdcAlarmForwarder "); 
					e.printStackTrace();    
					log.error(e.getMessage());   
				}  
		} else {         
			log.trace("service_util.forwardAlarmToUverseCDC(): uverseCdcAlarmForwarder is still NULL.");
		}			 
		if (log.isTraceEnabled()) { 
			LogHelper.exit(log, "service_util.forwardAlarmToUverseCDC(): uverseCdcAlarmForwarder()");
		}  
		  
	}
	
	private static void writeToAlarmForwarderFileOrig(Scenario scenario, Alarm alarm) {
		if(fileAlarmForwarder == null) {
			log.trace("service_util.writeToAlarmForwarderFile(): retrieving the file alarm forwarder from spring bean = ");
			if(retrieveBeanFromContextXml(scenario,"alarmForwarder") != null) {
				fileAlarmForwarder = (FileAlarmForwarder) (retrieveBeanFromContextXml(scenario,"alarmForwarder"));
				log.trace("service_util.writeToAlarmForwarderFile(): retrieved file alarm forwarder from spring bean = ");
			}     
		}
		if(fileAlarmForwarder != null) {  
			try { 
				fileAlarmForwarder.write(alarm);
				log.trace("service_util.writeToAlarmForwarderFile(): Wrote alarm to file:" + alarm.getIdentifier() + " having Seq Number = " + alarm.getCustomFieldValue(GFPFields.SEQNUMBER)); 
			} catch (AlarmForwarderException e) {
				log.error(e.getMessage()); 
				e.printStackTrace();      
			} 
		} else {  
			log.trace("service_util.writeToAlarmForwarderFile(): fileAlarmForwarder is still NULL.");
		}  
		
	}
	private static void writeToAlarmForwarderFile(Scenario scenario, Alarm alarm) {
		AlarmFileForwardingUtil.writeToLog(alarm);      
	}

	public static void sendClearAlarmToCascadingVPs(Scenario scenario, Alarm alarm) {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "sendClearAlarmToCascadingVPs()"); 
		}
		alarm.setPerceivedSeverity(PerceivedSeverity.CLEAR);
		alarm.setCustomFieldValue(GFPFields.IS_PURGE_INTERVAL_EXPIRED, "YES");
		//cascade to other VPs   
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "sendClearAlarmToCascadingVPs()");  
		}
	}
	  
	/**
	 * Forwards the alarm to NOM
	 * @param scenario
	 * @param alarm    
	 */
	public static void forwardAlarmToMobilityCDC(Scenario scenario,Alarm alarm) {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "forwardAlarmToMobilityCDC()");
		}
		writeToAlarmForwarderFile(scenario, alarm); 
		if(mobilityCdcAlarmForwarder == null) {
				if(retrieveBeanFromContextXml(scenario,"mobilityCDCAlarmForwarder") != null) {
					mobilityCdcAlarmForwarder = (OpenMediationAlarmForwarder) retrieveBeanFromContextXml(scenario,"mobilityCDCAlarmForwarder");
					log.trace("service_util.forwardAlarmToMobilityCDC(): Retrieved mobilityCDCAlarmForwarder from spring bean : ");
				}
		}  
		if(mobilityCdcAlarmForwarder != null) { 
				try {   
					// can't do this in JUNIT..sure
					Alarm newAlarm = (Alarm) alarm;
					mobilityCdcAlarmForwarder.write(newAlarm);
					log.trace("service_util.forwardAlarmToMobilityCDC(): Forwarded alarm to mobilityCdcAlarmForwarder: " + newAlarm.getIdentifier());
				} catch (AlarmForwarderException e) {
					log.error(e.getMessage());  
				}  
		} else {
			log.trace("service_util.forwardAlarmToMobilityCDC(): mobilityCdcAlarmForwarder is still NULL.");
		}			
		if (log.isTraceEnabled()) { 
			LogHelper.exit(log, "forwardAlarmToNOM()");
		}  
		 
	}
	
	/**
	 * Forwards the alarm to NOM
	 * @param scenario  
	 * @param alarm    
	 */
	public static void forwardAlarmToDecomposerInstance(Scenario scenario,Alarm alarm) { 
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "service_util.forwardAlarmToDecomposerInstance()");
		}
		//writeToAlarmForwarderFile(scenario,alarm); 
		if(decomposerJmsAlarmForwarder == null) { 
			if(retrieveBeanFromContextXml(scenario, GFPFields.DECOMPOSER_JMS_ALARM_FORWARDER) != null) {
				decomposerJmsAlarmForwarder = (JMSAlarmForwarder) retrieveBeanFromContextXml(scenario, GFPFields.DECOMPOSER_JMS_ALARM_FORWARDER);
				log.trace("service_util.forwardAlarmToDecomposerInstance(): Retrieved cpeCdcAlarmForwarder from spring bean : ");
			} 
		}  else {
			log.trace("forwardAlarmToDecomposerInstance exists alredy toString(): " + decomposerJmsAlarmForwarder.toString());
		}
		    
		if(decomposerJmsAlarmForwarder != null) {    
				try {    
					Alarm newAlarm = (Alarm) alarm; 
					decomposerJmsAlarmForwarder.write(alarm);
					log.trace("service_util.forwardAlarmToDecomposerInstance(): Forwarded alarm to decomposer instace: " + newAlarm.getIdentifier()+ " having Seq Number = " + alarm.getCustomFieldValue(GFPFields.SEQNUMBER));
					log.trace("service_util.forwardAlarmToDecomposerInstance(): classLoader = " + decomposerJmsAlarmForwarder.getClass().getClassLoader()); 
				} catch (AlarmForwarderException e) {
					log.trace("service_util.forwardAlarmToDecomposerInstance(): Exception when forwarding to decompserInstance "); 
					e.printStackTrace();    
					log.error(e.getMessage());    
				}  
		} else {         
			log.trace("service_util.forwardAlarmToDecomposerInstance(): forwardAlarmToDecomposerInstance is still NULL.");
		}			 
		if (log.isTraceEnabled()) { 
			LogHelper.exit(log, "service_util.forwardAlarmToDecomposerInstance(): forwardAlarmToDecomposerInstance()");
		}  
		  
	}
	
	/**
	 * Forwards the alarm to Juniper UCA instance
	 * @param scenario  
	 * @param alarm    
	 */
	public static void forwardAlarmToJuniperInstance(Scenario scenario,Alarm alarm) { 
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "service_util.forwardAlarmToJuniperInstance()");
		}
		//writeToAlarmForwarderFile(scenario,alarm); 
		if(juniperJmsAlarmForwarder == null) { 
			if(retrieveBeanFromContextXml(scenario, GFPFields.JUNIPER_JMS_ALARM_FORWARDER) != null) {
				juniperJmsAlarmForwarder = (JMSAlarmForwarder) retrieveBeanFromContextXml(scenario, GFPFields.JUNIPER_JMS_ALARM_FORWARDER);
				log.trace("service_util.forwardAlarmToJuniperInstance(): Retrieved juniperJmsAlarmForwarder from spring bean : ");
			} 
		}  else {
			log.trace("forwardAlarmToJuniperInstance exists alredy toString(): " + juniperJmsAlarmForwarder.toString());
		}
		    
		if(juniperJmsAlarmForwarder != null) {    
				try {    
					Alarm newAlarm = (Alarm) alarm; 
					juniperJmsAlarmForwarder.write(alarm);
					log.trace("service_util.forwardAlarmToJuniperInstance(): Forwarded alarm to decomposer instace: " + newAlarm.getIdentifier()+ " having Seq Number = " + alarm.getCustomFieldValue(GFPFields.SEQNUMBER));
					log.trace("service_util.forwardAlarmToJuniperInstance(): classLoader = " + juniperJmsAlarmForwarder.getClass().getClassLoader()); 
				} catch (AlarmForwarderException e) {
					log.trace("service_util.forwardAlarmToDecomposerInstance(): Exception when forwarding to JuniperInstance "); 
					e.printStackTrace();    
					log.error(e.getMessage());    
				}  
		} else {         
			log.trace("service_util.forwardAlarmToJuniperInstance(): forwardAlarmToJuniperInstance is still NULL.");
		}			 
		if (log.isTraceEnabled()) {  
			LogHelper.exit(log, "service_util.forwardAlarmToJuniperInstance(): forwardAlarmToJuniperInstance()");
		}  
		  
	}

	/**
	 * Forwards the alarm to Juniper UCA instance
	 * @param scenario  
	 * @param alarm    
	 */
	public static void forwardAlarmToCienaInstance(Scenario scenario,Alarm alarm) { 
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "service_util.forwardAlarmToCienaInstance()");
		}
		//writeToAlarmForwarderFile(scenario,alarm); 
		if(cienaJmsAlarmForwarder == null) { 
			if(retrieveBeanFromContextXml(scenario, GFPFields.CIENA_JMS_ALARM_FORWARDER) != null) {
				cienaJmsAlarmForwarder = (JMSAlarmForwarder) retrieveBeanFromContextXml(scenario, GFPFields.CIENA_JMS_ALARM_FORWARDER);
				log.trace("service_util.forwardAlarmToCienaInstance(): Retrieved cienaJmsAlarmForwarder from spring bean : ");
			} 
		}  else {
			log.trace("forwardAlarmToCienaInstance exists alredy toString(): " + cienaJmsAlarmForwarder.toString());
		}
		    
		if(cienaJmsAlarmForwarder != null) {    
				try {    
					Alarm newAlarm = (Alarm) alarm; 
					cienaJmsAlarmForwarder.write(alarm);
					log.trace("service_util.forwardAlarmToCienaInstance(): Forwarded alarm to decomposer instace: " + newAlarm.getIdentifier()+ " having Seq Number = " + alarm.getCustomFieldValue(GFPFields.SEQNUMBER));
					log.trace("service_util.forwardAlarmToCienaInstance(): classLoader = " + cienaJmsAlarmForwarder.getClass().getClassLoader()); 
				} catch (AlarmForwarderException e) {
					log.trace("service_util.forwardAlarmToCienaInstance(): Exception when forwarding to CienaInstance "); 
					e.printStackTrace();    
					log.error(e.getMessage());    
				}  
		} else {         
			log.trace("service_util.forwardAlarmToCienaInstance(): forwardAlarmToCienaInstance is still NULL.");
		}			 
		if (log.isTraceEnabled()) {  
			LogHelper.exit(log, "service_util.forwardAlarmToCienaInstance(): forwardAlarmToCienaInstance()");
		}  
		  
	} 

	/**
	 * Forwards the alarm to Adtran UCA instance
	 * @param scenario  
	 * @param alarm    
	 */
	public static void forwardAlarmToAdtranInstance(Scenario scenario,Alarm alarm) { 
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "service_util.forwardAlarmToAdtranInstance()");
		}
		//writeToAlarmForwarderFile(scenario,alarm); 
		if(adtranJmsAlarmForwarder == null) { 
			if(retrieveBeanFromContextXml(scenario, GFPFields.ADTRAN_JMS_ALARM_FORWARDER) != null) {
				adtranJmsAlarmForwarder = (JMSAlarmForwarder) retrieveBeanFromContextXml(scenario, GFPFields.ADTRAN_JMS_ALARM_FORWARDER);
				log.trace("service_util.forwardAlarmToAdtranInstance(): Retrieved adtranJmsAlarmForwarder from spring bean : ");
			} 
		}  else {
			log.trace("forwardAlarmToAdtranInstance exists alredy toString(): " + adtranJmsAlarmForwarder.toString());
		}
		    
		if(adtranJmsAlarmForwarder != null) {     
				try {    
					Alarm newAlarm = (Alarm) alarm; 
					adtranJmsAlarmForwarder.write(alarm);
					log.trace("service_util.forwardAlarmToAdtranInstance(): Forwarded alarm to Adtran instace: " + newAlarm.getIdentifier()+ " having Seq Number = " + alarm.getCustomFieldValue(GFPFields.SEQNUMBER));
					log.trace("service_util.forwardAlarmToAdtranInstance(): classLoader = " + adtranJmsAlarmForwarder.getClass().getClassLoader()); 
				} catch (AlarmForwarderException e) {
					log.trace("service_util.forwardAlarmToAdtranInstance(): Exception when forwarding to AdtranInstance "); 
					e.printStackTrace();    
					log.error(e.getMessage());    
				}  
		} else {         
			log.trace("service_util.forwardAlarmToAdtranInstance(): forwardAlarmToAdtranInstance is still NULL.");
		}			 
		if (log.isTraceEnabled()) {  
			LogHelper.exit(log, "service_util.forwardAlarmToAdtranInstance(): forwardAlarmToAdtranInstance()");
		}  
		  
	}  
	
}
