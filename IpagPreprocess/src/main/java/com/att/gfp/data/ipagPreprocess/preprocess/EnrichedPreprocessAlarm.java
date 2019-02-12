package com.att.gfp.data.ipagPreprocess.preprocess;

/**
 * 
 */

import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.AlarmState;
import com.att.gfp.data.util.CDCFields;
import com.att.gfp.data.util.NetcoolFields;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
import com.hp.uca.expert.alarm.Alarm;  
import com.hp.uca.expert.alarm.AlarmHelper;  
import com.hp.uca.expert.x733alarm.PerceivedSeverity;


@XmlRootElement
public class EnrichedPreprocessAlarm extends com.att.gfp.data.ipagAlarm.EnrichedAlarm {

	private static final String CE = "CE";
	private static final String PE = "PE";
	/**
	 * 
	 */
	private static final String F_REMOTEDATASOURCE = "remoteDataSource";
	private static final String F_REMOTENODETYPE = "remoteNodeType"; 
	private static final String F_CDCSUBSCRIPTIONTYPE = "cdcSubscriptionType";  

	// remotedevicename 


	/**
	 * 
	 */
	private static final long serialVersionUID = -5235137849600943223L;

	private static Logger log = LoggerFactory.getLogger(EnrichedPreprocessAlarm.class);

	/**
	 * Alarm extension attributes
	 */
	private String remoteDataSource;   
	private String remoteNodeType; 
	private String cdcSubscriptionType;
	private String containingPportClfi; 
	private String contaiiningPportremDevType;
	private String containingPportRemotePPortInstance;
	private String provStatus;

	public String getContainingPportRemotePPortInstance() {
		return containingPportRemotePPortInstance;
	}

	public void setContainingPportRemotePPortInstance(
			String containingPportRemotePPortInstance) {
		this.containingPportRemotePPortInstance = containingPportRemotePPortInstance;
	} 	

	public String getContaiiningPportremDevType() {
		return contaiiningPportremDevType; 
	}

	public void setContaiiningPportremDevType(String contaiiningPportremDevType) {
		this.contaiiningPportremDevType = contaiiningPportremDevType;
	}


	public String getContainingPportClfi() {
		return containingPportClfi;
	}

	public void setContainingPportClfi(String containingPportClfi) {
		this.containingPportClfi = containingPportClfi;
	}

	public String getCdcSubscriptionType() {
		return cdcSubscriptionType;
	}

	public void setCdcSubscriptionType(String cdcSubscriptionType) {
		this.cdcSubscriptionType = cdcSubscriptionType;
	}

	public String getRemoteNodeType() {
		return remoteNodeType; 
	}

	public void setRemoteNodeType(String remoteNodeType) {
		this.remoteNodeType = remoteNodeType;
	}

	public String getRemoteDataSource() {
		return remoteDataSource;
	}

	public void setRemoteDataSource(String remoteDataSource) {
		this.remoteDataSource = remoteDataSource;
	}
	
	public String getProvStatus() {
		return  provStatus;
	}
	
	public void setProvStatus(String provStatus) {
		this.provStatus = provStatus;
	}

	/**
	 * EnrichedAlarm Constructor
	 */
	public EnrichedPreprocessAlarm() {
		super();
	}
	/**
	 * EnrichedAlarm Copy Constructor.
	 * 
	 * This constructor constructs an Extended Alarm and uses the Alarm provided as
	 * parameter to set all the standard alarm fields
	 * @throws Exception 
	 */
	public EnrichedPreprocessAlarm(Alarm alarm) {
		super(alarm);


		// initialize the suppress value to not suppress for now 

		// this number will allow identical alarms to be differentiated in the rules
		//sequenceNumber = System.currentTimeMillis();
	}

