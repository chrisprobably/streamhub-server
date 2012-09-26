package com.streamhub.client;

import static org.junit.Assert.assertTrue;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.streamhub.Connection;
import com.streamhub.DirectRequestTest;
import com.streamhub.api.Client;
import com.streamhub.api.Payload;
import com.streamhub.client.CannotCreateClientException;
import com.streamhub.client.ClientFactory;
import com.streamhub.client.CometClient;
import com.streamhub.client.DirectClient;
import com.streamhub.handler.RawHandlerTest;
import com.streamhub.request.DirectRequest;
import com.streamhub.request.HttpRequest;
import com.streamhub.request.HttpRequestTest;
import com.streamhub.request.Request;
import com.streamhub.util.Browser;

@RunWith(JMock.class)
public class ClientFactoryTest {
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
	public void creatingCometClientFromRequest() throws Exception {
		context.checking(new Expectations() {{
			one(connection).readBytes(); will(returnValue(RawHandlerTest.createCometRequest().getBytes()));
		}});
		
		Client client = new ClientFactory(null).createFrom(HttpRequest.createFrom(connection));
		assertTrue(client instanceof CometClient);
	}
	
	@Test
	public void creatingWebSocketClientFromRequest() throws Exception {
		context.checking(new Expectations() {{
			one(connection).readBytes(); will(returnValue(HttpRequestTest.createWebSocketRequest().getBytes()));
		}});
		
		Client client = new ClientFactory(null).createFrom(HttpRequest.createFrom(connection));
		assertTrue(client instanceof WebSocketClient);
	}
	
	@Test
	public void creatingDirectClientFromRequest() throws Exception {
		Client client = new ClientFactory(null).createFrom(DirectRequest.createFrom(DirectRequestTest.createDirectConnectionRequest()));
		assertTrue(client instanceof DirectClient);
	}
	
	@Test(expected = CannotCreateClientException.class)
	public void throwsExceptionWhenRequestTypeUnknown() throws Exception {
		Request anonymousRequest = new Request() {
			public Browser getBrowser() {
				return null;
			}
			public String getDomain() {
				return null;
			}
			public String[] getSubscriptionTopics() {
				return null;
			}
			public String getUid() {
				return null;
			}
			public String getUrl() {
				return null;
			}
			public boolean isDisconnection() {
				return false;
			}
			public boolean isKeepAliveConnection() {
				return false;
			}
			public boolean isRequestIFrameConnection() {
				return false;
			}
			public boolean isResponseConnection() {
				return false;
			}
			public boolean isSubscription() {
				return false;
			}
			public Connection getConnection() {
				return null;
			}
			public boolean isIframeHtmlRequest() {
				return false;
			}
			public boolean isPublish() {
				return false;
			}
			public Payload getPayload() {
				return null;
			}
			public String getPublishTopic() {
				return null;
			}
			public boolean isUnSubscribe() {
				return false;
			}
			public boolean isPoll() {
				return false;
			}
			public boolean isCloseResponse() {
				return false;
			}
			public String getContext() {
				return null;
			}
			public String getProcessedUrl() {
				return null;
			}
			public boolean isWebSocket() {
				return false;
			}
		};
		
		new ClientFactory(null).createFrom(anonymousRequest);
	}
}
