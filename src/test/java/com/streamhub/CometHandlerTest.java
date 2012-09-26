package com.streamhub;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.streamhub.api.Payload;
import com.streamhub.client.IStreamingClient;
import com.streamhub.request.Request;
import com.streamhub.util.Browser;

@RunWith(JMock.class)
public class CometHandlerTest {
	private Mockery context;
	private Request request;
	private Connection connection;
	private StreamingSubscriptionManager subscriptionManager;
	private ContextHandler cometHandler;
	private String queuedMessages;
	private IStreamingClient client;
	protected Payload payload;
	
	@Before
	public void setUp() {
		context = new Mockery() {{ setImposteriser(ClassImposteriser.INSTANCE); }};
		request = context.mock(Request.class);
		connection = context.mock(Connection.class);
		subscriptionManager = context.mock(StreamingSubscriptionManager.class);
		client = context.mock(IStreamingClient.class);
		payload = context.mock(Payload.class);
		queuedMessages = "";
		cometHandler = new ContextHandler(subscriptionManager);
		context.checking(new Expectations() {{
			allowing(connection).getChannel();
			allowing(request).getContext();
				will(returnValue("/streamhub"));
			ignoring(request).isWebSocket();				
		}});
	}
	
	@Test
	public void handlesSpecialPage() throws Exception {
		final String expectedResponse = SpecialPages.iframeHttpResponse();
		
		context.checking(new Expectations() {{
			allowing(connection).getRequest();
				will(returnValue(request));
			allowing(request).isIframeHtmlRequest();
				will(returnValue(true));
			allowing(request).getUrl();
				will(returnValue("/streamhub/iframe.html"));
			allowing(request).isKeepAliveConnection();
				will(returnValue(false));
			allowing(connection).isSelfClosing();
				will(returnValue(false));
			one(connection).write(expectedResponse);
			one(connection).close();
		}});		
		
		cometHandler.handle(connection);
	}
	
	@Test
	public void handlesDisconnection() throws Exception {
		final String domain = "bob.com";
		final Browser browser = Browser.FF3;
		final String expectedResponse = ResponseFactory.disconnectionResponse(domain, browser);
		
		context.checking(new Expectations() {{
			allowing(request).isIframeHtmlRequest();
			allowing(connection).getRequest();
				will(returnValue(request));
			ignoring(request).getUrl();
			one(request).getDomain();
				will(returnValue(domain));
			one(request).getBrowser();
				will(returnValue(browser));
			allowing(request).isDisconnection();
				will(returnValue(true));
			allowing(request).isKeepAliveConnection();
				will(returnValue(false));
			allowing(connection).isSelfClosing();
				will(returnValue(false));				
			one(subscriptionManager).findOrCreateClient(request);
				will(returnValue(client));
			one(connection).write(expectedResponse);
			one(connection).close();
			one(client).disconnect();
		}});		
		
		cometHandler.handle(connection);
	}
	
	@Test
	public void handlesSubscription() throws Exception {
		final String domain = "bob.com";
		final Browser browser = Browser.FF3;
		final String expectedResponse = ResponseFactory.subscriptionResponse(domain, browser);
		
		context.checking(new Expectations() {{
			ignoring(request).getUrl();
			ignoring(request).isIframeHtmlRequest();
			ignoring(request).isDisconnection();
			one(request).getDomain();
				will(returnValue(domain));
			one(request).getBrowser();
				will(returnValue(browser));
			allowing(connection).getRequest();
				will(returnValue(request));
			allowing(request).isSubscription();
				will(returnValue(true));
			allowing(request).isKeepAliveConnection();
				will(returnValue(false));
			allowing(connection).isSelfClosing();
				will(returnValue(false));				
			one(connection).write(expectedResponse);
			one(connection).close();
			one(subscriptionManager).addSubscription(request);
		}});		
		
		cometHandler.handle(connection);
	}	
	
	@Test
	public void handlesPoll() throws Exception {
		final String domain = "bob.com";
		final Browser browser = Browser.FF3;
		final String expectedResponse = ResponseFactory.pollResponse(domain, browser, queuedMessages);
		context.checking(new Expectations() {{
			ignoring(request).getUrl();
			ignoring(request).isIframeHtmlRequest();
			ignoring(request).isDisconnection();
			ignoring(request).isSubscription();
			ignoring(request).isUnSubscribe();
			ignoring(request).isPublish();
			ignoring(request).isResponseConnection();
			ignoring(request).isRequestIFrameConnection();			
			allowing(connection).getRequest();
				will(returnValue(request));
			one(request).getDomain();
				will(returnValue(domain));
			one(request).getBrowser();
				will(returnValue(browser));
			one(request).isPoll();
				will(returnValue(true));
			one(request).isKeepAliveConnection();
				will(returnValue(false));
			allowing(connection).isSelfClosing();
				will(returnValue(false));				
			one(subscriptionManager).findOrCreateClient(request);
				will(returnValue(client));
			one(client).getUid();
			one(client).getQueuedMessages();
				will(returnValue(queuedMessages));
			one(connection).write(expectedResponse);
			one(connection).close();
		}});		
		
		cometHandler.handle(connection);
	}	
	
