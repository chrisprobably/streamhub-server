package com.streamhub;

import java.nio.ByteBuffer;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.streamhub.api.Payload;
import com.streamhub.request.Request;

@RunWith(JMock.class)
public class WebSocketHandlerTest {
	private Request request;
	private Mockery context;
	private Connection connection;
	private StreamingSubscriptionManager subscriptionManager;
	private ContextHandler cometHandler;
	protected Payload payload;
	
	@Before
	public void setUp() {
		context = new Mockery() {{ setImposteriser(ClassImposteriser.INSTANCE); }};
		connection = context.mock(Connection.class);
		subscriptionManager = context.mock(StreamingSubscriptionManager.class);
		payload = context.mock(Payload.class);
		request = context.mock(Request.class);
		cometHandler = new ContextHandler(subscriptionManager);
		context.checking(new Expectations() {{
			allowing(connection).getChannel();
		}});
	}
	
	@Test
	public void handlesWebSocketConnection() throws Exception {
		final String rawHttpRequest = "GET /streamhubws/ HTTP/1.1\r\n"+
								"Upgrade: WebSocket\r\n"+
								"Connection: Upgrade\r\n"+
								"Host: localhost:7979\r\n"+
								"Origin: http://localhost:7979\r\n\r\n";
		final String expectedResponse = "HTTP/1.1 101 Web Socket Protocol Handshake\r\n" +
								"Upgrade: WebSocket\r\n" +
								"Connection: Upgrade\r\n" +
								"WebSocket-Origin: http://localhost:7979\r\n" +
								"WebSocket-Location: ws://localhost:7979/streamhubws/\r\n" +
								"WebSocket-Protocol: StreamHubWS\r\n" +
								"Server: StreamHub\r\n\r\n";
		context.checking(new Expectations() {{
			allowing(connection).getRequest();
				will(returnValue(request));
			one(request).getContext();
				will(returnValue("/streamhubws"));
			one(request).getUrl();
				will(returnValue("/streamhubws/"));
			allowing(connection).readBytes();
				will(returnValue(rawHttpRequest.getBytes()));
			one(connection).setReadableEventInterceptor(with(any(Connection.class)));
			one(connection).write(ByteBuffer.wrap(expectedResponse.getBytes()));
			one(request).isKeepAliveConnection();
				will(returnValue(true));
		}});		
		
		cometHandler.handle(connection);
	}
	
	@Test
	public void handlesSecureWebSocketConnection() throws Exception {
		final String rawHttpRequest = "GET /streamhubws/ HTTP/1.1\r\n"+
		"Upgrade: WebSocket\r\n"+
		"Connection: Upgrade\r\n"+
		"Host: localhost:7979\r\n"+
		"Origin: http://localhost:7979\r\n"+
		"Sec-WebSocket-Key1: 18x 6]8vM;54 *(5:  {   U1]8  z [  8\r\n"+
        "Sec-WebSocket-Key2: 1_ tx7X d  <  nw  334J702) 7]o}` 0\r\n\r\n" + 
        "Tm[K T2u";
		final String expectedResponse = "HTTP/1.1 101 Web Socket Protocol Handshake\r\n" +
		"Upgrade: WebSocket\r\n" +
		"Connection: Upgrade\r\n" +
		"Server: StreamHub\r\n" + 
		"Sec-WebSocket-Origin: http://localhost:7979\r\n" +
		"Sec-WebSocket-Location: ws://localhost:7979/streamhubws/\r\n" +
		"Sec-WebSocket-Protocol: StreamHubWS\r\n\r\n" +
		"fQJ,fN/4F4!~K~MH";
		context.checking(new Expectations() {{
			allowing(connection).getRequest();
				will(returnValue(request));
			one(request).getContext();
				will(returnValue("/streamhubws"));
			one(request).getUrl();
				will(returnValue("/streamhubws/"));
			allowing(connection).readBytes();
				will(returnValue(rawHttpRequest.getBytes()));
			one(connection).setReadableEventInterceptor(with(any(Connection.class)));
			one(connection).write(ByteBuffer.wrap(expectedResponse.getBytes()));
			one(request).isKeepAliveConnection();
				will(returnValue(true));
		}});		
		
		cometHandler.handle(connection);
	}

	
}
