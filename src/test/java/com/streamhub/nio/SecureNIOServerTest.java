package com.streamhub.nio;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;

import com.streamhub.StreamingServerTest;
import com.streamhub.StreamingServerTestCase;
import com.streamhub.api.JsonPayload;
import com.streamhub.api.PushServer;
import com.streamhub.tools.TestSSLContext;
import com.streamhub.util.HttpClient;

public class SecureNIOServerTest extends StreamingServerTestCase {
	private PushServer server;
	
	@Before
	public void setUp() throws Exception {
		super.setUp();
		server = new SecureNIOServer(444, TestSSLContext.newInstance());
		server.start();
		HttpClient.enableTrustAllCerts();
	}
	
	@After
	public void tearDown() throws Exception {
		server.stop();
		super.tearDown();
	}
	
	public void testConnectingViaHttps() throws Exception {
		try {
			HttpClient.get(new URL("https://localhost:444/"));
			fail("Expected 404");
		} catch (FileNotFoundException expected404) {
			
		}
	}
	
	public void testConnectingTwiceViaHttps() throws Exception {
		try {
			HttpClient.get(new URL("https://localhost:444/"));
			fail("Expected 404");
		} catch (FileNotFoundException expected404) {
		}
		
		try {
			HttpClient.get(new URL("https://localhost:444/404.html"));
			fail("Expected 404");
		} catch (FileNotFoundException expected404) {
		}
	}
	
	public void testServingStaticPages() throws Exception {
		server.addStaticContent(new File("src/test/resources/static"));
		String response = HttpClient.get(new URL("https://localhost:444/index.html"));
		assertEquals("<html>\r\n<head><title>Index</title></head>\r\n<body>Index</body>\r\n</html>", response);
	}
	
	public void testServingStaticPagesFromGivenContext() throws Exception {
		server.addStaticContent(new File("src/test/resources/static"), "/test");
		String response = HttpClient.get(new URL("https://localhost:444/test/index.html"));
		assertEquals("<html>\r\n<head><title>Index</title></head>\r\n<body>Index</body>\r\n</html>", response);
	}
	
	public void testServesIndexHtmlAsWelcomePageForDirectory() throws Exception {
		server.addStaticContent(new File("src/test/resources/static"));
		String response = HttpClient.get(new URL("https://localhost:444/"));
		assertEquals("<html>\r\n<head><title>Index</title></head>\r\n<body>Index</body>\r\n</html>", response);
	}

	public void testServesIndexHtmlAsWelcomePageForSubFolder() throws Exception {
		server.addStaticContent(new File("src/test/resources/static"));
		String response = HttpClient.get(new URL("https://localhost:444/subfolder/"));
		assertEquals("<html>\r\n<head><title>Subfolder Index</title></head>\r\n<body>Subfolder Index</body>\r\n</html>", response);
	}

	public void testServingStaticPagesFromSubFolders() throws Exception {
		server.addStaticContent(new File("src/test/resources/static"));
		String response = HttpClient.get(new URL("https://localhost:444/subfolder/bob.js"));
		assertEquals("var x = 45;", response);
	}

	public void testStaticServingIncludesHttpHeaders() throws Exception {
		server.addStaticContent(new File("src/test/resources/static"));
		Map<String, List<String>> headerFields = HttpClient.getHeaderFields(new URL("https://localhost:444/index.html"));
		assertEquals("StreamHub", headerFields.get("Server").get(0));
		assertEquals("close", headerFields.get("Connection").get(0));
	}
	
	public void testSetsContentTypeCorrectlyForImages() throws Exception {
		server.addStaticContent(new File("src/test/resources/static"));
		Map<String, List<String>> headerFields = HttpClient.getHeaderFields(new URL("https://localhost:444/alert.png"));
		assertEquals("image/png", headerFields.get("Content-Type").get(0));
		headerFields = HttpClient.getHeaderFields(new URL("https://localhost:444/arrow.gif"));
		assertEquals("image/gif", headerFields.get("Content-Type").get(0));
		headerFields = HttpClient.getHeaderFields(new URL("https://localhost:444/product.jpg"));
		assertEquals("image/jpeg", headerFields.get("Content-Type").get(0));
	}
	
	public void testSetsContentTypeCorrectlyForHtmlCssAndJs() throws Exception {
		server.addStaticContent(new File("src/test/resources/static"));
		Map<String, List<String>> headerFields = HttpClient.getHeaderFields(new URL("https://localhost:444/subfolder/index.html"));
		assertEquals("text/html", headerFields.get("Content-Type").get(0));
		headerFields = HttpClient.getHeaderFields(new URL("https://localhost:444/subfolder/bob.js"));
		assertEquals("application/x-javascript", headerFields.get("Content-Type").get(0));
		headerFields = HttpClient.getHeaderFields(new URL("https://localhost:444/subfolder/demo.css"));
		assertEquals("text/css", headerFields.get("Content-Type").get(0));
	}
	
	public void testSetsContentLength() throws Exception {
		server.addStaticContent(new File("src/test/resources/static"));
		int expectedLength = FileUtils.readFileToByteArray(new File("src/test/resources/static/alert.png")).length;
		Map<String, List<String>> headerFields = HttpClient.getHeaderFields(new URL("https://localhost:444/alert.png"));
		assertEquals(expectedLength, Integer.parseInt(headerFields.get("Content-Length").get(0)));
	}
	
