package com.streamhub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.integration.junit4.JMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.streamhub.api.JsonPayload;
import com.streamhub.api.PublishListener;
import com.streamhub.api.SubscriptionListener;
import com.streamhub.client.ClientManager;
import com.streamhub.client.IStreamingClient;
import com.streamhub.request.Request;
import com.streamhub.util.Sleep;

@RunWith(JMock.class)
public class StreamingSubscriptionManagerTest {
	private Mockery context;
	private StreamingSubscriptionManager subscriptionManager;
	private ClientManager clientManager;
	private IStreamingClient client;
	private IStreamingClient clientTwo;
	private Request request;
	private Request multipleSubscriptionRequest;
	private String topic;
	private String[] multipleTopics;
	private Request requestTwo;
	
	@Before
	public void setUp() {
		context = new Mockery();
		clientManager = context.mock(ClientManager.class);
		client = context.mock(IStreamingClient.class);
		clientTwo = context.mock(IStreamingClient.class, "clientTwo");
		subscriptionManager = new StreamingSubscriptionManager();
		subscriptionManager.start(clientManager);
		request = context.mock(Request.class);
		requestTwo = context.mock(Request.class, "RequestTwo");
		multipleSubscriptionRequest = context.mock(Request.class, "multipleSubscriptionRequest");
		topic = "AAPL";
		multipleTopics = new String[] {"GOOG", "MSFT", "WMT"};
		
		context.checking(new Expectations() {{
			allowing(client).removeSubscription(topic);
			allowing(clientTwo).removeSubscription(topic);
			ignoring(request).isResponseConnection();
			ignoring(request).getUid();
			ignoring(client).getUid();
			ignoring(clientTwo).getUid();
			ignoring(client).addSubscription(with(any(String.class)));
			ignoring(clientTwo).addSubscription(with(any(String.class)));
			allowing(request).getSubscriptionTopics(); will(returnValue(new String[] {topic}));
			allowing(requestTwo).getSubscriptionTopics(); will(returnValue(new String[] {topic}));
			ignoring(multipleSubscriptionRequest).isResponseConnection();
			ignoring(multipleSubscriptionRequest).getUid();
			allowing(multipleSubscriptionRequest).getSubscriptionTopics(); will(returnValue(multipleTopics));
		}});
	}
	
	@Test
	public void testNoLongerNotifiesListenerWhenRemoved() throws Exception {
		final SubscriptionListener listener = context.mock(SubscriptionListener.class);
		
		subscriptionManager.addSubscriptionListener(listener);
		
		context.checking(new Expectations() {{
			one(clientManager).findOrCreate(request); will(returnValue(client));	
			one(clientManager).findOrCreate(requestTwo); will(returnValue(clientTwo)); 		
			one(listener).onSubscribe(topic, client);
		}});
		
		subscriptionManager.addSubscription(request);
		Sleep.millis(50);
		subscriptionManager.removeSubscriptionListener(listener);
		subscriptionManager.addSubscription(requestTwo);
		Sleep.millis(250);
 	}
	
	@Test
	public void testNotifiesListenerWhenSubscriptionReceived() throws Exception {
		final SubscriptionListener listener = context.mock(SubscriptionListener.class);
		
		subscriptionManager.addSubscriptionListener(listener);
		
		context.checking(new Expectations() {{
			one(clientManager).findOrCreate(request); will(returnValue(client));			
			one(listener).onSubscribe(topic, client);
		}});
		
		subscriptionManager.addSubscription(request);
		Sleep.millis(50);
	}

	@Test
	public void testNotifiesListenerWhenUnSubscribeReceived() throws Exception {
		final SubscriptionListener listener = context.mock(SubscriptionListener.class);
		
		subscriptionManager.addSubscriptionListener(listener);
		
		context.checking(new Expectations() {{
			one(clientManager).findOrCreate(request); will(returnValue(client));			
			one(listener).onUnSubscribe(topic, client);
			
		}});
		
		subscriptionManager.removeSubscription(request);
		Sleep.millis(50);
	}
	
	@Test
	public void testRemovesSubscriptionListeners() {
		final SubscriptionListener listener = context.mock(SubscriptionListener.class);
		subscriptionManager.addSubscriptionListener(listener);
		assertEquals(1, subscriptionManager.getSubscriptionListeners().size());
		subscriptionManager.removeSubscriptionListener(listener);
		assertEquals(0, subscriptionManager.getSubscriptionListeners().size());
	}
	
