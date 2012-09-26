package com.streamhub.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class UrlUtils {
	private static final char SPACE_CHAR = ' ';
	private static final char LF_CHAR = '\n';
	private static final char CR_CHAR = '\r';
	private static final char COLON_CHAR = ':';
	private static final String EQUALS = "=";
	private static final String AMPERSAND = "&";
	private static final String QUESTION_MARK = "?";
	private static final int HTTP_HEADERS_LIMIT = 25;

	public static String getQueryParam(String requestUrl, String name) {
		String queryParam = null;
		
		if (requestUrl.contains(QUESTION_MARK)) {
			Map<String, String> queryParams = getQueryParams(requestUrl);
			queryParam = queryParams.get(name);
		}
		return queryParam;
	}

	public static String[] getQueryParamAsArray(String requestUrl, String name) {
		String queryParam = null;
		String[] array = null;
		
		if (requestUrl.contains(QUESTION_MARK)) {
			Map<String, String> queryParams = getQueryParams(requestUrl);
			queryParam = queryParams.get(name);
		}
		
		if (queryParam == null) {
			return null;
		}
		
		if (queryParam.contains("%2C") || queryParam.contains("%2c")) {
			array = queryParam.split("%2[Cc]");			
		} else {
			array = queryParam.split(",");			
		}

		return array;
	}
	
	public static String[] queryValueToArray(String queryValue) {
		String[] array = null;

		if (queryValue == null) {
			return array;
		} else if (queryValue.contains("%2C") || queryValue.contains("%2c")) {
			array = queryValue.split("%2[Cc]");			
		} else {
			array = queryValue.split(",");			
		}
		
		return array;
	}	

	public static Map<String, String> getQueryParams(String requestUrl) {
		int firstQuestionMark = requestUrl.indexOf(QUESTION_MARK);
		
		if (firstQuestionMark < 0) {
			return Collections.emptyMap();
		}
		
		Map<String, String> queryParams = new HashMap<String, String>();
		String queryString = requestUrl.substring(firstQuestionMark + 1);
		String[] splitParams = queryString.split(AMPERSAND);
		
		for (String param : splitParams) {
			int firstEqualSign = param.indexOf(EQUALS);
			if (firstEqualSign < 0) {
				continue;
			}
			queryParams.put(param.substring(0, firstEqualSign), param.substring(firstEqualSign+1).trim());
		}
		
		return queryParams;
	}

	public static String getRequestUrl(String request, char[] charArray) {
		int index = 0;
		int startIndex = 0;
		int spaceCount = 0;
		while (index < charArray.length) {
			if (charArray[index] == ' ' || charArray[index] == '\r' || charArray[index] == '\n') {
				spaceCount++;
				if (spaceCount == 1) {
					startIndex = index + 1;
				} else if (spaceCount == 2) {
					return request.substring(startIndex, index);
				}
			}
			
			index++;
		}

		return request;
	}

	public static Map<String, String> getHttpHeaders(String httpRequest, char[] charArray) {
		Map<String, String> headers = new HashMap<String, String>();
		
		if (charArray.length == 0) {
			return headers;
		}

		int startIndex = 0;
		char index = 0;
		String headerKey = "";
		String headerValue = "";
		boolean waitingOnValue = false;
		
		do {
			if (! waitingOnValue && charArray[index] == COLON_CHAR) {
				headerKey = httpRequest.substring(startIndex, index);
				startIndex = index + 1;
				while (charArray[++index] == SPACE_CHAR) {
					startIndex = index + 1;
				}
				waitingOnValue = true;
			} 
			
			if (charArray[index] == CR_CHAR || charArray[index] == LF_CHAR) {
				if (waitingOnValue) {
					headerValue = httpRequest.substring(startIndex, index);
					headers.put(headerKey, headerValue);
					waitingOnValue = false;
				}
				
				startIndex = index + 1;
			} 
			
		} while(++index < charArray.length && headers.size() < HTTP_HEADERS_LIMIT);
		
		return headers;
	}

	public static String getContext(String url) {
		int secondSlashIndex = url.indexOf('/', 1);
		if (secondSlashIndex < 0 || (url.length() > secondSlashIndex + 1 && url.charAt(secondSlashIndex + 1) == '?')) {
			return "/";
		}
		int qMarkIndex = url.indexOf('?');
		if (qMarkIndex > -1 && qMarkIndex < secondSlashIndex) {
			secondSlashIndex = 1;
		}
		String context = url.substring(0, secondSlashIndex);
		return context;
	}

	public static String stripContext(String context, String url) {
		if (context.length() > 1) {
			return url.replaceFirst(context, "");
		}
		return url;
	}

	public static String normalizeContext(String context) {
		context = context.replaceAll("\\*", "");
		int secondSlashIndex = context.indexOf('/', 1);
		if (secondSlashIndex < 0) {
			secondSlashIndex = context.length();
		}
		String normalizedContext = context.substring(0, secondSlashIndex);
		return normalizedContext;
	} 
}
