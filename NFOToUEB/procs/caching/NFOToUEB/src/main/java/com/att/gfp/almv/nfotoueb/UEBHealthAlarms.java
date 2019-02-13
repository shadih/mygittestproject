package com.att.gfp.almv.nfotoueb;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.att.nsa.cambria.client.CambriaBatchingPublisher;
import com.att.nsa.cambria.client.CambriaClientBuilders;
import com.att.nsa.cambria.client.CambriaPublisher.message;

class UEBHealthAlarms implements Runnable {

	private static final Logger log = LoggerFactory.getLogger ( UEBHealthAlarms.class );
	private static HashMap<String,String> RubyUEBAlarmFieldsMapping = new HashMap<String,String>();
	private ArrayList<CambriaBatchingPublisher> uebRubyPub = new ArrayList<CambriaBatchingPublisher>();
	private static BufferedWriter ruby_ueb_bw = null;
	private String UEB_TOPIC = "";

	UEBHealthAlarms(ArrayList<CambriaBatchingPublisher> passedUebRubyPub, String passedUEB_TOPIC, String RUBY_UEB_LOG_FILE) {

		log.info("UEBHealthAlarms Constructor; Creating RUBY HealthAlarms UEB Publisher");
		log.info("RUBY_LOG_FILE -> " + RUBY_UEB_LOG_FILE);	
		this.uebRubyPub = passedUebRubyPub;
		this.UEB_TOPIC = passedUEB_TOPIC;

		try {
			ruby_ueb_bw = getBWtoWrite(RUBY_UEB_LOG_FILE) ;

			RubyUEBAlarmFieldsMapping.put("AlertKey", "");
			RubyUEBAlarmFieldsMapping.put("TimeStamp", "");
			RubyUEBAlarmFieldsMapping.put("TimeInSeconds", "");
			RubyUEBAlarmFieldsMapping.put("Layer", "");
			RubyUEBAlarmFieldsMapping.put("CustomerName", "");
			RubyUEBAlarmFieldsMapping.put("Classification", "HEARTBEAT");					
			RubyUEBAlarmFieldsMapping.put("Severity", "");
			RubyUEBAlarmFieldsMapping.put("Domain", "");
			RubyUEBAlarmFieldsMapping.put("NodeOrSwitchName", "GFP-DATA_ALMV");
			RubyUEBAlarmFieldsMapping.put("Component", "");
			RubyUEBAlarmFieldsMapping.put("Reason", "");
			RubyUEBAlarmFieldsMapping.put("CLFI", "");
			RubyUEBAlarmFieldsMapping.put("CLCI", "");
			RubyUEBAlarmFieldsMapping.put("CLLI", "");
			RubyUEBAlarmFieldsMapping.put("HasCLI", "");
			RubyUEBAlarmFieldsMapping.put("HasAddnInfo", "");
			RubyUEBAlarmFieldsMapping.put("Info1", "");
			RubyUEBAlarmFieldsMapping.put("Info2", "");
			RubyUEBAlarmFieldsMapping.put("Info3", "");
			RubyUEBAlarmFieldsMapping.put("HasSecondary", "");
			RubyUEBAlarmFieldsMapping.put("SecondarySourceDomain", "");
			RubyUEBAlarmFieldsMapping.put("SecondaryTimeStamp", "");
			RubyUEBAlarmFieldsMapping.put("SecondaryAlertKey", "");
			RubyUEBAlarmFieldsMapping.put("MCN", "");
			RubyUEBAlarmFieldsMapping.put("VRF", "");
			RubyUEBAlarmFieldsMapping.put("FLAGS", "");
			RubyUEBAlarmFieldsMapping.put("EVCID", "");

		} catch (Exception e) {
			log.error("Exception Creating BufferedWriter / Initializinf Health Alarm Record:" + e.toString(), e);
		}
	}

	public void run() {

		log.info("Creating RUBY UEB HealthAlarms on topic: " + UEB_TOPIC);

		String be_time_stamp = Long.toString(System.currentTimeMillis() / 1000);

		JSONObject uebEvent = new JSONObject();

		try {

			for (Map.Entry<String, String> entry : RubyUEBAlarmFieldsMapping.entrySet()) {

				if ( entry.getKey().equals("TimeInSeconds") ){
					uebEvent.put(entry.getKey(),be_time_stamp);
				}
				else if (entry.getKey().equals("TimeStamp")) {
					uebEvent.put(entry.getKey(),DbAccess.getCST(be_time_stamp));
				}
				else if (entry.getKey().equals("AlertKey")) {
					uebEvent.put(entry.getKey(),DbAccess.getCST(be_time_stamp));
				}
				else if (entry.getKey().equals("Info1")) {
					uebEvent.put(entry.getKey(),UEB_TOPIC);
				}
				else {
					uebEvent.put(entry.getKey(),entry.getValue());
				}
			}

			publishUebEvent(uebEvent);

		}
		catch (Exception e) {
			log.error("Exception Attempting to create UEB Event for event with TimeStamp: " + be_time_stamp + 
					  " on topic: " + UEB_TOPIC + " Error = " + e.toString(), e);
		}
	}


	public void writeToFile(BufferedWriter bw, String txt)	throws Exception {

		SimpleDateFormat df = new SimpleDateFormat("MM/dd/yy HH:mm:ss z");
		String logTime = new String(df.format( new Date())  );

		txt = logTime + "|" + txt ;

		try {
			bw.write(txt) ;
			bw.newLine();
			bw.flush() ;
		} catch (Exception e) {
			throw e;
		}
	}

	public BufferedWriter getBWtoWrite(String actualFile) {
		BufferedWriter bufWriter = null;
		try {

			File fileName = new File(actualFile) ;
			if (! fileName.exists() )
				fileName.createNewFile() ;

			FileWriter fileWriter;
			fileWriter    = new FileWriter(fileName.getAbsoluteFile(), true) ;
			bufWriter = new BufferedWriter(fileWriter) ;
		}
		catch (Exception e) {
			log.error(" Error creating BufferedWriter for actualFile = " + actualFile + "Exception = " + e.toString(), e) ;
		}
		return bufWriter ;
	}

	private void publishUebEvent(JSONObject uebEvent) {

		try {
			for (CambriaBatchingPublisher pub : uebRubyPub){
				log.info("Publishing event with TimeStamp: " +  uebEvent.getString("TimeStamp") + " to UEB: " + uebEvent.toString());
				pub.send(uebEvent.getString("TimeStamp"), uebEvent.toString());

			}
			writeToFile(ruby_ueb_bw, uebEvent.toString());
		}
		catch (Exception e) {
			log.error("Exception Creating UEB Event for event with TimeStamp: " + uebEvent.getString("TimeStamp") + " Exception = " + e.toString(), e);
		}
	}

}

