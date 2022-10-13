package edu.bu.ist.apps.kualiautomation.util;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

public enum DateOffset {

	MMDDYYYY("MM/dd/yyyy", "MM/DD/YYYY"),
	CUSTOM(null, "Custom Format");
	
	private String description;
	private String pattern;

	public static enum DatePart {
		SECOND(Calendar.SECOND, 1000L), 
		MINUTE(Calendar.MINUTE, SECOND.getMilliseconds() * 60), 
		HOUR(Calendar.HOUR, MINUTE.getMilliseconds() * 60), 
		DAY(Calendar.DAY_OF_MONTH, HOUR.getMilliseconds() * 24), 
		MONTH(Calendar.MONTH, null), 
		YEAR(Calendar.YEAR, null);
		
		private Long milliseconds;
		private int calendarId;
		
		private DatePart(int calendarId, Long milliseconds) {
			this.calendarId = calendarId;
			this.milliseconds = milliseconds;
		}
		public long getMilliseconds() {
			return milliseconds;
		}
		public int getCalendarId() {
			return calendarId;
		}
		public boolean is(String name) {
			return this.name().equals(name);
		}
		/**
		 * Get a list of the enum names where only the first letter is capitalized. 
		 * @return
		 */
		public static Map<String, String> toJson() {
			// Create a map of DateParts in the same order they are declared in the enum.
			Map<String, String> map = new TreeMap<String, String>(new Comparator<String>(){
				@Override public int compare(String key1, String key2) {
					int idx1 = Arrays.asList(DatePart.values()).indexOf(DatePart.valueOf(key1));
					int idx2 = Arrays.asList(DatePart.values()).indexOf(DatePart.valueOf(key2));
					return idx1 < idx2 ? -1 : (idx2 < idx1 ? 1 : 0);
				}});
			for(DatePart part : DatePart.values()) {
				map.put(part.name(), part.name().substring(0, 1) + part.name().substring(1).toLowerCase());
			}
			return map;
		}
	}
	
	private DateOffset(String pattern, String description) {
		this.pattern = pattern;
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public boolean is(String name) {
		return this.name().equals(name);
	}

	/**
	 * @param format A simplified, human-readable version of the pattern being returned.
	 * @return The exact value as would be provided to {@link SimpleDateFormat#applyPattern(String).
	 */
	private String getPattern(String format) {
		return format == null ? null : format.toLowerCase().replaceAll("m", "M");
	}

	public String getOffsetDate(DatePart datePart, Integer dateUnits) {
		return getDate(getPattern(this.pattern), datePart, dateUnits);
	}

	public String getOffsetDate(String format, DatePart datePart, Integer dateUnits) {
		return getDate(getPattern(format), datePart, dateUnits);
		
	}

	private String getDate(String pattern, DatePart datePart, Integer dateUnits) {
		
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		// Get the time of today before its first millisecond has elapsed (midnight).
		long now = System.currentTimeMillis();
		long rounded = now - (now % DatePart.DAY.getMilliseconds());
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(rounded);
		c.add(datePart.getCalendarId(), dateUnits);
		
		return sdf.format(new Date(c.getTimeInMillis()));
	}
	
	public String getTimeOffset(DatePart datePart, Integer dateUnits) {
		return getDate("EEE, d MMM yyyy HH:mm:ss.SSS", datePart, dateUnits);
	}
	
	/**
	 * An alternative to valueOf where a null is returned if no matching enum member can be found.
	 * Also the match is case-insensitive.
	 * @param val
	 * @return
	 */
	public static DateOffset valueOfOrNull(String val) {
		if(Utils.isEmpty(val))
			return null;
		try {
			return DateOffset.valueOf(val.toUpperCase().trim());
		} 
		catch (IllegalArgumentException e) {
			return null;
		}
	}
	
	public static Map<String, String> toJson() {
		// Return a sorted map, sorted by alpha-numeric value with the exception of "CUSTOM", which must always go to the bottom.
		Map<String, String> map = new TreeMap<String, String>(new Comparator<String>(){
			@Override public int compare(String o1, String o2) {
				int compared = o1.compareTo(o2);
				if(compared != 0) {
					if(CUSTOM.is(o1))
						return 1;
					if(CUSTOM.is(o2))
						return -1;
				}
				return compared;
			}});
		for(DateOffset offset : DateOffset.values()) {
			map.put(offset.name(), offset.getDescription());
		}
		return map;
	}

	public static void main(String[] args) {
		System.out.println(MMDDYYYY.getTimeOffset(DatePart.DAY, 2));
		System.out.println(MMDDYYYY.getTimeOffset(DatePart.DAY, -2));
		System.out.println(MMDDYYYY.getTimeOffset(DatePart.MONTH, 2));
		System.out.println(MMDDYYYY.getTimeOffset(DatePart.MONTH, -2));
		System.out.println(MMDDYYYY.getTimeOffset(DatePart.YEAR, 2));
		System.out.println(MMDDYYYY.getTimeOffset(DatePart.YEAR, -2));		
	}
}
