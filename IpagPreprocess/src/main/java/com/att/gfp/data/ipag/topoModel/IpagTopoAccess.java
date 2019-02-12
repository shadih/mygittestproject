package com.att.gfp.data.ipag.topoModel;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagPreprocess.preprocess.CDCAlarm;
import com.att.gfp.data.ipagPreprocess.preprocess.EnrichedPreprocessAlarm;
import com.att.gfp.data.util.CDCFields; 
import com.att.gfp.data.util.NetcoolFields;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
import com.hp.uca.expert.topology.TopoAccess;

/* topology access for ipag */
public class IpagTopoAccess extends TopoAccess 
{
	private static final String CE = "CE";
	private static final String PE = "PE";
	public static final String CE_PPORT_INDEX = "CE_PPort";
	public static final String PE_PPORT_INDEX = "PE_PPort";
	public static final String CE_LPORT_INDEX = "CE_LPort";
	public static final String PE_LPORT_INDEX = "PE_LPort";
	public static final String CE_CARD_INDEX = "CE_Card";
	public static final String PE_CARD_INDEX = "PE_Card";
	public static final String CE_SLOT_INDEX = "CE_Slot"; 
	public static final String PE_SLOT_INDEX = "PE_Slot";

	public static final String KEY_UNIQUEID = "key";
	public static final String PE_DEVICE_INDEX = "PE_Device";
	public static final String CE_DEVICE_INDEX = "CE_Device";
	public static final String DEVICE_UNIQUEID = "key";
	private static final String MPLSLDP = "50003/100/21";
	private static final String LINKDOWN_KEY = "50003/100/1";

	private static final Logger log = LoggerFactory.getLogger(IpagTopoAccess.class);;
	private static GraphDatabaseService override = null;
	private static IpagTopoAccess topologyAccessor = null;  
	private static ExecutionEngine engine= GFPUtil.getCypherEngine(); 


	public static synchronized IpagTopoAccess getInstance() {
		if (topologyAccessor==null) {
			topologyAccessor =  new IpagTopoAccess(); 
		}
		if(engine == null)
			engine = GFPUtil.getCypherEngine(); 
		return topologyAccessor;
	}   

	//	private static GraphDatabaseService getDb()
	//	{
	//		if ( override != null ) return override;
	//		return TopoAccess.getGraphDB ();
	//	}
	//
	//	public static void testAccessSetDb ( GraphDatabaseService db )
	//	{
	//		override = db;
	//	}


	public void fetchDeviceLevelInformation(String instance, EnrichedPreprocessAlarm alarm, 
			Boolean isTargetAtDeviceLevel, String nodeType) {

		if (log.isTraceEnabled()) {
			log.trace("FetchDeviceLevelInformation() Enter : ");
		}
		String query = null;

		query = "START device=node:" + nodeType + "_Device(key = \""
				+ instance
				+ "\")" 
				+
				"RETURN device";  

		if (log.isTraceEnabled()) {
			log.trace("##### CYPHER QUERY: fetchDeviceLevelInformation " + query);
		}

		ExecutionResult result = engine.execute(query);  

		int count=0;
		Node node = null;
		Iterator<Node> columnRows = result.columnAs("device");
		while (columnRows.hasNext()) {
			node = columnRows.next();

			alarm.setDeviceType((String)node.getProperty("device_type")); 
			alarm.setDeviceModel((String)node.getProperty("device_model")); 
			alarm.setDeviceName((String)node.getProperty("device_name"));
			if(node.hasProperty("gcp_device_type")) {
				alarm.setGcpDeviceType((String) node.getProperty("gcp_device_type"));  
			} 
			if(node.hasProperty("legacy_org_ind")) {
				alarm.setLegacyOrgInd((String) node.getProperty("legacy_org_ind"));  
			} 
			if(node.hasProperty("device_sub_role")) {
				alarm.setDeviceSubRole((String) node.getProperty("device_sub_role"));  
			} 
			if(node.hasProperty("device_role")) {
				alarm.setDeviceRole((String) node.getProperty("device_role"));  
			} 
			if(node.hasProperty("ptnii")) {
				alarm.setPtnii((String) node.getProperty("ptnii"));  
			} 
			if(node.hasProperty("nghbr_inmaint")) {
				if ( ((String)node.getProperty("nghbr_inmaint")).equals("true") && alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(MPLSLDP)) {
					alarm.setSuppressed(true);
					if(log.isTraceEnabled()) {
						log.trace("Suppressing alarm: " + alarm.getIdentifier() + " with SeqNum: " + alarm.getCustomFieldValue(GFPFields.SEQNUMBER) +
								" since Device has nghbr_inmaint = true.");
					}
				}
			}				

			if(node.hasProperty("provision_status")) {
				alarm.setProvStatus((String) node.getProperty("provision_status"));  
			}	
			
			if(node.hasProperty("multitenantind")) {
				String multitenetInd = (String) node.getProperty("multitenantind");
				if("Y".equalsIgnoreCase(multitenetInd)) {
					alarm.setCustomFieldValue(GFPFields.INFO1, "MultiTenantInd=<Y>");  
				}  
			} 

			if(nodeType.equals(CE)) {
				if(alarm.getMultiNni() == null || alarm.getMultiNni().isEmpty()) {	
					alarm.setMultiNni((String)node.getProperty("multi_nni"));
				}
				if(alarm.getMultiUni() == null || alarm.getMultiUni().isEmpty()) {
					alarm.setMultiUni((String)node.getProperty("multi_uni"));
				}
				if(alarm.getInEffect() == null || alarm.getInEffect().isEmpty()) {
					alarm.setInEffect((String)node.getProperty("ineffect"));   
				}

			}

			if(isTargetAtDeviceLevel) {
				if(node.hasProperty("multi_uni")) {
					String multiUni = (String) node.getProperty("multi_uni");
					if("Y".equalsIgnoreCase(multiUni)) { 
						String info2 = alarm.getCustomFieldValue(GFPFields.INFO2);
						if(info2 == null || info2.isEmpty()) {
							alarm.setCustomFieldValue(GFPFields.INFO2, "MultiUNI=<Y>"); 
						} else {
							alarm.setCustomFieldValue(GFPFields.INFO2, info2 + "MultiUNI=<Y>"); 
						} 
					}    
				}   
				//alarm.setCustomFieldValue(NetcoolFields.CLASSIFICATION, (String)node.getProperty("alarm_classification"));
				//alarm.setCustomFieldValue(NetcoolFields.DOMAIN, (String)node.getProperty("alarm_domain"));
				if(node.hasProperty("has_mis")) {  
					if("true".equalsIgnoreCase(((String)node.getProperty("has_mis")))) {
						alarm.setHasMis(true);
					}
				} 
			}    
			if ( alarm.getCustomFieldValue(NetcoolFields.CLASSIFICATION) == null || alarm.getCustomFieldValue(NetcoolFields.CLASSIFICATION).isEmpty() ){
				alarm.setCustomFieldValue(NetcoolFields.CLASSIFICATION, (String)node.getProperty("alarm_classification"));
			}
			if ( alarm.getCustomFieldValue(NetcoolFields.DOMAIN) == null || alarm.getCustomFieldValue(NetcoolFields.DOMAIN).isEmpty() ){
				alarm.setCustomFieldValue(NetcoolFields.DOMAIN, (String)node.getProperty("alarm_domain"));
			}

			if("NFO-MOBILITY".equalsIgnoreCase(alarm.getCustomFieldValue(NetcoolFields.CLASSIFICATION)) || 
					"NFO-MOBILITYUNI".equalsIgnoreCase(alarm.getCustomFieldValue(NetcoolFields.CLASSIFICATION))) {
				String info2 = alarm.getCustomFieldValue(GFPFields.INFO2);
				if(info2 == null ) {
					info2 = "";
				}
				if((info2 != null) && !(info2.contains("MultiUNI"))) {
					if(node.hasProperty("multi_uni")) { 
						String multiUni = (String) node.getProperty("multi_uni");
						if("Y".equalsIgnoreCase(multiUni)) { 
							if(info2 == null || info2.isEmpty()) { 
								alarm.setCustomFieldValue(GFPFields.INFO2, "MultiUNI=<Y>"); 
							} else {
								alarm.setCustomFieldValue(GFPFields.INFO2, info2 + "MultiUNI=<Y>"); 
							} 
						}    
					}   
				}
			}			
			if(isTargetAtDeviceLevel) {
				// if the original alarm target was at the device level then set 
				// to true if we have the device in topology
				alarm.setAlarmTargetExists(true);

				try {
					if(((String)node.getProperty("is_operational")).contains("false")) {
						alarm.setOperational(false);
						alarm.setSuppressed(true);   
					}
				} catch (Exception e) {
					// is_operational is null
					if (log.isTraceEnabled()) {
						log.trace("IsOperational value is Null for : " + alarm.getIdentifier());
					}
				}
			}

			alarm.setDeviceLevelExists(true);

			count++;
		}
		if (log.isTraceEnabled()) {
			log.trace("DEVICE INFO ####count:"+ count + " model:" + alarm.getDeviceModel() + " name:" +
					alarm.getDeviceName() + " type:" +
					alarm.getDeviceType() + " domain:" +
					alarm.getCustomFieldValue("domain") + " classification:" +
					alarm.getCustomFieldValue("classification") );

			log.trace("FetchDeviceLevelInformation() Exit : ");
		}
	}

