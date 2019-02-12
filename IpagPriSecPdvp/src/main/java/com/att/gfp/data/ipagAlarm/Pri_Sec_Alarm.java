package com.att.gfp.data.ipagAlarm;

/**
 * 
 */
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.helper.GFPFields;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.vp.pd.problem.Util;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.vp.pd.services.PD_Service_Group;
import com.hp.uca.expert.x733alarm.CustomField;
import com.hp.uca.expert.x733alarm.CustomFields;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;
import com.hp.uca.expert.vp.pd.problem.ProcessJuniperLD_LAG;
import com.hp.uca.expert.vp.pd.problem.ProcessLACPAlarm;
import com.hp.uca.expert.vp.pd.problem.ProcessOspfAlarms;


// @XmlRootElement
public class Pri_Sec_Alarm extends EnrichedAlarm {	

	//public static final long AGGR_ALARM_CORR_WINDOW = 120 * 1000;
	//public static final long TUNNEL_ALARM_AGING_WINDOW = 60 * 1000;

	private int bfDownThreshold;

	private Boolean isSent = false;
	private Boolean isClear = false;

	private HashSet<String> vrfset;

	private int numberOfWatches = 0;
	private int numberOfExpiredWatches = 0;
	private int groupsAsCandidate = 0;
	private int numberOfCandidateWatches = 0;
	private String ifIndex;
	//private int numHoursAdjusted = 0;
	//private String containingPPort;
	private String portLagId;
	private String deviceInstance;
	private String lagIdFromAlarm;
	private String deviceIpAddr = null;
	private boolean isLagSubInterfaceLinkDown = false;
	private long highestCandidateAgeingValue = 0;
	private boolean DST = false;

	/**
	 * 
	 */
	private static final long serialVersionUID = -184871297978421735L;	
	private static Logger log = LoggerFactory.getLogger(Pri_Sec_Alarm.class);


	/**
	 * EnrichedAlarm Constructor
	 */
	public Pri_Sec_Alarm() {
		super();

		// initialize the suppress value to not suppress for now 
		isSent = false;
		isClear = false;

		numberOfWatches = 0;
		numberOfExpiredWatches = 0;
		numberOfCandidateWatches = 0;
		//numHoursAdjusted = 0;
		//containingPPort = null;
		portLagId = null;
		deviceInstance = null;
		lagIdFromAlarm = null;
		deviceIpAddr = null;
		ifIndex = null;
		isLagSubInterfaceLinkDown = false;
		groupsAsCandidate = 0;
		highestCandidateAgeingValue = 0;
		DST = false;

	}

	/**
	 * EnrichedAlarm Copy Constructor.
	 * 
	 * This constructor constructs an Extended Alarm and uses the Alarm provided as
	 * parameter to set all the standard alarm fields
	 * @throws Exception 
	 */
	public Pri_Sec_Alarm(Alarm alarm) throws Exception {
		super(alarm);

		// initialize the suppress value to not suppress for now 

		isSent = false;
		isClear = false;

		numberOfWatches = 0;
		numberOfExpiredWatches = 0;
		numberOfCandidateWatches = 0;
		//numHoursAdjusted = 0;
		//containingPPort = null;
		portLagId = null;
		deviceInstance = null;
		lagIdFromAlarm = null;
		deviceIpAddr = null;
		isLagSubInterfaceLinkDown = false;
		groupsAsCandidate =0;
		ifIndex = null;
		highestCandidateAgeingValue = 0;
		DST = false;

	}


	public Pri_Sec_Alarm( Pri_Sec_Alarm alarm) throws Exception {
		this ( (Alarm) alarm );	

		isSent = alarm.isSent;
		isClear = alarm.isClear;

		numberOfWatches = alarm.numberOfWatches;
		numberOfExpiredWatches = alarm.numberOfExpiredWatches;
		//containingPPort = alarm.containingPPort;

		portLagId = alarm.portLagId;
		deviceInstance = alarm.deviceInstance;
		lagIdFromAlarm = alarm.lagIdFromAlarm;
		deviceIpAddr = alarm.deviceIpAddr;
		isLagSubInterfaceLinkDown = alarm.isLagSubInterfaceLinkDown;
		groupsAsCandidate = alarm.groupsAsCandidate; 
		numberOfCandidateWatches = alarm.numberOfCandidateWatches;
		ifIndex = alarm.ifIndex;
		highestCandidateAgeingValue = alarm.highestCandidateAgeingValue;
		//numHoursAdjusted = alarm.numHoursAdjusted;
		DST = alarm.DST;
	}


