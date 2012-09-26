package com.streamhub.request;

import java.nio.channels.ClosedChannelException;

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

@RunWith(JMock.class)
public class RemotePublishListenerTest {
	private Mockery context;
	private ConnectionListener connectionListener;
	private Connection connection;
	private Client client;
	private RemotePublishListener publishListener;
	
	@Before
	public void setUp() {
		context = new Mockery() {{
			setImposteriser(ClassImposteriser.INSTANCE);
		}};
		connection = context.mock(Connection.class);
		connectionListener = context.mock(ConnectionListener.class);
		client = context.mock(Client.class);
		publishListener = new RemotePublishListener(connection, connectionListener);
		context.checking(new Expectations() {{
			allowing(client).getUid(); will(returnValue("test-UID"));
		}});
	}
	
	@Test
	public void testConnectionListenerIsNotifiedWhenConnectionLost() throws Exception {
		context.checking(new Expectations() {{
			one(connection).write(with(any(String.class))); 
				will(throwException(new ClosedChannelException()));
			one(connection).close();
			one(connectionListener).connectionLost(publishListener);
		}});
		publishListener.onMessageReceived(client, "TOPIC", new JsonPayload("TOPIC"));
	}
}
