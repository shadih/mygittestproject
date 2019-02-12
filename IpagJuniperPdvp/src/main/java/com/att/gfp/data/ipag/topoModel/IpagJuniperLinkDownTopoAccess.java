package com.att.gfp.data.ipag.topoModel;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;

import com.att.gfp.helper.GFPUtil;
import com.att.gfp.helper.GFPFields;

import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.data.ipagAlarm.EnrichedJuniperAlarm;
import com.hp.uca.expert.topology.TopoAccess;

/* topology access for ipag */
public class IpagJuniperLinkDownTopoAccess extends TopoAccess 
{
	
	//private static final String CE = "CE";
	//private static final String PE = "PE";
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
		
	
	private static IpagJuniperLinkDownTopoAccess topologyAccessor = null;
	private ExecutionEngine engine;

	private IpagJuniperLinkDownTopoAccess() {
		super();
		//engine = new ExecutionEngine(getGraphDB());
		engine = GFPUtil.getCypherEngine();
	}

	public static synchronized IpagJuniperLinkDownTopoAccess getInstance() {
		if (topologyAccessor == null) {
			topologyAccessor = new IpagJuniperLinkDownTopoAccess();
		}
		return topologyAccessor;
	}

	
	
	private static final Logger log = Logger.getLogger ( IpagJuniperLinkDownTopoAccess.class );
	private static GraphDatabaseService override = null;
	
	private static GraphDatabaseService getDb()
	{
		if ( override != null ) return override;
		return TopoAccess.getGraphDB ();
	}

	public static void testAccessSetDb ( GraphDatabaseService db )
	{
		override = db;
	}
	

	public void FetchRemoteDeviceRelatedLag(
			String lagId, String localPPortKey, EnrichedJuniperAlarm alarm) {

		if (log.isTraceEnabled())
			log.trace("FetchRemoteDeviceRelatedLag() Enter : ");

		// go from the local device to the local local port using the lagid from the alarm to the remote port
		// traversing all the relations in between.
		//		String query = "START locDev=node:PE_Device(key = \""
		//			+ deviceInstance
		//			+ "\") MATCH (locDev)-[:Composed_Of]->(locSlot)-[:Composed_Of]->(locCard)-[:Composed_Of]->(locPPort)-[:PLink]-(remPPort)" +
		//			"-[:Composed_Of]-(card)-[:Composed_Of]-(slot)-[:Composed_Of]-(remdev)" +
		//			" WHERE locPPort.class=\"IpagPPort\" AND locSlot.class=\"IpagSlot\" AND locCard.class=\"IpagCard\" AND " +
		//			"remPPort.class=\"IpagPPort\" AND remdev.class=\"IpagDevice\" AND slot.class=\"IpagSlot\" AND card.class=\"IpagCard\" AND locPPort.port_lag_id = \"" + lagId + "\" RETURN remdev.TDL_instance_name,locPPort.port_aid, locPPort.remote_port_aid";


		String query = "START locPPort=node:PE_PPort(key = \""
				+ localPPortKey + "\")"  
				+ "MATCH (locPPort)-[:PLink]-(remPPort)" 
				+ " RETURN remPPort.TDL_instance_name";


		if (log.isDebugEnabled())
			log.debug("##### CYPHER QUERY: " + query);

		if(engine == null)
			engine = GFPUtil.getCypherEngine();
		//engine = new ExecutionEngine(getGraphDB()); 

		ExecutionResult result = engine.execute(query);

		// only one should be returned
		for ( Map<String, Object> row : result ) {

			if (log.isDebugEnabled())
				log.debug("#### lagId on local end :" + lagId + 
						" Remote Port " + (String)row.get("remPPort.TDL_instance_name") );

					alarm.setRemoteDevice(((String)row.get("remPPort.TDL_instance_name")).split("/")[0]);
					// set remote device IP address against the Enriched Alarm as well since downstream processing needs this from EnrichedAlarm
					alarm.setRemoteDeviceIpaddr(((String)row.get("remPPort.TDL_instance_name")).split("/")[0]);
					alarm.setRemotePportInstanceName((String)row.get("remPPort.TDL_instance_name"));
		}

		if (log.isTraceEnabled())
			log.trace("FetchRemoteDeviceRelatedLag() Exit : ");
	}

