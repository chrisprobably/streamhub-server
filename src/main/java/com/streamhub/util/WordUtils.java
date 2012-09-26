package com.streamhub.util;

public class WordUtils {
	public static String capitalizeFully(String string) {
		String capitalized = string;
		int indexOfDash = string.indexOf("-");
		
		if (indexOfDash > 0 && string.length() > (indexOfDash + 1)) {
	        String firstLetter = string.substring(0,1);
	        String preDash   = string.substring(1, indexOfDash);
	        String secondLetter = string.substring(indexOfDash + 1, indexOfDash + 2);
	        String remainder = string.substring(indexOfDash + 2);
	        capitalized = firstLetter.toUpperCase() + preDash.toLowerCase() + "-" + secondLetter.toUpperCase() + remainder.toLowerCase();
		} else {
	        String firstLetter = string.substring(0,1);
	        String remainder   = string.substring(1);
	        capitalized = firstLetter.toUpperCase() + remainder.toLowerCase();
		}
		
		return capitalized;
	}
}