	public void fetchRemoteDeviceLevelInformation(String instance, EnrichedPreprocessAlarm alarm 
			) {

		if (log.isTraceEnabled()) {
			log.trace("FetchRemoteDeviceLevelInformation() Enter : ");
			log.trace("FetchRemoteDeviceLevelInformation() remote data source " + alarm.getRemoteDataSource());
		}
		String query = null;
		if(NetcoolFields.NTE.equalsIgnoreCase(alarm.getRemoteDataSource())) {
			alarm.setRemoteNodeType("CE");
		} else if(NetcoolFields.EGS.equalsIgnoreCase(alarm.getRemoteDataSource()) ||
				NetcoolFields.IPAG.equalsIgnoreCase(alarm.getRemoteDataSource()) || 
				NetcoolFields.SROUTER.equalsIgnoreCase(alarm.getRemoteDataSource()) || 
				NetcoolFields.OEWSROUTER.equalsIgnoreCase(alarm.getRemoteDataSource()) ||
				NetcoolFields.OEWVPLS.equalsIgnoreCase(alarm.getRemoteDataSource())
				) {
			alarm.setRemoteNodeType("PE");
		} else {
			alarm.setRemoteNodeType("PE"); 
		}

		query = "START device=node:" + alarm.getRemoteNodeType().toUpperCase() +"_Device(key = \""
				+ instance
				+ "\")"   
				+
				"RETURN device.device_type, device.device_model, device.device_name";			 


		if (log.isTraceEnabled()) {
			log.trace("##### CYPHER QUERY: FetchRemoteDeviceLevelInformation " + query);
		}

		ExecutionResult result = engine.execute(query);

		int count=0;
		// TODO: get rid of this for loop
		for ( Map<String, Object> row : result ) {			
			alarm.setRemoteDeviceType((String)row.get("device.device_type")); 
			alarm.setRemoteDeviceModel((String)row.get("device.device_model")); 
			alarm.setRemoteDeviceName((String)row.get("device.device_name"));
			//			break; 
		}

		if (log.isTraceEnabled()) {
			log.trace("REMOTE DEVICE INFO ####count:"+ count + " model:" + alarm.getRemoteDeviceModel() + " name:" +
					alarm.getRemoteDeviceName() + " type:" +
					alarm.getRemoteDeviceType() + " domain:"   
					);  

			log.trace("FetchDeviceLevelInformation() Exit : ");
		}		  
	}