	public EnrichedPreprocessAlarm(EnrichedPreprocessAlarm alarm) {
		super(alarm);
		remoteDataSource = null;
		remoteNodeType = null;  
	}  




	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.uca.expert.alarm.AlarmCommon#toFormattedString()
	 */
	@Override
	public String toFormattedString() {
		StringBuffer toStringBuffer= AlarmHelper.toFormattedStringBuffer(this);

		AlarmHelper.addFormatedItem(toStringBuffer, F_REMOTEDATASOURCE , getRemoteDataSource());   
		AlarmHelper.addFormatedItem(toStringBuffer, F_REMOTENODETYPE , getRemoteNodeType());    
		AlarmHelper.addFormatedItem(toStringBuffer, F_CDCSUBSCRIPTIONTYPE , getCdcSubscriptionType());      


		return toStringBuffer.toString();
	}



	public void setHealthTrapReasonText() {

		String reason = getCustomFieldValue(NetcoolFields.REASON);
		setCustomFieldValue(NetcoolFields.REASON, reason + "; CDM_IN=" + System.currentTimeMillis()/1000);

	}

	public void createMissingCustomFields() {

		// current time for some of the custom fields
		String now = String.valueOf(System.currentTimeMillis()/1000);

		String managedObjectClass = getOriginatingManagedEntity().split(" ")[0];
		String managedObjectInstance = getOriginatingManagedEntity().split(" ")[1];

		// default is now zero aging
		setCustomFieldValue(NetcoolFields.AGING, "0");  

		// set to current time
		if(getCustomFieldValue(NetcoolFields.BE_ALARM_TIME_STAMP) == null) {
			setCustomFieldValue(NetcoolFields.BE_ALARM_TIME_STAMP, now);
		} 
		if(getCustomFieldValue(NetcoolFields.FE_TIME) == null) {
			setCustomFieldValue(NetcoolFields.FE_TIME, now);
		}    
		String component = "";
		if(getCustomFieldValue(NetcoolFields.COMPONENT)!= null) {
			component = getCustomFieldValue(NetcoolFields.COMPONENT);
		} 
		// component  
		if(getPortAid() == null) 
			// deviceType=<[device-type]> deviceModel=<[device-model]>" 
			setCustomFieldValue(NetcoolFields.COMPONENT, "deviceType=" + "<" +
					getDeviceType() + ">" + " deviceModel=" + "<" + getDeviceModel() + ">" + " " + component);
		else 
			// deviceType=<[device-type]> deviceModel=<[device-model]> portAID=<[port-aid]>"
			setCustomFieldValue(NetcoolFields.COMPONENT, 
					"deviceType=" + "<" + getDeviceType() +  ">" + 
							" deviceModel=" + "<" + getDeviceModel() +  ">" +   
							" portAID=" + "<" + getPortAid()+    ">" +" " + component);   

		// we just updated this alarm so...
		setCustomFieldValue(NetcoolFields.LAST_UPDATE, now);   

		// set to device name
		if ( getCustomFieldValue(NetcoolFields.EVENT_KEY).equals("50005/6/6001")) {
			setCustomFieldValue(NetcoolFields.NODE_NAME, "PMOSS");
		}
		else {
			setCustomFieldValue(NetcoolFields.NODE_NAME, getDeviceName()); 
			if((getCustomFieldValue(NetcoolFields.NODE_NAME) == null) || ("".equals(getCustomFieldValue(NetcoolFields.NODE_NAME)))) {
				if("ALL".equalsIgnoreCase(managedObjectInstance)) { 
					setCustomFieldValue(NetcoolFields.NODE_NAME, "ALL"); 
				}
			} 
		}

		// [emsName]-[domain-name]
		//Removed the ems field
		//		setCustomFieldValue(NetcoolFields.SM_SOURCEDOMAIN, 
		//				getCustomFieldValue(NetcoolFields.EMS) + "-" +
		//				getCustomFieldValue(NetcoolFields.DOMAIN)); 

		setCustomFieldValue(NetcoolFields.SM_SOURCEDOMAIN, 
				getCustomFieldValue(NetcoolFields.DOMAIN));   

		// managed object class	
		setCustomFieldValue(NetcoolFields.SM_CLASS, managedObjectClass); 			




		// should have been set earlier
		if(getCustomFieldValue(NetcoolFields.CLASSIFICATION) == null ) {
			setCustomFieldValue(NetcoolFields.CLASSIFICATION, ""); 
		} 

		// should have been set earlier
		if(getCustomFieldValue(NetcoolFields.CLLI) == null ) {
			setCustomFieldValue(NetcoolFields.CLLI, ""); 
		} 

		if ((getCustomFieldValue(NetcoolFields.CLFI) == null || getCustomFieldValue(NetcoolFields.CLFI).isEmpty())  &&
			("LPORT".equalsIgnoreCase(managedObjectClass)) ) {			
				setCustomFieldValue(NetcoolFields.CLFI, getContainingPportClfi());   		
		} 

		// should have been set earlier
		if(getCustomFieldValue(NetcoolFields.CLFI) == null ) {
			setCustomFieldValue(NetcoolFields.CLFI, ""); 
		} 

		if(getCdcSubscriptionType() == null) {
			setCdcSubscriptionType("");  
		}
		// should have been set earlier
		if(getCustomFieldValue(NetcoolFields.DOMAIN) == null ) {
			setCustomFieldValue(NetcoolFields.DOMAIN, ""); 
		}

		// should have been set earlier
		if(getCustomFieldValue(NetcoolFields.CLCI) == null ) {
			setCustomFieldValue(NetcoolFields.CLCI, ""); 
		}

		// should have been set earlier
		if(getCustomFieldValue(NetcoolFields.BMP_CLCI) == null ) {
			setCustomFieldValue(NetcoolFields.BMP_CLCI, ""); 
		} 

		// should have been set earlier
		if(getCustomFieldValue(NetcoolFields.EVENT_KEY) == null ) {
			setCustomFieldValue(NetcoolFields.EVENT_KEY, ""); 
		}



		// will be set when a clear alarm arrives
		if(getCustomFieldValue(NetcoolFields.LAST_CLEAR_TIME) == null ) {
			setCustomFieldValue(NetcoolFields.LAST_CLEAR_TIME, "0"); 
		}         




		// None of these are set during pre-Processing
		if(getCustomFieldValue(NetcoolFields.INFO1) == null ) {
			setCustomFieldValue(NetcoolFields.INFO1, ""); 
		}
		if(getCustomFieldValue(NetcoolFields.INFO2) == null ) {
			setCustomFieldValue(NetcoolFields.INFO2, ""); 
		}
		if(getCustomFieldValue(NetcoolFields.INFO3) == null ) {
			setCustomFieldValue(NetcoolFields.INFO3, ""); 
		}
		if(getCustomFieldValue(NetcoolFields.INFO) == null ) {
			setCustomFieldValue(NetcoolFields.INFO, ""); 
		} 



		// not set by preprocess
		if(getCustomFieldValue(NetcoolFields.EVC_NAME) == null ) {
			setCustomFieldValue(NetcoolFields.EVC_NAME, ""); 
		}

		// unclear what this is to be set to
		if(getCustomFieldValue(NetcoolFields.ACNABAN) == null ) {
			setCustomFieldValue(NetcoolFields.ACNABAN, ""); 
		} 

		// not present in the doc
		if(getCustomFieldValue(NetcoolFields.SEC_ALARM_TIME) == null ) {
			setCustomFieldValue(NetcoolFields.SEC_ALARM_TIME, ""); 
		} 


		if(getCustomFieldValue(NetcoolFields.RUBY_DELAY) == null ) {
			setCustomFieldValue(NetcoolFields.RUBY_DELAY, ""); 
		}  

		// not clear what to do with these in preprocessing 
		//		if(!(GFPUtil.getPmossTrapsList().contains((getCustomFieldValue(GFPFields.EVENT_KEY)))) &&
		//				!("50005/6/9208".equalsIgnoreCase(getCustomFieldValue(GFPFields.EVENT_KEY)))) {
		if(getCustomFieldValue(NetcoolFields.REASON) == null || getCustomFieldValue(NetcoolFields.REASON).isEmpty()) {  
			setCustomFieldValue(NetcoolFields.REASON, getProbableCause()); 
		}  
		//				setCustomFieldValue(NetcoolFields.REASON, getProbableCause());   
		//			}    
		//		}

		if(getCustomFieldValue(NetcoolFields.REASON_CODE) == null ) {
			setCustomFieldValue(NetcoolFields.REASON_CODE, ""); 
		} 

		// not in pre-process
		if(getCustomFieldValue(NetcoolFields.CUSTOMER) == null ) {
			setCustomFieldValue(NetcoolFields.CUSTOMER, ""); 
		}

		// not in doc
		if(getCustomFieldValue(NetcoolFields.SM_EVENT_TEXT) == null ) {
			setCustomFieldValue(NetcoolFields.SM_EVENT_TEXT, ""); 
		}

		// not in doc
		if(getCustomFieldValue(NetcoolFields.XD_ALARM_TIME) == null ) {
			setCustomFieldValue(NetcoolFields.XD_ALARM_TIME, ""); 
		}

		// not in pre-process
		if(getCustomFieldValue(NetcoolFields.CFM_ALARM_TIME) == null ) {
			setCustomFieldValue(NetcoolFields.CFM_ALARM_TIME, ""); 
		}

		// value is passed through
		if(getCustomFieldValue(NetcoolFields.SECONDARY_ALERT_ID) == null ) {
			setCustomFieldValue(NetcoolFields.SECONDARY_ALERT_ID, ""); 
		}

		// not in pre-processing
		if(getCustomFieldValue(NetcoolFields.MCN) == null ) {
			setCustomFieldValue(NetcoolFields.MCN, ""); 
		}

		// set by the CA

		setCustomFieldValue(NetcoolFields.ALERT_ID, getCustomFieldValue(GFPFields.EVENT_KEY) + "-" + managedObjectInstance + "-" + getCustomFieldValue(GFPFields.REASON_CODE));
		// not in pre-processing
		if(getCustomFieldValue(NetcoolFields.CIRCUIT_ID) == null ) {  
			setCustomFieldValue(NetcoolFields.CIRCUIT_ID, "");
		}

		// Set the sm-event of M = [alarmName]  what is alarmName? 
		if(getCustomFieldValue(NetcoolFields.SM_EVENT) == null ) {
			setCustomFieldValue(NetcoolFields.SM_EVENT, ""); 
		} 

		setCustomFieldValue(GFPFields.SECONDARYTIMESTAMP, "");
		setCustomFieldValue(GFPFields.SECONDARYSOURCEDOMAIN, "");  
		setCustomFieldValue(GFPFields.IS_GENERATED_BY_DECOMPOSITION, "false");
	}    

	/**
	 * creates the CDC(webservice) custom fields
	 *  
	 */
	//TODO: remove the hardcoded fields..fill them from topology or some other configuration file.

	public void createCDCCustomFields() {

		setCustomFieldValue(CDCFields.HISTALARM_INDEX, "");
		setCustomFieldValue(CDCFields.RealTimeFlag, "R");
		setCustomFieldValue(CDCFields.DomainSource, "IPAG02");
		setCustomFieldValue(CDCFields.EventKey, getCustomFieldValue(NetcoolFields.EVENT_KEY));
		setCustomFieldValue(CDCFields.CircuitType, "VLXP");
		setCustomFieldValue(CDCFields.EventTime, getCustomFieldValue(NetcoolFields.BE_ALARM_TIME_STAMP));
		setCustomFieldValue(CDCFields.Severity, "");
		//TODO: get evc-name from topology..
		//setCustomFieldValue(CDCFields.CircuitId, ""); 
		setCustomFieldValue(CDCFields.HasSecondary,"N");
		setCustomFieldValue(CDCFields.PrimaryKey,"");

	}


}
