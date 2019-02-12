package com.att.gfp.data.ipagAlarm;
//      com.att.gfp.data.ipagPreprocess.preprocess.EnrichedAlarm

/**
 * Extends Alarm class
 * 
 */

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger; 
import org.slf4j.LoggerFactory;

import com.att.gfp.data.config.DecomposeRulesConfiguration; 
import com.att.gfp.data.config.DecompseConfig;
import com.att.gfp.data.ipag.decomposition.Decomposer;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.alarm.AlarmHelper;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.scenario.Scenario; 
import com.hp.uca.expert.scenario.ScenarioThreadLocal;
import com.hp.uca.expert.vp.pd.services.PD_Service_Group;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;


@XmlRootElement
public class EnrichedAlarm extends Alarm { 
	  
	public static final String CE = "CE";
	public static final String PE = "PE";
	/**  
	 * 	
	 */
	private static final String F_SUPPRESSED = "suppressed";
	private static final String F_ALARMSTATE = "alarmstate";
	private static final String F_TUNABLE = "tunable";
	private static final String F_NODETYPE = "nodeType";
	private static final String F_SEVERITY = "severity";
	private static final String F_DEVICETYPE = "devicetype";
	private static final String F_ALARMTARGETEXISTS = "alarmtargetexists";
	private static final String F_DEVICELEVELEXISTS = "devicelevelexists";
 	private static final String F_ORIGMECLASS = "origmeclass";
	private static final String F_DEVICENAME = "devicename";
	private static final String F_DEVICEMODEL = "devicemodel";
	private static final String F_PORTAID = "portaid";
	private static final String F_ISOPERATIONAL = "isoperational";
	//private static final String F_ALARMCLASSIFICATION = "alarmclassification";
	//private static final String F_ALARMDOMAIN = "alarmdomain";
	private static final String F_REMOTEDEVICEIPADDR = "remotedeviceipaddr";
	private static final String F_REMOTEDEVICETYPE = "remotedevicetype";
	private static final String F_REMOTEPORTAID = "remoteportaid";
	private static final String F_REMOTEPORTNUMBER = "remoteportnumber";
	private static final String F_INEFFECT = "ineffect";
	private static final String F_DATASOURCE = "datasource";
	private static final String F_MULTINNI = "multinni";
	private static final String F_MULTIUNI = "multiuni";
	private static final String F_SEQUENCENUMBER = "sequencenumber";
	private static final String F_REMOTEPPORTINSTANCENAME = "remotePportInstanceName";
	private static final String F_REMOTEDEVICEMODEL = "remoteDeviceModel";
	private static final String F_REMOTEDEVICENAME = "remoteDeviceName";
	private static final String F_LEGACYORGIND = "legacyOrgInd"; 
	private static final String F_PPORTINFOEXISTS = "pportInfoExists";
	private static final String F_REMOTEPPORTINFOEXISTS = "remotePPortInfoExists";
	private static final String F_UNINNI = "uninni"; 
	private static final String F_PPORTLEGACYORGIND = "pportLegacyOrgInd"; 
	private static final String F_CARDTYPE = "cardType";
	private static final String F_DS1CKTID = "ds1CktId";
	private static final String F_EVCNAME = "evcName";
	private static final String F_NVLANIDTOP = "nVlanIdTop";
	private static final String F_VLANID = "vlanId";
	private static final String F_DEVICESUBROLE = "deviceSubRole"; 
	private static final String F_REMOTEPPORTCLFI = "remotePportClfi";
	private static final String F_REMOTEPPORTDIVERSECKTID = "remotePportDiverseCktId";
	private static final String F_REMOTEPPORTAAFDAROLE = "remotePportAafdaRole";
	private static final String F_REMOTEPPORTPORTNUM = "remotePportPortNum";
	private static final String F_REMOTEPPORTKEY = "remotePportKey";
	private static final String F_ORIGINALIDENTIFIER = "originalIdentifier";
	private static final String F_SENDTOCDC = "sendToCdc"; 
	
   
	

	
	/**  
	 * 
	 */
	private static final long serialVersionUID = -5235137849600943223L;

	private static Logger log = LoggerFactory.getLogger(EnrichedAlarm.class);
	
	/**
	 * Alarm extension attributes
	 */
	private String tunable;        // the tunable...
	private Boolean suppressed;    // suppress this alarm if true
	private AlarmState alarmState;   // true if 
	private String nodeType;       // node type PE or CE
	private String deviceType;     // device type from topology
	private Boolean alarmTargetExists;  // does the device as targeted in the alarm exist in topo
	private Boolean deviceLevelExists;  // does the device level node exist in topology
	private String origMEClass;    // what is the original managed entity class (in case we change it)
	private String deviceName;    // the device name from topology
	private String deviceModel;    // the device model from topology
	private String portAid;        // the port aid from tolology (if the target is a pport)
	private Boolean operational; // is device operational from topology
	//private String alarmClassification; // from topology
	//private String alarmDomain;    // from topology
	private String remoteDeviceIpaddr; // REMOTE PPORT info from topo
	private String remoteDeviceType; //REMOTE PPORT info from topo
	private String remotePortAid; //REMOTE PPORT info from topo
	private String remotePortNumber; //REMOTE PPORT info from topo
	private String datasource; // data source from topo
	private String inEffect; // PPORT info from topo
	private String multiUni; // from topo
	private String multiNni; // from topo 
	private long sequenceNumber; // generated by me 
	private String remotePportInstanceName;// REMOTE PPORT info from topo
	private String remoteDeviceModel;
	private String remoteDeviceName;
	private String legacyOrgInd;
	int severity;                  // severity as a number
	private boolean pportInfoExists;//checks if the PPORT exists
	private boolean remotePPortInfoExists;//checks if the REMOTE PPORT exists
	private String uninni;
	private String pportLegacyOrgInd;
	private String cardType;
	private String ds1CktId; 
	private String evcName;
	private String nVlanIdTop;
	private String deviceSubRole;
	private String remotePportClfi;
	private String remotePportDiverseCktId;
	private String remotePportAafdaRole; 
	private String remotePportPortNum;
	private String remotePportKey;
	private boolean duplicate; 
	private String originalIdentifier;
	private boolean sendToCdc;
	private boolean passthru;
	private String containingPPort;
	private String lPortScpService;  
	private String cdcPportClfi; 
	private String remotePportName;
	private String localPeeringPort;
	private String remotePeeringPort;
	private String aafDaRole;
	private String diverseCircuitID;
	private String relatedCLLI;
	private String relatedPortAID;
	private Boolean decomposed;
	private List<EnrichedAlarm> decomposedAlarms;
	private List<String> redundantNNIPorts;
	private Boolean hasMis;
	private Boolean issendToCpeCdc;
	private String additionalCLFIInfo; 
	private Boolean remotePPortHasMis;
	private String correspondingPPortRemoteDeviceType;
	private String lportServiceName;
	private String localPortAid;
	private String hairPinIndicator;
	private String ptnii;
	private String remotePtnii;
	private String portLagId;
	private String nmvlan;
	private String slavlan;
	private String remoteDeviceIpAddrFromLocalPort;
	private String remoteDeviceTypeFromLocalPort;
	private String remotePePportInstanceName;
	private String remotePePportClfi;
	private String remotePePportDevieType;
	private String remotePePportPortAid;
	private String remotePePportPortNum; 
	private String evcNodeAlarmSource;
	private String evcNodeAcnaban;
	private String evcNodeVrfName;
	private String additionalClciInfo;
	private String lportClci;
	private String gcpDeviceType;
	private String deviceRole;
	private String isPurgeIntervalExpired;
	protected long agingIimeIn = 0; 
	protected long agingAccumulativeTime = 0;
 
