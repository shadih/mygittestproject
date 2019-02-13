package com.att.gfp.almv.nfotoueb;

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

public class NFOToUEB {

	private static final Logger log = LoggerFactory.getLogger ( NFOToUEB.class );
	private static JSONObject mainJson;
	private static final String configFile = "etc/NFOToUEBconfig.json";
	private final static String AV_IFR_TABLENAME = "av_ifr";
	private final static String AV_IFR_ASI_TABLENAME = "av_ifr_asi";

	private static int avIfrStartSerialId = 1;
	private static int avIfrAsiStartSerialId = 1;

	static ArrayList<CambriaBatchingPublisher> uebRubydataPub = new ArrayList<CambriaBatchingPublisher>();
	final static boolean withGzip = false;

	public static void main(String[] args) {

		log.info("Starting NFOToUEB...");

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
		// Find the serialId to start with	
		String location =  mainJson.optString("serialIdLocation","log");
		avIfrAsiStartSerialId = mainJson.optInt("avIfrAsiStartSerialId",1);
		avIfrStartSerialId = mainJson.optInt("avIfrStartSerialId",1);

		avIfrStartSerialId = avIfrStartSerialId - 1;
		avIfrAsiStartSerialId = avIfrAsiStartSerialId - 1;
		
		if (location.equals("log")){
			getSerialId();
		}


		int rowsRetLimit = mainJson.optInt("rowsRetLimit",10);
		int sleepBetweenQueries = mainJson.optInt("sleepBetweenQueries",5000);
		int dbQsize = mainJson.optInt("dbQsize",10000);
		int uebHealthAlarmFreq = mainJson.optInt("UEBHealthAlarmFrequency",900);

		String UEB_HOST1 = System.getenv("RUBYDATA_UEB_HOST1");
		String UEB_HOST2 = System.getenv("RUBYDATA_UEB_HOST2");
		String UEB_TOPIC = System.getenv("RUBYDATA_TOPIC");

		if ( null == UEB_HOST1 || UEB_HOST1.isEmpty() || null == UEB_TOPIC || UEB_TOPIC.isEmpty() ) {
			log.error("RUBYDATA_UEB_HOST1/RUBYDATA_TOPIC is null/empty. Exiting.");
			System.exit(1);
		}

		int UEB_batchSize = mainJson.optInt("UEB_batchSize",1024);
		int UEB_batchMaxAgeMs = mainJson.optInt("UEB_batchMaxAgeMs",1000);
		String UEB_compression = mainJson.optString("UEB_compression","true");
		String RUBYNFO_UEB_LOG_FILE = mainJson.optString("RUBYNFO_UEB_LOG_FILE","/gcfp/alarmvw/current/NFOToUEB/logs/RUBYNFO_UEB.log");


		if ((null != UEB_HOST2) && !(UEB_HOST2.isEmpty())){

			// We have two publishers
			log.info("Using serialIdLocation = "+location+", avIfrStartSerialId = " + avIfrStartSerialId + ", avIfrAsiStartSerialId = " + avIfrAsiStartSerialId +
					", rows return limit = " + rowsRetLimit + ", with sleep between queries = " + sleepBetweenQueries + 
					" msec and DB queue size = " + dbQsize + " UEB_HOST1 = " + UEB_HOST1 +" UEB_HOST2 = " + UEB_HOST2 + " UEB_TOPIC = " + UEB_TOPIC + 
					" UEB_batchSize = " + UEB_batchSize + " UEB_batchMaxAgeMs = " + UEB_batchMaxAgeMs + " UEB_compression = " + UEB_compression + 
					" UEBHealthAlarmFrequency = " + uebHealthAlarmFreq + ".");

			try {
				uebRubydataPub.add(
//						CambriaClientFactory.createBatchingPublisher(UEB_HOST1, UEB_TOPIC, 
//						UEB_batchSize, 
//						UEB_batchMaxAgeMs, 
//						Boolean.valueOf(UEB_compression))
						
						new CambriaClientBuilders.PublisherBuilder ()
						.usingHosts ( UEB_HOST1 )
						.onTopic ( UEB_TOPIC )
						.limitBatch ( UEB_batchSize, UEB_batchMaxAgeMs )
						.enableCompresion ( withGzip )
//						.authenticatedBy ( apiKey, apiSecret )
						.build ()
						);

				uebRubydataPub.add(
						/*CambriaClientFactory.createBatchingPublisher(UEB_HOST2, UEB_TOPIC, 
						UEB_batchSize, 
						UEB_batchMaxAgeMs, 
						Boolean.valueOf(UEB_compression))*/
						
						new CambriaClientBuilders.PublisherBuilder ()
						.usingHosts ( UEB_HOST2 )
						.onTopic ( UEB_TOPIC )
						.limitBatch ( UEB_batchSize, UEB_batchMaxAgeMs )
						.enableCompresion ( withGzip )
//						.authenticatedBy ( apiKey, apiSecret )
						.build ()
						);

			} catch (Exception e) {
				log.error("Exception Creating RUBYDATA UEB Publisher:" + e.toString(), e);
			}
		}
		else{
			// We have single publisher
			log.info("Using serialIdLocation = "+location+", avIfrStartSerialId = " + avIfrStartSerialId + ", avIfrAsiStartSerialId = " + avIfrAsiStartSerialId +
					", rows return limit = " + rowsRetLimit + ", with sleep between queries = " + sleepBetweenQueries + 
					" msec and DB queue size = " + dbQsize + " UEB_HOST1 = " + UEB_HOST1 + " UEB_TOPIC = " + UEB_TOPIC + " UEB_batchSize = " +
					UEB_batchSize + " UEB_batchMaxAgeMs = " + UEB_batchMaxAgeMs + " UEB_compression = " + UEB_compression + 
					" UEBHealthAlarmFrequency = " + uebHealthAlarmFreq + ".");

			try {
				uebRubydataPub.add(
//						CambriaClientFactory.createBatchingPublisher(UEB_HOST1, UEB_TOPIC, 
//						UEB_batchSize, 
//						UEB_batchMaxAgeMs, 
//						Boolean.valueOf(UEB_compression))
						
						new CambriaClientBuilders.PublisherBuilder ()
						.usingHosts ( UEB_HOST1 )
						.onTopic ( UEB_TOPIC )
						.limitBatch ( UEB_batchSize, UEB_batchMaxAgeMs )
						.enableCompresion ( withGzip )
//						.authenticatedBy ( apiKey, apiSecret )
						.build ()
						);

			} catch (Exception e) {
				log.error("Exception Creating RUBYDATA UEB Publisher:" + e.toString(), e);
			}
		}


		final ExecutorService executor = Executors.newFixedThreadPool(3);

		BlockingQueue<JSONObject> dbQ = new LinkedBlockingQueue<JSONObject>(dbQsize);

		log.info("Starting DbAccces Thread...  ");

		Thread DbAccessThrd = new Thread ( new DbAccess(avIfrStartSerialId, avIfrAsiStartSerialId, rowsRetLimit, sleepBetweenQueries, dbQ) );

		executor.execute(DbAccessThrd);

		log.info("Starting SendToUEB Thread...  ");

		Thread SendToUEBThrd = new Thread ( new SendToUEB(dbQ, uebRubydataPub, RUBYNFO_UEB_LOG_FILE) );

		executor.execute(SendToUEBThrd);

		UEBHealthAlarms hltUebAlarm = new UEBHealthAlarms(uebRubydataPub, UEB_TOPIC, RUBYNFO_UEB_LOG_FILE);
		ScheduledThreadPoolExecutor stpeUebHealth = new ScheduledThreadPoolExecutor(5);
		stpeUebHealth.scheduleAtFixedRate(hltUebAlarm, 60, uebHealthAlarmFreq, TimeUnit.SECONDS);
		log.info("Scheduler for UEB Health Alarms will start after " + 60 + " secs and repeat after " + uebHealthAlarmFreq + " secs");
	}

	private static void getSerialId() {
		// Check for last serialID info for each of the DB Tables in the logfile. Used during VCS failover or restart


		// Get the last serialid that was sent as recorded in the log4j logfile
		String file = System.getProperty("log_fl");
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(file));
			String line; 

			while (true) {
				line = br.readLine();
				if (null == line ) {
					break;
				}
				else if (! line.contains("VCS Info")|| line.isEmpty()) {
					continue;
				}
				else if ( line.contains("VCS Info") && line.contains(AV_IFR_ASI_TABLENAME+"=")){
					avIfrAsiStartSerialId = Integer.parseInt(line.split("=")[1]); 
				}
				else if ( line.contains("VCS Info") && line.contains(AV_IFR_TABLENAME+"=")){
					avIfrStartSerialId = Integer.parseInt(line.split("=")[1]); 
				}
			}
			br.close();

		} catch (IOException e) {
			log.error("Exception while reading logfile  = "+file+". " + e.toString(), e);
		}
	}


}
