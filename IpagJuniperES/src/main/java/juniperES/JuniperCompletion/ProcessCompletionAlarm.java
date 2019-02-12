package juniperES.JuniperCompletion;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.att.gfp.data.ipag.JunipertopoModel.IpagJuniperESTopoAccess;
import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.data.ipagAlarm.AlarmState;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.data.ipagJuniperAlarm.JuniperESEnrichedAlarm;
import com.att.gfp.data.juniperES.helper.SendAlarm;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
import com.hp.uca.common.trace.LogHelper;
import juniperES.JuniperCompletion.PPort;

public class ProcessCompletionAlarm {

	private static final String DA = "DA";
	private static final String AAF_SECONDARY = "AAF-SECONDARY";
	private static final String AAF_PRIMARY = "AAF-PRIMARY";
	private static final String VRF = "VRF";
	private static final String LAG = "LAG";
	private static final String FRU = "FRU";


	private static final Logger log = Logger.getLogger ( ProcessCompletionAlarm.class );

	private static ProcessCompletionAlarm completionAccessor = null;

	public static synchronized ProcessCompletionAlarm getInstance() {
		if (completionAccessor == null) {
			completionAccessor = new ProcessCompletionAlarm();
		}
		return completionAccessor;
	}


	ProcessCompletionAlarm() {

	}

	public void ProcessCompletionCardSlot(JuniperESEnrichedAlarm alarm, String processingInfo) {

		if (log.isTraceEnabled())
			log.trace("ProcessCompletionCardSlot() Enter : ");

		String managedObjectInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String managedObjectClass = alarm.getOriginatingManagedEntity().split(" ")[0];
		//String remoteDeviceType = alarm.getRemoteDeviceType();

		if(processingInfo == LAG) {
			if(alarm.getDeviceType().equals("JUNIPER MX SERIES") || alarm.getDeviceType().equals("NV1") 
					|| alarm.getDeviceType().equals("NV2") || alarm.getDeviceType().equals("NV3") )
				//					(!remoteDeviceType.equals("ADTRAN 5000 SERIES") &&
				//							!remoteDeviceType.equals("JUNIPER MX SERIES") &&
				//							!remoteDeviceType.equals("CIENA EMUX") &&
				//							!remoteDeviceType.equals("VR1"))) {
				//
				//				alarm.setCustomFieldValue(GFPFields.INFO2, 
				//						alarm.getCustomFieldValue(GFPFields.INFO2) + "RemoteNTECLLI=<" +
				//								remoteDeviceType + "> RemotePortAID=<" +
				//								alarm.getRemotePortAid() + ">");
				//			}
				ProcessCompletionDeviceLAG(alarm);
		} else {
			ArrayList<PPort> pPorts = IpagJuniperESTopoAccess.getInstance().findAllPports(managedObjectInstance, managedObjectClass);

			if(!pPorts.isEmpty())
			{
				for (PPort pPort : pPorts) {
					//deviceType=<JUNIPER MX SERIES> deviceModel=<MX480> portAID=<xe-7/3/0> Slot=<7> Card=<3> Port=<0> FRU Name fru test name, Type: FPC, Slot: 3
					String component = setComponentForSlotOrCard(pPort);

					String aafdaRole = pPort.getAafda_role();

					if (log.isTraceEnabled())
						log.trace("processing port " + pPort.getName() + " with aafda role " + pPort.getAafda_role() + 
								" alarm device type: " + alarm.getDeviceType() + " remote device type: " +
								pPort.getRemoteDeviceType());

					//processing port 143.0.68.46/0/0/1 with aafda role  
					//alarm device type: JUNIPER MX SERIES remote device type: CIENA NTE				

					if(pPort.getRemoteDeviceType().equals("CIENA NTE") &&
							alarm.getDeviceType().equals("JUNIPER MX SERIES") &&
							!aafdaRole.equals(AAF_PRIMARY) &&
							!aafdaRole.equals(AAF_SECONDARY) &&
							!aafdaRole.equals(DA)) {

						processPPortForServiceImpact(pPort.getName(), alarm, processingInfo, component);
					}
				}
			} else
				log.info("No remote PPorts found of type CIENA NTE for alarm " + alarm.getIdentifier() + 
						" seq #:" + alarm.getCustomFieldValue(GFPFields.SEQNUMBER));

		}
		if (log.isTraceEnabled())
			log.trace("ProcessCompletionCardSlot() Exit : ");
	}		

