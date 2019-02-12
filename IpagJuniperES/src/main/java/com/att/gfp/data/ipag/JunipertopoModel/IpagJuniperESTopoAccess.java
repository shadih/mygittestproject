package com.att.gfp.data.ipag.JunipertopoModel;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import juniperES.JuniperCompletion.PPort;

import org.apache.log4j.Logger;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

import com.att.gfp.data.ipagJuniperAlarm.EnrichedNTDAlarm;
import com.att.gfp.data.ipagJuniperAlarm.JuniperESEnrichedAlarm;
import com.att.gfp.helper.GFPUtil;
import com.hp.uca.expert.topology.TopoAccess;

/* topology access for ipag */
public class IpagJuniperESTopoAccess extends TopoAccess 
{
	
	private static IpagJuniperESTopoAccess topologyAccessor = null;
	private ExecutionEngine engine;

	private IpagJuniperESTopoAccess() {
		super();
		engine = GFPUtil.getCypherEngine();
	}

	public static synchronized IpagJuniperESTopoAccess getInstance() {
		if (topologyAccessor == null) {
			topologyAccessor = new IpagJuniperESTopoAccess();
		}
		return topologyAccessor;
	}

	
	
	private static final Logger log = Logger.getLogger ( IpagJuniperESTopoAccess.class );
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
	
