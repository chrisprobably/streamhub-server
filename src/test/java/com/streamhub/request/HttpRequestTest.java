package com.streamhub.request;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.streamhub.Connection;
import com.streamhub.api.Payload;
import com.streamhub.request.HttpRequest;
import com.streamhub.request.Request;
import com.streamhub.request.UrlEncodedJsonPayload;

@RunWith(JMock.class)
public class HttpRequestTest {
	private Mockery context;
	private Connection connection;
	
	@Before
	public void setUp() {
		context = new Mockery() {{
			setImposteriser(ClassImposteriser.INSTANCE);
		}};
		connection = context.mock(Connection.class);
	}
	
	@Test
	public void testFactoryMethodConstructsRequestFromInputStream() throws Exception {
		Request request = buildRequest("/request/?uid=1&domain=127.0.0.1");
		
		assertEquals("1", request.getUid());
		assertEquals("127.0.0.1", request.getDomain());
	}
	
	@Test
	public void testFactoryMethodConstructsRequestFromNIOConnection() throws Exception {
		final String httpRequest = buildHttpRequest("/request/?uid=1&domain=127.0.0.1");
		context.checking(new Expectations() {{
			one(connection).readBytes(); will(returnValue(httpRequest.getBytes()));
		}});

		Request request = HttpRequest.createFrom(connection);
		assertEquals("1", request.getUid());
		assertEquals("127.0.0.1", request.getDomain());
	}
	
	@Test
	public void testGettingContextFromStreamHubRequest() throws Exception {
		final String httpRequest = buildHttpRequest("/request/?uid=1&domain=127.0.0.1");
		context.checking(new Expectations() {{
			one(connection).readBytes(); will(returnValue(httpRequest.getBytes()));
		}});
		
		Request request = HttpRequest.createFrom(connection);
		assertEquals("/", request.getContext());
	}
	
	@Test
	public void testDetectsWebSocket() throws Exception {
		final String httpRequest = createWebSocketRequest();
		context.checking(new Expectations() {{
			one(connection).readBytes(); will(returnValue(httpRequest.getBytes()));
		}});
		
		Request request = HttpRequest.createFrom(connection);
		assertEquals(true, request.isWebSocket());
	}
	
	@Test
	public void testCreateFromPerformance() throws Exception {
		final String httpRequest = buildHttpRequest("/subscribe/?uid=1&domain=127.0.0.1&r=123154532&topic=BAC,C,WMT,MCD");
		context.checking(new Expectations() {{
			allowing(connection).readBytes(); will(returnValue(httpRequest.getBytes()));
		}});
		
		int num = 100000;
		long startTime = System.currentTimeMillis();
		
		for (int i = 0; i < num; i ++) {
			HttpRequest.createFrom(httpRequest);
		}
		long elapsed = System.currentTimeMillis() - startTime;
		System.out.println("Creating " + num + " HttpRequests took " + elapsed + "ms");
	}
	
	@Test
	public void testFactoryMethodConstructsRequestFromNIOConnectionWithConnection() throws Exception {
		final String httpRequest = buildHttpRequest("/request/?uid=1&domain=127.0.0.1");
		context.checking(new Expectations() {{
			one(connection).readBytes(); will(returnValue(httpRequest.getBytes()));
		}});
		
		Request request = HttpRequest.createFrom(connection);
		assertEquals(connection, request.getConnection());
	}
	
	@Test
	public void testFactoryMethodConstructsRequestWithSubscriptionTopic() throws Exception {
		Request request = buildRequest("/subscribe/?uid=1&domain=127.0.0.1&topic=AAPL");
		
		assertEquals("AAPL", request.getSubscriptionTopics()[0]);
	}
	
	@Test
	public void testFactoryMethodConstructsRequestWithMultipleSubscriptionTopics() throws Exception {
		Request request = buildRequest("/subscribe/?uid=1&domain=127.0.0.1&topic=AAPL,MSFT,WMT");
		
		assertEquals("AAPL", request.getSubscriptionTopics()[0]);
		assertEquals("MSFT", request.getSubscriptionTopics()[1]);
		assertEquals("WMT", request.getSubscriptionTopics()[2]);
	}
	
	@Test
	public void testFactoryMethodConstructsRequestWithMultipleEncodedSubscriptionTopics() throws Exception {
		Request request = buildRequest("/subscribe/?uid=1&domain=127.0.0.1&topic=AAPL%2CMSFT%2CWMT");
		
		assertEquals("AAPL", request.getSubscriptionTopics()[0]);
		assertEquals("MSFT", request.getSubscriptionTopics()[1]);
		assertEquals("WMT", request.getSubscriptionTopics()[2]);
	}
	
	@Test
	public void testIdentifiesUnSubscribe() throws Exception {
		Request request = buildRequest("/unsubscribe/?uid=1&domain=127.0.0.1&topic=AAPL");
		assertTrue(request.isUnSubscribe());
	}

	@Test
	public void testFactoryMethodConstructsRequestWithUnSubscribeTopic() throws Exception {
		Request request = buildRequest("/unsubscribe/?uid=1&domain=127.0.0.1&topic=AAPL");
		
		assertEquals("AAPL", request.getSubscriptionTopics()[0]);
	}
	
	@Test
	public void testFactoryMethodConstructsRequestWithMultipleUnSubscribeTopics() throws Exception {
		Request request = buildRequest("/unsubscribe/?uid=1&domain=127.0.0.1&topic=AAPL,MSFT,WMT");
		
		assertEquals("AAPL", request.getSubscriptionTopics()[0]);
		assertEquals("MSFT", request.getSubscriptionTopics()[1]);
		assertEquals("WMT", request.getSubscriptionTopics()[2]);
	}
	
