package com.hp.uca.expert.vp.pd.problem;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException; 
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipag.decomposition.Decomposer;
import com.att.gfp.data.ipag.topoModel.IpagJuniperLinkDownTopoAccess;
import com.att.gfp.data.ipag.topoModel.NodeManager;
import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.data.ipagAlarm.AlarmState;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.data.ipagAlarm.EnrichedJuniperAlarm;
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
import com.hp.uca.expert.vp.pd.services.PD_Service_Navigation;
import com.hp.uca.expert.x733alarm.AttributeChange;
import com.hp.uca.expert.x733alarm.NetworkState;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;


public class JuniperLinkDownExtendedLifeCycle extends LifeCycleAnalysis {

	private static final String _50002_100_19 = "50002/100/19";
	private static final String _50004_1_10 = "50004/1/10";
	private static final String _50004_1_7 = "50004/1/7";
	private static final String VR1 = "VR1";
	private static final String _50002_100_21 = "50002/100/21";
	private static final String _50002_100_55 = "50002/100/55";
	private static final String _50003_100_7 = "50003/100/7";
	private static final String _50003_100_6 = "50003/100/6";
	private static final String _50004_1_2 = "50004/1/2";
	private static final String _50003_100_2 = "50003/100/2";
	private static final String PORT_AID = "portAID";
	private static final String DEVICE = "DEVICE";
	private static final String DEVICE_MODEL = "deviceModel";
	private static final String LINKDOWN_KEY = "50003/100/1";
	//private static final String CIENA = "CIENA";
	private static final String PPORT = "PPORT";
	private static final String LPORT = "LPORT";
	private static final String EVC = "EVCNODE";
	private static Logger log = LoggerFactory.getLogger(JuniperLinkDownExtendedLifeCycle.class);
	// public static final String LINK_DOWN_KEY = LINKDOWN_KEY;
	
	public JuniperLinkDownExtendedLifeCycle(Scenario scenario) {
		super(scenario);

		/*
		 * If needed more configuration, use the context.xml to define any beans
		 * that will be available here using scenario.getGlobals()
		 */
		scenario.getGlobals();
	}

	
	@Override
	public AlarmCommon onAlarmCreationProcess(Alarm alarm) {
		try {

			if (log.isTraceEnabled()) 
				LogHelper.enter(log, "onAlarmCreationProcess()");

			EnrichedJuniperAlarm enrichedJuniperAlarm = null;
			EnrichedAlarm enrichedAlarm = null;
			IpagJuniperLinkDownTopoAccess topo = IpagJuniperLinkDownTopoAccess.getInstance();


			// orphan clear alarms are not processed except if it's for an LPORT and in
			// this case we want to forward it to the PriSec VP. Its because we don't process LPORTs
			// here and PriSec does so this clear maybe an orphan to us but to PriSec it could be
			// a long lost child.
			if(alarm.getPerceivedSeverity() == PerceivedSeverity.CLEAR &&
					!alarm.getOriginatingManagedEntity().split(" ")[0].equals(LPORT)) {
				if (log.isTraceEnabled())
					log.trace("Orphan clear has been received: " + alarm.getIdentifier());
				return null;
			}

			// we could be running in Junit or there is a cascade from a scenario running in another instance
			// so if we already have an enriched alarm then we just admit it and if not we create one.
			if ( alarm instanceof EnrichedAlarm)	{
				enrichedAlarm = (EnrichedAlarm) alarm;
				if (log.isTraceEnabled()) 
					if (log.isTraceEnabled())
						LogHelper.method(log, "onAlarmCreationProcess() : enrichedAlarm object received");
			} else {
				
				if (log.isTraceEnabled()) 
					LogHelper.method(log, "onAlarmCreationProcess() : enrichedAlarm object created !!");
	
				enrichedAlarm = GFPUtil.populateEnrichedAlarmObj(alarm);	
			}
			
			// at this point the alarm is of the EnricheAlarm variety and from here on we need a EnrichedJuniperAlarm.
			try {
				enrichedJuniperAlarm= new EnrichedJuniperAlarm(enrichedAlarm);
			} catch (Exception e) {
				e.printStackTrace();
					LogHelper.method(log, "onAlarmCreationProcess()", "Unable to create a enrichedJuniperAlarm for:" + alarm.getIdentifier());
				return null;
			}
			
			if(enrichedJuniperAlarm != null) {	
				
				// determine if this is junit test or not...
				//setAlarmAttributesForJunitTesting(enrichedJuniperAlarm);
				
				// 
				if(enrichedJuniperAlarm.getCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA) == null || 
						enrichedJuniperAlarm.getCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA).isEmpty())
					enrichedJuniperAlarm.setCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA, "NO");
				

				if(enrichedJuniperAlarm.getCustomFieldValue(GFPFields.G2SUPPRESS) == null || 
						enrichedJuniperAlarm.getCustomFieldValue(GFPFields.G2SUPPRESS).isEmpty())
					enrichedJuniperAlarm.setCustomFieldValue(GFPFields.G2SUPPRESS, "none");
				
				
				enrichedJuniperAlarm.setSuppressed(false);