	private String setComponentForSlotOrCard(PPort pPort) {
		String slot = null;
		String card = null;
		String port = null;

		//" portAID=<" + pPort.getAid() + "> Slot=<0> Card=<0> Port<" + pPort.getNum() + "> ";

		// expecting 10.10.10.10/1/1/1 - slot, card, port
		// or
		// slot, port
		String [] subs = pPort.getName().split("/");

		if(subs.length == 4) {
			// we have slot, card, and port
			slot = subs[1];
			card = subs[2];
			port = subs[3];
		} else if(subs.length == 3) {
			slot = subs[1];
			port = subs[2];
		}

		return " portAID=<" + pPort.getAid() + "> Slot=<" + slot + "> Card=<" + card + "> Port<" + port + "> ";

	}


	public void ProcessCompletionLPort(JuniperESEnrichedAlarm alarm, String processingInfo) {

		//String lPort = alarm.getOriginatingManagedEntity().split(" ")[1];

		//String pPort = IpagJuniperESTopoAccess.getInstance().FetchSuperiorPPort(lPort);

		if (log.isTraceEnabled())
			log.trace("Containing PPort is: " + alarm.getContainingPPort());
		
		processPPortForServiceImpact(alarm.getContainingPPort(), alarm, processingInfo, alarm.getCustomFieldValue(GFPFields.COMPONENT));

	}


	public void ProcessCompletionPPort(JuniperESEnrichedAlarm alarm, String processingInfo) {

		if (log.isTraceEnabled())
			log.trace("ProcessCompletionPPort() Enter : ");

		String remoteDeviceType = alarm.getRemoteDeviceType();

		if(remoteDeviceType == null || remoteDeviceType.isEmpty())
			return;

		if(processingInfo == LAG) {
			if(alarm.getDeviceType().equals("JUNIPER MX SERIES") &&
					(!remoteDeviceType.equals("ADTRAN 5000 SERIES") &&
							!remoteDeviceType.equals("JUNIPER MX SERIES") &&
							!remoteDeviceType.equals("CIENA EMUX") &&
							!remoteDeviceType.equals("VR1") &&
							!remoteDeviceType.equals("NV1") &&
							!remoteDeviceType.equals("NV2") &&
							!remoteDeviceType.equals("NV3"))) {

				String info2 = alarm.getCustomFieldValue(GFPFields.INFO2);
				if(!info2.contains("RemoteNTECLLI=")) {
					alarm.setCustomFieldValue(GFPFields.INFO2, info2 + "RemoteNTECLLI=<" +
									alarm.getRemoteDeviceName() + "> RemotePortAID=<" +
									alarm.getRemotePortAid() + ">");
				}
			}

			if(alarm.getRemoteDeviceType().equalsIgnoreCase("CIENA EMUX") &&
					alarm.getCustomFieldValue(GFPFields.CLASSIFICATION).equalsIgnoreCase("ETHERNET-NFO")){

					// we need to generate a duplicate alarm for this case
					EnrichedAlarm duplicate = new EnrichedAlarm(alarm);
					
					// need to change the alert id to make it unique
					String alertId = alarm.getCustomFieldValue(GFPFields.ALERT_ID) + "EMUX";					
					duplicate.setCustomFieldValue(GFPFields.ALERT_ID, alertId);
				
					duplicate.setCustomFieldValue(GFPFields.CLASSIFICATION, "NFO-EMUX");
					duplicate.setCustomFieldValue(GFPFields.DOMAIN, "EMUX");

					log.info("Suppressing ETHERNET-NFO alarm and generating a duplicate alarm for RUBY-TELCO and sending it for alarm: " + 
							duplicate.getIdentifier() + " sequence # " + duplicate.getCustomFieldValue(GFPFields.SEQNUMBER));

					GFPUtil.forwardOrCascadeAlarm(duplicate, AlarmDelegationType.FORWARD, null);	
					//suppress linkdown pport alarm with ETHERNET-NFO classifcation
					alarm.setSuppressed(true);
			} else {
				// # DF - without this if statement all pports without the port lag id in the 
				// compoment will be suppressed and this is not correct.
				// If this is a LAG alarm then process all pports on the device
				if (alarm.getCustomFieldValue(GFPFields.COMPONENT).contains("PortLagId=<ae"))
					ProcessCompletionDeviceLAG(alarm);
				else
					processPPortForServiceImpact(alarm.getOriginatingManagedEntity().split(" ")[1], 
							alarm, processingInfo, alarm.getCustomFieldValue(GFPFields.COMPONENT));
			}
		} else {
			processPPortForServiceImpact(alarm.getOriginatingManagedEntity().split(" ")[1], 
					alarm, processingInfo, alarm.getCustomFieldValue(GFPFields.COMPONENT));
		}

		if (log.isTraceEnabled())
			log.trace("ProcessCompletionPPort() Exit : ");

	}


