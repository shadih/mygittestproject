package com.hp.uca.expert.vp.pd.problem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipag.topoModel.JuniperSyslogTopoAccess;
import com.att.gfp.data.ipag.topoModel.NodeManager;
import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.data.ipagAlarm.SyslogAlarm;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
import com.att.gfp.helper.StandardFields;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.alarm.AlarmCommon;
import com.hp.uca.expert.alarm.AlarmUpdater;
import com.hp.uca.expert.alarm.AlarmUpdater.UsualVar;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.lifecycle.LifeCycleAnalysis;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.vp.pd.services.PD_Service_Group;
import com.hp.uca.expert.x733alarm.AttributeChange;
import com.hp.uca.expert.x733alarm.NetworkState;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;



public class SyslogExtendedLifeCycle extends LifeCycleAnalysis {

	private static final String SYSLOG_RPD_MPLS_LSP_DOWN = "50004/1/7";
	private static final String SYSLOG_LINKDOWN = "50004/1/2";
	private static final String SYSLOG_POLLINGEVENT = "50004/3/2";
	private static final String RPD_RSVP_BYPASS_DOWN = "50004/1/9";
	private static final String BFDOWN = "50004/1/12";
	private static Logger log = LoggerFactory.getLogger(SyslogExtendedLifeCycle.class);
	private static final String SYSLOG_LPORT = "50004/10/1";
	//private static final String LINKDOWN_KEY = "50004/1/2";
	private  Scenario scenario ;



	public SyslogExtendedLifeCycle(Scenario scenario) {
		super(scenario);
		this.scenario = scenario;
		/*
		 * If needed more configuration, use the context.xml to define any beans
		 * that will be available here using scenario.getGlobals()
		 */
		scenario.getGlobals();
	}

	// 	Correlation Syslog Polling Event - (50004/3/2)
	//	Enrichment Syslog LPORT Event - (50004/10/1)

	@Override
	public AlarmCommon onAlarmCreationProcess(Alarm alarm) {

		EnrichedAlarm enrichedAlarm = null;
		SyslogAlarm syslogAlarm = null;

		if (log.isTraceEnabled())
			LogHelper.enter(log, "onAlarmCreationProcess()");

		try {
			if (log.isTraceEnabled()) {
				String axml = alarm.toXMLString();
				axml = axml.replaceAll("\\n", " ");
				log.info("Incoming alarm: "+axml);
			}

			if(alarm.getPerceivedSeverity() == PerceivedSeverity.CLEAR) {
				if (log.isTraceEnabled())
					log.trace("onAlarmCreationProcess: Dropping the alarm as the severity is CLEAR " + alarm.getIdentifier() + "|{" + alarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}");
				return null;  
			} 

			if ( alarm instanceof EnrichedAlarm)    
			{    
				enrichedAlarm = (EnrichedAlarm) alarm;

				if (log.isTraceEnabled()) 
					LogHelper.method(log, "onAlarmCreationProcess() : enrichedAlarm object received");
			} 
			// BFDOWN correlated alarms should not get enriched.
			else if (!alarm.getIdentifier().contains("CORR"))
			{
				// case for junit testing and receiving an alarm from another instance
				if (log.isTraceEnabled()) 
					LogHelper.method(log, "onAlarmCreationProcess() : enrichedAlarm object created...");

				enrichedAlarm = GFPUtil.populateEnrichedAlarmObj(alarm);			
			}		

			// At this point we have an EnrichedAlarm instance no matter what...
			try {			
				syslogAlarm = new SyslogAlarm(enrichedAlarm);

			} catch (Exception e1) {
				// TODO Auto-generated catch block
				log.trace("onAlarmCreationProcess: ERROR:"
						+ Arrays.toString(e1.getStackTrace()));
			} 

			if(syslogAlarm != null ) {
				if (log.isTraceEnabled()) {
					log.trace("Enrichment: "+((EnrichedAlarm)syslogAlarm).toString());
					log.trace("device type: "+syslogAlarm.getDeviceType());
				}
				//test to see if this is a junit test
				// setAlarmAttributesForJunitTesting( syslogAlarm);

				if(syslogAlarm.getCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA) == null || 
						syslogAlarm.getCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA).isEmpty())
					syslogAlarm.setCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA, "NO");