	public void testReturnsCorrectBytesOfImage() throws Exception {
		server.addStaticContent(new File("src/test/resources/static"));
		byte[] expectedBytes = FileUtils.readFileToByteArray(new File("src/test/resources/static/alert.png"));
		byte[] actualBytes = HttpClient.getAsBytes(new URL("https://localhost:444/alert.png"));
		assertEquals(Arrays.toString(expectedBytes), Arrays.toString(actualBytes));
	}

	public void testConnectingToServerAsRequestIFrame() throws Exception {
		String response = HttpClient.get(new URL("https://localhost:444/streamhub/request/?domain=fred.com&uid=1"));
		assertEquals("<html><head><script>document.domain='fred.com';</script></head><body>request OK</body></html>",
				response);
		response = HttpClient.get(new URL("https://localhost:444/streamhub/request/?domain=127.0.0.1&uid=234324"));
		assertEquals("<html><head><script>document.domain='127.0.0.1';</script></head><body>request OK</body></html>",
				response);

		Map<String, List<String>> headers = HttpClient.getHeaderFields(new URL("https://localhost:444/streamhub/request/?uid=1"));
		assertEquals("StreamHub", headers.get("Server").get(0));
		assertEquals("text/html", headers.get("Content-Type").get(0));
	}

	public void testConnectingToServerAsResponseIFrame() throws Exception {
		String response = HttpClient.get(new URL("https://localhost:444/streamhub/response/?uid=1&domain=cheese.com"));
		assertEquals(
				"<html><head>"
						+ StreamingServerTest.TITLE
						+ "<script>document.domain='cheese.com';</script></head><body onload=\"window.parent.l()\"><script>x=window.parent.x;</script><script>x(\"response OK\");</script>",
				response);
		response = HttpClient.get(new URL("https://localhost:444/streamhub/response/?domain=bob.com&uid=235432"));
		assertEquals(
				"<html><head>"
						+ StreamingServerTest.TITLE
						+ "<script>document.domain='bob.com';</script></head><body onload=\"window.parent.l()\"><script>x=window.parent.x;</script><script>x(\"response OK\");</script>",
				response);

		Map<String, List<String>> headers = HttpClient
				.getHeaderFields(new URL("https://localhost:444/streamhub/response/?uid=1"));
		assertEquals("StreamHub", headers.get("Server").get(0));
		assertEquals("text/html", headers.get("Content-Type").get(0));
	}

	public void testGettingSpecialIFrameHtmlPage() throws Exception {
		String response = HttpClient.get(new URL("https://localhost:444/streamhub/iframe.html"));
		assertTrue(response.contains("<title>iframe</title>"));
	}

	public void testPublishingToTheServer() throws Exception {
		String response = HttpClient.get(new URL("https://localhost:444/streamhub/publish/?domain=bob.com&uid=234234&payload={\"topic\":\"chat\",\"message\":\"Hello%20how%20are%20you?\"}"));
		assertEquals(
				"<html><head><script>document.domain='bob.com';</script></head><body>publish OK</body></html>",
				response);
	}

	public void testNotPossibleToGetStreamingDataBeforeHttpHeaders() throws Exception {
		String response = HttpClient.get(new URL(
				"https://localhost:444/streamhub/subscribe/?domain=fred.com&uid=1&topic=AAPL,MSFT,WMT"));
		assertEquals(
				"<html><head><script>document.domain='fred.com';</script></head><body>subscription OK</body></html>",
				response);

		server.publish("AAPL", new JsonPayload("AAPL") {
			{
				addField("Something", "does not matter");
			}
		});

		response = HttpClient.get(new URL("https://localhost:444/streamhub/response/?uid=1&domain=cheese.com"));
		assertEquals(
				"<html><head>"
						+ StreamingServerTest.TITLE
						+ "<script>document.domain='cheese.com';</script></head><body onload=\"window.parent.l()\"><script>x=window.parent.x;</script><script>x(\"response OK\");</script><script>x({\"topic\":\"AAPL\",\"Something\":\"does not matter\"});</script>",
				response);
	}
	
	public void testBindingToDifferentAddress() throws Exception {
		PushServer diffServer = new SecureNIOServer(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 8735), TestSSLContext.newInstance());
		try {
			diffServer.start();
			HttpClient.get(new URL("https://127.0.0.1:8735/non-Existent"));
			fail("Expected 404");
		} catch (FileNotFoundException expected404) {
			
		} finally {
			diffServer.stop();
		}
	}

	public void testBindingToDifferentAddressWithNullStreamingAdapter() throws Exception {
		PushServer diffServer = new SecureNIOServer(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 8736), null, TestSSLContext.newInstance());
		try {
			diffServer.start();
			HttpClient.get(new URL("https://127.0.0.1:8736/non-Existent"));
			fail("Expected 404");
		} catch (FileNotFoundException expected404) {
			
		} finally {
			diffServer.stop();
		}
	}
	
	@SuppressWarnings("deprecation")
	public void testBindingToDifferentAddressWithNullStreamingAdapterAndCustomLoggingUrl() throws Exception {
		File loggingConfig = new File("conf/log4j.xml");
		URL loggingUrl = loggingConfig.toURL();
		PushServer diffServer = new SecureNIOServer(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 8738), null, loggingUrl, TestSSLContext.newInstance());
		try {
			diffServer.start();
			HttpClient.get(new URL("https://127.0.0.1:8738/non-Existent"));
			fail("Expected 404");
		} catch (FileNotFoundException expected404) {
			
		} finally {
			diffServer.stop();
		}
	}
}
