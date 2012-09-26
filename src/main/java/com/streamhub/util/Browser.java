package com.streamhub.util;

public enum Browser {
	FF3("*firefox"),
	FF2("*firefox"),
	IE("*iehta"),
	IE7("*iehta"),
	IE8("*iehta"),
	CHROME("*googlechrome"), 
	SAFARI("*safari"), 
	UNKNOWN("n/a");
	
	private static final String FIREFOX_3 = "Firefox/3";
	private static final String FIREFOX_2 = "Firefox/2";
	private static final String CHROME_ = "Chrome/";
	private static final String SAFARI_ = "Safari/";
	private static final String MSIE = "MSIE";
	private static final String MSIE_7 = "MSIE 7";
	private static final String MSIE_8 = "MSIE 8";
	private final String seleniumSpec;
	
	Browser(String seleniumSpec) {
		this.seleniumSpec = seleniumSpec;
	}
	
	public String getSeleniumSpec() {
		return seleniumSpec;
	}
	
	public boolean isFirefoxFamily() {
		return this == FF3 || this == FF2;
	}
	
	public boolean isIEFamily() {
		return this == IE || this == IE7 || this == IE8;
	}
	
	public boolean isWebKitFamily() {
		return this == SAFARI || this == CHROME;
	}

	public static Browser fromUserAgent(String userAgent) {
		if (userAgent == null) {
			return Browser.UNKNOWN;
		} else if (userAgent.contains(MSIE_8)) {
			return IE8;
		} else if (userAgent.contains(MSIE_7)) {
			return IE7;
		} else if (userAgent.contains(MSIE)) {
			return IE;
		} else if (userAgent.contains(SAFARI_) && !userAgent.contains(CHROME_)) {
			return SAFARI;
		} else if (userAgent.contains(CHROME_)) {
			return CHROME;
		} else if (userAgent.contains(FIREFOX_2)) {
			return FF2;
		} else if (userAgent.contains(FIREFOX_3)) {
			return FF3;
		}
		
		return Browser.UNKNOWN;
	}
}