	public void SetTimeIn(){
		// this method sets the time an alarm enters a scenario
		agingIimeIn = System.currentTimeMillis(); 
		
		if (log.isTraceEnabled())
			log.trace("SetTimein:  time in is:" + agingIimeIn);
	}
	
	public void SetAccumulativeTime() {
		// this method keeps track of the total time spent so far in all scenarios (aging)
		Long now = System.currentTimeMillis();  
		
		Long timeSpentHere = now - agingIimeIn;
		Long totalAgingNow = timeSpentHere + agingAccumulativeTime;
		
		
		if (log.isTraceEnabled()) {
			log.trace("SetAccumulativeTime:  time now is:" + now + " time in was:" + agingIimeIn + " time spent here is:" + timeSpentHere + " aging before this scenario:" + agingAccumulativeTime);
			log.trace("Total time of aging including this scenario is now:" + totalAgingNow);
		}
		
		agingAccumulativeTime = totalAgingNow;
	}
	
	public long TimeRemaining(long expectedAging) {
		// this method will return the aging time remaining given the total aging time expected
		
		if(expectedAging <= agingAccumulativeTime)
			return 0; 
		else
			return expectedAging - agingAccumulativeTime;	
	}
	
	public void SetAgingAccumulativeTime(long tm){
		agingAccumulativeTime = tm;
		
		if (log.isTraceEnabled())
			log.trace("SetAgingAccumulativeTime:  seting aging for JUnit testing:" + agingAccumulativeTime);
	}

	
	public String getIsPurgeIntervalExpired() {
		return isPurgeIntervalExpired;
	}

	public void setIsPurgeIntervalExpired(String isPurgeIntervalExpired) {
		this.isPurgeIntervalExpired = isPurgeIntervalExpired;
	}
	
	public String getDeviceRole() {
		return deviceRole;
	}

	public void setDeviceRole(String deviceRole) {
		this.deviceRole = deviceRole;
	}
	
	public String getGcpDeviceType() {
		return gcpDeviceType;
	}

	public void setGcpDeviceType(String gcpDeviceType) {
		this.gcpDeviceType = gcpDeviceType;
	}
	
	public String getLportClci() { 
		return lportClci;
	}

	public void setLportClci(String lportClci) {
		this.lportClci = lportClci;
	}
	
	public String getAdditionalClciInfo() {
		return additionalClciInfo;
	}

	public void setAdditionalClciInfo(String additionalClciInfo) {
		this.additionalClciInfo = additionalClciInfo;
	}
	
	public String getEvcNodeAlarmSource() {
		return evcNodeAlarmSource;
	}


	public void setEvcNodeAlarmSource(String evcNodeAlarmSource) {
		this.evcNodeAlarmSource = evcNodeAlarmSource;
	}


	public String getEvcNodeAcnaban() {
		return evcNodeAcnaban;
	}


	public void setEvcNodeAcnaban(String evcNodeAcnaban) {
		this.evcNodeAcnaban = evcNodeAcnaban;
	}


	public String getEvcNodeVrfName() {
		return evcNodeVrfName;
	}


	public void setEvcNodeVrfName(String evcNodeVrfName) {
		this.evcNodeVrfName = evcNodeVrfName;
	}
	
	
	public String getPortLagId() {
		return portLagId; 
	}


	public void setPortLagId(String portLagId) {
		this.portLagId = portLagId;
	}


	public String getNmvlan() {
		return nmvlan;
	}


	public void setNmvlan(String nmvlan) {
		this.nmvlan = nmvlan;
	}


	public String getSlavlan() {
		return slavlan;
	}


	public void setSlavlan(String slavlan) {
		this.slavlan = slavlan;
	}


	public String getRemoteDeviceIpAddrFromLocalPort() {
		return remoteDeviceIpAddrFromLocalPort;
	}


	public void setRemoteDeviceIpAddrFromLocalPort(
			String remoteDeviceIpAddrFromLocalPort) {
		this.remoteDeviceIpAddrFromLocalPort = remoteDeviceIpAddrFromLocalPort;
	}


	public String getRemoteDeviceTypeFromLocalPort() {
		return remoteDeviceTypeFromLocalPort;
	}


	public void setRemoteDeviceTypeFromLocalPort(
			String remoteDeviceTypeFromLocalPort) {
		this.remoteDeviceTypeFromLocalPort = remoteDeviceTypeFromLocalPort;
	}


	public String getRemotePePportInstanceName() {
		return remotePePportInstanceName;
	}


	public void setRemotePePportInstanceName(String remotePePportInstanceName) {
		this.remotePePportInstanceName = remotePePportInstanceName;
	}


	public String getRemotePePportClfi() {
		return remotePePportClfi;
	}


	public void setRemotePePportClfi(String remotePePportClfi) {
		this.remotePePportClfi = remotePePportClfi;
	}


	public String getRemotePePportDevieType() {
		return remotePePportDevieType;
	}


	public void setRemotePePportDevieType(String remotePePportDevieType) {
		this.remotePePportDevieType = remotePePportDevieType;
	}


	public String getRemotePePportPortAid() {
		return remotePePportPortAid;
	}


	public void setRemotePePportPortAid(String remotePePportPortAid) {
		this.remotePePportPortAid = remotePePportPortAid;
	}


	public String getRemotePePportPortNum() {
		return remotePePportPortNum;
	}


	public void setRemotePePportPortNum(String remotePePportPortNum) {
		this.remotePePportPortNum = remotePePportPortNum;
	}
	
	
	public String getPtnii() {
		return ptnii;
	}


	public void setPtnii(String ptnii) {
		this.ptnii = ptnii;
	}
	
	public String getHairPinIndicator() {
		return hairPinIndicator;
	}


	public void setHairPinIndicator(String hairPinIndicator) {
		this.hairPinIndicator = hairPinIndicator;
	}

	
	public String getLocalPortAid() {
		return localPortAid; 
	}


	public void setLocalPortAid(String localPortAid) {
		this.localPortAid = localPortAid;
	}


	public Boolean getIsCpeCdcEvent() {
		return isCpeCdcEvent;
	} 


	public void setIsCpeCdcEvent(Boolean isCpeCdcEvent) {
		this.isCpeCdcEvent = isCpeCdcEvent;
	}

	private Boolean isCpeCdcEvent;
	 
	  
	public String getLportServiceName() {
		return lportServiceName;
	}


	public void setLportServiceName(String lportServiceName) {
		this.lportServiceName = lportServiceName;
	}


	public String getCorrespondingPPortRemoteDeviceType() { 
		return correspondingPPortRemoteDeviceType;
	}


	public void setCorrespondingPPortRemoteDeviceType(
			String correspondingPPortRemoteDeviceType) {
		this.correspondingPPortRemoteDeviceType = correspondingPPortRemoteDeviceType;
	}


	public Boolean getRemotePPortHasMis() {
		return remotePPortHasMis;
	}


	public void setRemotePPortHasMis(Boolean remotePPortHasMis) {
		this.remotePPortHasMis = remotePPortHasMis;
	}


	public String getAdditionalCLFIInfo() {
		return additionalCLFIInfo;
	} 