	@Test
	public void testRemovesClientFromTopicToClientsMapOnUnSubscribe() throws Exception {
		final SubscriptionListener listener = context.mock(SubscriptionListener.class);
		subscriptionManager.addSubscriptionListener(listener);
		
		context.checking(new Expectations() {{
			allowing(clientManager).findOrCreate(request); will(returnValue(client));			
			allowing(listener).onSubscribe(topic, client);
			allowing(listener).onUnSubscribe(topic, client);
		}});
		
		subscriptionManager.addSubscription(request);
		Map<String, IStreamingClient[]> topicToClientsAfterSubscribe = subscriptionManager.getTopicToClients();
		assertEquals(1, topicToClientsAfterSubscribe.get(topic).length);
		assertEquals(client, topicToClientsAfterSubscribe.get(topic)[0]);
		
		subscriptionManager.removeSubscription(request);
		Sleep.millis(50);
		Map<String, IStreamingClient[]> topicToClientsAfterUnSubscribe = subscriptionManager.getTopicToClients();
		assertNull(topicToClientsAfterUnSubscribe.get(topic));
	}
	
	@Test
	public void testDeletesTopicListFromTopicToClientsMapOnUnSubscribe() throws Exception {
		final SubscriptionListener listener = context.mock(SubscriptionListener.class);
		subscriptionManager.addSubscriptionListener(listener);
		
		context.checking(new Expectations() {{
			allowing(clientManager).findOrCreate(request); will(returnValue(client));			
			allowing(listener).onSubscribe(topic, client);
			allowing(listener).onUnSubscribe(topic, client);
		}});
		
		subscriptionManager.addSubscription(request);
		subscriptionManager.addSubscription(request);
		Map<String, IStreamingClient[]> topicToClientsAfterSubscribe = subscriptionManager.getTopicToClients();
		assertEquals(2, topicToClientsAfterSubscribe.get(topic).length);
		assertEquals(client, topicToClientsAfterSubscribe.get(topic)[0]);
		assertEquals(client, topicToClientsAfterSubscribe.get(topic)[1]);
		
		subscriptionManager.removeSubscription(request);
		Sleep.millis(50);
		Map<String, IStreamingClient[]> topicToClientsAfterUnSubscribe = subscriptionManager.getTopicToClients();
		assertNull(topicToClientsAfterUnSubscribe.get(topic));
	}
	
	@Test
	public void testDoesNotDeleteTopicListIfClientWhoIsNotSubscribedUnsubscribes() throws Exception {
		final SubscriptionListener listener = context.mock(SubscriptionListener.class);
		subscriptionManager.addSubscriptionListener(listener);
		
		context.checking(new Expectations() {{
			exactly(2).of(clientManager).findOrCreate(request); 
				will(onConsecutiveCalls(returnValue(client), returnValue(clientTwo)));			
			one(listener).onSubscribe(topic, client);
			one(listener).onUnSubscribe(topic, clientTwo);
		}});
		
		subscriptionManager.addSubscription(request);
		Map<String, IStreamingClient[]> topicToClientsAfterSubscribe = subscriptionManager.getTopicToClients();
		assertEquals(1, topicToClientsAfterSubscribe.get(topic).length);
		assertEquals(client, topicToClientsAfterSubscribe.get(topic)[0]);
		
		subscriptionManager.removeSubscription(request);
		Sleep.millis(50);
		Map<String, IStreamingClient[]> topicToClientsAfterUnSubscribeFromNotYetSubscribedClient = subscriptionManager.getTopicToClients();
		assertEquals(1, topicToClientsAfterUnSubscribeFromNotYetSubscribedClient.get(topic).length);
		assertEquals(client, topicToClientsAfterUnSubscribeFromNotYetSubscribedClient.get(topic)[0]);
	}
	
