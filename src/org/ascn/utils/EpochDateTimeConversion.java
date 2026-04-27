package org.ascn.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EpochDateTimeConversion {

	public String convertToDate(String inEpoch) {

		Date date = new Date(Long.parseLong(inEpoch));
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String myDate = format.format(date);

		return myDate;
	}
	public long convertToEpoch(String inDate) throws ParseException {

		String myDate = inDate;
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		Date date = dateFormat.parse(inDate);
		long epoch = date.getTime();
		System.out.println(epoch);
		return epoch;
	}

}
