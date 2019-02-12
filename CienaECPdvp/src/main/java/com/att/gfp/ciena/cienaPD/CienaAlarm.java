package com.att.gfp.ciena.cienaPD;

import java.util.HashSet;

import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.data.ipagAlarm.AlarmState;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.helper.GFPFields;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.alarm.AlarmHelper;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;
import org.neo4j.graphdb.Node;

@XmlRootElement
public class CienaAlarm extends EnrichedAlarm {

	private static Logger log = LoggerFactory.getLogger(CienaAlarm.class);
	
	/**
	 * Alarm extension attributes
	 */
	// private AlarmState alarmState;
	// private int severity; 
	
	private String slavlan_nmvlan;
	private String sm_element;
	// private String multinni;
	// private String remote_devicetype;
	// private String remote_deviceip;
	private String remote_pport_key;
	private String eventKey;
	private String device_ip;
	// private String device_type;
	private String be_time_stamp;
	private String classification;
	private String instance;
	private String iClass;
	private String gevm_key;
	private boolean updateSeverity = false;
	private int aging = 0;
	// private boolean isPtpMpt = false;
	// private boolean isFBSPtp = false;
	private boolean isFBS_PtpMpt = false;
	private Node pportNode;
	private Node deviceNode;
	private Node evcNode;
	private HashSet<String> vrfSet = new HashSet<String>();
	private boolean unreachableAlarm2JuniperVP = false;

	private int numberOfWatches;
	private int numberOfExpiredWatches;
	private Boolean isSent = false;
	private Boolean isSent2DEC = false;
	private Boolean isClear = false;
	// private Boolean isSuppressed = false;	// already defined in EnrichedAlarm class
	private Boolean inPool = false;   

	/**
	 * CienaAlarm Constructor
	 */
	public CienaAlarm() {
		super();
	}

	/**
	 * CienaAlarm Copy Constructor.
	 * 
	 * This constructor constructs an Extended Alarm and uses the Alarm provided as
	 * parameter to set all the standard alarm fields
	 * @throws Exception 
	 */
	public CienaAlarm(Alarm alarm) throws Exception {
		super(alarm);

		// System.out.println("Incoming Alarm = " + alarm.toXMLString());
		// send to AM
		// alarmState = AlarmState.pending;
		setAlarmState(AlarmState.pending);
		eventKey = alarm.getCustomFieldValue(GFPFields.EVENT_KEY);
		// no aging for cold start 50002/100/58 as it doesn't have
		// clear alarm
		if (eventKey.equals("50002/100/14") || 
				eventKey.equals("50002/100/21") ||
				eventKey.equals("50004/1/3") ||
				eventKey.equals("50004/1/4"))
			// alarm is sent by watchdog after 75 seconds
			aging = 75000;	// in unit of msec
		
		// set the numeric version of the severity
		// setSeverity(alarm.getPerceivedSeverity());
		setSeverity(getSeverity());
		// setEventKey(eventKey);
		setBe_time_stamp(alarm.getCustomFieldValue("be_time_stamp"));
		classification = alarm.getCustomFieldValue(GFPFields.CLASSIFICATION);
		if (classification == null)
			classification = "";
		setClassification(classification);
		setInstance(getOriginatingManagedEntity().split(" ")[1]);
		setIClass(getOriginatingManagedEntity().split(" ")[0]);

		if (log.isInfoEnabled())
		log.info("severity = " + getSeverity() +
			  ", eventKey = " + eventKey +
			  ", be_time_stamp = " + be_time_stamp +
			  ", instance = " + instance);
	}

/*
	public AlarmState getAlarmState() {
		return this.alarmState;
	}

	public void setAlarmState(AlarmState alarmState) {
		this.alarmState = alarmState;
	}
*/

	public void setSlavlan_nmvlan(String slavlan_nmvlan) {
		this.slavlan_nmvlan = slavlan_nmvlan;
	}
	
	public String getSlavlan_nmvlan() {
		return this.slavlan_nmvlan;
	}

	public void setSm_element(String sm_element) {
		this.sm_element = sm_element;
	}
	
	public String getSm_element() {
		return this.sm_element;
	}

/*
	public void setMultinni(String multinni) {
		this.multinni = multinni;
	}
	
	public String getMultinni() {
		return this.multinni;
	}
*/

	public void setInstance(String instance) {
		this.instance = instance;
	}
	
	public String getInstance() {
		return this.instance;
	}

	public void setIClass(String iClass) {
		this.iClass = iClass;
	}
	
	public String getIClass() {
		return this.iClass;
	}

	public void setGevm_key(String gevm_key) {
		this.gevm_key = gevm_key;
	}
	
	public String getGevm_key() {
		return this.gevm_key;
	}

	public void setBe_time_stamp(String be_time_stamp) {
		this.be_time_stamp = be_time_stamp;
	}
	
	public String getBe_time_stamp() {
		return this.be_time_stamp;
	}

	public void setClassification(String classification) {
		this.classification = classification;
	}
	
	public String getClassification() {
		return this.classification;
	}

	public void setDevice_ip(String device_ip) {
		this.device_ip = device_ip;
	}
	
	public String getDevice_ip() {
		return this.device_ip;
	}

/*
	EnrichedAlarm already defined setDeviceType() for you to set
	public void setDevice_type(String device_type) {
		this.device_type = device_type;
	}
	
	public String getDevice_type() {
		return this.device_type;
	}
*/

	public void setRemote_pport_key(String remote_pport_key) {
		this.remote_pport_key = remote_pport_key;
	}
	