	public void setAdditionalCLFIInfo(String additionalCLFIInfo) {
		this.additionalCLFIInfo = additionalCLFIInfo;
	}


	public Boolean getIssendToCpeCdc() {
		return issendToCpeCdc;
	}


	public void setIssendToCpeCdc(Boolean issendToCpeCdc) {
		this.issendToCpeCdc = issendToCpeCdc;
	}


	public Boolean getHasMis() {
		return hasMis;
	}


	public void setHasMis(Boolean hasMis) {
		this.hasMis = hasMis;
	}


	public List<String> getRedundantNNIPorts() { 
		return redundantNNIPorts;
	}


	public void setRedundantNNIPorts(List<String> redundantNNIPorts) {
		this.redundantNNIPorts = redundantNNIPorts;
	}


	public List<EnrichedAlarm> getDecomposedAlarms() {
		return decomposedAlarms;
	}


	public void setDecomposedAlarms(List<EnrichedAlarm> decomposedAlarms) {
		this.decomposedAlarms = decomposedAlarms;
	}


	public Boolean getDecomposed() {
		return decomposed;
	}


	public void setDecomposed(Boolean decomposed) {
		this.decomposed = decomposed;
	}
	
	
	public String getRelatedCLLI() {
		return relatedCLLI;
	}

	public void setRelatedCLLI(String relatedCLLI) {
		this.relatedCLLI = relatedCLLI;
	}

	public String getRelatedPortAID() {
		return relatedPortAID;
	}

	public void setRelatedPortAID(String relatedPortAID) {
		this.relatedPortAID = relatedPortAID;
	}

	public String getAafDaRole() {
		return aafDaRole;
	}

	public void setAafDaRole(String aafDaRole) {
		this.aafDaRole = aafDaRole;
	}

	public String getDiverseCircuitID() {
		return diverseCircuitID;
	}

	public void setDiverseCircuitID(String diverseCircuitID) {
		this.diverseCircuitID = diverseCircuitID;
	}

	public String getLocalPeeringPort() {
		return localPeeringPort;
	}

	public void setLocalPeeringPort(String localPeeringPort) {
		this.localPeeringPort = localPeeringPort;
	}

	public String getRemotePeeringPort() {
		return remotePeeringPort;
	}

	public void setRemotePeeringPort(String remotePeeringPort) {
		this.remotePeeringPort = remotePeeringPort;
	}

	public String getCdcPportClfi() {
		return cdcPportClfi;
	}

	public void setCdcPportClfi(String cdcPportClfi) {
		this.cdcPportClfi = cdcPportClfi;
	}

	public String getRemotePportName() {
		return remotePportName;
	}

	public void setRemotePportName(String remotePportName) {
		this.remotePportName = remotePportName;
	}

	public String getlPortScpService() {
		return lPortScpService;
	}

	public void setlPortScpService(String lPortScpService) {
		this.lPortScpService = lPortScpService;
	}

	public boolean isPassthru() { 
		return passthru; 
	}

	public String getContainingPPort() {
		return containingPPort;
	}

	public void setContainingPPort(String containingPPort) {
		this.containingPPort = containingPPort;
	}

	public void setPassthru(boolean passthru) {
		this.passthru = passthru;
	}

	public boolean isSendToCdc() {
		return sendToCdc;
	}

	public void setSendToCdc(boolean sendToCdc) {
		this.sendToCdc = sendToCdc;
	}

	public boolean isDuplicate() {
		return duplicate;
	}

	public void setDuplicate(boolean duplicate) {
		this.duplicate = duplicate;
	}

	public String getOriginalIdentifier() {
		return originalIdentifier;
	}

	public void setOriginalIdentifier(String originalIdentifier) {
		this.originalIdentifier = originalIdentifier;
	}

	public String getRemotePportKey() {
		return remotePportKey;
	}

	public void setRemotePportKey(String remotePportKey) {
		this.remotePportKey = remotePportKey;
	}

	public String getRemotePportClfi() {
		return remotePportClfi;
	}

	public void setRemotePportClfi(String remotePportClfi) {
		this.remotePportClfi = remotePportClfi;
	}

	public String getRemotePportDiverseCktId() {
		return remotePportDiverseCktId;
	}

	public void setRemotePportDiverseCktId(String remotePportDiverseCktId) {
		this.remotePportDiverseCktId = remotePportDiverseCktId;
	}

	public String getRemotePportAafdaRole() {
		return remotePportAafdaRole;
	}

	public void setRemotePportAafdaRole(String remotePportAafdaRole) {
		this.remotePportAafdaRole = remotePportAafdaRole;
	}

	public String getRemotePportPortNum() {
		return remotePportPortNum;
	}

	public void setRemotePportPortNum(String remotePportPortNum) {
		this.remotePportPortNum = remotePportPortNum;
	}

	public String getDeviceSubRole() {
		return deviceSubRole; 
	}

	public void setDeviceSubRole(String deviceSubRole) {
		this.deviceSubRole = deviceSubRole;
	}

	public String getnVlanIdTop() {
		return nVlanIdTop;
	}

	public void setnVlanIdTop(String nVlanIdTop) {
		this.nVlanIdTop = nVlanIdTop;
	}

	public String getVlanId() {
		return vlanId;
	}

	public void setVlanId(String vlanId) {
		this.vlanId = vlanId;
	}

	private String vlanId;
	
	public String getCardType() {
		return cardType;
	}

	public void setCardType(String cardType) {
		this.cardType = cardType;
	}

	public String getDs1CktId() {
		return ds1CktId;
	}

	public void setDs1CktId(String ds1CktId) {
		this.ds1CktId = ds1CktId;
	}

	public String getEvcName() {
		return evcName;
	}

	public void setEvcName(String evcName) {
		this.evcName = evcName;
	}

	public boolean isPportInfoExists() {
		return pportInfoExists; 
	}

	public String getUninni() {
		return uninni;
	}

	public void setUninni(String uninni) {
		this.uninni = uninni;
	}

	public String getPportLegacyOrgInd() {
		return pportLegacyOrgInd;
	}

	public void setPportLegacyOrgInd(String pportLegacyOrgInd) {
		this.pportLegacyOrgInd = pportLegacyOrgInd;
	}

	public void setPportInfoExists(boolean pportInfoExists) {
		this.pportInfoExists = pportInfoExists;
	}

	public boolean isRemotePPortInfoExists() {  
		return remotePPortInfoExists;
	}

	public void setRemotePPortInfoExists(boolean remotePPortInfoExists) {
		this.remotePPortInfoExists = remotePPortInfoExists;
	}

	public String getRemotePportInstanceName() {
		return remotePportInstanceName;
	}

	public void setRemotePportInstanceName(String remotePportInstanceName) {
		this.remotePportInstanceName = remotePportInstanceName;
	}

	public String getRemoteDeviceModel() {
		return remoteDeviceModel;
	}

	public void setRemoteDeviceModel(String remoteDeviceModel) {
		this.remoteDeviceModel = remoteDeviceModel;
	}

	public String getRemoteDeviceName() {
		return remoteDeviceName;
	}

	public void setRemoteDeviceName(String remoteDeviceName) {
		this.remoteDeviceName = remoteDeviceName;
	}

	public String getLegacyOrgInd() {
		return legacyOrgInd;
	}

	public void setLegacyOrgInd(String legacyOrgInd) {
		this.legacyOrgInd = legacyOrgInd;
	}

	
	// possible device subclasses for alarm targets
	public static enum DevSubClass {
	    PPORT, LPORT, SLOT, CARD, EVC;
	}