				String eventKey = enrichedJuniperAlarm.getCustomFieldValue(GFPFields.EVENT_KEY);

				if (log.isTraceEnabled()) {
					String axml = alarm.toXMLString();
					axml = axml.replaceAll("\\n", " ");
					log.trace("Incoming alarm: "+axml);
					log.trace("Enrichment: "+enrichedJuniperAlarm.toString());			
				}
				
				enrichedJuniperAlarm.setAlarmState(AlarmState.pending);
				if (log.isTraceEnabled())
					log.trace("Incoming alarm " + enrichedJuniperAlarm.getIdentifier() + " tunable=" + 
						enrichedJuniperAlarm.getCustomFieldValue(GFPFields.EVENT_KEY));


				String targetClass = enrichedJuniperAlarm.getOriginatingManagedEntity().split(" ")[0];
				String targetInstance = enrichedJuniperAlarm.getOriginatingManagedEntity().split(" ")[1];

				// all juniper LD LPORT alarms will be sent to PriSec without processing by this VP
				// the reason is that we cascade only by eventKey.   In the future when Orchestra is in 
				// place we will be able to cascade based on eventKey and ME class.   Here we are just
				// a pass through for LPORT alarms
				if(targetClass.equals(LPORT)) {
					Util.whereToSendThenSend(enrichedJuniperAlarm);
					return null;
				}

				//50004/2/3,50004/2/58916875,50004/2/58916876,50004/2/58916877
				// health trap
				if (eventKey.equals("50004/2/3") ||
						eventKey.equals("50004/2/58916875") ||
						eventKey.equals("50004/2/58916876") ||
						eventKey.equals("50004/2/58916877"))
				{
					if (log.isDebugEnabled())
						log.debug("Health Trap. Send it to JuniperES.");
					Util.whereToSendThenSend(enrichedJuniperAlarm);
					return null;
				}