	//run
	public void FetchCLFIinfo(EnrichedNTDAlarm alarm) {
		// access the CLFI nodes
		// here we have the alarm and the port CLFI.  From that we can get the other
		// variations of the CLFI, CLFI2, CLFI2Plus, CLFI3-LIST.

		if (log.isTraceEnabled())		
			log.trace("FetchCLFIinfo() Enter : ");

		try{
			String query = "START portClfi=node:CLFI(key = \""
					+ alarm.getPortCLFI()
					+ "\") RETURN portClfi.clfi2, portClfi.clfi2plus, portClfi.clfi3list";

			if (log.isDebugEnabled())
				log.debug("##### CYPHER QUERY: " + query);

			if(engine == null)
				engine = GFPUtil.getCypherEngine();

			ExecutionResult result = engine.execute(query);

			// only one should be returned
			for ( Map<String, Object> row : result ) {
				alarm.setPortCLFI2((String)row.get("portClfi.clfi2"));
				alarm.setPortCLFI2Plus((String)row.get("portClfi.clfi2plus"));
				alarm.setPortCLFI3List((String)row.get("portClfi.clfi3list"));
			}

			if (log.isDebugEnabled())
				log.debug("#### CLFIs: 2=" + alarm.getPortCLFI2() + " 2+=" + alarm.getPortCLFI2Plus() +
						" 3list=" + alarm.getPortCLFI3List() + "####");
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		if (log.isTraceEnabled())
			log.trace("FetchCLFIinfo() Exit : ");

		return;
	}

	public boolean CheckCLFI_list(String clfi_list, NTDticket ticket) {
		boolean ret = false;
		//["AALKDJF", "AIOUIUA", "U101/GE10/DTRTMIBL0AW/DTRTMIBL0BW", "QOIW878"]

		if (log.isTraceEnabled())
			log.trace("CheckCLFI_list() Enter : ");

		try {
			String query = "START n=node(0)  MATCH (n)-[:NTDTicket]-(ticket) " +
					" WHERE  ticket.clfi  in " + clfi_list +
					" RETURN  ticket.TicketNumber, ticket.clfi, ticket.NWPHostname";

//			String query = "START ticket=node(*)  WHERE ( HAS(ticket.class) and ticket.class=\"NTD\" and ticket.clfi= "+clfi_list+ ") " 
//					+ "RETURN ticket.TicketNumber, ticket.clfi, ticket.NWPHostname";

			if (log.isDebugEnabled())
				log.debug("##### CYPHER QUERY: " + query);

			if(engine == null)
				engine = GFPUtil.getCypherEngine();

			ExecutionResult result = engine.execute(query);

			// only one should be returned
			for ( Map<String, Object> row : result ) {
				ticket.setNumber((String)row.get("ticket.TicketNumber"));
				ticket.setCLFI((String)row.get("ticket.clfi"));
				ticket.setHostName((String)row.get("ticket.NWPHostname"));
			}

			if(ticket.getNumber() != null) {
				if (log.isDebugEnabled())
					log.debug("#### Ticket found:" + ticket.getNumber() + " host=" + ticket.getHostName() +
							" clfi=" + ticket.getCLFI() + "####");

				ret=true;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (log.isTraceEnabled())
			log.trace("CheckCLFI_list() Exit : ");

		return ret;
	}

	public boolean CheckCLFI(String clfi, NTDticket ticket) {
		boolean ret = false;

		if (log.isTraceEnabled())
			log.trace("CheckCLFI() Enter : ");

		String quoteCLFI = "\"" + clfi + "\"";

		String query = "START home=node(0)  MATCH (home)-[:NTDTicket]->(ticket) WHERE ticket.clfi = " + quoteCLFI +
				" RETURN ticket.NWPHostname, ticket.TicketNumber";
//		String query = "START ticket=node(*)  WHERE ( HAS(ticket.class) and ticket.class=\"NTD\" and ticket.clfi= "+quoteCLFI+ ") RETURN ticket.NWPHostname, ticket.TicketNumber";

		if (log.isDebugEnabled())
			log.debug("##### CYPHER QUERY: " + query);

		try { 
			if(engine == null)
				engine = GFPUtil.getCypherEngine();

			ExecutionResult result = engine.execute(query);

			// only one should be returned
			for ( Map<String, Object> row : result ) {
				ticket.setNumber((String)row.get("ticket.TicketNumber"));
				ticket.setCLFI(clfi);
				ticket.setHostName((String)row.get("ticket.NWPHostname"));
			}

			if(ticket.getNumber() != null) {
				log.trace("#### Ticket found:" + ticket.getNumber() + " host=" + ticket.getHostName() +
						" clfi=" + ticket.getCLFI() + "####");

				ret=true;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (log.isTraceEnabled())
			log.trace("CheckCLFI() Exit : ");

		return ret;
	}
	
	// Added tm1731
	public String[] FetchRoadminfo(String clfi) {
		// Get roadmClfiList that contains this alarm's clfi.
		// For those roadmclfilist get the roadmrootcauseClfiList

		if (log.isTraceEnabled())		
			log.trace("FetchRoadminfo() Enter : ");
		String rootCauseClfi = "";
		String aotsTktNum = "";
		
		try{
			String query = "START n=node(0)  MATCH (n)-[:ROADMTicket]-(ticket) WHERE  ticket.roadmClfiList =~ '.*"+clfi+".*'  RETURN ticket.roadmRootCauseClfiList, ticket.aotsTicketNum";

			if (log.isDebugEnabled())
				log.debug("##### CYPHER QUERY: " + query);

			if(engine == null)
				engine = GFPUtil.getCypherEngine();

			ExecutionResult result = engine.execute(query);

			// Multiple rows can be returned
			for ( Map<String, Object> row : result ) {
				rootCauseClfi = rootCauseClfi + (String)row.get("ticket.roadmRootCauseClfiList")+",";
				if ( ! ((String)row.get("ticket.aotsTicketNum")).isEmpty() )
				aotsTktNum = aotsTktNum + (String)row.get("ticket.aotsTicketNum")+",";
			}

			if (log.isDebugEnabled()) {
				log.debug("#### ROADM Root Cause CLFI list is:"  +rootCauseClfi+ "####");
				log.debug("#### ROADM AOTS Ticket Number is:"  +aotsTktNum+ "####");
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		if (log.isTraceEnabled())
			log.trace("FetchRoadminfo() Exit : ");

		String[] ret = {rootCauseClfi, aotsTktNum};
		return ret;
	}


/*	public void FetchLocalPPortLevelInformation(
			String pportInstance, EnrichedNTDAlarm alarm) {
		
		if (log.isTraceEnabled())
			log.trace("FetchLocalPPortLevelInformation() Enter : ");
		
		String type = alarm.getNodeType().toUpperCase();
		String query = "START pport=node:" + type + "_PPort(key = \""
				+ pportInstance
				+ "\") RETURN pport.clfi";
		
		if (log.isDebugEnabled())
			log.debug("##### CYPHER QUERY: " + query);

		if(engine == null)
			engine = GFPUtil.getCypherEngine();

		ExecutionResult result = engine.execute(query);

		// only one should be returned
		for ( Map<String, Object> row : result ) {
				alarm.setPortCLFI((String)row.get("pport.clfi"));
		} 

		if (log.isDebugEnabled())
			log.debug("#### PPORT:" + pportInstance + 
				" clfi :" + alarm.getPortCLFI() +
				 "####");
		
		if (log.isTraceEnabled())
			log.trace("FetchLocalPPortLevelInformation() Exit : ");	
	}*/
	
	public String AreAnyVFRs(String pportInstance, String alarmBeTimeStamp) {
		
		if (log.isTraceEnabled())
			log.trace("AreAnyVFRs() Enter : Port is " + pportInstance + " be-time-stamp " + alarmBeTimeStamp);
	
		String cdcInfo = null;
		String allcdcInfo = null;
		int numInfos = 0;
	
		String query = "START pport=node:PE_PPort(key = \"" 
			+ pportInstance
			+ "\") MATCH (pport)-[:Composed_Of]-(PE_LPORT)-[:Composed_Of]-(EVCNode)-[:Associated_To]-(EVC)"
			+ " WHERE PE_LPORT.class=\"IpagLPort\" AND EVCNode.class=\"IpagEvcNode\" AND EVC.class=\"IpagEvc\"" 
			+ "AND HAS (EVC.cdc_info)"
			+ " RETURN EVC.cdc_info";     
			
		if (log.isDebugEnabled())
			log.debug("##### CYPHER QUERY: " + query);

		if(engine == null)
			engine = GFPUtil.getCypherEngine();

		try {

			ExecutionResult result = engine.execute(query);

			// CDCInfo = "CFMAlertKey=<"+alert_id+"-IPAG01> CFMTimeStamp=<"+be_time_stamp+">"

			String strTime = null;
			long alarmTime;

			long now = Long.parseLong(alarmBeTimeStamp);

			// the time window is 3 minutes before now (alarm raised time)
			long beforeMax = now - (60 * 3);

			// check each alarm to see if it fits in the time window
			// will be only one
			for ( Map<String, Object> row : result ) {
				if(((String)row.get("EVC.cdc_info")) != null && ((String)row.get("EVC.cdc_info")).length()>1) {
					cdcInfo = (String)row.get("EVC.cdc_info");

					if (log.isDebugEnabled())
						log.debug("##### the CDC info is: " + cdcInfo);

					if(cdcInfo.contains("CFMTimeStamp")) {
						strTime = parseLabeledText(cdcInfo, "CFMTimeStamp"); 

						alarmTime = Long.parseLong(strTime);

						// for JUnit testing I set the alarm time to 0 in the topology
						if( (alarmTime <= now && alarmTime >= beforeMax) || alarmTime == 0) {
							if(numInfos == 0)
								allcdcInfo = cdcInfo;
							else 
								allcdcInfo = allcdcInfo + ", " + cdcInfo; 
						} 
						numInfos++;
					}
				}			
			}
		} catch (Exception e) {
			log.error("Query has failed.");
			
			e.printStackTrace();
		}

		if (log.isTraceEnabled())
			log.trace("AreAnyVFRs() Exit : " + cdcInfo);	
		
		return allcdcInfo;
	}

	private String parseLabeledText(String textStr, String label) {
		
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
	
	public String FetchRemoteAAFDArole(String pportInstance) {

		if (log.isTraceEnabled())
			log.trace("FetchRemoteAAFDArole() Enter : ");

		String aafdaRole = null;

		String query = "START pePPort=node:PE_PPort(key = \""
				+ pportInstance  
				+ "\")" 
				+ " MATCH (pePPort)" +
				"-[:PLink]-(PE_Port) RETURN PE_Port";
		if (log.isDebugEnabled())
			log.debug("##### CYPHER QUERY: " + query);

		if(engine == null)
			engine = GFPUtil.getCypherEngine();

		ExecutionResult result = engine.execute(query);
		Node node = null;
		Iterator<Node> columnRows = result.columnAs("PE_Port");
		while (columnRows.hasNext()) {
			node = columnRows.next();
			if (log.isTraceEnabled())
				log.trace("##### CYPHER QUERY: FetchRemoteAAFDArole query got results");

			// only one should be returned
			if(node.hasProperty("aafda_role")) {  
				if (log.isDebugEnabled())
					log.debug("##### CYPHER QUERY: FetchRemoteAAFDArole aafda_role exists");
				aafdaRole = ((String)node.getProperty("aafda_role"));
			}
		} 

		if(aafdaRole == null || aafdaRole.isEmpty())
			aafdaRole = "unknown";

		if (log.isDebugEnabled())
			log.debug("#### PPORT:" + pportInstance +  " aafda_role :" + aafdaRole);

		if (log.isTraceEnabled())
			log.trace("FetchRemoteAAFDArole() Exit : ");

		return aafdaRole;

	}  
	
	public String CheckForMatchingLPort(String vfrName, String deviceInstance) {
		
		if (log.isTraceEnabled())
			log.trace("CheckForMatchingLPort() Enter : ");
		
		String pport = null;

		String query = "Start device=node:PE_Device(key = \"" 
			+ deviceInstance
			+ "\") MATCH (device)-[:Composed_Of]-(slot)-[:Composed_Of]-(card)-[:Composed_Of]-(pport)-[:Composed_Of]-(lport)"
			+ " WHERE pport.class=\"IpagPPort\" AND lport.class=\"IpagLPort\" AND slot.class=\"IpagSlot\" AND card.class=\"IpagCard\" AND lport.vrf_name = \"" + vfrName 
			+ "\" RETURN DISTINCT pport.TDL_instance_name";     
			
		if (log.isDebugEnabled())
			log.debug("##### CYPHER QUERY: " + query);

		if(engine == null)
			engine = GFPUtil.getCypherEngine();

		ExecutionResult result = engine.execute(query);
	
		// only one should be returned
		for ( Map<String, Object> row : result ) {
			pport = ((String)row.get("pport.TDL_instance_name"));
		}

		if (log.isTraceEnabled())
			log.trace("CheckForMatchingLPort() Exit :   Port=" + pport);
	
		return pport;
	}
	
	public ArrayList<String> findAllLports(String pport) {
	
		if (log.isTraceEnabled())
			log.trace("findAllLports() Enter : ");

		ArrayList<String>  lports = new ArrayList<String> ();

		//if the scpservice of L = "AVPN" or "MIS" or "OEW" 
		String query = "Start pport=node:PE_PPort(key = \"" 
			+ pport
			+ "\") MATCH (pport)-[:Composed_Of]-(lport)"
			+ " WHERE lport.class=\"IpagLPort\" AND (lport.scp_service=\"AVPN\" OR lport.scp_service=\"MIS\" OR lport.scp_service=\"OEW\")"
			+ " RETURN lport.TDL_instance_name";     
			
		if (log.isDebugEnabled())
			log.debug("##### CYPHER QUERY: " + query);

		if(engine == null)
			engine = GFPUtil.getCypherEngine();

		ExecutionResult result = engine.execute(query);
	
		// could be a few returned
		for ( Map<String, Object> row : result ) {
			if (log.isDebugEnabled())
				log.debug("findAllLports():  LPort found: " + (String)row.get("lport.TDL_instance_name"));
			lports.add((String)row.get("lport.TDL_instance_name"));
		}

		if (log.isTraceEnabled())
			log.trace("findAllLports() Exit : ");
		
		return lports;		
	}

	public ArrayList<String> findAllLagPports(String device, String lagId) {
		if (log.isTraceEnabled())
			log.trace("findAllLagPports() Enter : ");

		ArrayList<String>  pports = new ArrayList<String> ();

		//if the scpservice of L = "AVPN" or "MIS" or "OEW" 
		String query = "Start device=node:PE_Device(key = \"" 
			+ device
			+ "\") MATCH (device)-[:Composed_Of]-(slot)-[:Composed_Of]-(card)-[:Composed_Of]-(pport)"
			+ " WHERE device.class=\"IpagDevice\" AND pport.class=\"IpagPPort\" AND slot.class=\"IpagSlot\" AND card.class=\"IpagCard\""
			+ " AND pport.port_lag_id=\"" + lagId 
			+ "\" RETURN pport.TDL_instance_name";     
			
		if (log.isDebugEnabled())
			log.debug("##### CYPHER QUERY: " + query);

		if(engine == null)
			engine = GFPUtil.getCypherEngine();

		ExecutionResult result = engine.execute(query);
	
		// could be a few returned
		for ( Map<String, Object> row : result ) {
			if (log.isDebugEnabled())
				log.debug("findAllLagPorts():  PPort found: " + (String)row.get("pport.TDL_instance_name"));

			pports.add((String)row.get("pport.TDL_instance_name"));
		}

		if (log.isTraceEnabled())
			log.trace("findAllLagPports() Exit : ");
		
		return pports;		
	}

	public ArrayList<PPort> findAllPports(String instance, String myClass) {
		if (log.isTraceEnabled())
			log.trace("findAllPports() Enter : ");

		ArrayList<PPort>  pports = new ArrayList<PPort> ();
		
		String cypherReturn = " RETURN pport.TDL_instance_name,  remPort.device_type, remPort.aafda_role, pport.port_aid, pport.port_num ";
		String cypherStart = null;
		String cypherMatch = null;
		String cypherWhere = null;
			
		// for this we only want info for the remote device type of CIENA NTE
		if(myClass.equals("CARD")) {
				cypherStart = "START " + myClass + "=node:PE_Card(key = \"" + instance + "\") ";
				cypherMatch = " MATCH (CARD)-[:Composed_Of]-(pport)-[:PLink]->(remPort)";
				cypherWhere = " WHERE CARD.class=\"IpagCard\" AND pport.class=\"IpagPPort\" AND remPort.device_type=\"CIENA NTE\"";
		} else {
			if(myClass.equals("SLOT")){
				cypherStart = "START " + myClass + "=node:PE_Slot(key = \"" + instance + "\") ";
				cypherMatch = " MATCH (SLOT)-[:Composed_Of]-(CARD)-[:Composed_Of]-(pport)-[:PLink]->(remPort)";
				cypherWhere = " WHERE SLOT.class=\"IpagSlot\" AND CARD.class=\"IpagCard\" AND pport.class=\"IpagPPort\"AND remPort.device_type=\"CIENA NTE\"";
				
			}
		}
			
		String query = cypherStart + cypherMatch + cypherWhere + cypherReturn;
		
		if (log.isDebugEnabled())
			log.debug("##### CYPHER QUERY: " + query);

		if(engine == null)
			engine = GFPUtil.getCypherEngine();

		ExecutionResult result = engine.execute(query);
	
		// could be a few returned
		for ( Map<String, Object> row : result ) {
			PPort myPPort = new PPort();
			myPPort.setName((String)row.get("pport.TDL_instance_name"));
			myPPort.setRemoteDeviceType((String)row.get("remPort.device_type"));
			myPPort.setAafda_role((String)row.get("remPort.aafda_role"));
			myPPort.setAid((String)row.get("pport.port_aid"));
			myPPort.setNum((String)row.get("pport.port_num"));
			
			if(myPPort.getAafda_role() == null || myPPort.getAafda_role().isEmpty()) 
				myPPort.setAafda_role("none");
			
			if (log.isDebugEnabled())
				log.debug("PPort: " + myPPort.getName() + ", remote device type: " + myPPort.getRemoteDeviceType() +
					", aafda role: " + myPPort.getAafda_role() + ", aid: " + myPPort.getAid() +
					", number: " + myPPort.getNum()); 
			
			pports.add(myPPort);
		}

		if (log.isTraceEnabled())
			log.trace("findAllPports() Exit : ");
		
		return pports;		
	}

	public String FetchDeviceForClass(String instance, String moClass) {
		if (log.isTraceEnabled())
			log.trace("FetchDeviceForPPort() Enter : ");
		
		String device = null;
		String query = null;
		
		if( moClass.equals("PPORT"))
			query = "Start pport=node:PE_PPort(key = \"" 
					+ instance
					+ "\") MATCH (pport)-[:Composed_Of]-(card)-[:Composed_Of]-(slot)-[:Composed_Of]-(device)"
					+ " WHERE pport.class=\"IpagPPort\" AND device.class=\"IpagDevice\" AND slot.class=\"IpagSlot\" AND card.class=\"IpagCard\"" 
					+ " RETURN device.TDL_instance_name";     
		else
			if(moClass.equals("CARD"))
				query = "Start card=node:PE_Card(key = \"" 
						+ instance
						+ "\") MATCH (card)-[:Composed_Of]-(slot)-[:Composed_Of]-(device)"
						+ " WHERE device.class=\"IpagDevice\" AND slot.class=\"IpagSlot\" AND card.class=\"IpagCard\"" 
						+ " RETURN device.TDL_instance_name"; 
			else
				query = "Start slot=node:PE_Slot(key = \"" 
						+ instance
						+ "\") MATCH (slot)-[:Composed_Of]-(device)"
						+ " WHERE slot.class=\"IpagSlot\" AND device.class=\"IpagDevice\"" 
						+ " RETURN device.TDL_instance_name";     

		if (log.isDebugEnabled())
			log.debug("##### CYPHER QUERY: " + query);

		if(engine == null)
			engine = GFPUtil.getCypherEngine();

		ExecutionResult result = engine.execute(query);
	
		// only one should be returned
		for ( Map<String, Object> row : result ) {
			device = ((String)row.get("device.TDL_instance_name"));
		}

		if (log.isTraceEnabled())
			log.trace("FetchDeviceForPPort() Exit : Device found: " + device);
	
		return device;
	}

	
	public void FetchPPortLevelInformation(
			String pportInstance, JuniperESEnrichedAlarm alarm) {
		
		if (log.isTraceEnabled())
			log.trace("FetchPPortLevelInformation() Enter : ");
		
		String query = "START pport=node:" + alarm.getNodeType() + "_PPort(key = \""
				+ pportInstance
				+ "\") RETURN pport.clfi, pport.clci, pport.bmp_clci";
		
		
		if (log.isDebugEnabled())
			log.debug("##### CYPHER QUERY: " + query);

		if(engine == null)
			engine = GFPUtil.getCypherEngine();

		ExecutionResult result = engine.execute(query);	

		// only one should be returned
		for ( Map<String, Object> row : result ) { 
			alarm.setCLFI((String)row.get("pport.clfi"));
			alarm.setCLCI((String)row.get("pport.clci"));
			alarm.setBmpCLCI((String)row.get("pport.bmp_clci"));
			alarm.setCircuitId((String)row.get("pport.clci"));
		}	

		if (log.isDebugEnabled())
			log.debug("#### PPORT:" + pportInstance + 
				" clfi :" + alarm.getCLFI() +
				" clci :" + alarm.getCLCI() +
				" bmp_clci :" + alarm.getBmpCLCI() +
				" circuit-id :" + alarm.getCircuitId() +
				 "####");
				
		if (log.isTraceEnabled())
			log.trace("FetchPPortLevelInformation() Exit : ");	
	}

	public void FetchLPortLevelInformation(
			String lportInstance, JuniperESEnrichedAlarm alarm) {
		
		if (log.isTraceEnabled())
			log.trace("FetchLPortLevelInformation() Enter : ");
		
		
		String query = "START lport=node:" + alarm.getNodeType() + "_LPort(key = \""
				+ lportInstance
				+ "\") RETURN lport.clfi, lport.clci, lport.bmp_clci";
		
		
		if (log.isDebugEnabled())
			log.debug("##### CYPHER QUERY: " + query);

		if(engine == null)
			engine = GFPUtil.getCypherEngine();

		ExecutionResult result = engine.execute(query);	

		// only one should be returned
		for ( Map<String, Object> row : result ) { 
				log.trace("## lport clfi: " + (String)row.get("lport.clfi"));
				log.trace("## lport clci: " + (String)row.get("lport.clci"));
				log.trace("## lport bmp_clci: " + (String)row.get("lport.bmp_clci"));
				alarm.setCLFI((String)row.get("lport.clfi"));
				alarm.setCLCI((String)row.get("lport.clci"));
				alarm.setBmpCLCI((String)row.get("lport.bmp_clci"));
				alarm.setCircuitId((String)row.get("lport.clci"));
		}	

		if (log.isDebugEnabled())
			log.debug("#### LPORT:" + lportInstance + 
				" clfi :" + alarm.getCLFI() +
				" clci :" + alarm.getCLCI() +
				" bmp_clci :" + alarm.getBmpCLCI() +
				" circuit-id :" + alarm.getCircuitId() +
				 "####");
		
		if (log.isTraceEnabled())
			log.trace("FetchLPortLevelInformation() Exit : ");	
	}
		
}
