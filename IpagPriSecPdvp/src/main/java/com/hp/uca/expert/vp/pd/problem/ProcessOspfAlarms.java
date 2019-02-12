package com.hp.uca.expert.vp.pd.problem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.actions.PriSecActionsFactory;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.data.ipagAlarm.Pri_Sec_Alarm;
import com.att.gfp.helper.GFPFields;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;

public class ProcessOspfAlarms {

	/**
	 * Logger used to trace this component
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(PriSecActionsFactory.class);

	private static Group globalGroup = null;
	/**
	 * 
	 */
	public ProcessOspfAlarms() {

	}

	// ##########################  OSPF ######################
	public static boolean processOspfProblem(Group group) {
		globalGroup = group;

		if (LOG.isTraceEnabled()) {
			LogHelper.enter(LOG, "processOspfProblem()", group.getName());
		}

		boolean correlated = false;
		Pri_Sec_Alarm locPPortAlarm = null;
		Pri_Sec_Alarm remPPortAlarm = null;
		Pri_Sec_Alarm ldDeviceAlarm = null;
		Pri_Sec_Alarm locLdpAlarm = null;
		Pri_Sec_Alarm remLdpAlarm = null;
		Pri_Sec_Alarm ospfAlarmIf = null;	
		Pri_Sec_Alarm ospfAlarmNbr = null;	
		Pri_Sec_Alarm locOspfAlarmNbr = null;
		Pri_Sec_Alarm remOspfAlarmNbr = null;
		Pri_Sec_Alarm ldSubInterfaceAlarm = null;
		Pri_Sec_Alarm locOspfAlarmIf = null;
		Pri_Sec_Alarm remOspfAlarmIf = null;

		String locDevice = group.getTrigger().getOriginatingManagedEntity().split(" ")[1];

		/// add logic to get set the local and remote ospf alarms like it was done for linkdown alarms

		for (Alarm alarm : group.getAlarmList()) {
			Pri_Sec_Alarm subAlarm = (Pri_Sec_Alarm) alarm;

			String eventKey = subAlarm.getCustomFieldValue(GFPFields.EVENT_KEY);
			String meClass = subAlarm.getOriginatingManagedEntity().split(" ")[0];

			if (LOG.isTraceEnabled())
				LOG.trace("Identifing alarm " + subAlarm.getIdentifier());

			if(eventKey.equals("50003/100/1") && meClass.equals("PPORT")) {
				if (LOG.isTraceEnabled())
					if ( locDevice.equals(subAlarm.getDeviceIpAddr()) ) {
						LOG.trace("It is a local pport alarm");
						locPPortAlarm = subAlarm;
					}
					else {
						LOG.trace("It is a remote pport alarm");
						remPPortAlarm = subAlarm;
					}
			}


			if(eventKey.equals("50003/100/1") && meClass.equals("DEVICE") &&
					!subAlarm.isLagSubInterfaceLinkDown()) {
				if (LOG.isTraceEnabled())
					LOG.trace("It is a LAG LD alarm");
				ldDeviceAlarm = subAlarm;
			}
			if(eventKey.equals("50003/100/1") && meClass.equals("DEVICE") &&
					subAlarm.isLagSubInterfaceLinkDown()) {
				if (LOG.isTraceEnabled())
					LOG.trace("It is a SUB INTERFACE alarm");
				ldSubInterfaceAlarm = subAlarm;
			}      
			if(eventKey.equals("50003/100/21")) {
				if ( locDevice.equals(subAlarm.getDeviceIpAddr()) ) {
					LOG.trace("It is a local LDP alarm");
					locLdpAlarm = subAlarm;
				}
				else {
					LOG.trace("It is a remote LDP alarm");
					remLdpAlarm = subAlarm;
				}
			}
			if(eventKey.equals("50003/100/3") ) {
				if ( locDevice.equals(subAlarm.getOriginatingManagedEntity().split(" ")[1]) ) {
					if (LOG.isTraceEnabled())
						LOG.trace("It is a local NBR alarm");
					locOspfAlarmNbr = subAlarm;
				}
				else {
					if (LOG.isTraceEnabled())
						LOG.trace("It is a remote NBR alarm");
					remOspfAlarmNbr = subAlarm;
				}
				//ospfAlarmNbr = subAlarm;
			}
			if(eventKey.equals("50003/100/4")) {
				if ( locDevice.equals(subAlarm.getOriginatingManagedEntity().split(" ")[1]) ) {
					if (LOG.isTraceEnabled())
						LOG.trace("It is a local IFR alarm");
					locOspfAlarmIf = subAlarm;
				}
				else {
					if (LOG.isTraceEnabled())
						LOG.trace("It is a remote IFR alarm");
					remOspfAlarmIf = subAlarm;
				}
				//ospfAlarmIf = subAlarm;
			}
		}

		if(locPPortAlarm != null)
			correlated = correlatePPort(locPPortAlarm, ldDeviceAlarm, ldSubInterfaceAlarm, 
					locLdpAlarm, remLdpAlarm, locOspfAlarmNbr, locOspfAlarmIf);

		if(ldDeviceAlarm != null && !correlated) 
			correlated = correlateLagLD(ldDeviceAlarm, locLdpAlarm, remLdpAlarm,
					ldSubInterfaceAlarm, locOspfAlarmNbr, locOspfAlarmIf);

		if(locLdpAlarm != null && !correlated) 
			//correlated = correlateLdp(ldpAlarm, ospfAlarmNbr, ospfAlarmIf, ldSubInterfaceAlarm);
			correlated = correlateLdp(locLdpAlarm, locOspfAlarmNbr, locOspfAlarmIf, ldSubInterfaceAlarm);

		if(remLdpAlarm != null && !correlated) 
			correlated = correlateLdp(remLdpAlarm, locOspfAlarmNbr, locOspfAlarmIf, ldSubInterfaceAlarm);

		//		if(ospfAlarmNbr != null && !correlated && ospfAlarmNbr.equals(group.getTrigger()))
		//if(ospfAlarmNbr != null && !correlated)
		if(locOspfAlarmNbr != null && !correlated)
			correlated = correlateNbr(ldSubInterfaceAlarm, locOspfAlarmNbr, remOspfAlarmNbr, locOspfAlarmIf, remOspfAlarmIf);

		//		if(ospfAlarmIf != null && !correlated && ospfAlarmIf.equals(group.getTrigger()))
		//if(ospfAlarmIf != null && !correlated)
		if(locOspfAlarmIf != null && !correlated)	
			correlated = correlateIf(locOspfAlarmIf, remOspfAlarmIf, ldSubInterfaceAlarm, remOspfAlarmNbr);

		if(remPPortAlarm != null)
			correlated = correlateRemPPort(remPPortAlarm, ldSubInterfaceAlarm, locOspfAlarmNbr, locOspfAlarmIf);

		if (LOG.isTraceEnabled()) {
			LogHelper.exit(LOG, "processOspfProblem() [" + correlated + "]");
		}

		return correlated;
	}		