				// we are going to drop this alarm if the device type is ME3 or if it is null (from above)
				else if(alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(LINKDOWN_KEY)) {

					if(enrichedJuniperAlarm.getDeviceType() != null && enrichedJuniperAlarm.getDeviceType().equals("ME3")) {
						enrichedJuniperAlarm = null;

						log.info("Alarm:" + alarm.getIdentifier() + " sequence #:" + alarm.getCustomFieldValue(GFPFields.SEQNUMBER) +
								" has been suppressed by Juniper Link Down Processing; Device Type=ME3");
						return null;
					} else {

						NodeManager.setPeerSet(enrichedJuniperAlarm);
						if (log.isTraceEnabled())
							log.trace("peerset = " + enrichedJuniperAlarm.getPeerSet() + ", id = " + enrichedJuniperAlarm.getIdentifier());

						if (targetClass.equals("PPORT")) {						
							int delim = targetInstance.indexOf("/");
							String ipAddr = targetInstance.substring(0, delim);
							enrichedJuniperAlarm.setDeviceIpAddr(ipAddr);
							if (log.isTraceEnabled())
								log.trace("deviceIpAddr="
									+ enrichedJuniperAlarm.getDeviceIpAddr());

							if (enrichedJuniperAlarm.getDeviceType() != null && enrichedJuniperAlarm.getDeviceType().equals(VR1)) {
								if (enrichedJuniperAlarm.getRemoteDeviceType() == null || enrichedJuniperAlarm.getRemoteDeviceType().isEmpty()) {
									enrichedJuniperAlarm.setRemoteDeviceType(topo.getPPortRemoteDeviceType(targetInstance, enrichedJuniperAlarm.getNodeType()));
								}
							}
						}
						// Get all information from topology that is needed
						getAllTopologyInformation(enrichedJuniperAlarm);

						// the lagid PPort set contains all ports with matching lag
						if (targetClass.equals("DEVICE") && enrichedJuniperAlarm.getLagIdPportset().isEmpty()) {
							
							// From GFP-DATA Post Oct/2013 Release Adders
							//   Suppress LAG linkdown when there is no matching PPORT in topology with the same lagid 
							//   When processing the juniper lag linkdown, after checking if there is a matching 
							//   PPORT in the topology with the same lagid, G2 shall drop the LAG linkdown if there is
							//   no match
							//   if gfp-data cannot find any matching pports with same lag id then drop the lag 
							//   linkdow
							//   This shall also apply to LAG subinterface alarms; G2 shall extract the lagid 
							//   from the subinterface (i.e. for subinterface ae4.127; lagid is ae4) and check
							//   as mentioned above if there is a matching PPORT w/ same lagid

							log.info("Alarm " + enrichedJuniperAlarm.getIdentifier() + " Seq # " + 
							enrichedJuniperAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + 
							" is suppressed - no associated PPorts exist with same lag ID.");
							
							return null;	
						}
	
						// preprocessor is doing this enrichment now...
						// tj: HLD-256258a-600
//						if( enrichedJuniperAlarm.getDeviceType() != null && enrichedJuniperAlarm.getDeviceType().equals("JUNIPER MX SERIES")) {

							//enrichedJuniperAlarm.setCustomFieldValue(GFPFields.CLCI, enrichedJuniperAlarm.getPortCLFI());
							//NodeManager.enrichJuniperLDForSeverity(enrichedJuniperAlarm);
//						}
						// end: HLD-256258a-600
					}
				} else {
					// tj
					if (enrichedJuniperAlarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50002_100_21))
					{
						enrichCienaLDForSeverity(enrichedJuniperAlarm);
						
						IpagJuniperLinkDownTopoAccess.getInstance().FetchPeeringTableInformation(enrichedJuniperAlarm); 
						
						if("CIENA EMUX".equalsIgnoreCase(enrichedJuniperAlarm.getDeviceType())) {
							IpagJuniperLinkDownTopoAccess.getInstance().fetchRedundantNNIPorts(enrichedJuniperAlarm);  
						} 
						
					} else if("50002/100/19".equalsIgnoreCase(enrichedJuniperAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
						IpagJuniperLinkDownTopoAccess.getInstance().FetchPeeringTableInformation(enrichedJuniperAlarm); 
					}   
					else if (enrichedJuniperAlarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50002_100_55))
					{
						NodeManager.setRemotePportSetMatchedRemoteDeviceType(enrichedJuniperAlarm);
						log.trace("remotePportset = " + enrichedJuniperAlarm.getRemotePportset() + ", id = " + enrichedJuniperAlarm.getIdentifier());
					}
					// tj: done
					else if (enrichedJuniperAlarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50003_100_2) ||
							enrichedJuniperAlarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50004_1_2)	) {

						// These two alarms must be at the device level cuz of the filter
						String ipAddr = enrichedJuniperAlarm.getOriginatingManagedEntity().substring(7);
						enrichedJuniperAlarm.setDeviceIpAddr(ipAddr);

						if (log.isTraceEnabled())
							log.trace("deviceIpAddr=" + enrichedJuniperAlarm.getDeviceIpAddr());

						if (enrichedJuniperAlarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50003_100_2)) {
							if (!enrichedJuniperAlarm.getDeviceLevelExist()) {
								log.info("BGP alarm local device does not exist for: " + 
										enrichedJuniperAlarm.getIdentifier() + " sequence #:" +
										enrichedJuniperAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) +
										" ; suppressing event.");	
								enrichedJuniperAlarm.setSuppressed(true);
							}
							String component = enrichedJuniperAlarm.getCustomFieldValue(GFPFields.COMPONENT);
							String tag = "Local=<";
							int i = component.indexOf(tag);
							if (i > 0) {
								i += tag.length();
								String str = component.substring(i);
								i = str.indexOf(">");
								if (i > 0) {
									str = str.substring(0, i);
									if (enrichedJuniperAlarm.getDeviceName() == null || !enrichedJuniperAlarm.getDeviceName().equals(str)) {

										log.info("BGP alarm "  + enrichedJuniperAlarm.getIdentifier() +
												" sequence #: " + enrichedJuniperAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) +
												" component device " + str +
												" does not match topology device of " + 
												enrichedJuniperAlarm.getDeviceName() + "; suppressing event.");	
										enrichedJuniperAlarm.setSuppressed(true);
									} else {
										if (log.isTraceEnabled())
											log.trace("compDeviceName=" + str);
									}
								}
							}
							if (component.endsWith("Info")) {
								i = component.lastIndexOf("Info");
								component = component.substring(0, i);
								enrichedJuniperAlarm.setCustomFieldValue(GFPFields.COMPONENT, component);
								enrichedJuniperAlarm.setCustomFieldValue(GFPFields.INFO1, 
										"Child=Y " + enrichedJuniperAlarm.getCustomFieldValue(GFPFields.INFO1));
								enrichedJuniperAlarm.setAlarmState(AlarmState.forward);
								if (log.isTraceEnabled())
									log.trace(enrichedJuniperAlarm.getIdentifier() + "component=" +
										enrichedJuniperAlarm.getCustomFieldValue(GFPFields.COMPONENT));
							}
							enrichedJuniperAlarm.setRemoteDeviceIpaddr(
									enrichedJuniperAlarm.getCustomFieldValue(GFPFields.REASON_CODE));
							if (log.isTraceEnabled())
								log.trace("remoteDeviceIpAddr=" + enrichedJuniperAlarm.getRemoteDeviceIpaddr());
							if (!topo.remoteDeviceExists(enrichedJuniperAlarm.getRemoteDeviceIpaddr(), enrichedJuniperAlarm)) { 
								log.info("BGP Neighbor does not exist for: " + enrichedJuniperAlarm.getIdentifier() +
										"sequence # " + enrichedJuniperAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) +
										" ; suppressing event");
								enrichedJuniperAlarm.setSuppressed(true);
							} 
							if (enrichedJuniperAlarm.getDeviceType() != null && enrichedJuniperAlarm.getDeviceType().equals(VR1)) {
								enrichedJuniperAlarm.setCrsFacingPportCount(topo.getCrsPportCount(
										enrichedJuniperAlarm.getDeviceIpAddr(), enrichedJuniperAlarm.getNodeType()));
							}
						}
					} else if ((enrichedJuniperAlarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50003_100_6) ||
							enrichedJuniperAlarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50003_100_7)) &&
							targetClass.equals(EVC)) {

						// only if alarms are at the device level
						//enrichedJuniperAlarm.doDecomposition();					
						String evcNode = enrichedJuniperAlarm.getOriginatingManagedEntity().split(" ")[1];
						
						String reason = enrichedJuniperAlarm.getCustomFieldValue(GFPFields.REASON);
						if(reason!=null && reason.contains("VRF")) {
							String vrfName = parseLabeledText(reason, "VRF");
							//if ( ! topo.getEvcInfo(ipAddr + "/" + vrfName, enrichedJuniperAlarm) ) {
							if ( ! topo.getEvcInfo(evcNode, enrichedJuniperAlarm) ) {
								log.info("VPN Alarm " + enrichedJuniperAlarm.getIdentifier() + " sequence # " + enrichedJuniperAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) +
										" has been suppressed due to no matching EVCNode instance");
								return null;
							}
							if ( (enrichedJuniperAlarm.getCustomFieldValue("vrf-name") == null) || 
									(enrichedJuniperAlarm.getCustomFieldValue("vrf-name").isEmpty()) ) {
								log.info("VPN Alarm " + enrichedJuniperAlarm.getIdentifier() + " sequence # " + enrichedJuniperAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) +
										" has been suppressed due to a null vrf-name from topology");
								return null;
							}
							enrichedJuniperAlarm.setCustomFieldValue(GFPFields.REASON, 
								reason.replaceFirst(" VRF=<" + vrfName + ">", ""));
							topo.getPportFromEvc(evcNode, enrichedJuniperAlarm);
						}
						
						if (log.isTraceEnabled())
							log.trace("updated reason=" + enrichedJuniperAlarm.getCustomFieldValue(GFPFields.REASON));
					} else if(enrichedJuniperAlarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50002_100_19)) {
						//getAAF_DA_Info1Information(enrichedJuniperAlarm);
						//enrichCienaAlarmsForAAF_DA(targetInstance, enrichedJuniperAlarm);	
					} else if(enrichedJuniperAlarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(_50004_1_10)) {
						//getAAF_DA_Info1Information(enrichedJuniperAlarm); 
					}


				}
			}
			
			if(enrichedJuniperAlarm != null) {
//				if (enrichedJuniperAlarm.getCustomFieldValue(GFPFields.REASON_CODE).contains("Chronic")) {
//					if (log.isTraceEnabled())
//						log.trace("\n\nThis is a Chronic alarm and we will not deal with it here: " + enrichedJuniperAlarm.getIdentifier() + "\n");
//
//					Util.whereToSendThenSend(enrichedJuniperAlarm);
//					enrichedJuniperAlarm = null;
//				} else {
					if (log.isTraceEnabled())
						log.trace("\n\n**** test **** alarm is going into WM: " + enrichedJuniperAlarm.toFormattedString() + "\n");
					enrichedJuniperAlarm.SetTimeIn();
//				}
			}

			if (log.isTraceEnabled()) {
				LogHelper.exit(log, "onAlarmCreationProcess()");
			}
			
			return enrichedJuniperAlarm;
			
		} catch(Exception e) {
			log.error("Dropped the alarm = " + alarm.getIdentifier() + " as onAlarmCreationProcess() threw exception: ", e);
			return null;
		}
	}
