package com.att.gfp.data.ipagAlarm;

//import com.att.gfp.enrichment.CDCAlarmBase;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipag.decomposition.Decomposer;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.localvariable.LocalVariable;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.scenario.ScenarioThreadLocal;
import com.hp.uca.expert.vp.pd.problem.Util;
import com.hp.uca.expert.vp.pd.services.PD_Service_Group;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;

public class EnrichedJuniperAlarm extends EnrichedAlarm {
	
	private static final String _50003_100_1 = "50003/100/1";
	private static final String F_DEVICEIPADDR = "deviceIpAddr";
	private static final String F_CRSFACINGPPORTCOUNT = "crsFacingPportCount";

	/**
	 * 
	 */
	private static final long serialVersionUID = -5734000362965235667L;

	private static Logger log = LoggerFactory.getLogger(EnrichedJuniperAlarm.class);
 
	//private String AafdaRole;
	//private String remotePortCLFI;
	//private String PortCLFI;
	private String containingPPort;
	//private String DiverseCkt;
	private String remotePortKey;
	private String peeringPort;
	//private String portLagId;
	private String lagIdFromAlarm;
	private String remoteDevice;
	private String PortCLFI2;
	private String PortCLFI2Plus;
	private String PortCLIF3List;
	private String deviceInstance;
	
	private Boolean isSent = false;
	private Boolean isClear = false;
	private Boolean inPool = false;   
	private Boolean isSubInterface = false;
	private Boolean redundantPPort = false;

	private Boolean triggerWatchSet;
	private int numberOfWatches;
	private int numberOfExpiredWatches;
	private Boolean primarySecondarySet;
	private String deviceIpAddr;		// tj: same as deviceInstance.
						// I reuse deviceInstance NOT
						// deviceIpAddr
	private int crsFacingPportCount;

	private HashSet<String> lagPportAidset = new HashSet<String>();
	// tj
	private HashSet<String> lagIdPportset = new HashSet<String>();
	private HashSet<String> remotePportset = new HashSet<String>();
	private HashSet<String> peerset = new HashSet<String>();
	private HashSet<String> pportset = new HashSet<String>();
	private HashSet<String> evcset = new HashSet<String>();
	
	private String pportInstance;
	// private String remoteDeviceType; // already defined in EnrichedAlarm
	//private String multinni;
	private String slavlan_nmvlan;
	//private String remoteDeviceIP;
	// tj: done

	
	
	public Boolean getTriggerWatchSet() {
		return triggerWatchSet;
	}


	public void setTriggerWatchSet(Boolean triggerWatchSet) {
		this.triggerWatchSet = triggerWatchSet;
	}


	/**
	 * EnrichedAlarm Constructor
	 */
	public EnrichedJuniperAlarm() {
		super();
	}

	
	/**
	 * EnrichedAlarm Copy Constructor.
	 * 
	 * This constructor constructs an Juniper Alarm and uses the Alarm provided as
	 * parameter to set all the standard alarm fields
	 * 
	 * @throws Exception 
	 */
	public EnrichedJuniperAlarm(EnrichedAlarm alarm) throws Exception {
		super(alarm);
		
		//setAafdaRole(null);
		//remotePortCLFI = null;
		//setDiverseCkt(null);
		//portLagId = null;

		inPool = false;
		isSent = false;
		isClear = false;
		isSubInterface = false;
		lagPportAidset = new HashSet<String>();
		lagIdPportset = new HashSet<String>();
		remotePportset = new HashSet<String>();
		peerset = new HashSet<String>();
		pportset = new HashSet<String>();
		evcset = new HashSet<String>();
		pportInstance = null;
		//multinni = null;
		slavlan_nmvlan = null;
		//remoteDeviceIP = null;
		remotePortKey = null;

		containingPPort = null;
		triggerWatchSet = false;
		remoteDevice = null;
		//PortCLFI = null;
		PortCLFI2 =  null;
		PortCLFI2Plus = null;
		PortCLIF3List = null;
		peeringPort = null;
		lagIdFromAlarm = null;
		deviceInstance = null;
		numberOfWatches = 0;
		numberOfExpiredWatches =0;
		deviceIpAddr = null;
		crsFacingPportCount = 0;
		primarySecondarySet = false;
		redundantPPort = false;

	}

