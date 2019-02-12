package com.att.gfp.data.ipag.topoModel;

import java.util.Iterator;
import java.util.Map;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.data.ipagAlarm.Pri_Sec_Alarm;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
import com.hp.uca.expert.topology.TopoAccess;


/* topology access for ipag */
public class PriSecTopoAccess extends TopoAccess 
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
			
	private static Logger log = LoggerFactory.getLogger(PriSecTopoAccess.class);

	private static GraphDatabaseService override = null;
	
	private static PriSecTopoAccess topologyAccessor = null;
	private static ExecutionEngine engine;

	private PriSecTopoAccess() {
		super();
		engine = GFPUtil.getCypherEngine();
	}

	private static GraphDatabaseService getDb()
	{
		if ( override != null ) return override;
		return TopoAccess.getGraphDB ();
		
	}
	
	public static synchronized PriSecTopoAccess getInstance() {
		if (topologyAccessor == null) {
			topologyAccessor = new PriSecTopoAccess();
		}
		return topologyAccessor;
	}

	public static void testAccessSetDb ( GraphDatabaseService db )
	{
		override = db;
	}

	
	public String FetchLagIdForPPort(String pportInstance) {
		
		if (log.isTraceEnabled())
			log.trace("FetchLagIdForPPort() Enter : ");
		
		String lagId = "unknown";
		
		String query = "START pport=node:PE_PPort(key = \""
				+ pportInstance
				+ "\") RETURN pport.port_lag_id";
		
		if (log.isTraceEnabled())
			log.trace("##### CYPHER QUERY: " + query);

		if(engine == null)
			engine = GFPUtil.getCypherEngine();

		ExecutionResult result = engine.execute(query);

		// only one should be returned
		for ( Map<String, Object> row : result ) {
				lagId = (String)row.get("pport.port_lag_id");
			break;
		}

		if (log.isTraceEnabled()) {
			log.trace("#### PPORT:" + pportInstance + 
				" portLagId : " + lagId + "####");
		
			log.trace("FetchLocalPPortLevelInformationForLinkDownAlarm() Exit : ");
		}
		
		return lagId;
	}
		
	public String FetchDeviceFromPPort(String pPortInstance) {

		if (log.isTraceEnabled())
			log.trace("FetchDeviceFromPPort() Enter : ");

		String device = "unknown";
		
		// go from the local device to the local local port using the lagid from the alarm to the remote port
		// traversing all the relations in between.
		String query = "START pport=node:PE_PPort(key = \""
			+ pPortInstance
			+ "\") MATCH (pport)-[:Composed_Of]-(card)-[:Composed_Of]-(slot)-[:Composed_Of]-(dev)" +
			" WHERE slot.class=\"IpagSlot\" AND card.class=\"IpagCard\" AND " +
			"dev.class=\"IpagDevice\" RETURN dev.TDL_instance_name";
	
		if (log.isTraceEnabled())
			log.trace("##### CYPHER QUERY: " + query);

		if(engine == null)
			engine = GFPUtil.getCypherEngine();

		ExecutionResult result = engine.execute(query);
		
		// only one should be returned
		for ( Map<String, Object> row : result ) {
			device = ((String)row.get("dev.TDL_instance_name"));
			break;
		}

		if (log.isTraceEnabled()) {
			log.trace("#### Device :" + device + "####");
			log.trace("FetchDeviceFromPPort() Exit : ");
		}
		
		return device;
	}
	
	public void getPortLagId(String deviceIpAddr, String localIp, 
			Pri_Sec_Alarm alarm) {
			
			String nodeIndex = null;
			if (alarm.getNodeType().equals("PE")) {
				nodeIndex = "PE_Device";
			} else {
				nodeIndex = "CE_Device";
			}
			
			String query = "START device=node:" + nodeIndex + "(key = \""
					+ deviceIpAddr
					+ "\") "
					+ "MATCH (device)-[:Composed_Of]->(slot)-[:Composed_Of]->(card)"
					+	"-[:Composed_Of]->(pport) "
					+ "WHERE (slot.class = \"IpagSlot\" and card.class = \"IpagCard\" and "
					+	"pport.class = \"IpagPPort\" and pport.port_lag_ip = \""
					+	localIp + "\") "
					+ "RETURN pport";

			if (log.isTraceEnabled())
				log.trace("CYPHER QUERY: " + query);

			if(engine == null)
				engine = GFPUtil.getCypherEngine();
			
			ExecutionResult result = engine.execute(query);
			
			Node pportNode = null;
			Iterator<Node> columnRows = result.columnAs("pport");
			alarm.setCustomFieldValue("lag_port_key", "");
			alarm.setCustomFieldValue("lag_id", "");
			while (columnRows.hasNext()) {
				pportNode = columnRows.next();
				alarm.setCustomFieldValue("lag_port_key", 
					(String) pportNode.getProperty("key"));
				alarm.setCustomFieldValue("lag_id", 
					(String) pportNode.getProperty("port_lag_id"));
				//DF - Added 5/12/14 MR gfpc140375 
				alarm.setCustomFieldValue("lag_port_aid",
					(String) pportNode.getProperty("port_aid"));
				alarm.setRemotePePportInstanceName((String) pportNode.getProperty("remote_pport_key"));
				break;
			}
			if (log.isTraceEnabled())
				log.trace(alarm.getIdentifier() + " lag_id=" + alarm.getCustomFieldValue("lag_id") +
						 " lag_port_key=" + alarm.getCustomFieldValue("lag_port_key"));
		}

		public void getPortLagIdFromNeighbor(String device, String neighborIp, 
				Pri_Sec_Alarm alarm) {
				
				String nodeIndex = null;
				if (alarm.getNodeType().equals("PE")) {
					nodeIndex = "PE_Device";
				} else {
					nodeIndex = "CE_Device";
				}
				
				String[] octets = neighborIp.split("\\.");
				int last_octet = Integer.valueOf(octets[3]);
				if ( last_octet % 2 == 0) last_octet--;
				else last_octet++;
				neighborIp = octets[0] + "." + octets[1] + "." + octets[2] + "." + last_octet;
				
/*				String query = "START device=node:" + nodeIndex + "(key = \""
						+ neighborRtrIp
						+ "\") "
						+ "MATCH (device)-[:Composed_Of]->(slot)-[:Composed_Of]->(card)"
						+	"-[:Composed_Of]->(neighbor_pport)-[:PLink]->(local_pport) "
						+ "WHERE (slot.class = \"IpagSlot\" and card.class = \"IpagCard\" and "
						+	"neighbor_pport.class = \"IpagPPort\" and "
						+	"local_pport.class = \"IpagPPort\" and neighbor_pport.port_lag_ip = \""
						+	neighborIp + "\") "
						+ "RETURN local_pport";*/
				
				String query = "START device=node:" + nodeIndex + "(key = \""
						+ device
						+ "\") "
						+ "MATCH (device)-[:Composed_Of]->(slot)-[:Composed_Of]->(card)"
						+	"-[:Composed_Of]->(neighbor_pport) "
						+ "WHERE (slot.class = \"IpagSlot\" and card.class = \"IpagCard\" and "
						+	"neighbor_pport.class = \"IpagPPort\" and "
						+	"neighbor_pport.port_lag_ip = \""
						+	neighborIp + "\") "
						+ "RETURN neighbor_pport";

				if (log.isTraceEnabled())
					log.trace("CYPHER QUERY: " + query);

				if(engine == null)
					engine = GFPUtil.getCypherEngine();
				
				ExecutionResult result = engine.execute(query);
				
				Node pportNode = null;
				Iterator<Node> columnRows = result.columnAs("neighbor_pport");
				alarm.setCustomFieldValue("lag_port_key", "");
				alarm.setCustomFieldValue("lag_id", "");
				while (columnRows.hasNext()) {
					pportNode = columnRows.next();
					alarm.setCustomFieldValue("lag_port_key", 
						(String) pportNode.getProperty("key"));
					alarm.setCustomFieldValue("lag_id", 
						(String) pportNode.getProperty("port_lag_id"));
					//DF - Added 5/12/14 MR gfpc140375 
					alarm.setCustomFieldValue("lag_port_aid",
						(String) pportNode.getProperty("port_aid"));
					alarm.setRemotePePportInstanceName((String) pportNode.getProperty("remote_pport_key"));
							
					//break;
				}
				if (log.isTraceEnabled())
					log.trace(alarm.getIdentifier() + " lag_id=" + alarm.getCustomFieldValue("lag_id") +
							  " lag_port_key=" + alarm.getCustomFieldValue("lag_port_key"));
			}

		public void FetchPeeringTableInformation(Pri_Sec_Alarm alarm) {

			if (log.isTraceEnabled()) {
				log.trace("FetchPeeringTableInformation() Enter : ");
				log.trace("FetchPeeringTableInformation() PportDiverseCktId" + alarm.getRemotePportDiverseCktId());
			}
			
			// I don't want to make this call to fetch the peering port more than once
			alarm.getVar().put("triedPeeringPort", true);
			String diverseCktIdtoBeUsed = "";
			if(alarm.getRemoteDeviceType() != null && alarm.getDeviceType() != null) {
				if(alarm.getRemoteDeviceType().toUpperCase().contains("CIENA NTE") &&
						(alarm.getDeviceType().toUpperCase().contains("JUNIPER MX"))) {
					diverseCktIdtoBeUsed = alarm.getRemotePportDiverseCktId();
				} else if(alarm.getRemoteDeviceType().toUpperCase().contains("JUNIPER MX") &&
						(alarm.getDeviceType().toUpperCase().contains("CIENA NTE"))) {
					diverseCktIdtoBeUsed = alarm.getDiverseCircuitID();		 
				}
			}
			if (log.isTraceEnabled())
				log.trace("FetchPeeringTableInformation() diverseCktIdtoBeUsed " + diverseCktIdtoBeUsed);
			if (diverseCktIdtoBeUsed != null) { 

				String query = "START peering=node:PeeringTable(key = \"" + diverseCktIdtoBeUsed + "\") RETURN peering.local_pport_key,peering.remote_pport_key";

				if (log.isTraceEnabled())
					log.trace("##### CYPHER QUERY: " + query);
	    
				if(engine == null)
					engine = GFPUtil.getCypherEngine();

				ExecutionResult result = engine.execute(query);

				// only one should be returned
				for ( Map<String, Object> row : result ) {
					alarm.setRemotePeeringPort((String)row.get("peering.remote_pport_key"));
					alarm.setLocalPeeringPort((String)row.get("peering.local_pport_key")); 
					fetchRelatedClciAndPortAid(alarm);
					break;
				} 

				if (log.isTraceEnabled())
					log.trace("#### Remote peering port :" + alarm.getRemotePeeringPort() + "####"); 
			}

			if (log.isTraceEnabled())
				log.trace("FetchPeeringTableInformation() Exit : ");
		}
		
		private void fetchRelatedClciAndPortAid(Pri_Sec_Alarm alarm) { 

			if (log.isTraceEnabled())
				log.trace("fetchRelatedClciAndPortAid() Enter : ");


				String query = "START other=node:CE_PPort(key = \"" + alarm.getLocalPeeringPort() + "\") RETURN other.remote_device_name,other.remote_port_aid";

				if (log.isTraceEnabled())
					log.trace("##### CYPHER QUERY: " + query); 
	  
				if(engine == null)
					engine = GFPUtil.getCypherEngine();

				ExecutionResult result = engine.execute(query);

				// only one should be returned
				for ( Map<String, Object> row : result ) {
					alarm.setRelatedCLLI(((String)row.get("other.remote_device_name")));
					alarm.setRelatedPortAID(((String)row.get("other.remote_port_aid"))); 
					break;
				} 

				if (log.isTraceEnabled()) {
					log.trace("#### Related CLLI :" + alarm.getRelatedCLLI() + " RelatedPortAid = " + alarm.getRelatedPortAID());   
					log.trace("fetchRelatedClciAndPortAid() Exit : ");
				}
		}
		
		public void FetchLPortInfo(Pri_Sec_Alarm alarm, String lportInstance) {
			String clci = null;
			String bmp = null;
			
			if (log.isTraceEnabled())
				log.trace("FetchLPortInfo() Enter : ");

			String query = "START peLPort=node:PE_LPort(key = \""
				+ lportInstance
				+ "\")" 
				+ " RETURN peLPort.clci, peLPort.bmp_clci";

			if (log.isDebugEnabled())
				log.debug("##### CYPHER QUERY: " + query);

			if(engine == null)
				engine = GFPUtil.getCypherEngine();

			ExecutionResult result = engine.execute(query);
			
			// only one should be returned
			for ( Map<String, Object> row : result ) {
				clci = (String)row.get("peLPort.clci");
				bmp = (String)row.get("peLPort.bmp_clci");
			}

			// the clci custom field
			alarm.setCustomFieldValue(GFPFields.CLCI, clci);
			alarm.setCustomFieldValue(GFPFields.BMP_CLCI, bmp);
			
			if (log.isDebugEnabled())
				log.debug("#### LPort" + lportInstance + ": clci=" + clci + " bmp-clci=" + bmp);

			if (log.isTraceEnabled())
				log.trace("FetchLPortInfo() Exit : ");
		}			
		
		public String findAllLagPports(Pri_Sec_Alarm priSecAlarm, String lagId) {
			if (log.isTraceEnabled())
				log.trace("findAllLagPports() Enter : ");

			String device = priSecAlarm.getDeviceIpAddr();
			
			String pports = "";

			//if the scpservice of L = "AVPN" or "MIS" or "OEW" 
			String query = "Start device=node:PE_Device(key = \"" 
				+ device
				+ "\") MATCH (device)-[:Composed_Of]-(slot)-[:Composed_Of]-(card)-[:Composed_Of]-(pport)"
				+ " WHERE device.class=\"IpagDevice\" AND pport.class=\"IpagPPort\" AND slot.class=\"IpagSlot\" AND card.class=\"IpagCard\""
				+ " AND pport.port_lag_id=\"" + lagId 
				+ "\" RETURN pport.TDL_instance_name, pport.remote_pport_key";     
						
			if (log.isDebugEnabled())
				log.debug("##### CYPHER QUERY: " + query);

			if(engine == null)
				engine = GFPUtil.getCypherEngine();

			ExecutionResult result = engine.execute(query);
		
			// could be a few returned
			for ( Map<String, Object> row : result ) {
				if (log.isDebugEnabled())
					log.debug("findAllLagPorts():  PPort found: " + (String)row.get("pport.TDL_instance_name"));

				pports = (String)row.get("pport.TDL_instance_name");
				priSecAlarm.setCustomFieldValue("lag_port_key", (String)row.get("pport.TDL_instance_name"));
				
				
				if ( (String)row.get("pport.remote_pport_key") != null && !((String)row.get("pport.remote_pport_key")).isEmpty() ) {
					priSecAlarm.setRemotePePportInstanceName((String)row.get("pport.remote_pport_key"));
				}
			}

			if (log.isTraceEnabled())
				log.trace("findAllLagPports() Exit : ");
			
			return pports;		
		}
}
	
