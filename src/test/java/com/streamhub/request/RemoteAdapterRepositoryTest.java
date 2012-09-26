package com.streamhub.request;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.streamhub.Connection;
import com.streamhub.StreamingSubscriptionManager;
import com.streamhub.api.PublishListener;
import com.streamhub.api.SubscriptionListener;

@RunWith(JMock.class)
public class RemoteAdapterRepositoryTest {
	private Mockery context;
	private Connection connection;
	private StreamingSubscriptionManager subscriptionManager;
	
	@Before
	public void setUp() {
		context = new Mockery() {{
			setImposteriser(ClassImposteriser.INSTANCE);
		}};
		connection = context.mock(Connection.class);
		subscriptionManager = context.mock(StreamingSubscriptionManager.class);
	}
	
	@Test
	public void testCreatesPublishListener() throws Exception {
		UIDRepository repo = new RemoteAdapterRepository(subscriptionManager);
		PublishListener publishListener = repo.findOrCreatePublishListener("1", null);
		assertNotNull(publishListener);
	}
	
	@Test
	public void testCreatesSubscriptionListener() throws Exception {
		UIDRepository repo = new RemoteAdapterRepository(subscriptionManager);
		SubscriptionListener subscriptionListener = repo.findOrCreateSubscriptionListener("1", null);
		assertNotNull(subscriptionListener);
	}
	
	@Test
	public void testFindsExistingPublishListener() throws Exception {
		UIDRepository repo = new RemoteAdapterRepository(subscriptionManager);
		PublishListener publishListener = repo.findOrCreatePublishListener("1", null);
		assertNotNull(publishListener);
		PublishListener foundPublishListener = repo.findOrCreatePublishListener("1", connection);
		assertSame(publishListener, foundPublishListener);
	}
	
	@Test
	public void testFindsExistingSubscriptionListener() throws Exception {
		UIDRepository repo = new RemoteAdapterRepository(subscriptionManager);
		SubscriptionListener subscriptionListener = repo.findOrCreateSubscriptionListener("1", null);
		assertNotNull(subscriptionListener);
		SubscriptionListener foundSubscriptionListener = repo.findOrCreateSubscriptionListener("1", connection);
		assertSame(subscriptionListener, foundSubscriptionListener);
	}
	
	@Test
	public void testRemovesSubscriptionListenerFromManagerWhenConnectionLost() throws Exception {
		final SubscriptionListener subscriptionListener = context.mock(SubscriptionListener.class);
		RemoteAdapterRepository repo = new RemoteAdapterRepository(subscriptionManager);
		context.checking(new Expectations() {{
			one(subscriptionManager).removeSubscriptionListener(subscriptionListener);
		}});
		repo.connectionLost(subscriptionListener);
	}
	
	@Test
	public void testRemovesSubscriptionListenerFromInternalMapWhenConnectionLost() throws Exception {
		RemoteAdapterRepository repo = new RemoteAdapterRepository(subscriptionManager);
		context.checking(new Expectations() {{
			allowing(subscriptionManager).removeSubscriptionListener(with(any(SubscriptionListener.class)));
		}});
		SubscriptionListener subscriptionListener = repo.findOrCreateSubscriptionListener("1", null);
		assertEquals(1, repo.getSubscriptionListeners().size());
		repo.connectionLost(subscriptionListener);
		assertEquals(0, repo.getSubscriptionListeners().size());
	}

	@Test
	public void testRemovesPublishListenerFromInternalMapWhenConnectionLost() throws Exception {
		RemoteAdapterRepository repo = new RemoteAdapterRepository(subscriptionManager);
		context.checking(new Expectations() {{
			allowing(subscriptionManager).removePublishListener(with(any(PublishListener.class)));
		}});
		PublishListener publishListener = repo.findOrCreatePublishListener("23", null);
		assertEquals(1, repo.getPublishListeners().size());
		repo.connectionLost(publishListener);
		assertEquals(0, repo.getPublishListeners().size());
	}
	
	@Test
	public void testRemovesPublishListenerFromManagerWhenConnectionLost() throws Exception {
		final PublishListener publishListener = context.mock(PublishListener.class);
		RemoteAdapterRepository repo = new RemoteAdapterRepository(subscriptionManager);
		context.checking(new Expectations() {{
			one(subscriptionManager).removePublishListener(publishListener);
		}});
		repo.connectionLost(publishListener);
	}
}