	private static boolean correlateNbr(Pri_Sec_Alarm ldSubInterfaceAlarm, Pri_Sec_Alarm locOspfAlarmNbr, Pri_Sec_Alarm remOspfAlarmNbr,
			Pri_Sec_Alarm locOspfAlarmIf, Pri_Sec_Alarm remOspfAlarmIf) {

		//correlated = correlateNbr(ldSubInterfaceAlarm, locOspfAlarmNbr, remOspfAlarmNbr, locOspfAlarmIf, remOspfAlarmIf, group);;

		if (LOG.isTraceEnabled()) {
			LogHelper.enter(LOG, "correlateNbr()", locOspfAlarmNbr.getIdentifier() );
		}

		boolean ret = false;

		// the If alarm is the primary in this case
		if(locOspfAlarmIf != null) {
			if(locOspfAlarmIf.getCustomFieldValue("lag_id") != null && !locOspfAlarmIf.getCustomFieldValue("lag_id").isEmpty()) {

				// correlate with neighbor alarm
				if (LOG.isTraceEnabled())
					LOG.trace("nbr lag is: " + locOspfAlarmNbr.getCustomFieldValue("lag_id") + " -- IF lag is: " + 
							locOspfAlarmIf.getCustomFieldValue("lag_id"));

				if(locOspfAlarmNbr.getCustomFieldValue("lag_id") != null &&
						locOspfAlarmIf.getCustomFieldValue("lag_id").equals(locOspfAlarmNbr.getCustomFieldValue("lag_id"))) {

					setPriSec(locOspfAlarmIf, locOspfAlarmNbr); 
					ret = true;
				}

				if(ldSubInterfaceAlarm != null) {
					if (LOG.isTraceEnabled())
						LOG.trace("sub lag is: " + ldSubInterfaceAlarm.getlagIdFromAlarm() + " -- IF lag is: " + 
								locOspfAlarmIf.getCustomFieldValue("lag_id"));

					// correlate with sub interface
					if(ldSubInterfaceAlarm.getlagIdFromAlarm() != null && 
							locOspfAlarmIf.getCustomFieldValue("lag_id").equals(ldSubInterfaceAlarm.getlagIdFromAlarm())) {

						setPriSec(locOspfAlarmIf, ldSubInterfaceAlarm);
						ret = true;

					}
				}
			}
		} 
		if (ldSubInterfaceAlarm != null ) {
			if (LOG.isTraceEnabled())
				LOG.trace("sub lag is: " + ldSubInterfaceAlarm.getlagIdFromAlarm() + " -- Nbr lag is: " + 
						locOspfAlarmNbr.getCustomFieldValue("lag_id"));

			// correlate with sub interface
			if(ldSubInterfaceAlarm.getlagIdFromAlarm() != null && 
					locOspfAlarmNbr.getCustomFieldValue("lag_id").equals(ldSubInterfaceAlarm.getlagIdFromAlarm())) {

				setPriSec(locOspfAlarmNbr, ldSubInterfaceAlarm);
				ret = true;

			}			
		}

		if ( remOspfAlarmIf != null ) {
			setPriSec(locOspfAlarmNbr, remOspfAlarmIf);
		}

		if ( remOspfAlarmNbr != null ) {
			setPriSec(locOspfAlarmNbr, remOspfAlarmNbr);
		}

		/*			for (Alarm alarm : group.getAlarmList()) {
				Pri_Sec_Alarm subAlarm = (Pri_Sec_Alarm) alarm;
				if ( subAlarm != ospfAlarmNbr){
					if(subAlarm.getlagIdFromAlarm() != null && ospfAlarmNbr.getCustomFieldValue("lag_id") != null &&
							subAlarm.getCustomFieldValue("lag_id").equals(ospfAlarmNbr.getCustomFieldValue("lag_id"))) {
						if ( Long.valueOf(subAlarm.getCustomFieldValue(GFPFields.FE_TIME_STAMP)) <= 
								Long.valueOf(ospfAlarmNbr.getCustomFieldValue(GFPFields.FE_TIME_STAMP)) ) {
							setPriSec(subAlarm, ospfAlarmNbr);
						}
						else {
							setPriSec(ospfAlarmNbr, subAlarm);
						}
						ret = true;
					}
				}
			}*/	


		if (LOG.isTraceEnabled()) {
			LogHelper.exit(LOG, "correlateNbr() {correlated=" + ret + "}");
		}


		return ret;

	}

