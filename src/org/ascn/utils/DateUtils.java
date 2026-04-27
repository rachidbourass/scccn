package org.ascn.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import sailpoint.object.Link;
import sailpoint.tools.GeneralException;

public class DateUtils {

	private static String adExpAttrName = "accountExpires";
	private static Pattern DATE_PATTERN = Pattern
			.compile("^((2000|2400|2800|(19|2[0-9])(0[48]|[2468][048]|[13579][26]))-02-29)$"
					+ "|^(((19|2[0-9])[0-9]{2})-02-(0[1-9]|1[0-9]|2[0-8]))$"
					+ "|^(((19|2[0-9])[0-9]{2})-(0[13578]|10|12)-(0[1-9]|[12][0-9]|3[01]))$"
					+ "|^(((19|2[0-9])[0-9]{2})-(0[469]|11)-(0[1-9]|[12][0-9]|30))$");
	private static String ZDT_DATE_FORMAT  = "MM/dd/yyyy hh:mm:ss a z";
	private static String DATE_FORMAT = "MM/dd/yyyy";
	private static String DATE_TIME_FORMAT  = "MM/dd/yyyy HH:mm:ss";

	public DateUtils() {
	}

	/**
	 * Date Match given string date
	 * @param date, String
	 * @return boolean
	 */
	public static boolean stringDateMatches(String date) {
		return DATE_PATTERN.matcher(date).matches();
	}

	/**
	 * daysBetween returns number of days between today and given date
	 * @param expDate, String
	 * @return long
	 */
	public static long daysBetween(String expDate) {
		long daysBetween = 0;
		boolean isValidDateFormat = false;
		if(expDate.length() == 10) {
			daysBetween = ChronoUnit.DAYS.between(getLocalDate(null), getLocalDate(expDate));
			isValidDateFormat = true;
		}else if(expDate.length() == 26) {
			daysBetween = ChronoUnit.DAYS.between(getZonedDateTime(null), getZonedDateTime(expDate));
			isValidDateFormat = true;
		}else if(expDate.length() == 19) {
			//calculate diff
			daysBetween = ChronoUnit.DAYS.between(getLocalDateTime(null), getLocalDateTime(expDate));
			isValidDateFormat = true;
		}

		return daysBetween;
	}

