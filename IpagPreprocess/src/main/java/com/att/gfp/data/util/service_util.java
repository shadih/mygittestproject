package com.att.gfp.data.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.att.gfp.data.config.IpagXmlConfiguration;
import com.att.gfp.data.config.UCAInstancesXmlConfiguration;
import com.att.gfp.data.config.UCAInstnaces;
import com.att.gfp.data.config.UCAInstnaces.UCAInstance;
import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.data.ipagPreprocess.preprocess.EnrichedPreprocessAlarm;
import com.att.gfp.data.preprocess.conf.PreProcessConfiguration;
import com.att.gfp.data.preprocess.conf.PreProcessPolicies.CascadeTargets.CascadeTarget;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.alarm.AlarmForwarder;
import com.hp.uca.expert.alarm.FileAlarmForwarder;
import com.hp.uca.expert.alarm.OpenMediationAlarmForwarder;
import com.hp.uca.expert.alarm.exception.AlarmForwarderException;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;

public class service_util {

	private static AlarmForwarder fileAlarmForwarder; 
	private static OpenMediationAlarmForwarder nomAlarmForwarder;
	private static OpenMediationAlarmForwarder mobilityCdcAlarmForwarder;
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

		ApplicationContext context = scenario.getVPApplicationContext(); 
		return context.getBean(beanName);   

	}

	// TODO: to be handled by the workflow manager
	public static void sendAlarm(Scenario scenario, EnrichedPreprocessAlarm alarm){

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "sendAlarm()");
		}
		String tunable = alarm.getCustomFieldValue("EventKey");
		//unenriched alarm  
		//		Alarm newAlarm = (Alarm) alarm;
		sendAlarm(scenario, alarm, tunable);

		if (log.isTraceEnabled()) {  
			LogHelper.exit(log, "sendAlarm()");
		}

	}

	public static void sendClearAlarmToCascadingVPs(Scenario scenario, Alarm alarm) {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "sendClearAlarmToCascadingVPs()");
		}
		EnrichedAlarm enrichedAlarm = new EnrichedAlarm(alarm);
		enrichedAlarm.setPerceivedSeverity(PerceivedSeverity.CLEAR);
		enrichedAlarm.setCustomFieldValue(NetcoolFields.IS_PURGE_INTERVAL_EXPIRED, "YES");
		enrichedAlarm.setIsPurgeIntervalExpired("YES");

		String tunable = alarm.getCustomFieldValue("EventKey");
		if(GFPUtil.getAdtranJuniperTraps().contains(tunable) && enrichedAlarm.isPassthru() ) {
			if (log.isTraceEnabled()) {
				log.trace("service_util.sendAlarm(): This is a pass thru CLEAR event - no need to cascade " + tunable);
			}  
		} 
		else {
			//cascade to other VPs  
			PreProcessConfiguration preprocessConf = (PreProcessConfiguration)retrieveBeanFromContextXml(scenario, "preProcConfig"); 
			List<CascadeTarget> cascadeTargets = preprocessConf.getPreProcessPolicies().getCascadeTargets().getCascadeTarget();
			for(CascadeTarget cascadeTarget : cascadeTargets) {
				if(cascadeTarget.getEventNames().getEventName().contains(enrichedAlarm.getCustomFieldValue(NetcoolFields.EVENT_KEY))) {
					if (log.isInfoEnabled()) {
						log.info("sendClearAlarmToCascadingVPs(): Cascading alarm  = " + enrichedAlarm.toXMLString()); 
						log.info("sendClearAlarmToCascadingVPs(): Cascading alarm to VP Name= " + cascadeTarget.getVpName()); 
					}

					if(getVPsInCurrentInstance(scenario).contains(cascadeTarget.getVpName())) {
						scenario.delegateAlarmToScenario( 
								cascadeTarget.getVpName(),    
								String.valueOf(cascadeTarget.getVersion()),  
								cascadeTarget.getScenarioName(), enrichedAlarm);
					}

					if(GFPUtil.getAdtranJuniperTraps().contains(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
						enrichedAlarm.setTargetValuePack("AdtranPdvp" + "-" + "0.1");
						GFPUtil.forwardAlarmtoAdtranInstnance(scenario, enrichedAlarm);
					}
					if(GFPUtil.getAdtranCFMTraps().contains(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
						enrichedAlarm.setTargetValuePack("ipagJuniperSyslogPdvp" + "-" + "0.3");
						GFPUtil.forwardAlarmtoJuniperInstnance(scenario, enrichedAlarm);
					}
					if( ("50004/3/2".equalsIgnoreCase(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) ||
							("50001/100/52".equalsIgnoreCase(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) ) {
						enrichedAlarm.setTargetValuePack("ipagJuniperSyslogPdvp" + "-" + "0.3");
						GFPUtil.forwardAlarmtoJuniperInstnance(scenario, enrichedAlarm);
					}
					if("50004/2/58916875".equalsIgnoreCase(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
						enrichedAlarm.setTargetValuePack("IpagJuniperPdvp" + "-" + "0.6");
						GFPUtil.forwardAlarmtoJuniperInstnance(scenario, enrichedAlarm);
					}
				}     
			} 
		}
		if (log.isTraceEnabled()) { 
			LogHelper.exit(log, "sendClearAlarmToCascadingVPs()");
		}
	}
	// TODO: to be handled by the workflow manager

	/**
	 * 
	 */
	public static void sendAlarm(Scenario scenario, Alarm alarm, String tunable){

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "sendAlarm()");
			log.trace("Tunable is:" + tunable);
		}

		// log every alarm to a file for now
		String axml = alarm.toXMLString();
		axml = axml.replaceAll("\\n", " ");

		com.att.gfp.data.ipagAlarm.EnrichedAlarm enrichedAlarm = null;
		enrichedAlarm = new com.att.gfp.data.ipagAlarm.EnrichedAlarm(alarm); 

		if(GFPUtil.getAdtranJuniperTraps().contains(tunable) && enrichedAlarm.isPassthru() ) {
			if (log.isTraceEnabled()) {
				log.trace("service_util.sendAlarm(): This is a pass thru event " + tunable);
				log.trace("service_util.sendAlarm(): Writing alarm to forwarded alarms file = " + axml);  
			}
			GFPUtil.forwardOrCascadeAlarm(alarm, AlarmDelegationType.FORWARD, null);  
		} 
		else {
			PreProcessConfiguration preprocessConf = (PreProcessConfiguration)retrieveBeanFromContextXml(scenario, "preProcConfig");
			List<String> passThruEvents = new ArrayList<String>();
			if(preprocessConf.getPreProcessPolicies() != null) {
				passThruEvents = preprocessConf.getPreProcessPolicies().getPassThru().getEventNames().getEventName();
			}

			List<CascadeTarget> cascadeTargets = preprocessConf.getPreProcessPolicies().getCascadeTargets().getCascadeTarget();
			for(CascadeTarget cascadeTarget : cascadeTargets) {
				if(cascadeTarget.getEventNames().getEventName().contains(alarm.getCustomFieldValue(NetcoolFields.EVENT_KEY))) {
					if(!enrichedAlarm.isPassthru())	 {
						if (log.isInfoEnabled()) {
							log.info("Sending the alarm : toString() " + enrichedAlarm.toString()); 
							log.info("Cascading alarm  = " + axml); 
							log.info("Cascading alarm to VP Name= " + cascadeTarget.getVpName() + " Scenario = " + cascadeTarget.getScenarioName());   
						}
						if(getVPsInCurrentInstance(scenario).contains(cascadeTarget.getVpName())) {
							scenario.copyAlarmToScenario(
									cascadeTarget.getVpName(),      
									String.valueOf(cascadeTarget.getVersion()),             
									cascadeTarget.getScenarioName(), enrichedAlarm);
						}

						if(GFPUtil.getAdtranJuniperTraps().contains(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
							enrichedAlarm.setTargetValuePack("AdtranPdvp" + "-" + "0.1");  
							GFPUtil.forwardAlarmtoAdtranInstnance(scenario, enrichedAlarm);
						}
						if(GFPUtil.getAdtranCFMTraps().contains(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
							enrichedAlarm.setTargetValuePack("ipagJuniperSyslogPdvp" + "-" + "0.3");  
							GFPUtil.forwardAlarmtoJuniperInstnance(scenario, enrichedAlarm);
						} 
						if( ("50004/3/2".equalsIgnoreCase(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) ||
								("50001/100/52".equalsIgnoreCase(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) ) {
							enrichedAlarm.setTargetValuePack("ipagJuniperSyslogPdvp" + "-" + "0.3");  
							GFPUtil.forwardAlarmtoJuniperInstnance(scenario, enrichedAlarm); 
						}					 
						if("50004/2/58916875".equalsIgnoreCase(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
							enrichedAlarm.setTargetValuePack("IpagJuniperPdvp" + "-" + "0.6");  
							GFPUtil.forwardAlarmtoJuniperInstnance(scenario, enrichedAlarm);
						}					 
					} else { 
						if (log.isTraceEnabled()) {
							log.trace("alarm is defined as a passthru " + enrichedAlarm.getIdentifier());
						}
					}
				}        
			}  

			if(passThruEvents.contains(tunable) || enrichedAlarm.isPassthru() || "10000/500/1".equalsIgnoreCase(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
				if (log.isTraceEnabled()) {
					log.trace("service_util.sendAlarm(): This is a pass thru event " + tunable);
					log.trace("service_util.sendAlarm(): Writing alarm to forwarded alarms file = " + axml);  
				}
				GFPUtil.forwardOrCascadeAlarm(alarm, AlarmDelegationType.FORWARD, null);  
			} 
		}
		if (log.isTraceEnabled()) {  
			LogHelper.exit(log, "sendAlarm()");
		} 		
	}

	public static void sendCdcAlarm(Scenario scenario, Alarm alarm){

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "sendCdcAlarm()"); 
		}  
		IpagXmlConfiguration ipagXmlConfig = (IpagXmlConfiguration) retrieveBeanFromContextXml(scenario, "ipagXmlConfig");
		if(ipagXmlConfig.getIpagPolicies().getCdcAlarms().getEventNames().getEventName().contains(alarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
			if (log.isTraceEnabled()) {
				log.trace("Alarm " + alarm.getIdentifier() + "|{" + alarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "} is configured to send to Mobility CDC");
			}
			if(fileAlarmForwarder == null) { 
				if(retrieveBeanFromContextXml(scenario,"alarmForwarder") != null) {
					fileAlarmForwarder = (FileAlarmForwarder) (retrieveBeanFromContextXml(scenario,"alarmForwarder"));
					log.trace("service_util.sendCdcAlarm(): retrieved file alarm forwarder from spring bean = ");
				}
			}
			if(fileAlarmForwarder != null) {
				try { 
					fileAlarmForwarder.write(alarm);
					if (log.isTraceEnabled()) {
						log.trace("service_util.sendCdcAlarm(): Wrote CDC alarm to file:" + alarm.getIdentifier());
					}
				} catch (AlarmForwarderException e) {
					log.error(e.getMessage()); 
				}
			} else {
				log.trace("service_util.sendCdcAlarm(): fileAlarmForwarder is still NULL.");
			}  

			// send to NOM   
			if(mobilityCdcAlarmForwarder == null) {
				if(retrieveBeanFromContextXml(scenario,"mobilityCDCAlarmForwarder") != null) {
					mobilityCdcAlarmForwarder = (OpenMediationAlarmForwarder) retrieveBeanFromContextXml(scenario,"mobilityCDCAlarmForwarder");
					if (log.isTraceEnabled()) {
						log.trace("service_util.sendCdcAlarm(): Retrieved mobilityCDCAlarmForwarder from spring bean : ");
					}
				}  
			}
			if(mobilityCdcAlarmForwarder != null) { 
				try {   
					// can't do this in JUNIT..sure  
					Alarm newAlarm = (Alarm) alarm;
					mobilityCdcAlarmForwarder.write(newAlarm);
					if (log.isTraceEnabled()) {
						log.trace("service_util.sendCdcAlarm(): Forwarded alarm to mobilityCdcAlarmForwarder: " + newAlarm.getIdentifier());
					}
				} catch (AlarmForwarderException e) {
					log.error(e.getMessage());
				}  
			} else {
				log.trace("mobilityCdcAlarmForwarder is still NULL.");
			}		 
		}
		if (log.isTraceEnabled()) { 
			LogHelper.exit(log, "forwardAlarmToNOM()");
		}  
	}
	public static List<String> getVPsInCurrentInstance(Scenario scenario) {
		String currentInstance=System.getProperty("uca.instance");
		try {
			List<String> vpNames = new ArrayList<String>();
			UCAInstancesXmlConfiguration ucaInstanceConfiguration = (UCAInstancesXmlConfiguration) service_util.retrieveBeanFromContextXml(scenario, "UCAInstances");
			for (UCAInstance ucaInstnace : ucaInstanceConfiguration.getUCAInstnaces().getUCAInstance()) {
				if (ucaInstnace.getInstancename().equals(currentInstance)){
					vpNames = ucaInstnace.getVpNames().getVpName();
					break;
				}
			}
			return vpNames;
		}
		catch (Exception e) {
			log.info("Exception" +Arrays.toString(e.getStackTrace()));

		}
		return null;
	}
}