/*
	private void doDemposition(EnrichedJuniperAlarm enrichedJuniperAlarm) {
		if (log.isDebugEnabled())
			log.debug("doDemposition: Generating decompsed alarms for " + enrichedJuniperAlarm.getIdentifier()); 
		List<EnrichedAlarm> decomposedAlarms = new ArrayList<EnrichedAlarm>();
		try {
			decomposedAlarms = Decomposer.decompose(enrichedJuniperAlarm);
		} catch (Exception e) { 
			e.printStackTrace(); 
		}
		if(decomposedAlarms != null && decomposedAlarms.size() > 0) {
			for(EnrichedAlarm decompseAlarm : decomposedAlarms) {  
				if (log.isDebugEnabled())
					log.debug("doDemposition: sending decompsed alarm : " + decompseAlarm.getIdentifier());
				if(decompseAlarm.getPerceivedSeverity() != PerceivedSeverity.CLEAR) {
					decompseAlarm.setPerceivedSeverity(PerceivedSeverity.CRITICAL);    
				}
				GFPUtil.forwardOrCascadeAlarm(decompseAlarm, AlarmDelegationType.FORWARD, null);
//				decompseAlarm.setSentToNOM(true);   
				enrichedJuniperAlarm.setDecomposed(true); 
			}     
			if (log.isTraceEnabled())
				log.trace("doDemposition: setting deomposed alarms List " + enrichedJuniperAlarm.getCustomFieldValue(GFPFields.SEQNUMBER) + " size = " + decomposedAlarms.size()); 
			enrichedJuniperAlarm.setDecomposedAlarms(decomposedAlarms);    
		}  
		
	}
*/
	//RelatedCLLI=<value> and RelatedPortAID=<value> will be populated into the RUBY INFO1 field if 
	//related link has any alarm (as in original requirement)  OR if there is no alarm on the 
	//related link.	