	@Test
	public void testTopicListDoesNotShrinkIfClientWhoIsNotSubscribedUnsubscribes() throws Exception {
		final SubscriptionListener listener = context.mock(SubscriptionListener.class);
		subscriptionManager.addSubscriptionListener(listener);
		
		context.checking(new Expectations() {{
			exactly(3).of(clientManager).findOrCreate(request); 
				will(onConsecutiveCalls(returnValue(client), returnValue(client), returnValue(clientTwo)));			
			exactly(2).of(listener).onSubscribe(topic, client);
			one(listener).onUnSubscribe(topic, clientTwo);
		}});
		
		subscriptionManager.addSubscription(request);
		subscriptionManager.addSubscription(request);
		Map<String, IStreamingClient[]> topicToClientsAfterSubscribe = subscriptionManager.getTopicToClients();
		assertEquals(2, topicToClientsAfterSubscribe.get(topic).length);
		assertEquals(client, topicToClientsAfterSubscribe.get(topic)[0]);
		assertEquals(client, topicToClientsAfterSubscribe.get(topic)[1]);
		
		subscriptionManager.removeSubscription(request);
		Sleep.millis(50);
		Map<String, IStreamingClient[]> topicToClientsAfterUnSubscribe = subscriptionManager.getTopicToClients();
		assertEquals(2, topicToClientsAfterUnSubscribe.get(topic).length);
		assertEquals(client, topicToClientsAfterUnSubscribe.get(topic)[0]);
		assertEquals(client, topicToClientsAfterUnSubscribe.get(topic)[1]);
	}
	
	@Test
	public void testSingleClientCanSubscribeThenUnsubscribe() throws Exception {
		final SubscriptionListener listener = context.mock(SubscriptionListener.class);
		subscriptionManager.addSubscriptionListener(listener);
		
		context.checking(new Expectations() {{
			exactly(2).of(clientManager).findOrCreate(request); 
				will(returnValue(client));			
			one(listener).onSubscribe(topic, client);
			one(listener).onUnSubscribe(topic, client);
		}});
		
		subscriptionManager.addSubscription(request);
		Map<String, IStreamingClient[]> topicToClientsAfterSubscribe = subscriptionManager.getTopicToClients();
		assertEquals(1, topicToClientsAfterSubscribe.get(topic).length);
		assertEquals(client, topicToClientsAfterSubscribe.get(topic)[0]);
		
		subscriptionManager.removeSubscription(request);
		Sleep.millis(50);
		Map<String, IStreamingClient[]> topicToClientsAfterUnSubscribe = subscriptionManager.getTopicToClients();
		assertNull(topicToClientsAfterUnSubscribe.get(topic));
	}	
	
	@Test
	public void testUnsubscribingFromEmptyTopicList() throws Exception {
		final SubscriptionListener listener = context.mock(SubscriptionListener.class);
		subscriptionManager.addSubscriptionListener(listener);
		
		context.checking(new Expectations() {{
			allowing(clientManager).findOrCreate(request); 
				will(returnValue(client));			
			one(listener).onUnSubscribe(topic, client);
		}});
		
		subscriptionManager.removeSubscription(request);
		Sleep.millis(50);
		Map<String, IStreamingClient[]> topicToClientsAfterUnSubscribe = subscriptionManager.getTopicToClients();
		assertNull(topicToClientsAfterUnSubscribe.get(topic));
	}	
	
	@Test
	public void testRemovesTopicFromClientsOnUnSubscribe() throws Exception {
		final SubscriptionListener listener = context.mock(SubscriptionListener.class);
		final IStreamingClient subscribedClient = context.mock(IStreamingClient.class, "subscribedClient");
		subscriptionManager.addSubscriptionListener(listener);
		
		context.checking(new Expectations() {{
			allowing(subscribedClient).getUid();
			allowing(clientManager).findOrCreate(request); will(returnValue(subscribedClient));			
			allowing(listener).onUnSubscribe(topic, subscribedClient);
			one(subscribedClient).removeSubscription(topic);
		}});
		
		subscriptionManager.removeSubscription(request);
		Sleep.millis(50);
	}
	
	@Test
	public void testAddsTopicToClientOnSubscribe() throws Exception {
		final SubscriptionListener listener = context.mock(SubscriptionListener.class);
		final IStreamingClient subscribedClient = context.mock(IStreamingClient.class, "subscribedClient");
		subscriptionManager.addSubscriptionListener(listener);
		
		context.checking(new Expectations() {{
			allowing(subscribedClient).getUid();
			allowing(clientManager).findOrCreate(request); will(returnValue(subscribedClient));			
			allowing(listener).onSubscribe(topic, subscribedClient);
			one(subscribedClient).addSubscription(topic);
		}});
		
		subscriptionManager.addSubscription(request);
		Sleep.millis(50);
	}
	