	public void ProcessCompletionDeviceFRU(JuniperESEnrichedAlarm alarm) {

		if (log.isTraceEnabled())
			log.trace("ProcessCompletionFRU() Enter : ");

		/*		String PPort = null;
		String alertId = alarm.getCustomFieldValue(GFPFields.ALERT_ID);


		String managedObjectInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String managedObjectClass = alarm.getOriginatingManagedEntity().split(" ")[0];*/

		if (log.isTraceEnabled())
			log.trace("ProcessCompletionFRU() Exit : ");
	}

	public void ProcessCompletionDeviceVPN(JuniperESEnrichedAlarm alarm) {

		if (log.isTraceEnabled())
			log.trace("ProcessCompletionVPN() Enter : ");

		String PPort = null;
		String vfrName = null;

		String managedObjectInstance = alarm.getOriginatingManagedEntity().split(" ")[1];		

		// get the vfrName from alarm
		//String reason = alarm.getCustomFieldValue(GFPFields.REASON);
		
		// the isocket takes the original reason field and maps it into the probableCause
		// so here we need the original reason field.
		String reason = alarm.getCustomFieldValue(GFPFields.REASON);
		vfrName = parseLabeledText(reason, VRF);
		alarm.setVFRName(vfrName);
		
		if (log.isTraceEnabled()) 
			log.trace("VRF name from reason = " + vfrName);
		
		// see if there is an LPort that has the vfr name 
		if ( (alarm.getCustomFieldValue("pportkeyfromevc") == null || (alarm.getCustomFieldValue("pportkeyfromevc").isEmpty())) ) {
			String deviceIP = managedObjectInstance.split("/")[0];
			PPort = IpagJuniperESTopoAccess.getInstance().CheckForMatchingLPort(vfrName, deviceIP);
		}
		else {
			PPort = alarm.getCustomFieldValue("pportkeyfromevc");
		}
		
		//String component = alarm.getCustomFieldValue(GFPFields.COMPONENT);
		if(PPort != null) {
			alarm.setRemotePportAafdaRole(IpagJuniperESTopoAccess.getInstance().FetchRemoteAAFDArole(PPort));
			processPPortForServiceImpact(PPort, alarm, VRF, " ");
			if (log.isTraceEnabled())
				log.trace("ProcessCompletionVPN() : found PPort : " + PPort);
		}

		if (log.isTraceEnabled())
			log.trace("ProcessCompletionVPN() Exit : ");
	}

	public void ProcessCompletionDeviceLAG(JuniperESEnrichedAlarm alarm) {

		if (log.isTraceEnabled())
			log.trace("ProcessCompletionDeviceLAG() Enter : ");

		String lagId = null;
		String managedObjectInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String managedObjectClass = alarm.getOriginatingManagedEntity().split(" ")[0];

		if(managedObjectClass.equals("DEVICE"))
			managedObjectInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		else
			managedObjectInstance = IpagJuniperESTopoAccess.getInstance().FetchDeviceForClass(managedObjectInstance, managedObjectClass);

		if (log.isTraceEnabled())
			log.trace("ProcessCompletionLAG(): Component is: " + alarm.getCustomFieldValue(GFPFields.COMPONENT));

		if (alarm.getCustomFieldValue(GFPFields.COMPONENT).contains("PortLagId=<ae")) {
			// extract the lag Id from the component field
			String component = alarm.getCustomFieldValue(GFPFields.COMPONENT);
			lagId = parseLabeledText(component, "PortLagId");
			if (log.isTraceEnabled())
				log.trace("ProcessCompletionLAG(): Found LAG id: " + lagId);

		}

		ArrayList<String> pPorts = new ArrayList();
		if(lagId != null) {
			// check if the list of matching pports with matching lag id is already set from PriSecVP; if not then call query from here
			if ( alarm.getCustomFieldValue("LagPport") == null ||   alarm.getCustomFieldValue("LagPport").isEmpty() ) {
				pPorts = IpagJuniperESTopoAccess.getInstance().findAllLagPports(managedObjectInstance, lagId);
			}
			else {
				pPorts.add(alarm.getCustomFieldValue("LagPport"));
			}

			// here we go thru the pports to generate new alarms, we just let the framework handle clears
			// and duplicates

			//For every PPORT under DEVICE, if the port_lag_id of PP = lagID then 
			//append portAID=<[the port_aid of PP]> onto the component field of M.
			for (String pPort : pPorts) {
				if (log.isTraceEnabled())
					log.trace("ProcessCompletionLAG(): Processing port: " + pPort);

				String component = alarm.getCustomFieldValue(GFPFields.COMPONENT) + " portAID=<" + pPort + ">";
				if (log.isTraceEnabled())
					log.trace("ProcessCompletionLAG(): Updating component: " + component);

				processPPortForServiceImpact(pPort, alarm, LAG, component);
			}
		} else {
			// here we have a LAG event but no port lag id in the alarm so we suppress it
			alarm.setSuppressed(true);
		}
		if (log.isTraceEnabled())
			log.trace("ProcessCompletionDeviceLAG() Exit : ");
	}

