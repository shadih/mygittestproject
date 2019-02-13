package com.hp.uca.expert.vp.pd.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.data.ipagAlarm.SyslogAlarm;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.StandardFields;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.alarm.AlarmUpdater;
import com.hp.uca.expert.alarm.AlarmUpdater.UsualVar;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.vp.pd.config.LongItem;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;
import com.hp.uca.expert.vp.pd.problem.Util;
import com.hp.uca.expert.vp.pd.services.PD_Service_Group;
import com.hp.uca.expert.x733alarm.AttributeChange;
import com.hp.uca.expert.x733alarm.NetworkState;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;

/**
 * @author MASSE
 * 
 */
public class JuniperSyslog_BFDOWN_Default extends JuniperSyslog_ProblemDefault implements
ProblemInterface {
	
	private static final String THRESHOLD_KEY = "SAVPNSITE-AGGREGATION-COUNT-THRESHOLD";
	private int myThreshold = 0;
	private long aging = 30000;

	private Logger log = LoggerFactory
			.getLogger(JuniperSyslog_BFDOWN_Default.class);

	public JuniperSyslog_BFDOWN_Default() {
		super();		
		setLog(LoggerFactory.getLogger(JuniperSyslog_BFDOWN_Default.class));
	}

	@Override
	public boolean isMatchingCandidateAlarmCriteria(Alarm a) throws Exception {
		boolean ret = false;
		SyslogAlarm ea = (SyslogAlarm) a;

		String localSiteId = ea.getLocalDevice_SavpnSiteID();
		String remoteSiteId = ea.getRemoteDevice_SavpnSiteID();

		if ((localSiteId != null && !localSiteId.isEmpty() ) && ( remoteSiteId !=null && !remoteSiteId.isEmpty()))
		{

			ret = true;
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
		}
		else {
			log.info("Alarm with Sequence number " + ea.getCustomFieldValue(GFPFields.SEQNUMBER) 
					+ "is missing Site ID information. Alarm will not be corellated with other BFDOWN alarms");
			Util.whereToSendThenSend(ea, false);
		}
		return ret;
	}
	

//	@Override
//	public boolean isMatchingSubAlarmCriteria(Alarm a,Group group) throws Exception {
//		boolean ret = false;
//		SyslogAlarm ea = (SyslogAlarm) a;
//		SyslogAlarm ta =  (SyslogAlarm) group.getTrigger();
//		String remoteSiteID = ta.getRemoteDevice_SavpnSiteID();
//		String localSiteID = ea.getLocalDevice_SavpnSiteID();
//		
//		if (remoteSiteID.equals(localSiteID) )
//			ret = true;
//		
//		//Long myAging = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger() + aging;
//		//Util.WDPool(ea, null, false, myAging, getScenario());
//		if (log.isTraceEnabled())
//			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingSubAlarmCriteria() " + "[" + ret + "], group name = " + group.getName());
//		return ret; 
//	} 

	@Override
	public boolean isMatchingTriggerAlarmCriteria(Alarm a) throws Exception {
		boolean ret = false;

		SyslogAlarm ea = (SyslogAlarm) a;
		String localSiteId = ea.getLocalDevice_SavpnSiteID();
		String remoteSiteId = ea.getRemoteDevice_SavpnSiteID();

		if ((localSiteId != null && !localSiteId.isEmpty() ) && ( remoteSiteId !=null && !remoteSiteId.isEmpty()))
		{
			// since neither alarm is used in any other correlation, we don't need to set this trigger watch dog.
			// The code is left here for historical sake.

			String watchdogDesc = "Sylog BFDOWN watch dog.";
			Long myAging = getProblemPolicy().getTimeWindow().getTimeWindowAfterTrigger() + 2000;   // plus 2 seconds

			Util.setTriggerWatch((SyslogAlarm)a, SyslogAlarm.class, "bfdownCallBack", myAging, watchdogDesc);
			ret = true;  
		}
		else {
			log.info("Alarm with Sequence number " + ea.getCustomFieldValue(GFPFields.SEQNUMBER) 
					+ "is missing Site ID information. Alarm will not be corellated with other BFDOWN alarms");
			Util.whereToSendThenSend(ea, false);
		}
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingTriggerAlarmCriteria() " + "[" + ret + "]");
		return ret;

	} 

	@Override
	public boolean isAllCriteriaForProblemAlarmCreation(Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			log.trace(
					"ENTER:  isAllCriteriaForProblemAlarmCreation() for group ["
							+ group.getTrigger().getIdentifier(), group.getName());
		}
		return false;
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
@Override
public boolean calculateIfProblemAlarmhasToBeCleared(Group group)
		throws Exception {
	boolean ret=false;
	log.trace(" calculateIfProblemAlarmhasToBeCleared - Enter");

	if ( group.getNumber() == 0 ){
		log.trace(" Clearing ProblemAlarm");

		Alarm pa= group.getProblemAlarm();
		if (  pa != null){
			if (log.isTraceEnabled())
				log.trace(pa.getIdentifier()+" is getting Cleared ");

			// Change the attributes in the alarm.

			List<AttributeChange> attributeChangesSC = new ArrayList<AttributeChange>();
			List<AttributeChange> attributeChangesAVC = new ArrayList<AttributeChange>();
			AttributeChange attributeChange = null;

			attributeChange = new AttributeChange();
			attributeChange.setName(StandardFields.NETWORK_STATE);
			attributeChange
			.setNewValue(NetworkState.CLEARED.toString());
			attributeChange.setOldValue(pa.getNetworkState()
					.toString());
			attributeChangesSC.add(attributeChange);

			// Change Severity

			attributeChange = new AttributeChange();
			attributeChange.setName(StandardFields.PERCEIVED_SEVERITY);
			attributeChange.setNewValue(PerceivedSeverity.CLEAR
					.toString());
			attributeChange.setOldValue(pa
					.getPerceivedSeverity().toString());
			attributeChangesAVC.add(attributeChange);

			//If State Change occurred
			if (!attributeChangesSC.isEmpty()) {
				AlarmUpdater.updateAlarmFromAttributesChanges(pa,
						UsualVar.StateChange, attributeChangesSC,
						System.currentTimeMillis());
				pa.setHasStateChanged(true);
			}

			// If Attribute change occurred
			if (!attributeChangesAVC.isEmpty()) {
				AlarmUpdater.updateAlarmFromAttributesChanges(pa,
						UsualVar.AVCChange , attributeChangesAVC,
						System.currentTimeMillis());
				pa.setHasAVCChanged(true);
			}
			ret=true;
		}
		
	}
	else
		log.trace("Alarms in Group "+ group.getName() +" is "+group.getNumber());
	
	log.trace(" calculateIfProblemAlarmhasToBeCleared - Exit");

	return ret;

}
	
	@Override
	public void whatToDoWhenSubAlarmIsCleared(Alarm subAlarm, Group group)
			throws Exception {
		log.trace(" whatToDoWhenSubAlarmIsCleared - Enter");

		Alarm pa= group.getProblemAlarm();
		if (  pa != null){
			if("YES".equalsIgnoreCase(subAlarm.getCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA))) {
				if (log.isTraceEnabled())
					log.trace(subAlarm.getIdentifier()+" is getting Cleared due to expired purge Interval. Not sending out to NOM ");
			} else {
				if (log.isTraceEnabled())
					log.trace(pa.getIdentifier()+" is getting Cleared ");

				// Change the attributes in the alarm.

				List<AttributeChange> attributeChangesSC = new ArrayList<AttributeChange>();
				List<AttributeChange> attributeChangesAVC = new ArrayList<AttributeChange>();
				AttributeChange attributeChange = null;

				attributeChange = new AttributeChange();
				attributeChange.setName(StandardFields.NETWORK_STATE);
				attributeChange
				.setNewValue(NetworkState.CLEARED.toString());
				attributeChange.setOldValue(pa.getNetworkState()
						.toString());
				attributeChangesSC.add(attributeChange);

				// Change Severity

				attributeChange = new AttributeChange();
				attributeChange.setName(StandardFields.PERCEIVED_SEVERITY);
				attributeChange.setNewValue(PerceivedSeverity.CLEAR
						.toString());
				attributeChange.setOldValue(pa
						.getPerceivedSeverity().toString());
				attributeChangesAVC.add(attributeChange);

				//If State Change occurred
				if (!attributeChangesSC.isEmpty()) {
					AlarmUpdater.updateAlarmFromAttributesChanges(pa,
							UsualVar.StateChange, attributeChangesSC,
							System.currentTimeMillis());
					pa.setHasStateChanged(true);
				}

				// If Attribute change occurred
				if (!attributeChangesAVC.isEmpty()) {
					AlarmUpdater.updateAlarmFromAttributesChanges(pa,
							UsualVar.AVCChange , attributeChangesAVC,
							System.currentTimeMillis());
					pa.setHasAVCChanged(true);
				}

				pa.setCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA, "NO");
				pa.setCustomFieldValue(GFPFields.SEQNUMBER, subAlarm.getCustomFieldValue(GFPFields.SEQNUMBER));
				Util.whereToSendThenSend((EnrichedAlarm) pa, false);
			}

		}

	}
	

	
	
	// Clear out all the SubAlarms in this group from WM now the PA has been cleared and
	// Sent out to AM.
	@Override
	public void whatToDoWhenProblemAlarmIsCleared ( Group group)
			throws Exception {
		log.trace(" whatToDoWhenProblemAlarmIsCleared - Enter");
		log.trace(group.getName() + " # of alarms" + group.getNumber());
		Collection<Alarm> alarms = group.getAlarmList();
		
		// Change the attributes in the alarm.

		List<AttributeChange> attributeChangesSC = new ArrayList<AttributeChange>();
		AttributeChange attributeChange = null;
		attributeChange = new AttributeChange();
		attributeChange.setName(StandardFields.NETWORK_STATE);
		attributeChange.setNewValue(NetworkState.CLEARED.toString());
		attributeChange.setOldValue(NetworkState.NOT_CLEARED.toString());
		attributeChangesSC.add(attributeChange);
		
		for (Alarm alarm : alarms) {

			if ( alarm.getNetworkState() == (NetworkState.NOT_CLEARED) ){
				
			if (log.isTraceEnabled())
				log.trace(alarm.getIdentifier()+" SubAlarm is getting Cleared ");
			
				AlarmUpdater.updateAlarmFromAttributesChanges(alarm,
						UsualVar.StateChange, attributeChangesSC,
						System.currentTimeMillis());
				alarm.setHasStateChanged(true);
			}

		}
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