	public EnrichedJuniperAlarm(Alarm alarm) throws Exception {
		super(alarm);
		
		//setAafdaRole(null);
		//remotePortCLFI = null;
		//setDiverseCkt(null);
		//portLagId = null;

		inPool = false;
		isSent = false;
		isClear = false;
		isSubInterface = false;
		lagPportAidset = new HashSet<String>();
		lagIdPportset = new HashSet<String>();
		remotePportset = new HashSet<String>();
		peerset = new HashSet<String>();
		pportset = new HashSet<String>();
		evcset = new HashSet<String>();
		pportInstance = null;
		//multinni = null;
		slavlan_nmvlan = null;
		//remoteDeviceIP = null;
		remotePortKey = null;
		
		containingPPort = null;
		triggerWatchSet = false;
		remoteDevice = null;
		//PortCLFI = null;
		PortCLFI2 =  null;
		PortCLFI2Plus = null;
		PortCLIF3List = null;
		peeringPort = null;
		lagIdFromAlarm = null;
		deviceInstance = null;
		numberOfWatches = 0;
		numberOfExpiredWatches =0;
		deviceIpAddr = null;
		crsFacingPportCount = 0;
		primarySecondarySet = false;
		redundantPPort = false;
	}

	/**
	 * Clones the provided Extended Alarm object
	 * @return a copy of the current object
	 * @throws CloneNotSupportedException
	 */
	@Override
	public EnrichedAlarm clone() throws CloneNotSupportedException {
		EnrichedJuniperAlarm newAlarm = (EnrichedJuniperAlarm) super.clone();
		//newAlarm.AafdaRole = this.AafdaRole;
		//newAlarm.remotePortCLFI = this.remotePortCLFI;
		//newAlarm.DiverseCkt = this.DiverseCkt;
		//newAlarm.portLagId = this.portLagId;
		newAlarm.containingPPort = this.containingPPort;
		newAlarm.isSent = this.isSent;

		newAlarm.inPool = this.inPool;
		newAlarm.isClear = this.isClear;
		newAlarm.lagPportAidset = this.lagPportAidset;
		newAlarm.lagIdPportset = this.lagIdPportset;
		newAlarm.remotePportset = this.remotePportset;
		newAlarm.peerset = this.peerset;
		newAlarm.pportset = this.pportset;
		newAlarm.pportInstance = this.pportInstance;
		//newAlarm.multinni = this.multinni;
		newAlarm.slavlan_nmvlan = this.slavlan_nmvlan;
		//newAlarm.remoteDeviceIP = this.remoteDeviceIP;
		newAlarm.remotePortKey = this.remotePortKey;

		newAlarm.isSubInterface = this.isSubInterface;
		newAlarm.triggerWatchSet = this.triggerWatchSet;
		newAlarm.remoteDevice = this.remoteDevice;
		//newAlarm.PortCLFI = this.PortCLFI;
		newAlarm.PortCLFI2 =  this.PortCLFI2;
		newAlarm.PortCLFI2Plus = this.PortCLFI2Plus;
		newAlarm.PortCLIF3List = this.PortCLIF3List;
		newAlarm.numberOfWatches = this.numberOfWatches;
		newAlarm.numberOfExpiredWatches = this.numberOfExpiredWatches;
		newAlarm.peeringPort = this.peeringPort;
		newAlarm.lagIdFromAlarm = this.lagIdFromAlarm;
		newAlarm.deviceInstance = this.deviceInstance;
		newAlarm.deviceIpAddr = this.deviceIpAddr;
		newAlarm.crsFacingPportCount = this.crsFacingPportCount;
		newAlarm.primarySecondarySet  = this.primarySecondarySet;
		newAlarm.redundantPPort = this.redundantPPort;
		newAlarm.evcset = this.evcset;
		return newAlarm;
	}
	
	public Boolean IsSubInterface() {
		return isSubInterface;
	}

	public void setIsSubInterface(Boolean isSubInterface) {
		this.isSubInterface = isSubInterface;
	}

	public String getDeviceIpAddr() {
		return deviceIpAddr;
	}
	
	public void setDeviceIpAddr(String deviceIpAddr) {
		this.deviceIpAddr = deviceIpAddr;
	}

	public int getCrsFacingPportCount() {
		return crsFacingPportCount;
	}

	public void setCrsFacingPportCount(int crsFacingPportCount) {
		this.crsFacingPportCount = crsFacingPportCount;
	}

//	public String getRemotePportAafdaRole() {
//		return remotePortAafdaRole;
//	}
//
//
//	public void setRemotePportAafdaRole(String aafdaRole) {
//		this.remotePortAafdaRole = aafdaRole;
//	}


//	public String getRemotePortCLFI() {
//		return remotePortCLFI;
//	}
//
//
//	public void setRemotePortCLFI(String remotePortCLFI) {
//		this.remotePortCLFI = remotePortCLFI;
//	}