	/**
	 * convertDate converts String to an Object of Date
	 * @param expDate, String
	 * @return Object
	 */
	public static Object convertDate(String expDate) {
		Object returnObj = null;
		if(expDate.length() == 10) {
			returnObj = getLocalDate(expDate);
		}else if(expDate.length() == 26) {
			returnObj = getZonedDateTime(expDate).toLocalDate();
		}else if(expDate.length() == 19) {
			returnObj = getLocalDateTime(expDate).toLocalDate();
		}else {
			throw new RuntimeException("ERROR: DateUtils.convertDate(String expDate): unconvertable date found");
		}

		return returnObj;
	}
	
	
	/**
	 * getLocalDate
	 * @param inDate, String
	 * @return LocalDate
	 */
	private static LocalDate getLocalDate(String inDate) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
		LocalDate date = null;
		if(null != inDate) {
			date = LocalDate.parse(inDate, formatter);
		}else {
			String dtStr = formatter.format( LocalDateTime.now() );
			date = LocalDate.parse( dtStr, formatter );
		}
		return date;
	}

	/**
	 * getLocalDateTime
	 * @param inDate String
	 * @return LocalDateTime
	 */
	private static LocalDateTime getLocalDateTime(String inDate) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
		LocalDateTime date = null;
		if(null != inDate) {
			date = LocalDateTime.parse(inDate, formatter);
		}else {
			String dtStr = formatter.format( LocalDateTime.now() );
			date = LocalDateTime.parse( dtStr, formatter );
		}
		return date;
	}

	/**
	 * getZonedDateTime
	 * @param inDate, String
	 * @return ZonedDateTime
	 */
	private static ZonedDateTime getZonedDateTime(String inDate) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(ZDT_DATE_FORMAT);
		ZonedDateTime date = null;
		if(null != inDate) {
			date = ZonedDateTime.parse(inDate, formatter);
		}else {
			String zdtStr = formatter.format( ZonedDateTime.now() );
			date = ZonedDateTime.parse( zdtStr, formatter );
		}
		return date;
	}

	/**
	 * daysBetweenDateTime
	 * @param expDate, String
	 * @return long
	 */
	public static long daysBetweenDateTime(String expDate) {
		LocalDateTime nowDate = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a z");
		LocalDateTime dateTime = null;
		long daysBetween = 0;
		if(expDate != null) {
			if (stringDateMatches(expDate)) {
				dateTime = LocalDateTime.parse(expDate, formatter);
				daysBetween = ChronoUnit.DAYS.between(nowDate, dateTime);
			}
		}
		return daysBetween;
	}

	/**
	 * daysBetween
	 * @param expDate, LocalDate
	 * @return long
	 */
	public static long daysBetween(LocalDate expDate) {
		LocalDate nowDate = LocalDate.now();

		long daysBetween = ChronoUnit.DAYS.between(nowDate, expDate);
		return daysBetween;
	}

	/**
	 * getCurrentDate
	 * @return LocalDate
	 */
	public static LocalDate getCurrentDate() {
		LocalDate date = LocalDate.now();
		return date;
	}

	/**
	 * daysBetween
	 * @param expDate Date
	 * @return long
	 */
	public static long daysBetween(Date expDate) {
		LocalDate nowDate = LocalDate.now();
		LocalDate inDate = (expDate).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		long daysBetween = ChronoUnit.DAYS.between(nowDate, inDate);
		return daysBetween;
	}

	/**
	 * getDays
	 * @return Date
	 */
	public static Date getDays() {

		Long l = 1346524199000l;
		Date d = new Date(l);
		System.out.println(d);

		Date earlier = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(90));

		return earlier;
	}

	// This methods returns days to compare between today and epoch date returned by
	// SailPoint
	// Pass number of days past you need to evaluate e.g., 90, 50, 60 etc
	// to getDateBetween(n) method
	// return value will be a integer, your if condition should check for > 0 and <
	// max days you provided.
	public static int getDateBetween(int numDays, long inEpochDate) throws ParseException {
		int diff = 0;
		LocalDate today = LocalDate.now();
		LocalDate pastDate = today.minusDays(numDays);
		Long l = inEpochDate;
		LocalDate ld = Instant.ofEpochMilli(l).atZone(ZoneId.systemDefault()).toLocalDate();
		System.out.println("Converted Date: " + ld);
		if (ld.isAfter(pastDate)) {
			diff = ld.compareTo(today);
			System.out.println("ONLY PAST 90 Days or Less: " + diff);
		} else {
			System.out.println("ONLY FORWARD 90 Days or Less: " + diff);
		}
		return diff;
	}

	/**
	 * checkADExpDateMatch
	 * @param link sailpoint.Object.Link
	 * @param rDays, int
	 * @return boolean
	 * @throws GeneralException
	 * @throws ParseException
	 */
	public static boolean checkADExpDateMatch(Link link, int rDays) throws GeneralException, ParseException {
		boolean readyForExpiration = false;
		Object adExpDateValueObj = link.getAttribute(adExpAttrName);
		if (null != adExpDateValueObj) {
			List<String> possibleValues = new ArrayList<>();
			possibleValues.add(null);
			possibleValues.add("");
			possibleValues.add(" ");
			possibleValues.add("never");
			if (adExpDateValueObj instanceof String) {
				if (!possibleValues.contains(adExpDateValueObj)) {
					long diff = daysBetween(adExpDateValueObj.toString());
					if (diff > 0 && diff <= rDays) {
						readyForExpiration = true;
					}
				}
			}
			if (adExpDateValueObj instanceof Date) {
				long diff = daysBetween((Date) adExpDateValueObj);
				if (diff > 0 && diff <= rDays) {
					readyForExpiration = true;
				}
			}
		}
		return readyForExpiration;
	}

	/**
	 * checkADExpDateMatch
	 * @param links Link
	 * @param rDays, Int
	 * @return List<Boolean>
	 * @throws GeneralException
	 * @throws ParseException
	 */
	public static List<Boolean> checkADExpDateMatch(List<Link> links, int rDays) throws GeneralException, ParseException {
		boolean readyForExpiration = false;
		List<Boolean> exists = new ArrayList<>();
		if (!links.isEmpty() && links.size() > 0) {
			exists.add(readyForExpiration);
			for (Link l : links) {
				Object adExpDateValueObj = l.getAttribute(adExpAttrName);
				if (null != adExpDateValueObj) {
					if (adExpDateValueObj instanceof String) {
						if (!adExpDateValueObj.toString().equalsIgnoreCase("never")) {
							long diff = daysBetween(adExpDateValueObj.toString());
							if (diff > 0 && diff <= rDays) {
								exists.add(true);
							}
						}
					}
					if (adExpDateValueObj instanceof Date) {
						long diff = daysBetween((Date) adExpDateValueObj);
						if (diff > 0 && diff <= rDays) {
							exists.add(true);
						}
					}
				}
			}
		}
		return exists;
	}

	/**
	 * Validate given EndDate field as per below conditions if given field is past
	 * date or more than 12 months from today throw error message
	 *
	 * @param value - End Date from Form field
	 * @return messages - Error message
	 */

	public static String validateEndDate(Date value, int maxDays, int minDays) {
		String messages = null;
		long numDays = daysBetween(value);
		if (numDays < minDays) {
			messages = "Date cannot be today or in the past: " + numDays;
		}
		if (numDays > maxDays) {
			messages = "Date cannot be more than 366 days from today: " + numDays;
		}
		return messages;
	}

	/**
	 * validateEndDate
	 * @param value Date
	 * @return String
	 */
	public static String validateEndDate(Date value) {
		String messages = null;
		long numDays = daysBetween(value);
		if (numDays < 0) {
			messages = "Date cannot be today or in the past: " + numDays;
		}
		if (numDays > 367) {
			messages = "Date cannot be more than 366 days from today: " + numDays;
		}

		return messages = messages == null ? "" : messages;
	}

	/**
	 * getEpochDateBetweenTodayAndPastDays
	 * @param numDays, int
	 * @param inEpochDate, long
	 * @return int
	 * @throws ParseException
	 */
	public static int getEpochDateBetweenTodayAndPastDays(int numDays, long inEpochDate) throws ParseException {
		int diff = 0;
		LocalDate today = LocalDate.now();
		LocalDate pastDate = today.minusDays(numDays);
		Long l = inEpochDate;
		LocalDate ld = Instant.ofEpochMilli(l).atZone(ZoneId.systemDefault()).toLocalDate();
		System.out.println("Converted Date: " + ld);
		if (ld.isAfter(pastDate)) {
			diff = ld.compareTo(today);

		} else {

		}
		return diff;
	}

	/**
	 * convertEpochToLocalDateTime
	 * @param inEpoch, String
	 * @return String
	 */
	public static String convertEpochToLocalDateTime(String inEpoch) {
		long epochTimeMillis = Long.parseLong(inEpoch + "L");
		Instant instant = Instant.ofEpochMilli(epochTimeMillis);
		ZoneId zoneId = ZoneId.systemDefault();
		LocalDateTime localDateTime = instant.atZone(zoneId).toLocalDateTime();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		String formattedDateTime = localDateTime.format(formatter);
		return formattedDateTime;
	}

	/**
	 * convertEpochToLocalDate
	 * @param inEpoch, String
	 * @return String
	 */
	public static String convertEpochToLocalDate(String inEpoch) {
		long epochTimeMillis = Long.parseLong(inEpoch + "L");
		Instant instant = Instant.ofEpochMilli(epochTimeMillis);
		ZoneId zoneId = ZoneId.systemDefault();
		LocalDate localDate = instant.atZone(zoneId).toLocalDate();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String formattedDate = localDate.format(formatter);
		return formattedDate;
	}

	/**
	 * convertLocalDateToEpoch
	 * @param inDate, Object
	 * @return long
	 */
	public static long convertLocalDateToEpoch(Object inDate) {
		ZoneId zoneId = ZoneId.systemDefault();
		long dateinmilliesecs = 0;
		if (inDate instanceof LocalDate) {
			dateinmilliesecs = ((LocalDate) inDate).atStartOfDay(zoneId).toInstant().toEpochMilli();
		} else if (inDate instanceof Date) {
			LocalDate inLDate = ((Date) inDate).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			dateinmilliesecs = inLDate.atStartOfDay(zoneId).toInstant().toEpochMilli();
		}
		return dateinmilliesecs;
	}

	/**
	 * convertLocalDateTimeToEpoch
	 * @param inDate, Object
	 * @return long
	 */
	public static long convertLocalDateTimeToEpoch(Object inDate) {
		ZoneId zoneId = ZoneId.systemDefault();
		long timemilliesecs = 0;
		if (inDate instanceof LocalDateTime) {
			timemilliesecs = ((LocalDateTime) inDate).atZone(zoneId).toInstant().toEpochMilli();
		} else if (inDate instanceof Date) {
			LocalDateTime inLDate = ((Date) inDate).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			timemilliesecs = inLDate.atZone(zoneId).toInstant().toEpochMilli();
		}
		return timemilliesecs;
	}

	/**
	 * convertToISO8601Format
	 * @param inDate, String
	 * @param inTime, String
	 * @return String
	 */
	public static String convertToISO8601Format(String inDate, String inTime) {
		DateTimeFormatter dateInputFormat = DateTimeFormatter.ofPattern("MM/dd/uuuu");
		String dateTime = LocalDate.parse(inDate, dateInputFormat).atTime(LocalTime.parse(inTime)).toString();
		return dateTime;
	}

	/**
	 * convertToISO8601Format
	 * @param inDate, String
	 * @return String
	 */
	public static String convertToISO8601Format(String inDate) {
		DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
		LocalDateTime date = LocalDateTime.parse(inDate, inputFormat);
		String dateTime = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(date);
		return dateTime;

	}

	/**
	 * convertStringToDate
	 * @param inDate, String
	 * @param informat, String
	 * @return Date
	 * @throws ParseException
	 */
	public static Date convertStringToDate(String inDate, String informat) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat(informat, Locale.US);
		formatter.setTimeZone(TimeZone.getDefault());
		Date date = formatter.parse(inDate);
		return date;
	}

	/**
	 * convertStringToLocalDate
	 * @param inDate, String
	 * @param inFormat, String
	 * @return LocalDate
	 */
	public static LocalDate convertStringToLocalDate(String inDate, String inFormat) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(inFormat);
		LocalDate localDate = LocalDate.parse(inDate,formatter);
		return localDate;
	}

	/**
	 * convertStringToLocalDateTime
	 * @param inDate
	 * @param inFormat
	 * @return LocalDateTime
	 */
	public static LocalDateTime convertStringToLocalDateTime(String inDate, String inFormat) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(inFormat);
		LocalDateTime localDateTime = LocalDateTime.parse(inDate,formatter);
		return localDateTime;
	}



}
