package com.streamhub.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.streamhub.Connection;
import com.streamhub.api.Client;
import com.streamhub.api.JsonPayload;
import com.streamhub.api.Payload;
import com.streamhub.request.Request;
import com.streamhub.tools.NullPayload;

@RunWith(JMock.class)
public class CometClientTest {
	private Mockery context;
	private Request request;
	private Request requestWithDifferentUid;
	private ClientConnectionListener clientListener;
	private Connection connection;
	protected SocketChannel channel;
	
	@Before
	public void setUp() {
		context = new Mockery() {{ setImposteriser(ClassImposteriser.INSTANCE); }};
		request = context.mock(Request.class);
		requestWithDifferentUid = context.mock(Request.class, "RequestTwo");
		clientListener = context.mock(ClientConnectionListener.class);
		connection = context.mock(Connection.class);
		channel = context.mock(SocketChannel.class);
		context.checking(new Expectations() {{
			ignoring(connection).isSecure();
		}});
	}
	
	@Test
	public void testCreatesClientFromRequestUsingFactoryMethod() throws Exception {
		context.checking(new Expectations() {{
			one(request).getUid(); will(returnValue("1"));
		}});
		
		Client client = CometClient.createFrom(request, clientListener);

		assertEquals("1", client.getUid());
	}
	
	@Test
	public void testEquality() throws Exception {
		context.checking(new Expectations() {{
			exactly(2).of(request).getUid(); will(returnValue("1"));
			one(requestWithDifferentUid).getUid(); will(returnValue("2"));
		}});
		
		Client clientOne = CometClient.createFrom(request, clientListener);
		Client clientTwo = CometClient.createFrom(request, clientListener);
		Client clientWithDifferentUid = CometClient.createFrom(requestWithDifferentUid, clientListener);
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
	}
	
	@Test
	public void testQueuesMessageIfNoConnectionHasBeenSet() throws Exception {
		context.checking(new Expectations() {{
			one(request).getUid(); will(returnValue("1"));
			one(connection).write(with(any(ByteBuffer.class)));
			one(clientListener).clientConnected(with(any(IStreamingClient.class)));
			ignoring(connection).getChannel();
		}});
		
		IStreamingClient client = CometClient.createFrom(request, clientListener);
		client.send("Some topic", new NullPayload());
		client.setConnection(connection);
		client.onConnect();
	}

	@Test
	public void testClearsQueuedMessagesOnConnect() throws Exception {
		context.checking(new Expectations() {{
			one(request).getUid(); will(returnValue("1"));
			exactly(2).of(connection).write(with(any(ByteBuffer.class)));
			exactly(2).of(clientListener).clientConnected(with(any(IStreamingClient.class)));
			one(clientListener).clientDisconnected(with(any(IStreamingClient.class)));
			one(connection).close();
			ignoring(connection).getChannel();
		}});
		
		IStreamingClient client = CometClient.createFrom(request, clientListener);
		client.send("Message one", new NullPayload());
		client.setConnection(connection);
		client.onConnect();
		client.disconnect();
		client.setConnection(null);
		client.send("Message two", new NullPayload());
		client.setConnection(connection);
		client.onConnect();		
	}
	
	@Test
	public void testQueuesMultipleMessagesIfNoConnectionHasBeenSet() throws Exception {
		context.checking(new Expectations() {{
			exactly(1).of(request).getUid(); will(returnValue("1"));
			exactly(2).of(connection).write(with(any(ByteBuffer.class)));
			one(clientListener).clientConnected(with(any(IStreamingClient.class)));
			ignoring(connection).getChannel();
		}});
		
		IStreamingClient client = CometClient.createFrom(request, clientListener);
		client.send("Some topic", new NullPayload());
		client.send("Some other topic", new NullPayload());
		client.setConnection(connection);
		client.onConnect();
	}
	
	@Test
	public void testGetQueuedMessages() throws Exception {
		context.checking(new Expectations() {{
			exactly(1).of(request).getUid(); will(returnValue("1"));
		}});
		
		IStreamingClient client = CometClient.createFrom(request, clientListener);
		client.send("A", new JsonPayload("A") {{ addField("B", "C"); }});
		client.send("B", new JsonPayload("B") {{ addField("D", "E"); }});
		String queuedMessages = getQueuedMessagesAsString(client);
	    assertEquals("[{\"topic\":\"A\",\"B\":\"C\"},{\"D\":\"E\",\"topic\":\"B\"}]", queuedMessages);
	}
	
	@Test
	public void testClearsQueuedMessagesWhenDestroyed() throws Exception {
		context.checking(new Expectations() {{
			exactly(1).of(request).getUid(); will(returnValue("1"));
		}});
		
		IStreamingClient client = CometClient.createFrom(request, clientListener);
		client.send("A", new JsonPayload("A") {{ addField("B", "C"); }});
		client.send("B", new JsonPayload("B") {{ addField("D", "E"); }});
		client.destroy();
		String queuedMessagesPostDestroy = getQueuedMessagesAsString(client);
		assertEquals("[]", queuedMessagesPostDestroy);
	}
	