	public String getContainingPPort() {
		return containingPPort;
	}


	public void setContainingPPort(String containingPPort) {
		this.containingPPort = containingPPort;
	}


//	public String getRemotePortDiverseCkt() {
//		return remotePortDiverseCkt;
//	}
//
//
//	public void setRemotePortDiverseCkt(String diverseCkt) {
//		this.remotePortDiverseCkt = diverseCkt;
//	}
//

	public String getRemotePortKey() {
		return remotePortKey;
	}


	public void setRemotePortKey(String remotePortKey) {
		this.remotePortKey = remotePortKey;
	}

	public void expirationCallBack() {
		
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "expirationCallBack()", getIdentifier());
		}
		
		Scenario scenario = ScenarioThreadLocal.getScenario();

		// increment the number of watch expirations
		setNumberOfExpiredWatches(getNumberOfExpiredWatches() + 1);

		if (log.isTraceEnabled())
			log.trace("WATCHES ARE:" + getNumberOfExpiredWatches() + " - " + getNumberOfWatches());


		// if this is the last watch to expire
		if(getNumberOfExpiredWatches() == getNumberOfWatches()) {
			setNumberOfExpiredWatches(0);
			setNumberOfWatches(0);

			// get the groups where this alarm is present
			Collection<Group> groups = PD_Service_Group.getGroupsOfAnAlarm(scenario, this);

			// if this trigger alarm is suppressed then we go thru the groups and send out
			// all of the subalarms for each group that has this as the trigger
			if(this.isSuppressed()) {
				if (log.isTraceEnabled()) {
					log.trace(" ## this trigger is suppressed !!");
				}
				for (Group group : groups) {
					if(group.getTrigger() == this){
						// Send out the subalarms
						for (Alarm alarm : group.getAlarmList()) {
							// make sure it hasn't been sent yet, isn't the trigger and has not been
							// suppressed by another group
							// log.trace("##### sending subalarms: " + alarm.getIdentifier() );
							if(alarm != group.getTrigger())  {
								// if this is a link down then we send it to the next step, if not
								// we send it out of UCA
								if(alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50003_100_1))
									Util.whereToSendThenSend((EnrichedAlarm) alarm);
								else
									Util.whereToSendThenSend((EnrichedAlarm) alarm);
							}
						}
					}
				}							
			} else {
				// if this trigger is not suppressed then we know that none of the 
				// suppression correlations happened

				// is an AAF_DA problem
				for (Group group : groups) {
					if(group.getName().contains("AAF_DA_AlarmProcessing")) {
						
						if (log.isTraceEnabled())
							log.trace(" There is an AAF_DA group !!");
						
						for (Alarm alarm : group.getAlarmList()) {
							EnrichedJuniperAlarm a = (EnrichedJuniperAlarm) alarm;
							
							if (log.isTraceEnabled())
								log.trace(" Alarm to update and send out: " + alarm.getIdentifier());
							
							// do the final enrichment of the alarms in the group
							// there is an alarm on both links
							if(group.getVar().getBoolean("hasCE") && group.getVar().getBoolean("hasPE")) {

								a.setCustomFieldValue(GFPFields.INFO1, alarm.getCustomFieldValue(GFPFields.INFO1) +
											" RelatedCLLI=<" + a.getRemoteDeviceName() + "> " +
											" RelatedPortAID=<" + a.getRemotePortAid() + ">");	
								
								a.setPerceivedSeverity(PerceivedSeverity.CRITICAL);
							}
							
							Util.whereToSendThenSend(a);
						}
					}					
				}

				Util.whereToSendThenSend((EnrichedAlarm)this);
			}

		}
		
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "expirationCallBack()");
		}

	}

	public void bgpProblemCallback() {
		if (log.isTraceEnabled())
			log.trace("bgpProblemCallback(): Alarm " + identifier);
		
		if (!isSuppressed() && perceivedSeverity != PerceivedSeverity.CLEAR) {
			Collection<Group> groups = PD_Service_Group.getGroupsOfAnAlarm(this, (Group) null);
			if (groups.size() != 1) {
				if (log.isTraceEnabled())
					log.trace("Invalid group for alarm " + identifier);
			} else {
				for (Group group : groups) {
					if (log.isTraceEnabled())
						log.trace("bgpProblemCallback(): " + group.toFormattedString());
					int crsFacingPportDownCount = 0;
					for (Alarm a : group.getAlarmList()) {
						EnrichedJuniperAlarm subAlarm = (EnrichedJuniperAlarm) a;
						if (subAlarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50004/1/2") && 
							subAlarm.getDeviceIpAddr().equals(this.getRemoteDeviceIpaddr())) {
							log.info("Suppressing BGP Down event, sequence : " + this.getCustomFieldValue(GFPFields.SEQNUMBER) +
				           		" alarm : " + this.getIdentifier() + "; neighbor " + 
				           		this.getRemoteDeviceIpaddr() + 
				           		" has active snmp unreachable event.");
							this.setSuppressed(true);
							break;
						} else if (subAlarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50003_100_1) &&
							subAlarm.getDeviceIpAddr().equals(this.getDeviceIpAddr())) {
							crsFacingPportDownCount++;
						}
					}
					if (!this.isSuppressed() &&
						this.getDeviceType().equals("VR1")) {
						if (crsFacingPportDownCount > 0 && 
							crsFacingPportDownCount >= this.getCrsFacingPportCount()) {
							log.info("Suppressing this BGP event, sequence : " + this.getCustomFieldValue(GFPFields.SEQNUMBER) +
					           		" alarm "  + this.getIdentifier() + "| " + this.getDeviceIpAddr() +
					           		" because devType is vr1, and ALL " + this.getCrsFacingPportCount() +
					           		" crs PPorts are known to be Down."); 
							this.setSuppressed(true);
						}
					}
					if (!this.isSuppressed()) {
						if (log.isDebugEnabled())
							log.debug("==> Sending Alarm: " + this.toString());
						Util.whereToSendThenSend((EnrichedAlarm) this);
						setAlarmState(AlarmState.sent);
					}
				}
			}
		}
	}
	
	public void vpnInterfaceProblemCallback() {
		if (log.isTraceEnabled())
			log.trace("vpnInterfaceProblemCallback(): " + getIdentifier());
        
		boolean keep = false;
        
		Scenario scenario = ScenarioThreadLocal.getScenario();

		for (Group group : PD_Service_Group.getGroupsOfAnAlarm(scenario, this)) {
			if (group.getNumber() > 1 && group.getTrigger() == this) {
				keep = true;
			}
		}

		if (keep) {
			log.info("VPN interface correlation suppressing event " + getIdentifier() + " sequence #: " +
					this.getCustomFieldValue(GFPFields.SEQNUMBER));
			setSuppressed(true);
		} else {
			// DF : this code is not necessary
//			EnrichedAlarm a = null;
//			try {
//				a = new EnrichedAlarm(this);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
			if (log.isTraceEnabled())
				log.trace("vpnInterfaceProblemCallback: Cascading alarm to Juniper Completion: " + this.toString());
//			Util.whereToSendThenSend(a, false);
			Util.whereToSendThenSend(this);
		}
	}
	
	/*public String getPortLagId() {
		return portLagId;
	}


	public void setPortLagId(String portLagId) {
		this.portLagId = portLagId;
	}
*/