	/**
	 * EnrichedAlarm Constructor
	 */
	public EnrichedAlarm() {
		super();
	}
	
	/**
	 * EnrichedAlarm Copy Constructor.
	 * 
	 * This constructor constructs an Extended Alarm and uses the Alarm provided as
	 * parameter to set all the standard alarm fields
	 * @throws Exception 
	 */
	public EnrichedAlarm(Alarm alarm1) {
		super(alarm1);
  		
		if(alarm1 instanceof EnrichedAlarm) {
			EnrichedAlarm alarm = (EnrichedAlarm) alarm1;
			
//			try { 
//				BeanUtils.copyProperties(this, alarm);
//			} catch (IllegalAccessException e) {
//				e.printStackTrace();
//			} catch (InvocationTargetException e) { 
//				e.printStackTrace();
//			}
			tunable = alarm.tunable;        // the tunable...
			suppressed = alarm.suppressed;    // suppress this alarm if true
			alarmState = alarm.alarmState;   // true if 
			nodeType = alarm.nodeType;       // node type PE or CE
			deviceType = alarm.deviceType;     // device type from topology
			alarmTargetExists= alarm.alarmTargetExists;  // does the device as targeted in the alarm exist in topo
			deviceLevelExists = alarm.deviceLevelExists;  // does the device level node exist in topology
			origMEClass = alarm.origMEClass;    // what is the original managed entity class (in case we change it)
			deviceName = alarm.deviceName;    // the device name from topology
			deviceModel = alarm.deviceModel;    // the device model from topology
			portAid = alarm.portAid;        // the port aid from tolology (if the target is a pport) 
			operational = alarm.operational; // is device operational from topology
			remoteDeviceIpaddr = alarm.remoteDeviceIpaddr; // PPORT info from topo
			remoteDeviceType = alarm.remoteDeviceType; // PPORT info from topo
			remotePortAid = alarm.remotePortAid; // PPORT info from topo
			remotePortNumber = alarm.remotePortNumber; // PPORT info from topo
			datasource = alarm.datasource; // data source from topo
			inEffect = alarm.inEffect; // PPORT info from topo
			multiUni = alarm.multiUni; // from topo
			multiNni = alarm.multiNni; // from topo
			sequenceNumber = alarm.sequenceNumber; // generated by me		
			severity = alarm.severity;                  // severity as a number
			remoteDeviceModel = alarm.remoteDeviceModel;
			remoteDeviceName = alarm.remoteDeviceName; 
			legacyOrgInd =  alarm.legacyOrgInd;
			remotePportInstanceName = alarm.remotePportInstanceName;// REMOTE PPORT info from topo
			pportInfoExists = alarm.pportInfoExists;
			remotePPortInfoExists = alarm.remotePPortInfoExists;
			uninni = alarm.uninni;
			pportLegacyOrgInd = alarm.pportLegacyOrgInd;
			cardType = alarm.cardType;
			ds1CktId = alarm.ds1CktId;
			evcName = alarm.evcName;
			nVlanIdTop = alarm.nVlanIdTop;
			vlanId = alarm.vlanId;
			deviceSubRole = alarm.deviceSubRole;
			remotePportClfi = alarm.remotePportClfi;
			remotePportDiverseCktId = alarm.remotePportDiverseCktId;
			remotePportAafdaRole = alarm.remotePportAafdaRole;
			remotePportPortNum = alarm.remotePportPortNum;
			remotePportKey = alarm.remotePportKey;  
			duplicate = alarm.duplicate;
			originalIdentifier = alarm.originalIdentifier; 
			sendToCdc = alarm.sendToCdc;
			passthru = alarm.passthru;
			containingPPort = alarm.containingPPort;
			lPortScpService = alarm.lPortScpService;
			cdcPportClfi = alarm.cdcPportClfi;
			remotePportName = alarm.remotePportName;
			localPeeringPort = alarm.localPeeringPort;
			remotePeeringPort = alarm.remotePeeringPort;
			aafDaRole = alarm.aafDaRole;
			diverseCircuitID = alarm.diverseCircuitID;
			relatedCLLI = alarm.relatedCLLI;
			relatedPortAID = alarm.relatedPortAID;
			decomposed = alarm.decomposed;
			decomposedAlarms = alarm.decomposedAlarms;
			redundantNNIPorts = alarm.redundantNNIPorts;
			hasMis = alarm.hasMis;
			issendToCpeCdc = alarm.issendToCpeCdc;
			additionalCLFIInfo = alarm.additionalCLFIInfo;
			remotePPortHasMis = alarm.remotePPortHasMis;
			correspondingPPortRemoteDeviceType = alarm.correspondingPPortRemoteDeviceType;
			lportServiceName = alarm.lportServiceName;
			isCpeCdcEvent = alarm.isCpeCdcEvent;
			localPortAid = alarm.localPortAid;
			hairPinIndicator = alarm.hairPinIndicator;
			ptnii = alarm.ptnii;
			remotePtnii = alarm.remotePtnii;
			portLagId = alarm.portLagId;
			nmvlan = alarm.nmvlan;
			slavlan = alarm.slavlan;
			remoteDeviceIpAddrFromLocalPort = alarm.remoteDeviceIpAddrFromLocalPort;
			remoteDeviceTypeFromLocalPort = alarm.remoteDeviceTypeFromLocalPort;
			remotePePportInstanceName = alarm.remotePePportInstanceName;
			remotePePportClfi = alarm.remotePePportClfi;
			remotePePportDevieType = alarm.remotePePportDevieType;
			remotePePportPortAid = alarm.remotePePportPortAid;
			remotePePportPortNum = alarm.remotePePportPortNum;
			evcNodeAlarmSource = alarm.evcNodeAlarmSource;
			evcNodeAcnaban = alarm.evcNodeAcnaban;
			evcNodeVrfName = alarm.evcNodeVrfName;
			additionalClciInfo = alarm.additionalClciInfo;
			lportClci = alarm.lportClci;
			gcpDeviceType = alarm.gcpDeviceType;
			deviceRole = alarm.deviceRole;
			isPurgeIntervalExpired = alarm.isPurgeIntervalExpired;
			agingIimeIn = alarm.agingIimeIn;
			agingAccumulativeTime = alarm.agingAccumulativeTime;
			origMEClass = alarm1.getOriginatingManagedEntity().split(" ")[0];
			setSeverity(alarm.getPerceivedSeverity());
			setPE_CE(alarm);     
			 
		} else {
		
		
		// initialize the suppress value to not suppress for now 
		suppressed = false;
		
		// initialize is operational to default false
		operational = true;
		
		// initialize the original alarm target as existing
		alarmTargetExists = false;

		// initalize the device exists in topo
		deviceLevelExists = false;
		passthru = false;
		
		duplicate = false;
		sendToCdc = false;  
		decomposed = false;
		decomposedAlarms = null;
		redundantNNIPorts = null;
		hasMis = false;
		issendToCpeCdc = false;
		additionalCLFIInfo = null;
		remotePPortHasMis = false;
		isCpeCdcEvent = false;
		// set the state of the alarm as pending meaning it is not ready to be
		// forwarded to the next VP
		alarmState = AlarmState.pending;
		
		// set the numeric version of the severity
		setSeverity(alarm1.getPerceivedSeverity());
				
		// save the original ME class we later change it
		origMEClass = alarm1.getOriginatingManagedEntity().split(" ")[0];

		// determine if this is a PE or CE device
		setPE_CE(alarm1);     
		}
		
		// this number will allow identical alarms to be differentiated in the rules
		//sequenceNumber = System.currentTimeMillis();
	}
	