	public void fetchPPortLevelInformation(String pportInstance,
			EnrichedPreprocessAlarm alarm) {
		if (log.isTraceEnabled()) {
			log.trace("FetchPPortLevelInformation() Enter : ");
		}
		// either CE or PE
		String query = null;
		String type = alarm.getNodeType().toUpperCase();  
		query = "START pport=node:" + type + "_PPort(key = \""
				+ pportInstance
				+ "\")" 
				+
				"RETURN pport";
		if (log.isTraceEnabled()) {
			log.trace("##### CYPHER QUERY: " + query);
		}

		ExecutionResult result = engine.execute(query);  

		int count=0;
		Node node = null;
		Iterator<Node> columnRows = result.columnAs("pport");
		while (columnRows.hasNext()) {
			node = columnRows.next();
			alarm.setPortAid((String)node.getProperty("port_aid"));
			alarm.setLocalPortAid((String)node.getProperty("port_aid"));

			alarm.setCustomFieldValue(NetcoolFields.CLASSIFICATION, (String)node.getProperty("alarm_classification"));
			alarm.setCustomFieldValue(NetcoolFields.DOMAIN, (String)node.getProperty("alarm_domain"));
			if(node.hasProperty("clli")) {
				alarm.setCustomFieldValue(NetcoolFields.CLLI, (String)node.getProperty("clli"));
			}  
			if(node.hasProperty("clfi")) {
				alarm.setCustomFieldValue(NetcoolFields.CLFI, (String)node.getProperty("clfi"));
			}
			if(node.hasProperty("clci")) {
				alarm.setCustomFieldValue(NetcoolFields.CLCI, (String)node.getProperty("clci"));
			}
			if(node.hasProperty("aafda_role")) {
				alarm.setAafDaRole((String)node.getProperty("aafda_role"));
			}
			if(node.hasProperty("diverse_circuit_id")) {
				alarm.setDiverseCircuitID((String)node.getProperty("diverse_circuit_id"));
			}
			if(node.hasProperty("bmp_clci")) { 
				alarm.setCustomFieldValue(GFPFields.BMP_CLCI, (String)node.getProperty("bmp_clci")); 
			}
			if(node.hasProperty("ineffect")) {  
				alarm.setInEffect((String)node.getProperty("ineffect"));   
			} 

			if(node.hasProperty("hair_pin_indicator")) {  
				alarm.setHairPinIndicator((String)node.getProperty("hair_pin_indicator"));   
			}  

			if(node.hasProperty("has_mis")) {  
				if("true".equalsIgnoreCase(((String)node.getProperty("has_mis")))) {
					alarm.setHasMis(true);
				}
			} 
			alarm.setDataSource((String)node.getProperty("data_source")); 
			if("EGS".equalsIgnoreCase(alarm.getDataSource())) {
				if(node.hasProperty("related_service_ind")) {  
					if("MIS".equalsIgnoreCase(((String)node.getProperty("related_service_ind")))) {
						alarm.setHasMis(true);
					} 
				} 
			}

			if(node.hasProperty("uni_nni")) {
				alarm.setUninni((String)node.getProperty("uni_nni"));
			}
			if(node.hasProperty("legacy_org_ind")) {
				alarm.setPportLegacyOrgInd((String)node.getProperty("legacy_org_ind")); 
			}
			if(node.hasProperty("remote_pport_key")) {
				alarm.setRemotePportKey((String)node.getProperty("remote_pport_key"));   
			}
			if(node.hasProperty("remote_ptnii")) {
				alarm.setRemotePtnii((String)node.getProperty("remote_ptnii"));   
			}   
			if(node.hasProperty("remote_port_name")) {
				alarm.setCustomFieldValue(GFPFields.REMOTE_PPORT_NAME, (String)node.getProperty("remote_port_name")); 
			}  
			if(node.hasProperty("device_type") &&
					( ((String)node.getProperty("device_type")).equals("NV1") || 
					  ((String)node.getProperty("device_type")).equals("NV2") ||
					  ((String)node.getProperty("device_type")).equals("NV3") )){
				alarm.setRemotePortId((String)node.getProperty("remote_port_aid"));
			}   
			if("ALCATEL".equalsIgnoreCase(alarm.getRemotePportKey())) {
				alarm.setRemoteDeviceName((String)node.getProperty("remote_device_name"));
				alarm.setRemotePortId((String)node.getProperty("remote_port_aid")); 
			}   

			if(node.hasProperty("remote_device_ipaddr")) {
				alarm.setRemoteDeviceIpAddrFromLocalPort((String)node.getProperty("remote_device_ipaddr"));   
			}
			if(node.hasProperty("remote_device_type")) {
				alarm.setRemoteDeviceTypeFromLocalPort((String)node.getProperty("remote_device_type"));    
			}
			if(node.hasProperty("device_type")) {
				alarm.setDeviceType((String)node.getProperty("device_type"));    
			}

			if(node.hasProperty("nghbr_inmaint")) {
				if ( ((String)node.getProperty("nghbr_inmaint")).equals("true") && alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(LINKDOWN_KEY) ) {
					alarm.setSuppressed(true);
					if(log.isTraceEnabled()) {
						log.trace("Suppressing alarm: " + alarm.getIdentifier() + " with SeqNum: " + alarm.getCustomFieldValue(GFPFields.SEQNUMBER) +
								" since PPort has nghbr_inmaint = true.");
					}
				}
			}				
			if(node.hasProperty("roadm_ind")  && alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals(LINKDOWN_KEY) ) {
				String roadmInd = (String)node.getProperty("roadm_ind");
				String info3 = alarm.getCustomFieldValue(GFPFields.INFO3); 
				if(info3 == null || info3.isEmpty()) {
					info3 = "";
				}
				alarm.setCustomFieldValue(NetcoolFields.INFO3, info3 + " ROADMIndicator=<" + roadmInd + ">");
				
			}	
			//alarm.setRemotePortNumber((String)row.get("pport.remote_port_number"));
			//alarm.setCustomFieldValue(NetcoolFields.BMP_CLCI, (String)row.get("bmp_clci")); 
			//alarm.setInEffect((String)row.get("pport.in_effect"));

			// check contents of is_operational  
			if(((String)node.getProperty("is_operational")).contains("false")) {
				alarm.setOperational(false);	
				alarm.setSuppressed(true);   
			}
			
			if(node.hasProperty("mcn")) {
				alarm.setCustomFieldValue(NetcoolFields.MCN, (String)node.getProperty("mcn"));
			}
			
			if(alarm.getNodeType().equals(PE)) {
				if(node.hasProperty("port_lag_id")) {
					alarm.setPortLagId((String)node.getProperty("port_lag_id"));
				}  
				if(node.hasProperty("cdc_subscription_type")) {
					alarm.setCdcSubscriptionType((String)node.getProperty("cdc_subscription_type"));
				} else {
					alarm.setCdcSubscriptionType("");
				}
				if("L2-7450-IPAG".equalsIgnoreCase(alarm.getCdcSubscriptionType()) ||
						"L2-7750-IPAG".equalsIgnoreCase(alarm.getCdcSubscriptionType())	) {
					alarm.setCdcPportClfi(alarm.getCustomFieldValue(GFPFields.CLFI));
					alarm.setRemotePportName(alarm.getCustomFieldValue(GFPFields.REMOTE_PPORT_NAME));
				}
			} else {  
				alarm.setCdcSubscriptionType("");
			} 

			if(alarm.getNodeType().equals(CE)) {
				if(node.hasProperty("nmvlan")) {
					alarm.setNmvlan((String)node.getProperty("nmvlan"));
				}  
				if(node.hasProperty("slavlan")) {
					alarm.setSlavlan((String)node.getProperty("slavlan"));
				}  
				if(node.hasProperty("mobility_ind_uni")) {
					String mobilityIndUni = (String)node.getProperty("mobility_ind_uni");
					String info2 = alarm.getCustomFieldValue(GFPFields.INFO2); 
					if(info2 == null || info2.isEmpty()) {
						info2 = "";
					}
					if((!(info2.contains("MobilityIndUni")))) {
						if("Y".equalsIgnoreCase(mobilityIndUni)) {
							alarm.setCustomFieldValue(NetcoolFields.INFO2, info2 + "MobilityIndUni=<Y>");
						}  
					}    
				}  
				if(node.hasProperty("reservation_details")) {
					String reservDetails = ((String)node.getProperty("reservation_details")).replaceAll(";;", "|");
					if ( null == reservDetails || reservDetails.isEmpty() ) {
						alarm.setSuppressed(true);
						if (log.isTraceEnabled()) {
							log.trace("Suppressed due to null reservation details: " + alarm.getCustomFieldValue(GFPFields.SEQNUMBER));
						}
					}
					else {
						String info3 = alarm.getCustomFieldValue(GFPFields.INFO3); 
						if(info3 == null || info3.isEmpty()) {
							info3 = "";
						}
						alarm.setCustomFieldValue(NetcoolFields.INFO3, info3 + " ReservationDetails=<" + reservDetails + ">");
					}
				}
				if(node.hasProperty("reservation_ineffect_indicator")) {
					String reservInd = ((String)node.getProperty("reservation_ineffect_indicator"));
					if ( null == reservInd || reservInd.isEmpty() ) {
						alarm.setSuppressed(true);
						if (log.isTraceEnabled()) {
							log.trace("Suppressed due to null reservation_ineffect_indicator: " + alarm.getCustomFieldValue(GFPFields.SEQNUMBER));
						}
					}
					else {
						if ( !reservInd.equalsIgnoreCase("YES") ) {
							alarm.setSuppressed(true);
							if (log.isTraceEnabled()) {
								log.trace("Suppressed due to non 'YES'; actual value: " + reservInd +
										" reservation_ineffect_indicator: " + alarm.getCustomFieldValue(GFPFields.SEQNUMBER));
							}
						}
						else {
							if (log.isTraceEnabled()) {
								log.trace("Continue processing alarm with " + reservInd + " reservation_ineffect_indicator: " + 
										alarm.getCustomFieldValue(GFPFields.SEQNUMBER));
							}
						}
					}
				}  
			} 
			// set the original alarm target sub-class instance found
			alarm.setAlarmTargetExists(true);

			count++;

		}

		if (log.isTraceEnabled()) {
			log.trace("####count:"+ count + " PPORT:" + alarm.getPortAid() +
					" domain:" + alarm.getCustomFieldValue("domain") + 
					" classification:" + alarm.getCustomFieldValue("classification") +
					" datasource:" + alarm.getDataSource() +
					" remote port aid:" + alarm.getRemotePortAid() + 
					" device type:" + alarm.getDeviceType() +
					" remote device type:" + alarm.getRemoteDeviceTypeFromLocalPort() + "####");

			log.trace("FetchPPortLevelInformation() Exit : ");
		}				
	}

	public void fetchRemotePPortLevelInformation(
			String managedObjectInstance,EnrichedPreprocessAlarm alarm) {
		if (log.isTraceEnabled()) {
			log.trace("FetchRemotePPortLevelInformation() Enter : ");
			log.trace("FetchRemotePPortLevelInformation() remote port key = " + managedObjectInstance);
		}
		String query = "START pePPort=node:"+ alarm.getNodeType() + "_PPort(key = \""
				+ managedObjectInstance
				+ "\")"   
				+ "MATCH (pePPort)" +  
				"-[:PLink]-(Remote_Port) RETURN Remote_Port";  

		if (log.isTraceEnabled()) {
			log.trace("##### CYPHER QUERY: FetchRemotePPortLevelInformation(changed to query either way CE,PE) " + query); 
		}

		ExecutionResult result = engine.execute(query);

		int count=0;  
		Node node = null;
		Iterator<Node> columnRows = result.columnAs("Remote_Port");
		while (columnRows.hasNext()) {
			node = columnRows.next();
			if (log.isTraceEnabled()) {
				log.trace("FetchRemotePPortLevelInformation() info found");
			}
			alarm.setRemotePportInstanceName((String) node.getProperty("TDL_instance_name")); 
			alarm.setRemotePortId((String) node.getProperty("port_aid")); 
			alarm.setRemoteDeviceIpaddr(alarm.getRemotePportInstanceName().split("/")[0]);  
			alarm.setRemoteDataSource((String) node.getProperty("data_source")); 
			if(node.hasProperty("clfi")) {
				alarm.setRemotePportClfi((String) node.getProperty("clfi"));
			}      
			if(node.hasProperty("diverse_circuit_id")) {
				alarm.setRemotePportDiverseCktId((String) node.getProperty("diverse_circuit_id"));
			}      
			if(node.hasProperty("aafda_role")) {
				alarm.setRemotePportAafdaRole((String) node.getProperty("aafda_role"));
			}       
			if(node.hasProperty("port_num")) {
				alarm.setRemotePportPortNum((String) node.getProperty("port_num"));
			}        
			if(node.hasProperty("has_mis")) {  
				if("true".equalsIgnoreCase(((String)node.getProperty("has_mis")))) {
					alarm.setRemotePPortHasMis(true);
				}
			} 
			alarm.setRemotePPortInfoExists(true);   
			count++; 
		} 
		if (log.isTraceEnabled()) {
			log.trace("FetchRemotePPortLevelInformation REMOTE PPORT INFO ####"+  "RemotePportInstanceName:" + alarm.getRemotePportInstanceName() + " RemotePortId:" +
					alarm.getRemotePortAid() + " RemoteDeviceIpaddr:" +
					alarm.getRemoteDeviceIpaddr() + " RemoteDataSource:"   +
					alarm.getRemoteDataSource() + " "
					);  
		}
		if(alarm.getRemotePportAafdaRole() == null || alarm.getRemotePportAafdaRole().isEmpty()) {
			alarm.setRemotePportAafdaRole("unknown");
		} 

		if(alarm.getRemoteDeviceIpaddr() != null)  
			fetchRemoteDeviceLevelInformation(alarm.getRemoteDeviceIpaddr(), alarm); 
		if (log.isTraceEnabled()) {
			log.trace("FetchPPortLevelInformation() Exit : ");   
		}	
	} 