	@Test
	public void testRemovesNullsFromTopicToClientsMapOnUnSubscribe() throws Exception {
		final SubscriptionListener listener = context.mock(SubscriptionListener.class);
		subscriptionManager.addSubscriptionListener(listener);
		
		context.checking(new Expectations() {{
			allowing(clientManager).findOrCreate(request); 
				will(returnValue(client)); 			
			allowing(clientManager).findOrCreate(requestTwo); 
				will(returnValue(clientTwo)); 			
			allowing(listener).onSubscribe(topic, client);
			allowing(listener).onSubscribe(topic, clientTwo);
			allowing(listener).onUnSubscribe(topic, client);
			allowing(listener).onUnSubscribe(topic, clientTwo);
		}});
		
		subscriptionManager.addSubscription(request);
		subscriptionManager.addSubscription(request);
		subscriptionManager.addSubscription(requestTwo);
		subscriptionManager.addSubscription(requestTwo);
		Map<String, IStreamingClient[]> topicToClientsAfterSubscribe = subscriptionManager.getTopicToClients();
		assertEquals(4, topicToClientsAfterSubscribe.get(topic).length);
		assertEquals(client, topicToClientsAfterSubscribe.get(topic)[0]);
		assertEquals(client, topicToClientsAfterSubscribe.get(topic)[1]);
		assertEquals(clientTwo, topicToClientsAfterSubscribe.get(topic)[2]);
		assertEquals(clientTwo, topicToClientsAfterSubscribe.get(topic)[3]);
		
		subscriptionManager.removeSubscription(request);
		Sleep.millis(50);
		Map<String, IStreamingClient[]> topicToClientsAfterUnSubscribe = subscriptionManager.getTopicToClients();
		assertEquals(2 ,topicToClientsAfterUnSubscribe.get(topic).length);
		assertEquals(clientTwo ,topicToClientsAfterUnSubscribe.get(topic)[0]);
		assertEquals(clientTwo ,topicToClientsAfterUnSubscribe.get(topic)[1]);
	}
	
	@Test
	public void testRemovesMultipleClientsFromTopicToClientsMapOnUnSubscribe() throws Exception {
		final SubscriptionListener listener = context.mock(SubscriptionListener.class);
		subscriptionManager.addSubscriptionListener(listener);
		
		context.checking(new Expectations() {{
			allowing(clientManager).findOrCreate(request); 
				will(returnValue(client)); 			
			allowing(clientManager).findOrCreate(requestTwo); 
				will(returnValue(clientTwo)); 			
			allowing(listener).onSubscribe(topic, client);
			allowing(listener).onSubscribe(topic, clientTwo);
			allowing(listener).onUnSubscribe(topic, client);
			allowing(listener).onUnSubscribe(topic, clientTwo);
		}});
		
		subscriptionManager.addSubscription(request);
		subscriptionManager.addSubscription(requestTwo);
		Map<String, IStreamingClient[]> topicToClientsAfterSubscribe = subscriptionManager.getTopicToClients();
		assertEquals(2, topicToClientsAfterSubscribe.get(topic).length);
		assertEquals(client, topicToClientsAfterSubscribe.get(topic)[0]);
		assertEquals(clientTwo, topicToClientsAfterSubscribe.get(topic)[1]);
		
		subscriptionManager.removeSubscription(request);
		Sleep.millis(50);
		Map<String, IStreamingClient[]> topicToClientsAfterUnSubscribe = subscriptionManager.getTopicToClients();
		assertEquals(1, topicToClientsAfterUnSubscribe.get(topic).length);
		assertEquals(clientTwo, topicToClientsAfterUnSubscribe.get(topic)[0]);
	}

	@Test
	public void testNotifiesSubscriberWhenMessageReceived() throws Exception {
		final PublishListener listener = context.mock(PublishListener.class);
		final JsonPayload expectedPayload = new JsonPayload("message");
		
		subscriptionManager.addPublishListener(listener);
		
		context.checking(new Expectations() {{
			one(listener).onMessageReceived(client, topic, expectedPayload);
		}});
		
		subscriptionManager.notifyPublishListeners(client, topic, expectedPayload);
		Sleep.millis(50);
	}
	