	@Test
	public void handlesCloseResponse() throws Exception {
		final String domain = "bob.com";
		final Browser browser = Browser.FF3;
		final String expectedResponse = ResponseFactory.closeResponse(domain, browser);
		context.checking(new Expectations() {{
			ignoring(request).getUrl();
			ignoring(request).isIframeHtmlRequest();
			ignoring(request).isDisconnection();
			ignoring(request).isSubscription();
			ignoring(request).isUnSubscribe();
			ignoring(request).isPublish();
			ignoring(request).isResponseConnection();
			ignoring(request).isRequestIFrameConnection();	
			ignoring(request).isPoll();
			allowing(connection).getRequest();
				will(returnValue(request));
			one(request).getDomain();
				will(returnValue(domain));
			one(request).getBrowser();
				will(returnValue(browser));
			one(request).isCloseResponse();
				will(returnValue(true));
			one(request).isKeepAliveConnection();
				will(returnValue(false));
			allowing(connection).isSelfClosing();
				will(returnValue(false));				
			one(subscriptionManager).findOrCreateClient(request);
				will(returnValue(client));
			one(client).getUid();
			one(client).disconnect();
			one(connection).write(expectedResponse);
			one(connection).close();
		}});		
		
		cometHandler.handle(connection);
	}	
	
	@Test
	public void handlesUnSubscribe() throws Exception {
		final String domain = "bob.com";
		final Browser browser = Browser.FF3;
		final String expectedResponse = ResponseFactory.unSubscribeResponse(domain, browser);
		
		context.checking(new Expectations() {{
			ignoring(request).getUrl();
			ignoring(request).isIframeHtmlRequest();
			ignoring(request).isDisconnection();
			ignoring(request).isSubscription();
			ignoring(request).isPublish();
			ignoring(request).isResponseConnection();
			ignoring(request).isRequestIFrameConnection();
			one(request).getDomain();
				will(returnValue(domain));
			one(request).getBrowser();
				will(returnValue(browser));
			allowing(connection).getRequest();
				will(returnValue(request));
			one(request).isUnSubscribe();
				will(returnValue(true));
			one(request).isKeepAliveConnection();
				will(returnValue(false));
			allowing(connection).isSelfClosing();
				will(returnValue(false));				
			one(connection).write(expectedResponse);
			one(connection).close();
			one(subscriptionManager).removeSubscription(request);
		}});		
		
		cometHandler.handle(connection);
	}	
	
	@Test
	public void handlesResponseConnection() throws Exception {
		final String domain = "bob.com";
		final Browser browser = Browser.FF3;
		final String expectedResponse = ResponseFactory.foreverFramePageHeader(domain, browser);
		
		context.checking(new Expectations() {{
			ignoring(request).getUrl();
			ignoring(request).isDisconnection();
			ignoring(request).isIframeHtmlRequest();
			ignoring(request).isSubscription();
			ignoring(request).isPublish();
			one(request).getDomain();
				will(returnValue(domain));
			allowing(request).getBrowser();
				will(returnValue(browser));
			allowing(connection).getRequest();
				will(returnValue(request));
			one(request).isResponseConnection();
				will(returnValue(true));
			one(request).isKeepAliveConnection();
				will(returnValue(true));
			one(connection).write(expectedResponse);
			one(subscriptionManager).findOrCreateClient(request);
				will(returnValue(client));
			allowing(client).getUid();
			((IStreamingClient) one(client)).onConnect();
			never(connection).close();
		}});		
		
		cometHandler.handle(connection);
	}	
	
	@Test
	public void handlesUnknownRequest() throws Exception {
		final String expectedResponse = ResponseFactory.defaultResponse(null);
		context.checking(new Expectations() {{
			ignoring(request).getUrl();
			ignoring(request).isIframeHtmlRequest();
			ignoring(request).isDisconnection();
			ignoring(request).isSubscription();
			ignoring(request).isPublish();
			ignoring(request).isResponseConnection();
			ignoring(request).isRequestIFrameConnection();
			ignoring(request).isUnSubscribe();
			ignoring(request).isPoll();
			ignoring(request).isCloseResponse();
			allowing(connection).getRequest();
				will(returnValue(request));
			one(request).isKeepAliveConnection();
				will(returnValue(false));
			allowing(connection).isSelfClosing();
				will(returnValue(false));				
			one(connection).write(expectedResponse);
			one(connection).close();
		}});		
		
		cometHandler.handle(connection);
	}	
	
	@Test
	public void handlesPublish() throws Exception {
		final String topic = "chat";
		final String domain = "bob.com";
		final Browser browser = Browser.FF3;
		final String expectedResponse = ResponseFactory.publishResponse(domain, browser);
		context.checking(new Expectations() {{
			ignoring(request).getUrl();
			ignoring(request).isIframeHtmlRequest();
			ignoring(request).isDisconnection();
			ignoring(request).isSubscription();
			ignoring(request).isResponseConnection();
			ignoring(request).isRequestIFrameConnection();
			one(request).getPayload();
				will(returnValue(payload));
			one(request).getPublishTopic();
				will(returnValue(topic));
			allowing(request).isPublish();
				will(returnValue(true));
			allowing(connection).getRequest();
				will(returnValue(request));
			one(request).getDomain();
				will(returnValue(domain));
			one(request).getBrowser();
				will(returnValue(browser));
			one(request).isKeepAliveConnection();
				will(returnValue(false));
			allowing(connection).isSelfClosing();
				will(returnValue(false));				
			one(subscriptionManager).findOrCreateClient(request);
				will(returnValue(client));				
			one(connection).write(expectedResponse);
			one(connection).close();
			one(subscriptionManager).notifyPublishListeners(client, topic, payload);
		}});		
		
		cometHandler.handle(connection);
	}	
}
