package juniperES.JuniperCompletion;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipag.JunipertopoModel.IpagJuniperESTopoAccess;
import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.data.ipagAlarm.AlarmState;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.data.ipagJuniperAlarm.JuniperESEnrichedAlarm;
import com.att.gfp.data.juniperES.helper.SendAlarm;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
import com.att.gfp.helper.StandardFields;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.alarm.AlarmCommon;
import com.hp.uca.expert.alarm.AlarmUpdater;
import com.hp.uca.expert.alarm.AlarmUpdater.UsualVar;
import com.hp.uca.expert.lifecycle.LifeCycleAnalysis;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.x733alarm.AttributeChange;
import com.hp.uca.expert.x733alarm.NetworkState;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;

public class JuniperCompletionExtendedLifeCycle extends LifeCycleAnalysis{

	public JuniperCompletionExtendedLifeCycle(Scenario scenario) {		
		super(scenario);

	}

	//private static final Object PPORT = "PPORT";
	private static Logger log = LoggerFactory.getLogger(JuniperCompletionExtendedLifeCycle.class);


	@Override
	public AlarmCommon onAlarmCreationProcess(Alarm alarm) {
		if (log.isTraceEnabled())
			LogHelper.enter(log, "onAlarmCreationProcess()");

		JuniperESEnrichedAlarm enrichedAlarm = null; // for junit testing		EnrichedNTDAlarm enrichedNTDAlarm = null;


		try {
			enrichedAlarm = new JuniperESEnrichedAlarm(alarm);
		} catch (Exception UnknowAlarmFieldException) {
			LogHelper.method(log, "onAlarmCreationProcess()", "Unable to create a enrichedJuniperAlarm for:" + alarm.getIdentifier());
		}
		
		try {

		if (!(alarm  instanceof EnrichedAlarm)) {
			if(enrichedAlarm != null) {
				setNeededAlarmValues(enrichedAlarm);
			}
		} else {
			// ## DF - has to be inside the if because this won't work during junit
			// testing.
			if (log.isTraceEnabled()) {
				String axml = alarm.toXMLString();
				axml = axml.replaceAll("\\n", " ");
				log.trace("Incoming alarm: "+axml);
				log.trace("Enrichment: "+((EnrichedAlarm)alarm).toString());
			}
		}

		setValuesForNull(enrichedAlarm);

		String myInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String myClass = enrichedAlarm.getOriginatingManagedEntity().split(" ")[0];
		String eventKey = enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY);

		// health trap
		if (eventKey.equals("50004/2/3") ||
				eventKey.equals("50004/2/58916875") ||
				eventKey.equals("50004/2/58916876") ||
				eventKey.equals("50004/2/58916877"))
		{
			if (log.isDebugEnabled())
				log.debug("Health Trap. Send it to Nom.");
			GFPUtil.forwardOrCascadeAlarm(enrichedAlarm, AlarmDelegationType.FORWARD, null);
			return null;
		}

		// all juniper link down alarms (LAG) go to WM
		if(eventKey.equals("50003/100/1")) {
			enrichedAlarm.setLAG(true);

			if(myClass.equals("PPORT")) {
				// CFM Processing
				String BeTimeStamp = enrichedAlarm.getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP);

				//String anyVRs = IpagJuniperESTopoAccess.getInstance().AreAnyVFRs(myInstance, BeTimeStamp);    

				//if(anyVRs != null)
					//enrichedAlarm.setCustomFieldValue(GFPFields.INFO3, enrichedAlarm.getCustomFieldValue(GFPFields.INFO3)+" CINFO="+ anyVRs);
				
				// in the case of a pport with nfo-mobility we update the info2 field.
				if(enrichedAlarm.getCustomFieldValue(GFPFields.CLASSIFICATION).equals("NFO-MOBILITY"))
					updateInfo2(enrichedAlarm);	
				
				//tm1731 - Update Reason field for MPCIO devices & ROAMD ind = OWB
				String deviceRole = enrichedAlarm.getDeviceRole();
				String info3 = enrichedAlarm.getCustomFieldValue(GFPFields.INFO3); 
				if (deviceRole.equals("MPCIO") && info3.contains("ROADMIndicator=<OWB>") )
					updateAlarmWithROADMInfo(enrichedAlarm);
			}

		} else if (eventKey.equals("50003/100/12") || eventKey.equals("50003/100/13") ||
				eventKey.equals("50003/100/16")) {

			enrichedAlarm.setFRU(true);

			//JNXFRUOffline Processing
			log.trace("#### JNXFRUOffline Alarm: managedObjectClass: " + myClass +
					" deviceType: " + enrichedAlarm.getDeviceType() +
					" RemoteDeviceType: " + enrichedAlarm.getRemoteDeviceType() +
					"####");

			//Check suppression of FRU event based on reason quantity
			int suppressJNXFRU = juniperSuppressFRU(enrichedAlarm);

			if (suppressJNXFRU == 0) {
				log.trace("#### Alarm: " + alarm.getIdentifier() + " has been suppressed by FRU Event Processing");

				enrichedAlarm.setSuppressed(true);
			}

		} if (eventKey.equals("50003/100/24")) { 
			// LAPC timeout processing

			log.trace("#### LACPTimeout Alarm: managedObjectClass: " + myClass +
					" deviceType: " + enrichedAlarm.getDeviceType() +
					" RemoteDeviceType: " + enrichedAlarm.getRemoteDeviceType() +
					"####");

			if ((enrichedAlarm.getDeviceType().equals("JUNIPER MX SERIES")) || 
					(enrichedAlarm.getDeviceType().equals("VR1"))) {

				if(myClass.equals("PPORT")) {
					// get pport attributes (clfi, clci, bmp_clci)
					getPPortInformation(enrichedAlarm);

					if(enrichedAlarm.getRemoteDeviceType() != null) {
						if(enrichedAlarm.getDeviceType().equals("JUNIPER MX SERIES") &&
								(!enrichedAlarm.getRemoteDeviceType().equals("") &&
										!enrichedAlarm.getRemoteDeviceType().contains("JUNIPER MX SERIES") &&
										!enrichedAlarm.getRemoteDeviceType().contains("ADTRAN 500 SERIES") &&
										!enrichedAlarm.getRemoteDeviceType().contains("CIENA EMUX") &&
										!enrichedAlarm.getRemoteDeviceType().contains("VR1") &&
										!enrichedAlarm.getRemoteDeviceType().contains("NV1") &&
										!enrichedAlarm.getRemoteDeviceType().contains("NV2") &&
										!enrichedAlarm.getRemoteDeviceType().contains("NV3") )){

							if (log.isTraceEnabled()) {
								LogHelper.method(log, "Alarm:" + alarm.getIdentifier() + " Add to info2: RemotePortAID=<[the remote_portaid of P]>");
							}
							updateInfo2(enrichedAlarm);
						}	
					}
				} else if(myClass.equals("LPORT")) {
					// get lport attributes (clfi, clci, bmp_clci)
					getLPortInformation(enrichedAlarm);
				}

				log.trace("#### Enriched Alarm : clfi :" + enrichedAlarm.getCLFI() +
						" clci :" + enrichedAlarm.getCLCI() +
						" bmp_clci :" + enrichedAlarm.getBmpCLCI() +
						" circuit-id :" + enrichedAlarm.getCircuitId() +
						"####");

				enrichedAlarm.setCustomFieldValue(GFPFields.CLFI, enrichedAlarm.getCLFI());
				enrichedAlarm.setCustomFieldValue(GFPFields.CLCI, enrichedAlarm.getCLCI());
				enrichedAlarm.setCustomFieldValue(GFPFields.BMP_CLCI, enrichedAlarm.getBmpCLCI());
				enrichedAlarm.setCustomFieldValue(GFPFields.CIRCUIT_ID, enrichedAlarm.getCircuitId());		
			}

			SendAlarm.send(enrichedAlarm, SendAlarm.COMPLETION);
			
			// no more processing needed for these alarms
			return null;

		} else if (eventKey.equals("50003/100/6") || eventKey.equals("50003/100/7")) {
			if ( enrichedAlarm.getCustomFieldValue(GFPFields.DEVICE_TYPE).equals("JUNIPER MX SERIES")) {
				enrichedAlarm.setCustomFieldValue(GFPFields.CLASSIFICATION, "IPAG-PWIF-CFO");
				enrichedAlarm.setCustomFieldValue(GFPFields.DOMAIN, "IPAG");
			}
			else if ( enrichedAlarm.getCustomFieldValue(GFPFields.DEVICE_TYPE).equals("VR1")) {
				enrichedAlarm.setCustomFieldValue(GFPFields.CLASSIFICATION, "VPLS-PWIF-CFO");
				enrichedAlarm.setCustomFieldValue(GFPFields.DOMAIN, "VPLS");
			}
			enrichedAlarm.setVPN(true);
		}