	@Test
	public void testNoLongerNotifiesSubscriberWhenMessageReceivedAfterListenerSubscriberRemoval() throws Exception {
		final PublishListener listener = context.mock(PublishListener.class);
		final JsonPayload expectedPayload = new JsonPayload("message");
		
		subscriptionManager.addPublishListener(listener);
		
		context.checking(new Expectations() {{
			one(listener).onMessageReceived(client, topic, expectedPayload);
		}});
		
		subscriptionManager.notifyPublishListeners(client, topic, expectedPayload);		
		Sleep.millis(50);
		subscriptionManager.removePublishListener(listener);
		subscriptionManager.notifyPublishListeners(client, topic, expectedPayload);
		Sleep.millis(200);
	}
	
	@Test
	public void testNotifiesSubscriberWhenMultipleMessagesReceived() throws Exception {
		final PublishListener publishListener = context.mock(PublishListener.class);
		final JsonPayload expectedPayloadOne = new JsonPayload("message one");
		final JsonPayload expectedPayloadTwo = new JsonPayload("message two");
		final JsonPayload expectedPayloadThree = new JsonPayload("message three");
		
		subscriptionManager.addPublishListener(publishListener);
		
		context.checking(new Expectations() {{
			one(publishListener).onMessageReceived(client, topic, expectedPayloadOne);
			one(publishListener).onMessageReceived(client, topic, expectedPayloadTwo);
			one(publishListener).onMessageReceived(client, topic, expectedPayloadThree);
		}});
		
		subscriptionManager.notifyPublishListeners(client, topic, expectedPayloadOne);
		subscriptionManager.notifyPublishListeners(client, topic, expectedPayloadTwo);
		subscriptionManager.notifyPublishListeners(client, topic, expectedPayloadThree);
		Sleep.millis(50);
	}
	
	@Test
	public void testNotifiesMultipleSubscribersWhenMessageReceived() throws Exception {
		final PublishListener subscriberOne = context.mock(PublishListener.class);
		final PublishListener subscriberTwo = context.mock(PublishListener.class, "subscriberTwo");
		final JsonPayload expectedPayload = new JsonPayload("message");
		
		subscriptionManager.addPublishListener(subscriberOne);
		subscriptionManager.addPublishListener(subscriberTwo);
		
		context.checking(new Expectations() {{
			one(subscriberOne).onMessageReceived(client, topic, expectedPayload);
			one(subscriberTwo).onMessageReceived(client, topic, expectedPayload);
		}});
		
		subscriptionManager.notifyPublishListeners(client, topic, expectedPayload);
		Sleep.millis(50);
	}
	
	@Test
	public void testNotifiesListenerWhenRequestWithMultipleSubscriptionsReceived() throws Exception {
		final SubscriptionListener listener = context.mock(SubscriptionListener.class);
		
		subscriptionManager.addSubscriptionListener(listener);
		
		context.checking(new Expectations() {{
			one(clientManager).findOrCreate(multipleSubscriptionRequest); will(returnValue(client));			
			one(listener).onSubscribe(multipleTopics[0], client);
			one(listener).onSubscribe(multipleTopics[1], client);
			one(listener).onSubscribe(multipleTopics[2], client);
		}});
		
		subscriptionManager.addSubscription(multipleSubscriptionRequest);
		Sleep.millis(100);
	}
	
	@Test
	public void testNotifiesListenerWhenUnSubscribeReceivedWithMultipleTopics() throws Exception {
		final SubscriptionListener listener = context.mock(SubscriptionListener.class);
		
		subscriptionManager.addSubscriptionListener(listener);
		
		context.checking(new Expectations() {{
			one(clientManager).findOrCreate(multipleSubscriptionRequest); will(returnValue(client));			
			one(listener).onUnSubscribe(multipleTopics[0], client);
			one(listener).onUnSubscribe(multipleTopics[1], client);
			one(listener).onUnSubscribe(multipleTopics[2], client);
			one(client).removeSubscription(multipleTopics[0]);
			one(client).removeSubscription(multipleTopics[1]);
			one(client).removeSubscription(multipleTopics[2]);
		}});
		
		subscriptionManager.removeSubscription(multipleSubscriptionRequest);
		Sleep.millis(100);
	}

