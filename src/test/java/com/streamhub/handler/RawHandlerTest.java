package com.streamhub.handler;

import static org.junit.Assert.assertEquals;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.streamhub.Connection;
import com.streamhub.DirectHandler;
import com.streamhub.request.HttpRequestTest;

@RunWith(JMock.class)
public class RawHandlerTest {
	private static final String DIRECT_REQUEST = DirectHandler.MAGIC_DIRECT_CONNECTION_STRING + "some stuff";
	private Mockery context;
	private Handler cometHandler;
	private Handler directHandler;
	private Connection connection;
	
	@Before
	public void setUp() {
		context = new Mockery() {{
			setImposteriser(ClassImposteriser.INSTANCE);
		}};
		connection = context.mock(Connection.class);
		cometHandler = context.mock(Handler.class);
		directHandler = context.mock(Handler.class, "directHandler");
	}
	
	@Test
	public void rawHandlerDirectsNIOCometConnectionToCometHandler() throws Exception {
		Handler handler = new RawHandler(cometHandler, directHandler);
		
		context.checking(new Expectations() {{
			one(connection).peekBytes(); will(returnValue("GE".getBytes()));
			one(cometHandler).handle(connection);
		}});
		
		handler.handle(connection);
	}
	
	@Test
	public void rawHandlerClosesConnectionIfPeekIsBlank() throws Exception {
		Handler handler = new RawHandler(cometHandler, directHandler);
		
		context.checking(new Expectations() {{
			one(connection).peekBytes(); will(returnValue("".getBytes()));
			one(connection).close();
		}});
		
		handler.handle(connection);
	}
	
	@Test
	public void rawHandlerDirectsNIODirectConnectionToDirectHandler() throws Exception {
		Handler handler = new RawHandler(cometHandler, directHandler);
		
		context.checking(new Expectations() {{
			one(connection).peekBytes(); will(returnValue(DirectHandler.DIRECT_MESSAGE_SEPARATOR.getBytes()));
			one(directHandler).handle(connection);
		}});
		
		handler.handle(connection);
	}

	@Test
	public void closesConnectionIfExceptionOccurs() throws Exception {
		Handler handler = new RawHandler(cometHandler, directHandler);
		
		context.checking(new Expectations() {{
			one(connection).peekBytes(); will(throwException(new Exception()));
			one(connection).close();
		}});
		
		handler.handle(connection);
	}
	
	@Test
	public void rawHandlerResetsConnectionAfterPeek() throws Exception {
		final StringBuilder testHandlerInput = new StringBuilder();
		Handler testHandler = new Handler() {
			public void handle(Connection connection) {
				try {
					testHandlerInput.append(new String(connection.readBytes()));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		Handler handler = new RawHandler(cometHandler, testHandler);
		
		context.checking(new Expectations() {{
			one(connection).peekBytes();
				will(returnValue(DirectHandler.DIRECT_MESSAGE_SEPARATOR.getBytes()));
			allowing(connection).readBytes(); will(returnValue(DIRECT_REQUEST.getBytes()));
		}});
		
		handler.handle(connection);
		assertEquals(DIRECT_REQUEST, testHandlerInput.toString());
	}

	public static String createCometRequest() {
		return HttpRequestTest.buildHttpRequest("/request/?uid=1&domain=127.0.0.1");
	}
}