	public EnrichedAlarm( EnrichedAlarm alarm) {

		this ((Alarm)alarm);
		 
		
		tunable = alarm.tunable;        // the tunable...
		suppressed = alarm.suppressed;    // suppress this alarm if true
		alarmState = alarm.alarmState;   // true if 
		nodeType = alarm.nodeType;       // node type PE or CE
		deviceType = alarm.deviceType;     // device type from topology
		alarmTargetExists= alarm.alarmTargetExists;  // does the device as targeted in the alarm exist in topo
		deviceLevelExists = alarm.deviceLevelExists;  // does the device level node exist in topology
		origMEClass = alarm.origMEClass;    // what is the original managed entity class (in case we change it)
		deviceName = alarm.deviceName;    // the device name from topology
		deviceModel = alarm.deviceModel;    // the device model from topology
		portAid = alarm.portAid;        // the port aid from tolology (if the target is a pport)
		operational = alarm.operational; // is device operational from topology
		remoteDeviceIpaddr = alarm.remoteDeviceIpaddr; // PPORT info from topo
		remoteDeviceType = alarm.remoteDeviceType; // PPORT info from topo
		remotePortAid = alarm.remotePortAid; // PPORT info from topo
		remotePortNumber = alarm.remotePortNumber; // PPORT info from topo
		datasource = alarm.datasource; // data source from topo
		inEffect = alarm.inEffect; // PPORT info from topo
		multiUni = alarm.multiUni; // from topo
		multiNni = alarm.multiNni; // from topo
		sequenceNumber = alarm.sequenceNumber; // generated by me		
		severity = alarm.severity;                  // severity as a number
		remoteDeviceModel = alarm.remoteDeviceModel;
		remoteDeviceName = alarm.remoteDeviceName; 
		legacyOrgInd =  alarm.legacyOrgInd;
		remotePportInstanceName = alarm.remotePportInstanceName;// REMOTE PPORT info from topo
		pportInfoExists = alarm.pportInfoExists;
		remotePPortInfoExists = alarm.remotePPortInfoExists;
		uninni = alarm.uninni;
		pportLegacyOrgInd = alarm.pportLegacyOrgInd;
		cardType = alarm.cardType;
		ds1CktId = alarm.ds1CktId;
		evcName = alarm.evcName;
		nVlanIdTop = alarm.nVlanIdTop;
		vlanId = alarm.vlanId;
		deviceSubRole = alarm.deviceSubRole;
		remotePportClfi = alarm.remotePportClfi;
		remotePportDiverseCktId = alarm.remotePportDiverseCktId;  
		remotePportAafdaRole = alarm.remotePportAafdaRole;
		remotePportPortNum = alarm.remotePportPortNum;
		remotePportKey = alarm.remotePportKey;  
		duplicate = alarm.duplicate;
		originalIdentifier = alarm.originalIdentifier;
		sendToCdc = alarm.sendToCdc;
		passthru = alarm.passthru;
		containingPPort = alarm.containingPPort;
		lPortScpService = alarm.lPortScpService;
		cdcPportClfi = alarm.cdcPportClfi;
		remotePportName = alarm.remotePportName;
		localPeeringPort = alarm.localPeeringPort;
		remotePeeringPort = alarm.remotePeeringPort;
		aafDaRole = alarm.aafDaRole;
		diverseCircuitID = alarm.diverseCircuitID;
		relatedCLLI = alarm.relatedCLLI;
		relatedPortAID = alarm.relatedPortAID;
		decomposed = alarm.decomposed;
		decomposedAlarms = alarm.decomposedAlarms;
		redundantNNIPorts = alarm.redundantNNIPorts;
		hasMis = alarm.hasMis;
		issendToCpeCdc = alarm.issendToCpeCdc;
		additionalCLFIInfo = alarm.additionalCLFIInfo;
		remotePPortHasMis = alarm.remotePPortHasMis;
		correspondingPPortRemoteDeviceType = alarm.correspondingPPortRemoteDeviceType;
		lportServiceName = alarm.lportServiceName;
		isCpeCdcEvent = alarm.isCpeCdcEvent;
		localPortAid = alarm.localPortAid;
		hairPinIndicator = alarm.hairPinIndicator;
		ptnii = alarm.ptnii;
		remotePtnii = alarm.remotePtnii;
		portLagId = alarm.portLagId;
		nmvlan = alarm.nmvlan;
		slavlan = alarm.slavlan;
		remoteDeviceIpAddrFromLocalPort = alarm.remoteDeviceIpAddrFromLocalPort;
		remoteDeviceTypeFromLocalPort = alarm.remoteDeviceTypeFromLocalPort;
		remotePePportInstanceName = alarm.remotePePportInstanceName;
		remotePePportClfi = alarm.remotePePportClfi;
		remotePePportDevieType = alarm.remotePePportDevieType;
		remotePePportPortAid = alarm.remotePePportPortAid;
		remotePePportPortNum = alarm.remotePePportPortNum;
		evcNodeAlarmSource = alarm.evcNodeAlarmSource;
		evcNodeAcnaban = alarm.evcNodeAcnaban;
		evcNodeVrfName = alarm.evcNodeVrfName;
		additionalClciInfo = alarm.additionalClciInfo;
		lportClci = alarm.lportClci;
		gcpDeviceType = alarm.gcpDeviceType;
		deviceRole = alarm.deviceRole;
		isPurgeIntervalExpired = alarm.isPurgeIntervalExpired;
		agingIimeIn = alarm.agingIimeIn;
		agingAccumulativeTime = alarm.agingAccumulativeTime;
		origMEClass = alarm.getOriginatingManagedEntity().split(" ")[0];
		setSeverity(alarm.getPerceivedSeverity());
		setPE_CE(alarm);     
	}
	
	
	private void setPE_CE(Alarm alarm) {
		
		String source = "";
		if(alarm.getSourceIdentifier() != null) { 
			source = alarm.getSourceIdentifier().toLowerCase();
		}
		if(source == null || source.isEmpty()) {
			nodeType = PE;
		} 

		if(source.equalsIgnoreCase("adtran") || source.equalsIgnoreCase("ciena") || source.equalsIgnoreCase("infovista") || source.equalsIgnoreCase("arista")) 
			nodeType = CE;  
		else
			if(source.equalsIgnoreCase("ipag") || source.equalsIgnoreCase("juniper") || source.equalsIgnoreCase("cisco") || source.equalsIgnoreCase("pmoss"))
				nodeType = PE;  
		
	}

