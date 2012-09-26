package com.streamhub.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.streamhub.Connection;
import com.streamhub.DirectHandler;
import com.streamhub.api.Client;
import com.streamhub.api.JsonPayload;
import com.streamhub.api.Payload;
import com.streamhub.request.Request;
import com.streamhub.tools.NullPayload;

@RunWith(JMock.class)
public class DirectClientTest {
	private Mockery context;
	private Request request;
	private Request requestWithDifferentUid;
	private ClientConnectionListener clientListener;
	private Connection connection;
	
	@Before
	public void setUp() {
		context = new Mockery() {{ setImposteriser(ClassImposteriser.INSTANCE); }};
		request = context.mock(Request.class);
		requestWithDifferentUid = context.mock(Request.class, "RequestTwo");
		clientListener = context.mock(ClientConnectionListener.class);
		connection = context.mock(Connection.class);
	}
	
	@Test
	public void createsClientFromRequestUsingFactoryMethod() throws Exception {
		context.checking(new Expectations() {{
			one(request).getUid(); will(returnValue("1"));
		}});
		
		IStreamingClient client = DirectClient.createFrom(request, clientListener);
		assertEquals("1", client.getUid());
	}
	
	@Test
	public void equality() throws Exception {
		context.checking(new Expectations() {{
			exactly(3).of(request).getUid(); will(returnValue("1"));
			one(requestWithDifferentUid).getUid(); will(returnValue("2"));
		}});
		
		Client clientOne = DirectClient.createFrom(request, clientListener);
		Client clientTwo = DirectClient.createFrom(request, clientListener);
		Client cometClient = CometClient.createFrom(request, clientListener);
		Client clientWithDifferentUid = DirectClient.createFrom(requestWithDifferentUid, clientListener);
		Client anonymousClient = new Client() {
			public String getUid() {
				return "1";
			}
			public void send(String topic, Payload payload) {}
			public void disconnect() {}
			public Set<String> getSubscriptions() {
				return null;
			}
			public boolean isConnected() {
				return true;
			}
		};
		
		assertTrue(clientOne.equals(clientTwo));
		assertTrue(clientTwo.equals(clientOne));
		assertEquals(clientOne.hashCode(), clientTwo.hashCode());
		assertFalse(clientOne.equals(clientWithDifferentUid));
		assertFalse(clientOne.hashCode() == clientWithDifferentUid.hashCode());
		assertFalse(clientOne.equals(null));
		assertFalse(clientOne.equals(anonymousClient));
		assertFalse(clientOne.hashCode() == anonymousClient.hashCode());
		assertFalse(clientOne.equals(cometClient));
		assertFalse(clientTwo.equals(cometClient));
		assertFalse(cometClient.equals(clientOne));
		assertFalse(cometClient.equals(clientTwo));
	}
	
	@Test
	public void notifiesManagerIfDisconnected() throws Exception {
		context.checking(new Expectations() {{
			one(request).getUid(); will(returnValue("1"));
			one(clientListener).clientDisconnected(with(any(IStreamingClient.class)));
			one(connection).close();
			ignoring(connection).getChannel();
		}});
		
		IStreamingClient client = DirectClient.createFrom(request, clientListener);
		client.setConnection(connection);
		client.disconnect();
	}
	
	@Test
	public void losesConnectionIfSendThrowsException() throws Exception {
		context.checking(new Expectations() {{
			one(request).getUid(); will(returnValue("1"));
			one(connection).write(with(any(String.class))); 
				will(throwException(new IOException("")));
			one(clientListener).clientConnected(with(any(IStreamingClient.class)));
			one(clientListener).clientLostConnection(with(any(IStreamingClient.class)));
			one(connection).close();
			ignoring(connection).getChannel();
		}});
		
		IStreamingClient client = DirectClient.createFrom(request, clientListener);
		
		client.setConnection(connection);
		client.onConnect();
		client.send("This should throw an exception", new NullPayload());
	}
	
	@Test
	public void keepsTrackOfSubscriptions() throws Exception {
		context.checking(new Expectations() {{
			one(request).getUid(); will(returnValue("1"));
		}});
		IStreamingClient client = DirectClient.createFrom(request, clientListener);
		client.addSubscription("AAPL");
		client.addSubscription("MSFT");
		Set<String> subscriptions = client.getSubscriptions();
		assertEquals(2, subscriptions.size());
		assertTrue(subscriptions.contains("AAPL"));
		assertTrue(subscriptions.contains("MSFT"));
		client.removeSubscription("AAPL");
		Set<String> postUnsubscribeSubscriptions = client.getSubscriptions();
		assertEquals(1, postUnsubscribeSubscriptions.size());
		assertTrue(postUnsubscribeSubscriptions.contains("MSFT"));
		assertFalse(postUnsubscribeSubscriptions.contains("AAPL"));
	}
	
	@Test
	public void keepsTrackOfConnectedStatus() throws Exception {
		context.checking(new Expectations() {{
			one(request).getUid(); will(returnValue("1"));
			one(clientListener).clientConnected(with(any(IStreamingClient.class)));
			one(clientListener).clientDisconnected(with(any(IStreamingClient.class)));
		}});
		
		IStreamingClient client = DirectClient.createFrom(request, clientListener);
		assertFalse(client.isConnected());
		client.onConnect();
		assertTrue(client.isConnected());
		client.disconnect();
		assertFalse(client.isConnected());
	}
	
	@Test
	public void messageSendFormat() throws Exception {
		String expectedJson = "{\"topic\":\"TOPIC\"}";
		final String expectedMessage = DirectHandler.DIRECT_MESSAGE_SEPARATOR + expectedJson + DirectHandler.DIRECT_MESSAGE_SEPARATOR;
		context.checking(new Expectations() {{
			one(request).getUid(); will(returnValue("1"));
			one(connection).write(expectedMessage);
			one(clientListener).clientConnected(with(any(IStreamingClient.class)));
			ignoring(connection).getChannel();
		}});
		
		IStreamingClient client = DirectClient.createFrom(request, clientListener);
		client.setConnection(connection);
		client.onConnect();
		Payload payload = new JsonPayload("TOPIC");
		client.send("TOPIC", payload);
	}

	@Test
	public void testDisconnectClosesNIOConnection() throws Exception {
		context.checking(new Expectations() {{
			one(request).getUid(); 
				will(returnValue("1"));
			one(connection).close();
			one(clientListener).clientDisconnected(with(any(IStreamingClient.class)));
			ignoring(connection).getChannel();
		}});
		
		IStreamingClient client = DirectClient.createFrom(request, clientListener);
		client.setConnection(connection);
		client.disconnect();
	}
	
	@Test
	public void sendWritesToNIOConnection() throws Exception {
		String expectedJson = "{\"topic\":\"TOPIC\"}";
		final String expectedMessage = DirectHandler.DIRECT_MESSAGE_SEPARATOR + expectedJson + DirectHandler.DIRECT_MESSAGE_SEPARATOR;
		context.checking(new Expectations() {{
			one(request).getUid(); will(returnValue("1"));
			one(connection).write(expectedMessage);
			ignoring(connection).getChannel();
		}});
		
		IStreamingClient client = DirectClient.createFrom(request, clientListener);
		client.setConnection(connection);
		client.send("something", new JsonPayload("TOPIC"));
	}	
}
