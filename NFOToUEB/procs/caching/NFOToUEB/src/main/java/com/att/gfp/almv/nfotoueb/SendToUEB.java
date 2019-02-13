package com.att.gfp.almv.nfotoueb;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.BlockingQueue;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.nsa.cambria.client.CambriaBatchingPublisher;
import com.att.nsa.cambria.client.CambriaClientBuilders;
import com.att.nsa.cambria.client.CambriaPublisher.message;


public class SendToUEB implements Runnable {

	private static final Logger log = LoggerFactory.getLogger ( SendToUEB.class );
	BlockingQueue<JSONObject> dbQ;
	ArrayList<CambriaBatchingPublisher> uebRubydataPub = new ArrayList<CambriaBatchingPublisher>();
	public static BufferedWriter rubydata_ueb_bw = null;

	public SendToUEB(BlockingQueue<JSONObject> passeddbQ, ArrayList<CambriaBatchingPublisher> passedUebRubydataPub, String RUBYNFO_UEB_LOG_FILE) {

		log.info("SendToUEB Constructor, Reading dbQ.");
		this.dbQ = passeddbQ;
		this.uebRubydataPub = passedUebRubydataPub;
		try {
		rubydata_ueb_bw = getBWtoWrite(RUBYNFO_UEB_LOG_FILE) ;
		} catch (Exception e) {
			log.error("Exception Creating RUBYDATA Buffered Writer:" + e.toString(), e);
		}

	}

	public void run() {

		log.info("Reading alarms from dbQ.");
		JSONObject alarm = null;

		while ( true ) {
			try {
				alarm = dbQ.take();
				log.info("Processing alarm: " + alarm.toString());
				publishUebEvent(alarm);
			} 
			catch (Exception e) {
				log.error("Exception processing alarm: " + alarm.toString(), e);
			}
		}
	}

	private void publishUebEvent(JSONObject uebEvent) {

		try {
			for (CambriaBatchingPublisher pub : uebRubydataPub){
				log.info("Publishing event with SerialId: " +  uebEvent.getString("SerialId") + " to UEB: " + uebEvent.toString());
				pub.send(uebEvent.getString("SerialId"), uebEvent.toString());

			}
			writeToFile(rubydata_ueb_bw, uebEvent.toString());
		}
		catch (Exception e) {
			log.error("Exception Creating UEB Event for event with SerialId: " + uebEvent.getString("SerialId") + " Exception = " + e.toString(), e);
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

}
