package com.att.gfp.data.ipagJuniperAlarm;

//import com.att.gfp.enrichment.CDCAlarmBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.service_util;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.scenario.ScenarioThreadLocal;
import com.att.gfp.data.ipag.JunipertopoModel.IpagJuniperESTopoAccess;
import com.att.gfp.data.ipag.JunipertopoModel.NTDticket;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;

public class EnrichedNTDAlarm extends EnrichedAlarm {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5734000362965235667L;

	private static Logger log = LoggerFactory.getLogger(EnrichedNTDAlarm.class);


	private String PortCLFI;
//	private String containingPPort;
	private Boolean sentAsSubAlarm;
	private Boolean sentAsTriggerAlarm;
	private Boolean triggerWatchSet; 
	private String PortCLFI2;
	private String PortCLFI2Plus;
	private String PortCLFI3List;
	
	
	public Boolean getTriggerWatchSet() {
		return triggerWatchSet;
	}


	public void setTriggerWatchSet(Boolean triggerWatchSet) {
		this.triggerWatchSet = triggerWatchSet;
	}


	/**
	 * EnrichedAlarm Constructor
	 */
	public EnrichedNTDAlarm() {
		super();
	}

	
	/**
	 * EnrichedAlarm Copy Constructor.
	 * 
	 * This constructor constructs an Juniper Alarm and uses the Alarm provided as
	 * parameter to set all the standard alarm fields
	 * @throws Exception 
	 */
	public EnrichedNTDAlarm(EnrichedAlarm alarm) throws Exception {
		super(alarm);
		
		sentAsSubAlarm = false;
//		containingPPort = null;
		sentAsTriggerAlarm = false;
		triggerWatchSet = false;
		PortCLFI = null;
		PortCLFI2 =  null;
		PortCLFI2Plus = null;
		PortCLFI3List = null;

	}

	public EnrichedNTDAlarm(Alarm alarm) throws Exception {
		super(alarm);
		
		
		sentAsSubAlarm = false;
//		containingPPort = null;
		sentAsTriggerAlarm = false;
		triggerWatchSet = false;
		PortCLFI = null;
		PortCLFI2 =  null;
		PortCLFI2Plus = null;
		PortCLFI3List = null;

	}

	/**
	 * Clones the provided Extended Alarm object
	 * @return a copy of the current object
	 * @throws CloneNotSupportedException
	 */
	@Override
	public EnrichedNTDAlarm clone() throws CloneNotSupportedException {
		EnrichedNTDAlarm newAlarm = (EnrichedNTDAlarm) super.clone();
		newAlarm.sentAsSubAlarm = this.sentAsSubAlarm;		
//		newAlarm.containingPPort = this.containingPPort;
		newAlarm.sentAsTriggerAlarm = this.sentAsTriggerAlarm;
		newAlarm.triggerWatchSet = this.triggerWatchSet;
		newAlarm.PortCLFI = this.PortCLFI;
		newAlarm.PortCLFI2 =  this.PortCLFI2;
		newAlarm.PortCLFI2Plus = this.PortCLFI2Plus;
		newAlarm.PortCLFI3List = this.PortCLFI3List;


	
		return newAlarm;
	}
	

//	public String getContainingPPort() {
//		return containingPPort;
//	}


//	public void setContainingPPort(String containingPPort) {
//		this.containingPPort = containingPPort;
//	}

	public Boolean getSentAsSubAlarm() {
		return sentAsSubAlarm;
	}


	public void setSentAsSubAlarm(Boolean sentAsSubAlarm) {
		this.sentAsSubAlarm = sentAsSubAlarm;
	}


	public Boolean getSentAsTriggerAlarm() {
		return sentAsTriggerAlarm;
	}


	public void setSentAsTriggerAlarm(Boolean sentAsTriggerAlarm) {
		this.sentAsTriggerAlarm = sentAsTriggerAlarm;
	}

	public String getPortCLFI() {
		return PortCLFI;
	}


	public void setPortCLFI(String portCLFI) {
		PortCLFI = portCLFI;
	}


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


	public String getPortCLFI3List() {
		return PortCLFI3List;
	}


	public void setPortCLFI3List(String portCLFI3List) {
		PortCLFI3List = portCLFI3List;
	}


	// Examples of CLFI below:
		//CLFI = JU104/GE1N/SNJLCA860AW/SNJSCA02W31	
		//CLFI2 = 999/S0308S/SNFCCA21/SNJSCA02	
		//CLFI2+ = 999/S0308S/SNFCCA21/SNJSCA02/4	
		//CLFI3-LIST = BR001/OMC/OKLDCA03/SNFCCA21,BR001/OMC/HYWRCA01/OKLDCA03,BR001/OMC/PLALCA02/SNTCCA01,BR001/OMC/SNBUCA02/SNFCCA21,BR001/OMC/SNJSCA02/SNTCCA01,BR001/OMC/PLALCA02/SNBUCA02,BR001/OMC/FRMTCA11/SNJSCA02,BR001/OMC/FRMTCA11/HYWRCA01

		
		//Look for an att-ntd-ticket such that 
		//(the clfi of the ticket is a member of clfi3-list) or 
		//(the clfi is a member of clfi2) or 
		//(the clfi is a member of clfi2plus) or 
		//(the clfi = clfi )
		//order is important, first check clfi3, then clfi2, clfiplus and last clfi
		
		//P101/GE10/DTRTMIBL0AW/DTRTMIBL0BW
		
