package com.att.gfp.data.ipag.topoModel;

import java.util.Iterator;
import java.util.Map;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.SyslogAlarm;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.topology.TopoAccess;


/* topology access for ipag */
public class JuniperSyslogTopoAccess extends TopoAccess 
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
			
	private static Logger log = LoggerFactory.getLogger(JuniperSyslogTopoAccess.class);

	private static GraphDatabaseService override = null;
	
	private static JuniperSyslogTopoAccess topologyAccessor = null;
	private static ExecutionEngine engine;

	private JuniperSyslogTopoAccess() {
		super();
		engine = GFPUtil.getCypherEngine();
	}

	private static GraphDatabaseService getDb()
	{
		if ( override != null ) return override;
		return TopoAccess.getGraphDB ();
		
	}
	
	public static synchronized JuniperSyslogTopoAccess getInstance() {
		if (topologyAccessor == null) {
			topologyAccessor = new JuniperSyslogTopoAccess();
		}
		return topologyAccessor;
	}

	public static void testAccessSetDb ( GraphDatabaseService db )
	{
		override = db;
	}


	public boolean FetchLocalLPortLevelInformationForLinkDownAlarm(
			String pportInstance,  SyslogAlarm alarm) {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "FetchLocalLPortLevelInformationForLinkDownAlarm()");
		}
		
		//		Below is an example alarm for your scenario.   As you can see my interpretation of the requirement was wrong.   The lines:
		//		1.	reason_code (gevm-detail) is <dev_name>_<nvlan_idtop>_<vlan_id>
		//		2.	Parse out dev_name, nvlan_idtop, and vlan_id
		//parse reasoncode into Dev_name,  cvlan_id_top, and cvlanid
		if (log.isTraceEnabled())
			log.trace("Parsing reason code..");
		String reasonCode = alarm.getCustomFieldValue(GFPFields.REASON_CODE);
		String info [] =reasonCode.split("_");//DLSTX405ME3_13_3097 
		String cvlan_id_top = info[1];
		String cvlanid = info[2];
		
		
		String query="";
		boolean returnQuery = false;
		String LPortTDL_instance_name = null;
		String  LPortInterface_Number = null;
		String LPortCLCI = null;
		String LPortEVC_Name = null;

		String devType = alarm.getDeviceType().toLowerCase();
		if (log.isTraceEnabled())
			log.trace("the device type:--" + devType + "-- ");

		if("ciena".equals(devType) || "ciena nte".equals(devType)
				|| "adtran 800 series".equals(devType)|| "ciena emux".equals(devType))
		{			
			if (log.isTraceEnabled())
				log.trace("Querying Ciena NTE, ADTRAN 800 series or Ciena EMUX ..");
			//fetch alarm from database
			query =  "START device=node:PE_Device(key=\""+pportInstance+"\")"+
					" MATCH (device)-[:Composed_Of]-(PE_PPort)-[:Composed_Of]-(LPort)"
					+" WHERE LPort.class=\"IpagLPort\" and PE_PPort.class=\"IpagPPort\" and LPort.cvlan_id_top  =\""  + cvlan_id_top+ "\" and LPort.cvlanid=\"" +cvlanid+"\""

					+" RETURN LPort.TDL_instance_name, LPort.interface_number, LPort.clci, LPort.evc_name";
		} else {
			if("adtran 500 series".equals(devType)) {
				if (log.isTraceEnabled())
					log.trace("Querying Adtran 500 series ..");

				query = "START device=node:PE_Device(key=\""+pportInstance+"\")"
						+" MATCH (device)-[:Composed_Of]-(PE_Slot)-[:Composed_Of]-(PE_Card)-[:Composed_Of]-(PE_PPort)-[:Composed_Of]-(LPort)"
						+" WHERE LPort.class=\"IpagLPort\" and PE_PPort.class=\"IpagPPort\" and LPort.cvlan_id_top  =\""  + cvlan_id_top+ "\" and LPort.cvlanid=\"" +cvlanid+"\""
						+" RETURN LPort.TDL_instance_name, LPort.interface_number, LPort.clci, LPort.evc_name";
			} else {
				if("juniper mx series".equals(devType) || "vr1".equals(devType)) {
					if (log.isTraceEnabled())
						log.trace("Querying JUNIPER MX Series or VR1 ..");

					query = "START device=node:PE_Device(key=\""+pportInstance+"\")"
							+" MATCH (device)-[:Composed_Of]-(PE_Slot)-[:Composed_Of]-(PE_Card)-[:Composed_Of]-(PE_PPort)-[:Composed_Of]-(LPort)"
							+" WHERE LPort.class=\"IpagLPort\" and PE_PPort.class=\"IpagPPort\" and LPort.cvlan_id_top  =\""  + cvlan_id_top+ "\" and LPort.cvlanid=\"" +cvlanid+"\""
							+" RETURN LPort.TDL_instance_name, LPort.interface_number, LPort.clci, LPort.evc_name";
				} else {
					if("cisco 2800 series".equals(devType) || "cisco 3500 series".equals(devType)
							|| "cisco catalyst 2900 series".equals(devType)|| "cisco 3900 series".equals(devType)
							||  "nm9".equals(devType)|| "hu4".equals(devType)) {

						//PE_Device->PE_PPort - CISCO 2800 SERIES, CISCO 3500 SERIES, CISCO CATALYST 2900 SERIES, CISCO 3900 SERIES, nm9, hu4
						log.trace("Querying CISCO 2800 SERIES, CISCO 3500 SERIES, CISCO CATALYST 2900 SERIES, CISCO 3900 SERIES, nm9, or hu4");

						query = "START device=node:PE_Device(key=\""+pportInstance+"\")"
								+" MATCH (device)-[:Composed_Of]-(PE_PPort)-[:Composed_Of]-(LPort)"
								+" WHERE LPort.class=\"IpagLPort\" and PE_PPort.class=\"IpagPPort\" and LPort.cvlan_id_top  =\""  + cvlan_id_top+ "\" and LPort.cvlanid=\"" +cvlanid+"\""
								+" RETURN LPort.TDL_instance_name, LPort.interface_number, LPort.clci, LPort.evc_name";
					} 
				}
			}
		}

		if (log.isDebugEnabled())
			log.debug("##### CYPHER QUERY: ############ " + query);

		if(engine == null)
			engine = GFPUtil.getCypherEngine();

		if(query != null) {

			ExecutionResult result = engine.execute(query);

			for ( Map<String, Object> row : result ) 
			{
				LPortTDL_instance_name = ((String)row.get("LPort.TDL_instance_name"));
				LPortInterface_Number = ((String)row.get("LPort.interface_number"));
				LPortCLCI=((String)row.get(("LPort.clci")));
				LPortEVC_Name=((String)row.get("LPort.evc_name"));
				// break;
			}

			if(LPortTDL_instance_name != null)
			{
				//1.the reason of M = [the reason of M] INTERFACE IP=<[the interface_ip_addr of LP]> EVCID=<[the evc_name of LP]>
				if(alarm.getCustomFieldValue(GFPFields.REASON)!=null)
				{
					alarm.setCustomFieldValue(GFPFields.REASON, alarm.getCustomFieldValue(GFPFields.REASON) 
							+ " INTERFACE IP="  + LPortInterface_Number+
							" EVCID="+ LPortEVC_Name + " PIVOT=&lt;Y&lt;");
				} 
				else 
				{
					alarm.setCustomFieldValue(GFPFields.REASON, "INTERFACE IP="  + LPortInterface_Number+
							" EVCID="+ LPortEVC_Name + " PIVOT=&lt;Y&lt;");
				}
				
				// set all fields for based in the lport
				alarm.setCustomFieldValue(GFPFields.CLCI, LPortCLCI); 
				alarm.setCustomFieldValue(GFPFields.CIRCUIT_ID,LPortCLCI);
				alarm.setCustomFieldValue(GFPFields.CLASSIFICATION, "PIVOT-CFO");
				alarm.setCustomFieldValue(GFPFields.DOMAIN, "IPAG");

				returnQuery= true;
			}
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "FetchLocalLPortLevelInformationForLinkDownAlarm()");
		}

		return returnQuery;

	}
	
	public void getTunnelInfo(String tunnelName, SyslogAlarm alarm) {
		
		String query = "START tunnel=node:Tunnel(key = \"" + tunnelName + "\") "
				+ "RETURN tunnel.tunnel_tail_ptnii, tunnel.tunnel_tail_dns";

		if (log.isDebugEnabled())
			log.debug("CYPHER QUERY: " + query);
		
		if(engine == null)
			engine = GFPUtil.getCypherEngine();

		ExecutionResult result = engine.execute(query);
		String tailRtr = null;
		for (Map<String, Object> row : result ) {
			tailRtr = (String) row.get("tunnel.tunnel_tail_ptnii");
			if (tailRtr == null) {
				tailRtr = (String) row.get("tunnel.tunnel_tail_dns");
			}
		}
		if (tailRtr == null) {
			tailRtr = parseTailRtr(tunnelName);
		}
		if (tailRtr != null) {
			alarm.setCustomFieldValue("tail-end-router", tailRtr.toUpperCase());
		}
	}
	
	public String parseTailRtr(String tunnelName) {
		
		String tailRtr = null;
		String tunnel = tunnelName.split("/")[1];
		if (tunnel != null && tunnel.contains("_")) {
			tailRtr = tunnel.split("_")[1];
		}
		return tailRtr;
	}

	
	public void FetchLoopBackIPsFromTunnel (String tunnelInstance, SyslogAlarm alarm)
	{
		if (log.isTraceEnabled()) 
			LogHelper.enter(log, "FetchLoopBackIPsFromTunnel()");

		String query = null;

		if(engine == null) 
			engine = GFPUtil.getCypherEngine();

		query ="START tunnel=node:Tunnel(key=\"" + tunnelInstance + "\")"
				+ " MATCH tunnel-[:Composed_Of]-(Protection_Path_Hop)"
				+ " WHERE tunnel.class = \"IpagTunnel\" AND Protection_Path_Hop.class=\"IpagProtectionPathHop\""
				+ " RETURN Protection_Path_Hop";
		
		if (log.isDebugEnabled())
			log.debug("The Query is:  " + query);

		ExecutionResult result = engine.execute(query);
		
		Node hopNode = null;
		Iterator<Node> columnRows = result.columnAs("Protection_Path_Hop");
		while (columnRows.hasNext()) {
			
			
			hopNode = columnRows.next();
			
			if (log.isDebugEnabled())                                   
				log.debug("Got a hop:  " + (String) hopNode.getProperty("key"));
		

			
			// here we store all of the loopback ips
			if((String) hopNode.getProperty("start_end_device_lookbackip") != null &&
					!((String) hopNode.getProperty("start_end_device_lookbackip")).isEmpty())
				alarm.addPPHip((String) hopNode.getProperty("start_end_device_lookbackip"));
			
			if((String) hopNode.getProperty("end_end_device_lookbackip") != null &&
					!((String) hopNode.getProperty("end_end_device_lookbackip")).isEmpty())
				alarm.addPPHip((String) hopNode.getProperty("end_end_device_lookbackip"));
			
			if((String) hopNode.getProperty("hop_head_lookbackip") != null &&
					!((String) hopNode.getProperty("hop_head_lookbackip")).isEmpty())
				alarm.addPPHip((String) hopNode.getProperty("hop_head_lookbackip"));
			
			if((String) hopNode.getProperty("hop_tail_lookbackip") != null &&
					!((String) hopNode.getProperty("hop_tail_lookbackip")).isEmpty())
				alarm.addPPHip((String) hopNode.getProperty("hop_tail_lookbackip"));
			

			// save the lag id and ip
			if((String) hopNode.getProperty("start_lag_id") != null &&
					!((String) hopNode.getProperty("start_lag_id")).isEmpty()) {
				if (log.isDebugEnabled())
					log.trace("from topology lag id: "+ (String) hopNode.getProperty("start_lag_id"));
				alarm.addHopStartLagId((String) hopNode.getProperty("start_lag_id") );
			}
			
			if((String) hopNode.getProperty("start_lag_ip") != null &&
					!((String) hopNode.getProperty("start_lag_ip")).isEmpty()) {
				if (log.isDebugEnabled())
					log.trace("from topology lag ip: "+ (String) hopNode.getProperty("start_lag_ip"));
				alarm.addHopStartLagIP((String) hopNode.getProperty("start_lag_ip") );
			}
		}
		if (log.isTraceEnabled()) 
			LogHelper.exit(log, "FetchLoopBackIPsFromTunnel()");

	}

	
	
	// these queries are no longer required because of a changed in the DB.   The above query will now suffice
	/*public String FetchLoopBackIPFromTunnelStartEndDeviceClli (String tunnelInstance, SyslogAlarm alarm)
	{
		if (log.isTraceEnabled()) 
			LogHelper.enter(log, "FetchLoopBackIPFromTunnelStartEndDeviceClli()");

		String query = null;
		String startDeviceClli = null;
		//String tunnelThatWasFound = null;
		String loopBackIP = null;


		if(engine == null) 
			engine = GFPUtil.getCypherEngine();

		query ="START tunnel=node:Tunnel(key=\"" + tunnelInstance + "\")"
				+ " MATCH tunnel-[:Composed_Of]-(Protection_Path_Hop)"
				+ " WHERE tunnel.class = \"IpagTunnel\" AND Protection_Path_Hop.class=\"IpagProtectionPathHop\""
				+ " RETURN Protection_Path_Hop.start_end_device_clli, Protection_Path_Hop.TDL_instance_name, Protection_Path_Hop.start_lag_id, Protection_Path_Hop.start_lag_ip"; 

		if (log.isDebugEnabled())
			log.debug("The Query is:  " + query);

		ExecutionResult result = engine.execute(query);

		for (Map<String, Object> row : result ) 
		{
			String resultSECilli = (String)row.get("Protection_Path_Hop.start_end_device_clli");

			if(resultSECilli != null && !resultSECilli.isEmpty()) {

				if (log.isTraceEnabled()) {
					log.trace("Hop is:" + resultSECilli );
					log.trace("the Protection_Path_Hop.start_end_device_clli found is:" + (String)row.get("Protection_Path_Hop.start_end_device_clli") );
					log.trace("the start_lag_id is: " + (String)row.get("Protection_Path_Hop.start_lag_id") );
					log.trace("the start_lag_ip is: " + (String)row.get("Protection_Path_Hop.start_lag_ip") );
				}

				if(startDeviceClli == null)
					startDeviceClli= resultSECilli;
				else {
					if(startDeviceClli.equals(resultSECilli)) {
						if (log.isTraceEnabled())
							log.trace("Good, same device clli found in records !");
					} else {
						if (log.isTraceEnabled())
							log.trace("Bad, the device clli associated with the tunnel start is different !!");
					}
				}

				// save the lag id and ip
				alarm.setStartLagId((String)row.get("Protection_Path_Hop.start_lag_id") );
				alarm.setStartLagIp((String)row.get("Protection_Path_Hop.start_lag_ip") );

				if (log.isTraceEnabled())
					log.trace("from topology lag id: "+alarm.getStartLagId()+" lag ip; "+alarm.getStartLagIp());
			}
		}

		if(startDeviceClli  == null)
		{
			if (log.isTraceEnabled())
				log.trace("Query failed to find a start end device clli !!!");
		} else {
			query = "START home=node(0)  MATCH (home)-[:networkElement]->(device) " +
			//query = "START home=node(0)  MATCH (home)-[:networkdevice]->(device) " +
					" WHERE device.class = \"IpagDevice\" AND device.device_name = \"" + startDeviceClli + "\" "
					+ "RETURN  device.TDL_instance_name, device.loopback_ipaddr";

			if (log.isDebugEnabled())
				log.debug("The query is: " + query);

			result = engine.execute(query);
			for (Map<String, Object> row : result ) 
			{
				loopBackIP = ((String)row.get("device.loopback_ipaddr"));
				if (log.isTraceEnabled())
					log.trace("the loopback ip address on device is:" + loopBackIP );
			}
			if(loopBackIP == null || loopBackIP.isEmpty())
			{
				if (log.isTraceEnabled())
					log.trace("The device.loopback_ipaddr with is NOT FOUND for device name= "+ startDeviceClli );
				loopBackIP = null;

			}
		}

		if (log.isTraceEnabled()) 
			LogHelper.exit(log, "FetchLoopBackIPFromTunnelStartEndDeviceClli()");

		return loopBackIP;
	}


	public String FetchLoopBackIPFromHead_ptnii (String tunnelInstance, SyslogAlarm alarm)
	{
		if (log.isTraceEnabled()) 
			LogHelper.enter(log, "FetchLoopBackIPFromHead_ptnii()");

		String query = null;
		String headPtnii = null;
		//String tunnelThatWasFound = null;
		String loopBackIP = null;


		if(engine == null) 
			engine = GFPUtil.getCypherEngine();

		query = "START tunnel=node:Tunnel(key=\"" + tunnelInstance + "\")"
				+ " MATCH tunnel-[:Composed_Of]->(Protection_Path_Hop)"
				+ " WHERE  tunnel.tunnel_head_ptnii = Protection_Path_Hop.hop_head_ptnii AND tunnel.class = \"IpagTunnel\" AND Protection_Path_Hop.class=\"IpagProtectionPathHop\""
				+ " RETURN Protection_Path_Hop.TDL_instance_name, Protection_Path_Hop.hop_head_ptnii, Protection_Path_Hop.start_lag_id, Protection_Path_Hop.start_lag_ip";

		if (log.isDebugEnabled())
			log.debug("The Query is:  " + query);

		ExecutionResult result = engine.execute(query);

		for (Map<String, Object> row : result ) 
		{
			if (log.isTraceEnabled()) {
				log.trace("Hop is:" + (String)row.get("Protection_Path_Hop.TDL_instance_name") );
				log.trace("the Protection_Path_Hop.hop_head_ptnii found is:" + (String)row.get("Protection_Path_Hop.hop_head_ptnii") );
			}
			
			if((String)row.get("Protection_Path_Hop.hop_head_ptnii") != null &&
					!((String)row.get("Protection_Path_Hop.hop_head_ptnii")).isEmpty()) {
				if(headPtnii == null)
					headPtnii= (String)row.get("Protection_Path_Hop.hop_head_ptnii");
				else {
					if(headPtnii.equals((String)row.get("Protection_Path_Hop.hop_head_ptnii")))
						if (log.isTraceEnabled())
							log.trace("Good, same head ptnii found in records !");
					else
						if (log.isTraceEnabled())
							log.trace("Bad, the head ptnii associated with the tunnel start is different !!");
				}
				
				// save the lag id and ip
				alarm.setStartLagId((String)row.get("Protection_Path_Hop.start_lag_id") );
				alarm.setStartLagIp((String)row.get("Protection_Path_Hop.start_lag_ip") );

				if (log.isTraceEnabled())
					log.trace("from topology lag id: "+alarm.getStartLagId()+" lag ip; "+alarm.getStartLagIp());
			}
		}

		if(headPtnii  == null)
		{
			if (log.isTraceEnabled())
				log.trace("Query failed to find a hop head ptnii !!!");
		} else {
			query = "START home=node(0)  MATCH (home)-[:networkElement]->(device) "
					+ " WHERE device.class = \"IpagDevice\" AND (device.ptnii = \"" + headPtnii.toUpperCase() + "\" OR device.device_name = \""+ headPtnii.toUpperCase() 
					+ "\") RETURN device.TDL_instance_name, device.loopback_ipaddr ";

			if (log.isDebugEnabled())
				log.debug("The query is: " + query);

			result = engine.execute(query);
			for (Map<String, Object> row : result ) 
			{
				loopBackIP = ((String)row.get("device.loopback_ipaddr"));
				if (log.isTraceEnabled())
					log.trace("the loopback ip address on device is:" + loopBackIP );
			}
			if(loopBackIP == null || loopBackIP.isEmpty())
			{
				if (log.isTraceEnabled())
					log.trace("The device.loopback_ipaddr with is NOT FOUND for device name= "+ headPtnii.toUpperCase() );
				loopBackIP = null;
			}
		}

		if (log.isTraceEnabled()) 
			LogHelper.exit(log, "FetchLoopBackIPFromHead_ptnii()");

		return loopBackIP;
	}


	public String FetchLoopBackIPFromTunnelEndEndDeviceClli (String tunnelInstance, SyslogAlarm alarm)
	{
		if (log.isTraceEnabled()) 
			LogHelper.enter(log, "FetchLoopBackIPFromTunnelEndEndDeviceClli()");

		String query = null;
		String endDeviceClli = null;
		String loopBackIP = null;


		if(engine == null) 
			engine = GFPUtil.getCypherEngine();

		query = "START tunnel=node:Tunnel(key=\"" + tunnelInstance + "\")"
				+ " MATCH tunnel-[:Composed_Of]->(Protection_Path_Hop)"
				+ " WHERE (NOT Protection_Path_Hop.end_end_device_clli = \"\") AND tunnel.class = \"IpagTunnel\" AND Protection_Path_Hop.class=\"IpagProtectionPathHop\""
				+ " RETURN Protection_Path_Hop.TDL_instance_name, Protection_Path_Hop.end_end_device_clli, Protection_Path_Hop.end_lag_id, Protection_Path_Hop.end_lag_ip";

		if (log.isDebugEnabled())
			log.debug("The Query is:  " + query);

		ExecutionResult result = engine.execute(query);

		for (Map<String, Object> row : result ) 
		{
			if (log.isTraceEnabled()) {
				log.trace("Hop is:" + (String)row.get("Protection_Path_Hop.TDL_instance_name") );
				log.trace("the Protection_Path_Hop.end_end_device_clli found is:" + (String)row.get("Protection_Path_Hop.end_end_device_clli") );
			}
			
			if((String)row.get("Protection_Path_Hop.end_end_device_clli") != null &&
					!((String)row.get("Protection_Path_Hop.end_end_device_clli")).isEmpty()) {
				if(endDeviceClli == null)
					endDeviceClli= (String)row.get("Protection_Path_Hop.end_end_device_clli");
				else {
					if(endDeviceClli.equals((String)row.get("Protection_Path_Hop.end_end_device_clli")))
						if (log.isTraceEnabled())
							log.trace("Good, same end_end_device clli found in records !");
					else
						if (log.isTraceEnabled())
							log.trace("Bad, the end_end_device clli associated with the tunnel start is different !!");
				}
				
				// save the lag id and ip
				alarm.setStartLagId((String)row.get("Protection_Path_Hop.end_lag_id") );
				alarm.setStartLagIp((String)row.get("Protection_Path_Hop.end_lag_ip") );

				if (log.isTraceEnabled())
					log.trace("from topology lag id: "+alarm.getStartLagId()+" lag ip; "+alarm.getStartLagIp());
			}
		}

		if(endDeviceClli  == null)
		{
			if (log.isTraceEnabled())
				log.trace("Query failed to find a end end device clli !!!");
		} else {
			query = "START home=node(0)  MATCH (home)-[:networkElement]->(device) "
					+ " WHERE device.class = \"IpagDevice\" AND device.device_name = \"" + endDeviceClli.toUpperCase() + "\""
					+ " RETURN device.TDL_instance_name, device.loopback_ipaddr ";

			if (log.isDebugEnabled())
				log.debug("The query is: " + query);

			result = engine.execute(query);
			for (Map<String, Object> row : result ) 
			{
				loopBackIP = ((String)row.get("device.loopback_ipaddr"));
				if (log.isTraceEnabled())
					log.trace("the loopback ip address on device is:" + loopBackIP );
			}
			if(loopBackIP == null || loopBackIP.isEmpty())
			{
				if (log.isTraceEnabled())
					log.trace("The device.loopback_ipaddr with is NOT FOUND for device name= "+ endDeviceClli.toUpperCase() );
				loopBackIP = null;
			}
		}

		if (log.isTraceEnabled()) 
			LogHelper.exit(log, "FetchLoopBackIPFromTunnelEndEndDeviceClli()");

		return loopBackIP;
	}

	public String FetchLoopBackIPFromTail_ptnii (String tunnelInstance, SyslogAlarm alarm)
	{
		if (log.isTraceEnabled()) 
			LogHelper.enter(log, "FetchLoopBackIPFromTail_ptnii()");

		String query = null;
		String tailPtnii = null;
		//String tunnelThatWasFound = null;
		String loopBackIP = null;


		if(engine == null) 
			engine = GFPUtil.getCypherEngine();

		query = "START tunnel=node:Tunnel(key=\"" + tunnelInstance + "\")"
				+ " MATCH tunnel-[:Composed_Of]->(Protection_Path_Hop)"
				+ " WHERE  tunnel.tunnel_tail_ptnii = Protection_Path_Hop.hop_tail_ptnii AND tunnel.class = \"IpagTunnel\" AND Protection_Path_Hop.class=\"IpagProtectionPathHop\""
				+ " RETURN Protection_Path_Hop.TDL_instance_name, Protection_Path_Hop.hop_tail_ptnii, Protection_Path_Hop.end_lag_id, Protection_Path_Hop.end_lag_ip";

		if (log.isDebugEnabled())
			log.debug("The Query is:  " + query);

		ExecutionResult result = engine.execute(query);

		for (Map<String, Object> row : result ) 
		{
			if (log.isTraceEnabled()) {
				log.trace("Hop is:" + (String)row.get("Protection_Path_Hop.TDL_instance_name") );
				log.trace("the Protection_Path_Hop.hop_tail_ptnii found is:" + (String)row.get("Protection_Path_Hop.hop_tail_ptnii") );
			}
			
			if((String)row.get("Protection_Path_Hop.hop_tail_ptnii") != null &&
					!((String)row.get("Protection_Path_Hop.hop_tail_ptnii")).isEmpty()) {
				if(tailPtnii == null)
					tailPtnii= (String)row.get("Protection_Path_Hop.hop_tail_ptnii");
				else {
					if(tailPtnii.equals((String)row.get("Protection_Path_Hop.hop_tail_ptnii")))
						if (log.isTraceEnabled())
							log.trace("Good, same tail ptnii found in records !");
					else
						if (log.isTraceEnabled())
							log.trace("Bad, the tail ptnii associated with the tunnel start is different !!");
				}
				
				// save the lag id and ip
				alarm.setStartLagId((String)row.get("Protection_Path_Hop.end_lag_id") );
				alarm.setStartLagIp((String)row.get("Protection_Path_Hop.end_lag_ip") );

				if (log.isTraceEnabled())
					log.trace("from topology lag id: "+alarm.getStartLagId()+" lag ip; "+alarm.getStartLagIp());
				
			}
		}

		if(tailPtnii  == null)
		{
			if (log.isTraceEnabled())
				log.trace("Query failed to find a hop tail ptnii !!!");
		} else {
			query = "START home=node(0)  MATCH (home)-[:networkElement]->(device) "
					+ " WHERE device.class = \"IpagDevice\" AND (device.ptnii = \"" + tailPtnii.toUpperCase() + "\" OR device.device_name = \""+ tailPtnii.toUpperCase() 
					+ "\") RETURN device.TDL_instance_name, device.loopback_ipaddr ";

			if (log.isDebugEnabled())
				log.debug("The query is: " + query);

			result = engine.execute(query);
			for (Map<String, Object> row : result ) 
			{
				loopBackIP = ((String)row.get("device.loopback_ipaddr"));
				if (log.isTraceEnabled())
					log.trace("the loopback ip address on device is:" + loopBackIP );
			}
			if(loopBackIP == null || loopBackIP.isEmpty())
			{
				if (log.isTraceEnabled())
					log.trace("The device.loopback_ipaddr with is NOT FOUND for device name= "+ tailPtnii.toUpperCase() );
				loopBackIP = null; 
			}
		}

		if (log.isTraceEnabled()) 
			LogHelper.exit(log, "FetchLoopBackIPFromTail_ptnii()");

		return loopBackIP;
	}

	*/
	
	
	
	public void FetchSyslog_SiteInformation(SyslogAlarm alarm, String localDevice, String savpnSiteID)
	{ 
		if (log.isTraceEnabled()) 
			LogHelper.enter(log, "FetchSyslog_SiteInformation()");


		//Existing: "LocalSite=<"localSiteId">, RemoteSite=<"remoteSiteId">"
		//Changes: "LocalSite=<"localSiteId">, RemoteSite=<"remoteIpAddress">"
		// the component field may have the remote site ip in it
		String component = alarm.getCustomFieldValue(GFPFields.COMPONENT);
		String PE_Device_RemoteIP = null;
		String PE_Device_Local_Savpn_Siteid = null;
		String remoteSite = null;
		String localDeviceName = null;
		String remoteDeviceName = null;	
		String comment = "";	


		// We have to get four pieces of information:
		//	1. The local device - this is the first part of the managed entity 'local_device/remotesite'
		//  2. The remote site - this is the second part of the managed entity 'local_device/remotesite' and was set in the LC class
		//  3. The remote device - it is in the component field or we use the remotesite to do a topology lookup to get it.
		//  4. The local site - is in the component field

		// first we try to get the site stuff from the component
		if(component != null && !component.isEmpty()) {
			PE_Device_Local_Savpn_Siteid = parseLabeledText(component, "LocalSite");
			remoteSite = parseLabeledText(component, "RemoteSite");

			if(remoteSite != null && !remoteSite.isEmpty() && remoteSite.contains(".")) {
				PE_Device_RemoteIP = remoteSite;
			}
			
			if (log.isTraceEnabled()) {
				log.trace("component: " + component);
				
				log.trace("From the component we have:  remote=" + remoteSite + " local=" + PE_Device_Local_Savpn_Siteid);
			
				log.trace("The local device savpn_siteid: "+PE_Device_Local_Savpn_Siteid  );

			}
			//set local site id
			if(PE_Device_Local_Savpn_Siteid != null && !PE_Device_Local_Savpn_Siteid.isEmpty())
				alarm.setLocalDevice_SavpnSiteID(PE_Device_Local_Savpn_Siteid);
			
			//at this point we have both the local and remote site ids saved in the SyslogAlarm attributes.
		}

		// if the remote site ip was not in the component then we have to query for it
		// 11/11/15 - TM1731. We no longer want to run this query as it is very expensive to get the Remote IP address. Update "component"
		// to state no remote ip address available.
/*		if(PE_Device_RemoteIP == null)
		{
			//returns Remote device
			String query="START home=node(0) "
					+"MATCH (home)-[:networkElement]-(PE_Device) "
					+"WHERE PE_Device.class=\"IpagDevice\" AND HAS (PE_Device.savpn_siteid)  AND PE_Device.savpn_siteid=\""+savpnSiteID+"\" "
					+"RETURN PE_Device.TDL_instance_name";		

			if(engine == null)
				engine = GFPUtil.getCypherEngine();

			if (log.isDebugEnabled())
				log.debug("Query to return RemoteIPAddress is: " + query);
			ExecutionResult result = engine.execute(query);

			for (Map<String, Object> row : result ) 
			{
				PE_Device_RemoteIP= (String)row.get("PE_Device.TDL_instance_name");		
				if (log.isDebugEnabled())
					log.debug("RemoteIPAddress instance: "+PE_Device_RemoteIP);
				// break;
			}
		}*/
		
		// Query to get the localDevice Name
		String query2="START device=node:PE_Device(key = \"" +localDevice+ "\") "
				+ " RETURN device.device_name";

		if (log.isTraceEnabled())
			log.trace("Query returns local device name is: " + query2);

		ExecutionResult result2 = engine.execute(query2);

		for (Map<String, Object> row : result2 ) 
		{
			localDeviceName=((String)row.get("device.device_name"));

			if (log.isTraceEnabled()) 
				log.trace("Local Device name fetched: )" + localDeviceName);
		}

		// if by now we don't have the remote device IP, give up...
		if(PE_Device_RemoteIP  == null || PE_Device_RemoteIP.isEmpty())
		{
			if (log.isDebugEnabled())
				log.debug("The remote device IP was not found for Remote SiteId: "+ remoteSite );
			remoteDeviceName = remoteSite;
			comment = " savpn_siteid mapping not available";
		} else { 
			// save the remote ip since we found it in topology, to be used later in the computeProblemEntity
			alarm.setRemoteDeviceIpaddr(PE_Device_RemoteIP);		

			//query2="START device=node:PE_Device(key = \"" +PE_Device_RemoteIP+ "\") "
			//		+ " RETURN device.device_name";
			
			query2="START device=node:PE_Device(key = \"" +PE_Device_RemoteIP+ "\") "
					+ " RETURN device";

			if (log.isTraceEnabled())
				log.trace("Query returns remote device name is: " + query2);

			result2 = engine.execute(query2);
			Node node = null;
			Iterator<Node> columnRows = result2.columnAs("device");
			
			while (columnRows.hasNext()) {
				node = columnRows.next();

				if(node.hasProperty("inmaint")) {
					if ( ((String)node.getProperty("inmaint")).equals("true") ) {
						alarm.setSuppressed(true);
						if (log.isTraceEnabled()) {
							log.trace("Suppressing alarm: " + alarm.getIdentifier() + " with SeqNum: " + alarm.getCustomFieldValue(GFPFields.SEQNUMBER) +
									  " since PE_Device_RemoteIP: " + PE_Device_RemoteIP + " has inmaint = true.");
						}
						break;
					}
				}	
				
				remoteDeviceName=((String)node.getProperty("device_name"));

				if (log.isTraceEnabled()) 
					log.trace("Remote Device name fetched: )" + remoteDeviceName);
				
			}	

/*			for (Map<String, Object> row : result2 ) 
			{					
				remoteDeviceName=((String)row.get("device.device_name"));

				if (log.isTraceEnabled()) 
					log.trace("Remote Device name fetched: )" + remoteDeviceName);

			}*/
		}

		if(remoteDeviceName != null && !remoteDeviceName.isEmpty() && localDeviceName != null && !localDeviceName.isEmpty())
			alarm.setCustomFieldValue(GFPFields.COMPONENT, fixBFDOWNcomponent(remoteDeviceName, localDeviceName, alarm.getCustomFieldValue(GFPFields.COMPONENT), comment));
		
		if (log.isTraceEnabled()) 
			LogHelper.exit(log, "FetchSyslog_SiteInformation()");

	}	
	
	private String fixBFDOWNcomponent(String remoteDeviceName,
			String localDeviceName, String oldComponent, String comment) {
	
		if (log.isTraceEnabled()) 
			LogHelper.enter(log, "fixBFDOWNcomponent(), Old component: " + oldComponent);

		//<customField value="deviceType=&lt;JUNIPER MX SERIES&gt; deviceModel=&lt;MX480&gt; LocalSite=&lt;1111&gt;, RemoteSite=&lt;201&gt;" name="component"/>  
		//<customField value="deviceType=<JUNIPER MX SERIES> deviceModel=<MX480> LocalSite=<1111>, RemoteSite=<201>" name="component"/>  
		String newComponent =  oldComponent.substring(0, oldComponent.indexOf("LocalSite"));
		newComponent = newComponent + "LocalSite=<" + localDeviceName + ">, RemoteSite=<" + remoteDeviceName + ">" + comment;
		
		
		if (log.isTraceEnabled()) 
			LogHelper.exit(log, "fixBFDOWNcomponent(), New component: " + newComponent);
		
		
		return newComponent;
	}

	public String parseLabeledText(String textStr, String label) {
		
		String parsedText = "";
		label += "=<";
		int i = textStr.indexOf(label);
		if (textStr.contains(label)) {
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
	
