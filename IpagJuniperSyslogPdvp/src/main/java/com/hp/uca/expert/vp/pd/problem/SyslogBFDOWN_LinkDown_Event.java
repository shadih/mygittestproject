/**
 * This Problem is empty and ready to define methods to customize this problem
 */
package com.hp.uca.expert.vp.pd.problem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.data.ipagAlarm.SyslogAlarm;
import com.att.gfp.helper.FilterTags;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.vp.pd.config.LongItem;
import com.hp.uca.expert.vp.pd.config.TimeWindow;
import com.hp.uca.expert.vp.pd.config.TimeWindowMode;
import com.hp.uca.expert.vp.pd.core.JuniperSyslog_ProblemDefault;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;
import com.hp.uca.expert.vp.pd.services.PD_Service_ProblemAlarm;

/**
 * @author MASSE
 * 
 */
public final class SyslogBFDOWN_LinkDown_Event extends JuniperSyslog_ProblemDefault implements
		ProblemInterface {
	
	private static final String THRESHOLD_KEY = "SAVPNSITE-AGGREGATION-COUNT-THRESHOLD";
	private int myThreshold = 0;
	private long aging = 30000;

	private Logger log = LoggerFactory
			.getLogger(SyslogBFDOWN_LinkDown_Event.class);

	public SyslogBFDOWN_LinkDown_Event() {
		super();		
		setLog(LoggerFactory.getLogger(SyslogBFDOWN_LinkDown_Event.class));
	}

	@Override
	public boolean isMatchingCandidateAlarmCriteria(Alarm a) throws Exception {
		SyslogAlarm ea = (SyslogAlarm) a;
		String eventKey = ea.getCustomFieldValue(GFPFields.EVENT_KEY);
		String targetName = null;

		Long myAging = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger() + aging;
		
		if (log.isTraceEnabled())
			log.trace("aging for syslog bfgdown = " + myAging);

		if("CORR".equalsIgnoreCase(a.getCustomFieldValue(GFPFields.REASON))) {
			log.info("CORRelated event: [" + a.getCustomFieldValue(GFPFields.SEQNUMBER) + " [" + 
					a.getOriginatingManagedEntity().split(" ")[1] + "] cat: [50004/1/12 processing: SyslogBFDOWN");
			Util.whereToSendThenSend((EnrichedAlarm) a, false);
		} else {
			Util.WDPool(ea, targetName, false, myAging, getScenario());
			if (log.isTraceEnabled())
				LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingCandidateAlarmCriteria() runs.");
		}
		return true;
	}
	

	@Override
	public boolean isMatchingSubAlarmCriteria(Alarm a,Group group) throws Exception {
		boolean ret = true;
		SyslogAlarm ea = (SyslogAlarm) a;
//		SyslogAlarm trigger = (SyslogAlarm) group.getTrigger();
//		String eventKey = ea.getCustomFieldValue(GFPFields.EVENT_KEY);
//		String searchText = eventKey + "-" + trigger.getRemoteDeviceIpaddr() + "/" + trigger.getLocalDevice_SavpnSiteID();
//		String targetName = null;
//		if(ea.getCustomFieldValue(GFPFields.ALERT_ID).contains(searchText)) {
//			ret = true; 
//		}
		Long myAging = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger() + aging;
		Util.WDPool(ea, null, false, myAging, getScenario());
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingSubAlarmCriteria() " + "[" + ret + "], group name = " + group.getName());
		return ret; 
	} 

	@Override
	public List<String> computeProblemEntity(Alarm a) throws Exception {

		//RemoteSavpnSiteID inside the managedObject
		//RemoteIP- from topology
		//LocalIP inside the managedObject
		//LocalSavpnSiteID - from topology
				
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "computeProblemEntity()");
		}

		List<String> problemEntities = new ArrayList<String>();
		SyslogAlarm alarm = (SyslogAlarm) a;

		// remote device - from topology
		String remoteDeviceIp = alarm.getRemoteDeviceIpaddr();
		if(remoteDeviceIp !=null && !remoteDeviceIp.isEmpty())
			problemEntities.add(alarm.getRemoteDeviceIpaddr());
		
		String localSiteId = alarm.getLocalDevice_SavpnSiteID();
		if(localSiteId != null && !localSiteId.isEmpty())
			problemEntities.add(localSiteId);

		String remoteSiteId = alarm.getRemoteDevice_SavpnSiteID();
		if( remoteSiteId !=null && !remoteSiteId.isEmpty())
			problemEntities.add(remoteSiteId);

		// here we add the local device and remote site or just the local device if no site
		String meInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		if(meInstance.contains("/")) {
			problemEntities.add(meInstance.split("/")[0]);
		} else
			problemEntities.add(meInstance);
				
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + alarm.getIdentifier() + ", computeProblemEntity() --- problemEntities=" + problemEntities);

		return problemEntities;
	}

	@Override
	public boolean isMatchingTriggerAlarmCriteria(Alarm a) throws Exception {
		boolean ret = false;

		// since neither alarm is used in any other correlation, we don't need to set this trigger watch dog.
		// The code is left here for historical sake.
		
		/*SyslogAlarm alarm = (SyslogAlarm) a;	
		String watchdogDesc = "RpdRsvpByPass watch dog.";
		Long myAging = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger() + 2000;   // plus 2 seconds
		
		Util.setTriggerWatch((SyslogAlarm)a, SyslogAlarm.class, "rpdRsvpByPassDownexpirationCallBack", myAging, watchdogDesc);*/
		ret = true;  

		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingTriggerAlarmCriteria() " + "[" + ret + "]");
		return ret;
	} 


	// This method is used to send alarms that have been attached to a group
	@Override
	public void whatToDoWhenSubAlarmIsAttachedToGroup(Alarm alarm, Group group) throws Exception {
		EnrichedAlarm a = (EnrichedAlarm) alarm;
		// we don't want to do the default behavior from our 'special' default
		
		// we are going to record the threshold in the trigger alarm for later use
		LogHelper.enter(log, "alarm = " + alarm.getIdentifier() + ", group = " + group.getName() + ", group # = " + group.getNumber() + ", whatToDoWhenSubAlarmIsAttachedToGroup()");

		if(alarm == group.getTrigger()) {
			//a.setSuppressed(true);
			((SyslogAlarm)alarm).setBfDownThreshold(getMyThreshold());
	
		}
	}

	@Override
	public void computeLongs() 
	{
		long value = 0; 
		if (getLongs() != null && getLongs().getLong() != null) {
			for (LongItem longItem : getLongs().getLong()) {
				if (longItem.getKey().equals(THRESHOLD_KEY)) {
					value = longItem.getValue();
					myThreshold = longToInt(value);
					
					if (log.isTraceEnabled())
						log.trace("SAVPNSITE-AGGREGATION-COUNT-THRESHOLD: = "+myThreshold);
					//Longs myTHRESHOLD = getLongs();
					if (log.isTraceEnabled())
						log.trace("*****myTHRESHOLD = "+myThreshold);
				}
			}
		}		 
	}

	private int getMyThreshold() {
		return myThreshold;
	}
	
	private int longToInt(Long myThreshold)
	{ Integer x=null;
	    try { 
	            x = Integer.valueOf(myThreshold.toString()); 
	        } catch(IllegalArgumentException e) { 
	               log.trace("print");
	        }
		return x;
	}

	