		CheckForDuplicateAlarm(enrichedAlarm);
		
		}
		catch (Exception e) {
			LogHelper.method(log, "onAlarmCreationProcess()", "Exception while processing alarm :" + alarm.getIdentifier() + " Trace = " + e.toString());
			e.printStackTrace();
			return null;
		}
		
		if (log.isTraceEnabled())
			LogHelper.exit(log, "onAlarmCreationProcess()");

		if(enrichedAlarm.isSuppressed())
			return null;
		else
			return enrichedAlarm;
	}

	private void updateAlarmWithROADMInfo(JuniperESEnrichedAlarm enrichedAlarm) {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "updateAlarmWithROADMInfo()");
		}

		String clfi = enrichedAlarm.getCustomFieldValue(GFPFields.CLFI);
		String alarmReasonText = enrichedAlarm.getCustomFieldValue(GFPFields.REASON);	
		String[] rootCauseClfi = IpagJuniperESTopoAccess.getInstance().FetchRoadminfo( clfi);
		
		if(null == rootCauseClfi[0]  || rootCauseClfi[0].isEmpty())
			log.info("ROADM enrichmnent is not done since this alarm CLFI "+clfi+" is not impacted. Alarm  with SeqNum: "
					+ enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER));
		// having matching clfi
		else 
		{
			if(rootCauseClfi[0].endsWith(","))
				rootCauseClfi[0] = rootCauseClfi[0].substring(0,rootCauseClfi[0].length() - 1);
			
			if(rootCauseClfi[1].endsWith(","))
				rootCauseClfi[1] = rootCauseClfi[1].substring(0,rootCauseClfi[1].length() - 1);

			String newReasonText = "There is a L1 Transport alarm from OWB-C ROADM impacting this L2 CLFI :" + clfi + 
					" with L1 RootCauseCLFI(s) = "+rootCauseClfi[0]  +
					". AOTSTicketNumber = " + rootCauseClfi[1] +
					": "+alarmReasonText;
			enrichedAlarm.setCustomFieldValue(GFPFields.REASON, newReasonText);
			log.info("ROADM enrichmnent is done since this alarm CLFI "+clfi+" is impacted in RootCauseCLFI's " +rootCauseClfi[0]+ 
					" (AOTSTicketNumber=" + rootCauseClfi[1] + "). Alarm  with SeqNum: " + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER));
		}

		

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "updateAlarmWithROADMInfo()");
		}

	}

	private void setValuesForNull(JuniperESEnrichedAlarm alarm) {
		
		if( null == alarm.getDeviceType() || alarm.getDeviceType().isEmpty())
			alarm.setDeviceType("none");
		
		if( null == alarm.getRemoteDeviceType()  || alarm.getRemoteDeviceType().isEmpty())
			alarm.setRemoteDeviceType("none");

		if( null == alarm.getCLFI() || alarm.getCLFI().isEmpty())
			alarm.setCLFI("none");
		
		if( null == alarm.getBmpCLCI() ||	alarm.getBmpCLCI().isEmpty())
			alarm.setBmpCLCI("none");
			
		if( null == alarm.getCircuitId() || alarm.getCircuitId().isEmpty()) 
			alarm.setCircuitId("none");
	}


	/**
	 * We do not receive AVC (attribute value chang) & SC (state change) events. So we need to 
	 * simulate the
	 * generation of this information by manually updates the Attributes Changes
	 * of the alarm in the Working Memory.
	 * <p>
	 * <u>List of attributes managed:</u>
	 * <li>perceivedSeverity (impacting the networkState severity is CLEAR)</li>
	 * <li>serverSerial</li>
	 * </p>
	 * <hr>
	 * 
	 * @see com.hp.uca.expert.lifecycle.CommonLifeCycle#onUpdateSpecificFieldsFromAlarm
	 *      (com.hp.uca.expert.alarm.Alarm, com.hp.uca.expert.alarm.AlarmCommon)
	 */
	@Override
	public boolean onUpdateSpecificFieldsFromAlarm(Alarm newAlarm,
			AlarmCommon alarmInWorkingMemory) {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "onUpdateSpecificFieldsFromAlarm()", newAlarm.getIdentifier());
		}

		// for now just handling the severity
		boolean ret = false;

		if (alarmInWorkingMemory instanceof Alarm) {
			Alarm alarmInWM = (Alarm) alarmInWorkingMemory;

			List<AttributeChange> attributeChangesSC = new ArrayList<AttributeChange>();
			List<AttributeChange> attributeChangesAVC = new ArrayList<AttributeChange>();
			AttributeChange attributeChange = null;

			/*
			 * Updating the Perceived Severity of the alarm in Working memory
			 * only if the Alarm received is different.
			 */
			if (newAlarm.getPerceivedSeverity() != alarmInWM
					.getPerceivedSeverity()) {

				if (newAlarm.getPerceivedSeverity() == PerceivedSeverity.CLEAR
						&& alarmInWM.getNetworkState() == NetworkState.NOT_CLEARED) {
					/*
					 * 
					 */

					JuniperESEnrichedAlarm a = (JuniperESEnrichedAlarm) alarmInWorkingMemory;

					a.setSeverity(PerceivedSeverity.CLEAR);
					//a.setSeverity(4);
					a.setPerceivedSeverity(PerceivedSeverity.CLEAR);

					a.setCustomFieldValue(GFPFields.LAST_CLEAR_TIME, ""+System.currentTimeMillis()/1000);

					// we also have to set the sequence number to that of the clear alarm
					a.setCustomFieldValue(GFPFields.SEQNUMBER, newAlarm.getCustomFieldValue(GFPFields.SEQNUMBER));


					// for some reason the secondary alarm id for clears must be 0 for the 
					// primary alarms
					if(a.getCustomFieldValue(GFPFields.SECONDARY_ALERT_ID) != null &&
							a.getCustomFieldValue(GFPFields.SECONDARY_ALERT_ID).equals("1"))
						a.setCustomFieldValue(GFPFields.SECONDARY_ALERT_ID, "0");

					// send clear to AM
					SendAlarm.send((EnrichedAlarm) a, SendAlarm.COMPLETION);
					
					// see if we have to duplacate the clear
					CheckForDuplicateAlarm((EnrichedAlarm) a);

					attributeChange = new AttributeChange();
					attributeChange.setName(StandardFields.NETWORK_STATE);
					attributeChange
					.setNewValue(NetworkState.CLEARED.toString());
					attributeChange.setOldValue(alarmInWM.getNetworkState()
							.toString());
					attributeChangesSC.add(attributeChange);

					/*
					 * 
					 */

					attributeChange = new AttributeChange();
					attributeChange.setName(StandardFields.PERCEIVED_SEVERITY);
					attributeChange.setNewValue(PerceivedSeverity.CLEAR
							.toString());
					attributeChange.setOldValue(alarmInWM
							.getPerceivedSeverity().toString());
					attributeChangesAVC.add(attributeChange);

					// change the clearence time of the alarm.
					attributeChange = new AttributeChange();
					attributeChange.setName(GFPFields.LAST_CLEAR_TIME);
					attributeChange.setNewValue(String.valueOf(System.currentTimeMillis()/1000));
					attributeChange.setOldValue(alarmInWM.getCustomFieldValue(GFPFields.LAST_CLEAR_TIME));
					attributeChangesAVC.add(attributeChange);
				}
			}


			/*
			 * 
			 */
			if (!attributeChangesSC.isEmpty()) {
				AlarmUpdater.updateAlarmFromAttributesChanges(alarmInWM,
						UsualVar.StateChange, attributeChangesSC,
						System.currentTimeMillis());
				alarmInWM.setHasStateChanged(true);
				ret = true;
			}

			/*
			 * 
			 */
			if (!attributeChangesAVC.isEmpty()) {
				AlarmUpdater.updateAlarmFromAttributesChanges(alarmInWM,
						UsualVar.AVCChange, attributeChangesAVC,
						System.currentTimeMillis());
				alarmInWM.setHasAVCChanged(true);
				ret = true;
			}

			if(ret)
				getScenario().getSession().retract(alarmInWorkingMemory);
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "onUpdateSpecificFieldsFromAlarm()",
					String.valueOf(ret));
		}
		return ret;
	}


	private void setNeededAlarmValues(EnrichedAlarm enrichedAlarm) {

		if (log.isTraceEnabled())
			log.debug("Setting fields for JUNIT testing !!");

		// set all values needed for junit testing
		// VPN
		if(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50003/100/6") ||
				enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50003/100/7")) {

			//String reason = enrichedAlarm.getCustomFieldValue(GFPFields.REASON);
			//enrichedAlarm.setCustomFieldValue(GFPFields.REASON, reason + "VRF=&lt;VPWS:169733&gt;");
			enrichedAlarm.setAlarmTargetExists(true);
			enrichedAlarm.setRemoteDeviceType("CIENA NTE");
			enrichedAlarm.setDeviceType(enrichedAlarm.getCustomFieldValue(GFPFields.DEVICE_TYPE));

		} else {
			//LAG
			if(enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50003/100/1")) {	
				
				if (log.isTraceEnabled())
					log.debug("Setting fields for Juniper LD...");

				enrichedAlarm.setDeviceType("JUNIPER MX SERIES");
				enrichedAlarm.setAlarmTargetExists(true);
				enrichedAlarm.setAlarmState(AlarmState.pending);
				enrichedAlarm.setRemoteDeviceType(enrichedAlarm.getCustomFieldValue("remoteDeviceType"));

				enrichedAlarm.setCustomFieldValue(GFPFields.INFO2, "");
				enrichedAlarm.setRemotePortId("");
				
				enrichedAlarm.setContainingPPort(enrichedAlarm.getCustomFieldValue("containingPPort"));
			} else {
				// fru	
				enrichedAlarm.setAlarmTargetExists(true);
				enrichedAlarm.setAlarmState(AlarmState.pending);

				enrichedAlarm.setRemoteDeviceType("");
				enrichedAlarm.setCustomFieldValue(GFPFields.INFO2, "");
				enrichedAlarm.setRemotePortId("");

			}
		}
	}

	private int juniperSuppressFRU(EnrichedAlarm alarm) {

		String reason = alarm.getCustomFieldValue(GFPFields.REASON);
		int suppressAlarm = 0;;
		int quantity = 0;

		log.trace("#### Alarm-reason: " + reason + " ####");
		String[] reasonFields = reason.split("\\s");

		for(String str : reasonFields){
			//log.trace("#### in loop: reason-str: " + str + "####");
			if (str.matches("\\d+")){
				quantity =Integer.parseInt(str);
				//log.trace("#### in loop: reason-quantity: " + quantity + "####");
				break;
			}
		}

		if (((quantity >= 5) && (quantity <= 9)) || (quantity == 22)) {
			log.trace("#### reason-quantity: " + quantity + "####");
			suppressAlarm = 0;
		}
		else {
			suppressAlarm = 1;
		}

		return suppressAlarm;
	}

	private void getPPortInformation(JuniperESEnrichedAlarm alarm) {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "getPPortInformation()");
		}
		String managedObjectClass = alarm.getOriginatingManagedEntity().split(" ")[0];
		String managedObjectInstance = alarm.getOriginatingManagedEntity().split(" ")[1];

		log.trace("#### getPPortInformation(): managedObjectClass:" + managedObjectClass + 
				"  managedObjectInstance:" + managedObjectInstance +
				"####");

		// fetch pport attributes from topology 
		IpagJuniperESTopoAccess.getInstance().FetchPPortLevelInformation(managedObjectInstance, alarm);

	}

	private void getLPortInformation(JuniperESEnrichedAlarm alarm) {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "getLPortInformation()");
		}
		String containingPPortInstance = null;
		String managedObjectClass = alarm.getOriginatingManagedEntity().split(" ")[0];
		String managedObjectInstance = alarm.getOriginatingManagedEntity().split(" ")[1];

		if (log.isTraceEnabled()) {

			log.trace("#### getLPortInformation(): managedObjectClass:" + managedObjectClass + 
					"  managedObjectInstance:" + managedObjectInstance +
					"####");


			log.trace("#### containingPPortInstance:" + containingPPortInstance + 
					"  managedObjectClass:" + managedObjectClass +
					"####");
		}		
		// fetch lport attributes from topology 
		IpagJuniperESTopoAccess.getInstance().FetchLPortLevelInformation(managedObjectInstance, alarm);

	}

	private void updateInfo2(JuniperESEnrichedAlarm alarm) {

		String info2 = alarm.getCustomFieldValue(GFPFields.INFO2);
		String newInfo2 = null;

		log.trace("#### Alarm-RemoteDeviceType: " + alarm.getRemoteDeviceType() + 
				"  Alarm-RemotePortAid:" + alarm.getRemotePortAid() +
				" Alarm-RemoteDeviceName: " + alarm.getRemoteDeviceName() +
				"####");

		if (!info2.contains("RemoteNTECLLI=<")) {

			//Add to info2: "RemoteNTECLLI=<[the remote_devicename of P]> RemotePortAID=<[the remote_portaid of P]>";

			String info2AddonText = "RemoteNTECLLI=<" + alarm.getRemoteDeviceName() + "> " + "RemotePortAID=<" + alarm.getRemotePortAid() + ">";

			if (info2.length()== 0){
				newInfo2 = info2AddonText;
			}
			else {
				newInfo2 = info2 + info2AddonText;
			}

			alarm.setCustomFieldValue(GFPFields.INFO2, newInfo2);

			log.trace("#### Alarm custom fields: " +
					"info2AddonText: " + info2AddonText +
					" INFO2 :" + alarm.getCustomFieldValue(GFPFields.INFO2) +
					"####");

		}

	}	

	private void CheckForDuplicateAlarm(EnrichedAlarm enrichedAlarm)
	{
		//19.	if the domain of M = "VPLS-PE" and the classification of M = "VPLS-CFO" then {send alarm again to RUBY-CNI}.  Logic:
		//		Set the classification of M = "CNI-CFO";
		//		Set the alert-id of M = "[alert-id]-CNI-CFO";
		if(enrichedAlarm.getCustomFieldValue(GFPFields.DOMAIN).equals("VPLS-PE") &&
				enrichedAlarm.getCustomFieldValue(GFPFields.CLASSIFICATION).equals("VPLS-CFO")) {

			if(enrichedAlarm.getCustomFieldValue("G2Suppress") != null &&
					!enrichedAlarm.getCustomFieldValue("G2Suppress").isEmpty() &&
					enrichedAlarm.getCustomFieldValue("G2Suppress").equals("IPAG02")) {

				log.info("Not generating a duplicate alarm for Ruby CNI because G2Suppress is IPAG02: " + 
						enrichedAlarm.getIdentifier() + " sequence # " + enrichedAlarm.getCustomFieldValue(GFPFields.SEQNUMBER));

			} else {
				EnrichedAlarm duplicate = new EnrichedAlarm(enrichedAlarm);

				//if(!enrichedAlarm.getCustomFieldValue(GFPFields.ALERT_ID).contains("CNI-CFO"))
				duplicate.setCustomFieldValue(GFPFields.ALERT_ID, enrichedAlarm.getCustomFieldValue(GFPFields.ALERT_ID) + "-CNI-CFO");

				duplicate.setCustomFieldValue(GFPFields.CLASSIFICATION, "CNI-CFO");
				duplicate.setIdentifier(enrichedAlarm.getIdentifier() + "-duplicate");

				if (log.isTraceEnabled()) {
					String axml = duplicate.toXMLString();
					log.trace("Duplicate alarm: "+axml);	
				}

				log.info("Generating a duplicate alarm for Ruby CNI and sending for alarm: " + 
						duplicate.getIdentifier() + " sequence # " + duplicate.getCustomFieldValue(GFPFields.SEQNUMBER));

				GFPUtil.forwardOrCascadeAlarm(duplicate, AlarmDelegationType.FORWARD, null);
			}
		}

	}

}
