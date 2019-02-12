package com.att.gfp.decomposed.decomposedScenario;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipag.decomposition.Decomposer;
import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;

import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public class Util 
{
	private static HashMap<String, HashSet<EnrichedAlarm>> alarmMap = new HashMap<String, HashSet<EnrichedAlarm>>();
	private static Logger log = LoggerFactory.getLogger(Util.class);

	public static void sendAlarm(EnrichedAlarm alarm) 
	{
	   try {
		String axml = alarm.toXMLString();
		axml = axml.replaceAll("\\n", " ");

		List<EnrichedAlarm> decomposedAlarms = null;
		try {
			decomposedAlarms = Decomposer.decompose(alarm);
		} catch (Exception e) {
			log.error("decompose() exception: ", e);
			e.printStackTrace();
			return;
		}
		String alarmid = alarm.getIdentifier();
		HashSet<EnrichedAlarm> alarmset = new HashSet<EnrichedAlarm>();
		alarmMap.put(alarmid, alarmset);
		log.info("Sending " + decomposedAlarms.size() + " decomposed alarms to the Alarm Manger:");
		for (EnrichedAlarm decomposedAlarm : decomposedAlarms)
		{
			alarmset.add(decomposedAlarm);
			String daxml = decomposedAlarm.toXMLString();
			daxml = daxml.replaceAll("\\n", " ");
			log.info("Decomposed alarm = " + daxml);

			GFPUtil.forwardOrCascadeAlarm(decomposedAlarm, AlarmDelegationType.FORWARD, null);
		}
		log.info("Sent successfully.");
	    } catch (Exception e) {
		log.error("Failed to send.", e);
	    }
	}

	public static void sendClear(String alarmid, String seq, boolean isGenByUCA, boolean isPurgeExp)
	{
	   try {
		HashSet<EnrichedAlarm> alarmset = alarmMap.get(alarmid);
		if (alarmset == null)
		{
			log.info("Cannot find the parent alarm: " + alarmid);
			return;
		}

		log.info("Sent decomposed clear alarms to the Alarm Manger:");
		GregorianCalendar gCalendar = new GregorianCalendar();
                gCalendar.setTime(new java.util.Date(System.currentTimeMillis()));    
                XMLGregorianCalendar alarmraisedtime = null; 

		Iterator i = alarmset.iterator();
		while(i.hasNext())
		{
			EnrichedAlarm ea = (EnrichedAlarm)i.next();
			try {
                        	alarmraisedtime = DatatypeFactory.newInstance().newXMLGregorianCalendar(gCalendar);
                        } catch (DatatypeConfigurationException e) {
                                e.printStackTrace(); 
                        } 
                        ea.setAlarmRaisedTime(alarmraisedtime);
                        ea.setCustomFieldValue(GFPFields.REASON, "CLEAR");
                        ea.setCustomFieldValue(GFPFields.FE_TIME_STAMP,String.valueOf(System.currentTimeMillis()/1000));

			// ea.setSeverity(4);
			ea.setPerceivedSeverity(PerceivedSeverity.CLEAR);
			ea.setCustomFieldValue("severity", "4");
			ea.setCustomFieldValue(GFPFields.LAST_CLEAR_TIME, ""+System.currentTimeMillis()/1000);
                     	ea.setCustomFieldValue(GFPFields.SEQNUMBER, seq);
			if (isGenByUCA)
				ea.setCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA, "YES"); 
			if (isPurgeExp)
				ea.setCustomFieldValue(GFPFields.IS_PURGE_INTERVAL_EXPIRED, "YES"); 
			GFPUtil.forwardOrCascadeAlarm(ea, AlarmDelegationType.FORWARD, null);
		}
		alarmMap.remove(alarmid);
		log.info("Sent successfully.");
	    } catch (Exception e) {
		log.error("Failed to send.", e);
	    }
	}
}
