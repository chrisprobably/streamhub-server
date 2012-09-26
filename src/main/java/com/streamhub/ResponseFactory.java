package com.streamhub;

import java.util.HashMap;
import java.util.Map;

import com.streamhub.request.Request;
import com.streamhub.util.Browser;
import com.streamhub.util.WordUtils;

@SuppressWarnings("serial")
class ResponseFactory {
	private static final String COLON = ": ";
	private static final String CRLF = "\r\n";
	private static final String CLOSE_HTML_HEAD_SHORT = "';</script></head>";
	private static final String CLOSE_HTML_HEAD = "';r=window.parent.r;window.onload=r;</script></head>";
	private static final String HTML_FOOTER = "</body></html>";
	private static final String RESPONSE_OK = "<script>x(\"response OK\");</script>";
	private static final String REQUEST_OK = "<body>request OK</body></html>";
	private static final String DISCONNECTION_OK = "<body>disconnection OK</body></html>";
	private static final String CLOSE_RESPONSE_OK = "<body>close response OK</body></html>";
	private static final String SUBSCRIPTION_OK = "<body>subscription OK</body></html>";
	private static final String UNSUBSCRIBE_OK = "<body>unsubscribe OK</body></html>";
	private static final String PUBLISH_OK = "<body>publish OK</body></html>";
	private static final String _404_NOT_FOUND = HttpHandler._404_NOT_FOUND;
	private static final String IE_PADDING = "<!--PADDING-PADDING-PADDING-PADDING-PADDING--><!--PADDING-PADDING-PADDING-PADDING-PADDING--><!--PADDING-PADDING-PADDING-PADDING-PADDING-->";
	private static final String WEBKIT_PADDING = "<div>                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                </div>" + "<div>                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                </div>";
	private static final String FOREVER_FRAME_SETUP_SCRIPT = "<script>x=window.parent.x;</script>";
	private static final Map<String, String> defaultPushHeaders = new HashMap<String, String>() {{
		put("Server", "StreamHub");
		put("Content-Type", "text/html");
		put("Cache-Control", "no-store, no-cache");
		put("Pragma", "no-cache");
		put("Expires", "Thu, 1 Jan 1970 00:00:00 GMT");
		put("Content-Length", "300000");
		put("Connection", "close");
	}};
	private static final Map<String, String> defaultHttpHeaders = new HashMap<String, String>() {{
		put("Server", "StreamHub");
		put("Content-Type", "text/html");
		put("Cache-Control", "no-store, no-cache");
		put("Pragma", "no-cache");
		put("Expires", "Thu, 1 Jan 1970 00:00:00 GMT");
		put("Connection", "close");
	}};
	private static String noCacheHttpHeader = buildNoCacheHttpHeader();
	private static String noCachePushHeader = buildNoCachePushHeader();
	private static String noCacheStaticHeader = buildNoCacheStaticHeader();

	private static String buildNoCacheStaticHeader() {
		return noCacheHttpHeader + "<html><head><script>document.domain='";
	}

