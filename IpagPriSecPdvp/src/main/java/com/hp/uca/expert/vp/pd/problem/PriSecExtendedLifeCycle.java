package com.hp.uca.expert.vp.pd.problem;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipag.topoModel.PriSecTopoAccess;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.data.ipagAlarm.Pri_Sec_Alarm;
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


public class PriSecExtendedLifeCycle extends LifeCycleAnalysis {

	private static final String LPORT = "LPORT";
	private static final String _50003_100_1 = "50003/100/1";
	private static Logger log = LoggerFactory.getLogger(PriSecExtendedLifeCycle.class);

	public PriSecExtendedLifeCycle(Scenario scenario) {
		super(scenario);

		/*
		 * If needed more configuration, use the context.xml to define any beans
		 * that will be available here using scenario.getGlobals()
		 */
		scenario.getGlobals();
	}

	@Override
	public AlarmCommon onAlarmCreationProcess(Alarm alarm) {

		LogHelper.enter(log, "onAlarmCreationProcess()");

		if (log.isTraceEnabled()) {
			String axml = alarm.toXMLString();
			axml = axml.replaceAll("\\n", " ");
			log.trace("Incoming alarm: "+axml);
		}

		Pri_Sec_Alarm priSecAlarm = null;		

		try {			
			priSecAlarm = new Pri_Sec_Alarm(alarm);

			if ( alarm instanceof EnrichedAlarm)    
			{    
				if (log.isTraceEnabled()) 
					LogHelper.method(log, "onAlarmCreationProcess() : enrichedAlarm object received");
			} 
			else 
			{
				// case for junit testing
				// set up the fake alarm for junit testing
				if (log.isTraceEnabled()) 
					LogHelper.method(log, "onAlarmCreationProcess() : enrichedAlarm object created for Junit");

				// uncomment only for Junit testing
				//setAlarmAttributesForJunitTesting( priSecAlarm);
			}		
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 

		if(priSecAlarm != null ) {

			if(alarm.getPerceivedSeverity() == PerceivedSeverity.CLEAR) {
				if (log.isTraceEnabled())
					log.trace("onAlarmCreationProcess: Dropping the alarm as the severity is CLEAR " + alarm.getIdentifier() + "|{" + alarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}");
				return null;  
			} 

			if(priSecAlarm.getCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA) == null || 
					priSecAlarm.getCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA).isEmpty())
				priSecAlarm.setCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA, "NO");

			if(priSecAlarm.getCustomFieldValue(GFPFields.G2SUPPRESS) == null || 
					priSecAlarm.getCustomFieldValue(GFPFields.G2SUPPRESS).isEmpty())
				priSecAlarm.setCustomFieldValue(GFPFields.G2SUPPRESS, "none");


			String moClass    = priSecAlarm.getOriginatingManagedEntity().split(" ")[0];
			String moInstance = priSecAlarm.getOriginatingManagedEntity().split(" ")[1];

			priSecAlarm.setSuppressed(false);
			String eventKey = priSecAlarm.getCustomFieldValue(GFPFields.EVENT_KEY);

