package com.att.gfp.gfpdata.gfpdataTaCdmTailEnvAdp;

import java.io.File;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.json.JSONObject;
import org.json.XML;
import org.apache.commons.io.input.TailerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.att.nsa.cambria.client.CambriaClientFactory;
import com.att.nsa.cambria.client.CambriaPublisher;

public class TailEnvAdpNPub {

	private static final int SLEEP = 500;
	private static File file = null;
	private static final Logger log = LoggerFactory.getLogger ( TailEnvAdpNPub.class );
	private static final CambriaPublisher uebPub = CambriaClientFactory.createBatchingPublisher(
			"uebsb91kcdc.it.att.com,uebsb92kcdc.it.att.com,uebsb93kcdc.it.att.com", 
			"SH1986-TEST-TOPIC-IN", 1024, 10000, false);

	public static void main(String[] args) throws Exception {
		try {
			if ( null == args[0] || args[0].isEmpty() ) {
				log.error("No input file name argument specified. Existing.");
				System.exit(1);
			}
		} catch (Exception e) {
			log.error("Error while reading argument file name. Exception = " + e.toString(), e);
			System.exit(1);
		}
		file = new File(args[0]);
		log.info("Starting TailEnvAdpNPub; Looking for [<alarm>...</alarm>] messages in: "  + args[0] + " and publishing them to UEB...");
		TailEnvAdpNPub tF = new TailEnvAdpNPub();
		tF.run();
	}

	private void run() throws InterruptedException {
		TailerListener listener = new MyTailerListener();
		Tailer tailer = Tailer.create(file, listener, SLEEP, true);
		while (true) {
			Thread.sleep(SLEEP);
		}
	}

	public class MyTailerListener extends TailerListenerAdapter {
		@Override
		public void handle(String line) {
			if ( line.contains("send: ") ) {
				log.info("Received: " + line);
				JSONObject xmlToJson = XML.toJSONObject(line.substring(line.indexOf("[<alarm>")+8, line.lastIndexOf("</alarm>]")));
				try {
					log.info("Publishing to UEB event: " + xmlToJson.toString());
					uebPub.send(xmlToJson.getString("SeqNumber"), xmlToJson.toString());
				} catch (Exception e) {
					log.error("Exception publishing to UEB Event with SeqNumber: " + xmlToJson.getString("SeqNumber") + " Error = " + e.toString(), e);
				}				
			}
		}
	}
}

