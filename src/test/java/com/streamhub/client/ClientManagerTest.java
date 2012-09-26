package com.streamhub.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.streamhub.Connection;
import com.streamhub.api.Client;
import com.streamhub.request.Request;

@RunWith(JMock.class)
public class ClientManagerTest {
	private Mockery context;
	private Request request;
	private Request requestTwo;
	private ClientManager clientManager;
	private ClientFactory clientFactory;
	
	@Before
	public void setUp() {
		context = new Mockery() {{
			setImposteriser(ClassImposteriser.INSTANCE);
		}};
		request = context.mock(Request.class);
		requestTwo = context.mock(Request.class, "requestTwo");		
		clientFactory = context.mock(ClientFactory.class);
		context.checking(new Expectations() {{
			ignoring(requestTwo).isResponseConnection();
			allowing(request).getSubscriptionTopics(); will(returnValue(new String[] {"topic"}));
			allowing(request).getUid(); will(returnValue("1"));
		}});
		clientManager = new StreamingClientManager(clientFactory, 1);
	}
	
	@Test
	public void createsClient() throws Exception {
		context.checking(new Expectations() {{
			ignoring(request).isResponseConnection();
			one(clientFactory).createFrom(request); will(returnValue(CometClient.createFrom(request, null)));
		}});
		Client client = clientManager.findOrCreate(request);
		assertEquals("1", client.getUid());
	}
	
	@Test
	public void findsExistingClient() throws Exception {
		context.checking(new Expectations() {{
			ignoring(request).isResponseConnection();
			one(clientFactory).createFrom(request); will(returnValue(CometClient.createFrom(request, null)));
		}});
		Client createdClient = clientManager.findOrCreate(request);
		assertEquals("1", createdClient.getUid());
		
		Client foundClient = clientManager.findOrCreate(request);
		assertSame(createdClient, foundClient);
	}
	
	@Test
	public void removesDisconnectedClient() throws Exception {
		context.checking(new Expectations() {{
			ignoring(request).isResponseConnection();
			one(clientFactory).createFrom(request); will(returnValue(CometClient.createFrom(request, null)));
		}});
		IStreamingClient createdClient = clientManager.findOrCreate(request);
		Map<String, IStreamingClient> clients = ((StreamingClientManager) clientManager).getClients();
		assertEquals(createdClient, clients.get("1"));
		
		clientManager.remove(createdClient);
		assertEquals(null, clients.get("1"));
	}
	
	@Test
	public void creatingClientUsesClientFactory() throws Exception {
		context.checking(new Expectations() {{
			ignoring(request).isResponseConnection();
			one(clientFactory).createFrom(request);
		}});
		clientManager.findOrCreate(request);
	}
	
	@Test
	public void setsNIOConnectionOnClientIfResponseConnection() throws Exception {
		final IStreamingClient client = context.mock(IStreamingClient.class);
		context.checking(new Expectations() {{
			one(clientFactory).createFrom(request);
				will(returnValue(client));
			one(request).isResponseConnection();
				will(returnValue(true));
			one(request).getConnection();
			one(client).setConnection(with(any(Connection.class)));
		}});
		clientManager.findOrCreate(request);
	}
	
	@Test
	public void doesNotCreateMoreUsersThanTheUserLimit() throws Exception {
		context.checking(new Expectations() {{
			ignoring(request).isResponseConnection();
			one(requestTwo).getUid(); will(returnValue("2"));
			one(clientFactory).createFrom(request);
			one(clientFactory).createFrom(requestTwo);
		}});
		clientManager = new StreamingClientManager(clientFactory, 1);
		clientManager.findOrCreate(request);
		Map<String, IStreamingClient> clients = ((StreamingClientManager) clientManager).getClients();
		assertEquals(1, clients.size());
		clientManager.findOrCreate(requestTwo);
		assertEquals(1, clients.size());
	}
	
	@Test
	public void newUserPushesOutOldUserIfMoreThanUsersLimit() throws Exception {
		final IStreamingClient oldUser = context.mock(IStreamingClient.class, "oldUser");
		final IStreamingClient newUser = context.mock(IStreamingClient.class, "newUser");
		context.checking(new Expectations() {{
			ignoring(request).isResponseConnection();
			ignoring(oldUser).disconnect(); 
			one(requestTwo).getUid(); will(returnValue("2"));
			one(clientFactory).createFrom(request);
				will(returnValue(oldUser));
			one(clientFactory).createFrom(requestTwo);
				will(returnValue(newUser));
			exactly(2).of(oldUser).getUid();
				will(returnValue("1"));
			one(newUser).getUid();
				will(returnValue("2"));
		}});
		clientManager = new StreamingClientManager(clientFactory, 1);
		clientManager.findOrCreate(request);
		Map<String, IStreamingClient> clients = ((StreamingClientManager) clientManager).getClients();
		IStreamingClient client = clients.values().iterator().next();
		assertEquals("1", client.getUid());
		assertEquals(1, clients.size());
		clientManager.findOrCreate(requestTwo);
		client = clients.values().iterator().next();
		assertEquals("2", client.getUid());
		assertEquals(1, clients.size());
	}
	
	@Test
	public void usersPushedOutBecauseOfUserLimitAreDisconnected() throws Exception {
		final IStreamingClient oldUser = context.mock(IStreamingClient.class, "oldUser");
		final IStreamingClient newUser = context.mock(IStreamingClient.class, "newUser");
		context.checking(new Expectations() {{
			ignoring(request).isResponseConnection();
			ignoring(oldUser).getUid();
			one(requestTwo).getUid(); will(returnValue("2"));
			one(clientFactory).createFrom(request);
				will(returnValue(oldUser));
			one(clientFactory).createFrom(requestTwo);
				will(returnValue(newUser));
			one(oldUser).disconnect();
		}});
		
		clientManager = new StreamingClientManager(clientFactory, 1);
		clientManager.findOrCreate(request);
		clientManager.findOrCreate(requestTwo);
	}
	
	@Test
	public void removingUserDoesNotCauseAnyFalseDisconnectionsDueToUserLimit() throws Exception {
		final IStreamingClient firstClient = context.mock(IStreamingClient.class, "firstClient");
		final IStreamingClient secondClient = context.mock(IStreamingClient.class, "secondClient");
		context.checking(new Expectations() {{
			ignoring(request).isResponseConnection();
			one(requestTwo).getUid(); will(returnValue("2"));
			one(clientFactory).createFrom(request);
				will(returnValue(firstClient));
			one(clientFactory).createFrom(requestTwo);
				will(returnValue(secondClient));
			one(firstClient).getUid();
				will(returnValue("1"));
			never(firstClient).disconnect();
			never(secondClient).disconnect();
		}});
		
		clientManager = new StreamingClientManager(clientFactory, 1);
		clientManager.findOrCreate(request);
		clientManager.remove(firstClient);
		clientManager.findOrCreate(requestTwo);
	}
}