//	private void enrichCienaAlarmsForAAF_DA(String pportInstance, EnrichedJuniperAlarm alarm) {
//
//		alarm.setCustomFieldValue(GFPFields.INFO1, alarm.getCustomFieldValue(GFPFields.INFO1) +
//				" RelatedCLLI=<" + alarm.getRemoteDeviceName() + "> " +
//				" RelatedPortAID=<" + alarm.getRemotePortAid() + ">");	
//	}


	private void setAlarmAttributesForJunitTesting(
			EnrichedJuniperAlarm enrichedJuniperAlarm) {

		if (log.isTraceEnabled())
			log.trace("THIS IS A JUNIT TEST, FIXING PREPROCESSOR PROVIDED FIELDS !!!!!!!!!!");
		
		enrichedJuniperAlarm.setSuppressed(false);
		String value = null;
	

		enrichedJuniperAlarm.setRemoteDeviceIpaddr(enrichedJuniperAlarm.getCustomFieldValue("remoteDeviceIpaddr"));
		enrichedJuniperAlarm.setRemotePortId(enrichedJuniperAlarm.getCustomFieldValue("remotePortAid"));
		enrichedJuniperAlarm.setRemoteDeviceType(enrichedJuniperAlarm.getCustomFieldValue("remoteDeviceType"));
		enrichedJuniperAlarm.setRemoteDeviceName(enrichedJuniperAlarm.getCustomFieldValue("remoteDeviceName"));

		
		if ((enrichedJuniperAlarm.getOriginatingManagedEntity().split(" ")[0]).equals("PPORT")) {
			enrichedJuniperAlarm.setRemoteDeviceModel(
				enrichedJuniperAlarm.getCustomFieldValue("remoteDeviceModel"));
			enrichedJuniperAlarm.setRemotePportInstanceName(
				enrichedJuniperAlarm.getCustomFieldValue("remotePportInstanceName"));	
			enrichedJuniperAlarm.setRemotePportAafdaRole(enrichedJuniperAlarm.getCustomFieldValue("remoteAAF_DARole"));
			enrichedJuniperAlarm.setRemotePportDiverseCktId(enrichedJuniperAlarm.getCustomFieldValue("remoreDiverseCkt"));
		}

		enrichedJuniperAlarm.setPortAid(enrichedJuniperAlarm.getCustomFieldValue("portAid"));

		//enrichedJuniperAlarm.setTunable(enrichedJuniperAlarm.getCustomFieldValue("EventKey"));
		enrichedJuniperAlarm.setDeviceName(enrichedJuniperAlarm.getCustomFieldValue("deviceName"));
		enrichedJuniperAlarm.setDeviceType(enrichedJuniperAlarm.getCustomFieldValue("deviceType"));
		enrichedJuniperAlarm.setDeviceModel(enrichedJuniperAlarm.getCustomFieldValue("deviceModel"));
		enrichedJuniperAlarm.setNodeType(enrichedJuniperAlarm.getCustomFieldValue("nodeType"));
		
		if (log.isTraceEnabled())
			log.trace(" sets nodeType=" + enrichedJuniperAlarm.getNodeType());	
	
		value = enrichedJuniperAlarm.getCustomFieldValue("deviceLevelExists");
		
		if (value != null && enrichedJuniperAlarm.getCustomFieldValue("deviceLevelExists").equals("true")) {
			enrichedJuniperAlarm.setDeviceLevelExists(true);
		} else {
			enrichedJuniperAlarm.setDeviceLevelExists(false);
		}

	}

	private void getAAF_DA_Info1Information(EnrichedJuniperAlarm alarm) {

		// <Info1>AAFDARole=&lt;AAF-PRIMARY&gt; DiverseCircuitID=&lt;JU101/GE1N/CRCYNVAA04T/CRCYNVAA17T&gt;  RelatedCLLI=&lt;DARIA-CRCYNVAA04T&gt; RelatedPortAID=&lt;ge-5/0/0&gt;</Info1>

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "getAAF_DA_Info1Information() for :" + alarm.getIdentifier() + " role :" + 
					alarm.getNodeType());
		}

		String managedObjectClass = alarm.getOriginatingManagedEntity().split(" ")[0];
		String managedObjectInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		String role = null;
		String circuit = null;

		// Get pport info
		if(managedObjectClass.equals(PPORT) && managedObjectInstance != null &&
				alarm.getCustomFieldValue(GFPFields.CLASSIFICATION).contains("NFO")) {

			if(alarm.getNodeType().equals(EnrichedAlarm.CE)) {

				// make the query into topology for local port information
				// for CE devices we have now the diverse circuit, aafda role, and clfi
				// for PE devices we have now the clfi
				//IpagJuniperLinkDownTopoAccess.getInstance().FetchLocalPPortLevelInformationForLinkDownAlarm(managedObjectInstance, alarm);
				role = alarm.getAafDaRole();
				//role = alarm.getAafdaRole();
				circuit = alarm.getDiverseCircuitID();
				//circuit = alarm.getDiverseCkt();
			} else {
				// the remote aafda role and diverse circuit is retrieved by the preprocessor
				role = alarm.getRemotePportAafdaRole();
				circuit = alarm.getRemotePportDiverseCktId();
			}

			// if this port has a peering port entry then we continue
			// the clfi is used to access these nodes
			// did we already fetch the peering port?, do we have the aaf_da role?
			if(alarm.getVar().getBoolean("triedPeeringPort")) {
				if(alarm.getPeeringPort() != null && role != null && !role.isEmpty()){
					alarm.setCustomFieldValue(GFPFields.INFO1, "AAFDARole=<" + role + "> " 
							+ "DiverseCircuitID=<" + circuit + ">");
				}
			} else {
				// we haven't already fetched the peering port
//				if(IpagJuniperLinkDownTopoAccess.getInstance().FetchPeeringTableInformation(alarm)) {
//					// info1 value to be populated since the device has entry in peering table and has
//					// remote aafda role value populated in topology.
//					// AAFDARole=&lt;AAF-PRIMARY&gt; DiverseCircuitID=&lt;JU101/GE1N/CRCYNVAA04T/CRCYNVAA17T&gt; 
//					if(alarm.getPeeringPort() != null && role != null && !role.isEmpty()){
//						alarm.setCustomFieldValue(GFPFields.INFO1, "AAFDARole=<" + role + "> " 
//								+ "DiverseCircuitID=<" + circuit + ">");
//					}
//				}
			}
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "getAAF_DA_Info1Information()");
		}		
	}

	private void getAllTopologyInformation(EnrichedJuniperAlarm alarm) {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "getAllTopologyInformation()");
		}

		// Find the aafda-role
		// clfi of the remote CE_PPort
		String managedObjectClass = alarm.getOriginatingManagedEntity().split(" ")[0];
		String managedObjectInstance = alarm.getOriginatingManagedEntity().split(" ")[1];

		// if the target is an LPort then get the containing pport and continue with the
		// processing