	public void fetchRemotePePPortLevelInformation(
			String managedObjectInstance,EnrichedPreprocessAlarm alarm) {
		if(log.isInfoEnabled()) {
			log.info("fetchRemotePePPortLevelInformation() Enter : ");
			log.trace("FetchRemotePPortLevelInformation() remote port key = " + managedObjectInstance);
		}
		String query = "START pport=node:PE_PPort(key = \""
				+ managedObjectInstance
				+ "\")" 
				+
				"RETURN pport.TDL_instance_name,pport.clfi,pport.device_type,pport.port_aid,pport.port_num";

		if(log.isInfoEnabled()) {
			log.info("##### CYPHER QUERY: fetchRemotePePPortLevelInformation " + query); 
		}

		ExecutionResult result = engine.execute(query);

		for ( Map<String, Object> row : result ) {			
			alarm.setRemotePePportInstanceName((String)row.get("pport.TDL_instance_name")); 
			alarm.setRemotePePportClfi((String)row.get("pport.clfi")); 
			alarm.setRemotePePportDevieType((String)row.get("pport.device_type"));
			alarm.setRemotePePportPortAid((String)row.get("pport.port_aid")); 
			alarm.setRemotePePportPortNum((String)row.get("pport.port_num"));
			//			break; 
		}
		if(log.isTraceEnabled()) {
			log.trace("fetchRemotePePPortLevelInformation REMOTE PE PPORT INFO ####"+  "RemotePportInstanceName:" + alarm.getRemotePePportInstanceName() + " RemotePortId:" +
					alarm.getRemotePePportPortAid() + " RemoteDeviceType:" +
					alarm.getRemotePePportDevieType() + " RemotePortAid:"   +
					alarm.getRemotePePportPortAid() + " "
					);  
		}

	} 


	public void fetchSubclassLevelInformation(

			String managedObjectInstance, String managedObjectClass, EnrichedPreprocessAlarm alarm) {

		if (log.isTraceEnabled()) {
			log.trace("FetchSubclassLevelInformation() Enter : ");
		}

		String moClass = managedObjectClass.toUpperCase();
		String index = null;
		String query = "";

		// set the index for the various classes
		// TODO: fix this mess
		if(moClass.equals("TUNNEL"))
			index = "Tunnel";
		else
			if(managedObjectClass.equals("LAG"))
				index = moClass;
			else
				if(moClass.equals("EVCNODE"))
					index="EVCNode"; 
				else
					if(moClass.equals("PPORT"))
						index = alarm.getNodeType().toUpperCase() + "_PPort";
					else
						if(moClass.equals("LPORT"))
							index = alarm.getNodeType().toUpperCase() + "_LPort";
						else {
							moClass = managedObjectClass.toLowerCase();
							index = alarm.getNodeType().toUpperCase() + "_" +
									Character.toUpperCase(moClass.charAt(0)) + moClass.substring(1);
						}
		query = "START other=node:" + index + "(key = \""
				+ managedObjectInstance
				+ "\") " 
				+
				"RETURN other";
		if (log.isTraceEnabled()) {
			log.trace("##### CYPHER QUERY: " + query);
		}

		ExecutionResult result = engine.execute(query);  

		int count=0;
		Node node = null;
		Iterator<Node> columnRows = result.columnAs("other");
		alarm.setCdcSubscriptionType("");
		while (columnRows.hasNext()) {
			node = columnRows.next();
			alarm.setCustomFieldValue("classification", (String) node.getProperty("alarm_classification"));
			alarm.setCustomFieldValue("domain", (String) node.getProperty("alarm_domain"));
			if("EVCNode".equalsIgnoreCase(index)) {
				log.trace("##### EVCNode of CYPHER QUERY: " + query);
				if(node.hasProperty("cdc_subscription_type")) {
					alarm.setCdcSubscriptionType((String) node.getProperty("cdc_subscription_type"));  
				}  
				if(node.hasProperty("service_ind")) { 
					if("MIS".equalsIgnoreCase((String) node.getProperty("service_ind"))) {
						alarm.setHasMis(true);
					}
				}
				if(node.hasProperty("evc_name")) { 
					alarm.setCustomFieldValue(CDCFields.CircuitId, (String) node.getProperty("evc_name")); 
					alarm.setEvcName((String) node.getProperty("evc_name")); 
				}
				if(node.hasProperty("data_source")) {  
					alarm.setEvcNodeAlarmSource((String)node.getProperty("data_source"));  
				}
				if(node.hasProperty("acnaban")) { 
					alarm.setEvcNodeAcnaban((String) node.getProperty("acnaban"));   
				}
				if(node.hasProperty("vrf_name")) { 
					alarm.setEvcNodeVrfName((String) node.getProperty("vrf_name")); 
				}

				if(node.hasProperty("unickt")) {
					alarm.setCustomFieldValue(GFPFields.UNICKT, (String) node.getProperty("unickt"));   
				}
			}
			if("PE_Slot".equalsIgnoreCase(index) || "PE_Card".equalsIgnoreCase(index)) {
				if(node.hasProperty("cdc_subscription_type")) {
					alarm.setCdcSubscriptionType((String) node.getProperty("cdc_subscription_type"));  
				} 
				if("L2-7450-IPAG".equalsIgnoreCase(alarm.getCdcSubscriptionType()) ||
						"L2-7750-IPAG".equalsIgnoreCase(alarm.getCdcSubscriptionType()) ) {
					fetchAssociatedClfi(index,managedObjectInstance,alarm);
				}
			}  
			if("PE_LPort".equalsIgnoreCase(index)) {

				if(node.hasProperty("service_name")) { 
					alarm.setLportServiceName((String) node.getProperty("service_name"));
				}
				if(node.hasProperty("mcn")) { 
					alarm.setCustomFieldValue(NetcoolFields.MCN, (String)node.getProperty("mcn"));  
				}
			}			  
			if(moClass.equals("LPORT")) {    
				alarm.setCardType((String)node.getProperty("card_type"));
				alarm.setDs1CktId((String)node.getProperty("ds1_ckt_id"));
				alarm.setEvcName((String)node.getProperty("evc_name"));  
				if(node.hasProperty("clci")) { 
					alarm.setLportClci((String) node.getProperty("clci"));  
				}				
			}
			if(node.hasProperty("has_mis")) {  
				if("true".equalsIgnoreCase(((String)node.getProperty("has_mis")))) {
					alarm.setHasMis(true);
				}
			} 

			if(node.hasProperty("bmp_clci")) { 
				alarm.setCustomFieldValue(GFPFields.BMP_CLCI, (String)node.getProperty("bmp_clci")); 
			}
			// the subclass as targeted in the alarm was found   
			alarm.setAlarmTargetExists(true);

			// check contents of is_operational
			if(((String)node.getProperty("is_operational")).contains("false")) {
				alarm.setOperational(false);
				alarm.setSuppressed(true);
			} 

			count++;
			//			break;
		}

		if (log.isTraceEnabled()) {
			log.trace("####count:"+ count + 
					" domain:" + alarm.getCustomFieldValue("domain") + 
					" classification:" + alarm.getCustomFieldValue("classification") +
					" cdcSubscriptionType :" + alarm.getCdcSubscriptionType()); 
		}		
		if("PE_LPort".equalsIgnoreCase(index)) {

			FetchContainingPPort(managedObjectInstance, alarm);
		} 
		if(moClass.equals("LPORT")) {  
			FetchCorrespondingPPort(managedObjectInstance.substring(0, managedObjectInstance.lastIndexOf("/")), alarm);
		}  

		if (log.isTraceEnabled()) {
			log.trace("FetchSubclassLevelInformation() Exit : ");
		}
	}	
	private void fetchAssociatedClfi(String index, String managedObjectInstance, EnrichedPreprocessAlarm alarm) {
		String query = "";
		if("PE_Card".equalsIgnoreCase(index)) {
			query = "START card=node:" + index + "(key = \""
					+ managedObjectInstance
					+ "\")  MATCH (card)-[:Composed_Of]-(pport)" 
					+ " WHERE pport.class=\"IpagPPort\" AND card.class=\"IpagCard\" AND"
					+ " pport.device_type=\"JUNIPER MX SERIES\""
					+ " RETURN pport.clfi, pport.remote_device_type, pport.remote_port_name";

		}
		if("PE_Slot".equalsIgnoreCase(index)) {
			query = "START slot=node:" + index + "(key = \""
					+ managedObjectInstance
					+ "\")  MATCH (slot)-[:Composed_Of]-(card)-[:Composed_Of]-(pport)" 
					+ " WHERE pport.class=\"IpagPPort\" AND card.class=\"IpagCard\" AND slot.class=\"IpagSlot\" AND"
					+ " pport.device_type=\"JUNIPER MX SERIES\""
					+ " RETURN pport.clfi, pport.remote_device_type, pport.remote_port_name"; 

		}
		ExecutionResult result = engine.execute(query);

		int count=0;
		StringBuilder clfiStr = new StringBuilder();
		StringBuilder remPportNmStr = new StringBuilder();
		// TODO: get rid of this for loop
		for ( Map<String, Object> row : result ) {
			if ( ((String)row.get("pport.remote_device_type")).contains("ALCATEL") ) {
				if(count == 0) {
					clfiStr.append((String)row.get("pport.clfi")); 
					remPportNmStr.append((String)row.get("pport.remote_port_name"));
				} else {
					clfiStr.append("," + (String)row.get("pport.clfi")); 
					remPportNmStr.append("," + (String)row.get("pport.remote_port_name"));
				}
				count++;  
			}
		}
		alarm.setCdcPportClfi(clfiStr.toString()); 
		alarm.setRemotePportName(remPportNmStr.toString());
		alarm.setCustomFieldValue(GFPFields.REMOTE_PPORT_NAME, remPportNmStr.toString());
		
		if (log.isTraceEnabled()) {
			log.trace("Pport clifs " + clfiStr + ", Pport remote_port_names " + remPportNmStr + ", count " + count);
		}
	}

