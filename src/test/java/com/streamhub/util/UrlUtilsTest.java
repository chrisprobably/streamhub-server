package com.streamhub.util;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

public class UrlUtilsTest {
	@Test
	public void getsHttpHeadersFromRequest() throws Exception {
		String httpRequest = 
			"GET /request/?uid=1&domain=127.0.0.1 HTTP/1.1\r\n" +
			"User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/525.13 (KHTML, like Gecko) Version/3.1 Safari/525.13.3\r\n" +
			"Accept-Encoding: gzip, deflate\r\n" +
			"Referer: http://127.0.0.1:8156/test/index.html\r\n" +
			"Accept: text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5\r\n" +
			"Accept-Language: en-US\r\n" +
			"Connection: keep-alive\r\n" +
			"Host: 127.0.0.1:8888\r\n\r\n";
		
		char[] charArray = httpRequest.toCharArray();
		
		Map<String, String> httpHeaders = UrlUtils.getHttpHeaders(httpRequest, charArray);

		assertEquals("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/525.13 (KHTML, like Gecko) Version/3.1 Safari/525.13.3", httpHeaders.get("User-Agent"));
		assertEquals("gzip, deflate", httpHeaders.get("Accept-Encoding"));
		assertEquals("text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5", httpHeaders.get("Accept"));
		assertEquals("en-US", httpHeaders.get("Accept-Language"));
		assertEquals("keep-alive", httpHeaders.get("Connection"));
		assertEquals("http://127.0.0.1:8156/test/index.html", httpHeaders.get("Referer"));
		assertEquals("127.0.0.1:8888", httpHeaders.get("Host"));
	}
	
	@Test
	public void getsQueryParamAsAnArray() throws Exception {
		String httpRequest = "/subscribe/?uid=1&domain=127.0.0.1&topic=AAPL,MSFT,WMT";
		String[] topics = UrlUtils.getQueryParamAsArray(httpRequest, "topic");
		
		assertEquals("AAPL", topics[0]);
		assertEquals("MSFT", topics[1]);
		assertEquals("WMT", topics[2]);
		assertEquals(3, topics.length);
	}
	
	@Test
	public void getsQueryParamAsAnArrayWhenOnlyOneItem() throws Exception {
		String httpRequest = "/subscribe/?uid=1&domain=127.0.0.1&topic=AAPL";
		String[] topics = UrlUtils.getQueryParamAsArray(httpRequest, "topic");
		
		assertEquals(1, topics.length);
		assertEquals("AAPL", topics[0]);
	}

	@Test
	public void getsQueryParamWhenEncoded() throws Exception {
		String httpRequest = "/subscribe/?uid=1&domain=127.0.0.1&topic=AAPL%2CMSFT%2CWMT";
		String[] topics = UrlUtils.getQueryParamAsArray(httpRequest, "topic");
		
		assertEquals("AAPL", topics[0]);
		assertEquals("MSFT", topics[1]);
		assertEquals("WMT", topics[2]);
		assertEquals(3, topics.length);
	}
	
	@Test
	public void getsQueryParamWhenEncodedLowercase() throws Exception {
		String httpRequest = "/subscribe/?uid=1&domain=127.0.0.1&topic=AAPL%2cMSFT%2cWMT";
		String[] topics = UrlUtils.getQueryParamAsArray(httpRequest, "topic");
		
		assertEquals("AAPL", topics[0]);
		assertEquals("MSFT", topics[1]);
		assertEquals("WMT", topics[2]);
		assertEquals(3, topics.length);
	}

	@Test
	public void getsQueryParamWithSpaces() throws Exception {
		String httpRequest = "/request/?uid=1&domain=127.0.0.1&hello=AAPL MSFT WMT";
		Map<String, String> queryParams = UrlUtils.getQueryParams(httpRequest);
		String hello = queryParams.get("hello");
		assertEquals("AAPL MSFT WMT", hello);
	}
	
	@Test
	public void doesNotBlowUpOnQueryParamWithNoEquals() throws Exception {
		String httpRequest = "/request/?equalsless";
		UrlUtils.getQueryParams(httpRequest);
	}
	
	@Test
	public void getsQueryParamWithSpacesInJson() throws Exception {
		String httpRequest = "/publish/?uid=1&domain=127.0.0.1&topic=chat&payload={\"topic\":\"chat\",\"message\":\"Hello how are you?\"}";;
		Map<String, String> queryParams = UrlUtils.getQueryParams(httpRequest);
		String payload = queryParams.get("payload");
		assertEquals("{\"topic\":\"chat\",\"message\":\"Hello how are you?\"}", payload);
	}
	
