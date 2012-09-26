package com.streamhub.handler;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.jmock.Expectations;
import org.jmock.Mockery;

import com.streamhub.Connection;
import com.streamhub.StreamingServerTestCase;
import com.streamhub.UnrecoverableStartupException;
import com.streamhub.api.PushServer;
import com.streamhub.nio.NIOServer;
import com.streamhub.request.Request;
import com.streamhub.util.Sleep;

public class ForwardingHandlerTest extends StreamingServerTestCase {
	private Handler forwardingHandler;
	private PushServer server;
	private Mockery context;
	private Request request;
	private Connection connection;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		server = new NIOServer(8888);
		server.addStaticContent(new File("src/test/resources/static"));
		try {
			server.start();
		} catch (UnrecoverableStartupException e) {
			// bind address exception - retry
			Sleep.millis(500);
			server.start();
		}
		InetSocketAddress localhost = new InetSocketAddress(InetAddress.getLocalHost(), 8888);
		forwardingHandler = new ForwardingHandler(localhost);
		context = new Mockery();
		request = context.mock(Request.class);
		connection = context.mock(Connection.class);
	}
	
	@Override
	protected void tearDown() throws Exception {
		server.addStaticContent(null, null);
		server.stop();
		super.tearDown();
	}

	public void testForwardingHandlerForwardsHttpRequest() throws Exception {
		final String requestData = "GET /index.html HTTP/1.1\r\n" +
									"User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/525.13 (KHTML, like Gecko) Version/3.1 Safari/525.13.3\r\n" +
									"Accept-Encoding: gzip, deflate\r\n" +
									"Referer: http://127.0.0.1:8156/test/index.html\r\n" +
									"Accept: text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5\r\n" +
									"Accept-Language: en-US\r\n" +
									"Connection: keep-alive\r\n" +
									"Host: 127.0.0.1:8888\r\n\r\n";
		context.checking(new Expectations() {{
			allowing(connection).getRequest();
				will(returnValue(request));
			one(connection).readBytes();
				will(returnValue(requestData.getBytes()));
			one(connection).write(with(any(ByteBuffer.class)));
			one(connection).setSelfClosing(true);
		}});		
		forwardingHandler.handle(connection);
		context.assertIsSatisfied();
	}
	
	public void testForwardingHandlerSetsSelfClosing() throws Exception {
		final String requestData = "GET /index.html HTTP/1.1\r\n" +
		"User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/525.13 (KHTML, like Gecko) Version/3.1 Safari/525.13.3\r\n" +
		"Accept-Encoding: gzip, deflate\r\n" +
		"Referer: http://127.0.0.1:8156/test/index.html\r\n" +
		"Accept: text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5\r\n" +
		"Accept-Language: en-US\r\n" +
		"Connection: keep-alive\r\n" +
		"Host: 127.0.0.1:8888\r\n\r\n";
		context.checking(new Expectations() {{
			allowing(connection).getRequest();
				will(returnValue(request));
			one(connection).readBytes();
				will(returnValue(requestData.getBytes()));
			one(connection).write(with(any(ByteBuffer.class)));
			one(connection).setSelfClosing(true);
		}});		
		forwardingHandler.handle(connection);
		context.assertIsSatisfied();
	}
	
	public void testForwardingHandlerForwardsMultipleHttpRequests() throws Exception {
		final String requestData = "GET /index.html HTTP/1.1\r\n" +
									"User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/525.13 (KHTML, like Gecko) Version/3.1 Safari/525.13.3\r\n" +
									"Accept-Encoding: gzip, deflate\r\n" +
									"Referer: http://127.0.0.1:8156/test/index.html\r\n" +
									"Accept: text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5\r\n" +
									"Accept-Language: en-US\r\n" +
									"Connection: keep-alive\r\n" +
									"Host: 127.0.0.1:8888\r\n\r\n";
		context.checking(new Expectations() {{
			allowing(connection).getRequest();
				will(returnValue(request));
			exactly(2).of(connection).readBytes();
				will(returnValue(requestData.getBytes()));
			exactly(2).of(connection).write(with(any(ByteBuffer.class)));
			exactly(2).of(connection).setSelfClosing(true);
		}});		
		forwardingHandler.handle(connection);
		forwardingHandler.handle(connection);
		context.assertIsSatisfied();
	}
}
