package com.att.gfp.almv.cfotoueb;

import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.att.nsa.cambria.client.CambriaBatchingPublisher;
import com.att.nsa.cambria.client.CambriaClientBuilders;
import com.att.nsa.cambria.client.CambriaPublisher.message;


public class CFOToUEB {


	private static final Logger log = LoggerFactory.getLogger ( CFOToUEB.class );
	private static JSONObject mainJson;
	private static final String configFile = "etc/CFOToUEBconfig.json";
	private static int lineToStart = 1;

	static ArrayList<CambriaBatchingPublisher> uebRubyCCPub = new ArrayList<CambriaBatchingPublisher>();
	final static boolean withGzip = false;

	public static void main(String[] args) {

		log.info("Starting CFOToUEB...");

		File f = new File(configFile);

		if ( f.exists() ) {
			log.info("Reading " + configFile + "...  ");

			try {
				mainJson = new JSONObject(new JSONTokener(new FileReader(configFile)));
			}
			catch (JSONException je) {
				log.error("JSONException While Initializing JSONObject = " + je.toString(), je);
				System.exit(1);
			}
			catch (Exception e) {
				log.error("Exception While Initializing JSONObject = " + e.toString(), e);
				System.exit(1);
			}
			log.info("Done reading "+ configFile + ".");
		} else {
			log.error("Config file: " + configFile + " not found. Exiting.");
			System.exit(1);
		}



		lineToStart = mainJson.optInt("lineToStart",1);
		String location =  mainJson.optString("lineNoLocation","log");
		if (location.equals("log")){
			getLineNo();
		}
		int Qsize = mainJson.optInt("Qsize",10000);
		int sleepBetweenQueries = mainJson.optInt("sleepBetweenQueries",5000);


		String UEB_HOST1 = System.getenv("RUBYCC_UEB_HOST1");
		String UEB_HOST2 = System.getenv("RUBYCC_UEB_HOST2");

		String UEB_TOPIC = System.getenv("RUBYCC_TOPIC");

		if ( null == UEB_HOST1 || UEB_HOST1.isEmpty() || null == UEB_TOPIC || UEB_TOPIC.isEmpty() ) {
			log.error("RUBYCC_UEB_HOST1/RUBYCC_TOPIC is null/empty. Exiting.");
			System.exit(1);
		}

		int UEB_batchSize = mainJson.optInt("UEB_batchSize",1024);
		int UEB_batchMaxAgeMs = mainJson.optInt("UEB_batchMaxAgeMs",1000);
		String UEB_compression = mainJson.optString("UEB_compression","true");
		String RUBYCFO_SRC_DIR = mainJson.optString("RUBYCFO_SRC_DIR","/gcfp/www/current/docs");
		String RUBYCFO_UEB_LOG_FILE = mainJson.optString("RUBYCFO_UEB_LOG_FILE","/gcfp/alarmvw/current/CFOToUEB/logs/RUBYCFO_UEB.log");
		int uebHealthAlarmFreq = mainJson.optInt("UEBHealthAlarmFrequency",900);


		if ((null != UEB_HOST2) && !(UEB_HOST2.isEmpty())){
			
			// We have two publishers
			log.info("Using lineNoLocation = "+location+", lineToStart = " + lineToStart + 
					" and  Queue size = " + Qsize + " UEB_HOST1 = " + UEB_HOST1 +" UEB_HOST2 = " + UEB_HOST2 + " UEB_TOPIC = " + UEB_TOPIC + " UEB_batchSize = " +
					UEB_batchSize + " UEB_batchMaxAgeMs = " + UEB_batchMaxAgeMs + " UEB_compression = " + UEB_compression + 
					" UEBHealthAlarmFrequency = " + uebHealthAlarmFreq + ".");
			try {
				 uebRubyCCPub.add(
/*						 CambriaClientFactory.createBatchingPublisher(UEB_HOST1, UEB_TOPIC,
						UEB_batchSize,
						UEB_batchMaxAgeMs,
						Boolean.valueOf(UEB_compression))*/
						
						 new CambriaClientBuilders.PublisherBuilder ()
							.usingHosts ( UEB_HOST1 )
							.onTopic ( UEB_TOPIC )
							.limitBatch ( UEB_batchSize, UEB_batchMaxAgeMs )
							.enableCompresion ( withGzip )
//							.authenticatedBy ( apiKey, apiSecret )
							.build ()
						 );
				 
				 uebRubyCCPub.add(
/*						 CambriaClientFactory.createBatchingPublisher(UEB_HOST2, UEB_TOPIC,
							UEB_batchSize,
							UEB_batchMaxAgeMs,
							Boolean.valueOf(UEB_compression))*/
						 
						 new CambriaClientBuilders.PublisherBuilder ()
							.usingHosts ( UEB_HOST2 )
							.onTopic ( UEB_TOPIC )
							.limitBatch ( UEB_batchSize, UEB_batchMaxAgeMs )
							.enableCompresion ( withGzip )
//							.authenticatedBy ( apiKey, apiSecret )
							.build ()
						 );				 
				 
			} catch (Exception e) {
				log.error("Exception Creating RUBYCC UEB Publisher:" + e.toString(), e);
			}

			
		}
		else{
			// We have single publisher
			log.info("Using lineNoLocation = "+location+", lineToStart = " + lineToStart + 
					" and  Queue size = " + Qsize + " UEB_HOST1 = " + UEB_HOST1 + " UEB_TOPIC = " + UEB_TOPIC + " UEB_batchSize = " +
					UEB_batchSize + " UEB_batchMaxAgeMs = " + UEB_batchMaxAgeMs + " UEB_compression = " + UEB_compression + 
					" UEBHealthAlarmFrequency = " + uebHealthAlarmFreq + ".");
			try {
				 uebRubyCCPub.add(
/*						 CambriaClientFactory.createBatchingPublisher(UEB_HOST1, UEB_TOPIC,
						UEB_batchSize,
						UEB_batchMaxAgeMs,
						Boolean.valueOf(UEB_compression))*/
						
						 new CambriaClientBuilders.PublisherBuilder ()
							.usingHosts ( UEB_HOST1 )
							.onTopic ( UEB_TOPIC )
							.limitBatch ( UEB_batchSize, UEB_batchMaxAgeMs )
							.enableCompresion ( withGzip )
//							.authenticatedBy ( apiKey, apiSecret )
							.build ()
						 );		
				 
			} catch (Exception e) {
				log.error("Exception Creating RUBYCC UEB Publisher:" + e.toString(), e);
			}
		}


		BlockingQueue<JSONObject> dbQ = new LinkedBlockingQueue<JSONObject>(Qsize);
		final ExecutorService executor = Executors.newFixedThreadPool(2);

		// Start the thread that will read in todays file and process	 
		log.info("Starting ProcessFile Thread...  ");
		Thread ProcessFileThread = new Thread(new ProcessFile(lineToStart, dbQ, sleepBetweenQueries, RUBYCFO_SRC_DIR ));
		executor.execute(ProcessFileThread);

		// Start the thread that will send alarms to UEB	 
		log.info("Starting SendToUEB Thread...  ");

		Thread SendToUEBThrd = new Thread ( new SendToUEB(dbQ, uebRubyCCPub, RUBYCFO_UEB_LOG_FILE) );
		executor.execute(SendToUEBThrd);

		UEBHealthAlarms hltUebAlarm = new UEBHealthAlarms(uebRubyCCPub, UEB_TOPIC, RUBYCFO_UEB_LOG_FILE);
		ScheduledThreadPoolExecutor stpeUebHealth = new ScheduledThreadPoolExecutor(5);
		stpeUebHealth.scheduleAtFixedRate(hltUebAlarm, 60, uebHealthAlarmFreq, TimeUnit.SECONDS);
		log.info("Scheduler for UEB Health Alarms will start after " + 60 + " secs and repeat after " + uebHealthAlarmFreq + " secs");
	}
	private static void getLineNo() {
		// Check for last line number info in the logfile. Used during VCS failover or restart
		// Get the last line number that was sent as recorded in the log4j logfile. Send that again
		// That's where we start reading the file from.
		String file = System.getProperty("log_fl");
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(file));
			String line; 
			lineToStart = 0;
			while (true) {
				line = br.readLine();
				if (null == line ) {
					break;
				}
				else if (! line.contains("VCS Info")|| line.isEmpty()) {
					continue;
				}
				else if ( line.contains("VCS Info")) {
					lineToStart = Integer.parseInt(line.split("=")[1]) ; 
				}
			}
			br.close();
			lineToStart = lineToStart + 1;

		} catch (IOException e) {
			log.error("Exception while reading logfile  = "+file+". " + e.toString(), e);
		}
	}

}