		//If ticket found then  set "NMA=<Y>" flag in reason
		//Also set reason to:  "There is a L1 Transport alarm impacting this facility CLFI=<[the clfi of T]>. 
		//NMA Ticket is TicketNumber=<[the ticket-number of T]> from NMA host NWPHostName=<[the nwp-host-name of T]>. 
		//Do not take action as it is being worked by IOF NRC: [the reason of M]"

		public boolean IsNTDExists() {
	
			if (log.isTraceEnabled()) {
				LogHelper.enter(log, "IsNTDExists()");
			}

			boolean ret = false;
			NTDticket ticket = new NTDticket();
			String clfiList = null;
			
			// first check if there are any tickets where the CLFI of the ticket is contained in the 
			// CLFI3-list		
			if(getPortCLFI3List() != null) {
				log.trace("getPortCLFI3List is not NULL");
				clfiList = CreateClfiList(getPortCLFI3List());
				if(IpagJuniperESTopoAccess.getInstance().CheckCLFI_list(clfiList, ticket)) {
					// found the ticket, now update the alarm
					UpdateAlarmWithTicketInfo(ticket);
					ret = true;	
				}
			}
			
			// check CLFI2 Plus
			if(getPortCLFI2Plus() != null && !ret) {
				log.trace("getPortCLFI2Plus is not NULL");
				clfiList = CreateClfiList(getPortCLFI2Plus());
				if(IpagJuniperESTopoAccess.getInstance().CheckCLFI_list(clfiList, ticket)) {
					// found the ticket, now update the alarm
					UpdateAlarmWithTicketInfo(ticket);
					ret = true;
				} 
			}
			
			// check CLFI2
			if(getPortCLFI2() != null && !ret){
				log.trace("getPortCLFI2 is not NULL");
				clfiList = CreateClfiList(getPortCLFI2());
				if(IpagJuniperESTopoAccess.getInstance().CheckCLFI_list(clfiList, ticket)) {
					// found the ticket, now update the alarm
					UpdateAlarmWithTicketInfo(ticket);
					ret = true;
				}
			}
			
			if(getPortCLFI() != null && !ret) {
				log.trace("getPortCLFI is not NULL");
				if(IpagJuniperESTopoAccess.getInstance().CheckCLFI(getPortCLFI(), ticket)) {
					// found the ticket, now update the alarm
					UpdateAlarmWithTicketInfo(ticket);
					ret = true; 
				}
			}

			if (log.isTraceEnabled()) {
				LogHelper.exit(log, "IsNTDExists()");
			}

			
			return ret;
			
		}
		
		private String CreateClfiList(String clfiString) {
			
			if (log.isTraceEnabled()) {
				LogHelper.enter(log, "CreateClfiList()  :  String=" + clfiString);

			}
	
			String clfiArray = "[";
			
			// what we have as input is something like:
			// BR001/OMC/OKLDCA03/SNFCCA21,BR001/OMC/HYWRCA01/OKLDCA03,BR001/OMC/PLALCA02/SNTCCA01,BR001/OMC/SNBUCA02/SNFCCA21
			// what we want out is:
			//["BR001/OMC/OKLDCA03/SNFCCA21", "BR001/OMC/HYWRCA01/OKLDCA03", "BR001/OMC/PLALCA02/SNTCCA01", "BR001/OMC/SNBUCA02/SNFCCA21"]
			String delims = ",";
			String[] tokens = clfiString.split(delims);

			// there may be only one clfi in the list...
			if(tokens.length > 0) {
				for (int i = 0; i < tokens.length; i++) {
					clfiArray = clfiArray + "\"" + tokens[i] + "\"";
					//clfiArray.concat("\"" + tokens[i] + "\"");
			    
					if(i < tokens.length -1)
						clfiArray = clfiArray + ", ";
						//clfiArray.concat(", ");
				}
			} else
				clfiArray = clfiArray + "\"" + clfiString + "\"";
				//clfiArray.concat("\"" + clfiString + "\"");
			
			clfiArray = clfiArray + "]";
			//clfiArray.concat("]");

			if (log.isTraceEnabled()) {
				LogHelper.exit(log, "CreateClfiList()  :  String=" + clfiArray);
			}

		return clfiArray;
	}


		public void UpdateAlarmWithTicketInfo(NTDticket ticket ) {
			// we are now going to update the alarm with the ticket info
			//If ticket found then  set "NMA=<Y>" flag in reason
			//Also set reason to:  "There is a L1 Transport alarm impacting this facility CLFI=<[the clfi of T]>. 
			//NMA Ticket is TicketNumber=<[the ticket-number of T]> from NMA host NWPHostName=<[the nwp-host-name of T]>. 
			//Do not take action as it is being worked by IOF NRC: [the reason of M]"

			if (log.isTraceEnabled()) {
				LogHelper.enter(log, "UpdateAlarmWithTicketInfo()");
			}

			String alarmReasonText = getCustomFieldValue(GFPFields.REASON);

			if(alarmReasonText.contains("NMA=")) {
				alarmReasonText.replace("NMA=<N>", "NMA=<Y>");
			} else {
				alarmReasonText = alarmReasonText + " NMA=<Y> ";
			}
			
			String newReasonText = "There is a L1 Transport alarm impacting this facility CLFI=" + ticket.getCLFI() +
					". NMA Ticket is TicketNumber=<" + ticket.getNumber() +
					"> NMA host NWPHostName=<" + ticket.getHostName() +
					">. Do not take action as it is being worked by IOF NRC:" + alarmReasonText;
			
			setCustomFieldValue(GFPFields.REASON, newReasonText);

			if (log.isTraceEnabled()) {
				LogHelper.exit(log, "UpdateAlarmWithTicketInfo()");
			}

		}
	
}
