package com.streamhub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.streamhub.request.DirectRequest;
import com.streamhub.request.Request;

public class DirectRequestTest {
	private static final String UID = "32432432";
	private static final String TOPIC = "AAPL";
	private static final String PUBLISH_JSON = "{\"topic\":\"AAPL\",\"price\":\"23.78234\"}";
	private static final String[] TOPICS = {"AAPL", "MSFT", "GOOG"};
	
	@Test
	public void creatingFromMessage() throws Exception {
		Request request = DirectRequest.createFrom(createDirectConnectionRequest());
		assertEquals(UID, request.getUid());
	}

	@Test
	public void isResponseConnection() throws Exception {
		Request request = DirectRequest.createFrom(createDirectConnectionRequest());
		assertTrue(request.isResponseConnection());
	}
	
	@Test
	public void isSubscription() throws Exception {
		Request request = DirectRequest.createFrom(createSubscriptionRequest());
		assertTrue(request.isSubscription());
	}
	
	@Test
	public void isUnSubscribe() throws Exception {
		Request request = DirectRequest.createFrom(createUnSubscribeRequest());
		assertTrue(request.isUnSubscribe());
	}

	@Test
	public void identifiesPublish() throws Exception {
		Request request = DirectRequest.createFrom(createPublishRequest());
		assertTrue(request.isPublish());
	}
	
	@Test
	public void getsTopicAndPayloadOnPublish() throws Exception {
		Request request = DirectRequest.createFrom(createPublishRequest());
		assertEquals("AAPL", request.getPublishTopic());
		assertEquals(PUBLISH_JSON, request.getPayload().toString());
	}

	@Test
	public void gettingSubscriptionTopic() throws Exception {
		Request request = DirectRequest.createFrom(createSubscriptionRequest());
		assertEquals(TOPIC, request.getSubscriptionTopics()[0]);
	}
	@Test
	public void gettingMultipleSubscriptionTopics() throws Exception {
		Request request = DirectRequest.createFrom(createMultipleSubscriptionRequest());
		Assert.assertArrayEquals(TOPICS, request.getSubscriptionTopics());
	}
	
	@Test
	public void gettingMultipleUnSubscribeTopics() throws Exception {
		Request request = DirectRequest.createFrom(createMultipleUnSubscribeRequest());
		Assert.assertArrayEquals(TOPICS, request.getSubscriptionTopics());
	}
	
	@Test
	public void isDisconnection() throws Exception {
		Request request = DirectRequest.createFrom(createDisconnectionRequest());
		assertTrue(request.isDisconnection());
	}
	
	@Test
	public void creatingWithUid() throws Exception {
		String uid = "23423";
		Request request = DirectRequest.createFrom(createMultipleSubscriptionRequest(), uid);
		assertEquals(uid, request.getUid());
		Assert.assertArrayEquals(TOPICS, request.getSubscriptionTopics());
	}
	
	public static String createDirectConnectionRequest() {
		return "uid=" + UID;
	}

	private static String createSubscriptionRequest() {
		return "subscribe=" + TOPIC;
	}
	
	
	private static String createUnSubscribeRequest() {
		return "unsubscribe=" + TOPIC;
	}
	
	private static String createMultipleSubscriptionRequest() {
		return "subscribe=" + commaSeparatedTopics();
	}
	
	private static String createMultipleUnSubscribeRequest() {
		return "unsubscribe=" + commaSeparatedTopics();
	}
	
	private static String createDisconnectionRequest() {
		return "disconnect";
	}

	private static String commaSeparatedTopics() {
		return Arrays.toString(TOPICS).replace("[", "").replace("]", "").replace(" ","");
	}

	private static String createPublishRequest() {
		return "publish(AAPL," + PUBLISH_JSON + ")";
	}
}