	@Test
	public void testReturnsEmptyArrayIfNoQueuedMessages() throws Exception {
		context.checking(new Expectations() {{
			exactly(1).of(request).getUid(); will(returnValue("1"));
		}});
		
		IStreamingClient client = CometClient.createFrom(request, clientListener);
		String queuedMessages = getQueuedMessagesAsString(client);
		assertEquals("[]", queuedMessages);
	}
	
	@Test
	public void testLosesConnectionIfQueueSizeExceedsTen() throws Exception {
		context.checking(new Expectations() {{
			one(request).getUid(); will(returnValue("1"));
			one(clientListener).clientLostConnection(with(any(IStreamingClient.class)));
		}});
		
		Client client = CometClient.createFrom(request, clientListener);
		for (int i = 1; i <= CometClient.QUEUE_SIZE + 1; i++) {
			client.send("" + i, new NullPayload());
		}
	}
	
	@Test
	public void testDoesNotLoseConnectionMultipleTimesIfQueueSizeContinuesToExceedTen() throws Exception {
		context.checking(new Expectations() {{
			one(request).getUid(); will(returnValue("1"));
			one(clientListener).clientLostConnection(with(any(IStreamingClient.class)));
		}});
		
		Client client = CometClient.createFrom(request, clientListener);
		for (int i = 1; i <= CometClient.QUEUE_SIZE + 3; i++) {
			client.send("" + i, new NullPayload());
		}
	}

	@Test
	public void testCancelsDisconnectIfQueueSizeReachesFiveAndThenTheClientIsConnected() throws Exception {
		context.checking(new Expectations() {{
			exactly(1).of(request).getUid(); will(returnValue("1"));
			one(clientListener).clientLostConnection(with(any(IStreamingClient.class)));
			exactly(1001).of(connection).write(with(any(ByteBuffer.class)));
			one(clientListener).clientConnected(with(any(IStreamingClient.class)));
			ignoring(connection).getChannel();
		}});
		
		IStreamingClient client = CometClient.createFrom(request, clientListener);
		for (int i = 1; i <= CometClient.QUEUE_SIZE + 1; i++) {
			client.send("" + i, new NullPayload());
		}
		client.setConnection(connection);
		client.onConnect();		
	}
	
	@Test
	public void testNotifiesClientManagerIfDisconnected() throws Exception {
		context.checking(new Expectations() {{
			one(request).getUid(); will(returnValue("1"));
			one(clientListener).clientDisconnected(with(any(IStreamingClient.class)));
			one(connection).close();
			ignoring(connection).getChannel();
		}});
		
		IStreamingClient client = CometClient.createFrom(request, clientListener);
		client.setConnection(connection);
		client.disconnect();
	}
	
	@Test
	public void testLosesConnectionIfSendThrowsException() throws Exception {
		context.checking(new Expectations() {{
			one(request).getUid(); will(returnValue("1"));
			one(connection).write(with(any(ByteBuffer.class)));
				will(throwException(new IOException("")));
			one(clientListener).clientConnected(with(any(IStreamingClient.class)));
			one(clientListener).clientLostConnection(with(any(IStreamingClient.class)));
			one(connection).close();
			ignoring(connection).getChannel();
		}});
		
		IStreamingClient client = CometClient.createFrom(request, clientListener);
		client.onConnect();
		client.setConnection(connection);
		client.send("This should throw an exception", new NullPayload());
	}
	
	@Test
	public void testDisconnectionSetsStatusToDisconnected() throws Exception {
		context.checking(new Expectations() {{
			one(request).getUid(); will(returnValue("1"));
			one(clientListener).clientDisconnected(with(any(IStreamingClient.class)));
		}});
		
		IStreamingClient client = CometClient.createFrom(request, clientListener);
		client.disconnect();
		assertFalse(client.isConnected());
	}
	
	@Test
	public void sendWritesToNIOConnection() throws Exception {
		context.checking(new Expectations() {{
			one(request).getUid(); will(returnValue("1"));
			one(clientListener).clientConnected(with(any(IStreamingClient.class)));
			one(connection).write(with(any(ByteBuffer.class)));
			ignoring(connection).getChannel();
		}});
		
		IStreamingClient client = CometClient.createFrom(request, clientListener);
		client.onConnect();
		assertTrue(client.isConnected());
		client.setConnection(connection);
		client.send("something", new JsonPayload("message"));
	}
	
	@Test
	public void sendDoesNotWriteToNIOConnectionIfDiconnected() throws Exception {
		context.checking(new Expectations() {{
			one(request).getUid(); will(returnValue("1"));
			one(clientListener).clientConnected(with(any(IStreamingClient.class)));
			one(clientListener).clientDisconnected(with(any(IStreamingClient.class)));
			never(connection).write(with(any(ByteBuffer.class)));
			ignoring(connection).getChannel();
		}});
		
		IStreamingClient client = CometClient.createFrom(request, clientListener);
		client.onConnect();
		assertTrue(client.isConnected());
		client.disconnect();
		assertFalse(client.isConnected());
		client.setConnection(connection);
		client.send("something", new JsonPayload("message"));
	}
	@Test
	public void keepsTrackOfSubscriptions() throws Exception {
		context.checking(new Expectations() {{
			one(request).getUid(); will(returnValue("1"));
		}});
		IStreamingClient client = CometClient.createFrom(request, clientListener);
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

	private static String getQueuedMessagesAsString(IStreamingClient client) {
		return client.getQueuedMessages();
	}
}
