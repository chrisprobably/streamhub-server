package com.streamhub.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.streamhub.StreamingServerTestCase;
import com.streamhub.UnrecoverableStartupException;
import com.streamhub.nio.NIOServer;

public class StreamUtilsTest extends StreamingServerTestCase {
	private static final int SERVER_PORT = 8888;
	private static final String GET_REQUEST = "GET /index.html HTTP/1.1\r\n"
			+ "User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/525.13 (KHTML, like Gecko) Version/3.1 Safari/525.13.3\r\n" + "Accept-Encoding: gzip, deflate\r\n"
			+ "Referer: http://127.0.0.1:8156/test/index.html\r\n" + "Accept: text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5\r\n"
			+ "Accept-Language: en-US\r\n" + "Connection: keep-alive\r\n" + "Host: 127.0.0.1:8888\r\n\r\n";
	private NIOServer server;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		server = new NIOServer(SERVER_PORT);
		try {
			server.addStaticContent(new File("src/test/resources/static"));
			server.start();
		} catch (UnrecoverableStartupException e) {
			// bind address exception - retry
			Sleep.millis(500);
			server.start();
		}
	}

	@Override
	public void tearDown() throws Exception {
		Sleep.millis(500);
		server.addStaticContent(null, null);
		server.stop();
		super.tearDown();
	}

	public void testConvertsInputStreamToString() throws Exception {
		String expected = "hello";
		InputStream inputStream = new ByteArrayInputStream(expected.getBytes());
		assertEquals(expected, StreamUtils.toString(inputStream));
	}

	public void testGrabsRemoteContentUsingNio() throws Exception {
		InetSocketAddress targetAddress = new InetSocketAddress(InetAddress.getLocalHost(), SERVER_PORT);
		byte[] bytes = StreamUtils.nioToBytes(targetAddress, GET_REQUEST);
		String response = new String(bytes);
		assertEquals("HTTP/1.1 200 OK\r\nServer: StreamHub\r\nConnection: close\r\nContent-Type: text/html\r\n\r\n<html>\r\n<head><title>Index</title></head>\r\n<body>Index</body>\r\n</html>",
				response);
	}

//	public void testGrabsRemoteContentFromTomcatUsingNio() throws Exception {
//		InetSocketAddress targetAddress = new InetSocketAddress("192.168.1.66", 8080);
//		long startTime = System.currentTimeMillis();
//		byte[] bytes = StreamUtils.nioToBytes(targetAddress, GET_REQUEST);
//		long elapsed = System.currentTimeMillis() - startTime;
//		System.out.println("nioToBytes took " + elapsed + "ms");
//		String response = new String(bytes);
//		assertEquals(8005, response.length());
//		assertEquals(true, response.endsWith("</html>\n"));
//	}
//
//	public void testGrabsRemoteImageFromTomcatUsingNio() throws Exception {
//		InetSocketAddress targetAddress = new InetSocketAddress("192.168.1.66", 8080);
//		long startTime = System.currentTimeMillis();
//		byte[] bytes = StreamUtils.nioToBytes(targetAddress, GET_IMAGE_REQUEST);
//		long elapsed = System.currentTimeMillis() - startTime;
//		System.out.println("nioToBytes took " + elapsed + "ms");
//		assertEquals(2162, bytes.length);
//	}
//
//	public void testGrabsRemotePngImageFromTomcatUsingNio() throws Exception {
//		InetSocketAddress targetAddress = new InetSocketAddress("192.168.1.66", 8080);
//		long startTime = System.currentTimeMillis();
//		byte[] bytes = StreamUtils.nioToBytes(targetAddress, GET_PNG_REQUEST);
//		long elapsed = System.currentTimeMillis() - startTime;
//		System.out.println("nioToBytes took " + elapsed + "ms");
//		assertEquals(1238, bytes.length);
//	}
//
//	public void testGrabsRemoteJavaScriptFromTomcatUsingNio() throws Exception {
//		InetSocketAddress targetAddress = new InetSocketAddress("192.168.1.66", 8080);
//		long startTime = System.currentTimeMillis();
//		byte[] bytes = StreamUtils.nioToBytes(targetAddress, GET_JS_REQUEST);
//		long elapsed = System.currentTimeMillis() - startTime;
//		System.out.println("nioToBytes took " + elapsed + "ms");
//		String response = new String(bytes);
//		assertEquals(192866, response.length());
//		// assertEquals(true, response.endsWith("</html>\n"));
//	}
//	
//	public void testGrabsRemoteJSPFromTomcatUsingNio() throws Exception {
//		InetSocketAddress targetAddress = new InetSocketAddress("192.168.1.66", 8080);
//		long startTime = System.currentTimeMillis();
//		byte[] bytes = StreamUtils.nioToBytes(targetAddress, GET_JSP_REQUEST);
//		long elapsed = System.currentTimeMillis() - startTime;
//		System.out.println("nioToBytes took " + elapsed + "ms");
//		String response = new String(bytes);
//		//System.out.println("response: [" + response + "]");
//		assertEquals(22680, response.length());
//		assertEquals(true, response.contains("</html>"));
//	}
//	
//	public void testGrabsDiffRemoteJSPFromTomcatUsingNio() throws Exception {
//		InetSocketAddress targetAddress = new InetSocketAddress("192.168.1.66", 8080);
//		long startTime = System.currentTimeMillis();
//		byte[] bytes = StreamUtils.nioToBytes(targetAddress, GET_JSP_REQUEST_2);
//		long elapsed = System.currentTimeMillis() - startTime;
//		System.out.println("nioToBytes took " + elapsed + "ms");
//		String response = new String(bytes);
//		assertEquals(3495, response.length());
//		assertEquals(true, response.contains("</html>"));
//	}
//	
//	public void testGrabsTinyRemoteJSPFromTomcatUsingNio() throws Exception {
//		InetSocketAddress targetAddress = new InetSocketAddress("192.168.1.66", 8080);
//		long startTime = System.currentTimeMillis();
//		byte[] bytes = StreamUtils.nioToBytes(targetAddress, GET_JSP_REQUEST_3);
//		long elapsed = System.currentTimeMillis() - startTime;
//		System.out.println("nioToBytes took " + elapsed + "ms");
//		String response = new String(bytes);
//		assertEquals(1402, response.length());
//		assertEquals(true, response.contains("</html>"));
//	}
}