	private static final String NO_CACHE_PUSH_HTML_HEADER = "<html>" + "<head>"
			+ "<title>StreamHub Push Page</title>" + "<script>document.domain='";
	private static final String NO_CACHE_HTML_TAIL = "';</script></head><body onload=\"window.parent.l()\">";
	private static final String DOC_DOMAIN_TAIL_PLUS_RESPONSE_SCRIPT_PLUS_REQUEST_OK_FOOTER = CLOSE_HTML_HEAD + REQUEST_OK;
	private static final String DOC_DOMAIN_HEAD = "';r=window.parent.r;window.onload=r;</script></head><body>";
	private static final String DOC_DOMAIN_TAIL_PLUS_RESPONSE_SCRIPT_PLUS_SUBSCRIPTION_OK_FOOTER = CLOSE_HTML_HEAD + SUBSCRIPTION_OK;
	private static final String DOC_DOMAIN_TAIL_PLUS_RESPONSE_SCRIPT_PLUS_UNSUBSCRIBE_OK_FOOTER = CLOSE_HTML_HEAD + UNSUBSCRIBE_OK;
	private static final String DOC_DOMAIN_TAIL_PLUS_RESPONSE_SCRIPT_PLUS_PUBLISH_OK_FOOTER = CLOSE_HTML_HEAD + PUBLISH_OK;
	private static final String DOC_DOMAIN_TAIL_PLUS_RESPONSE_SCRIPT_PLUS_DISCONNECTION_OK_FOOTER = CLOSE_HTML_HEAD + DISCONNECTION_OK;
	private static final String DOC_DOMAIN_TAIL_PLUS_RESPONSE_SCRIPT_PLUS_CLOSE_RESPONSE_OK_FOOTER = CLOSE_HTML_HEAD + CLOSE_RESPONSE_OK;
	private static final String NO_CACHE_REQUEST_OK_FOOTER = CLOSE_HTML_HEAD_SHORT + REQUEST_OK;
	private static final String NO_CACHE_SUBSCRIPTION_OK_FOOTER = CLOSE_HTML_HEAD_SHORT + SUBSCRIPTION_OK;
	private static final String NO_CACHE_UNSUBSCRIBE_OK_FOOTER = CLOSE_HTML_HEAD_SHORT + UNSUBSCRIBE_OK;
	private static final String NO_CACHE_PUBLISH_OK_FOOTER = CLOSE_HTML_HEAD_SHORT + PUBLISH_OK;
	private static final String NO_CACHE_DISCONNECTION_OK_FOOTER = CLOSE_HTML_HEAD_SHORT + DISCONNECTION_OK;
	private static final String NO_CACHE_CLOSE_RESPONSE_OK_FOOTER = CLOSE_HTML_HEAD_SHORT + CLOSE_RESPONSE_OK;
	
	public static String subscriptionResponse(String domain, Browser browser) {
		StringBuilder html = new StringBuilder();
		if (browser.isIEFamily()) {
			html.append(noCacheStaticHeader);
			html.append(domain);
			html.append(DOC_DOMAIN_TAIL_PLUS_RESPONSE_SCRIPT_PLUS_SUBSCRIPTION_OK_FOOTER);
		} else {
			html.append(noCacheStaticHeader);
			html.append(domain);
			html.append(NO_CACHE_SUBSCRIPTION_OK_FOOTER);
		}
		return html.toString();
	}

	public static String unSubscribeResponse(String domain, Browser browser) {
		StringBuilder html = new StringBuilder();
		if (browser.isIEFamily()) {
			html.append(noCacheStaticHeader);
			html.append(domain);
			html.append(DOC_DOMAIN_TAIL_PLUS_RESPONSE_SCRIPT_PLUS_UNSUBSCRIBE_OK_FOOTER);
		} else {
			html.append(noCacheStaticHeader);
			html.append(domain);
			html.append(NO_CACHE_UNSUBSCRIBE_OK_FOOTER);
		}
		return html.toString();
	}
	
	public static String publishResponse(String domain, Browser browser) {
		StringBuilder html = new StringBuilder();
		if (browser.isIEFamily()) {
			html.append(noCacheStaticHeader);
			html.append(domain);
			html.append(DOC_DOMAIN_TAIL_PLUS_RESPONSE_SCRIPT_PLUS_PUBLISH_OK_FOOTER);
		} else {
			html.append(noCacheStaticHeader);
			html.append(domain);
			html.append(NO_CACHE_PUBLISH_OK_FOOTER);
		}
		return html.toString();
	}

	public static String requestResponse(String domain, Browser browser) {
		StringBuilder html = new StringBuilder();
		if (browser.isIEFamily()) {
			html.append(noCacheStaticHeader);
			html.append(domain);
			html.append(DOC_DOMAIN_TAIL_PLUS_RESPONSE_SCRIPT_PLUS_REQUEST_OK_FOOTER);
		} else {
			html.append(noCacheStaticHeader);
			html.append(domain);
			html.append(NO_CACHE_REQUEST_OK_FOOTER);
		}
		return html.toString();
	}
	
