package com.streamhub.request;


public enum RequestType {
	SYNCHRONOUS, KEEP_ALIVE, UNKNOWN;

	public static RequestType fromHttpRequest(boolean urlStartsWithRequest, boolean urlStartsWithResponse,
			boolean connectionCloseHeaderPresent) {
	    if (urlStartsWithRequest) {
			return SYNCHRONOUS;
	    } else if (urlStartsWithResponse) {
	    	if (connectionCloseHeaderPresent) {
	    		return SYNCHRONOUS;
	    	}
	    	
			return KEEP_ALIVE;
		}
		
		return UNKNOWN;
	}
}
