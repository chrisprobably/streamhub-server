package com.streamhub;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.streamhub.api.JsonPayload;
import com.streamhub.api.PushServer;
import com.streamhub.nio.NIOServer;
import com.streamhub.util.HttpClient;
import com.streamhub.util.Sleep;

public class StreamingServerTest extends StreamingServerTestCase {
	public static final String TITLE = "<title>StreamHub Push Page</title>";
	private PushServer server;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		server = new NIOServer(8888);
		try {
			server.start();
		} catch (UnrecoverableStartupException e) {
			// bind address exception - retry
			Sleep.millis(500);
			server.start();
		}
	}

	@Override
	protected void tearDown() throws Exception {
		server.setDefaultHeader("Content-Type", "text/html");
		server.setDefaultPushHeader("Content-Type", "text/html");
		server.setDefaultHeader("Content-Length", null);
		server.setDefaultPushHeader("Content-Length", "300000");
		server.addStaticContent(null, null);
		server.stop();
		super.tearDown();
	}

	public void testBindingToDifferentAddress() throws Exception {
		PushServer diffServer = new NIOServer(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 8734));
		try {
			diffServer.start();
			HttpClient.get(new URL("http://127.0.0.1:8734/"));
			fail("Expected FileNotFoundException as file should return 404");
		} catch(FileNotFoundException expected) {
			
		} finally {
			diffServer.stop();
		}
	}

	public void testConnectingToServer() throws Exception {
		try {
			HttpClient.get(new URL("http://localhost:8888/"));
			fail("Expected FileNotFoundException as file should return 404");
		} catch (FileNotFoundException expected) {
			
		}
	}
	
	public void testServingStaticPages() throws Exception {
		server.addStaticContent(new File("src/test/resources/static"));
		String response = HttpClient.get(new URL("http://localhost:8888/index.html"));
		assertEquals("<html>\r\n<head><title>Index</title></head>\r\n<body>Index</body>\r\n</html>", response);
	}
	
	public void testDoesNotServePagesFromParentDirectory() throws Exception {
		server.addStaticContent(new File("src/test/resources/static/subfolder"));
		try {
			HttpClient.get(new URL("http://localhost:8888/../secret.txt"));
			fail("Expected FileNotFoundException as file should return 404");
		} catch(FileNotFoundException expected) {
		
		}
	}
	
	public void testDoesNotServePagesFromParentDirectoryTwo() throws Exception {
		server.addStaticContent(new File("src/test/resources/static/subfolder"));
		try {
			HttpClient.get(new URL("http://localhost:8888/\\.\\./secret.txt"));
			fail("Expected FileNotFoundException as file should return 404");
		} catch(FileNotFoundException expected) {
			
		}
	}
	
	public void testDoesNotServePagesFromParentDirectoryThree() throws Exception {
		server.addStaticContent(new File("src/test/resources/static/subfolder"));
		try {
			HttpClient.get(new URL("http://localhost:8888/file:///me.txt"));
			fail("Expected FileNotFoundException as file should return 404");
		} catch(FileNotFoundException expected) {
			
		}
	}
	
	public void testDoesNotServePagesFromParentDirectoryFour() throws Exception {
		server.addStaticContent(new File("src/test/resources/static/subfolder"));
		try {
			HttpClient.get(new URL("http://localhost:8888/%2E%2E/secret.txt"));
			fail("Expected FileNotFoundException as file should return 404");
		} catch(FileNotFoundException expected) {
			
		}
	}
	
	public void testServingStaticPagesFromGivenContext() throws Exception {
		server.addStaticContent(new File("src/test/resources/static"), "/test");
		String response = HttpClient.get(new URL("http://localhost:8888/test/index.html"));
		assertEquals("<html>\r\n<head><title>Index</title></head>\r\n<body>Index</body>\r\n</html>", response);
	}
	
	public void testServesIndexHtmlAsWelcomePageForDirectory() throws Exception {
		server.addStaticContent(new File("src/test/resources/static"));
		String response = HttpClient.get(new URL("http://localhost:8888/"));
		assertEquals("<html>\r\n<head><title>Index</title></head>\r\n<body>Index</body>\r\n</html>", response);
	}

	public void testServesIndexHtmlAsWelcomePageForSubFolder() throws Exception {
		server.addStaticContent(new File("src/test/resources/static"));
		String response = HttpClient.get(new URL("http://localhost:8888/subfolder/"));
		assertEquals("<html>\r\n<head><title>Subfolder Index</title></head>\r\n<body>Subfolder Index</body>\r\n</html>", response);
	}

	public void testServingStaticPagesFromSubFolders() throws Exception {
		server.addStaticContent(new File("src/test/resources/static"));
		String response = HttpClient.get(new URL("http://localhost:8888/subfolder/bob.js"));
		assertEquals("var x = 45;", response);
	}

	public void testStaticServingIncludesHttpHeaders() throws Exception {
		server.addStaticContent(new File("src/test/resources/static"));
		Map<String, List<String>> headerFields = HttpClient.getHeaderFields(new URL("http://localhost:8888/index.html"));
		assertEquals("StreamHub", headerFields.get("Server").get(0));
		assertEquals("close", headerFields.get("Connection").get(0));
	}
	
	public void testSetsContentTypeCorrectlyForImages() throws Exception {
		server.addStaticContent(new File("src/test/resources/static"));
		Map<String, List<String>> headerFields = HttpClient.getHeaderFields(new URL("http://localhost:8888/alert.png"));
		assertEquals("image/png", headerFields.get("Content-Type").get(0));
		headerFields = HttpClient.getHeaderFields(new URL("http://localhost:8888/arrow.gif"));
		assertEquals("image/gif", headerFields.get("Content-Type").get(0));
		headerFields = HttpClient.getHeaderFields(new URL("http://localhost:8888/product.jpg"));
		assertEquals("image/jpeg", headerFields.get("Content-Type").get(0));
	}
	
	public void testSetsContentTypeCorrectlyForHtmlCssAndJs() throws Exception {
		server.addStaticContent(new File("src/test/resources/static"));
		Map<String, List<String>> headerFields = HttpClient.getHeaderFields(new URL("http://localhost:8888/subfolder/index.html"));
		assertEquals("text/html", headerFields.get("Content-Type").get(0));
		headerFields = HttpClient.getHeaderFields(new URL("http://localhost:8888/subfolder/bob.js"));
		assertEquals("application/x-javascript", headerFields.get("Content-Type").get(0));
		headerFields = HttpClient.getHeaderFields(new URL("http://localhost:8888/subfolder/demo.css"));
		assertEquals("text/css", headerFields.get("Content-Type").get(0));
	}
	
	public void testSetsContentLength() throws Exception {
		server.addStaticContent(new File("src/test/resources/static"));
		int expectedLength = FileUtils.readFileToByteArray(new File("src/test/resources/static/alert.png")).length;
		Map<String, List<String>> headerFields = HttpClient.getHeaderFields(new URL("http://localhost:8888/alert.png"));
		assertEquals(expectedLength, Integer.parseInt(headerFields.get("Content-Length").get(0)));
	}
	
	public void testReturnsCorrectBytesOfImage() throws Exception {
		server.addStaticContent(new File("src/test/resources/static"));
		byte[] expectedBytes = FileUtils.readFileToByteArray(new File("src/test/resources/static/alert.png"));
		byte[] actualBytes = HttpClient.getAsBytes(new URL("http://localhost:8888/alert.png"));
		assertEquals(Arrays.toString(expectedBytes), Arrays.toString(actualBytes));
	}

	public void testConnectingToServerAsRequestIFrame() throws Exception {
		String response = HttpClient.get(new URL("http://localhost:8888/streamhub/request/?domain=fred.com&uid=1"));
		assertEquals("<html><head><script>document.domain='fred.com';</script></head><body>request OK</body></html>",
				response);
		response = HttpClient.get(new URL("http://localhost:8888/streamhub/request/?domain=127.0.0.1&uid=234324"));
		assertEquals("<html><head><script>document.domain='127.0.0.1';</script></head><body>request OK</body></html>",
				response);

		Map<String, List<String>> headers = HttpClient.getHeaderFields(new URL("http://localhost:8888/streamhub/request/?uid=1"));
		assertEquals("StreamHub", headers.get("Server").get(0));
		assertEquals("text/html", headers.get("Content-Type").get(0));
	}

	public void testConnectingToServerAsResponseIFrame() throws Exception {
		String response = HttpClient.get(new URL("http://localhost:8888/streamhub/response/?uid=1&domain=cheese.com"));
		assertEquals(
				"<html><head>"
						+ TITLE
						+ "<script>document.domain='cheese.com';</script></head><body onload=\"window.parent.l()\"><script>x=window.parent.x;</script><script>x(\"response OK\");</script>",
				response);
		response = HttpClient.get(new URL("http://localhost:8888/streamhub/response/?domain=bob.com&uid=235432"));
		assertEquals(
				"<html><head>"
						+ TITLE
						+ "<script>document.domain='bob.com';</script></head><body onload=\"window.parent.l()\"><script>x=window.parent.x;</script><script>x(\"response OK\");</script>",
				response);

		Map<String, List<String>> headers = HttpClient
				.getHeaderFields(new URL("http://localhost:8888/streamhub/response/?uid=1"));
		assertEquals("StreamHub", headers.get("Server").get(0));
		assertEquals("text/html", headers.get("Content-Type").get(0));
	}
	
	public void testSettingDefaultPushHeader() throws Exception {
		server.setDefaultPushHeader("Content-Type", "text/html;charset=euc-kr");
		Map<String, List<String>> headers = HttpClient
		.getHeaderFields(new URL("http://localhost:8888/streamhub/response/?uid=1"));
		assertEquals("text/html;charset=euc-kr", headers.get("Content-Type").get(0));
	}
	
	public void testSettingDefaultHeader() throws Exception {
		server.setDefaultHeader("Content-Type", "text/html;charset=euc-kr");
		Map<String, List<String>> headers = HttpClient
		.getHeaderFields(new URL("http://localhost:8888/streamhub/subscribe/?domain=fred.com&uid=1&topic=AAPL,MSFT,WMT"));
		List<String> contentType = headers.get("Content-Type");
		assertEquals(1, contentType.size());
		assertEquals("text/html;charset=euc-kr", contentType.get(0));
	}
	
	public void testSettingPushContentLength() throws Exception {
		server.setDefaultPushHeader("Content-Length", "60000");
		Map<String, List<String>> headers = HttpClient
		.getHeaderFields(new URL("http://localhost:8888/streamhub/response/?uid=1"));
		assertEquals("60000", headers.get("Content-Length").get(0));
	}
	
	public void testSettingContentLength() throws Exception {
		server.setDefaultHeader("Content-Length", "2341");
		Map<String, List<String>> headers = HttpClient
		.getHeaderFields(new URL("http://localhost:8888/streamhub/subscribe/?domain=fred.com&uid=1&topic=AAPL,MSFT,WMT"));
		assertEquals("2341", headers.get("Content-Length").get(0));
	}
	
	public void testSettingDefaultPushHeaderUsingMixedCase() throws Exception {
		server.setDefaultPushHeader("coNtent-type", "text/html;charset=euc-kr");
		Map<String, List<String>> headers = HttpClient
		.getHeaderFields(new URL("http://localhost:8888/streamhub/response/?uid=1"));
		List<String> contentType = headers.get("Content-Type");
		assertEquals(1, contentType.size());
		assertEquals("text/html;charset=euc-kr", contentType.get(0));
	}
	
	public void testSettingDefaultHeaderUsingMixedCase() throws Exception {
		server.setDefaultHeader("CONTENT-TYPE", "text/html;charset=euc-kr");
		Map<String, List<String>> headers = HttpClient
		.getHeaderFields(new URL("http://localhost:8888/streamhub/subscribe/?domain=fred.com&uid=1&topic=AAPL,MSFT,WMT"));
		List<String> contentType = headers.get("Content-Type");
		assertEquals(1, contentType.size());
		assertEquals("text/html;charset=euc-kr", contentType.get(0));
	}
	
	public void testSettingNewPushHeader() throws Exception {
		server.setDefaultPushHeader("Set-Cookie", "UserID=JohnDoe; Max-Age=3600; Version=1");
		Map<String, List<String>> headers = HttpClient
		.getHeaderFields(new URL("http://localhost:8888/streamhub/response/?uid=1"));
		assertEquals("UserID=JohnDoe; Max-Age=3600; Version=1", headers.get("Set-Cookie").get(0));
	}
	
	public void testSettingNewHeader() throws Exception {
		server.setDefaultHeader("Set-Cookie", "UserID=JohnDoe; Max-Age=3600; Version=1");
		Map<String, List<String>> headers = HttpClient
		.getHeaderFields(new URL("http://localhost:8888/streamhub/subscribe/?domain=fred.com&uid=1&topic=AAPL,MSFT,WMT"));
		assertEquals("UserID=JohnDoe; Max-Age=3600; Version=1", headers.get("Set-Cookie").get(0));
	}
	
	public void testRemovingDefaultPushHeaderBySettingToNull() throws Exception {
		server.setDefaultPushHeader("Content-Type", null);
		Map<String, List<String>> headers = HttpClient
		.getHeaderFields(new URL("http://localhost:8888/streamhub/response/?uid=1"));
		List<String> contentType = headers.get("Content-Type");
		assertNull(contentType);
	}
	
	public void testRemovingDefaultHeaderBySettingToNull() throws Exception {
		server.setDefaultHeader("Content-Type", null);
		Map<String, List<String>> headers = HttpClient
		.getHeaderFields(new URL("http://localhost:8888/streamhub/subscribe/?domain=fred.com&uid=1&topic=AAPL,MSFT,WMT"));
		List<String> contentType = headers.get("Content-Type");
		assertNull(contentType);
	}

	public void testGettingSpecialIFrameHtmlPage() throws Exception {
		String response = HttpClient.get(new URL("http://localhost:8888/streamhub/iframe.html"));
		assertTrue(response.contains("<title>iframe</title>"));
	}

	public void testPublishingToTheServer() throws Exception {
		String response = HttpClient.get(new URL("http://localhost:8888/streamhub/publish/?domain=bob.com&uid=234234&payload={\"topic\":\"chat\",\"message\":\"Hello%20how%20are%20you?\"}"));
		assertEquals(
				"<html><head><script>document.domain='bob.com';</script></head><body>publish OK</body></html>",
				response);
	}

	public void testNotPossibleToGetStreamingDataBeforeHttpHeaders() throws Exception {
		String response = HttpClient.get(new URL(
				"http://localhost:8888/streamhub/subscribe/?domain=fred.com&uid=1&topic=AAPL,MSFT,WMT"));
		assertEquals(
				"<html><head><script>document.domain='fred.com';</script></head><body>subscription OK</body></html>",
				response);

		server.publish("AAPL", new JsonPayload("AAPL") {
			{
				addField("Something", "does not matter");
			}
		});

		response = HttpClient.get(new URL("http://localhost:8888/streamhub/response/?uid=1&domain=cheese.com"));
		assertEquals(
				"<html><head>"
						+ TITLE
						+ "<script>document.domain='cheese.com';</script></head><body onload=\"window.parent.l()\"><script>x=window.parent.x;</script><script>x(\"response OK\");</script><script>x({\"topic\":\"AAPL\",\"Something\":\"does not matter\"});</script>",
				response);
	}
}