	@Test
	public void testNotifiesAllListenersWhenUnSubscribeReceivedWithMultipleTopics() throws Exception {
		final SubscriptionListener listener = context.mock(SubscriptionListener.class);
		final SubscriptionListener listenerTwo = context.mock(SubscriptionListener.class, "SubscriptionListenerTwo");		
		
		subscriptionManager.addSubscriptionListener(listener);
		subscriptionManager.addSubscriptionListener(listenerTwo);
		
		context.checking(new Expectations() {{
			one(clientManager).findOrCreate(multipleSubscriptionRequest); will(returnValue(client));			
			one(listener).onUnSubscribe(multipleTopics[0], client);
			one(listener).onUnSubscribe(multipleTopics[1], client);
			one(listener).onUnSubscribe(multipleTopics[2], client);
			one(listenerTwo).onUnSubscribe(multipleTopics[0], client);
			one(listenerTwo).onUnSubscribe(multipleTopics[1], client);
			one(listenerTwo).onUnSubscribe(multipleTopics[2], client);
			one(client).removeSubscription(multipleTopics[0]);
			one(client).removeSubscription(multipleTopics[1]);
			one(client).removeSubscription(multipleTopics[2]);			
		}});
		
		subscriptionManager.removeSubscription(multipleSubscriptionRequest);
		Sleep.millis(50);
	}
	
	@Test
	public void testNotifiesAllListenersWhenSubscriptionReceived() throws Exception {
		final SubscriptionListener listener = context.mock(SubscriptionListener.class);
		final SubscriptionListener listenerTwo = context.mock(SubscriptionListener.class, "SubscriptionListenerTwo");
		
		subscriptionManager.addSubscriptionListener(listener);
		subscriptionManager.addSubscriptionListener(listenerTwo);
		
		context.checking(new Expectations() {{
			one(clientManager).findOrCreate(request); will(returnValue(client));	
			one(listener).onSubscribe(topic, client);
			one(listenerTwo).onSubscribe(topic, client);
		}});
		
		subscriptionManager.addSubscription(request);
		Sleep.millis(100);
	}
	
	@Test
	public void testNotifiesAllListenersWhenRequestWithMultipleSubscriptionsReceived() throws Exception {
		final SubscriptionListener listener = context.mock(SubscriptionListener.class);
		final SubscriptionListener listenerTwo = context.mock(SubscriptionListener.class, "SubscriptionListenerTwo");
		subscriptionManager.addSubscriptionListener(listener);
		subscriptionManager.addSubscriptionListener(listenerTwo);
		
		context.checking(new Expectations() {{
			one(clientManager).findOrCreate(multipleSubscriptionRequest); will(returnValue(client));	
			one(listener).onSubscribe(multipleTopics[0], client);
			one(listener).onSubscribe(multipleTopics[1], client);
			one(listener).onSubscribe(multipleTopics[2], client);
			one(listenerTwo).onSubscribe(multipleTopics[0], client);
			one(listenerTwo).onSubscribe(multipleTopics[1], client);
			one(listenerTwo).onSubscribe(multipleTopics[2], client);
		}});
		
		subscriptionManager.addSubscription(multipleSubscriptionRequest);
		Sleep.millis(100);
	}
	
	@Test
	public void testPassesClientThroughWhenSubscriptionReceived() throws Exception {
		final SubscriptionListener listener = context.mock(SubscriptionListener.class);
		subscriptionManager.addSubscriptionListener(listener);
		
		context.checking(new Expectations() {{
			exactly(2).of(clientManager).findOrCreate(request); will(onConsecutiveCalls(returnValue(client), returnValue(clientTwo)));	
			one(listener).onSubscribe(topic, client);
			one(listener).onSubscribe(topic, clientTwo);
		}});
		
		subscriptionManager.addSubscription(request);
		subscriptionManager.addSubscription(request);
		Sleep.millis(100);
	}
	
	@Test
	public void testStripsNewlinesOffTheEndOfTopic() throws Exception {
		context = new Mockery();
		request = context.mock(Request.class);
		clientManager = context.mock(ClientManager.class);
		context.checking(new Expectations() {{
			ignoring(request).isResponseConnection();
			ignoring(request).getUid();
			ignoring(clientManager).findOrCreate(with(any(Request.class))); will(returnValue(client));			
			allowing(request).getSubscriptionTopics(); will(returnValue(new String[] {"Topic\r\n"}));
		}});
		StreamingSubscriptionManager manager = new StreamingSubscriptionManager();
		manager.start(clientManager);
		subscriptionManager.start(clientManager);
		manager.addSubscription(request);
		assertNull(manager.getTopicToClients().get("Topic\r\n"));
		assertNotNull(manager.getTopicToClients().get("Topic"));
	}
	