	private static boolean correlateIf(Pri_Sec_Alarm locOspfAlarmIf, Pri_Sec_Alarm remOspfAlarmIf, Pri_Sec_Alarm ldSubInterfaceAlarm, Pri_Sec_Alarm remOspfAlarmNbr) {
		boolean ret = false;

		//correlateIf(locOspfAlarmIf, remOspfAlarmIf, ldSubInterfaceAlarm, remOspfAlarmNbr, group);

		if (LOG.isTraceEnabled()) {
			LogHelper.enter(LOG, "correlateIf()", locOspfAlarmIf.getIdentifier() );
		}

		// correlate with sub interface
		if(ldSubInterfaceAlarm != null) {
			if(ldSubInterfaceAlarm.getlagIdFromAlarm() != null && locOspfAlarmIf.getCustomFieldValue("lag_id") != null &&
					locOspfAlarmIf.getCustomFieldValue("lag_id").equals(ldSubInterfaceAlarm.getlagIdFromAlarm())) {

				setPriSec(locOspfAlarmIf, ldSubInterfaceAlarm);
				ret = true;
			}
		}

		if ( remOspfAlarmIf != null ) {
			setPriSec(locOspfAlarmIf, remOspfAlarmIf);
		}

		if ( remOspfAlarmNbr != null ) {
			setPriSec(locOspfAlarmIf, remOspfAlarmNbr);
		}

		/*		else {
			for (Alarm alarm : group.getAlarmList()) {
				Pri_Sec_Alarm subAlarm = (Pri_Sec_Alarm) alarm;
				if ( subAlarm != ospfAlarmIf){
					if(subAlarm.getlagIdFromAlarm() != null && ospfAlarmIf.getCustomFieldValue("lag_id") != null &&
							subAlarm.getCustomFieldValue("lag_id").equals(ospfAlarmIf.getCustomFieldValue("lag_id"))) {
						if ( Long.valueOf(subAlarm.getCustomFieldValue(GFPFields.FE_TIME_STAMP)) <= 
								Long.valueOf(ospfAlarmIf.getCustomFieldValue(GFPFields.FE_TIME_STAMP)) ) {
							setPriSec(subAlarm, ospfAlarmIf);
						}
						else {
							setPriSec(ospfAlarmIf, subAlarm);
						}
						ret = true;
					}
				}
			}	
		}*/

		if (LOG.isTraceEnabled()) {
			LogHelper.exit(LOG, "correlateIf() {correlated=" + ret + "}");
		}

		return ret;

	}