	/**
	 * Clones the provided Extended Alarm object
	 * @return a copy of the current object
	 * @throws CloneNotSupportedException
	 */
	@Override
	public EnrichedAlarm clone() throws CloneNotSupportedException {
		EnrichedAlarm newAlarm = (EnrichedAlarm) super.clone();
		newAlarm.tunable = this.tunable;
		newAlarm.severity = this.severity;
		newAlarm.nodeType = this.nodeType;
		newAlarm.suppressed = this.suppressed; 
		newAlarm.deviceType = this.deviceType;
		newAlarm.deviceLevelExists = this.deviceLevelExists;
		newAlarm.alarmTargetExists = this.alarmTargetExists;
		newAlarm.origMEClass = this.origMEClass;
		newAlarm.deviceName = this.deviceName;
		newAlarm.deviceModel = this.deviceModel;
		newAlarm.portAid = this.portAid;
		newAlarm.operational = this.operational; 
		//newAlarm.alarmClassification = this.alarmClassification;
		//newAlarm.alarmDomain = this.alarmDomain;
		newAlarm.remoteDeviceIpaddr = this.remoteDeviceIpaddr;
		newAlarm.remoteDeviceType = this.remoteDeviceType;
		newAlarm.remotePortAid = this.remotePortAid;
		newAlarm.remotePortNumber = this.remotePortNumber;
		newAlarm.datasource = this.datasource; 
		newAlarm.alarmState = this.alarmState;
		newAlarm.multiNni = this.multiNni;
		newAlarm.multiUni = this.multiUni;
		newAlarm.sequenceNumber = this.sequenceNumber;
		newAlarm.remoteDeviceModel = this.remoteDeviceModel;
		newAlarm.remoteDeviceName = this.remoteDeviceName; 
		newAlarm.legacyOrgInd =  this.legacyOrgInd;
		newAlarm.remotePportInstanceName = this.remotePportInstanceName;
		newAlarm.pportInfoExists = this.pportInfoExists;
		newAlarm.remotePPortInfoExists = this.remotePPortInfoExists;
		newAlarm.uninni = this.uninni;
		newAlarm.pportLegacyOrgInd = this.pportLegacyOrgInd;
		newAlarm.cardType = this.cardType;
		newAlarm.ds1CktId = this.ds1CktId;
		newAlarm.evcName = this.evcName;
		newAlarm.nVlanIdTop = this.nVlanIdTop;
		newAlarm.vlanId = this.vlanId;
		newAlarm.deviceSubRole = this.deviceSubRole;
		newAlarm.remotePportClfi = this.remotePportClfi;
		newAlarm.remotePportDiverseCktId = this.remotePportDiverseCktId;
		newAlarm.remotePportAafdaRole = this.remotePportAafdaRole;
		newAlarm.remotePportPortNum = this.remotePportPortNum;
		newAlarm.remotePportKey = this.remotePportKey;
		newAlarm.duplicate = this.duplicate;
		newAlarm.originalIdentifier = this.originalIdentifier;
		newAlarm.sendToCdc = this.sendToCdc; 
		newAlarm.passthru = this.passthru; 
		newAlarm.containingPPort = this.containingPPort;
		newAlarm.lPortScpService = this.lPortScpService;
		newAlarm.cdcPportClfi = this.cdcPportClfi;
		newAlarm.remotePportName = this.remotePportName;
		newAlarm.localPeeringPort = this.localPeeringPort;
		newAlarm.remotePeeringPort = this.remotePeeringPort;
		newAlarm.aafDaRole = this.aafDaRole;
		newAlarm.diverseCircuitID = this.diverseCircuitID;
		newAlarm.relatedCLLI = this.relatedCLLI;
		newAlarm.relatedPortAID = this.relatedPortAID; 
		newAlarm.decomposed = this.decomposed;
		newAlarm.decomposedAlarms = this.decomposedAlarms;
		newAlarm.redundantNNIPorts = this.redundantNNIPorts;
		newAlarm.hasMis = this.hasMis;
		newAlarm.issendToCpeCdc = this.issendToCpeCdc;
		newAlarm.additionalCLFIInfo = this.additionalCLFIInfo;
		newAlarm.remotePPortHasMis = this.remotePPortHasMis;
		newAlarm.correspondingPPortRemoteDeviceType = this.correspondingPPortRemoteDeviceType;
		newAlarm.lportServiceName = this.lportServiceName;
		newAlarm.isCpeCdcEvent = this.isCpeCdcEvent;
		newAlarm.localPortAid = this.localPortAid;
		newAlarm.hairPinIndicator = this.hairPinIndicator;
		newAlarm.ptnii = this.ptnii;
		newAlarm.remotePtnii = this.remotePtnii;
		newAlarm.portLagId = this.portLagId;
		newAlarm.nmvlan = this.nmvlan;
		newAlarm.slavlan = this.slavlan;
		newAlarm.remoteDeviceIpAddrFromLocalPort = this.remoteDeviceIpAddrFromLocalPort;
		newAlarm.remoteDeviceTypeFromLocalPort = this.remoteDeviceTypeFromLocalPort;
		newAlarm.remotePePportInstanceName = this.remotePePportInstanceName;
		newAlarm.remotePePportClfi = this.remotePePportClfi;
		newAlarm.remotePePportDevieType = this.remotePePportDevieType;
		newAlarm.remotePePportPortAid = this.remotePePportPortAid;
		newAlarm.remotePePportPortNum = this.remotePePportPortNum;
		newAlarm.evcNodeAlarmSource = this.evcNodeAlarmSource;
		newAlarm.evcNodeAcnaban = this.evcNodeAcnaban;
		newAlarm.evcNodeVrfName = this.evcNodeVrfName;
		newAlarm.additionalClciInfo = this.additionalClciInfo;
		newAlarm.lportClci = this.lportClci; 
		newAlarm.gcpDeviceType = this.gcpDeviceType;  
		newAlarm.deviceRole = this.deviceRole; 
		newAlarm.isPurgeIntervalExpired = this.isPurgeIntervalExpired; 
		newAlarm.agingIimeIn = this.agingIimeIn;
		newAlarm.agingAccumulativeTime = this.agingAccumulativeTime;
		
		return newAlarm; 
	}  

		
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.uca.expert.alarm.AlarmCommon#toFormattedString()
	 */
	@Override
	public String toFormattedString() {
		StringBuffer toStringBuffer= AlarmHelper.toFormattedStringBuffer(this);
		
		AlarmHelper.addFormatedItem(toStringBuffer, F_SUPPRESSED, isSuppressed());
		AlarmHelper.addFormatedItem(toStringBuffer, F_TUNABLE, getTunable());
		AlarmHelper.addFormatedItem(toStringBuffer, F_NODETYPE, getNodeType());
		AlarmHelper.addFormatedItem(toStringBuffer, F_SEVERITY, getSeverity());
		AlarmHelper.addFormatedItem(toStringBuffer, F_DEVICETYPE, getDeviceType());
		AlarmHelper.addFormatedItem(toStringBuffer, F_DEVICELEVELEXISTS, getDeviceLevelExist());
		AlarmHelper.addFormatedItem(toStringBuffer, F_ALARMTARGETEXISTS, getAlarmTargetExist());
		AlarmHelper.addFormatedItem(toStringBuffer, F_ORIGMECLASS, getOrigMEClass());
		AlarmHelper.addFormatedItem(toStringBuffer, F_DEVICENAME, getDeviceName());
		AlarmHelper.addFormatedItem(toStringBuffer, F_DEVICEMODEL, getDeviceModel());
		AlarmHelper.addFormatedItem(toStringBuffer, F_PORTAID, getPortAid());
		AlarmHelper.addFormatedItem(toStringBuffer, F_ISOPERATIONAL, isOperational());
		//AlarmHelper.addFormatedItem(toStringBuffer, F_ALARMCLASSIFICATION, getAlarmClassification());
		//AlarmHelper.addFormatedItem(toStringBuffer, F_ALARMDOMAIN, getAlarmDomain());
		AlarmHelper.addFormatedItem(toStringBuffer, F_REMOTEDEVICEIPADDR, getRemoteDeviceIpaddr());
		AlarmHelper.addFormatedItem(toStringBuffer, F_REMOTEDEVICETYPE, getRemoteDeviceType());
		AlarmHelper.addFormatedItem(toStringBuffer, F_REMOTEPORTAID, getRemotePortAid());
		AlarmHelper.addFormatedItem(toStringBuffer, F_REMOTEPORTNUMBER, getRemotePortNumber());
		AlarmHelper.addFormatedItem(toStringBuffer, F_INEFFECT, getInEffect());
		AlarmHelper.addFormatedItem(toStringBuffer, F_DATASOURCE, getDataSource());
		AlarmHelper.addFormatedItem(toStringBuffer, F_ALARMSTATE, getAlarmState()); 
		AlarmHelper.addFormatedItem(toStringBuffer, F_MULTIUNI, getMultiUni());   
		AlarmHelper.addFormatedItem(toStringBuffer, F_MULTINNI, getMultiNni());
		AlarmHelper.addFormatedItem(toStringBuffer, F_SEQUENCENUMBER, getSequenceNumber());
		AlarmHelper.addFormatedItem(toStringBuffer, F_REMOTEPPORTINSTANCENAME , getRemotePportInstanceName());
		AlarmHelper.addFormatedItem(toStringBuffer, F_REMOTEDEVICEMODEL , getRemoteDeviceModel());
		AlarmHelper.addFormatedItem(toStringBuffer, F_REMOTEDEVICENAME , getRemoteDeviceName());
		AlarmHelper.addFormatedItem(toStringBuffer, F_LEGACYORGIND , getLegacyOrgInd()); 
		AlarmHelper.addFormatedItem(toStringBuffer, F_PPORTINFOEXISTS , isPportInfoExists()); 
		AlarmHelper.addFormatedItem(toStringBuffer, F_REMOTEPPORTINFOEXISTS , isRemotePPortInfoExists());  
		AlarmHelper.addFormatedItem(toStringBuffer, F_UNINNI , getUninni());    
		AlarmHelper.addFormatedItem(toStringBuffer, F_PPORTLEGACYORGIND , getPportLegacyOrgInd());    
		AlarmHelper.addFormatedItem(toStringBuffer, F_CARDTYPE , getCardType());    
		AlarmHelper.addFormatedItem(toStringBuffer, F_DS1CKTID , getDs1CktId());     
		AlarmHelper.addFormatedItem(toStringBuffer, F_EVCNAME , getEvcName());    
		AlarmHelper.addFormatedItem(toStringBuffer, F_NVLANIDTOP , getnVlanIdTop());    
		AlarmHelper.addFormatedItem(toStringBuffer, F_VLANID , getVlanId());    
		AlarmHelper.addFormatedItem(toStringBuffer, F_DEVICESUBROLE , getDeviceSubRole());     
		AlarmHelper.addFormatedItem(toStringBuffer, F_REMOTEPPORTCLFI , getRemotePportClfi());     
		AlarmHelper.addFormatedItem(toStringBuffer, F_REMOTEPPORTDIVERSECKTID , getRemotePportDiverseCktId());     
		AlarmHelper.addFormatedItem(toStringBuffer, F_REMOTEPPORTAAFDAROLE , getRemotePportAafdaRole());     
		AlarmHelper.addFormatedItem(toStringBuffer, F_REMOTEPPORTPORTNUM , getRemotePportPortNum());      
		AlarmHelper.addFormatedItem(toStringBuffer, F_REMOTEPPORTKEY , getRemotePportKey());       
		AlarmHelper.addFormatedItem(toStringBuffer, F_ORIGINALIDENTIFIER , getOriginalIdentifier());       
		AlarmHelper.addFormatedItem(toStringBuffer, F_SENDTOCDC , isSendToCdc());         

		return toStringBuffer.toString(); 
	}
	public String getMultiUni() {
		return this.multiUni;  
	}

