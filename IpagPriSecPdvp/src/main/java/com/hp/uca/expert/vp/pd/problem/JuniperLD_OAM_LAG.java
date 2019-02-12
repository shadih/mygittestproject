/**
 * This Problem is empty and ready to define methods to customize this problem
 */
package com.hp.uca.expert.vp.pd.problem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.Pri_Sec_Alarm;
import com.att.gfp.helper.GFPFields;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.vp.pd.core.PriSec_ProblemDefault;
import com.hp.uca.expert.vp.pd.core.ProblemDefault;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;

/**
 * @author df
 * 
 */
public final class JuniperLD_OAM_LAG extends PriSec_ProblemDefault implements
		ProblemInterface {

	
	private static final String LINKDOWN_KEY = "50003/100/1";
	private Logger log = LoggerFactory.getLogger(JuniperLD_OAM_LAG.class);

	public JuniperLD_OAM_LAG() {
		super();

	}
		 
	@Override
	public List<String> computeProblemEntity(Alarm a) throws Exception {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "computeProblemEntity()");
		}

		String meInstance = a.getOriginatingManagedEntity().split(" ")[1];
		String meClass = a.getOriginatingManagedEntity().split(" ")[0];

		List<String> problemEntities = new ArrayList<String>();
		Pri_Sec_Alarm alarm = (Pri_Sec_Alarm) a;

		// the problem entity is the containing pport if an LPORT alarm 
		if(alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(LINKDOWN_KEY)) {
			if(meClass.equals("LPORT")) {
				if(alarm.getContainingPPort()!=null){
					problemEntities.add(alarm.getContainingPPort());
					if (log.isTraceEnabled())
							log.trace("### Containing port="+ alarm.getContainingPPort());
				}
			} else {
				// its the lagid if pport
				if(meClass.equals("PPORT")) {
					if(alarm.getPortLagId() != null && !alarm.getPortLagId().isEmpty()) 
						problemEntities.add(alarm.getPortLagId());
					
					if(alarm.getDeviceInstance() != null && !alarm.getDeviceInstance().isEmpty())
						problemEntities.add(alarm.getDeviceInstance());					
				} else {
					// if a device then we need the lag
					if(meClass.equals("DEVICE")) {
						if(alarm.getlagIdFromAlarm() != null)
							problemEntities.add(alarm.getlagIdFromAlarm());
					}
				}
			}
		} else {
			// if the component contains the lagid
			if(alarm.getlagIdFromAlarm() != null)
				problemEntities.add(alarm.getlagIdFromAlarm());
		}

		// in any case we put the ME instance 
		problemEntities.add(meInstance);

		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", computeProblemEntity() --- problemEntities=" + problemEntities);

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

			if(!alarm.isLagSubInterfaceLinkDown()) {
				
				// this is what we use to determine how long to hold the trigger before we let it go
				long afterTrigger = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger();
				long ageLeft = alarm.TimeRemaining(afterTrigger); 
				long holdTime = ageLeft + maxSAge; 
				
				log.trace("The time of the trigger callback is " + holdTime);
				
				String watchdogDesc = "Creating watchdog for:" + getProblemPolicy().getName();

				Util.setTriggerWatch((Pri_Sec_Alarm)alarm, Pri_Sec_Alarm.class, "simpleSendCallBack", holdTime, watchdogDesc);
				ret = true;
			}
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "isMatchingTriggerAlarmCriteria() " + "[" + ret + "]");
		}

		return ret;
	}
	
	@Override
	public boolean isMatchingSubAlarmCriteria(Alarm a, Group group)
			throws Exception {
		boolean ret = true;

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "isMatchingSubAlarmCriteria()");
		}

		if ( a instanceof Pri_Sec_Alarm)  {
			Pri_Sec_Alarm alarm = (Pri_Sec_Alarm) a;	

			// if this is a sub-interface device alarm then we don't care for it here.
			if(alarm.getOriginatingManagedEntity().split(" ")[0].equals("DEVICE") && alarm.isLagSubInterfaceLinkDown())
				ret = false;
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "isMatchingSubAlarmCriteria() " + "[" + ret + "]");
		}

		return ret;
	}
}