	// group, loclocPPortAlarm, ldDeviceAlarm, ldSubInterfaceAlarm, 
	// ldpAlarm, ospfAlarmNbr, ospfAlarmIf
	private static boolean correlatePPort(Pri_Sec_Alarm locPPortAlarm, Pri_Sec_Alarm ldDeviceAlarm,
			Pri_Sec_Alarm ldSubInterfaceAlarm, Pri_Sec_Alarm locLdpAlarm, Pri_Sec_Alarm remLdpAlarm,Pri_Sec_Alarm ospfAlarmNbr,
			Pri_Sec_Alarm ospfAlarmIf) {

		if (LOG.isTraceEnabled()) {
			LogHelper.enter(LOG, "correlatePPort()", locPPortAlarm.getIdentifier() );
		}

		boolean ret = false;
		String ppLag = null;

		// OsfNbr
		if(ospfAlarmNbr != null) {
			if (LOG.isTraceEnabled())
				LOG.trace("Attempting NBR alarm");
			// then we compare the neighbor ip with the pport
			if(ospfAlarmNbr.getCustomFieldValue("lag_port_key").equals(locPPortAlarm.getOriginatingManagedEntity().split(" ")[1])) {
				setPriSec(locPPortAlarm, ospfAlarmNbr); 
				ret = true;
			}
		}

		// lag LD
		if(ldDeviceAlarm != null ) {

		}

		// lag subInterface down
		if(ldSubInterfaceAlarm != null) {
			if(locPPortAlarm.getPortLagId() != null && !locPPortAlarm.getPortLagId().isEmpty()) 
				ppLag = locPPortAlarm.getPortLagId();
			if(ldSubInterfaceAlarm.getlagIdFromAlarm() != null) {
				if (LOG.isTraceEnabled())
					LOG.trace("Attempting SubInterface alarm");
				if(ldSubInterfaceAlarm.getlagIdFromAlarm().equals(ppLag)) {
					setPriSec(locPPortAlarm, ldSubInterfaceAlarm); 
					ret = true;
				}	
			}			

		}

		// local ldp 
		if(locLdpAlarm != null) {
			// for both alarms the reason code is the ifIndex	
			String ldpIfIndex = locLdpAlarm.getCustomFieldValue("reason_code");
			String reasonCode = locPPortAlarm.getCustomFieldValue("reason_code");
			if (LOG.isTraceEnabled())
				LOG.trace("Attempting Local LDP alarm");
			if (ldpIfIndex.equals(reasonCode)) {							
				setPriSec(locPPortAlarm, locLdpAlarm);
				ret = true;
			}					
		}

		// remote ldp 
		if(remLdpAlarm != null) {
			// for both alarms the reason code is the ifIndex	
			String ldpIfIndex = remLdpAlarm.getCustomFieldValue("reason_code");
			String reasonCode = locPPortAlarm.getCustomFieldValue("reason_code");
			if (LOG.isTraceEnabled())
				LOG.trace("Attempting Remote LDP alarm");
			if (ldpIfIndex.equals(reasonCode)) {							
				setPriSec(locPPortAlarm, remLdpAlarm);
				ret = true;
			}					
		}


		if(ospfAlarmIf != null) {
			// then we compare the local ip with the pport
			if (LOG.isTraceEnabled())
				LOG.trace("Attempting ifIndex alarm");
			LOG.trace(ospfAlarmIf.getIdentifier() + " lag_port_key=" + ospfAlarmIf.getCustomFieldValue("lag_port_key") + ".");
			LOG.trace(locPPortAlarm.getIdentifier() + " OriginatingManagedEntity=" + locPPortAlarm.getOriginatingManagedEntity().split(" ")[1] + ".");


			if(ospfAlarmIf.getCustomFieldValue("lag_port_key").equals(locPPortAlarm.getOriginatingManagedEntity().split(" ")[1])) {
				setPriSec(locPPortAlarm, ospfAlarmIf); 
				ret = true;
			}							
		}
		if (LOG.isTraceEnabled()) {
			LogHelper.exit(LOG, "correlatePPort() {correlated=" + ret + "}");
		}


		return ret;
	}

