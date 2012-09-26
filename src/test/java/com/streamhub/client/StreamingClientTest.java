package com.streamhub.client;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.streamhub.Connection;
import com.streamhub.request.Request;


@RunWith(JMock.class)
public class StreamingClientTest {
	private Mockery context;
	private Request request;
	private ClientConnectionListener clientListener;
	private Connection connection;
	private StreamingClient client;
	
	@Before
	public void setUp() {
		context = new Mockery() {{ setImposteriser(ClassImposteriser.INSTANCE); }};
		request = context.mock(Request.class);
		clientListener = context.mock(ClientConnectionListener.class);
		connection = context.mock(Connection.class);
		context.checking(new Expectations() {{
			ignoring(request).getUid();
		}});
		client = (StreamingClient) DirectClient.createFrom(request, clientListener);
	}
	
	@Test
	public void writesToNIOConnection() throws Exception {
		context.checking(new Expectations() {{
			one(connection).write("something");
			ignoring(connection).getChannel();
		}});
		
		client.setConnection(connection);
		client.write("something");
	}
	
	@Test
	public void disconnectClosesConnection() throws Exception {
		context.checking(new Expectations() {{
			one(clientListener).clientDisconnected(client);
			one(connection).close();
			ignoring(connection).getChannel();
		}});
		
		client.setConnection(connection);
		client.disconnect();
	}
}