	private void processPPortForServiceImpact(String pPort, JuniperESEnrichedAlarm alarm, String processingInfo, String component) {

		if (log.isTraceEnabled())
			log.trace("processPPortForServiceImpact() Enter :   Pport is:" + pPort);

		String alertId = alarm.getCustomFieldValue(GFPFields.ALERT_ID);

		if ( processingInfo.equals("FRU")) { //|| processingInfo.equals("LAG")) {
			alertId = alertId + "-" + pPort + "-SCP-CFO";
		} else {
			alertId = alertId + "-SCP-CFO";
		}
		
		String aafdaRole = null; 

		// I have to force the issue here, causing an error (unexplained).
		if(alarm.getRemotePportAafdaRole() == null || alarm.getRemotePportAafdaRole().isEmpty()) {
			aafdaRole = "none";
		} else
			aafdaRole = alarm.getRemotePportAafdaRole();
		
		if(alarm.getRemoteDeviceType() == null || alarm.getRemoteDeviceType().isEmpty())
			alarm.setRemoteDeviceType("none");


		// if we have a pport or lport alarm, then we can process the alarm because we have all 
		// the info.
		if(alarm.getOrigMEClass().equals("PPORT") ||
				alarm.getOrigMEClass().equals("LPORT")) {
		
			if ( (!(AAF_PRIMARY.equals(aafdaRole)) && !(AAF_SECONDARY.equals(aafdaRole)) && !(DA.equals(aafdaRole)))
					&& processingInfo.equals("LAG") && alarm.getRemoteDeviceType().equalsIgnoreCase("CIENA NTE")) {
				//					  ("CIENA EMUX".equals(alarm.getRemoteDeviceType()) || "ADTRAN 5000 SERIES".equalsIgnoreCase(alarm.getRemoteDeviceType())) 
				//					|| ( !("LAG".equals(processingInfo)) && "CIENA NTE".equalsIgnoreCase(alarm.getRemoteDeviceType()))))) {

				ArrayList<String> lPorts = IpagJuniperESTopoAccess.getInstance().findAllLports(pPort);

				if (log.isTraceEnabled())
					log.trace("AAFDA role matches for PPort");

				// for now we duplicate the PPort alarm if we find one service type match
				if(!lPorts.isEmpty()) {

					if(alarm.getCustomFieldValue("G2Suppress") != null &&
							!alarm.getCustomFieldValue("G2Suppress").isEmpty() &&
							alarm.getCustomFieldValue("G2Suppress").equals("IPAG02")) {

						log.info("Not generating a duplicate alarm for Ruby CC because G2Suppress is IPAG02: " + 
								alarm.getIdentifier() + " sequence # " + alarm.getCustomFieldValue(GFPFields.SEQNUMBER));
					} else {

						// we found at least one lport with the correct service
						EnrichedAlarm duplicate = new EnrichedAlarm(alarm);

						duplicate.setCustomFieldValue(GFPFields.CLASSIFICATION, "IPAG-SCP-CFO");
						duplicate.setCustomFieldValue(GFPFields.ALERT_ID, alertId);
						duplicate.setIdentifier("PPORT-" + pPort + "--" + alarm.getIdentifier());
						
						//duplicate.setOriginatingManagedEntity("PPORT " + pPort);

						if (log.isTraceEnabled())
							log.trace(duplicate.toString());

						log.info("Generating a duplicate alarm for Ruby CC and sending for alarm: " + 
								duplicate.getIdentifier() + " sequence # " + duplicate.getCustomFieldValue(GFPFields.SEQNUMBER));

						GFPUtil.forwardOrCascadeAlarm(duplicate, AlarmDelegationType.FORWARD, null);
					}
				} else
					if (log.isTraceEnabled())
						log.trace("There are no lports that match the service type for this pport");
			}
		} else if(alarm.getOrigMEClass().equals("CARD") ||
				alarm.getOrigMEClass().equals("SLOT") ||
				alarm.getOrigMEClass().equals("EVCNODE") ||
				(alarm.getOrigMEClass().equals("DEVICE") &&
						(!AAF_PRIMARY.equals(aafdaRole) && !AAF_SECONDARY.equals(aafdaRole) && !DA.equals(aafdaRole)) &&
						alarm.getVFRName()!=null && !alarm.getVFRName().isEmpty())				
				){

			ArrayList<String> lPorts = IpagJuniperESTopoAccess.getInstance().findAllLports(pPort);

			// for now we duplicate the PPort alarm if we find one service type match
			if(!lPorts.isEmpty()) {
				
				if(alarm.getCustomFieldValue("G2Suppress") != null &&
						!alarm.getCustomFieldValue("G2Suppress").isEmpty() &&
						alarm.getCustomFieldValue("G2Suppress").equals("IPAG02")) {

					log.info("Not generating a duplicate alarm for Ruby CC because G2Suppress is IPAG02: " + 
							alarm.getIdentifier() + " sequence # " + alarm.getCustomFieldValue(GFPFields.SEQNUMBER));
				} else {

				// we found at least one lport with the correct service
				EnrichedAlarm duplicate = new EnrichedAlarm(alarm);

				duplicate.setCustomFieldValue(GFPFields.CLASSIFICATION, "IPAG-SCP-CFO");
				duplicate.setCustomFieldValue(GFPFields.ALERT_ID, alertId);
				
				//duplicate.setOriginatingManagedEntity("PPORT " + pPort);
				if ( ! duplicate.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50003/100/6") &&
					 ! duplicate.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50003/100/7")) {
			
					duplicate.setIdentifier("PPORT-" + pPort + "--" + alarm.getIdentifier());

					// update reason to start with "Linkdown -" followed by remaining existing text; for example:
					duplicate.setCustomFieldValue(GFPFields.REASON, "Linkdown -" + alarm.getCustomFieldValue(GFPFields.REASON));
	
					// <customField value="deviceType=&lt;JUNIPER MX SERIES&gt; deviceModel=&lt;MX480&gt; FRU Name fru test name, Type: 0, Slot: 0, Off at testing, On for testing" name="component"/> 
					String alarmComponent = alarm.getCustomFieldValue(GFPFields.COMPONENT);
					int mpos = alarmComponent.indexOf("deviceModel=<");
					int spos = alarmComponent.indexOf(" ", mpos);
					String comBeginning = alarmComponent.substring(0, spos);
					String comEnd = alarmComponent.substring(spos);
	
					duplicate.setCustomFieldValue(GFPFields.COMPONENT, comBeginning + component + comEnd); 
				}

				if (log.isTraceEnabled())
					log.trace(duplicate.toString());

				log.info("Generating a duplicate alarm for Ruby CC and sending for alarm: " + 
						duplicate.getIdentifier() + " sequence # " + duplicate.getCustomFieldValue(GFPFields.SEQNUMBER));

				GFPUtil.forwardOrCascadeAlarm(duplicate, AlarmDelegationType.FORWARD, null);	
				}
			} else
				if (log.isTraceEnabled())
					log.trace("No LPorts found that have the correct service...");



			// here we go thru the lports to see if the service type is what we want  
			//			for (String lPort : lPorts) {
			//				EnrichedAlarm newAlarm = null;       
			//
			//				try {
			//					newAlarm = new EnrichedAlarm(alarm);
			//				} catch (Exception UnknowAlarmFieldException) {
			//					log.error("onAlarmCreationProcess() : Unable to create a enrichedJuniperAlarm for duplication of LPort alarms:" + alarm.getIdentifier());
			//				}
			//
			//				log.trace("processPPortForServiceImpact(): Found an LPort:" + lPort);
			//				
			//				if(newAlarm != null) {
			//					CalculateAlarmAttributes(lPort, newAlarm, alertId, "LPORT");
			//
			//					newAlarm.setCustomFieldValue(GFPFields.COMPONENT, component);
			//					
			//					// Send the duplicated Alarms as if they were cascaded. Then
			//					// full processing (including filters) is complete.
			//					//Bootstrap.getInstance().getDispatcher().enqueueAlarm(newAlarm);
			//
			//					// forward the alarm
			//					SendAlarm.send((EnrichedAlarm) newAlarm, SendAlarm.COMPLETION);
			//				}
			//			}
		}
		if (log.isTraceEnabled())
			log.trace("processPPortForServiceImpact() Exit : ");
	}