	public boolean searchNUpdateCDCNode(CDCAlarm cdcAlarm){

		if (log.isTraceEnabled()) {
			log.trace("searchNUpdateCDCNode() : Enter : ");
		}
		String query = null;
		boolean isCdcNodeExists = false;
		if (log.isTraceEnabled()) {
			log.trace("searchNUpdateCDCNode() : Extract Fields from Alarm ");
			log.trace("searchNUpdateCDCNode() : Build query to create CDC node in neo4j Graph DB ");
		}			
		// Example create node without relationship - This will not work for us
		// CREATE (n  { class : "NTD" , clfi : "P101/GE10/DTRTMIBL0AW/DTRTMIBL0BW" ,  TicketNumber :"512066" , NWPHostname :"RAMAhost" , PublishFlag : "H" , AlarmTime : "1453248000" }) return n
		// Example2: Create node with NTDTicket as relationship
		// CREATE (n  { class : "NTD" , clfi : "P101/GE10" ,  TicketNumber :"512064" , NWPHostname :"RAMAhost" , PublishFlag : "H" , AlarmTime : "1453248000" })-[r:NTDTicket]->neo return n
		query = "START cdc=node:CDC(subscriptionType=\""+cdcAlarm.getSubscriptionType()+"\") where cdc.fromAppId=\""+cdcAlarm.getFromAppId()+"\" SET " +
				"cdc.sourceIdentifier = \""+cdcAlarm.getSourceIdentifier()+"\", " +
				"cdc.operation = \""+cdcAlarm.getOperation()+"\", " +
				"cdc.subscriptionId = \""+cdcAlarm.getSubscriptionId()+"\", " +
				"cdc.pubEventType = \""+cdcAlarm.getPubEventType()+"\", " +
				"cdc.initialize = \""+cdcAlarm.getInitialize()+"\", " +  
				"cdc.initializeTimeStamp = \""+cdcAlarm.getInitializeTimeStamp()+"\" " +
				"  return cdc";

		if (log.isTraceEnabled()) {
			log.trace("searchNUpdateCDCNode() : Execute query to create CDC Node in neo4j Graph DB ........ " );
			log.trace("searchNUpdateCDCNode() : Cypher Query " +  query );
		}
		ExecutionResult result = engine.execute(query);

		int count=0;
		for ( Map<String, Object> row : result ) {
			log.trace("CDC Node exists in topo SubScription ID = " + (String)row.get("cdc.subscriptionId"));  
			cdcAlarm.setExistsInToplogy(true);
			count++;
			isCdcNodeExists = true;
			break;
		}   

		if (log.isTraceEnabled()) {
			log.trace("####count:"+ count); 
			log.trace("searchNUpdateCDCNode() : Exit : ");
		}
		return isCdcNodeExists;  
	}

	// Method1: CreateNode(Alarm ntdAlarm) will be called from ExtendedLifeCycle.java (when we receive an active alarm)
	public void createCdcNode(CDCAlarm cdcAlarm){

		if (log.isTraceEnabled()) {
			log.trace("createCdcNode() : Enter : ");
		}
		String query = null;

		if (log.isTraceEnabled()) {
			log.trace("createCdcNode() : Extract Fields from Alarm ");
			log.trace("createCdcNode() : Build query to create CDC node in neo4j Graph DB ");
		}			
		// Example create node without relationship - This will not work for us
		// CREATE (n  { class : "NTD" , clfi : "P101/GE10/DTRTMIBL0AW/DTRTMIBL0BW" ,  TicketNumber :"512066" , NWPHostname :"RAMAhost" , PublishFlag : "H" , AlarmTime : "1453248000" }) return n
		// Example2: Create node with NTDTicket as relationship
		// CREATE (n  { class : "NTD" , clfi : "P101/GE10" ,  TicketNumber :"512064" , NWPHostname :"RAMAhost" , PublishFlag : "H" , AlarmTime : "1453248000" })-[r:NTDTicket]->neo return n

		query = "CREATE (n " +
				" { class : \"CDC\" , " +
				"sourceIdentifier : \"" + cdcAlarm.getSourceIdentifier() + "\" , " +
				"operation :\"" + cdcAlarm.getOperation() + "\" , " +
				"subscriptionId :\"" + cdcAlarm.getSubscriptionId()+ "\" , " +
				"subscriptionType : \"" + cdcAlarm.getSubscriptionId() + "\" , " +
				"fromAppId : \"" + cdcAlarm.getFromAppId() + "\" , " +
				"pubEventType : \"" + cdcAlarm.getPubEventType() + "\" , " + 
				"initialize : \"" + cdcAlarm.getInitialize() + "\" , " +
				"initializeTimeStamp : \"" + cdcAlarm.getInitializeTimeStamp()  + "\" }" +
				")" + 
				" return n" ; 

		if (log.isTraceEnabled()) {
			log.trace("createCdcNode() : Execute query to create CDC Node in neo4j Graph DB ........ " );
			log.trace("createCdcNode() : Cypher Query " +  query );
		}
		ExecutionResult result = engine.execute(query);
		if (log.isTraceEnabled()) {
			log.trace("createCdcNode() : Exit : ");
		}
	}

