package com.att.gfp.almv.cfotoueb;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONObject;


public class ProcessFile  extends Object implements Runnable {
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd", Locale.ENGLISH);
	private int lineToStart;
	private int sleepBetweenQueries;
	private String RUBYCFO_SRC_DIR;
	private BlockingQueue<JSONObject> Q;
	private final Logger log = LoggerFactory.getLogger ( ProcessFile.class );


	public ProcessFile(int lineToStart, BlockingQueue<JSONObject> passedQ, int sleepBetweenQueries, String RUBYCFO_SRC_DIR) {
		this.lineToStart = lineToStart;
		this.Q = passedQ;
		this.sleepBetweenQueries = sleepBetweenQueries;
		this.RUBYCFO_SRC_DIR = RUBYCFO_SRC_DIR;
		log.info("ProcessFile Constructor finished.");

	}
	public void run() {
		BufferedReader br = null;
		while (true) {

			// Get today's file
			long today = System.currentTimeMillis();
			Date date = new Date(today);
			String todayDate = sdf.format(date);
			String fName = RUBYCFO_SRC_DIR +"/RUBYCFO." + todayDate + ".log";	
			log.info("Starting Processing of file "+fName +" with line # "+lineToStart);

			try {
				br = new BufferedReader(new FileReader(fName));


				// Skip to line # requested
				int cnt = 1;
				while ( cnt  < lineToStart) {

					br.readLine();
					cnt++;

				}
				// Start reading the rest of the file & process it
				lineToStart = 0;
				readFromFile(br, sleepBetweenQueries, cnt);
				br.close();
				Thread.sleep (60000);
			}  

			catch (IOException  e) {
				log.error("File Not found" + fName+ ". Exception " + e.toString(), e);
				System.exit(1);

			}
			catch ( InterruptedException e) {
				log.error("InterruptedException" + e.toString(), e);

			}
		} 
	}
	private void readFromFile(BufferedReader br, int sleepBetweenQueries, int cnt) throws InterruptedException {

		while (true){
			String line = null;
			try {
				line = br.readLine();
			} catch (IOException e) {
				log.error("Problem reading BufferedReader. IOException" + e.toString(), e);

			}

			if (null == line ) {
				Thread.sleep(sleepBetweenQueries);
			}
			else if (line.isEmpty()) {
				//wait until there is more of the file for us to read
				Thread.sleep(sleepBetweenQueries);
				cnt++;
			}

			else if ( line.startsWith("FIRSTLINE") ){
				// skip this line do nothing
				cnt++;
			}
			else if ( line.startsWith("LASTLINE")){
				// Reached the end of file move on to Next file
				log.info("Reached the end of file move on to next file.  "+line);
				log.info("VCS Info Line Number=0");

				Thread.sleep(1000);
				break; 
			}
			else {
				//do something interesting with the line
				createJsonEvent(line, cnt);
				cnt++;

			}

		}
	}
	private void createJsonEvent(String line, int cnt) {
		String CustomerName = "";
		String Classification = "";
		String Severity = "";
		String Domain = "";
		String NodeOrSwitchName = "";
		String TimeStamp = "";
		String Component = "";
		String Reason = "";
		String ticketNumber = "";
		String MCN = "";
		String circiutID = "";
		String TimeInSeconds = "";

		// line is as follows:
		// Alert text || Ticket Number || MCN || Circuit ID 
		// Alert text is as follows
		// Customer Name:Classification:Severity:Domain:Node dd/mm/yyyy hh:mm:ss TZ Alert Component:Reason

		// For ALMV; following fields are currently always null (in production) and they will continue to be null 
		//when inserting ALMV CFO alarms to UEB
		// VRF,  FLAGS, EVCID,  INFO

		try {
			String[] data = line.split(":");
			CustomerName = data[0];
			Classification = data[1];
			Severity = data[2];
			Domain = data[3];
			String[]  tmp = data[4].split(" ");
			NodeOrSwitchName = tmp[0];
			String[] tmp2 = data[6].split(" ");
			TimeStamp = tmp[1]+" "+tmp[2]+":"+data[5]+":"+tmp2[0]+" "+tmp2[1];
			for (int i=3; i < tmp2.length; i++ ){
				Component = Component + tmp2[i] +" ";
			}
			String[] tmp3 = data[7].split("\\|\\|");
			Reason = tmp3[0];
			ticketNumber = tmp3[1];
			MCN = tmp3[2];
			circiutID = tmp3[3];
			TimeInSeconds = getEpochTime(TimeStamp);
		}
		catch (Exception e){
			log.error("Dropping this alarm. Error parsing line "+line+"\nException" + e.toString(), e);
			return;

		}

		JSONObject uebEvent = new JSONObject();
		uebEvent.put("AlertKey", ticketNumber.trim());
		uebEvent.put("TimeStamp", TimeStamp.trim());
		uebEvent.put("SecondaryTimeStamp", "");
		uebEvent.put("Layer", "");
		uebEvent.put("CustomerName", CustomerName.trim());
		uebEvent.put("Classification", Classification.trim());
		uebEvent.put("Severity", Severity.trim());
		uebEvent.put("Domain", Domain.trim());
		uebEvent.put("NodeOrSwitchName", NodeOrSwitchName.trim());
		uebEvent.put("Component", Component.trim());
		uebEvent.put("Reason", Reason.trim());
		uebEvent.put("SecondaryAlertKey", "");
		uebEvent.put("CLCI", circiutID.trim());
		uebEvent.put("CLLI", "");
		uebEvent.put("CLFI","");
		uebEvent.put("HasSecondary","");
		uebEvent.put("HasCLI", "");
		uebEvent.put("HasAddnInfo", "");
		uebEvent.put("Info1", "");
		uebEvent.put("Info2","");
		uebEvent.put("Info3","");
		uebEvent.put("TimeInSeconds",TimeInSeconds);  
		uebEvent.put("SecondarySourceDomain", "");
		uebEvent.put("MCN", MCN.trim());
		uebEvent.put("VRF", "");
		uebEvent.put("FLAGS", "");
		uebEvent.put("EVCID", "");

		if ( Q.offer(uebEvent) ) {
			log.info("Added to  Queue: \n" + uebEvent.toString() + ".");
			log.info("VCS Info Line Number="+cnt);

		}
		else {
			log.error("Error inserting: DB queue is full");
		}

	}
	private String getEpochTime(String time) {
		SimpleDateFormat df = new SimpleDateFormat("MM/dd/yy HH:mm:ss z");
		Date date = null;
		try {
			date = df.parse(time);
		} catch (ParseException e) {
			log.error("Error converting date to epoch" + time +"\nException" + e.toString(), e);

		}
		String epoch =  Long.toString(date.getTime()/1000);
		return epoch;

	}

}