	public void FetchDeviceFromPPort(String pPortInstance, EnrichedJuniperAlarm alarm) {

		if (log.isTraceEnabled())
			log.trace("FetchDeviceFromPPort() Enter : ");
		
		
		// instead of doing this expensive query we just take the port and derive the device from
		// it by taking everything from the first /
		alarm.setDeviceInstance(pPortInstance.split("/")[0]);

		// go from the local device to the local local port using the lagid from the alarm to the remote port
		// traversing all the relations in between.
		/*String query = "START pport=node:PE_PPort(key = \""
			+ pPortInstance
			+ "\") MATCH (pport)-[:Composed_Of]-(card)-[:Composed_Of]-(slot)-[:Composed_Of]-(dev)" +
			" WHERE slot.class=\"IpagSlot\" AND card.class=\"IpagCard\" AND " +
			"dev.class=\"IpagDevice\" RETURN dev.TDL_instance_name";
	
		if (log.isDebugEnabled())
			log.debug("##### CYPHER QUERY: " + query);

		if(engine == null)
			engine = GFPUtil.getCypherEngine();
			//engine = new ExecutionEngine(getGraphDB());

		ExecutionResult result = engine.execute(query);
		
		// only one should be returned
		for ( Map<String, Object> row : result ) {
			alarm.setDeviceInstance((String)row.get("dev.TDL_instance_name"));
		}*/

		if (log.isDebugEnabled())
			log.debug("#### Device :" + alarm.getDeviceInstance() + "####");
		
		if (log.isTraceEnabled())
			log.trace("FetchDeviceFromPPort() Exit : ");
	}


	public void FetchPeeringTableInformation(EnrichedJuniperAlarm alarm) {

		if (log.isTraceEnabled()) {
			log.trace("FetchPeeringTableInformation() Enter : ");

			log.trace("FetchPeeringTableInformation() PportDiverseCktId"
				+ alarm.getRemotePportDiverseCktId());
		}
		
		// I don't want to make this call to fetch the peering port more than
		// once
		alarm.getVar().put("triedPeeringPort", true);
		String diverseCktIdtoBeUsed = "";
		// String clfi = alarm.getPortCLFI();
		if (alarm.getRemoteDeviceType() == null || alarm.getDeviceType() == null) {
			diverseCktIdtoBeUsed = null;
		} else if (alarm.getRemoteDeviceType().toUpperCase().contains("CIENA NTE")
				&& (alarm.getDeviceType().toUpperCase().contains("JUNIPER MX"))) {
			diverseCktIdtoBeUsed = alarm.getRemotePportDiverseCktId();
		} else if (alarm.getRemoteDeviceType().toUpperCase().contains("JUNIPER MX")
				&& (alarm.getDeviceType().toUpperCase().contains("CIENA NTE"))) {
			diverseCktIdtoBeUsed = alarm.getDiverseCircuitID();
		}

		if (log.isTraceEnabled())
			log.trace("FetchPeeringTableInformation() diverseCktIdtoBeUsed " + diverseCktIdtoBeUsed);

		if (diverseCktIdtoBeUsed != null) { 

			String query = "START peering=node:PeeringTable(key = \"" + diverseCktIdtoBeUsed + "\") RETURN peering.local_pport_key,peering.remote_pport_key";

			if (log.isDebugEnabled())
				log.debug("##### CYPHER QUERY: " + query);
    
			if(engine == null)
				engine = GFPUtil.getCypherEngine();

			ExecutionResult result = engine.execute(query);

			// only one should be returned
			for ( Map<String, Object> row : result ) {
				alarm.setRemotePeeringPort((String)row.get("peering.remote_pport_key"));
				alarm.setLocalPeeringPort((String)row.get("peering.local_pport_key")); 
                               // tj: move it outside the for loop as it is
                                // done once
                                // fetchRelatedClciAndPortAid(alarm);
                                // break;
			} 
                       fetchRelatedClciAndPortAid(alarm);      // tj

			if (log.isDebugEnabled())
				log.debug("#### Remote peering port :" + alarm.getPeeringPort() + "####"); 
		}

		if (log.isTraceEnabled())
			log.trace("FetchPeeringTableInformation() Exit : ");
	}	
	