	private static boolean correlateRemPPort(Pri_Sec_Alarm remPPortAlarm, Pri_Sec_Alarm ldSubInterfaceAlarm, 
			Pri_Sec_Alarm locOspfAlarmNbr, Pri_Sec_Alarm locOspfAlarmIf) {

		if (LOG.isTraceEnabled()) {
			LogHelper.enter(LOG, "correlateRemPPort()", remPPortAlarm.getIdentifier() );
		}

		boolean ret = false;
		String ppLag = null;

		// lag subInterface down
		if(ldSubInterfaceAlarm != null) {
			/*			if(remPPortAlarm.getPortLagId() != null && !remPPortAlarm.getPortLagId().isEmpty()) 
				ppLag = remPPortAlarm.getPortLagId();*/
			if(ldSubInterfaceAlarm.getRemotePePportInstanceName().equals(remPPortAlarm.getOriginatingManagedEntity().split(" ")[1]) ) {
				setPriSec(remPPortAlarm, ldSubInterfaceAlarm); 
				ret = true;	
			}			

		}

		// osfNbr
		if(locOspfAlarmNbr != null) {
			if(locOspfAlarmNbr.getRemotePePportInstanceName().equals(remPPortAlarm.getOriginatingManagedEntity().split(" ")[1])) {
				setPriSec(remPPortAlarm, locOspfAlarmNbr); 
				ret = true;
			}				
		}

		if(locOspfAlarmIf != null) {
			// then we compare the local ip with the pport
			if(locOspfAlarmIf.getRemotePePportInstanceName().equals(remPPortAlarm.getOriginatingManagedEntity().split(" ")[1])) {
				setPriSec(remPPortAlarm, locOspfAlarmIf); 
				ret = true;
			}							
		}
		if (LOG.isTraceEnabled()) {
			LogHelper.exit(LOG, "correlatePPort() {correlated=" + ret + "}");
		}


		return ret;
	}