	public void setMultiUni(String multiUni) {
		this.multiUni = multiUni;
	}
		
	public String getMultiNni() {
		return this.multiNni;
	}

	public void setMultiNni(String multiNni) {
		this.multiNni = multiNni;
	}
	
	   
	public AlarmState getAlarmState() {
		return this.alarmState;
	}

	public void setAlarmState(AlarmState alarmState) {
		this.alarmState = alarmState;
	}
	
	public String getDataSource() {
		return this.datasource;
	}
	
	public void setDataSource(String dataSource){
		this.datasource = dataSource;
	}

	public String getInEffect() {
		return this.inEffect;
	}

	public String getRemotePortNumber() {
		return this.remotePortNumber;
	}

	public String getRemotePortAid() {
		return this.remotePortAid;
	}

	public String getRemoteDeviceType() {
		return this.remoteDeviceType;
	}

	public String getRemoteDeviceIpaddr() {
		return this.remoteDeviceIpaddr;
	}

	public void setInEffect(String inEffect) {
		this.inEffect = inEffect;
	}

	public void setRemotePortNumber(String remotePortNumber) {
		this.remotePortNumber = remotePortNumber;
	}

	public void setRemotePortId(String remotePortAid) {
		this.remotePortAid = remotePortAid;
	}

	public void setRemoteDeviceType(String remoteDeviceType) {
		this.remoteDeviceType = remoteDeviceType;
	}

	public void setRemoteDeviceIpaddr(String remoteDeviceIpaddr) {
		this.remoteDeviceIpaddr = remoteDeviceIpaddr;
	}

	public Boolean isOperational() {  
		return operational;  
	}

	public void setOperational(Boolean isOperational) {
		this.operational = isOperational;
	}
	  
	public String getPortAid() {		
		return this.portAid;
	}

	public void setPortAid(String portAid) {
		this.portAid = portAid;
	}
	
	public String getDeviceModel() {
		return this.deviceModel;
	}

	public void setDeviceModel(String deviceModel) {
		this.deviceModel = deviceModel;
	}
	
	public String getDeviceName() {
		return this.deviceName;
	}
	
	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public String getOrigMEClass() {
		return this.origMEClass;
	}

	public void setOrigMEClass(String origMEClass) {
		this.origMEClass = origMEClass;
	}
	
	public Boolean getAlarmTargetExist() {
		return this.alarmTargetExists;
	}

	public void setAlarmTargetExists(Boolean alarmTargetExists) {
		this.alarmTargetExists = alarmTargetExists;
	}
	
	public Boolean getDeviceLevelExist() {
		return this.deviceLevelExists;
	}

	public void setDeviceLevelExists(Boolean deviceLevelExists) {
		this.deviceLevelExists = deviceLevelExists; 
	}
	
	
	
	public String getDeviceType() {
		return this.deviceType;
	}

	public void setDeviceType(String deviceType){
		this.deviceType = deviceType;
	}
	public String getTunable() {
		return tunable;
	}

	public void setTunable(String tunable) {

		this.tunable = tunable;
	}
	
	public boolean isSuppressed() {
		return suppressed;
	}
	
	public void setSuppressed(boolean suppressed) {
		this.suppressed = suppressed;
	}

	public String getNodeType() {
		return nodeType;
	}