	private void fetchRelatedClciAndPortAid(EnrichedJuniperAlarm alarm) { 

		if (log.isTraceEnabled())
			log.trace("fetchRelatedClciAndPortAid() Enter : ");


			String query = "START other=node:CE_PPort(key = \"" + alarm.getLocalPeeringPort() + "\") RETURN other.remote_device_name,other.remote_port_aid";

			if (log.isDebugEnabled())
				log.debug("##### CYPHER QUERY: " + query); 
  
			if(engine == null)
				engine = GFPUtil.getCypherEngine();
				//engine = new ExecutionEngine(getGraphDB()); 

			ExecutionResult result = engine.execute(query);

			// only one should be returned
			for ( Map<String, Object> row : result ) {
				alarm.setRelatedCLLI(((String)row.get("other.remote_device_name")));
				alarm.setRelatedPortAID(((String)row.get("other.remote_port_aid"))); 
			} 

			if (log.isDebugEnabled())
				log.debug("#### Related CLLI :" + alarm.getRelatedCLLI() + " RelatedPortAid = " + alarm.getRelatedPortAID());   

			if (log.isTraceEnabled())
				log.trace("fetchRelatedClciAndPortAid() Exit : ");
		
	}
	
	public boolean remoteDeviceExists(String ipAddress, EnrichedJuniperAlarm alarm) {
		
		boolean ret = deviceExists(ipAddress, "PE_Device", alarm);
		if (!ret) {
			ret = deviceExists(ipAddress, "CE_Device", alarm);
		}
		return ret;
	}
	

	public boolean deviceExists(String ipAddress, String nodeIndex, EnrichedJuniperAlarm alarm) {

		String query = "START device=node:" + nodeIndex + "(key = \""
				+ ipAddress
				+ "\") "
				+ "RETURN device";
		
		if (log.isDebugEnabled())
			log.debug("##### : CYPHER QUERY " + query);

		if(engine == null)
			engine = GFPUtil.getCypherEngine();
			//engine = new ExecutionEngine(getGraphDB());
		
		ExecutionResult result = engine.execute(query);

		boolean nodeResult = false;
		Node node = null;
		Iterator<Node> columnRows = result.columnAs("device");
		while (columnRows.hasNext()) {
			node = columnRows.next();
			alarm.setRemoteDeviceType((String)node.getProperty("device_type")); 
			nodeResult = true;
		}
		
		if (log.isDebugEnabled())
			log.debug("Remote devie type = "+ alarm.getRemoteDeviceType());
		return nodeResult;
	}
	
	
	public String getPPortRemoteDeviceType(String pport, String nodeType) {
		
		String nodeIndex = null;
		if ("PE".equals(nodeType)) {
			nodeIndex = "PE_PPort";
		} else {
			nodeIndex = "CE_PPort";
		}
		
		String query = "START pport=node:" + nodeIndex + "(key = \""
				+ pport
				+ "\") "
				+ "RETURN pport";

		if (log.isDebugEnabled())
			log.debug("##### : CYPHER QUERY " + query);

		if(engine == null)
			engine = GFPUtil.getCypherEngine();

		ExecutionResult result = engine.execute(query);

		Node pportNode = null;
		String rdeviceType = "not_found";
		Iterator<Node> columnRows = result.columnAs("pport");
		while (columnRows.hasNext()) {
			pportNode = columnRows.next();
			rdeviceType = (String) pportNode.getProperty("remote_device_type");
		}
		
		if (log.isDebugEnabled())
			log.debug(pport + " rdeviceType=" + rdeviceType);

		return rdeviceType;
	}

	
	public int getCrsPportCount(String ipAddress, String nodeType) {
		
		String nodeIndex = null;
		if ("PE".equals(nodeType)) {
			nodeIndex = "PE_Device";
		} else {
			nodeIndex = "CE_Device";
		}
		
		String query = "START device=node:" + nodeIndex + "(key = \""
				+ ipAddress
				+ "\") "
				+ "MATCH (device)-[:Composed_Of]->(slot)-[:Composed_Of]->(card)"
				+	"-[:Composed_Of]->(pport) "
				+ "WHERE (slot.class = \"IpagSlot\" and card.class = \"IpagCard\" and "
				+	"pport.class = \"IpagPPort\" and pport.remote_device_type=\"crs\") " 
				+ "RETURN pport";

		if (log.isDebugEnabled())
			log.debug("##### : CYPHER QUERY " + query);

		if(engine == null)
			engine = GFPUtil.getCypherEngine();
			//engine = new ExecutionEngine(getGraphDB());

		ExecutionResult result = engine.execute(query);

		int crsPportCount = 0;
		Iterator<Node> columnRows = result.columnAs("pport");
		while (columnRows.hasNext()) {
			crsPportCount++;
			columnRows.next();
		}
		
		if (log.isDebugEnabled())
			log.debug(ipAddress + " crsPportCount=" + crsPportCount);
		return crsPportCount;
	}
	