/*	@Override
	public TimeWindow computeTimeWindow(Alarm alarm) {
		if (log.isTraceEnabled()) {
			getLog().trace(
					"ENTER:  computeTimeWindow()",
					String.format(
							"of alarm [%s] in the context of ProblemContext [%s]",
							alarm.getIdentifier(), getProblemContext()
									.getName()));
		}

		TimeWindow timeWindow = new TimeWindow();
		// two hours
		long aggregateWindow = 7200000;
		// after is 2 seconds
		long after = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger();
		
		long now = System.currentTimeMillis()/1000;
		
		
		long timewindow = aggregateWindow + after - (now - (GFPUtil.retrieveFeTimeFromCustomFields(alarm))); 

		if (log.isTraceEnabled())
			log.trace("after time window = " + timewindow);

		timeWindow.setTimeWindowMode(TimeWindowMode.TRIGGER);

		// converting seconds to milliseconds
		timeWindow.setTimeWindowBeforeTrigger((long) -1); 
		timeWindow.setTimeWindowAfterTrigger(timewindow);
		aging = timewindow;
		String output = "";
		if (timeWindow.getTimeWindowMode() == TimeWindowMode.NONE) {
			output = String.format(" [TimeWindowMode=%s]",
					timeWindow.getTimeWindowMode());
		} else {
			output = String
					.format(" [TimeWindowMode=%s][TimeWindowAfterTrigger=%s][TimeWindowBeforeTrigger=%s]",
							timeWindow.getTimeWindowMode(),
							timeWindow.getTimeWindowAfterTrigger(),
							timeWindow.getTimeWindowBeforeTrigger());

		}
		if (log.isTraceEnabled())
			LogHelper.exit(log, "computeTimeWindow()", output);

		return timeWindow;
	}
	*/
}