			if (eventKey.equals(_50003_100_1)) {
				if (moClass.equals(LPORT)) {		
					//priSecAlarm.setContainingPPort(PriSecTopoAccess.getInstance().FetchContainingPPort(moInstance))
					//10.144.0.65/3/2/0
					PriSecTopoAccess.getInstance().FetchLPortInfo(priSecAlarm, moInstance);

					if (log.isTraceEnabled())
						log.trace("containing pport: " + priSecAlarm.getContainingPPort());
					if(priSecAlarm.getContainingPPort() != null)
						priSecAlarm.setDeviceIpAddr(priSecAlarm.getContainingPPort().split("/")[0]);
					else
						priSecAlarm.setDeviceIpAddr("no.containing.pport.is.an.error");
				}
				else if (moClass.equals("PPORT")) {
					priSecAlarm.setPortLagId(PriSecTopoAccess.getInstance().FetchLagIdForPPort(moInstance));
					priSecAlarm.setDeviceInstance(PriSecTopoAccess.getInstance().FetchDeviceFromPPort(moInstance));

					String ipAddr = moInstance.split("/")[0];
					priSecAlarm.setDeviceIpAddr(ipAddr);

					if(priSecAlarm.getCustomFieldValue("component").contains("PortLagId"))
						priSecAlarm.setCustomFieldValue("lag_id", 
								parseLabeledText(priSecAlarm.getCustomFieldValue("component"), 
										"PortLagId"));

					log.info("onAlarmCreationProcess(): This event " + priSecAlarm.getIdentifier() +
							"| deviceIpAddr=" + priSecAlarm.getDeviceIpAddr() +
							"; lag_id=" + priSecAlarm.getCustomFieldValue("lag_id") +
							"; reason_code=" + priSecAlarm.getCustomFieldValue("reason_code"));

				} else if (moClass.equals("DEVICE")) {
					priSecAlarm.setDeviceIpAddr(priSecAlarm.getOriginatingManagedEntity().split(" ")[1]);

					// this changed with HLD-256258a-GFP-Data-200 where instead of the lagid is taken out
					// of the component field, it is taken from the reason code.
					String lagId = null;

					// it isn't clear if the reason_code or the component contains the lag id so we do both
					lagId = getLagIdFromReasonCode(priSecAlarm);

					if(lagId != null) {
						priSecAlarm.setComponentLagId(lagId);
						if (log.isTraceEnabled())
							log.trace("Lag extracted from the reason_code field: " + lagId);						
					}
					else {
						lagId = getLagIdFromComponent(priSecAlarm);
						priSecAlarm.setComponentLagId(lagId);
						if (log.isTraceEnabled())
							log.trace("Lag extracted from the Component field: " + lagId);
					}

					if(lagId != null) {
						priSecAlarm.setCustomFieldValue("lag_id", lagId);
						priSecAlarm.setCustomFieldValue("LagPport", PriSecTopoAccess.getInstance().findAllLagPports(priSecAlarm, lagId));
					}

				}
			} else if (eventKey.equals("50003/100/3")) {
				priSecAlarm.setDeviceIpAddr(priSecAlarm.getOriginatingManagedEntity().split(" ")[1]);

				String neighborRtrIp = 
						parseLabeledTextNoDelims(priSecAlarm.getCustomFieldValue("component"),
								"neighbor ospfRouterId");

				String neighborIp = parseLabeledTextNoDelims(
						priSecAlarm.getCustomFieldValue("component"), "neighbor IP");

				log.info("onAlarmCreationProcess(): This event: " + 
						priSecAlarm.getIdentifier() + "| neighborIp = " + neighborIp +
						"| neighborOspfRouterId = " + neighborRtrIp);

				PriSecTopoAccess.getInstance().getPortLagIdFromNeighbor(priSecAlarm.getDeviceIpAddr(), neighborIp, priSecAlarm);


				// df - added 5/12/14 MR gfpc140375 
				// , PortAID=<et-0/1/0>, PortLagId=<ae20>
				if(priSecAlarm.getCustomFieldValue("lag_id") != null && !priSecAlarm.getCustomFieldValue("lag_id").isEmpty()) {
					String component = priSecAlarm.getCustomFieldValue(GFPFields.COMPONENT);
					priSecAlarm.setCustomFieldValue(GFPFields.COMPONENT, component + ", PortAID=<" + 
							priSecAlarm.getCustomFieldValue("lag_port_aid") + ">, PortLagId=<" +
							priSecAlarm.getCustomFieldValue("lag_id") + ">");
				}

			} else if (eventKey.equals("50003/100/4")) {

				String deviceIpAddr = priSecAlarm.getOriginatingManagedEntity().split(" ")[1];
				priSecAlarm.setDeviceIpAddr(deviceIpAddr);

				String localIp = parseLabeledTextNoDelims(
						priSecAlarm.getCustomFieldValue("component"), "local IP");

				PriSecTopoAccess.getInstance().getPortLagId(deviceIpAddr, localIp, priSecAlarm);

				log.info("onAlarmCreationProcess(): This event: " + 
						priSecAlarm.getIdentifier() + "| localIp (LAG IP) = " + localIp +
						"| localOspfRouterId = " + deviceIpAddr);

				// df - added 5/12/14 MR gfpc140375 
				// , PortAID=<et-0/1/0>, PortLagId=<ae20>
				if(priSecAlarm.getCustomFieldValue("lag_id") != null && !priSecAlarm.getCustomFieldValue("lag_id").isEmpty()) {
					String component = priSecAlarm.getCustomFieldValue(GFPFields.COMPONENT);
					priSecAlarm.setCustomFieldValue(GFPFields.COMPONENT, component + ", PortAID=<" + 
							priSecAlarm.getCustomFieldValue("lag_port_aid") + ">, PortLagId=<" +
							priSecAlarm.getCustomFieldValue("lag_id") + ">");
				}

			} else if (eventKey.equals("50003/100/21")) {
				String deviceIpAddr = priSecAlarm.getOriginatingManagedEntity().split(" ")[1];
				priSecAlarm.setDeviceIpAddr(deviceIpAddr);
				log.info("onAlarmCreationProcess(): This event " + priSecAlarm.getIdentifier() +
						"| deviceIpAddr=" + priSecAlarm.getDeviceIpAddr() +
						"; reason_code=" + priSecAlarm.getCustomFieldValue("reason_code"));
			}  else if(eventKey.equals("50004/1/10")) {
				PriSecTopoAccess.getInstance().FetchPeeringTableInformation(priSecAlarm);
				if(!GFPUtil.isAafDaAlarm((priSecAlarm))) { 
					GFPUtil.forwardAlarmToDecomposerInstance(priSecAlarm, "CIENA_DECOMPOSER");
					priSecAlarm.setDecomposed(true); 
				} 
			}
		}

		if(priSecAlarm != null) {

			// modify alarm raised time to local
			//priSecAlarm.timeToLocal();

			//			if (priSecAlarm.getCustomFieldValue(GFPFields.REASON_CODE).contains("Chronic")) {
			//				if (log.isTraceEnabled())
			//					log.trace("\n\nThis is a Chronic alarm and we will not deal with it here: " + priSecAlarm.getIdentifier() + "\n");
			//
			//					Util.WhereToSendAndSend(priSecAlarm);
			//					priSecAlarm = null; 
			//			} else {
			if (log.isTraceEnabled())
				log.trace("\n\n**** test **** alarm is going into WM: " + priSecAlarm.toFormattedString() + "\n");

			priSecAlarm.SetTimeIn();
			priSecAlarm.setCustomFieldValue("HasSecondary", "false");
			//			}		
			if ( null == priSecAlarm.getRemotePePportInstanceName() || priSecAlarm.getRemotePePportInstanceName().isEmpty() ) {
				priSecAlarm.setRemotePePportInstanceName("NONE");
			}
		}
		return priSecAlarm;
	}


	private String getLagIdFromReasonCode(Pri_Sec_Alarm alarm) {
		String customFieldValue = alarm.getCustomFieldValue(GFPFields.REASON_CODE);
		String lagId = null;

		if(customFieldValue != null && customFieldValue.startsWith("ae"))
		{
			// this lag is on a sub interface so drop everything after the .
			// the ifindex follows the lag so drop that as well
			if(customFieldValue.contains(".")) {
				lagId = customFieldValue.substring(0, customFieldValue.indexOf('.'));
				alarm.setLagSubInterfaceLinkDown(true);
			} else if(customFieldValue.contains("_")) {
				lagId = customFieldValue.split("_")[0];
				alarm.setIfIndex(customFieldValue.split("_")[1]);
			} else
				lagId = customFieldValue;

			if (log.isTraceEnabled())
				log.trace("The reason code contained the lagId:" + lagId);

		}
		return lagId;
	}


	private String getLagIdFromComponent(Pri_Sec_Alarm alarm) {
		// examples:    PortLagId=<ae3.2452>     PortLagId=<ae26>      PortLagId=<ae44.76>

		String lagId = null;
		String component = alarm.getCustomFieldValue(GFPFields.COMPONENT);

		int lagPos = 0;
		int lessThanPos = 0;
		int greaterThanPos = 0;

		// this should give me back <value>
		//agId = GFPUtil.parseSpecifiedValueFromField("PortLagId", component, " ");

		if(component != null) {
			lagPos = component.indexOf("PortLagId");
			lessThanPos = component.indexOf('<', lagPos);
			greaterThanPos = component.indexOf('>', lessThanPos);

			if(lagPos > 0 && lessThanPos > lagPos && greaterThanPos > lessThanPos) {
				lagId = component.substring(lessThanPos+1, greaterThanPos);

				// this lag is on a sub interface so drop everything after the .
				if(lagId.contains(".")) {
					lagId.substring(0, lagId.indexOf('.'));
					alarm.setLagSubInterfaceLinkDown(true);
				}
			} 
		}

		if (log.isTraceEnabled())
			log.trace("The component contained the lagId:" + lagId);

		return lagId;
	}

	private void setAlarmAttributesForJunitTesting(Pri_Sec_Alarm alarm) {

		if (log.isTraceEnabled())
			log.trace("Setting alarms attributes for junit testing !!!!!!");

		// set the alarm time 5 hours ahead
		long now = System.currentTimeMillis() / 1000;

		Date date = new Date(now*1000);
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTimeZone(TimeZone.getTimeZone("GMT"));
		cal.setTime(date);
		try {
			alarm.setAlarmRaisedTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(cal));
			alarm.setCustomFieldValue(GFPFields.FE_TIME_STAMP, Long.toString(now));

			if (log.isTraceEnabled()){
				log.trace("New alarm time set: " + alarm.getAlarmRaisedTime());
				log.trace("New fe time stamp " + alarm.getCustomFieldValue(GFPFields.FE_TIME_STAMP));
			}
		} catch (DatatypeConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}						

		alarm.setCustomFieldValue(GFPFields.G2SUPPRESS, "NONE");

		String age = alarm.getCustomFieldValue("age");
		if(age != null && !age.isEmpty())
			alarm.SetAgingAccumulativeTime(Long.parseLong(age));

		log.trace("Age of alarm for JUnit testing: " + alarm.TimeRemaining(0));

		//enrichedAlarm.setDeviceType(alarm.getCustomFieldValue("device_type"));
		alarm.setRemoteDeviceType(alarm.getCustomFieldValue("remote_device_type"));

		alarm.setDeviceType(alarm.getCustomFieldValue("deviceType"));
		alarm.setRemotePportInstanceName(alarm.getCustomFieldValue("remotePort"));
		alarm.setRemoteDeviceIpaddr(alarm.getCustomFieldValue("RemoteDeviceIpaddr"));

		if(alarm.getCustomFieldValue("deviceExists") == null)
			alarm.setAlarmTargetExists(true);
		else if(alarm.getCustomFieldValue("deviceExists").equals("false"))
			alarm.setAlarmTargetExists(false);
		else
			alarm.setAlarmTargetExists(true);

		if (alarm.getCustomFieldValue("deviceLevelExists") == null)
			alarm.setDeviceLevelExists(true);              
		else if (alarm.getCustomFieldValue("deviceLevelExists").equals("true")) 
			alarm.setDeviceLevelExists(true);
		else
			alarm.setDeviceLevelExists(false);
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

					Pri_Sec_Alarm a = (Pri_Sec_Alarm) alarmInWorkingMemory;

					a.setSeverity(4);
					a.setPerceivedSeverity(PerceivedSeverity.CLEAR);

					a.setCustomFieldValue(GFPFields.LAST_CLEAR_TIME, ""+System.currentTimeMillis()/1000);

					// we also have to set the sequence number to that of the clear alarm
					a.setCustomFieldValue(GFPFields.SEQNUMBER, newAlarm.getCustomFieldValue(GFPFields.SEQNUMBER));

					// the clear alarm will not be sent because of the canBeSent() check in Util.sendAlarm()
					// so I will set IsSent temporarily to false to fool the sendAlarm method
					a.setIsSent(false);
					a.setIsClear(false);
					if("YES".equalsIgnoreCase(newAlarm.getCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA))) {
						a.setCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA, "YES"); 
					} 


					// for some reason the secondary alarm id for clears must be 0 for the 
					// primary alarms
					if(a.getCustomFieldValue(GFPFields.SECONDARY_ALERT_ID) != null &&
							a.getCustomFieldValue(GFPFields.SECONDARY_ALERT_ID).equals("1"))
						a.setCustomFieldValue(GFPFields.SECONDARY_ALERT_ID, "0");

					if("YES".equalsIgnoreCase(newAlarm.getCustomFieldValue(GFPFields. IS_PURGE_INTERVAL_EXPIRED)))
						a.setCustomFieldValue(GFPFields. IS_PURGE_INTERVAL_EXPIRED, "YES"); 

					GFPUtil.populateEnrichedAlarmObj(a);

					// send clear to AM

					// always send 50004/1/39 clear to cdc/uverse
					if ( a.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50004/1/39") ) {
						a.setSendToCdc(true);
					}

					Util.WhereToSendAndSend(a);


					// put it back in case this value is used in later processing
					a.setIsSent(true);				
					if(a.getDecomposed()) {
						if("50003/100/1".equalsIgnoreCase(a.getCustomFieldValue(GFPFields.EVENT_KEY)) ||
								"50002/100/21".equalsIgnoreCase(a.getCustomFieldValue(GFPFields.EVENT_KEY))) {
							GFPUtil.forwardAlarmToDecomposerInstance(a, "JUNIPER_DECOMPOSER"); 
						} else {
							GFPUtil.forwardAlarmToDecomposerInstance(a, "CIENA_DECOMPOSER"); 
						}  
					}


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
					//				} else {
					//
					//					/*
					//					 * 
					//					 */
					//					attributeChange = new AttributeChange();
					//					attributeChange.setName(StandardFields.PERCEIVED_SEVERITY);
					//					attributeChange.setNewValue(newAlarm.getPerceivedSeverity()
					//							.toString());
					//					attributeChange.setOldValue(alarmInWM
					//							.getPerceivedSeverity().toString());
					//					attributeChangesAVC.add(attributeChange);
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

			//if(ret)
			//	getScenario().getSession().retract(alarmInWorkingMemory);
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "onUpdateSpecificFieldsFromAlarm()",
					String.valueOf(ret));
		}
		return ret;
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

	public String parseLabeledTextNoDelims(String textStr, String label) {

		String parsedText = "";
		label += " = ";
		int i = textStr.indexOf(label);
		if (i > 0) {
			i += label.length();
			parsedText = textStr.substring(i);
			i = parsedText.indexOf(",");
			if (i > 0) {
				parsedText = parsedText.substring(0, i);
			}
		}
		return parsedText;
	}
}