	public boolean getEvcInfo(String evcName, EnrichedAlarm alarm) {
		boolean ret = false;
		
		String query = "START evcNode=node:EVCNode(key = \"" + evcName + "\") "
				+ "RETURN evcNode";
//				+ "RETURN evcNode.key, evcNode.alarm_classification, evcNode.alarm_domain, "
//				+ 	"evcNode.data_source, evcNode.acnaban, evcNode.vrf_name, evcNode.evc_name, "
//				+	"evcNode.unickt";

		if (log.isDebugEnabled())
			log.debug(": " + query);

		if(engine == null)
			engine = GFPUtil.getCypherEngine();
		
		ExecutionResult result = engine.execute(query);

		if (log.isDebugEnabled())
			log.debug("Query returns... ");

		Node evcNodeNode = null;
		Iterator<Node> columnRows = result.columnAs("evcNode");
		
		while (columnRows.hasNext()) {
			evcNodeNode = columnRows.next();
			
			if (log.isDebugEnabled())
				log.debug("found node:" + (String) evcNodeNode.getProperty("TDL_instance_name"));
		
			alarm.setCustomFieldValue("vrf-name", (String) evcNodeNode.getProperty("vrf_name"));
			alarm.setCustomFieldValue("clci", (String) evcNodeNode.getProperty("unickt"));
			alarm.setCustomFieldValue("circuit-id", (String) evcNodeNode.getProperty("unickt"));
			alarm.setCustomFieldValue("acnaban", (String) evcNodeNode.getProperty("acnaban"));
			alarm.setCustomFieldValue("evc-name", (String) evcNodeNode.getProperty("evc_name"));
			ret = true;
		}
		
//		for ( Map<String, Object> row : result ) {
//			alarm.setCustomFieldValue("vrf-name", (String) row.get("evcNode.vrf_name"));
//			alarm.setCustomFieldValue("clci", (String) row.get("evcNode.unickt"));
//			alarm.setCustomFieldValue("circuit-id", (String) row.get("evcNode.unickt"));
//			alarm.setCustomFieldValue("acnaban", (String) row.get("evcNode.acnaban"));
//			alarm.setCustomFieldValue("evc-name", (String) row.get("evcNode.evc_name"));
//		}
		return ret;
	}
	
