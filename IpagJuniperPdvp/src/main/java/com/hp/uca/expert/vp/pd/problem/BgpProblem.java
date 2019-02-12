/**
 * 
 */
package com.hp.uca.expert.vp.pd.problem;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.data.ipagAlarm.AlarmState;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.data.ipagAlarm.EnrichedJuniperAlarm;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
import com.hp.uca.common.callback.Callback;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.vp.pd.core.JuniperLinkDown_ProblemDefault;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.scenario.ScenarioThreadLocal;
import com.hp.uca.expert.vp.pd.core.ProblemDefault;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;

/**
 * @author MASSE
 * 
 */
public final class BgpProblem extends JuniperLinkDown_ProblemDefault implements ProblemInterface {

	private static final String _50004_1_2 = "50004/1/2";
	private static final String _50003_100_1 = "50003/100/1";
	private static final String _50003_100_2 = "50003/100/2";
	/**
	 * Logger used to trace this component
	 */
	private Logger log = LoggerFactory.getLogger(BgpProblem.class);

	@Override
	public List<String> computeProblemEntity(Alarm a) throws Exception {

		List<String> problemEntities = new ArrayList<String>();
		
		if (a instanceof EnrichedJuniperAlarm) {
			EnrichedJuniperAlarm ea = (EnrichedJuniperAlarm) a;
			if (!ea.isSuppressed()) {
				if (ea.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50003_100_2)) {
					if(ea.getDeviceIpAddr()!=null && !ea.getDeviceIpAddr().isEmpty())
						problemEntities.add(ea.getDeviceIpAddr());
					if(ea.getRemoteDeviceIpaddr()!=null && !ea.getRemoteDeviceIpaddr().isEmpty())
						problemEntities.add(ea.getRemoteDeviceIpaddr());
				} else if (ea.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50004_1_2)) {
					if(ea.getDeviceIpAddr()!=null && !ea.getDeviceIpAddr().isEmpty())
						problemEntities.add(ea.getDeviceIpAddr());
				} else if (ea.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50003_100_1) &&
						ea.getDeviceType()!=null && !ea.getDeviceType().isEmpty() &&
						ea.getDeviceType().equals("VR1") &&
						ea.getOriginatingManagedEntity().startsWith("PPORT") &&
						ea.getRemoteDeviceType()!=null && !ea.getRemoteDeviceType().isEmpty() &&
						ea.getRemoteDeviceType().equals("crs")) {
							problemEntities.add(ea.getDeviceIpAddr());
				}
			}
		}
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", computeProblemEntity() --- problemEntities=" + problemEntities);
		return problemEntities;

	}
	
	@Override
	public boolean isMatchingSubAlarmCriteria(Alarm a, Group group)
			throws Exception {

		boolean ret = false;
		EnrichedJuniperAlarm triggerAlarm = (EnrichedJuniperAlarm) group.getTrigger();
		if (a == triggerAlarm) {
			if (log.isTraceEnabled())
				log.trace("isMatchingSubAlarmCriteria: triggerAlarm.id=" + triggerAlarm.getIdentifier());
			ret = true;
		} else {
			EnrichedJuniperAlarm subAlarm = (EnrichedJuniperAlarm) a;
			if (subAlarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50003_100_2)) {
				if (subAlarm.getRemoteDeviceIpaddr().equals(triggerAlarm.getDeviceIpAddr()) &&
					subAlarm.getDeviceIpAddr().equals(triggerAlarm.getRemoteDeviceIpaddr())) {
					ret = true;
				}
			} else {
				ret = true;
			}
		}	
		if (ret == true)
		{
			EnrichedJuniperAlarm ea = (EnrichedJuniperAlarm) a;
			String targetName = null;
			Util.WDPool(ea, 0, getScenario());
		}
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingSubAlarmCriteria() " + "[" + ret + "], group name = " + group.getName());
		return ret;
	}


	@Override
	public boolean isAllCriteriaForProblemAlarmCreation(Group group)
			throws Exception {
		if (log.isTraceEnabled())
			log.trace("isAllCriteriaForProblemAlarmCreation(): " + group.toString());
		return false;
	}

	@Override
	public boolean isMatchingTriggerAlarmCriteria(Alarm a) throws Exception {
		boolean ret = true;

		
		// This method is only here as a workaround for an issue where the bgpProblem is calling this method
		// Instantiated in the Other_LinkDown_AlarmSuppresion class
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "isMatchingTriggerAlarmCriteria()");
		}


		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "isMatchingTriggerAlarmCriteria() " + "[" + ret + "]");
		}

		return ret;
	}
	
	public void compareBgpAlarms(EnrichedJuniperAlarm triggerAlarm, EnrichedJuniperAlarm subAlarm) {
		
		if (isGfpIpDevice(triggerAlarm.getDeviceType()) &&
			isGfpIpDevice(subAlarm.getDeviceType())) {
	           log.info("Suppressing BGP Down event: " + 
        		   subAlarm.getIdentifier() + " | " + 
        		   subAlarm.getDeviceIpAddr() +
        		   "  because local and neighbor are both GFP-IP devices.  Letting " +
        		   "neighbor take precendence.");
	           subAlarm.setSuppressed(true);
		} else if (isGfpIpDevice(subAlarm.getDeviceType())) {
			String info1 = triggerAlarm.getCustomFieldValue("info1");
			if (info1 == null || info1.isEmpty()) {
				info1 = "<Child=Y> ";
			} else if(!(info1.contains("Child="))){
				info1 = "<Child=Y> " + info1;
			}
			triggerAlarm.setCustomFieldValue("info1", info1);
			log.info("BGP Down event: " + triggerAlarm.getIdentifier() + " | " + 
				triggerAlarm.getDeviceIpAddr() + " neighbor is a GFP-IP device; info1=" + 
				triggerAlarm.getCustomFieldValue("info1"));
			log.info("Suppressing this BGP Down event: " + 
				subAlarm.getIdentifier() + " | " + 
				subAlarm.getDeviceIpAddr() +
           		" because neighbor has same event and takes precendence.");
			subAlarm.setSuppressed(true);
		} else if (isGfpIpDevice(triggerAlarm.getDeviceType())) {
			String info1 = subAlarm.getCustomFieldValue("info1");
			if (info1 == null || info1.isEmpty()) {
				info1 = "<Child=Y> ";
			} else if(!(info1.contains("Child="))){
				info1 = "<Child=Y> " + info1;
			}
			subAlarm.setCustomFieldValue("info1", info1);
			log.info("BGP Down event: " + subAlarm.getIdentifier() + " | " + 
				subAlarm.getDeviceIpAddr() + " neighbor is a GFP-IP device; info1=" + 
				subAlarm.getCustomFieldValue("info1"));
			log.info("Suppressing this BGP Down event: " + 
				triggerAlarm.getIdentifier() + " | " + 
				triggerAlarm.getDeviceIpAddr() +
	           		" because neighbor has same event and takes precendence.");
			triggerAlarm.setSuppressed(true);
		} else if ( ( subAlarm.getDeviceType().equals("JUNIPER MX SERIES") || subAlarm.getDeviceType().equals("NV1") || subAlarm.getDeviceType().equals("NV2") ) &&
				   !subAlarm.getDeviceSubRole().equals("MRR") &&
				   ( triggerAlarm.getDeviceType().equals("JUNIPER MX SERIES") || triggerAlarm.getDeviceType().equals("NV1") 
				     || triggerAlarm.getDeviceType().equals("NV2") || triggerAlarm.getDeviceType().equals("VR1") ) &&
				   !triggerAlarm.getDeviceSubRole().equals("MRR") ) {
			log.info("The local DeviceSubRole is not MRR; neighbor DeviceSubRole is not MRR - " +
				"suppressing local alarm.");
			triggerAlarm.setSuppressed(true);			
			// DF - I added the stuff below this
		} else if ( ( subAlarm.getDeviceType().equals("JUNIPER MX SERIES") || subAlarm.getDeviceType().equals("NV1") || subAlarm.getDeviceType().equals("NV2") ) &&			
				   !subAlarm.getDeviceSubRole().equals("MRR") &&
				   (triggerAlarm.getDeviceType().equals("JUNIPER MX SERIES") || triggerAlarm.getDeviceType().equals("NV1") 
				    || triggerAlarm.getDeviceType().equals("NV2") || triggerAlarm.getDeviceType().equals("VR1")) &&
					triggerAlarm.getDeviceSubRole().equals("MRR")) {
			log.info("The local DeviceSubRole is MRR; neighbor DeviceSubRole is not MRR - " +
				"suppressing remote alarm:" + subAlarm.getIdentifier());
			subAlarm.setSuppressed(true);
		} else if ( ( subAlarm.getDeviceType().equals("JUNIPER MX SERIES") || subAlarm.getDeviceType().equals("NV1") || subAlarm.getDeviceType().equals("NV2") ) &&			
				   subAlarm.getDeviceSubRole().equals("MRR") &&
				   (triggerAlarm.getDeviceType().equals("JUNIPER MX SERIES") || triggerAlarm.getDeviceType().equals("NV1") 
				    || triggerAlarm.getDeviceType().equals("NV2") || triggerAlarm.getDeviceType().equals("VR1")) && 
					!triggerAlarm.getDeviceSubRole().equals("MRR")) {
			log.info("The local DeviceSubRole is MRR; neighbor DeviceSubRole is not MRR - " +
				"suppressing remote alarm:" + triggerAlarm.getIdentifier());
			triggerAlarm.setSuppressed(true); 
		} else if ( ( subAlarm.getDeviceType().equals("JUNIPER MX SERIES") || subAlarm.getDeviceType().equals("NV1") || subAlarm.getDeviceType().equals("NV2") ) &&
				subAlarm.getDeviceSubRole().equals("MRR") &&
				   (triggerAlarm.getDeviceType().equals("JUNIPER MX SERIES")  || triggerAlarm.getDeviceType().equals("NV1") 
				    || triggerAlarm.getDeviceType().equals("NV2") || triggerAlarm.getDeviceType().equals("VR1")) &&
					triggerAlarm.getDeviceSubRole().equals("MRR")) {
			log.info("The local DeviceSubRole is MRR; neighbor DeviceSubRole is MRR - " +
				"suppressing local alarm:" + triggerAlarm.getIdentifier());
			triggerAlarm.setSuppressed(true);
		} else {
			log.info("Suppressing this BGP Down event: " + 
					triggerAlarm.getIdentifier() + 
           		" because the first recieved alarm takes precendence.");
			subAlarm.setSuppressed(true);
		}
	}
	
	public boolean isGfpIpDevice(String deviceType) {
		if (deviceType.equals("AR") ||
			deviceType.equals("BR") ||
			deviceType.equals("RR")) {
				return true;
		} else {
			return false;
		}
	}

	/**
	 * TODO
	 */
	@Override
	public boolean calculateIfProblemAlarmhasToBeCleared(Group arg0)
			throws Exception {
		/*
		 * ProblemAlarm Clearance is manually managed through this Customization
		 */
		return false;

	}

	@Override
	public void whatToDoWhenSubAlarmIsAttachedToGroup(Alarm alarm, Group group) throws Exception {

		if (log.isTraceEnabled())
			LogHelper.enter(log, "alarm = " + alarm.getIdentifier() + ", group = " + group.getName() + ", group # = " + group.getNumber() + ", whatToDoWhenSubAlarmIsAttachedToGroup()");
		
		EnrichedJuniperAlarm bgpAlarm = (EnrichedJuniperAlarm) alarm;
		EnrichedJuniperAlarm triggerAlarm = (EnrichedJuniperAlarm) group.getTrigger();
		
		if (bgpAlarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50003_100_2)) {
			Scenario scenario = ScenarioThreadLocal.getScenario();

			Class<?> partypes[] = new Class[0];
			Object arglist[] = new Object[0];
			Method method = EnrichedJuniperAlarm.class.getMethod("bgpProblemCallback",
					partypes);
			Callback callback = new Callback(method, bgpAlarm, arglist);
			
			long correlationWindow = 0L;
			/***
			String correlationWindowStr = alarm.getPassingFiltersParams()
					.get("BGP_PBD").get("CorrelationWindow");

			try {
				Long temp = Long.parseLong(correlationWindowStr);
				correlationWindow = temp * 1000;
			} catch (Exception e) {
				correlationWindow = 5 * 1000;
			}
			***/
			
			// Currently hard-coding to 3 minutes.
			correlationWindow = 180 * 1000;
			scenario.addCallbackWatchdogItem(correlationWindow, callback, false,
					"Correlation WatchdogItem", true, bgpAlarm);
			if (log.isTraceEnabled())
				log.trace("whatToDoWhenSubAlarmIsAttachedToGroup(): Aging " + 
						bgpAlarm.getIdentifier() + " for 3 mins.");
			if(alarm == group.getTrigger()) {
				if (log.isTraceEnabled())
					log.trace("whatToDoWhenSubAlarmIsAttachedToGroup() alarm is trigger");
				if(triggerAlarm.getRemoteDeviceType() != null && triggerAlarm.getDeviceType() != null) {
				if(isGfpIpDevice(triggerAlarm.getRemoteDeviceType()) && !(isGfpIpDevice(triggerAlarm.getDeviceType()))) {
					String info1 = triggerAlarm.getCustomFieldValue("info1");
					if (info1 == null || info1.isEmpty()) {
						info1 = "<Child=Y> ";
					} else if(!(info1.contains("Child="))){
						info1 = "<Child=Y> " + info1;
					}
					triggerAlarm.setCustomFieldValue(GFPFields.INFO1, info1);
				}
				}
			}
			
			if (bgpAlarm != triggerAlarm) {
				compareBgpAlarms(triggerAlarm, bgpAlarm);
			}
		}
	}
	
/*	@Override
	public void whatToDoWhenSubAlarmIsCleared(Alarm alarm, Group group)
			throws Exception {

		if (alarm instanceof EnrichedJuniperAlarm) {
			EnrichedJuniperAlarm a = (EnrichedJuniperAlarm) alarm;
			if (a.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50003_100_2)) {
				if (a.getAlarmState() == AlarmState.sent) {
					log.info("==> Sending Clear: " + a.toString());
					Util.sendAlarm((EnrichedAlarm) a, AlarmDelegationType.FORWARD, null, false);
				} else {
					log.info("Alarm " + a.getIdentifier() + " is not active, is suppressed or " +
						"has cleared within correlation window; discarding clear.");
					a.setSuppressed(true);
				}
			}
		}
	}*/


	/**
	 * TODO
	 */
	@Override
	public void computeBooleans() {
	}

	/**
	 * TODO
	 */
	@Override
	public void computeLongs() {
	}

	/**
	 * TODO
	 */
	@Override
	public void computeStrings() {
	}

}
