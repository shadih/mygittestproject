/**
 * This Problem is empty and ready to define methods to customize this problem
 */
package com.hp.uca.expert.vp.pd.problem;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.data.ipagAlarm.SyslogAlarm;
import com.att.gfp.helper.GFPFields;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.vp.pd.core.JuniperSyslog_ProblemDefault;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;

/**
 * @author MASSE
 * 
 */
public final class RpdRsvpBypassDown extends JuniperSyslog_ProblemDefault implements ProblemInterface {
	private Logger log = LoggerFactory.getLogger(RpdRsvpBypassDown.class);
	//2.	Other events referenced here for correlation:
	//	1.	juniper-LinkDown-eventKey = "50003/100/1"
	//	2.	crs1-LAG-LinkDown-evenKey = "50003/100/23"
	private static final String Trigger_LINKDOWN_KEY = "50004/1/9";
	//subalarms
	private static final String CSR1_LAG_LINKDOWN = "50003/100/23";
	private static final String JUNIPER_lINKDOWN  = "50003/100/1";
	private static final String RPD_RSVP_BYPASS_DOWN = "50004/1/9";

	public RpdRsvpBypassDown() {
		super();
		setLog(LoggerFactory.getLogger(RpdRsvpBypassDown.class));
	}

	@Override
	public boolean isMatchingTriggerAlarmCriteria(Alarm a) throws Exception {
		boolean ret = false;

		SyslogAlarm alarm = (SyslogAlarm) a;	
		long after = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger();
		String watchdogDesc = "RpdRsvpByPass watch dog.";
		Util.setTriggerWatch((SyslogAlarm)a, SyslogAlarm.class, "rpdRsvpByPassDownexpirationCallBack", after, watchdogDesc);
		ret = true; 

		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingTriggerAlarmCriteria() " + "[" + ret + "]");
		return ret;
	} 


	@Override
	public boolean isMatchingCandidateAlarmCriteria(Alarm a) throws Exception {
		SyslogAlarm ea = (SyslogAlarm) a;
		String eventKey = ea.getCustomFieldValue(GFPFields.EVENT_KEY);
		String targetName = null;
		long holdTime = 0;
		if (eventKey.equals(CSR1_LAG_LINKDOWN)){

			long beforeTrigger = getProblemPolicy().getTimeWindow().getTimeWindowBeforeTrigger();
			holdTime = beforeTrigger + 2000;
		}

		Util.WDPool(ea, targetName, false, holdTime, getScenario());


		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingCandidateAlarmCriteria() runs.");
		return true;
	}

	@Override
	public List<String> computeProblemEntity(Alarm a) throws Exception {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "computeProblemEntity()");
		}
		SyslogAlarm sla = (SyslogAlarm) a;

		List<String> problemEntities = new ArrayList<String>();

		// the problem entity is loop back ip if this is the bypass down alarm
		if(a.getCustomFieldValue(GFPFields.EVENT_KEY).equals(RPD_RSVP_BYPASS_DOWN)) 
		{
			// this is the RPD_RSVP_BYPASS_DOWN so we need the loopback IPs from topology
			List<String> iPs = sla.getPPHIps();
			for (String ip : iPs) {
				problemEntities.add(ip);
			}
			problemEntities.add(a.getOriginatingManagedEntity().split(" ")[1].split("/")[0]);	
		}
		else {
			problemEntities.add(a.getOriginatingManagedEntity().split(" ")[1]);	
		}


		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", computeProblemEntity() --- problemEntities=" + problemEntities);

		return problemEntities;
	} 

	@Override
	public boolean isMatchingSubAlarmCriteria(Alarm a, Group group)
			throws Exception {

		boolean ret = false;
		SyslogAlarm sla = (SyslogAlarm) a;
		SyslogAlarm trigger = (SyslogAlarm) group.getTrigger();

		if(a != trigger)
		{	
			// if is-contained-in-text ("PortLagId=<[the start_lag_id of PPH]", the component of E) or
			//    is-contained-in-text ("PortLagIp=<[the start_lag_ip of PPH]>", the component of E)
			String component = a.getCustomFieldValue(GFPFields.COMPONENT);

			if (log.isTraceEnabled()) {
				LogHelper.exit(log, "component " + component );
				LogHelper.exit(log, "Num of ids:" + trigger.getHopStartLagId().size());
				LogHelper.exit(log, "Num of ips:" + trigger.getHopStartLagIp().size());			
			}

			if(component.contains("PortLagId=<")) {
				List<String> lagIds = trigger.getHopStartLagId();
				for (String id : lagIds) {

					if (log.isTraceEnabled()) {
						LogHelper.exit(log, "id " + id );
					}

					if(component.contains("PortLagId=<" + id)) {
						ret = true;
						break;
					}
				}
			}

			if(component.contains("PortLagIp=<")) {
				if(!ret) {
					List<String> lagIps = trigger.getHopStartLagIp();
					for (String ip : lagIps) {
						if (log.isTraceEnabled()) {
							LogHelper.exit(log, "id " + ip );
						}

						if(component.contains("PortLagIp=<" + ip)) {
							ret = true;
							break;
						}
					}
				}
			}


		}

		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingSubAlarmCriteria() " + "[" + ret + "], group name = " + group.getName());

		return ret;

	}

	@Override
	public void whatToDoWhenSubAlarmIsAttachedToGroup(Alarm a, Group group)
			throws Exception {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "whatToDoWhenSubAlarmIsAttachedToGroup()");
		}
		EnrichedAlarm trigger = (EnrichedAlarm) group.getTrigger();

		if(a.getCustomFieldValue(GFPFields.EVENT_KEY).equals(JUNIPER_lINKDOWN)) {
			trigger.setSuppressed(true);

			log.info("Event: " + trigger.getCustomFieldValue(GFPFields.SEQNUMBER) + " | Supressing this event due to active LinkDown hop interface in this tunnel. Down interface component info: "
					+ a.getCustomFieldValue(GFPFields.COMPONENT)); 
		}  else {

			// if the device_type of start-dev = "BR" or the device_type of start-dev = "ABR" then
			//	Log "Event: [the sequence-number of M] | Supressing the correlated LinkDown on CRS device (that event seqnum is: [the sequence-number of E])"
			//	Suppress the crs1-LAG-LinkDown-event that you just found.
			//	Done
			if(a.getCustomFieldValue(GFPFields.EVENT_KEY).equals(CSR1_LAG_LINKDOWN)) {
				if(((SyslogAlarm) a).getDeviceType().equals("BR") || ((SyslogAlarm) a).getDeviceType().equals("ABR")) {
					log.info("alarm = " + ((SyslogAlarm) a).getIdentifier() + " is suppressed.");
					((SyslogAlarm) a).setSuppressed(true);
					log.info("Event: " + group.getTrigger().getCustomFieldValue(GFPFields.SEQNUMBER) + " suppressing the correlated LinkDown on CRS device (that event seqnum is: "
							+ a.getCustomFieldValue(GFPFields.SEQNUMBER));
				}
			}
		} 

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenSubAlarmIsAttachedToGroup()");
		}

	}


}
