package com.streamhub;

import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.streamhub.api.PushServer;
import com.streamhub.nio.NIOServer;
import com.streamhub.tools.proxy.Proxy;
import com.streamhub.util.HttpClient;
import com.streamhub.util.StaticHttpServer;

public class ProxyTest extends StreamingServerTestCase {
	private PushServer streamingServer;
	private StaticHttpServer httpServer;
	private Proxy staticServerProxy;
	private Proxy streamingServerProxy;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		streamingServer = new NIOServer(9962);
		httpServer = new StaticHttpServer(9932);
		staticServerProxy = new Proxy(9942, httpServer.getUrl());
		staticServerProxy.start();
		streamingServerProxy = new Proxy(9952, new URL("http://localhost:9962/"));
		streamingServerProxy.start();
		streamingServer.start();
		httpServer.start();
	}

	@After
	public void tearDown() throws Exception {
		httpServer.stop();
		streamingServer.stop();
		streamingServerProxy.stop();
		staticServerProxy.stop();
		super.tearDown();
	}

	@Test
	public void testGettingStaticResourceThroughProxy() throws Exception {
		String page = HttpClient.get(new URL("http://localhost:9942/test/"));
		assertTrue(page.contains("Test Page"));
	}

	@Test
	public void testConnectingToStreamingServerThroughProxy() throws Exception {
		String page = HttpClient.get(new URL("http://localhost:9952/streamhub/request/?uid=1&domain=sdfd.com"));
		assertTrue(page.contains("request OK"));
	}

	@Test
	public void testIsStoppedBehavesWhenStoppingAndStarting() throws Exception {
		staticServerProxy.stop();
		assertTrue(staticServerProxy.isStopped());
		staticServerProxy.start();
		assertFalse(staticServerProxy.isStopped());
	}
}