				String instance = syslogAlarm.getOriginatingManagedEntity().split(" ")[1];

				// if the preprocessor doesn't supply these values then we get them here
				/*if(syslogAlarm.getOriginatingManagedEntity().split(" ")[0].equals("DEVICE") &&
					(syslogAlarm.getDeviceType() == null || syslogAlarm.getDeviceType().isEmpty()) || 
					(syslogAlarm.getDeviceSubRole() == null || syslogAlarm.getDeviceSubRole().isEmpty())) {
					NodeManager.getDeviceInfo(syslogAlarm, instance);
					}*/

				syslogAlarm.setSuppressed(false);
				String eventKey = syslogAlarm.getCustomFieldValue(GFPFields.EVENT_KEY);
				if (eventKey.equals("50002/100/21"))
				{
					NodeManager.setMobilityByPport(syslogAlarm);
				}
				else if (eventKey.equals("50002/100/58"))
				{			
					NodeManager.setColdStartVRFSet(syslogAlarm);
					if (log.isTraceEnabled())
						log.trace("ColdStartVRFSet = " + syslogAlarm.getVRFSet() + ", id = " + syslogAlarm.getIdentifier());
					// String deviceInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
					// syslogAlarm.setVRFSet(NodeManager.queryColdStartVRFSet(deviceInstance));
				}
				else if (eventKey.equals("50003/100/12") || eventKey.equals("50003/100/13") || eventKey.equals("50003/100/14") || eventKey.equals("50003/100/15") || eventKey.equals("50003/100/16"))
				{			
					if (NodeManager.setFRUVRFSet(syslogAlarm) == false) {
						log.info("The event, seq: " + syslogAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + " id: " 
								+ syslogAlarm.getIdentifier() + " was dropped by the syslogPdvp because there are no FRFs associated with it.");
						return null;
					}

					if (log.isTraceEnabled())
						log.trace("FRUVRFSet = " + syslogAlarm.getVRFSet() + ", id = " + syslogAlarm.getIdentifier());

					// String deviceInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
					// syslogAlarm.setVRFSet(NodeManager.queryFRUVRFSet(deviceInstance));
				}
				else if(eventKey.equals("50002/100/52") || eventKey.equals("50001/100/61") || eventKey.equals("50001/100/62") || eventKey.equals("50001/100/63") || eventKey.equals("50001/100/64") || eventKey.equals("50001/100/65") || eventKey.equals("50004/1/11")) {			
					String mgtety = alarm.getOriginatingManagedEntity();
					// String evcnodeInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
					//String deviceInstance = evcnodeInstance.split("/")[0];
					// syslogAlarm.setFBSPtpAlarm(NodeManager.isFBSCiena_EMT(deviceInstance));
					if(mgtety.contains("EVC"))
					{
						boolean isJuniper = false;
						if (eventKey.equals("50004/1/11"))
							isJuniper = true;

						if (NodeManager.setFBS_PtpMpt(syslogAlarm, isJuniper) == false) {
							log.info("This event, seq: " + syslogAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + " id: " 
									+ syslogAlarm.getIdentifier() + " was dropped by the SyslogPdvp because there is no point/multi point data for it.");
							return null; // drop it
						}
					}
				}
				/*
			else if(eventKey.equals("50004/1/11")) {			
				String evcnodeInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
				String deviceInstance = evcnodeInstance.split("/")[0];
				syslogAlarm.setFBSPtpAlarm(NodeManager.isFBSJuniper_MX(deviceInstance));
			}
				 */
				// This is the Syslog LPORT Event enrichment
				else if(eventKey.equals(SYSLOG_LPORT)) {			
					boolean lPortExists = JuniperSyslogTopoAccess.getInstance().FetchLocalLPortLevelInformationForLinkDownAlarm(instance, syslogAlarm );

					if (log.isTraceEnabled())
						log.trace("Alarm 50004/10/1: Lport exists: " + lPortExists);

					//if no LPORT
					if(lPortExists==false)
					{
						log.info("This event " + syslogAlarm.getCustomFieldValue("SeqNumber") 
								+" Cat: " + SYSLOG_LPORT + " discarded by: Syslog LPORT Event | Single LPort was not found for: "
								+ alarm.getIdentifier()+" and has been suppressed\n");

						// this suppresses the alarms
						syslogAlarm = null;
					} else {
						if (log.isTraceEnabled()) {
							log.trace("GFPUtil **************");
							LogHelper.enter(log, "Alarm: " + syslogAlarm.getCustomFieldValue("SeqNumber") 
									+ " " + alarm.getIdentifier()+" has been fowarded");
							log.trace("Get Interface, CLCI and EvcName attributes from LP and put them into message (M) attributes as follows:"+
									"1.the reason of M = [the reason of M] INTERFACE IP=<[the interface_ip_addr of LP]> EVCID=<[the evc_name of LP]>"
									+ alarm.getCustomFieldValue(GFPFields.REASON)
									+"2.	the clci of M = the clci of LP"+ alarm.getCustomFieldValue(GFPFields.CLCI)
									+	"3.the circuit-id of M = the clci of LP" +alarm.getCustomFieldValue(GFPFields.CIRCUIT_ID)
									+	"4.	the classification of M = PIVOT-CFO"	+alarm.getCustomFieldValue(GFPFields.CLASSIFICATION)
									+	"5.	the domain of M = IPAG"+ alarm.getCustomFieldValue(GFPFields.DOMAIN)
									+"6.	call set-flags-in-reason(M, PIVOT=<Y>)"+alarm.getCustomFieldValue(GFPFields.REASON_CODE)
									+	"6.	Forward event to Cacher*/");
						}
						// no need to go further with this alarm, enrichment only.
						syslogAlarm = null;
					}
				} else {
					if(eventKey.equals(SYSLOG_POLLINGEVENT)) {
						// This is a device level alarm so see if it exists
						//						if(!syslogAlarm.getAlarmTargetExist()) {
						//							// suppress this alarm because it the device does not exist
						//							log.info("This event: " + syslogAlarm.getCustomFieldValue("sequence-number") +
						//									" | cat: " + SYSLOG_POLLINGEVENT + " SUPPRESSED by: Ipag Juniper Syslog PD VP - SyslogExtendedLifecycle | Device not found " + 
						//									syslogAlarm.getOriginatingManagedEntity().split(" ")[1]);
						//							syslogAlarm = null;
						//						}
					} else {
						if (eventKey.equals(SYSLOG_RPD_MPLS_LSP_DOWN) &&
								syslogAlarm.getDeviceLevelExist()) {
							String tunnelName = syslogAlarm.getOriginatingManagedEntity().split(" ")[1];
							JuniperSyslogTopoAccess.getInstance().getTunnelInfo(tunnelName, syslogAlarm);
						} else {
							if(eventKey.equals(SYSLOG_LINKDOWN))
							{
								//Target is a DEVICE.
								//Find the target device.  If device does not exist then suppress this alarm and done.
								//Log  "This event: [the sequence-number of M] | cat: [the gevm-category of M] 
								//SUPPRESSED by: [the name of this procedure]| Device not found [local-ip]"
								String device = alarm.getOriginatingManagedEntity().split(" ")[1];

								// suppress this alarm because it the device does not exist 
								//								if(syslogAlarm.getAlarmTargetExist() == false)
								//								{
								//									//"This event: [the sequence-number of M] | cat: [the gevm-category of M] 
								//									//SUPPRESSED by: [the name of this procedure]| Device not found [local-ip]"
								//									log.info("This event: " +  syslogAlarm.getCustomFieldValue(GFPFields.SEQNUMBER)
								//											+ " | cat: " + SYSLOG_LINKDOWN +
								//											" SUPPRESSED by: Ipag Juniper Syslog PD VP - SyslogExtendedLifecycle" +
								//											" | Device not found " + device) ;
								//									//suppressed
								//									syslogAlarm = null;
								//								}
							} else {
								if(eventKey.equals(RPD_RSVP_BYPASS_DOWN)) {
									String meClass = alarm.getOriginatingManagedEntity().split(" ")[0];
									String meInstance =alarm.getOriginatingManagedEntity().split(" ")[1];

									if (log.isTraceEnabled())
										log.trace("Alarm recieved,  class " + meClass + " instance " + meInstance);

									if(meClass.equals("TUNNEL")) {

										// is method call is all that is needed due to the db change.   The below stuff is commented out.
										JuniperSyslogTopoAccess.getInstance().FetchLoopBackIPsFromTunnel (meInstance, syslogAlarm);

										// There are four ways to find the device (loopBack IP) that we will be correlating with.
										// According to the requirements, there is an order and below is it...
										/*loopBackIP = JuniperSyslogTopoAccess.getInstance().FetchLoopBackIPFromTunnelStartEndDeviceClli(meInstance, syslogAlarm);
										if(loopBackIP == null)
											loopBackIP = JuniperSyslogTopoAccess.getInstance().FetchLoopBackIPFromHead_ptnii(meInstance, syslogAlarm);

										if(loopBackIP == null)
											loopBackIP = JuniperSyslogTopoAccess.getInstance().FetchLoopBackIPFromTunnelEndEndDeviceClli(meInstance, syslogAlarm);

										if(loopBackIP == null)
											loopBackIP = JuniperSyslogTopoAccess.getInstance().FetchLoopBackIPFromTail_ptnii(meInstance, syslogAlarm);

										// after all that, store the loop back ip to be used later in grouping as the problemEntity
										if(loopBackIP != null)
											syslogAlarm.setTunnelLoopBackIP(loopBackIP);*/


									}
								} else {
									// only if this is a BF Down alarm and there is a potential for having the site id 
									// in the ME
									if((eventKey.equals(BFDOWN) || eventKey.equals("50004/1/13")) &&
											syslogAlarm.getOriginatingManagedEntity().split(" ")[1].contains("/") && 
											!alarm.getIdentifier().contains("CORR"))
									{

										// parse out the remote site id
										String savpnSiteID = instance.split("/")[1];
										syslogAlarm.setRemoteDevice_SavpnSiteID(savpnSiteID);

										// now we get the local device
										String localDevice = instance.split("/")[0];
										JuniperSyslogTopoAccess.getInstance().FetchSyslog_SiteInformation(syslogAlarm, localDevice, savpnSiteID );
										
										if ( syslogAlarm.isSuppressed() ) {
											syslogAlarm = null;
										}

									}

									//Update the remote/local site id so this CORR alarm can form/added to group via computproblementity
									else if ((eventKey.equals(BFDOWN) || eventKey.equals("50004/1/13")) &&
											(alarm.getIdentifier().contains("CORR"))) {
										syslogAlarm.setRemoteDevice_SavpnSiteID(syslogAlarm.getCustomFieldValue("SavpnSiteID_Remote"));
										syslogAlarm.setLocalDevice_SavpnSiteID(syslogAlarm.getCustomFieldValue("SavpnSiteID_Local"));
									}

								}
							}
						}
					}
				}
			}

			if(syslogAlarm != null) {
				//				if (syslogAlarm.getCustomFieldValue(GFPFields.REASON_CODE).contains("Chronic")) {
				//					if (log.isTraceEnabled())
				//						log.trace("\n\nThis is a Chronic alarm and we will not deal with it here: " + syslogAlarm.getIdentifier() + "\n");
				//
				//					Util.whereToSendThenSend(syslogAlarm, false);
				//					syslogAlarm = null;
				//				} else {
				if (log.isTraceEnabled())
					log.trace("\n\n**** test **** alarm is going into WM: " + syslogAlarm.toFormattedString() + "\n");
				syslogAlarm.SetTimeIn();
				//				}
			}

			return syslogAlarm;

		} catch(Exception e) {
			log.error("Dropped the alarm = " + alarm.getIdentifier() + " as onAlarmCreationProcess() threw exception: ", e);
			return null;
		}
	}

	private void setAlarmAttributesForJunitTesting(SyslogAlarm syslogAlarm) {

		//enrichedAlarm.setDeviceType(alarm.getCustomFieldValue("device_type"));
		syslogAlarm.setRemoteDeviceType(syslogAlarm.getCustomFieldValue("remote_device_type"));

		syslogAlarm.setDeviceType(syslogAlarm.getCustomFieldValue("deviceType"));
		syslogAlarm.setRemotePportInstanceName(syslogAlarm.getCustomFieldValue("remotePort"));

		syslogAlarm.setPtnii(syslogAlarm.getCustomFieldValue("ptnii"));

		if(syslogAlarm.getCustomFieldValue("deviceExists") == null)
			syslogAlarm.setAlarmTargetExists(true);
		else if(syslogAlarm.getCustomFieldValue("deviceExists").equals("false"))
			syslogAlarm.setAlarmTargetExists(false);
		else
			syslogAlarm.setAlarmTargetExists(true);

		if (syslogAlarm.getCustomFieldValue("deviceLevelExists") == null)
			syslogAlarm.setDeviceLevelExists(true);              
		else if (syslogAlarm.getCustomFieldValue("deviceLevelExists").equals("true")) 
			syslogAlarm.setDeviceLevelExists(true);
		else
			syslogAlarm.setDeviceLevelExists(false);
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

		if (log.isTraceEnabled()) {
			String axml = newAlarm.toXMLString();
			axml = axml.replaceAll("\\n", " ");
			log.trace("Incoming updated alarm: "+axml);
		}

		// for now just handling the severity
		boolean ret = false;
		boolean isClear = false;
		boolean isFBS = false;

		if (alarmInWorkingMemory instanceof Alarm) {
			Alarm alarmInWM = (Alarm) alarmInWorkingMemory;
			SyslogAlarm sa = (SyslogAlarm) alarmInWM;
			isFBS = sa.getIsFBSPtp();

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
					isClear = true;
					SyslogAlarm a = (SyslogAlarm) alarmInWorkingMemory;
					a.setSeverity(4);
					a.setPerceivedSeverity(PerceivedSeverity.CLEAR);
					a.setCustomFieldValue(GFPFields.LAST_CLEAR_TIME, ""+System.currentTimeMillis()/1000);

					// DF - we also have to set the sequence number to that of the clear alarm
					a.setCustomFieldValue(GFPFields.SEQNUMBER, newAlarm.getCustomFieldValue(GFPFields.SEQNUMBER));

					// DF - setting this here will prevent the clear from being sent because of the
					// canBeSent() check in Util.sendAlarm()
					//a.setIsClear(true);
					boolean isBFDown = getGroupsAlarmBelongsToBFDown((SyslogAlarm) alarmInWorkingMemory);
					if (!a.isSuppressed() || isBFDown )
					{
						// DF - again here because the alarm was already sent (as tested above)
						// the clear alarm will not be sent because of the canBeSent() check in Util.sendAlarm()
						// so I will set IsSent temporarily to false to fool the sendAlarm method
						a.setIsSent(false);
						a.setIsClear(false);
						if("YES".equalsIgnoreCase(newAlarm.getCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA))) {
							a.setCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA, "YES"); 
						} 

						if("YES".equalsIgnoreCase(newAlarm.getCustomFieldValue(GFPFields. IS_PURGE_INTERVAL_EXPIRED)))
							a.setCustomFieldValue(GFPFields. IS_PURGE_INTERVAL_EXPIRED, "YES"); 

						GFPUtil.populateEnrichedAlarmObj(a);

						// send clear 
						Util.whereToSendThenSend(a, false);

						// DF - and now I will put it back in case this value is used in later processing
						a.setIsSent(true);
						if(a.getDecomposed()) {
							List<EnrichedAlarm> decmoposedAlarms = a.getDecomposedAlarms();
							if(decmoposedAlarms != null && decmoposedAlarms.size() > 0) {
								if (log.isTraceEnabled())
									log.trace("Creating Synthetic CLEARS for decomposed alarms size = " + decmoposedAlarms.size());   
								for(EnrichedAlarm decomposedAlarm : decmoposedAlarms) { 
									if (log.isTraceEnabled())
										log.trace("Creating Synthetic CLEARS for decomposed alarms");  
									GregorianCalendar gCalendar = new GregorianCalendar();
									gCalendar.setTime(new java.util.Date(System.currentTimeMillis()));	 
									XMLGregorianCalendar alarmraisedtime = null; 
									try {
										alarmraisedtime = DatatypeFactory.newInstance().newXMLGregorianCalendar(gCalendar);
									} catch (DatatypeConfigurationException e) {
										log.trace("onUpdateSpecificFieldsFromAlarm: ERROR:"
												+ Arrays.toString(e.getStackTrace()));
									} 
									decomposedAlarm.setAlarmRaisedTime(alarmraisedtime); 
									decomposedAlarm.setCustomFieldValue(GFPFields.REASON, "CLEAR");
									decomposedAlarm.setCustomFieldValue(GFPFields.LAST_CLEAR_TIME, String.valueOf(System.currentTimeMillis()/1000));
									decomposedAlarm.setCustomFieldValue(GFPFields.FE_TIME_STAMP,String.valueOf(System.currentTimeMillis()/1000));
									decomposedAlarm.setPerceivedSeverity(PerceivedSeverity.CLEAR); 
									decomposedAlarm.setCustomFieldValue(GFPFields.SEQNUMBER, newAlarm.getCustomFieldValue(GFPFields.SEQNUMBER));
									if (log.isTraceEnabled())
										log.trace("Forwarding sythetic CLEAR for decomposed alarm to NOM");  
									GFPUtil.forwardOrCascadeAlarm(decomposedAlarm, AlarmDelegationType.FORWARD, null); 
									//enrichedAdtranSytheticClear.setSentToNOM(true);     
								}   
							} 
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
				} else {

					/*
					 * 
					 */
					attributeChange = new AttributeChange();
					attributeChange.setName(StandardFields.PERCEIVED_SEVERITY);
					attributeChange.setNewValue(newAlarm.getPerceivedSeverity()
							.toString());
					attributeChange.setOldValue(alarmInWM
							.getPerceivedSeverity().toString());
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

		}
		if (isClear == true && isFBS == true)
		{
			SyslogAlarm a = (SyslogAlarm) alarmInWorkingMemory;

			if (log.isTraceEnabled())
				log.trace("attempt to clear the synthetic alarm.");
			boolean isGenByUCA = false;
			boolean isPurgeItvlExp = false;
			if("YES".equalsIgnoreCase(newAlarm.getCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA)))
			{
				isGenByUCA = true;
			}
			if("YES".equalsIgnoreCase(newAlarm.getCustomFieldValue(GFPFields.IS_PURGE_INTERVAL_EXPIRED)))
			{
				isPurgeItvlExp = true;
			}
			a.clearSyntheticAlarm(a.getCustomFieldValue("vrf-name"), newAlarm.getCustomFieldValue(GFPFields.SEQNUMBER), isGenByUCA, isPurgeItvlExp);
			// set its clear to true to stop FBS processing before
			// synthetic is sent
			a.setIsClear(true);
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "onUpdateSpecificFieldsFromAlarm()",
					String.valueOf(ret));
		}
		return ret;
	}
	private boolean getGroupsAlarmBelongsToBFDown(SyslogAlarm syslogAlarm) {
		for (Group group : PD_Service_Group.getGroupsOfAnAlarm(scenario, syslogAlarm)) {

			if (group.getName().contains("SyslogBFDOWN_LinkDown_Event"))
				return true;
		}
		return false;
	}

}