	/**
	 * Clones the provided Extended Alarm object
	 * @return a copy of the current object
	 * @throws CloneNotSupportedException
	 */
	@Override
	public Pri_Sec_Alarm clone() throws CloneNotSupportedException {
		Pri_Sec_Alarm newAlarm = (Pri_Sec_Alarm) super.clone();

		newAlarm.isSent = this.isSent;
		newAlarm.isClear = this.isClear;

		newAlarm.numberOfWatches = this.numberOfWatches;
		newAlarm.numberOfExpiredWatches = this.numberOfExpiredWatches;
		//newAlarm.containingPPort = this.containingPPort;
		newAlarm.portLagId = this.portLagId;
		newAlarm.deviceInstance = this.deviceInstance;
		newAlarm.lagIdFromAlarm = this.lagIdFromAlarm;

		newAlarm.deviceIpAddr = this.deviceIpAddr;
		newAlarm.isLagSubInterfaceLinkDown = this.isLagSubInterfaceLinkDown;
		newAlarm.groupsAsCandidate = this.groupsAsCandidate;
		newAlarm.numberOfCandidateWatches = this.numberOfCandidateWatches;
		newAlarm.ifIndex = this.ifIndex;
		newAlarm.highestCandidateAgeingValue = this.highestCandidateAgeingValue;
		//newAlarm.numHoursAdjusted = this.numHoursAdjusted;
		newAlarm.DST = this.DST;

		return newAlarm;
	}


	/**
	 * 
	 */


	//	public String getContainingPPort() {
	//		return containingPPort;
	//	}


	//	public void setContainingPPort(String containingPPort) {
	//		this.containingPPort = containingPPort;
	//	}

	public String getlagIdFromAlarm() {
		return lagIdFromAlarm;
	}


	public void setComponentLagId(String lagIdFromAlarm) {
		this.lagIdFromAlarm = lagIdFromAlarm;
	}



	///
	public Boolean getIsSent() {
		return isSent;
	}

	public void setIsSent(boolean isSent) {
		this.isSent = isSent;
	}
	///
	public Boolean getIsClear() {
		return isClear;
	}

	public void setIsClear(Boolean isClear) {
		this.isClear = isClear;
	}
	///
	public Boolean getCanSend() {
		log.info("getCanSend(): alarm = " + getIdentifier() + ", isClear = " + isClear+ ", isSent = " + isSent+ ", isSuppressed = " + isSuppressed());
		return (!isClear && !isSent && !isSuppressed());
	}
	///
	public Boolean getCanProcess() {
		log.info("getCanProcess(): alarm = " + getIdentifier() + ", isClear = " + isClear+ ", isSent = " + isSent+ ", isSuppressed = " + isSuppressed());
		return (!isClear && !isSuppressed());
	}

	public void setVRFSet(HashSet<String> vrfset)
	{
		this.vrfset = vrfset;
	}

	public HashSet<String> getVRFSet()
	{
		return vrfset;
	}

	public int getNumberOfWatches() {
		return numberOfWatches;
	}

	public void setNumberOfWatches(int numberOfWatches) {
		this.numberOfWatches = numberOfWatches;
	}

	public int getNumberOfExpiredWatches() {
		return numberOfExpiredWatches;
	}

	public void setNumberOfExpiredWatches(int numberOfExpiredWatches) {
		this.numberOfExpiredWatches = numberOfExpiredWatches;
	}
	///
	public void setSeverity(int severity) {
		switch(severity) {
		case 0:
			setPerceivedSeverity(PerceivedSeverity.CRITICAL);
			setSeverity(PerceivedSeverity.CRITICAL);
			break;
		case 1:
			setPerceivedSeverity(PerceivedSeverity.MAJOR);
			setSeverity(PerceivedSeverity.MAJOR);
			break;
		case 2:
			setPerceivedSeverity(PerceivedSeverity.MINOR);
			setSeverity(PerceivedSeverity.MINOR);
			break;
		case 3:
			setPerceivedSeverity(PerceivedSeverity.WARNING);
			setSeverity(PerceivedSeverity.WARNING);
			break;
		case 5:
			setPerceivedSeverity(PerceivedSeverity.INDETERMINATE);
			setSeverity(PerceivedSeverity.INDETERMINATE);
			break;
		case 4:
			setPerceivedSeverity(PerceivedSeverity.CLEAR);
			setSeverity(PerceivedSeverity.CLEAR);
			break;
		default:
			setPerceivedSeverity(PerceivedSeverity.INDETERMINATE);
			setSeverity(PerceivedSeverity.INDETERMINATE);
			break;
		}	
	}