	public void getPportFromEvc(String evcNode, EnrichedAlarm alarm) {
		
		String query = "START evcNode=node:EVCNode(key = \"" + evcNode + "\") "
				+ " MATCH (evcNode)-[:Composed_Of]-(PE_LPORT)-[:Composed_Of]-(PPORT)"
				+ " WHERE PE_LPORT.class=\"IpagLPort\" AND PPORT.class=\"IpagPPort\"" 
				+ "RETURN PPORT.port_aid, PPORT.key";
		

		if (log.isDebugEnabled())
			log.debug(": " + query);

		if(engine == null)
			engine = GFPUtil.getCypherEngine();

		ExecutionResult result = engine.execute(query);
		
		if (log.isDebugEnabled())
			log.debug("Query returns... ");
		
		String portaid = "";
		String pportkey = "";
		
		for ( Map<String, Object> row : result ) {
			portaid = (String)row.get("PPORT.port_aid");
			pportkey = (String)row.get("PPORT.key");
			alarm.setCustomFieldValue("pportkeyfromevc", pportkey);
		}	
		
		if ( portaid != null && ! portaid.isEmpty() ) {
			String component = alarm.getCustomFieldValue(GFPFields.COMPONENT);
			int start = 0;
			int modelIndex = component.indexOf("deviceModel");
			if ( modelIndex >= 0) {
				start = component.substring(modelIndex).indexOf(">");
				start = modelIndex + start + 1; 
			}
			String[] slotcardport = pportkey.split("/");
			String portinfo = " portAID=<" + portaid + "> Slot=<" + slotcardport[1] + "> Card=<" + slotcardport[2] +
					"> Port=<" + slotcardport[3] + ">";
			alarm.setCustomFieldValue(GFPFields.COMPONENT, component.substring(0, start) + portinfo + component.substring(start));
			String reason = alarm.getCustomFieldValue(GFPFields.REASON) + " EVCID=<" + alarm.getCustomFieldValue(GFPFields.EVC_NAME) + ">";
			alarm.setCustomFieldValue(GFPFields.REASON, reason);
		}
	}
	
	
	public void fetchRedundantNNIPorts(
			 EnrichedJuniperAlarm alarm) {

		if (log.isTraceEnabled())
			log.trace("fetchRedundantNNIPorts() Enter : ");
		List<String> redundencyPports = new ArrayList<String>();
		// go from the local device to the local local port using the lagid from the alarm to the remote port
		// traversing all the relations in between.
		String query = "START locDev=node:CE_Device(key = \""
			+ alarm.getOriginatingManagedEntity().split(" ")[1].split("/")[0]
			+ "\") MATCH (locDev)-[:Composed_Of_PPort]->(locPPort)" +
			// taking out where clause for performance
			//" WHERE locPPort.remote_device_type=\"JUNIPER MX SERIES\" " +
			"RETURN locPPort.TDL_instance_name,locPPort.remote_port_aid, locPPort.remote_device_type";
	
		if (log.isDebugEnabled())
			log.debug("##### CYPHER QUERY: " + query);  
  
		if(engine == null) 
			engine = GFPUtil.getCypherEngine();

		ExecutionResult result = engine.execute(query); 
		Set<String> remotePortAidSet = new HashSet<String>();
		 
		String redPortAid = "";
		for ( Map<String, Object> row : result ) {
			if(((String)row.get("locPPort.remote_device_type")).equals("JUNIPER MX SERIES")) {
				if(((String)row.get("locPPort.TDL_instance_name")) != null) {
					redundencyPports.add(((String)row.get("locPPort.TDL_instance_name"))); 
				}
				if(((String)row.get("locPPort.remote_port_aid")) != null) {
					remotePortAidSet.add((((String)row.get("locPPort.remote_port_aid")))); 
				}
			}
		}
		if(remotePortAidSet.size() > 1) {
			alarm.setRedundantNNIPorts(redundencyPports);
		}
		
		if (log.isDebugEnabled())
			log.debug("#### Remote NNI ports size :" + redundencyPports.size() + "####");
		
		if (log.isTraceEnabled())
			log.trace("fetchRedundantNNIPorts() Exit : ");
	}
	
	public void AreAnyEVCs(String pportInstance, EnrichedJuniperAlarm alarm) {
		
		if (log.isTraceEnabled())
			log.trace("AreAnyEVCs() Enter : Port is " + pportInstance);
	
		int count = 0;
	
		String query = "START pport=node:PE_PPort(key = \"" 
			+ pportInstance
			+ "\") MATCH (pport)-[:Composed_Of]-(PE_LPORT)-[:Composed_Of]-(EVCNode)"
			+ " WHERE PE_LPORT.class=\"IpagLPort\" AND EVCNode.class=\"IpagEvcNode\"" 
			+ " RETURN EVCNode.vrf_name";     
			
		if (log.isDebugEnabled())
			log.debug("##### CYPHER QUERY: " + query);

		if(engine == null)
			engine = GFPUtil.getCypherEngine();

		try {

			ExecutionResult result = engine.execute(query);

			// check each alarm to see if it fits in the time window
			// will be only one
			for ( Map<String, Object> row : result ) {
				if(((String)row.get("EVCNode.vrf_name")) != null && ((String)row.get("EVCNode.vrf_name")).length()>1) {

					String evc = (String)row.get("EVCNode.vrf_name");
					
					if (log.isDebugEnabled())
						log.debug("Found a related vrf: " + evc);
					
					alarm.addEvc(evc);
					count++;
				}			
			}
		} catch (Exception e) {
			log.error("Query has failed.");
			
			e.printStackTrace();
		}

		if (log.isTraceEnabled())
			log.trace("AreAnyEVCs() Exit : Number found=" + count);	
	}

}