	public static String pollResponse(String domain, Browser browser, String queuedMessages) {
		StringBuilder html = new StringBuilder();
		if (browser.isIEFamily()) {
			html.append(noCacheStaticHeader);
			html.append(domain);
			html.append(DOC_DOMAIN_HEAD);
			html.append(queuedMessages);
			html.append(HTML_FOOTER);
		} else {
			html.append(noCacheHttpHeader);
			html.append(queuedMessages);
		}
		return html.toString();
	}

	public static String disconnectionResponse(String domain, Browser browser) {
		StringBuilder html = new StringBuilder();
		if (browser.isIEFamily()) {
			html.append(noCacheStaticHeader);
			html.append(domain);
			html.append(DOC_DOMAIN_TAIL_PLUS_RESPONSE_SCRIPT_PLUS_DISCONNECTION_OK_FOOTER);
		} else {
			html.append(noCacheStaticHeader);
			html.append(domain);
			html.append(NO_CACHE_DISCONNECTION_OK_FOOTER);
		}
		return html.toString();
	}
	
	public static String closeResponse(String domain, Browser browser) {
		StringBuilder html = new StringBuilder();
		if (browser.isIEFamily()) {
			html.append(noCacheStaticHeader);
			html.append(domain);
			html.append(DOC_DOMAIN_TAIL_PLUS_RESPONSE_SCRIPT_PLUS_CLOSE_RESPONSE_OK_FOOTER);
		} else {
			html.append(noCacheStaticHeader);
			html.append(domain);
			html.append(NO_CACHE_CLOSE_RESPONSE_OK_FOOTER);
		}
		return html.toString();
	}

	public static String defaultResponse(Request request) {
		return _404_NOT_FOUND;
	}

	public static String foreverFramePageHeader(String domain, Browser browser) {
		StringBuilder html = new StringBuilder();
		html.append(noCachePushHeader);
		html.append(pushHtmlHeader(domain));
		html.append(foreverFrameSetupScript(browser));
		html.append(RESPONSE_OK);
		return html.toString();
	}
	
	public static void setDefaultPushHeader(String name, String value) {

		if (value == null) {
			defaultPushHeaders.remove(name);
		} else {
			defaultPushHeaders.put(WordUtils.capitalizeFully(name), value);
		}
		noCachePushHeader = buildNoCachePushHeader();
	}

	public static void setDefaultHeader(String name, String value) {
		if (value == null) {
			defaultHttpHeaders.remove(name);
		} else {
			defaultHttpHeaders.put(WordUtils.capitalizeFully(name), value);
		}
		noCacheHttpHeader = buildNoCacheHttpHeader();
		noCacheStaticHeader = buildNoCacheStaticHeader();
	}

	private static String pushHtmlHeader(String domain) {
		StringBuilder header = new StringBuilder();
		header.append(NO_CACHE_PUSH_HTML_HEADER);
		header.append(domain);
		header.append(NO_CACHE_HTML_TAIL);
		return header.toString();
	}

	private static String foreverFrameSetupScript(Browser browser) {
		StringBuilder script = new StringBuilder();
		script.append(FOREVER_FRAME_SETUP_SCRIPT);

		if (browser.isWebKitFamily()) {
			script.append(WEBKIT_PADDING);
		} else if (browser.isIEFamily()) {
			script.append(IE_PADDING);
		}

		return script.toString();
	}

	private static String buildNoCachePushHeaders() {
		StringBuilder result = new StringBuilder();
		for (Map.Entry<String, String> header : defaultPushHeaders.entrySet()) {
			result.append(header.getKey()).append(COLON).append(header.getValue()).append(CRLF);
		}
		return result.toString();
	}

	private static String buildNoCacheHttpHeaders() {
		StringBuilder result = new StringBuilder();
		for (Map.Entry<String, String> header : defaultHttpHeaders.entrySet()) {
			result.append(header.getKey()).append(COLON).append(header.getValue()).append(CRLF);
		}
		return result.toString();
	}
	
	private static String buildNoCachePushHeader() {
		return "HTTP/1.1 200 OK\r\n"
				+ buildNoCachePushHeaders()
				+ CRLF;
	}
	
	private static String buildNoCacheHttpHeader() {
		return "HTTP/1.1 200 OK\r\n"
				+ buildNoCacheHttpHeaders()
				+ CRLF;
	}
}
