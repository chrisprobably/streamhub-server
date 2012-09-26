package com.streamhub;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.streamhub.api.Payload;
import com.streamhub.handler.Handler;
import com.streamhub.request.Request;

@RunWith(JMock.class)
public class ContextHandlerTest {
	private Mockery context;
	private Request request;
	private Connection connection;
	private StreamingSubscriptionManager subscriptionManager;
	private ContextHandler cometHandler;
	private Handler webSocketHandler;
	private Handler streamhubHandler;
	private Handler rootHandler;
	protected Payload payload;
	
	@Before
	public void setUp() {
		context = new Mockery() {{ setImposteriser(ClassImposteriser.INSTANCE); }};
		request = context.mock(Request.class);
		connection = context.mock(Connection.class);
		subscriptionManager = context.mock(StreamingSubscriptionManager.class);
		payload = context.mock(Payload.class);
		webSocketHandler = context.mock(Handler.class, "webSocketHandler");
		streamhubHandler = context.mock(Handler.class, "streamhubHandler");
		rootHandler = context.mock(Handler.class, "rootHandler");
		cometHandler = new ContextHandler(subscriptionManager);
		context.checking(new Expectations() {{
			allowing(connection).getChannel();
			allowing(request).getUrl();
			ignoring(connection).close();
			ignoring(request).isWebSocket();
		}});
	}
	
	@Test
	public void addingContext() throws Exception {
		final String streamhubContext = "/streamhub";
		cometHandler.addContext(streamhubContext, streamhubHandler);
		
		context.checking(new Expectations() {{
			allowing(connection).getRequest();
				will(returnValue(request));
			allowing(connection).setSelfClosing(true);
			one(request).getContext();
				will(returnValue(streamhubContext));
			ignoring(connection).isSelfClosing();
			ignoring(request).isKeepAliveConnection();
			one(streamhubHandler).handle(connection);
		}});		
		
		cometHandler.handle(connection);
	}
	
	@Test
	public void rootContextActsAsDefaultHandler() throws Exception {
		final String slashStarContext = "/*";
		cometHandler.addContext(slashStarContext, rootHandler);
		
		context.checking(new Expectations() {{
			allowing(connection).getRequest();
				will(returnValue(request));
			allowing(connection).setSelfClosing(true);				
			one(request).getContext();
				will(returnValue("/unknown-context"));
			ignoring(connection).isSelfClosing();
			ignoring(request).isKeepAliveConnection();
			one(rootHandler).handle(connection);
		}});		
		
		cometHandler.handle(connection);
	}
	
	@Test
	public void httpHandlerActsAsDefaultHandlerInAbsenceOfRootContext() throws Exception {
		cometHandler.contexts.remove("/");
		
		context.checking(new Expectations() {{
			allowing(connection).getRequest();
				will(returnValue(request));
			allowing(connection).setSelfClosing(true);				
			one(request).getContext();
				will(returnValue("/unknown-context"));
			ignoring(connection).isSelfClosing();
			ignoring(request).isKeepAliveConnection();
			one(connection).write(HttpHandler._404_NOT_FOUND);
		}});		
		
		cometHandler.handle(connection);
	}
	
	@Test
	public void addingMultipleContexts() throws Exception {
		final String streamhubContext = "/streamhub";
		final String rootContext = "/";
		cometHandler.addContext(streamhubContext, streamhubHandler);
		cometHandler.addContext(rootContext, rootHandler);
		
		context.checking(new Expectations() {{
			allowing(connection).getRequest();
				will(returnValue(request));
			one(request).getContext();
				will(returnValue(rootContext));
			ignoring(connection).isSelfClosing();
			ignoring(request).isKeepAliveConnection();
			one(rootHandler).handle(connection);
		}});		
		
		cometHandler.handle(connection);
	}
	
	@Test
	public void addingMultipleContextsUsingSlashStarSyntax() throws Exception {
		final String webSocketContext = "/streamhubws/*";
		final String streamhubContext = "/streamhub/*";
		final String rootContext = "/*";
		cometHandler.addContext(webSocketContext, webSocketHandler);
		cometHandler.addContext(streamhubContext, streamhubHandler);
		cometHandler.addContext(rootContext, rootHandler);
		Map<String, Handler> contexts = cometHandler.contexts;
		assertEquals(3, contexts.size());
		assertEquals(webSocketHandler, contexts.get("/streamhubws"));
		assertEquals(streamhubHandler, contexts.get("/streamhub"));
		assertEquals(rootHandler, contexts.get("/"));
	}
	
	@Test
	public void doesNotCloseSelfClosingConnections() throws Exception {
		context.checking(new Expectations() {{
			allowing(connection).getRequest();
				will(returnValue(request));
			allowing(connection).setSelfClosing(true);				
			ignoring(request).getContext();
			one(connection).isSelfClosing();
				will(returnValue(true));
			ignoring(connection).write(with(any(String.class)));
			one(request).isKeepAliveConnection();
				will(returnValue(false));
			never(connection).close();
		}});		
		
		cometHandler.handle(connection);
	}
}
