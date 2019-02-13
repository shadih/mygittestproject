/**
 * 
 */
package com.att.gfp.helper;

/**
 * Utility Class to define the customFields used for GFP.
 * 
 * @author 
 * 
 */
public class GFPFields {

	// IPAG fields
	public static final String AGING = "aging";
	public static final String ALERT_ID = "alert-id";
	public static final String BE_ALARM_TIME = "be_time";
	public static final String CIRCUIT_ID = "circuit-id";
	public static final String CIRCUIT_TYPE = "circuitType";
	public static final String COMPONENT = "component";
	public static final String CLASSIFICATION = "classification";
	public static final String CLLI = "clli";
	public static final String CLFI = "clfi";
	public static final String CUSTOMER = "customer";
	public static final String DOMAIN = "domain";
	public static final String LAST_UPDATE = "last-update";
	public static final String LAST_CLEAR_TIME = "last-clear-time"; 
	public static final String INFO1 = "info1";
	public static final String INFO2 = "info2";
	public static final String INFO3 = "info3";
	public static final String MCN = "mcn";
	public static final String NODE_NAME = "node-name";
	public static final String SECONDARY_ALERT_ID = "secondary-alert-id";  
	public static final String EVENT_KEY = "EventKey";  
	public static final String XD_ALARM_TIME = "xd-alarm-time";
	public static final String CFM_ALARM_TIME = "cfm-alarm-time";
	public static final String SM_SOURCEDOMAIN = "sm-sourcedomain";
	public static final String SM_EVENT = "sm-event";
	public static final String SM_CLASS = "sm-class";
	public static final String SM_EVENT_TEXT = "sm-event-text";
	public static final String CLCI = "clci";
	public static final String BMP_CLCI = "bmp-clci";
	public static final String EVC_NAME = "evc-name";
	public static final String ACNABAN = "acnaban";
	public static final String INFO = "info";
	public static final String SEC_ALARM_TIME = "sec-alarm-time";
	public static final String RUBY_DELAY = "ruby-delay";
	public static final String REASON = "reason";
	public static final String REASON_CODE = "reason_code";
	public static final String EMS = "ems";
	public static final String IS_PURGE_INTERVAL_EXPIRED = "PURGE_INTERVAL_EXPIRED";
	public static final String FE_TIME_STAMP = "fe_time_stamp"; 
	public static final String SEND_TO_RUBY = "send-to-ruby";
	public static final String PURGE_INTERVAL = "purge-interval";
	public static final String NTE = "NTE";
	public static final String EGS = "EGS";
	public static final String IPAG = "IPAG";
	public static final String SROUTER = "SROUTER";   
	public static final String OEWSROUTER = "OEW-SROUTER";
	public static final String OEWVPLS = "OEW-VPLS";
	public static final String REMOTENTECLLI = "RemoteNTECLLI";
	public static final String REMOTEPORTAID = "RemotePortAID";
	public static final String SLOT = "Slot";
	public static final String SLOTNAME = "slotname";
	public static final String PORT = "Port";
	public static final String UNI = "UNI";  
	public static final String UNINNI = "uni_nni";	
	public static final String IP_ADDRESS = "IP_ADDRESS";
	public static final String REMOTEDEVICENAME =  "remote_device_name";
	public static final String CLFIUNKNOWN =  "CLFI-UNKNOWN";  
	public static final String DEVICE_MODEL_NETVANTA838 = "NetVanta838"; 
	public static final String BE_ALARM_TIME_STAMP = "be_time_stamp";
	public static final String SEQNUMBER  = "SeqNumber";  
	public static final String CDC_OPERATION  = "operation";
	public static final String CDC_SUBSCRIPTIONID  = "subscriptionId";
	public static final String CDC_SUBSCRIPTIONTYPE  = "subscriptionType";
	public static final String CDC_FROMAPPID  = "fromAppId";  
	public static final String CDC_PUBEVENTTYPE  = "pubEventType";
	public static final String CDC_INITIALIZE  = "initialize";
	public static final String CDC_INITIALIZETIMESTAMP  = "initializeTimeStamp";
	public static final String SECONDARY_TIMESTAMP  = "secondary-timestamp";
	public static final String SECONDARYTIMESTAMP  = "SecondaryTimeStamp";
	public static final String SECONDARYSOURCEDOMAIN  = "SecondarySourceDomain";
	public static final String VRF_NAME = "vrf-name";   
	public static final String VRF_IN_ALARM = "vrf-in-alarm";
	public static final String UNICKT = "unickt"; 
	public static final String IS_GENERATED_BY_UCA = "IS_GENERATED_BY_UCA";  
	public static final String SUPER_EMUX_DEVICE_TYPE = "CN5150";  
	public static final String IS_GENERATED_BY_DECOMPOSITION = "isGeneratedByDecomposition";   
	public static final String HASSECONDARY = "HasSecondary";    
	public static final String DECOMPOSED_ALARMS_BEAN = "DecomposedAlarms";
	public static final String NOM_ALARM_FORWARDER_BEAN = "nomAlarmForwarder"; 
	public static final String CPE_CDC_ALARM_FORWARDER_BEAN = "CpeAlarms"; 
	public static final String DECOMPOSER_JMS_ALARM_FORWARDER ="decomposerJmsAlarmForwarder";
	public static final String REMOTE_DEVICE_TYPE ="remoteDeviceType";
	public static final String REMOTE_DEVICE_NAME ="remoteDeviceName";
	public static final String REMOTE_DEVICE_MODEL ="remoteDeviceModel";
	public static final String DEVICE_TYPE ="deviceType";	
	public static final String REMOTE_PPORT_INSTANCE_NAME = "remotePportInstanceName";
	public static final String DA = "DA";
	public static final String AAF_SECONDARY = "AAF-SECONDARY";
	public static final String AAF_PRIMARY = "AAF-PRIMARY";
	public static final String DEVICE_NAME ="deviceName"; 
	public static final String DEVICE_MODEL ="deviceModel"; 
	public static final String G2SUPPRESS = "G2Suppress";
	public static final String UVERSE_CDC_ALARM_FORWARDER_BEAN = "UverseAlarms"; 
	public static final String JUNIPER_JMS_ALARM_FORWARDER ="juniperJmsAlarmForwarder";
	public static final String ADTRAN_JMS_ALARM_FORWARDER ="adtranJmsAlarmForwarder";
	public static final String CIENA_JMS_ALARM_FORWARDER ="cienaJmsAlarmForwarder";
	public static final String NFO_MOBILITYUNI = "NFOMOBILITYUNI"; 
	public static final String REDUNDANTNNIPORTS = "redundantNNIPorts"; 
	public static final String REMOTE_DEVICE_IPADDR = "remoteDeviceIpaddr"; 
	public static final String REMOTE_PPORT_AAFDAROLE = "remotePportAafdaRole"; 
	public static final String REMOTE_PPORT_NAME = "remotePportName";
	public static final String AAFDAROLE = "aafdaRole"; 
	public static final String DIVERSECIRCUITID = "diverseCircuitID"; 
	public static final String RELATEDCLLI = "relatedCLLI"; 
	public static final String RELATEDPORTAID = "relatedPortAid"; 
	public static final String LOCALPEERINGPORT = "localPeeringPort"; 
	public static final String REMOTEPEERINGPORT = "remotePeeringPort";
	public static final String REMOTE_PORT_AID = "remotePortAid";
	public static final String GCP_DEVICE_TYPE = "gcpDeviceType";
	public static final String ISSENDTOCPECDC = "IssendToCpeCdc";
	public static final String ISSENDTOCDC = "isSendToCdc";
	public static final String DEVICEROLE = "deviceRole";
	public static final String DEVICESUBROLE = "deviceSubRole";
	public static final String REMOVE_ADDITIONAL_CUSTOM_FIELDS = "removeAdditionalCustomFields";
	

}