///
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
///
	public Boolean getIsClear() {
		return isClear;
	}

	public void setIsClear(Boolean isClear) {
		this.isClear = isClear;
	}
///
	public Boolean getCanSend() {
		if (log.isDebugEnabled())
			log.debug("alarm = " + getIdentifier() + ", isClear = " + isClear+ ", isSent = " + isSent+ ", isSuppressed = " + isSuppressed());
		return (!isClear && !isSent && !isSuppressed());
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
///

	public String getRemoteDevice() {
		return remoteDevice;
	}


	public void setRemoteDevice(String remoteDevice) {
		this.remoteDevice = remoteDevice;
	}


/*	public String getPortCLFI() {
		return PortCLFI;
	}


	public void setPortCLFI(String portCLFI) {
		PortCLFI = portCLFI;
	}
*/

	public String getPortCLFI2() {
		return PortCLFI2;
	}


	public void setPortCLFI2(String portCLFI2) {
		PortCLFI2 = portCLFI2;
	}


	public String getPortCLFI2Plus() {
		return PortCLFI2Plus;
	}


	public void setPortCLFI2Plus(String portCLFI2Plus) {
		PortCLFI2Plus = portCLFI2Plus;
	}


	public String getPortCLIF3List() {
		return PortCLIF3List;
	}


	public void setPortCLIF3List(String portCLIF3List) {
		PortCLIF3List = portCLIF3List;
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


	public void setPeeringPort(String peeringPort) {
		this.peeringPort = peeringPort;
	}


	public String getPeeringPort() {
		return peeringPort;
	}

	// tj
	public void setPportInstance(String pportInstance) {
		this.pportInstance = pportInstance;
	}
	public String getPportInstance() {
		return pportInstance;
	}
/*
	public void setRemoteDeviceType(String remoteDeviceType) {
		this.remoteDeviceType = remoteDeviceType;
	}
	public String getRemoteDeviceType() {
		return remoteDeviceType;
	}
*/
/*	public void setMultinni(String multinni) {
		this.multinni = multinni;
	}
	public String getMultinni() {
		return multinni;
	}*/
	
	public void setSlavlan_nmvlan(String slavlan_nmvlan) {
		this.slavlan_nmvlan = slavlan_nmvlan;
	}
	public String getSlavlan_nmvlan() {
		return slavlan_nmvlan;
	}
/*	public void setRemoteDeviceIP(String remoteDeviceIP) {
		this.remoteDeviceIP = remoteDeviceIP;
	}
	public String getRemoteDeviceIP() {
		return remoteDeviceIP;
	}*/
	
	
	public void setLagIdPport(String lagIdPport) {
		lagIdPportset.add(lagIdPport);
	}
	public HashSet<String> getLagIdPportset() {
		return lagIdPportset;
	}
	
	public void setLagPportAid(String lagIdPport) {
		lagPportAidset.add(lagIdPport);
	}
	public HashSet<String> getLagPportAidset() {
		return lagPportAidset;
	}

	public void addEvc(String evc)
	{
		evcset.add(evc);
	}

	public HashSet<String> getEVCSet() {
		return evcset;
	}

	
	public void addRemotePport(String remotePport)
	{
		remotePportset.add(remotePport);
	}
	public HashSet<String> getRemotePportset() {
		return remotePportset;
	}

	public void addPeer(String peer)
	{
		peerset.add(peer);
	}

	public HashSet<String> getPeerSet() {
		return peerset;
	}

	// setPportset() is not needed
	public HashSet<String> getPportset() {
		return pportset;
	}
	// tj: done

	public void setNumberOfExpiredWatches(int numberOfExpiredWatches) {
		this.numberOfExpiredWatches = numberOfExpiredWatches;
	}


	public String getlagIdFromAlarm() {
		return lagIdFromAlarm;
	}


	public void setComponentLagId(String lagIdFromAlarm) {
		this.lagIdFromAlarm = lagIdFromAlarm;
	}


	public String getDeviceInstance() {
		return deviceInstance;
	}


	public void setDeviceInstance(String deviceInstance) {
		this.deviceInstance = deviceInstance;
	}

//	public void doDecomposition() {
//		Util.whereToSendThenSend(this, true);
//	}
//

	public Boolean getPrimarySecondarySet() {
		return primarySecondarySet;
	}


	public void setPrimarySecondarySet(Boolean primarySecondarySet) {
		this.primarySecondarySet = primarySecondarySet;
	}
	public void simpleSendCallBack() {
		// when the callback is called by more than one problem groups
		// blow checking is required
		if (log.isTraceEnabled())
			log.trace("simpleSendCallBack() runs.");

		// increment the number of watch expirations
		setNumberOfExpiredWatches(getNumberOfExpiredWatches() + 1);
		if (log.isTraceEnabled())
			log.trace("WATCHES ARE:" + getNumberOfExpiredWatches() + " - " + getNumberOfWatches());
		// if this is the last watch to expire
		if(getNumberOfExpiredWatches() != getNumberOfWatches())
			return;
		setNumberOfExpiredWatches(0);
		setNumberOfWatches(0);
		String eventKey = this.getCustomFieldValue(GFPFields.EVENT_KEY);
		if (eventKey.equals("50002/100/19"))
			// send to syslog for further correlation
			Util.whereToSendThenSend(this);
		else
			Util.whereToSendThenSend(this);
	}


	public Boolean isRedundantPPort() {
		return redundantPPort;
	}


	public void setIsRedundantPPort(Boolean redundantPPort) {
		this.redundantPPort = redundantPPort;
	}


/*	public String getAafdaRole() {
		return AafdaRole;
	}


	public void setAafdaRole(String aafdaRole) {
		AafdaRole = aafdaRole;
	}


	public String getDiverseCkt() {
		return DiverseCkt;
	}


	public void setDiverseCkt(String diverseCkt) {
		DiverseCkt = diverseCkt;
	}*/

}