	public String getRemote_pport_key() {
		return this.remote_pport_key;
	}

	public String getEventKey() {
		return eventKey;
	}

	public void setEventKey(String eventKey) {

		this.eventKey = eventKey;
	}

	public boolean getUnreachableAlarm2JuniperVP() {
		return unreachableAlarm2JuniperVP;
	}

	public void setUnreachableAlarm2JuniperVP(boolean unreachableAlarm2JuniperVP) {

		this.unreachableAlarm2JuniperVP = unreachableAlarm2JuniperVP;
	}


/*
	public void setRemote_devicetype(String remote_devicetype) {
		this.remote_devicetype = remote_devicetype;
	}
	
	public String getRemote_devicetype() {
		return this.remote_devicetype;
	}
*/


/*
	public void setRemote_deviceip(String remote_deviceip) {
		this.remote_deviceip = remote_deviceip;
	}
	
	public String getRemote_deviceip() {
		return this.remote_deviceip;
	}
*/
	
	public void setUpdateSeverity(boolean updateSeverity) {
		this.updateSeverity = updateSeverity;
	}
	
	public boolean getUpdateSeverity() {
		return this.updateSeverity;
	}

	public int getAging() {
		return this.aging;
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

	public Boolean getInPool() {
		return inPool;
	}

	public void setInPool(Boolean inPool) {
		this.inPool = inPool;
	}
///
	public Boolean getIsSent() {
		return isSent;
	}

	public void setIsSent(boolean isSent) {
		this.isSent = isSent;
	}

	public Boolean getIsSent2DEC() {
		return isSent2DEC;
	}

	public void setIsSent2DEC(boolean isSent2DEC) {
		this.isSent2DEC = isSent2DEC;
	}

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
	public void setIsFBS_PtpMpt(boolean isFBS_PtpMpt) {
		this.isFBS_PtpMpt = isFBS_PtpMpt;
	}
	
	public boolean getIsFBS_PtpMpt() {
		return this.isFBS_PtpMpt;
	}

	public void setPportNode(Node pportNode) {
		this.pportNode = pportNode;
	}
	
	public Node getPportNode() {
		return this.pportNode;
	}

	public void setDeviceNode(Node deviceNode) {
		this.deviceNode = deviceNode;
	}
	
	public Node getDeviceNode() {
		return this.deviceNode;
	}

	public void setEvcNode(Node evcNode) {
		this.evcNode = evcNode;
	}
	
	public Node getEvcNode() {
		return this.evcNode;
	}

	// EVCNode instance = A/B	==> EVC=VRF=B
	public void addVrf(String vrf) {
		vrfSet.add(vrf);
	}
	
	public HashSet<String> getVrfSet() {
		return this.vrfSet;
	}

/*
	public void setIsPtpMpt(boolean isPtpMpt) {
		this.isPtpMpt = isPtpMpt;
	}
	
	public boolean getIsPtpMpt() {
		return this.isPtpMpt;
	}

	public void setIsFBSPtp(boolean isFBSPtp) {
		this.isFBSPtp = isFBSPtp;
	}
	
	public boolean getIsFBSPtp() {
		return this.isFBSPtp;
	}
*/
/*
	public void setSeverity(PerceivedSeverity severity) {
		int s;
	
		switch(severity) {
	    case CRITICAL:
	    	s=0;
	    	break;
	    case MAJOR:
	    	s=1;
	    	break;
	    case MINOR:
	    	s=2;
	    	break;
	    case WARNING:
	    	s=3;
	    	break;
	    case INDETERMINATE:
	    	s=5;
	    	break;
	    case CLEAR:
	    	s=4;
	    	break;
	    default:
	    	s=5;
	    	break;
		}	
		
	    this.severity = s;
	    setCustomFieldValue("severity", Integer.toString(s));
	}
*/

	public void setSeverity(int severity) {
		setCustomFieldValue("severity", Integer.toString(severity));
		switch(severity) {
	    case 0:
		setPerceivedSeverity(PerceivedSeverity.CRITICAL);
	    	break;
	    case 1:
		setPerceivedSeverity(PerceivedSeverity.MAJOR);
	    	break;
	    case 2:
		setPerceivedSeverity(PerceivedSeverity.MINOR);
	    	break;
	    case 3:
		setPerceivedSeverity(PerceivedSeverity.WARNING);
	    	break;
	    case 5:
		setPerceivedSeverity(PerceivedSeverity.INDETERMINATE);
	    	break;
	    case 4:
		setPerceivedSeverity(PerceivedSeverity.CLEAR);
	    	break;
	    default:
		setPerceivedSeverity(PerceivedSeverity.INDETERMINATE);
	    	break;
		}	
	}
/*
	public int getSeverity(){
		return severity;
	}
*/
	public void simpleSendCallBack() {
		// when the callback is called by more than one problem groups
		// blow checking is required
		log.info("simpleSendCallBack() runs.");

		// increment the number of watch expirations
		setNumberOfExpiredWatches(getNumberOfExpiredWatches() + 1);
		log.info("WATCHES ARE:" + getNumberOfExpiredWatches() + " - " + getNumberOfWatches());
		// if this is the last watch to expire
		if(getNumberOfExpiredWatches() != getNumberOfWatches())
			return;
		setNumberOfExpiredWatches(0);
		setNumberOfWatches(0);
		String eventKey = this.getCustomFieldValue(GFPFields.EVENT_KEY);
		Util.sendAlarm(this, false);
	}
	
}