	// Method2: DeleteNode(string TicketNumber) will be called from ExtendedLifeCycle.java (When we receive a clear alarm)
	public void deleteCdcNode(CDCAlarm cdcAlarm) {

		if (log.isTraceEnabled()) {
			log.trace("deleteCdcNode() : Enter : ");
		}
		String query = null;


		// query = "START n=node(*) MATCH n WHERE n.TicketNumber = \'" + TicketNumberValue +  "\' DELETE n"; // This fails when there are nodes that do not have n.TicketNumber (example EVC Nodes, PPort Nodes)
		// query = START n=node(*)  MATCH (n)-[r:NTDTicket]-() WHERE ( HAS(n.class) and n.class="NTD" and HAS(n.TicketNumber) and n.TicketNumber="512064" ) DELETE n, r; // This works when executed manuelly
		query = "START n=node(0) WHERE ( HAS(n.class) and n.class=\"CDC\" and HAS(n.subscriptionType) and n.subscriptionType=\"" + cdcAlarm.getSubscriptionType() + "\" ) DELETE n";


		if (log.isTraceEnabled()) {
			log.trace("deleteCdcNode() : Cypher Query " +  query );
		}

		ExecutionResult result = engine.execute(query);

		// This does not work
		// log.trace("DeleteNode() : Number of nodes deleted are : " + result); 

		if (log.isTraceEnabled()) {
			log.trace("deleteCdcNode() : Exit : ");
		}

	}
	public String FetchContainingPPort(String lportInstance, EnrichedPreprocessAlarm alarm) {
		String pport = "unknown";

		if (log.isTraceEnabled()) {
			log.trace("FetchContainingPPort() Enter : ");
		}

		String query = "START peLPort=node:PE_LPort(key = \""
				+ lportInstance
				+ "\")" 
				+ " MATCH (peLPort)<-[:Composed_Of]-(PE_PPort)-[:PLink]->(remPort)" 
				+ " WHERE PE_PPort.class=\"IpagPPort\"" 
				+ " RETURN PE_PPort.TDL_instance_name, PE_PPort.port_aid,PE_PPort.remote_device_type,remPort, peLPort.scp_service";

		if (log.isTraceEnabled()) {
			log.trace("##### CYPHER QUERY: " + query);
		}

		if(engine == null)
			engine = new ExecutionEngine(getGraphDB());  

		ExecutionResult result = engine.execute(query); 


		// only one should be returned
		for ( Map<String, Object> row : result ) {
			alarm.setContainingPPort((String)row.get("PE_PPort.TDL_instance_name"));
			log.trace("localTDL isnance name = " + (String)row.get("PE_PPort.TDL_instance_name)")); 
			//      alarm.setRemoteDeviceType((String)row.get("remPort.device_type"));
			//      alarm.setCorrespondingPPortRemoteDeviceType((String)row.get("remPort.device_type"));
			//    alarm.setRemotePportAafdaRole((String)row.get("remPort.aafda_role"));   
			alarm.setlPortScpService((String)row.get("peLPort.scp_service")); 
			log.trace("local port AID = " + (String)row.get("PE_PPort.port_aid"));    
			alarm.setPortAid((String)row.get("PE_PPort.port_aid"));  
			alarm.setLocalPortAid((String)row.get("PE_PPort.port_aid"));  
			alarm.setContaiiningPportremDevType((String)row.get("PE_PPort.remote_device_type")); 
			Node node = (Node) row.get("remPort");
			if(node != null) {
				log.trace("remote pport node retreived " + (String) node.getProperty("TDL_instance_name"));  
				if(node.hasProperty("TDL_instance_name")) {
					alarm.setContainingPportRemotePPortInstance((String) node.getProperty("TDL_instance_name")); 
					log.trace("remote pport isnance name = " + (String) node.getProperty("TDL_instance_name"));  
				}
				if(node.hasProperty("device_type")) {  
					alarm.setCorrespondingPPortRemoteDeviceType((String) node.getProperty("device_type"));  
					alarm.setRemoteDeviceType((String) node.getProperty("device_type"));   
				} 
				if(node.hasProperty("aafda_role")) {
					alarm.setRemotePportAafdaRole((String) node.getProperty("aafda_role"));   
				}   
			}

			//               break;
		}

		if (log.isTraceEnabled()) {
			log.trace("#### Containing PPORT:" + alarm.getPortAid() + "####"); 
			log.trace("FetchContainingPPort() Exit : ");
		}
		return pport;
	}

	public String FetchCorrespondingPPort(String lportInstance, EnrichedPreprocessAlarm alarm) {
		String pport = "unknown";

		log.trace("FetchCorrespondingPPort() Enter : ");
		String type = alarm.getNodeType().toUpperCase();  
		String query = "START pePPort=node:"+ type +"_PPort(key = \""
				+ lportInstance
				+ "\")" 
				+ " RETURN pePPort.TDL_instance_name,pePPort.device_type,pePPort.port_aid,pePPort.clfi,pePPort.remote_device_type";

		log.trace("##### CYPHER QUERY: " + query);

		if(engine == null)
			engine = new ExecutionEngine(getGraphDB()); 

		ExecutionResult result = engine.execute(query); 

		// only one should be returned
		for ( Map<String, Object> row : result ) {
			log.trace("local port AID = " + (String)row.get("pePPort.port_aid"));   
			alarm.setPortAid((String)row.get("pePPort.port_aid"));   
			alarm.setLocalPortAid((String)row.get("pePPort.port_aid"));  
			alarm.setContainingPportClfi((String)row.get("pePPort.clfi"));   
			alarm.setContaiiningPportremDevType((String)row.get("pePPort.remote_device_type"));  
			alarm.setDeviceType((String)row.get("pePPort.device_type"));

			//               break;
		}

		if (log.isTraceEnabled()) {
			log.trace("#### Containing PPORT:" + alarm.getPortAid() +
					" Remote Device Type: " + alarm.getContaiiningPportremDevType() +
					" Containing Device Type: " + alarm.getDeviceType() + " ####"); 
		}

		log.trace("FetchCorrespondingPPort() Exit : ");           
		return pport;
	}

	public void findAdditionalCienaClfis(
			EnrichedPreprocessAlarm alarm) {

		if (log.isTraceEnabled()) {
			log.trace("findAdditionalCienaClfis() Enter : ");
		}
		// go from the local device to the local local port using the lagid from the alarm to the remote port
		// traversing all the relations in between.
		String query = "START locDev=node:CE_Device(key = \""
				+ alarm.getOriginatingManagedEntity().split(" ")[1].split("/")[0]
						+ "\") MATCH (locDev)-[:Composed_Of_PPort]->(locPPort)" +
						" WHERE locPPort.has_mis=\"true\" RETURN locPPort.TDL_instance_name,locPPort.clfi";

		if (log.isTraceEnabled()) {
			log.trace("##### CYPHER QUERY: " + query);  
		}

		if(engine == null) 
			engine = new ExecutionEngine(getGraphDB());

		ExecutionResult result = engine.execute(query); 

		// only one should be returned
		StringBuilder sb = new StringBuilder();
		int count = 0;
		for ( Map<String, Object> row : result ) {
			if(((String)row.get("locPPort.clfi")) != null && !(((String)row.get("locPPort.clfi")).isEmpty())) {
				if(count > 0 && !(sb.toString().isEmpty())) {
					sb.append(",");
				}
				sb.append(((String)row.get("locPPort.clfi"))); 
			}
			count++;
		}
		alarm.setAdditionalCLFIInfo(sb.toString()); 
		if (log.isTraceEnabled()) {
			log.trace("#### AdditionalCienaClfis :" + sb.toString() + "####");
			log.trace("findAdditionalCienaClfis() Exit : ");
		}
	}
	public void findAdditionalCienaEmuxClfis(
			EnrichedPreprocessAlarm alarm) {

		if (log.isTraceEnabled()) {
			log.trace("findAdditionalCienaEmuxClfis() Enter : ");
		}
		// go from the local device to the local local port using the lagid from the alarm to the remote port
		// traversing all the relations in between.
		String query = "START locDev=node:CE_Device(key = \""
				+ alarm.getOriginatingManagedEntity().split(" ")[1].split("/")[0]
						+ "\") MATCH (locDev)-[:Composed_Of_PPort]->(locPPort)-[:PLink]-(Remote_Port)" +
						" WHERE (locPPort.has_mis=\"true\" OR Remote_Port.has_mis=\"true\") AND locPPort.remote_device_type <> \"JUNIPER MX SERIES\" RETURN locPPort.TDL_instance_name,locPPort.clfi";

		if (log.isTraceEnabled()) {
			log.trace("##### CYPHER QUERY: " + query);  
		}

		if(engine == null) 
			engine = new ExecutionEngine(getGraphDB());

		ExecutionResult result = engine.execute(query); 

		// only one should be returned
		StringBuilder sb = new StringBuilder();
		int count = 0;
		for ( Map<String, Object> row : result ) {
			alarm.setIssendToCpeCdc(true);
			if(((String)row.get("locPPort.clfi")) != null && !(((String)row.get("locPPort.clfi")).isEmpty())) {
				if(count > 0 && !(sb.toString().isEmpty())) {
					sb.append(",");
				}
				sb.append(((String)row.get("locPPort.clfi"))); 
			}
			count++;
		}
		alarm.setAdditionalCLFIInfo(sb.toString()); 
		if (log.isTraceEnabled()) {
			log.trace("#### AdditionalCienaEmuxClfis :" + sb.toString() + "####");
			log.trace("findAdditionalCienaEmuxClfis() Exit : ");
		}
	}