	public int getBfDownThreshold() {
		return bfDownThreshold;
	}

	public void setBfDownThreshold(int bfDownThreshold) {
		this.bfDownThreshold = bfDownThreshold;
	}


	public void clearCustomFields(Pri_Sec_Alarm a)
	{
		CustomFields cflds = a.getCustomFields();
		List<CustomField> customFields = cflds.getCustomField();
		for (CustomField cf:customFields)
			a.setCustomFieldValue(cf.getName(), "");
	}

	public void simpleSendCallBack() {
		if (log.isTraceEnabled())
			log.trace("simpleSendCallBack() runs for " + this.getIdentifier());

		boolean groupProcessed = false;

		// increment the number of watch expirations
		setNumberOfExpiredWatches(getNumberOfExpiredWatches() + 1);
		if (log.isTraceEnabled()) 
			log.trace("WATCHES ARE:" + getNumberOfExpiredWatches() + " - " + getNumberOfWatches());

		// if all trigger watches for this alarm have expired then we can process the group/s
		// we don't have to wait for all of the candidate watches to expire
		if(getNumberOfExpiredWatches() == getNumberOfWatches())
			groupProcessed = processAlarms();

		// We can wait longer if all trigger and candidate watches have not expired yet
		if(getNumberOfExpiredWatches() != getNumberOfWatches() || getNumberOfCandidateWatches() > 0)
			return;

		setNumberOfExpiredWatches(0);
		setNumberOfWatches(0);


		// at this point all watches for this alarm have expired		
		if(!groupProcessed) {

			/*			Collection<Group> groups = PD_Service_Group.getGroupsWhereAlarmSetAsCandidateSubAlarmOrTrigger(this, null);
			if (log.isTraceEnabled())
				log.trace("Number of groups " + groups.size()); */

			// the alarm could be a trigger or subalarm for a group that is not processed here so we have to check for this and
			// not send the alarm if this method did not process it but it is a member of a group that has more 
			// then one alarm
			//if(!checkIfMember(groups)) {
			// its not a trigger for any group so send it
			// the candidate callback would send it out anyway but here we can preempt it
			Util.WhereToSendAndSend(this);
			//} 
			
			// to account for the case when correlation does not take place and a potential secondary is not correlated to a primary
			// we need to make sure this potential secondary or now a standalone alarm is sent out (since ProcessOspfAlarms.setPriSec() only sends 
			// out the alarm when correlation takes place
			// below will send out this un-correlated alarm when it's not the trigger for another group since if it's that trigger watch will send it out
			
			Collection<Group> groups = PD_Service_Group.getGroupsWhereAlarmSetAsCandidateSubAlarmOrTrigger(this, null);
			for (Group group : groups) {
				if(group.getName().contains("Ospf") && this == group.getTrigger()) {
					for (Alarm alarm : group.getAlarmList()) {
						if ( alarm != this ) {
							Pri_Sec_Alarm OspfSubAlarm = (Pri_Sec_Alarm) alarm;
							if ( OspfSubAlarm.getCustomFieldValue(GFPFields.SECONDARY_ALERT_ID).length() == 0 ) {
								boolean sendOspfSubAlarm = true;
								Collection<Group> OspfSubAlarmGroups = PD_Service_Group.getGroupsWhereAlarmSetAsCandidateSubAlarmOrTrigger(OspfSubAlarm, group);
								for (Group OspfSubAlarmGroup : OspfSubAlarmGroups) {
									if (OspfSubAlarm == OspfSubAlarmGroup.getTrigger() ) {
										sendOspfSubAlarm = false;
										break;
									}
								}
								if (sendOspfSubAlarm) {
									Util.WhereToSendAndSend(OspfSubAlarm);
								}
							}
						}
					}
				}
			}
		} 
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "simpleSendCallBack()");
		}
	}

	private boolean checkIfMember(Collection<Group> groups) {
		boolean foundGroup = false;
		for (Group group : groups) {
			// we find a group that this alarm is a part of and the group has more than one alarm 
			// we can rely on some other processing
			if(group.getNumber() > 1){
				foundGroup = true;
				if (log.isTraceEnabled())
					log.trace("This is a trigger for group " + group.getName());
				break;
			}
		}
		return foundGroup;
	}

	private boolean processAlarms() {
		boolean ret = false;

		Collection<Group> groups = PD_Service_Group.getGroupsWhereAlarmSetAsCandidateSubAlarmOrTrigger(this, null);

		for (Group group : groups) {
			// we find a group that this is the trigger
			if(group.getTrigger() == this && group.getNumber() > 1 ) {

				if(group.getName().contains("JuniperLD_OAM_LAG")) {

					if (log.isTraceEnabled())
						log.trace("Detected a JuniperLD_OAM_LAG group...");

					ret = true;
					ProcessJuniperLD_LAG thing1 = new ProcessJuniperLD_LAG(); 
					thing1.processJuniperLD_OAM_LAG(group);

					// send out all alarms for this group
					for (Alarm alarm : group.getAlarmList()) {
						Util.WhereToSendAndSend((EnrichedAlarm) alarm);		
					}

					break;

				} else if(group.getName().contains("JnxLacp")) {
					if (log.isTraceEnabled())
						log.trace("Detected a JnxLacp group...");

					Collection<Group> subGroups = PD_Service_Group
							.getGroupsWhereAlarmSetAsCandidateSubAlarmOrTrigger(
									this, group);

					log.trace("Number of subgroups : " + subGroups.size());

					//if (subGroups != null && !subGroups.isEmpty() && subGroups.size() > 1 && !PD_Service_Group.isLeadGroup(group, subGroups)) {

					if (subGroups != null && !subGroups.isEmpty() && subGroups.size() > 1)  {
						if (!PD_Service_Group.isLeadGroup(group, subGroups)) {


							//The current Group is not the leaderGroup (lower priority)
							log.info("Group [" + group.getName()
									+ "] is not the lead group ");
						} else {
							ret = true;
							ProcessLACPAlarm thing2 = new ProcessLACPAlarm(); 
							thing2.processLACPAlarm(group);

							// send out all subalarms for this group
							for (Alarm alarm : group.getAlarmList()) {
								Util.WhereToSendAndSend((EnrichedAlarm) alarm);		
							}
							break;

						}

					} else {
						ret = true;
						ProcessLACPAlarm thing2 = new ProcessLACPAlarm(); 
						thing2.processLACPAlarm(group);

						// send out all subalarms for this group
						for (Alarm alarm : group.getAlarmList()) {
							Util.WhereToSendAndSend((EnrichedAlarm) alarm);		
						}
						break;

					}
				} else if(group.getName().contains("Ospf")) {
					//ProcessOspfAlarms ospfAlarms = new ProcessOspfAlarms();
					ProcessOspfAlarms.processOspfProblem(group);
				}
			}
		}

		if (log.isTraceEnabled())
			log.trace("processAlarms() ends, return is " + ret);

		return ret;
	}

	public void candidateSendCallBack() {
		boolean sendAlarm = true;

		// when the callback is called by more than one candidate
		// send the candidate
		if (log.isTraceEnabled())
			log.trace("candidateSendCallBack() enter:  watches are: " + getNumberOfCandidateWatches());

		// decrement the number of watches
		if(getNumberOfCandidateWatches() != 0) {
			int num = getNumberOfCandidateWatches();
			setNumberOfCandidateWatches(num -1);
		}

		//		if (log.isTraceEnabled())
		//			log.trace("CANDIDATE WATCHES ARE:" + getNumberOfCandidateWatches() );

		// we will only send the alarm out here if there are no groups that have this alarm 
		// and contain more than one.   That means it will be handled when that gourp is processed.
		if(getNumberOfCandidateWatches() == 0 && getNumberOfExpiredWatches() == getNumberOfWatches()) {
			if (log.isTraceEnabled())
				log.trace("All watches have expired, checking for groups." );

			Collection<Group> groups = PD_Service_Group.getGroupsWhereAlarmSetAsCandidateSubAlarmOrTrigger(this, null);

			//			if (log.isTraceEnabled())
			//				log.trace("Number of groups found:" + groups.size() );

			for (Group group : groups) {
				//				if (log.isTraceEnabled())
				//					log.trace("Checking group: " + group.getName() + " Number in group = " + group.getNumber() );

				if(group.getNumber() >1 ) {
					if (log.isTraceEnabled())
						log.trace("Found a group with another alarm, will will not send the alarm here !!" );

					sendAlarm = false;
					break;
				}
			}
		} else
			sendAlarm = false;

		if(sendAlarm)
			Util.WhereToSendAndSend((EnrichedAlarm) this);
	}


	public String getPortLagId() {
		return portLagId;
	}

	public void setPortLagId(String portLagId) {
		this.portLagId = portLagId;
	}

	public String getDeviceInstance() {
		return deviceInstance;
	}

	public void setDeviceInstance(String deviceInstance) {
		this.deviceInstance = deviceInstance;
	}	

	public String getDeviceIpAddr() {
		return deviceIpAddr;
	}

	public void setDeviceIpAddr(String deviceIpAddr) {
		this.deviceIpAddr = deviceIpAddr;
	}

	public boolean isLagSubInterfaceLinkDown() {
		return isLagSubInterfaceLinkDown;
	}

	public void setLagSubInterfaceLinkDown(boolean isLagSubInterfaceLinkDown) {
		this.isLagSubInterfaceLinkDown = isLagSubInterfaceLinkDown;
	}

	public int getGroupsAsCandidate() {
		return groupsAsCandidate;
	}

	public void setGroupsAsCandidate(int groupsAsCandidate) {
		this.groupsAsCandidate = groupsAsCandidate;
	}

	public void setNumberOfCandidateWatches(int num) {
		this.numberOfCandidateWatches = num;
	}

	public int getNumberOfCandidateWatches() {
		return numberOfCandidateWatches;
	}

	public String getIfIndex() {
		return ifIndex;
	}

	public void setIfIndex(String ifIndex) {
		this.ifIndex = ifIndex;
	}

	public long getHighestCandidateAgeingValue() {
		return highestCandidateAgeingValue;
	}

	public void setHighestCandidateAgeingValue(
			long highestCandidateAgeingValue) {
		this.highestCandidateAgeingValue = highestCandidateAgeingValue;
	}

	public boolean isItTimeToCreateProblemAlarm(long delayWeWant) {
		boolean ret = false;

		Long now = System.currentTimeMillis();
		long delayHereSoFar = now - agingIimeIn;
		long delayWeNeedNow = delayWeWant - agingAccumulativeTime;

		if (log.isTraceEnabled()) {
			log.trace("We have delayed the problem alarm here so far: " + delayHereSoFar );
			log.trace("Delay we need now: " + delayWeNeedNow );
		}

		if(delayWeNeedNow <= delayHereSoFar)
			ret = true;

		return ret;
	}

	public void timeToLocal() {
		/*			String alarmTimeStr = getCustomFieldValue(GFPFields.FE_TIME_STAMP);
			long alarmTimeLng = Long.valueOf(alarmTimeStr);

			if (log.isTraceEnabled()) {
				log.trace("timeToLocal() : enter");
			}

			if (log.isTraceEnabled()) {
				log.trace("Original alarm time" + getAlarmRaisedTime());
			}

			Date date = new Date((long)alarmTimeLng*1000);
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTimeZone(TimeZone.getDefault());
			cal.setTime(date);
			try {
				if (log.isTraceEnabled())
					log.trace("Original Alarm Time: " + DatatypeFactory.newInstance().newXMLGregorianCalendar(cal));

				setAlarmRaisedTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(cal));

			} catch (DatatypeConfigurationException e) {
				e.printStackTrace();
			}				

			if (log.isTraceEnabled()) {
				log.trace("Modified alarm time" + getAlarmRaisedTime());
			}

			if (log.isTraceEnabled()) {
				log.trace("timeToLocal() : exit");
			}*/
	}

	public void timeBackToNormal() {
		/*			String alarmTimeStr = getCustomFieldValue(GFPFields.FE_TIME_STAMP);
			long alarmTimeLng = Long.valueOf(alarmTimeStr);

			if (log.isTraceEnabled()) {
				log.trace("timeBackToNormal() : enter ");
			}

			if (log.isTraceEnabled()) {
				log.trace("Modified alarm time" + getAlarmRaisedTime());
			}

			// convert fe time to alarm time
			Date date = new Date((long)alarmTimeLng*1000);
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTimeZone(TimeZone.getTimeZone("GMT"));

			cal.setTime(date);
			try {
				setAlarmRaisedTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(cal));
			} catch (DatatypeConfigurationException e) {
				e.printStackTrace();
			}

			if (log.isTraceEnabled()) {
				log.trace("Original alarm time" + getAlarmRaisedTime());
			}

			if (log.isTraceEnabled()) {
				log.trace("timeBackToNormal() : exit");
			}*/
	}
}