	private static boolean correlateLagLD(Pri_Sec_Alarm ldDeviceAlarm, Pri_Sec_Alarm locLdpAlarm, Pri_Sec_Alarm remLdpAlarm,
			Pri_Sec_Alarm ldSubInterfaceAlarm, Pri_Sec_Alarm ospfAlarmNbr,
			Pri_Sec_Alarm ospfAlarmIf) {

		if (LOG.isTraceEnabled()) {
			LogHelper.enter(LOG, "correlateLagLD()", ldDeviceAlarm.getIdentifier() );
		}

		boolean ret = false;

		// Local ldp 
		if(locLdpAlarm != null) {
			if (LOG.isTraceEnabled())
				LOG.trace("Attempting Local LDP alarm");

			String ldpIfIndex = locLdpAlarm.getCustomFieldValue("reason_code");
			if (ldDeviceAlarm.getIfIndex() != null) {				
				if (LOG.isTraceEnabled())
					LOG.trace("Correlating the lag index [" + ldpIfIndex + "] with the LDP index: [" +  ldpIfIndex + "]");
				if (ldpIfIndex.equals(ldDeviceAlarm.getIfIndex())) {							
					setPriSec(ldDeviceAlarm, locLdpAlarm);
					ret = true;
				}					
			}
		}

		// Remote ldp 
		if(remLdpAlarm != null) {
			if (LOG.isTraceEnabled())
				LOG.trace("Attempting Remote LDP alarm");

			String ldpIfIndex = remLdpAlarm.getCustomFieldValue("reason_code");
			if (ldDeviceAlarm.getIfIndex() != null) {				
				if (LOG.isTraceEnabled())
					LOG.trace("Correlating the lag index [" + ldpIfIndex + "] with the LDP index: [" +  ldpIfIndex + "]");
				if (ldpIfIndex.equals(ldDeviceAlarm.getIfIndex())) {							
					setPriSec(ldDeviceAlarm, remLdpAlarm);
					ret = true;
				}					
			}
		}
		// lag subInterface
		if(ldSubInterfaceAlarm != null) {
			if (LOG.isTraceEnabled())
				LOG.trace("Attempting sub Interface alarm:  Id from LAG=" + ldDeviceAlarm.getlagIdFromAlarm()+
						"   Id from sub=" + ldSubInterfaceAlarm.getlagIdFromAlarm());

			// correlated with a device lag alarm
			if(ldDeviceAlarm.getlagIdFromAlarm() != null) {
				String devLag = ldDeviceAlarm.getlagIdFromAlarm();

				if(ldSubInterfaceAlarm.getlagIdFromAlarm() != null) {
					if(ldSubInterfaceAlarm.getlagIdFromAlarm().equals(devLag)) {
						setPriSec(ldDeviceAlarm, ldSubInterfaceAlarm); 
						ret = true;
					}					
				}	
			}
		}

		if(ospfAlarmNbr != null) {
			if (LOG.isTraceEnabled())
				LOG.trace("Attempting NBR alarm");

			if(ospfAlarmNbr.getCustomFieldValue("lag_id").equals(ldDeviceAlarm.getlagIdFromAlarm())) {
				setPriSec(ldDeviceAlarm, ospfAlarmNbr); 
				ret = true;
			}
		}

		if(ospfAlarmIf != null && ospfAlarmIf.getCustomFieldValue("lag_id") != null) {
			if (LOG.isTraceEnabled())
				LOG.trace("Attempting ifIndex alarm");

			if(ospfAlarmIf.getCustomFieldValue("lag_id").equals(ldDeviceAlarm.getlagIdFromAlarm())) {
				setPriSec(ldDeviceAlarm, ospfAlarmIf); 
				ret = true;
			}
		}



		if (LOG.isTraceEnabled()) {
			LogHelper.exit(LOG, "correlateLagLD() {correlated=" + ret + "}");
		}

		return ret;
	}