	public void setNodeType(String nodeType) {
		this.nodeType = nodeType;
	}
	
	
	// record the int version of severity
	// TODO:  fix this mess
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
	}















	@Override
	public String toString() {
		return "EnrichedAlarm [tunable=" + tunable + ", suppressed="
				+ suppressed + ", alarmState=" + alarmState + ", nodeType="
				+ nodeType + ", deviceType=" + deviceType
				+ ", alarmTargetExists=" + alarmTargetExists
				+ ", deviceLevelExists=" + deviceLevelExists + ", origMEClass="
				+ origMEClass + ", deviceName=" + deviceName + ", deviceModel="
				+ deviceModel + ", portAid=" + portAid + ", operational="
				+ operational + ", remoteDeviceIpaddr=" + remoteDeviceIpaddr
				+ ", remoteDeviceType=" + remoteDeviceType + ", remotePortAid="
				+ remotePortAid + ", remotePortNumber=" + remotePortNumber
				+ ", datasource=" + datasource + ", inEffect=" + inEffect 
				+ ", multiUni=" + multiUni + ", multiNni=" + multiNni
				+ ", sequenceNumber=" + sequenceNumber
				+ ", remotePportInstanceName=" + remotePportInstanceName
				+ ", remoteDeviceModel=" + remoteDeviceModel
				+ ", remoteDeviceName=" + remoteDeviceName + ", legacyOrgInd="
				+ legacyOrgInd + ", severity=" + severity
				+ ", pportInfoExists=" + pportInfoExists
				+ ", remotePPortInfoExists=" + remotePPortInfoExists
				+ ", uninni=" + uninni + ", pportLegacyOrgInd="
				+ pportLegacyOrgInd + ", cardType=" + cardType + ", ds1CktId="
				+ ds1CktId + ", evcName=" + evcName + ", nVlanIdTop="
				+ nVlanIdTop + ", deviceSubRole=" + deviceSubRole
				+ ", remotePportClfi=" + remotePportClfi
				+ ", remotePportDiverseCktId=" + remotePportDiverseCktId
				+ ", remotePportAafdaRole=" + remotePportAafdaRole
				+ ", remotePportPortNum=" + remotePportPortNum
				+ ", remotePportKey=" + remotePportKey + ", duplicate="
				+ duplicate + ", originalIdentifier=" + originalIdentifier
				+ ", sendToCdc=" + sendToCdc + ", passthru=" + passthru
				+ ", containingPPort=" + containingPPort + ", lPortScpService="
				+ lPortScpService + ", cdcPportClfi=" + cdcPportClfi
				+ ", remotePportName=" + remotePportName
				+ ", localPeeringPort=" + localPeeringPort
				+ ", remotePeeringPort=" + remotePeeringPort + ", aafDaRole="
				+ aafDaRole + ", diverseCircuitID=" + diverseCircuitID
				+ ", relatedCLLI=" + relatedCLLI + ", relatedPortAID="
				+ relatedPortAID + ", decomposed=" + decomposed
				+ ", decomposedAlarms=" + decomposedAlarms
				+ ", redundantNNIPorts=" + redundantNNIPorts + ", hasMis="
				+ hasMis + ", issendToCpeCdc=" + issendToCpeCdc
				+ ", additionalCLFIInfo=" + additionalCLFIInfo
				+ ", remotePPortHasMis=" + remotePPortHasMis
				+ ", correspondingPPortRemoteDeviceType="
				+ correspondingPPortRemoteDeviceType + ", lportServiceName="
				+ lportServiceName + ", localPortAid=" + localPortAid
				+ ", hairPinIndicator=" + hairPinIndicator + ", ptnii=" + ptnii + ", remotePtnii=" + remotePtnii
				+ ", portLagId=" + portLagId + ", nmvlan=" + nmvlan 
				+ ", slavlan=" + slavlan + ", remoteDeviceIpAddrFromLocalPort="
				+ remoteDeviceIpAddrFromLocalPort
				+ ", remoteDeviceTypeFromLocalPort="
				+ remoteDeviceTypeFromLocalPort
				+ ", remotePePportInstanceName=" + remotePePportInstanceName
				+ ", remotePePportClfi=" + remotePePportClfi
				+ ", remotePePportDevieType=" + remotePePportDevieType
				+ ", remotePePportPortAid=" + remotePePportPortAid
				+ ", remotePePportPortNum=" + remotePePportPortNum
				+ ", deviceRole=" + deviceRole 
				+ ", isCpeCdcEvent=" + isCpeCdcEvent + ", vlanId=" + vlanId + ", isPurgeIntervalExpired=" + isPurgeIntervalExpired 
				+ "]";
	}


	public int getSeverity(){
		return severity;
	}
	
	public void setHealthTrapReasonText() {
		
		String reason = getCustomFieldValue(GFPFields.REASON);
		setCustomFieldValue(GFPFields.REASON, reason + "; CDM_IN=" + System.currentTimeMillis()/1000);
		
	}
 
	public Long getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(Long sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	/**
	 * This is a call back method that is invoked if there is no sub-alarm found for the trigger   
	 */
	public void expirationCallBack() {
		
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "expirationCallBack()", getIdentifier());
		}
		boolean keep = false;
		Scenario scenario = ScenarioThreadLocal.getScenario();
		for (Group group : PD_Service_Group.getGroupsOfAnAlarm(scenario, this)) {
			if(group.getNumber() > 1) {
				keep = true;
				break;  
			}
			if(group.getTrigger().isAboutToBeRetracted()) {
				keep = true;
				break;  
			}
		}
		if (!keep)  {
			// cascade alarm
			DecomposeRulesConfiguration decomposeConfig = new DecomposeRulesConfiguration();
			List<DecompseConfig.DecomposeRules.DecomposeRule> decomposeRules = decomposeConfig.getDecompseConfig().getDecomposeRules().getDecomposeRule();
			if(decomposeRules != null && decomposeRules.size() > 0) {
			for(DecompseConfig.DecomposeRules.DecomposeRule decomposeRule : decomposeRules) {   
				log.trace("EnrichedAlarm:expirationCallBack: Found decompose Rules");
				log.trace("EnrichedAlarm:expirationCallBack: Type = " + decomposeRule.getType());   
				log.trace("EnrichedAlarm:expirationCallBack: Order = " + decomposeRule.getOrder()); 
				if(decomposeRule.getEventNames().getEventName().contains(getCustomFieldValue(GFPFields.EVENT_KEY))) { 
					if("whileSendingTheEvent".equalsIgnoreCase(decomposeRule.getOrder())) {
					List<EnrichedAlarm> decomposedAlarms = null;
					try {
						decomposedAlarms = Decomposer.decompose(this);
					} catch (Exception e) {
						e.printStackTrace();            
					}
					if(decomposedAlarms != null && decomposedAlarms.size() > 0) {
						for(EnrichedAlarm enrichedAlrm : decomposedAlarms) {
							log.trace("EnrichedAlarm:expirationCallBack: sending decomposed alarm : " + enrichedAlrm.getIdentifier()); 
							GFPUtil.forwardOrCascadeAlarm(enrichedAlrm, AlarmDelegationType.FORWARD, null);
						}     
					}    
				} 
			}   
		}  
	}
			if("50001/100/1".equals(getCustomFieldValue(GFPFields.EVENT_KEY))) {

			}   
		log.trace("EnrichedAlarm:expirationCallBack: sending the EnrichedAlarm"); 
		GFPUtil.forwardOrCascadeAlarm(this, AlarmDelegationType.FORWARD, null);
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "retractionCallBack()");      
		}

		}
	}

	public String getRemotePtnii() {
		return remotePtnii;
	}

	public void setRemotePtnii(String remotePtnii) {
		this.remotePtnii = remotePtnii;
	}   
	
	
}