	public void findAdditionalJuniperCardClfis(
			EnrichedPreprocessAlarm alarm) {

		if (log.isTraceEnabled()) {
			log.trace("findAdditionalJuniperCardClfis() Enter : ");
		}
		// go from the local device to the local local port using the lagid from the alarm to the remote port
		// traversing all the relations in between.
		String query = "";
		if("JUNIPER MX SERIES".equalsIgnoreCase(alarm.getDeviceType())) {
			query = "START locCard=node:PE_Card(key = \""
					+ alarm.getOriginatingManagedEntity().split(" ")[1]
							+ "\") MATCH (locCard)- [:Composed_Of]->(locPPort)-[:Composed_Of]->(locLPort)" +
							" WHERE locLPort.scp_service=\"MIS\" AND locPPort.remote_device_type = \"CIENA NTE\" RETURN locPPort.TDL_instance_name,locPPort.clfi";
		} 
		else if("VR1".equalsIgnoreCase(alarm.getDeviceType())) {
			query = "START locCard=node:PE_Card(key = \""
					+ alarm.getOriginatingManagedEntity().split(" ")[1]
							+ "\") MATCH (locCard)- [:Composed_Of]->(locPPort)-[:Composed_Of]->(locLPort)" +
							" WHERE locLPort.service_name=\"MIS\" RETURN locPPort.TDL_instance_name,locPPort.clci"; 
		} 
		else {
			if (log.isTraceEnabled()) {
				log.trace("findAdditionalJuniperCardClfis() Exit : ");
			}
			return;
		}

		if (log.isTraceEnabled()) {
			log.trace("##### CYPHER QUERY: " + query);
		}

		if(engine == null) 
			engine = new ExecutionEngine(getGraphDB()); 

		ExecutionResult result = engine.execute(query); 

		// only one should be returned
		StringBuilder sb = new StringBuilder();
		int count = 0;
		if("VR1".equalsIgnoreCase(alarm.getDeviceType())) {
			for ( Map<String, Object> row : result ) {
				alarm.setIssendToCpeCdc(true);
				if(((String)row.get("locPPort.clci")) != null && !(((String)row.get("locPPort.clci")).isEmpty())) {
					if(count > 0 && !(sb.toString().isEmpty())) {
						sb.append(",");
					}
					sb.append(((String)row.get("locPPort.clci")));  
				}
				count++;
			}
			alarm.setAdditionalClciInfo(sb.toString()); 
		} else {
			for ( Map<String, Object> row : result ) {
				alarm.setIssendToCpeCdc(true);
				if(((String)row.get("locPPort.clfi")) != null && !(((String)row.get("locPPort.clfi")).isEmpty())) {
					if(count > 0 && !(sb.toString().isEmpty())) {
						sb.append(",");
					}
					sb.append(((String)row.get("locPPort.clfi"))); 
				}
				count++;
			}
			alarm.setAdditionalCLFIInfo(sb.toString()); 
		}
		if (log.isTraceEnabled()) {
			log.trace("#### AdditionalJuniperCardClfis :" + sb.toString() + "####");
			log.trace("findAdditionalJuniperCardClfis() Exit : ");
		}
	}

	public void findAdditionalJuniperSlotClfis(
			EnrichedPreprocessAlarm alarm) {

		if (log.isTraceEnabled()) {
			log.trace("findAdditionalJuniperSlotClfis() Enter : ");
		}
		// go from the local device to the local local port using the lagid from the alarm to the remote port
		// traversing all the relations in between.
		String query = "";
		if("JUNIPER MX SERIES".equalsIgnoreCase(alarm.getDeviceType())) {
			query = "START locSlot=node:PE_Slot(key = \""
					+ alarm.getOriginatingManagedEntity().split(" ")[1]
							+ "\") MATCH (locSlot)-[:Composed_Of]-> (locCard)- [:Composed_Of]->(locPPort)-[:Composed_Of]->(locLPort)" +
							" WHERE locLPort.scp_service=\"MIS\" AND locPPort.remote_device_type = \"CIENA NTE\" RETURN locPPort.TDL_instance_name,locPPort.clfi";
		} 
		else if("VR1".equalsIgnoreCase(alarm.getDeviceType())) {
			query = "START locSlot=node:PE_Slot(key = \""
					+ alarm.getOriginatingManagedEntity().split(" ")[1]
							+ "\") MATCH (locSlot)-[:Composed_Of]-> (locCard)- [:Composed_Of]->(locPPort)-[:Composed_Of]->(locLPort)" +
							" WHERE locLPort.service_name=\"MIS\" RETURN locPPort.TDL_instance_name,locPPort.clci"; 
		}
		else {
			if (log.isTraceEnabled()) {
				log.trace("findAdditionalJuniperSlotClfis() Exit : ");
			}
			return;
		}

		if (log.isTraceEnabled()) {
			log.trace("##### CYPHER QUERY: " + query);
		}  

		if(engine == null) 
			engine = new ExecutionEngine(getGraphDB()); 

		ExecutionResult result = engine.execute(query); 

		// only one should be returned
		StringBuilder sb = new StringBuilder();
		int count = 0;
		if("VR1".equalsIgnoreCase(alarm.getDeviceType())) {
			for ( Map<String, Object> row : result ) {
				alarm.setIssendToCpeCdc(true);
				if(((String)row.get("locPPort.clci")) != null && !(((String)row.get("locPPort.clci")).isEmpty())) {
					if(count > 0 && !(sb.toString().isEmpty())) {
						sb.append(",");
					} 
					sb.append(((String)row.get("locPPort.clci"))); 
				}
				count++;
			}
			alarm.setAdditionalClciInfo(sb.toString()); 
		} else {
			for ( Map<String, Object> row : result ) {
				alarm.setIssendToCpeCdc(true);
				if(((String)row.get("locPPort.clfi")) != null && !(((String)row.get("locPPort.clfi")).isEmpty())) {
					if(count > 0 && !(sb.toString().isEmpty())) {
						sb.append(",");
					}
					sb.append(((String)row.get("locPPort.clfi"))); 
				}
				count++;
			}
			alarm.setAdditionalCLFIInfo(sb.toString());  
		}
		if (log.isTraceEnabled()) {
			log.trace("#### AdditionalJuniperSlotClfis :" + sb.toString() + "####");
			log.trace("findAdditionalJuniperSlotClfis() Exit : ");
		}
	}

	public void findAdditionalJuniperDeviceClfis(
			EnrichedPreprocessAlarm alarm) {

		log.trace("findAdditionalJuniperDeviceClfis() Enter : ");
		// go from the local device to the local local port using the lagid from the alarm to the remote port
		// traversing all the relations in between.
		String query = "";
		if("JUNIPER MX SERIES".equalsIgnoreCase(alarm.getDeviceType())) {
			query = "START locDevice=node:PE_Device(key = \""
					+ alarm.getOriginatingManagedEntity().split(" ")[1]
							+ "\") MATCH (locDevice)-[:Composed_Of]-> (locSlot)-[:Composed_Of]-> (locCard)- [:Composed_Of]->(locPPort)-[:Composed_Of]->(locLPort)" +
							" WHERE locLPort.scp_service=\"MIS\" AND locPPort.remote_device_type = \"CIENA NTE\" RETURN locPPort.TDL_instance_name,locPPort.clfi";
		}   
		else if("VR1".equalsIgnoreCase(alarm.getDeviceType())) {
			query = "START locDevice=node:PE_Device(key = \""
					+ alarm.getOriginatingManagedEntity().split(" ")[1]
							+ "\") MATCH (locDevice)-[:Composed_Of]->(locSlot)-[:Composed_Of]-> (locCard)- [:Composed_Of]->(locPPort)-[:Composed_Of]->(locLPort)" +
							" WHERE locLPort.service_name=\"MIS\" RETURN locPPort.TDL_instance_name,locPPort.clci";
		}
		else {
			if (log.isTraceEnabled()) {
				log.trace("findAdditionalJuniperDeviceClfis() Exit : ");
			}
			return;
		}

		log.trace("##### CYPHER QUERY: " + query);  

		if(engine == null) 
			engine = new ExecutionEngine(getGraphDB()); 

		ExecutionResult result = engine.execute(query); 

		// only one should be returned
		StringBuilder sb = new StringBuilder();
		int count = 0;
		if("VR1".equalsIgnoreCase(alarm.getDeviceType())) {
			for ( Map<String, Object> row : result ) {
				alarm.setIssendToCpeCdc(true);
				if(((String)row.get("locPPort.clci")) != null && !(((String)row.get("locPPort.clci")).isEmpty())) {
					sb.append(((String)row.get("locPPort.clci"))); 
					if(count > 0) {
						sb.append(",");
					}
				}
				count++;
			}
			alarm.setAdditionalClciInfo(sb.toString()); 
		} else {
			for ( Map<String, Object> row : result ) {
				alarm.setIssendToCpeCdc(true);
				if(((String)row.get("locPPort.clfi")) != null && !(((String)row.get("locPPort.clfi")).isEmpty())) {
					sb.append(((String)row.get("locPPort.clfi"))); 
					if(count > 0) {
						sb.append(",");
					}
				}
				count++;
			}
			alarm.setAdditionalCLFIInfo(sb.toString()); 
		}
		if (log.isTraceEnabled()) {
			log.trace("#### AdditionalJuniperDeviceClfis :" + sb.toString() + "####");

			log.trace("findAdditionalJuniperDeviceClfis() Exit : ");
		}
	}