	@Test
	public void testFactoryMethodConstructsRequestWithMultipleEncodedUnSubscribeTopics() throws Exception {
		Request request = buildRequest("/unsubscribe/?uid=1&domain=127.0.0.1&topic=AAPL%2CMSFT%2CWMT");
		
		assertEquals("AAPL", request.getSubscriptionTopics()[0]);
		assertEquals("MSFT", request.getSubscriptionTopics()[1]);
		assertEquals("WMT", request.getSubscriptionTopics()[2]);
	}
	
	@Test
	public void testIdentifiesPublishRequest() throws Exception {
		String url = "/publish/?uid=1&domain=127.0.0.1&payload={\"topic\":\"chat\",\"message\":\"Hello%20how%20are%20you?\"}";
		Request request = buildRequest(url);
		assertTrue(request.isPublish());
	}
	
	@Test
	public void testIdentifiesDisconnectRequest() throws Exception {
		Request request = buildRequest("/disconnect/?uid=1&domain=127.0.0.1");
		assertTrue(request.isDisconnection());
	}
	
	@Test
	public void testIdentifiesPoll() throws Exception {
		Request request = buildRequest("/poll/?uid=1&domain=127.0.0.1");
		assertTrue(request.isPoll());
	}
	
	@Test
	public void testIdentifiesCloseResponse() throws Exception {
		Request request = buildRequest("/closeresponse/?uid=1&domain=127.0.0.1");
		assertTrue(request.isCloseResponse());
	}
	
	@Test
	public void testStrippingContext() throws Exception {
		Request request = buildRequest("/streamhub/closeresponse/?uid=1&domain=127.0.0.1");
		assertEquals("/closeresponse/?uid=1&domain=127.0.0.1", request.getProcessedUrl());
	}
	
	@Test
	public void testStrippingRootContext() throws Exception {
		Request request = buildRequest("/closeresponse?uid=1&domain=127.0.0.1");
		assertEquals("/closeresponse?uid=1&domain=127.0.0.1", request.getProcessedUrl());
	}
	
	@Test
	public void testStrippingNoContext() throws Exception {
		Request request = buildRequest("/index.html");
		assertEquals("/index.html", request.getProcessedUrl());
	}
	
	@Test
	public void testGetsTopicOfPublishRequest() throws Exception {
		String url = "/publish/?uid=1&domain=127.0.0.1&topic=chat&payload={\"topic\":\"chat\",\"message\":\"Hello%20how%20are%20you?\"}";
		Request request = buildRequest(url);
		assertEquals("chat", request.getPublishTopic());
	}
	
	@Test
	public void testGetsPayloadOfPublishRequest() throws Exception {
		String url = "/publish/?uid=1&domain=127.0.0.1&topic=chat&payload={\"topic\":\"chat\",\"message\":\"Hello%20how%20are%20you?\"}";
		Request request = buildRequest(url);
		Payload payload = request.getPayload();
		assertEquals("chat", ((UrlEncodedJsonPayload) payload).getField("topic"));
		assertEquals("Hello how are you?", ((UrlEncodedJsonPayload) payload).getField("message"));
	}

	@Test
	public void testGettingUrl() throws Exception {
		String url = "/subscribe/?uid=1&domain=127.0.0.1&topic=AAPL";
		Request request = buildRequest(url);
		
		assertEquals(url, request.getUrl());
	}
	
	@Test
	public void testStripsNewlinesOffQueryParams() throws Exception {
		String url = "/request/?uid=1&domain=127.0.0.1\r\n";
		Request request = buildRequest(url);
		
		assertEquals("127.0.0.1", request.getDomain());
	}
	
	private Request buildRequest(String url) throws IOException {
		final String httpRequest = buildHttpRequest(url);
		context.checking(new Expectations() {{
			allowing(connection).readBytes(); will(returnValue(httpRequest.getBytes()));
		}});
		return HttpRequest.createFrom(connection);
	}

	public static String buildHttpRequest(String url) {
		StringBuilder request = new StringBuilder();
		request.append("GET ").append(url).append(" HTTP/1.1\r\n");
		request.append(httpHeaders());
		return request.toString();
	}
	
	public static String createWebSocketRequest() {
		return "GET /streamhubws/?uid=124234234 HTTP/1.1\r\n" +
								"Upgrade: WebSocket\r\n" +
								"Connection: Upgrade\r\n" +
								"Host: stream-hub.com\r\n" +
								"Origin: http://www.stream-hub.com\r\n\r\n";
	}
	
	private static String httpHeaders() {
		StringBuilder headers = new StringBuilder();
		headers.append("Host: 127.0.0.1:8888").append("\r\n");
		headers.append("User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.1; en-GB; rv:1.9.0.8) Gecko/2009032609 Firefox/3.0.8 (.NET CLR 3.5.30729)").append("\r\n");
		headers.append("Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8").append("\r\n");
		headers.append("Accept-Language: en-gb,en;q=0.5").append("\r\n");
		headers.append("Accept-Encoding: gzip,deflate").append("\r\n");
		headers.append("Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7").append("\r\n");
		headers.append("Keep-Alive: 300").append("\r\n");
		headers.append("Connection: keep-alive").append("\r\n");
		headers.append("Referer: http://127.0.0.1:8156/test/index.html").append("\r\n");
		headers.append("\r\n");
		return headers.toString();
	}
}
