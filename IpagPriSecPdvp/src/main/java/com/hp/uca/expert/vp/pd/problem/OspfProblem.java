package com.hp.uca.expert.vp.pd.problem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.Int;

import com.att.gfp.data.ipagAlarm.Pri_Sec_Alarm;
import com.att.gfp.helper.GFPFields;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.vp.pd.core.PriSec_ProblemDefault;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;


/**
 * @author MASSE
 * 
 */
public final class OspfProblem extends PriSec_ProblemDefault implements
		ProblemInterface {
	
	private Logger log = LoggerFactory.getLogger(OspfProblem.class);

	public OspfProblem() {
		super();
	}

/*
	@Override
	public boolean isAllCriteriaForProblemAlarmCreation(Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			log.trace(
					"ENTER:  isAllCriteriaForProblemAlarmCreation() for group ["
							+ group.getTrigger().getIdentifier() + "]",
							group.getName());
		}

		boolean ret = false;
		
		Collection<Alarm> alarmsInGroup = group.getAlarmList();
		int numberOfAlarmsInGroup = alarmsInGroup.size();

		if (log.isDebugEnabled()) {
			log.debug("numberOfAlarmsInGroup = " + numberOfAlarmsInGroup);
		}

		if (numberOfAlarmsInGroup > 1) 
			ret = true;
		
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "isAllCriteriaForProblemAlarmCreation()",
					String.valueOf(ret));
		}
		return ret;
	}*/
	
	@Override
	public List<String> computeProblemEntity(Alarm alarm) throws Exception {

		List<String> problemEntities = new ArrayList<String>();
		Pri_Sec_Alarm a = (Pri_Sec_Alarm) alarm;
		
/*		if (a.getCustomFieldValue("lag_id") != null && ! a.getCustomFieldValue("lag_id").isEmpty() ) {
			problemEntities.add(a.getCustomFieldValue("lag_id"));
		}*/
		//add remote device ip address to problem entity for device level lag linkdown and for pport level alarms.... 

		if ( a.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50003/100/21") ) {
			problemEntities.add(a.getDeviceIpAddr());
			problemEntities.add(a.getDeviceIpAddr() + "_" + a.getCustomFieldValue("reason_code"));
		}
		else {
		if ( a.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50003/100/1") && a.getOrigMEClass().equals("PPORT") ) {		
			problemEntities.add(a.getOriginatingManagedEntity().split(" ")[1]);
			problemEntities.add(a.getDeviceIpAddr() + "_" + a.getCustomFieldValue("reason_code"));
		} else {
			problemEntities.add(a.getDeviceIpAddr());  
			if ( a.getCustomFieldValue("lag_port_key") != null && ! a.getCustomFieldValue("lag_port_key").isEmpty() ) {		
				problemEntities.add(a.getCustomFieldValue("lag_port_key"));
			}
			if ( a.getRemoteDeviceIpaddr() != null && ! a.getRemoteDeviceIpaddr().isEmpty() ) {		
				problemEntities.add(a.getRemoteDeviceIpaddr());
			}
			
			// add pport to problemEntities
			if ( a.getRemotePePportInstanceName() != null && ! a.getRemotePePportInstanceName().isEmpty() ) {
				problemEntities.add(a.getRemotePePportInstanceName());
			}
		}
		}
		
		return problemEntities;

	} 
	
	@Override
	public boolean isMatchingTriggerAlarmCriteria(Alarm a) throws Exception {
		boolean ret = false;

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "isMatchingTriggerAlarmCriteria()");
		}


		if ( a instanceof Pri_Sec_Alarm)  {
			Pri_Sec_Alarm alarm = (Pri_Sec_Alarm) a;	

			// if the alarm is a device level then we only want it to be a trigger
			// if it is a subinterface link down.
			if(alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50003/100/1") &&
					alarm.getOriginatingManagedEntity().split(" ")[0].equals("DEVICE") && 
					!alarm.isLagSubInterfaceLinkDown())  {
				ret = false;
			} else {
				// this is what we use to determine how long to hold the trigger before we let it go
				long afterTrigger = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger();
				long ageLeft = alarm.TimeRemaining(afterTrigger); 
				long holdTime = ageLeft + maxSAge; 

				String watchdogDesc = "Creating watchdog for:" + getProblemPolicy().getName() +
						" timer:" + holdTime;

				Util.setTriggerWatch((Pri_Sec_Alarm)alarm, Pri_Sec_Alarm.class, "simpleSendCallBack", holdTime, watchdogDesc);
				ret = true;
			}
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "isMatchingTriggerAlarmCriteria() " + "[" + ret + "]");
		}

		return ret;
	}

}