/*		if(managedObjectClass.equals(LPORT)) {
			managedObjectInstance = IpagJuniperLinkDownTopoAccess.getInstance().FetchContainingPPort(alarm, managedObjectInstance);
			// save the containing PPort instance for this LPORT for later use
			alarm.setContainingPPort(managedObjectInstance);
			managedObjectClass = PPORT;
		}
*/		
		// Get pport info
		if(managedObjectClass.equals(PPORT) && managedObjectInstance != null) {

			if(alarm.getRemoteDeviceIpaddr() != null && alarm.getRemotePortAid() != null ) {
				String remotePortKey = alarm.getRemoteDeviceIpaddr() + "/" + alarm.getRemotePortAid();
				alarm.setRemotePortKey(remotePortKey);
			}

			// get the device of this pport
			IpagJuniperLinkDownTopoAccess.getInstance().FetchDeviceFromPPort(managedObjectInstance, alarm);

			// make a query to topology for the peering table 
			// if this has a peering port entry then it may be AAF_DA
			IpagJuniperLinkDownTopoAccess.getInstance().FetchPeeringTableInformation(alarm);
			//commented out because aaf_da info is being populated in prisec pdvp
			 
			// add portaid to component field 
			fixComponentForPPort(alarm); 
			
			IpagJuniperLinkDownTopoAccess.getInstance().AreAnyEVCs(managedObjectInstance, alarm);
			
			
		} else {
			if(managedObjectClass.equals(DEVICE))
			{
				// this changed with HLD-256258a-GFP-Data-200 where instead of the lagid is taken out
				// of the component field, it is taken from the reason code.
				//String componentLagId = getLagIdFromComponent(alarm.getCustomFieldValue(GFPFields.COMPONENT), alarm);
				String reasonCodeLagId = getLagIdFromReasonCode(alarm.getCustomFieldValue(GFPFields.REASON_CODE), alarm);
				if (log.isTraceEnabled()) {
					//log.trace("Lag extracted from the Component field: " + componentLagId);
					log.trace("Lag extracted from the reason_code field: " + reasonCodeLagId);
				}
				
				// tj
				if(reasonCodeLagId != null && !reasonCodeLagId.isEmpty()) {
					alarm.setComponentLagId(reasonCodeLagId);
					
					alarm.setDeviceInstance(managedObjectInstance);
					NodeManager.getPportSetMatchedPortLagID(managedObjectInstance, reasonCodeLagId, alarm);
					
					// if there are no pports on this device associated with the lag then we will suppress this
					// alarm, no need to go further.
					if(alarm.getLagIdPportset().isEmpty())
						return;
				}
				
				// tj: done
				
				// get one local pport with the lag and then get the remote end
				String localPPort = alarm.getLagIdPportset().iterator().next();
				
				IpagJuniperLinkDownTopoAccess.getInstance().FetchRemoteDeviceRelatedLag(reasonCodeLagId, localPPort, alarm);
				fixComponentForDevice(alarm);
			} 
		}
		
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "getAllTopologyInformation()");
		}

	}

	 private String getLagIdFromReasonCode(String customFieldValue, EnrichedJuniperAlarm alarm) {
		String lagId = null;
		
		if(customFieldValue.startsWith("ae"))
			lagId = customFieldValue;

		// does the lagId contain the index, if so drop it off
		if(lagId != null && lagId.contains("_"))
			lagId = lagId.substring(0, lagId.indexOf("_"));
		
		 // this lag is on a sub interface so drop everything after the .
		 if(lagId != null && lagId.contains(".")) {
			 alarm.setIsSubInterface(true);
			 lagId = lagId.substring(0, lagId.indexOf("."));
		 }
		return lagId;
	}