	@Test
	public void getsJsonQueryParamWithEqualsCharacter() throws Exception {
		String httpRequest = "/publish/?uid=1&domain=127.0.0.1&topic=chat&payload={\"topic\":\"chat\",\"message\":\"Hello=Awesome\"}";;
		Map<String, String> queryParams = UrlUtils.getQueryParams(httpRequest);
		String payload = queryParams.get("payload");
		assertEquals("{\"topic\":\"chat\",\"message\":\"Hello=Awesome\"}", payload);
	}
	
	@Test
	public void doesNotBlowUpOnHeaderlessRequest() throws Exception {
		String httpRequest = "GET /request/?uid=1&domain=127.0.0.1 HTTP/1.1\r\n\r\n";
		char[] charArray = httpRequest.toCharArray();
		Map<String, String> httpHeaders = UrlUtils.getHttpHeaders(httpRequest, charArray);
		assertEquals(0, httpHeaders.size());
	}
	
	@Test
	public void doesNotBlowUpOnRubbishRequest() throws Exception {
		String httpRequest = 
			"GET: /request/\r\n?ui\nd=1&domain=127.0.0.1 HTTP/1.1\r\n\r\n:\r\n" +
			"::::User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/525.13 (KHTML, like Gecko) Version/3.1 Safari/525.13.3\r\n";
		char[] charArray = httpRequest.toCharArray();
		UrlUtils.getHttpHeaders(httpRequest, charArray);
	}

	@Test
	public void getQueryParamsDoesNotBlowUpOnIframeRequest() throws Exception {
		UrlUtils.getQueryParams("/iframe.html");
	}
	
	@Test
	public void getsContext() throws Exception {
		assertEquals("/", UrlUtils.getContext("/request/?uid=242343"));
		assertEquals("/mywebapp", UrlUtils.getContext("/mywebapp/"));
		assertEquals("/", UrlUtils.getContext("/index.html"));
		assertEquals("/mywebapp", UrlUtils.getContext("/mywebapp/myapp"));
		assertEquals("/mywebapp", UrlUtils.getContext("/mywebapp/myapp/"));
		assertEquals("/mywebapp", UrlUtils.getContext("/mywebapp/myapp/page.jsp"));
		assertEquals("/", UrlUtils.getContext("/"));
		assertEquals("/", UrlUtils.getContext("/1"));
		assertEquals("/", UrlUtils.getContext("/index.jsp?path=/home"));
		assertEquals("/myapp", UrlUtils.getContext("/myapp/index.jsp?path=/home"));
		assertEquals("/~bob", UrlUtils.getContext("/~bob/index.jsp?path=/home"));
		assertEquals("/-a", UrlUtils.getContext("/-a/home.html?s=//232--"));
		assertEquals("/", UrlUtils.getContext("//////index.html"));
		assertEquals("/a", UrlUtils.getContext("/a/b/c/d/e/index.html"));
	}
	
	@Test
	public void convertContext() throws Exception {
		assertEquals("/mywebapp", UrlUtils.normalizeContext("/mywebapp/*"));
		assertEquals("/mywebapp", UrlUtils.normalizeContext("/mywebapp/"));
		assertEquals("/mywebapp", UrlUtils.normalizeContext("/mywebapp"));
		assertEquals("/", UrlUtils.normalizeContext("/*"));
		assertEquals("/", UrlUtils.normalizeContext("/"));
	}
	
	@Test
	public void getRequestUrlGetsUrlToEndOfLine() throws Exception {
		String request = "GET: /request/\r\n?ui\nd=1&domain=127.0.0.1 HTTP/1.1\r\n\r\n:\r\n" +
			"::::User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/525.13 (KHTML, like Gecko) Version/3.1 Safari/525.13.3\r\n";
		char[] charArray = request.toCharArray();
		assertEquals("/request/", UrlUtils.getRequestUrl(request, charArray));
	}
	
	@Test
	public void addsNoMoreThanTwentyFiveHeaders() throws Exception {
		String httpRequest = 
			"GET /request/?uid=1&domain=127.0.0.1 HTTP/1.1\r\n";
		
		for (int i=0; i < 30; i++) {
			httpRequest += "Woot" + i + ": woot\r\n";
		}
		
		httpRequest += "\r\n";
		char[] charArray = httpRequest.toCharArray();
		Map<String, String> httpHeaders = UrlUtils.getHttpHeaders(httpRequest, charArray);

		assertEquals(25, httpHeaders.size());
	}
}