	@SuppressWarnings("serial")
	@Test
	public void testNotifiesListenersWhenClientDisconnected() throws Exception {
		final SubscriptionListener listener = context.mock(SubscriptionListener.class);
		subscriptionManager.addSubscriptionListener(listener);
		
		context.checking(new Expectations() {{
			one(clientManager).findOrCreate(request); will(returnValue(client));			
			one(listener).onSubscribe(topic, client);
			allowing(client).getSubscriptions(); will(setContaining(topic));
			one(client).isConnected(); will(returnValue(false));
			one(clientManager).remove(client);
			one(listener).onUnSubscribe(topic, client);
			one(client).destroy();
		}

		private Action setContaining(final String topic) {
			return returnValue(new HashSet<String>() {{ add(topic); }});
		}});
		
		subscriptionManager.addSubscription(request);
		subscriptionManager.setReconnectionTimeout(0);
		subscriptionManager.clientDisconnected(client);
		Sleep.millis(50);
 	}
	
	@SuppressWarnings("serial")
	@Test
	public void testNotifiesMultipleListenersWhenClientDisconnected() throws Exception {
		final SubscriptionListener listener = context.mock(SubscriptionListener.class);
		final SubscriptionListener listenerTwo = context.mock(SubscriptionListener.class, "SubscriptionListenerTwo");
		subscriptionManager.addSubscriptionListener(listener);
		subscriptionManager.addSubscriptionListener(listenerTwo);
		
		context.checking(new Expectations() {{
			one(clientManager).findOrCreate(request); will(returnValue(client));			
			one(listener).onSubscribe(topic, client);
			one(listenerTwo).onSubscribe(topic, client);
			allowing(client).getSubscriptions(); will(setContaining(topic));
			one(client).isConnected(); will(returnValue(false));
			one(clientManager).remove(client);
			one(listener).onUnSubscribe(topic, client);
			one(listenerTwo).onUnSubscribe(topic, client);
			one(client).destroy();
		}
		
		private Action setContaining(final String topic) {
			return returnValue(new HashSet<String>() {{ add(topic); }});
		}});
		
		subscriptionManager.addSubscription(request);
		subscriptionManager.setReconnectionTimeout(0);
		subscriptionManager.clientDisconnected(client);
		Sleep.millis(50);
	}
	
	public void testDisconnectSchedulesClientForRemoval() throws Exception {
		final ScheduledExecutorService scheduler = context.mock(ScheduledExecutorService.class);
		context.checking(new Expectations() {{
			one(scheduler).schedule(with(any(Runnable.class)), with(any(long.class)), with(any(TimeUnit.class)));
		}});
		
		subscriptionManager.setScheduler(scheduler);
		subscriptionManager.clientDisconnected(client);
	}

	@Test
	public void testDisconnectDestroysClient() throws Exception {
		context.checking(new Expectations() {{
			one(clientManager).remove(client);
			allowing(client).getSubscriptions();
			one(client).isConnected(); will(returnValue(false));
			one(client).destroy();
		}});
		
		subscriptionManager.clientDisconnected(client);
	}

	@Test
	public void testSecondConnectionLossDoesNotScheduleAdditionalRemoval() throws Exception {
		final ScheduledExecutorService scheduler = context.mock(ScheduledExecutorService.class);
		context.checking(new Expectations() {{
			one(scheduler).isShutdown();
			one(scheduler).schedule(with(any(Runnable.class)), with(any(long.class)), with(any(TimeUnit.class)));
		}});
		
		subscriptionManager.setScheduler(scheduler);
		subscriptionManager.clientLostConnection(client);
		subscriptionManager.clientLostConnection(client);
	}
	
	@Test
	public void testReconnectionShutsDownScheduledRemoval() throws Exception {
		final ScheduledExecutorService scheduler = context.mock(ScheduledExecutorService.class);
		final ScheduledFuture<?> scheduledRemoval = context.mock(ScheduledFuture.class);
		context.checking(new Expectations() {{
			one(scheduler).isShutdown();
			one(scheduler).schedule(with(any(Runnable.class)), with(any(long.class)), with(any(TimeUnit.class)));
				will(returnValue(scheduledRemoval));
			one(scheduledRemoval).cancel(false);
		}});
		
		subscriptionManager.setScheduler(scheduler);
		subscriptionManager.clientConnected(client);
		subscriptionManager.clientLostConnection(client);
		subscriptionManager.clientConnected(client);
	}
}
