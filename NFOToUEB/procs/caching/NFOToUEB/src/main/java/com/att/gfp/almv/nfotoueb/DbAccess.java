package com.att.gfp.almv.nfotoueb;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbAccess implements Runnable {

	private Connection conn = null;
	private int avIfrStartSerialId;
	private int avIfrAsiStartSerialId;
	private int rowsRetLimit;
	private int sleepBetweenQueries;
	private PreparedStatement avIfrPS;
	private final Logger log = LoggerFactory.getLogger ( DbAccess.class );
	private BlockingQueue<JSONObject> dbQ;    
	private String AV_IFR_SELECT;
	private String AV_IFR_ASI_SELECT;
	private final String AV_IFR_TABLENAME = "av_ifr";
	private final String AV_IFR_ASI_TABLENAME = "av_ifr_asi";


	public DbAccess(int passedavIfrStartSerialId, int passedavIfrAsiStartSerialId, int passedrowsRetLimit, 
			int passedsleepBetweenQueries, BlockingQueue<JSONObject> passeddbQ) {
		this.avIfrStartSerialId = passedavIfrStartSerialId;
		this.avIfrAsiStartSerialId = passedavIfrAsiStartSerialId;
		this.rowsRetLimit = passedrowsRetLimit;
		this.sleepBetweenQueries = passedsleepBetweenQueries;
		this.dbQ = passeddbQ;

		AV_IFR_SELECT 	= "select first " + rowsRetLimit + " serial_id, failure_id, alarm_time_stamp, primary_time_stamp, layer, trim(customer)," +
				" trim(classification), severity, trim(domain), node, component, reason, sec_failure_id, cli_data, addn_info, clci, clli, clfi," +
				" has_secondary, has_cli, has_addn_info, info1, info2, info3 from " + AV_IFR_TABLENAME + " where serial_id > ?";

		AV_IFR_ASI_SELECT = "select first " + rowsRetLimit + " serial_id, failure_id, alarm_time_stamp, primary_time_stamp, layer, trim(customer)," +
				" trim(classification), severity, trim(domain), node, component, reason, sec_failure_id, cli_data, addn_info, clci, clli, clfi," +
				" has_secondary, has_cli, has_addn_info, info1, info2, info3 from " + AV_IFR_ASI_TABLENAME + " where serial_id > ?";


		String hostname = null;
		try {
			InetAddress addr = java.net.InetAddress.getLocalHost();
			hostname = addr.getHostName();
			if ( hostname.contains("gfpdst") ) {
				hostname = "gfpdst.oss.att.com";
			}
			else if (  hostname.contains("gfpdp1") ) {
				hostname = "gfpdp1.noc.att.com";
			}
			else if (  hostname.contains("gfpdp2") ) {
				hostname = "gfpdp2.noc.att.com";
			}

		} catch (UnknownHostException e) {
			log.error("connDb() UnknownHostException: " + e.toString(), e);
			e.printStackTrace();
			System.exit(1);
		} catch (Exception e) {
			log.error("connDb() Exception: "+ e.toString(), e);
			e.printStackTrace();
			System.exit(1);
		}

		final String INFXSRVRPRT= System.getenv("INFXSRVRPRT");
		final String INFORMIXSERVER = System.getenv("INFORMIXSERVER");
		final String SIC_AVDB = System.getenv("SIC_AVDB");

		final String dburl = "jdbc:informix-sqli://" + hostname + ":" + INFXSRVRPRT + "/" + SIC_AVDB + ":INFORMIXSERVER=" + INFORMIXSERVER;

		try {
			log.info("Connecting to DB using: " + dburl + "...");
			Class.forName("com.informix.jdbc.IfxDriver");
			conn = DriverManager.getConnection(dburl);
			log.info("Done");
		} catch(SQLException sqle) {
			log.error("connDb() SQLException = " + sqle.toString(), sqle);
			sqle.printStackTrace();
			disconnDb();
			System.exit(1);
		} catch(Exception e) {
			log.error("connDb() Exception = " + e.toString(), e);
			e.printStackTrace();
			disconnDb();
			System.exit(1);
		}

	}

	public void run() {

		while ( true ) {

			runQuery(AV_IFR_SELECT, avIfrStartSerialId);
			runQuery(AV_IFR_ASI_SELECT, avIfrAsiStartSerialId);

			try {
				Thread.sleep(sleepBetweenQueries);
			} catch (Exception e) {
				log.error("Error while sleeping, Exception = " + e.toString(), e);
			}

		}
	}

	private void disconnDb() {
		log.info("Disconnecting from DB...");
		if (conn != null) {
			try {
				conn.close();
				log.info("Done");
			} catch (SQLException e) {
				log.error("failed to disconnect from the database SQLException: ", e);
			}
			log.info("Done");
		}
	}

	private void runQuery(String SQL, int startSerialId) {

		ResultSet rs = null;

		try {

			log.info("Running SQL: " + SQL + " (startSerialId = " + startSerialId + ")");

			avIfrPS = conn.prepareStatement(SQL);
			avIfrPS.setInt(1, startSerialId);

			rs = avIfrPS.executeQuery();

			while(rs.next()) {
				JSONObject uebEvent = new JSONObject();
				startSerialId = rs.getInt(1);
				uebEvent.put("SerialId", rs.getString(1));
				uebEvent.put("AlertKey", rs.getString(2));
				uebEvent.put("TimeStamp", getCST(rs.getString(3)));
				uebEvent.put("SecondaryTimeStamp", getCST(rs.getString(4)));
				uebEvent.put("Layer", rs.getString(5));
				uebEvent.put("CustomerName", rs.getString(6));
				uebEvent.put("Classification", rs.getString(7));
				uebEvent.put("Severity", getSeverity(rs.getString(8)));
				uebEvent.put("Domain", rs.getString(9));
				uebEvent.put("NodeOrSwitchName", rs.getString(10));
				uebEvent.put("Component", rs.getString(11));
				uebEvent.put("Reason", rs.getString(12));
				uebEvent.put("SecondaryAlertKey", rs.getString(13));
				// CLI Data and AddnInfo are not required for GFP-Data to RUBY Interface over UEB
				//uebEvent.put("CLIData", rs.getString(14));
				//uebEvent.put("AddnInfo", rs.getString(15));
				uebEvent.put("CLCI", rs.getString(16));
				uebEvent.put("CLLI", rs.getString(17));
				uebEvent.put("CLFI", rs.getString(18));
				uebEvent.put("HasSecondary", mapHasData(rs.getString(19)));
				uebEvent.put("HasCLI", mapHasData(rs.getString(20)));
				uebEvent.put("HasAddnInfo", mapHasData(rs.getString(21)));
				uebEvent.put("Info1", rs.getString(22));
				uebEvent.put("Info2", rs.getString(23));
				uebEvent.put("Info3", rs.getString(24));
				uebEvent.put("TimeInSeconds", rs.getString(3));
				uebEvent.put("SecondarySourceDomain", "");
				uebEvent.put("MCN", "");
				uebEvent.put("VRF", "");
				uebEvent.put("FLAGS", "");
				uebEvent.put("EVCID", "");

				if ( dbQ.offer(uebEvent) ) {
					log.info("Added to DB Queue: \n" + uebEvent.toString() + ".");
				}
				else {
					log.error("Error inserting: DB queue is full");
				}
			}	
			if ( SQL.contains(AV_IFR_ASI_TABLENAME) ) {
				avIfrAsiStartSerialId = startSerialId;
				log.info("VCS Info Serial id for "+AV_IFR_ASI_TABLENAME+"="+startSerialId);

			}
			else { 
				avIfrStartSerialId = startSerialId; 
				log.info("VCS Info Serial id for "+AV_IFR_TABLENAME+"="+startSerialId);

			}

		} catch(SQLException sqle) {
			log.error("SQLException" + sqle.toString(), sqle);
		} catch(Exception e) {
			log.error("Exception" + e.toString(), e);
		} finally {
			try { rs.close(); } catch (Exception rsce) { log.error("rs.close() Exception = " + rsce.toString(), rsce); }
			try { avIfrPS.close(); } catch (Exception psce) { log.error("avIfrPS.close() Exception = " + psce.toString(), psce); }
		}

	}

	public static String getCST(String ctime)
	{

		long millis = (Long.parseLong(ctime)-3600)*1000;

		Date date = new Date(millis);
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.ENGLISH);
		sdf.setTimeZone(TimeZone.getTimeZone("EST"));
		String formattedDate = sdf.format(date)+" CST";
		return formattedDate;
	}

	private String getSeverity(String severity)     {
		String sev = "Warning" ;

		if (severity != null && severity.length() > 0) {
			if (severity.equals("0"))
				sev = "Critical" ;
			else if (severity.equals("1"))
				sev = "Major" ;
			else if (severity.equals("2"))
				sev = "Minor" ;
			else if (severity.equals("3"))
				sev = "Warning" ;
			else if (severity.equals("4"))
				sev = "Clear" ;

		}
		return sev;
	}

	private String mapHasData (String hasCli) {
		String retHasCli = "false";

		if ( hasCli.equals("1") ) {
			retHasCli = "true";
		}

		return retHasCli;
	}

}