	private void CalculateAlarmAttributes(String myInstance, JuniperESEnrichedAlarm alarm, String alertId, String myClass) {

		if (log.isTraceEnabled())
			log.trace("CalculateAlarmAttributes() Enter : ");

		// mainly for UCA's benefit
		alarm.setOriginatingManagedEntity(myClass + " " + myInstance);
		alarm.setIdentifier(myClass + "-" + myInstance + "-" + alarm.getNotificationIdentifier());

		// these are not in the alarm as we receive it
		//conclude that the event-history of Msg-out = S; 
		//conclude that the gevm-lifetime of msg-out = the gevm-lifetime of msg-in; 
		//conclude that the gevm-priority of msg-out = severity;
		//conclude that the gevm-message of Msg-out = the gevm-message of msg-in; 
		//conclude that the domain of Msg-out = the domain of msg-in;
		//conclude that the gevm-key of Msg-out = obj-inst;
		//conclude that the gevm-category of Msg-out = new-event-key;
		//conclude that the vrf-name of msg-out = the vrf-name of msg-in;
		//conclude that the  gevm-creation-timestamp of Msg-out = the current time; 
		//conclude that the gevm-last-update-timestamp of Msg-out = the current time; 

		// the sequence number is handled in the CA
		//conclude that the sequence-number of Msg-out = "[sequence-num]"; 

		// the event-key is there, we use the original message one (I guess)
		//conclude that the event-key of Msg-out = new-event-key; 

		// this one we have to change
		// conclude that the alert-id of Msg-out = "[new-event-key]-[obj-inst]"; 
		alarm.setCustomFieldValue(GFPFields.ALERT_ID, alertId + "-" + myInstance);

		// these are the same as the input message
		//conclude that the severity of msg-out = severity;
		//conclude that the component of Msg-out = the component of msg-in; 
		//conclude that the info1 of Msg-out = the info1 of msg-in; 
		//conclude that the info2 of msg-out = the info2 of msg-in; 
		//conclude that the info3 of msg-out = the info3 of msg-in;
		//conclude that the sm-event of msg-out = the sm-event of msg-in; 
		//conclude that the sm-element of msg-out = the sm-element of msg-in;
		//conclude that the sm-elementclass of msg-out = the sm-elementclass of msg-in;
		//conclude that the sm-sourcedomain of msg-out = the sm-sourcedomain of msg-in;
		//conclude that the managed-object-instance of msg-out = obj-inst;
		//conclude that the managed-object-class of msg-out = the managed-object-class of msg-in; 
		//conclude that the classification of msg-out = the classification of msg-in; 
		//conclude that the sm-class of msg-out = the sm-class of msg-in;
		//conclude that the aging of Msg-out = the aging of Msg-in; 

		double now = System.currentTimeMillis()/1000;

		//conclude that the fe-alarm-time of Msg-out = curr-time;
		//conclude that the be-alarm-time of Msg-out =  curr-time;

		// TODO:  get this line back
		//alarm.setCustomFieldValue(GFPFields., Double.toString(now)); 
		alarm.setCustomFieldValue(GFPFields.BE_ALARM_TIME, Double.toString(now));

		//conclude that the reason of Msg-out = "[the reason of msg-in]-[reason]"; 
		alarm.setCustomFieldValue(GFPFields.REASON, alarm.getCustomFieldValue(GFPFields.REASON) + "-SCPService");

		// mark this alarm as having been duplicated	
		alarm.setAlarmState(AlarmState.duplicated);

		if (log.isTraceEnabled())
			log.trace("checkLPortsforThisPPort() Exit : ");
	}

	public String parseLabeledText(String textStr, String label) {

		String parsedText = "";
		label += "=<";
		int i = textStr.indexOf(label);
		if (i > 0) {
			i += label.length();
			parsedText = textStr.substring(i);
			i = parsedText.indexOf(">");
			if (i > 0) {
				parsedText = parsedText.substring(0, i);
			}
		}
		return parsedText;
	}

}