	//group, ldpAlarm, ospfAlarmNbr, ospfAlarmIf, ldpAlarm)
	private static boolean correlateLdp(Pri_Sec_Alarm ldpAlarm, Pri_Sec_Alarm ospfAlarmNbr,
			Pri_Sec_Alarm ospfAlarmIf, Pri_Sec_Alarm ldSubInterfaceAlarm) {

		if (LOG.isTraceEnabled()) {
			LogHelper.enter(LOG, "correlateLdp()", ldpAlarm.getIdentifier() );
		}

		boolean ret = false;
		String tIfindex = null;
		String ldpindex = null;

		if(ldSubInterfaceAlarm != null) {
			if (LOG.isTraceEnabled())
				LOG.trace("Attempting SubInterface alarm");

			// get the index from the trigger (subinterface alarm)
			String rc = ldSubInterfaceAlarm.getCustomFieldValue(GFPFields.REASON_CODE);

			if(rc.contains("_"))
				tIfindex = rc.split("_")[1];

			// get the ifindex from the LDP alarm
			rc = ldpAlarm.getCustomFieldValue(GFPFields.REASON_CODE);
			if(rc.contains("_"))
				ldpindex = rc.split("_")[1];
			else		// gfpc140537
				ldpindex = rc;

			if(tIfindex.equals(ldpindex)) {
				setPriSec(ldpAlarm, ldSubInterfaceAlarm); 
				ret = true;
			}	
		}

		if(ospfAlarmNbr != null && ldSubInterfaceAlarm != null && ldSubInterfaceAlarm.getCustomFieldValue("lag_id") != null &&
				ospfAlarmNbr.getCustomFieldValue("lag_id") != null) {
			if (LOG.isTraceEnabled())
				LOG.trace("Attempting NBR alarm");
			if(ldSubInterfaceAlarm.getCustomFieldValue("lag_id").equals(ospfAlarmNbr.getCustomFieldValue("lag_id")) 
					&& tIfindex != null ) {
				if( tIfindex.equals(ldpindex))
					setPriSec(ldpAlarm, ospfAlarmNbr);
			}
		}	

		if(ospfAlarmIf != null && ldSubInterfaceAlarm != null && ldSubInterfaceAlarm.getCustomFieldValue("lag_id") != null &&
				ospfAlarmIf.getCustomFieldValue("lag_id") != null) {
			if (LOG.isTraceEnabled())
				LOG.trace("Attempting ifIndex alarm");
			if(ldSubInterfaceAlarm.getCustomFieldValue("lag_id").equals(ospfAlarmIf.getCustomFieldValue("lag_id")) 
					&& tIfindex != null ) {
				if( tIfindex.equals(ldpindex))
					setPriSec(ldpAlarm, ospfAlarmIf);
			}
		}	

		if (LOG.isTraceEnabled()) {
			LogHelper.exit(LOG, "correlateLdp() {correlated=" + ret + "}");
		}

		return ret;
	}

	// processes the subalarm
	public static void setPriSec(Pri_Sec_Alarm primary, Pri_Sec_Alarm secondary) {

		// mark and send the secondary and primary

		LOG.info("Setting " + secondary.getIdentifier() + " as secondary to alert " + primary.getIdentifier() + 
				" - Secondary sequence-number = " + secondary.getCustomFieldValue(GFPFields.SEQNUMBER) + ")");

		secondary.setCustomFieldValue(GFPFields.SECONDARY_ALERT_ID, primary.getCustomFieldValue(GFPFields.ALERT_ID));
		secondary.setCustomFieldValue(GFPFields.SECONDARYTIMESTAMP, primary.getCustomFieldValue(GFPFields.BE_ALARM_TIME_STAMP));

		LOG.info("Setting " + primary.getIdentifier() + " as primary to " + secondary.getIdentifier() + 
				" - Primary sequence-number = " + primary.getCustomFieldValue(GFPFields.SEQNUMBER) + ")");

		primary.setCustomFieldValue("HasSecondary", "true");
		// df gfpc140343 
		primary.setCustomFieldValue(GFPFields.SECONDARY_ALERT_ID, "1");
		primary.setCustomFieldValue(GFPFields.SECONDARYTIMESTAMP, "");
		primary.setCustomFieldValue("sent_to_ruby", "true");

		Util.WhereToSendAndSend((EnrichedAlarm) primary);
		Util.WhereToSendAndSend((EnrichedAlarm) secondary);

		// no need to hold the secondary alarms anymore

		/*		if (primary.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50003/100/1") && 
			primary.getOriginatingManagedEntity().split(" ")[0].equals("PPORT")  ) {

	        long holdTime = 180000; 
	        String watchdogDesc = "Watchdog for:" + globalGroup.getName();
	        Util.setGroupWatch(globalGroup, holdTime, watchdogDesc);
		}
		else {
			Util.WhereToSendAndSend((EnrichedAlarm) secondary);
		}*/


	}


}
