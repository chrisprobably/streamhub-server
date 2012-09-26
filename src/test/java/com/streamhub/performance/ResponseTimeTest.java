package com.streamhub.performance;

import java.net.URL;

import com.streamhub.StreamingServerTestCase;
import com.streamhub.api.PushServer;
import com.streamhub.nio.NIOServer;
import com.streamhub.util.HttpClient;
import com.streamhub.util.Sleep;

public class ResponseTimeTest extends StreamingServerTestCase {
	public static final String TITLE = "<title>StreamHub Push Page</title>";
	private PushServer server;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		server = new NIOServer(8888);
		server.start();
		// Allow the server some time to initialize since these are performance tests
		Sleep.seconds(3);
	}

	@Override
	protected void tearDown() throws Exception {
		server.stop();
		super.tearDown();
	}

	public void testResponseTimeOfRequestIFrameIsLessThanSeventyMillis() throws Exception {
		long startTime = System.currentTimeMillis();
		String response = HttpClient.get(new URL("http://localhost:8888/streamhub/request/?domain=fred.com&uid=1"));
		long responseTimeMillis = System.currentTimeMillis() - startTime;
		System.out.println("responseTimeMillis=" + responseTimeMillis);
		assertEquals("<html><head><script>document.domain='fred.com';</script></head><body>request OK</body></html>", response);
		assertTrue(responseTimeMillis < 70);
	}
	
	public void testResponseTimeOfSubscriptionIsLessThanFiftyMillis() throws Exception {
		long startTime = System.currentTimeMillis();
		String response = HttpClient.get(new URL("http://localhost:8888/streamhub/subscribe/?domain=fred.com&uid=1&topic=AAPL"));
		long responseTimeMillis = System.currentTimeMillis() - startTime;
		System.out.println("responseTimeMillis=" + responseTimeMillis);
		assertEquals("<html><head><script>document.domain='fred.com';</script></head><body>subscription OK</body></html>", response);
		assertTrue(responseTimeMillis < 50);
	}
	
	public void testResponseTimeOfDisconnectionIsLessThanFiftyMillis() throws Exception {
		long startTime = System.currentTimeMillis();
		String response = HttpClient.get(new URL("http://localhost:8888/streamhub/disconnect/?domain=fred.com&uid=1"));
		long responseTimeMillis = System.currentTimeMillis() - startTime;
		System.out.println("responseTimeMillis=" + responseTimeMillis);
		assertEquals("<html><head><script>document.domain='fred.com';</script></head><body>disconnection OK</body></html>", response);
		assertTrue(responseTimeMillis < 50);
	}
	
	public void testResponseTimeOfSpecialIFrameHtmlPageIsLessThanThirtyMillis() throws Exception {
		long startTime = System.currentTimeMillis();
		String response = HttpClient.get(new URL("http://localhost:8888/streamhub/iframe.html"));
		long responseTimeMillis = System.currentTimeMillis() - startTime;
		System.out.println("responseTimeMillis=" + responseTimeMillis);
		assertTrue(response.contains("<title>iframe</title>"));
		assertTrue(responseTimeMillis < 30);
	}
	
	public void testResponseTimeOfResponseIFrameIsLessThanThirtyMillis() throws Exception {
		long startTime = System.currentTimeMillis();
		String response = HttpClient.get(new URL("http://localhost:8888/streamhub/response/?uid=1&domain=cheese.com"));
		long responseTimeMillis = System.currentTimeMillis() - startTime;
		System.out.println("responseTimeMillis=" + responseTimeMillis);
		assertEquals("<html><head>" + TITLE + "<script>document.domain='cheese.com';</script></head><body onload=\"window.parent.l()\"><script>x=window.parent.x;</script><script>x(\"response OK\");</script>", response);
		assertTrue(responseTimeMillis < 30);
	}
}