/*	private String getLagIdFromComponent(String component, EnrichedJuniperAlarm alarm) {
		 // examples:    PortLagId=&lt;ae3.2452&gt;     PortLagId=&lt;ae26&gt;      PortLagId=&lt;ae44.76&gt;

		 String lagId = null;
		 int lagPos = 0;
		 int lessThanPos = 0;
		 int greaterThanPos = 0;

		 // this should give me back <value>
		//agId = GFPUtil.parseSpecifiedValueFromField("PortLagId", component, " ");
		 
		lagPos = component.indexOf("PortLagId");
		lessThanPos = component.indexOf('<', lagPos);
		greaterThanPos = component.indexOf('>', lessThanPos);
		
		if(lagPos > 0 && lessThanPos > lagPos && greaterThanPos > lessThanPos) {
			 lagId = component.substring(lessThanPos+1, greaterThanPos);
			 
			 // this lag is on a sub interface so drop everything after the .
			 if(lagId.contains(".")) {
				 alarm.setIsSubInterface(true);
				 lagId = lagId.substring(0, lagId.indexOf("."));
			 }
		} 

		 return lagId;
	 }
	 */
	 
	private void fixComponentForPPort(EnrichedJuniperAlarm alarm) {
		
		String component = alarm.getCustomFieldValue(GFPFields.COMPONENT);
		String newComponent = null;
	
		if(component.contains(PORT_AID))  
			newComponent = component.replaceAll("portAID=<\\w*/*\\w*/*\\w*>", "portAID=<" + alarm.getPortAid() + ">");
		else  
			newComponent = component + "portAID=" + alarm.getPortAid();

		// now we append the portLagId onto the component field if we have one from topology
		//removed PortLagId to fix MR gfpc140135, add for fix to MR gfpc140766
		if((alarm.getPortLagId()!=null) && !(alarm.getPortLagId().isEmpty()))
				newComponent = newComponent + " PortLagId=<" + alarm.getPortLagId() + ">";
		
		alarm.setCustomFieldValue(GFPFields.COMPONENT, newComponent);

	}

	private void fixComponentForDevice(EnrichedJuniperAlarm alarm) {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "fixComponentForDevice()");
		}

		String component = alarm.getCustomFieldValue(GFPFields.COMPONENT);
		String newComponent = null;
		HashSet<String> lagIdPportset = alarm.getLagPportAidset();
		
		if (lagIdPportset.size() > 0)
		{
			if (log.isTraceEnabled())
				log.trace("Found port/s for this lag.");
			
			newComponent = component;
	
			Iterator<String> i = lagIdPportset.iterator();
			while(i.hasNext())
			{
				String port = (String)i.next();
				newComponent = newComponent + " portAID=<" + port + ">";			
			}
			
		}

		if (log.isTraceEnabled())
			log.trace("New Component is:" + newComponent);

		alarm.setCustomFieldValue(GFPFields.COMPONENT, newComponent);
		
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "fixComponentForDevice()");
		}

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

		//log.trace("\n\n**** test ****Alarm in WM is: \n" + alarmInWorkingMemory.toFormattedString());
		
		//log.trace("\n\n**** test ****New Alarm is: \n" + newAlarm.toFormattedString() + "\n");
		
		if (log.isTraceEnabled()) {
			String axml = newAlarm.toXMLString();
			axml = axml.replaceAll("\\n", " ");
			log.trace("Incoming updated alarm: "+axml);
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
			if (log.isTraceEnabled()) {
				LogHelper.method(log, "Severity of new alarm:" + newAlarm.getPerceivedSeverity() 
						+ " Severity from alarm in WM:" + alarmInWM.getPerceivedSeverity(), newAlarm.getIdentifier());
			}
			
			if (newAlarm.getPerceivedSeverity() != alarmInWM.getPerceivedSeverity()) {
				
				if (log.isTraceEnabled()) {
					LogHelper.method(log, "The severity of new alarm is not equal to WM.");
				}

				if (newAlarm.getPerceivedSeverity() == PerceivedSeverity.CLEAR
						&& alarmInWM.getNetworkState() == NetworkState.NOT_CLEARED) {
					/*
					 * 
					 */

					if (log.isTraceEnabled()) {
						LogHelper.method(log, "Clear alarm received", newAlarm.getIdentifier());
					}

					EnrichedJuniperAlarm a = (EnrichedJuniperAlarm) alarmInWorkingMemory;
					String eventKey = a.getCustomFieldValue(GFPFields.EVENT_KEY);
					a.setSeverity(4);
					a.setPerceivedSeverity(PerceivedSeverity.CLEAR);
					
					a.setCustomFieldValue(GFPFields.LAST_CLEAR_TIME, ""+System.currentTimeMillis()/1000);

					// DF - we also have to set the sequence number to that of the clear alarm
					a.setCustomFieldValue(GFPFields.SEQNUMBER, newAlarm.getCustomFieldValue(GFPFields.SEQNUMBER));

					if (log.isTraceEnabled()) {
						LogHelper.method(log, "Is the alarm in WM suppressed:" + a.isSuppressed(), a.getIdentifier());
					}
					
					// DF - if the alarm in WM has been sent somewhere then we must also send the clear
					if (!a.isSuppressed()) 
					{
						
						// DF - again here because the alarm was already sent (as tested above)
						// the clear alarm will not be sent because of the canBeSent() check in Util.sendAlarm()
						// so I will set IsSent temporarily to false to fool the sendAlarm method
						a.setIsSent(false);
						a.setIsClear(false);
						
						if("YES".equalsIgnoreCase(newAlarm.getCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA))) {
							a.setCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA, "YES"); 
						} 				

						// we don't have to send the clear to any scenarios in juniperES value pack because
						// the alarms are not preserved in WM.   No need to clear them...
								
						if("YES".equalsIgnoreCase(newAlarm.getCustomFieldValue(GFPFields. IS_PURGE_INTERVAL_EXPIRED)))
							a.setCustomFieldValue(GFPFields. IS_PURGE_INTERVAL_EXPIRED, "YES"); 
						
						GFPUtil.populateEnrichedAlarmObj(a);

						Util.whereToSendThenSend(a);
						
						a.setIsSent(true);
						String moClass = a.getOriginatingManagedEntity().split(" ")[0];
						
						if(a.getDecomposed() && "50002/100/21".equalsIgnoreCase(a.getCustomFieldValue(GFPFields.EVENT_KEY))) {
							GFPUtil.forwardAlarmToDecomposerInstance(a, "JUNIPER_DECOMPOSER");
						}
						else if(eventKey.equals("50003/100/1")) {
							if (a.getDeviceLevelExist() && a.getRemoteDeviceType() != null && 
									a.getRemoteDeviceType().equals("CIENA NTE") && !(GFPUtil.isAafDaAlarm(a))) 
								GFPUtil.forwardAlarmToDecomposerInstance(a, "JUNIPER_DECOMPOSER");
						}
						else if (eventKey.equals("50003/100/7") && moClass.equals("DEVICE") ) {
							GFPUtil.forwardAlarmToDecomposerInstance(a, "JUNIPER_DECOMPOSER");
						}
						else if ( (eventKey.equals("50002/100/19")) && !(GFPUtil.isAafDaAlarm(a)) ) {
							GFPUtil.forwardAlarmToDecomposerInstance(a, "JUNIPER_DECOMPOSER");
						}
						else if ( eventKey.equals("50003/100/6") ) {
							GFPUtil.forwardAlarmToDecomposerInstance(a, "JUNIPER_DECOMPOSER");
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

			if (log.isTraceEnabled()) {
				LogHelper.method(log, "Completed clear identification, now will process clear.", newAlarm.getIdentifier());
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
		//bgpL2vpn_VPWS:21641_131073(Chronic)
		
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
	
	private void enrichCienaLDForSeverity(EnrichedJuniperAlarm alarm)
	{
		String pportInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
		
		String nmvlan = "";
		String slavlan = "";
		String device_type = "";
		String remote_device_type = "";
		String multinni = "";

		nmvlan = alarm.getNmvlan(); 
		slavlan = alarm.getSlavlan(); 
		remote_device_type = alarm.getRemoteDeviceType(); 
		device_type = alarm.getDeviceType(); 

		String deviceInstance = pportInstance.split("/")[0];
		//multinni = queryMultinni(deviceInstance);

		if (log.isDebugEnabled())
			log.debug("nmvlan = "+nmvlan+", slavlan = "+slavlan+", remote_device_type = "+remote_device_type+", device_type = "+device_type+", multinni = "+multinni);
		
		alarm.setDeviceInstance(deviceInstance);
		alarm.setPportInstance(pportInstance);

		if ((nmvlan != null && nmvlan.length() > 0) ||
		    (slavlan != null && slavlan.length() > 0))
			alarm.setSlavlan_nmvlan("Y");
		else
			alarm.setSlavlan_nmvlan("");
	}

	
}
