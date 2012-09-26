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
import com.streamhub.handler.Handler;
import com.streamhub.request.DirectRequest;

@RunWith(JMock.class)
public class DirectHandlerTest {
	private static final String SEP = DirectHandler.DIRECT_MESSAGE_SEPARATOR;
	private static final String CONNECTION_STRING = SEP + "uid=3423423" + SEP;
	private static final String DISCONNECTION_REQUEST = CONNECTION_STRING + SEP + "disconnect" + SEP;
	private static final String SUBSCRIPTION_REQUEST = CONNECTION_STRING + SEP + "subscribe=AAPL" + SEP;
	private static final String MULTIPLE_SUBSCRIPTION_REQUEST = CONNECTION_STRING + SEP + "subscribe=AAPL,GOOG,MSFT" + SEP;
	private static final String UNSUBSCRIBE_REQUEST = CONNECTION_STRING + SEP + "unsubscribe=AAPL" + SEP;
	private static final String MULTIPLE_UNSUBSCRIBE_REQUEST = CONNECTION_STRING + SEP + "unsubscribe=AAPL,GOOG,MSFT" + SEP;
	private static final String PUBLISH_REQUEST = CONNECTION_STRING + SEP + "publish(AAPL,{\"topic\":\"AAPL\",\"price\":\"23.78234\"})" + SEP;
	private static final String EMPTY_SUBSCRIPTION_REQUEST = CONNECTION_STRING + SEP + "subscribe=" + SEP;;
	private static final String EMPTY_UNSUBSCRIBE_REQUEST = CONNECTION_STRING + SEP + "unsubscribe=" + SEP;;
	private Handler handler;
	private Mockery context;
	private StreamingSubscriptionManager subscriptionManager;
	private IStreamingClient client;
	private Connection connection;
	
	@Before
	public void setUp() throws Exception {
		context = new Mockery() {{
			setImposteriser(ClassImposteriser.INSTANCE);
		}};
		client = context.mock(IStreamingClient.class);
		subscriptionManager = context.mock(StreamingSubscriptionManager.class);
		connection = context.mock(Connection.class);
		handler = new DirectHandler(subscriptionManager);
		context.checking(new Expectations() {{
			ignoring(connection).getAttachment();
			ignoring(connection).setAttachment(with(any(String.class)));
		}});
	}
	
	@Test
	public void handlesConnect() throws Exception {
		context.checking(new Expectations() {{
			one(connection).readBytes();
				will(returnValue(CONNECTION_STRING.getBytes()));
			one(subscriptionManager).findOrCreateClient(with(any(DirectRequest.class)));
				will(returnValue(client));
			one(client).setConnection(connection);
			one(client).onConnect();
		}});
		
		handler.handle(connection);
	}
	
	@Test
	public void handlesSubscription() throws Exception {
		context.checking(new Expectations() {{
			one(connection).readBytes();
				will(returnValue(SUBSCRIPTION_REQUEST.getBytes()));
			one(subscriptionManager).findOrCreateClient(with(any(DirectRequest.class)));
			one(subscriptionManager).addSubscription(with(any(DirectRequest.class)));
		}});
		
		handler.handle(connection);
	}
	
	@Test
	public void doesNotBlowUpWithEmptySubscriptionTopic() throws Exception {
		context.checking(new Expectations() {{
			one(connection).readBytes();
			will(returnValue(EMPTY_SUBSCRIPTION_REQUEST.getBytes()));
			one(subscriptionManager).findOrCreateClient(with(any(DirectRequest.class)));
		}});
		
		handler.handle(connection);
	}
	
	@Test
	public void doesNotBlowUpWithEmptyUnSubscribeTopic() throws Exception {
		context.checking(new Expectations() {{
			one(connection).readBytes();
			will(returnValue(EMPTY_UNSUBSCRIBE_REQUEST.getBytes()));
			one(subscriptionManager).findOrCreateClient(with(any(DirectRequest.class)));
		}});
		
		handler.handle(connection);
	}
	
	@Test
	public void handlesSubscriptionWithMultipleTopics() throws Exception {
		context.checking(new Expectations() {{
			one(connection).readBytes();
				will(returnValue(MULTIPLE_SUBSCRIPTION_REQUEST.getBytes()));
			one(subscriptionManager).findOrCreateClient(with(any(DirectRequest.class)));
			one(subscriptionManager).addSubscription(with(any(DirectRequest.class)));
		}});
		
		handler.handle(connection);
	}
	
	@Test
	public void handlesUnSubscribe() throws Exception {
		context.checking(new Expectations() {{
			one(connection).readBytes();
				will(returnValue(UNSUBSCRIBE_REQUEST.getBytes()));
			one(subscriptionManager).findOrCreateClient(with(any(DirectRequest.class)));
			one(subscriptionManager).removeSubscription(with(any(DirectRequest.class)));
		}});
		
		handler.handle(connection);
	}
	
	@Test
	public void handlesUnSubscribeWithMultipleTopics() throws Exception {
		context.checking(new Expectations() {{
			one(connection).readBytes();
				will(returnValue(MULTIPLE_UNSUBSCRIBE_REQUEST.getBytes()));
			one(subscriptionManager).findOrCreateClient(with(any(DirectRequest.class)));
			one(subscriptionManager).removeSubscription(with(any(DirectRequest.class)));
		}});
		
		handler.handle(connection);
	}
	
	@Test
	public void handlesDisconnection() throws Exception {
		context.checking(new Expectations() {{
			ignoring(client).setConnection(connection);
			ignoring(client).onConnect();
			one(connection).readBytes();
				will(returnValue(DISCONNECTION_REQUEST.getBytes()));
			exactly(2).of(subscriptionManager).findOrCreateClient(with(any(DirectRequest.class))); 
				will(returnValue(client));
			one(client).disconnect();
			one(connection).close();
		}});
		
		handler.handle(connection);
	}
	
	@Test
	public void handlesPublish() throws Exception {
		context.checking(new Expectations() {{
			one(connection).readBytes();
				will(returnValue(PUBLISH_REQUEST.getBytes()));
			exactly(2).of(subscriptionManager).findOrCreateClient(with(any(DirectRequest.class)));
			one(subscriptionManager).notifyPublishListeners(with(any(IStreamingClient.class)), with("AAPL"), with(any(Payload.class)));
		}});
		
		handler.handle(connection);
	}	
}
