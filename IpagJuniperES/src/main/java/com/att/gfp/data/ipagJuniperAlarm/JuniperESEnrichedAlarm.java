package com.att.gfp.data.ipagJuniperAlarm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.hp.uca.expert.alarm.Alarm;

public class JuniperESEnrichedAlarm extends EnrichedAlarm {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5734000362965235667L;

	private static Logger log = LoggerFactory.getLogger(JuniperESEnrichedAlarm.class);


	private String CLFI;
	private String CLCI;
	private String CircuitId;
	private String BmpCLCI;
	//private String containingPPort;
	private boolean isFRU;
	private String VRFName;
	
	public boolean isFRU() {
		return isFRU;
	}


	public void setFRU(boolean isFRU) {
		this.isFRU = isFRU;
	}


	public boolean isVPN() {
		return isVPN;
	}


	public void setVPN(boolean isVPN) {
		this.isVPN = isVPN;
	}


	public boolean isLAG() {
		return isLAG;
	}


	public void setLAG(boolean isLAG) {
		this.isLAG = isLAG;
	}


	private boolean isVPN;
	private boolean isLAG;

	/**
	 * EnrichedAlarm Constructor
	 */
	public JuniperESEnrichedAlarm() {
		super();
	}

	
	/**
	 * EnrichedAlarm Copy Constructor.
	 * 
	 * This constructor constructs an Juniper Alarm and uses the Alarm provided as
	 * parameter to set all the standard alarm fields
	 * @throws Exception 
	 */
	public JuniperESEnrichedAlarm(EnrichedAlarm alarm) throws Exception {
		super(alarm);
		
		CLFI = null;
		CLCI = null;
		CircuitId = null;
		BmpCLCI = null;
		isFRU = false;
		isVPN = false;
		isLAG = false;
		VRFName = null;
		
		//containingPPort = null;

	}

	public JuniperESEnrichedAlarm(Alarm alarm) throws Exception {
		super(alarm);
		
		CLFI = null;
		CLCI = null;
		CircuitId = null;
		BmpCLCI = null;
		//containingPPort = null;
		VRFName = null;
		isFRU = false;
		isVPN = false;
		isLAG = false;


	}

	/**
	 * Clones the provided Extended Alarm object
	 * @return a copy of the current object
	 * @throws CloneNotSupportedException
	 */
	@Override
	public JuniperESEnrichedAlarm clone() throws CloneNotSupportedException {
		JuniperESEnrichedAlarm newAlarm = (JuniperESEnrichedAlarm) super.clone();
		
		newAlarm.CLFI = this.CLFI;
		newAlarm.CLCI = this.CLCI;
		newAlarm.CircuitId = this.CircuitId;
		newAlarm.BmpCLCI = this.BmpCLCI;
		//newAlarm.containingPPort = this.containingPPort;
		newAlarm.isFRU = this.isFRU;
		newAlarm.isVPN = this.isVPN;
		newAlarm.isLAG = this.isLAG;
		newAlarm.VRFName = this.VRFName;
	
		return newAlarm;
	}
	

//	public String getContainingPPort() {
//		return containingPPort;
//	}
//
//
//	public void setContainingPPort(String containingPPort) {
//		this.containingPPort = containingPPort;
//	}


	public String getCLFI() {
		return CLFI;
	}


	public void setCLFI(String portCLFI) {
		this.CLFI = portCLFI;
	}


	public String getCLCI() {
		return CLCI;
	}


	public void setCLCI(String portCLCI) {
		this.CLCI = portCLCI;
	}


	public String getCircuitId() {
		return CircuitId;
	}


	public void setCircuitId(String portCircuitId) {
		this.CircuitId = portCircuitId;
	}


	public String getBmpCLCI() {
		return BmpCLCI;
	}


	public void setBmpCLCI(String portBmpCLCI) {
		this.BmpCLCI = portBmpCLCI;
	}


	public String getVFRName() {
		return VRFName;
	}


	public void setVFRName(String vRFName) {
		VRFName = vRFName;
	}


}