	public void findClciForCienaAlarms(
			EnrichedPreprocessAlarm alarm) {

		if (log.isTraceEnabled()) {
			log.trace("findClciForCienaAlarms() Enter : ");
		}
		// go from the local device to the local local port using the lagid from the alarm to the remote port
		// traversing all the relations in between.
		String query = "START locDev=node:CE_Device(key = \""
				+ alarm.getOriginatingManagedEntity().split(" ")[1].split("/")[0]
						+ "\") MATCH (locDev)-[:Composed_Of_PPort]->(locPPort)-[:Composed_Of]->(evcNode) RETURN evcNode.unickt";

		log.trace("##### CYPHER QUERY: " + query);   

		if(engine == null) 
			engine = new ExecutionEngine(getGraphDB());

		ExecutionResult result = engine.execute(query); 

		// only one should be returned 
		for ( Map<String, Object> row : result ) {
			alarm.setCustomFieldValue(GFPFields.CLCI, (String)row.get("evcNode.unickt"));
			//			break;
		}
		if (log.isTraceEnabled()) {
			log.trace("#### findClciForCienaAlarms :" + alarm.getCustomFieldValue(GFPFields.CLCI) + "####");  
			log.trace("findClciForCienaAlarms() Exit : ");
		}
	}	

	public void getPportSetMatchedPortLagID(
			String managedObjectInstance, String portLagID, EnrichedPreprocessAlarm alarm) {
		if(log.isInfoEnabled()) {
			log.info("getPportSetMatchedPortLagID() Enter : ");
			log.trace("getPportSetMatchedPortLagID() LagId = " + portLagID);
		}

		String query = "START locDev=node:PE_Device(key=\""+managedObjectInstance+"\") match " +
				"(locDev)-[:Composed_Of]->(locSlot)-[:Composed_Of]->(locCard)-[:Composed_Of]->(locPPort) where " +
				"locPPort.port_lag_id=\""+portLagID+"\" return " +
				"locPPort";


		if(log.isInfoEnabled()) {
			log.info("##### CYPHER QUERY: getPportSetMatchedPortLagID " + query); 
		}

		ExecutionResult result = engine.execute(query);

		Node node = null;
		Iterator<Node> columnRows = result.columnAs("locPPort");

		while (columnRows.hasNext()) {
			node = columnRows.next();

			if(node.hasProperty("nghbr_inmaint")) {
				if ( ((String)node.getProperty("nghbr_inmaint")).equals("true") ) {
					alarm.setSuppressed(true);
					if(log.isTraceEnabled()) {
						log.trace("Suppressing alarm: " + alarm.getIdentifier() + " with SeqNum: " + alarm.getCustomFieldValue(GFPFields.SEQNUMBER) +
								" since PPort: " + (String)node.getProperty("key") + " has nghbr_inmaint = true.");
					}
					break;
				}
			}				
			alarm.setDeviceType((String)node.getProperty("device_type"));
			alarm.setRemoteDeviceType((String)node.getProperty("remote_device_type"));
			alarm.setRemoteDeviceModel((String)node.getProperty("remote_device_model"));
			alarm.setRemoteDeviceName((String)node.getProperty("remote_device_name"));
			alarm.setRemoteDeviceIpaddr((String)node.getProperty("remote_device_ipaddr"));
			alarm.setRemotePportInstanceName((String)node.getProperty("remote_pport_key"));
			alarm.setRemotePortId((String)node.getProperty("remote_port_aid"));
			alarm.setPortAid((String)node.getProperty("port_aid"));
		}

		if(log.isTraceEnabled()) {
			log.trace("getPportSetMatchedPortLagID MATCHING LAGID PPORT INFO ####"+  "RemotePportInstanceName:" + alarm.getRemotePportInstanceName() + 
					" RemotePortId:" + alarm.getRemotePortAid() +
					" DeviceType:" + alarm.getDeviceType() +
					" RemoteDeviceType:" + alarm.getRemoteDeviceType() +
					" RemoteDeviceModel:" + alarm.getRemoteDeviceModel() +
					" RemoteDeviceName:" + alarm.getRemoteDeviceName() +
					" RemoteDeviceIpaddr:" + alarm.getRemoteDeviceIpaddr() +
					" PortAid:" + alarm.getPortAid() 
					);  
		}

	} 

	public void updateCDCInfo(String evcnodeInstance, String cdcInfo)
	{
		StringBuilder qbld = new StringBuilder("START evcnode=node:EVCNode(key=\""+evcnodeInstance+"\") return evcnode"); 
		ExecutionResult result = engine.execute(qbld.toString());
		Node nd = null;
		for ( Map<String, Object> row : result )
		{

			nd = (Node) row.get("evcnode");
			log.trace("EVC fetched: " + nd.getProperty("key")); 
			setProp(nd, "cdc_info", cdcInfo);

		}
		//		StringBuilder qbld = new StringBuilder("START evcnode=node:EVCNode(key=\""+evcnodeInstance+"\") SET evcnode.cdc_info = \""+cdcInfo+"\" return evcnode");
		//
		//                log.info("Query ="+ qbld.toString());
		//		ExecutionResult result = engine.execute(qbld.toString());
	}

	private void setProp(Node node, String cdcInfo, String value)
	{
		log.trace("setProp() Enter : ");

		Transaction tx = getGraphDB().beginTx();


		try
		{
			log.trace(" Setting property: " + cdcInfo + " to " + value);
			node.setProperty(cdcInfo, value);
			log.trace(" Set property successful");
			tx.success();
		}
		finally
		{
			log.trace(" Set property finished");
			tx.finish(); 
		} 

		log.trace("setProp() Exit : ");

	}	
	
	public String getPtnii(String instance, String nodeType) {

		String ptnii = "";
		
		if (log.isTraceEnabled()) {
			log.trace("getPtnii() Enter : ");
		}
		String query = null;

		query = "START device=node:" + nodeType + "_Device(key = \""
				+ instance
				+ "\")" 
				+
				"RETURN device.ptnii";  

		if (log.isTraceEnabled()) {
			log.trace("##### CYPHER QUERY: getPtnii " + query);
		}

		ExecutionResult result = engine.execute(query);  

		for ( Map<String, Object> row : result ) {
			ptnii = (String)row.get("device.ptnii");
			if (log.isTraceEnabled()) {
				log.trace("instance: " + instance + "'s ptnii: " + ptnii);
			}
		}
		
		log.trace("getPtnii() Exit : ");
		
		return ptnii;
	}

	public String getPtniiByLB10 (String instance, String nodeType) {

		String ptnii = "";
		
		if (log.isTraceEnabled()) {
			log.trace("getPtniiByLB10() Enter : ");
		}
		String query = null;

		// sample query against loopback10 index
		// start dev=node:PE_Device_loopback10_index(loopback10_ipaddr="12.122.240.186") return dev.ptnii
		
		query = "START device=node:" + nodeType + "_Device_loopback10_index(loopback10_ipaddr = \""
				+ instance
				+ "\")" 
				+
				"RETURN device.ptnii";  

		if (log.isTraceEnabled()) {
			log.trace("##### CYPHER QUERY: getPtniiByLB10 " + query);
		}

		ExecutionResult result = engine.execute(query);  

		for ( Map<String, Object> row : result ) {
			ptnii = (String)row.get("device.ptnii");
			if (log.isTraceEnabled()) {
				log.trace("instance: " + instance + "'s ptnii: " + ptnii);
			}
		}
		
		log.trace("getPtniiByLB10() Exit : ");
		
		return ptnii;
	}
}
