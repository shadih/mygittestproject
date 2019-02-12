package com.att.gfp.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.uca.expert.alarm.Alarm;

public class AlarmFileForwardingUtil {
	private static Logger log = LoggerFactory.getLogger(AlarmFileForwardingUtil.class);
	public static void writeToLog(Alarm alarm) {
		log.trace(alarm.toXMLString()); 
	}
}
