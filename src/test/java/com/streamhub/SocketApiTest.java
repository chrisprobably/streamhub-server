package com.streamhub;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.streamhub.api.JsonPayload;
import com.streamhub.api.Payload;
import com.streamhub.client.IStreamingClient;
import com.streamhub.nio.NIOServer;
import com.streamhub.reader.DirectStreamReader;
import com.streamhub.reader.MessageListener;
import com.streamhub.request.HttpRequest;
import com.streamhub.request.HttpRequestTest;
import com.streamhub.request.Request;
import com.streamhub.tools.ConditionRunner;
import com.streamhub.tools.browser.MockBrowser;
import com.streamhub.util.Sleep;
import com.streamhub.util.SocketUtils;
import com.thoughtworks.selenium.condition.Condition;
import com.thoughtworks.selenium.condition.ConditionRunner.Context;

@RunWith(JMock.class)
public class SocketApiTest extends StreamingServerTestCase {
	private static final String SEP = DirectHandler.DIRECT_MESSAGE_SEPARATOR;
	private static final String ADD_SUBSCRIPTION_LISTENER_REQUEST = SEP + "addSubscriptionListener" + SEP;
	private static final String REMOVE_SUBSCRIPTION_LISTENER_REQUEST = SEP + "removeSubscriptionListener" + SEP;
	private static final String ADD_PUBLISH_LISTENER_REQUEST = SEP + "addPublishListener" + SEP;
	private static final String REMOVE_PUBLISH_LISTENER_REQUEST = SEP + "removePublishListener" + SEP;
	private static final int SOCKET_API_PORT = 9432;
	private static final String TOPIC = "topic";
	private static final String PUBLISH_REQUEST = SEP + "publish(AAPL,{\"topic\":\"AAPL\",\"price\":\"23.78234\"})" + SEP;
	private static final String SEND_REQUEST = SEP + "send(34,AAPL,{\"topic\":\"AAPL\",\"price\":\"23.78234\"})" + SEP;
	private final ConditionRunner conditionRunner = new ConditionRunner(50, 4000);
	private NIOServer server;
	private Socket socket;
	private Mockery context;
	private Request request;
	private Connection connection;
	private Request requestTwo;
	private IStreamingClient client;
	private IStreamingClient clientTwo;
	private StreamingSubscriptionManager subscriptionManager;
	private URL serverUrl;

	@Override @Before
	public void setUp() throws Exception {
		super.setUp();
		context = new Mockery();
		context.setImposteriser(ClassImposteriser.INSTANCE);
		connection = context.mock(Connection.class);
		client = context.mock(IStreamingClient.class);
		clientTwo = context.mock(IStreamingClient.class, "clientTwo");
		final String httpRequest = HttpRequestTest.buildHttpRequest("/subscribe/?uid=1&domain=127.0.0.1&topic="
				+ TOPIC);
		final String httpRequestTwo = HttpRequestTest.buildHttpRequest("/subscribe/?uid=2&domain=127.0.0.1&topic="
				+ TOPIC);
		context.checking(new Expectations() {{
				allowing(client).getUid();
					will(returnValue("1"));
				allowing(clientTwo).getUid();
					will(returnValue("2"));
				allowing(connection).readBytes();
					will(onConsecutiveCalls(returnValue(httpRequest.getBytes()), returnValue(httpRequestTwo.getBytes())));
		}});


		request = HttpRequest.createFrom(connection);
		requestTwo = HttpRequest.createFrom(connection);
		server = new NIOServer(new InetSocketAddress(9321), new InetSocketAddress(SOCKET_API_PORT));
		server.start();
		subscriptionManager = (StreamingSubscriptionManager) server.getSubscriptionManager();
		serverUrl = new URL("http://localhost:9321/streamhub/");
	}

	@Override @After
	public void tearDown() throws Exception {
		server.stop();
		if (socket != null) {
			SocketUtils.closeQuietly(socket);
		}
		super.tearDown();
	}

	@Test
	public void testConnectingToSocketApiPort() throws Exception {
		socket = new Socket("localhost", SOCKET_API_PORT);
		socket.getInputStream();
	}

	@Test
	public void testAddingSingleSubscriptionListener() throws Exception {
		final List<String> replies = addSelfAsSubscriptionListener();
		subscriptionManager.addSubscription(request);
		waitForReplies(replies, 1);
		assertEquals("onSubscribe(" + TOPIC + ",1)", replies.get(0));
	}
	
	@Test
	public void testGettingOnUnSubscribeViaSubscriptionListener() throws Exception {
		final List<String> replies = addSelfAsSubscriptionListener();
		subscriptionManager.addSubscription(request);
		subscriptionManager.removeSubscription(request);
		waitForReplies(replies, 2);
		assertEquals("onSubscribe(" + TOPIC + ",1)", replies.get(0));
		assertEquals("onUnSubscribe(" + TOPIC + ",1)", replies.get(1));
	}
	
	@Test
	public void testGettingMultipleOnSubscribeCallbacksOnSingleSubscriptionListener() throws Exception {
		final List<String> replies = addSelfAsSubscriptionListener();
		
		subscriptionManager.addSubscription(request);
		subscriptionManager.addSubscription(requestTwo);
		waitForReplies(replies, 2);
		assertEquals("onSubscribe(" + TOPIC + ",1)", replies.get(0));
		assertEquals("onSubscribe(" + TOPIC + ",2)", replies.get(1));
	}
	
	@Test
	public void testDoNotGetUpdatesAfterRemovingSubscriptionListener() throws Exception {
		final List<String> replies = addSelfAsSubscriptionListener();
		subscriptionManager.addSubscription(request);
		waitForReplies(replies, 1);
		assertEquals("onSubscribe(" + TOPIC + ",1)", replies.get(0));
		removeSelfAsSubscriptionListener();
		subscriptionManager.addSubscription(requestTwo);
		Sleep.millis(500);
		assertFalse(replies.size() > 1);
	}

	@Test
	public void testGettingMultipleOnSubscribeAndOnUnSubscribeCallbacksOnSingleSubscriptionListener() throws Exception {
		final List<String> replies = addSelfAsSubscriptionListener();
		subscriptionManager.addSubscription(request);
		subscriptionManager.addSubscription(requestTwo);
		Sleep.millis(200);
		subscriptionManager.removeSubscription(requestTwo);
		subscriptionManager.removeSubscription(request);
		waitForReplies(replies, 4);
		assertEquals("onSubscribe(" + TOPIC + ",1)", replies.get(0));
		assertEquals("onSubscribe(" + TOPIC + ",2)", replies.get(1));
		assertEquals("onUnSubscribe(" + TOPIC + ",2)", replies.get(2));
		assertEquals("onUnSubscribe(" + TOPIC + ",1)", replies.get(3));
	}
	
	@Test
	public void testAddingSinglePublishListener() throws Exception {
		final List<String> replies = addSelfAsPublishListener();
		JsonPayload payload = new JsonPayload("aloha");
		subscriptionManager.notifyPublishListeners(client, TOPIC, payload);
		waitForReplies(replies, 1);
		assertEquals("onMessageReceived(1," + TOPIC + "," + payload.toString() + ")", replies.get(0));
	}
	
	@Test
	public void testGettingMultipleOnMessageReceivedCallbacksOnSingleListener() throws Exception {
		final List<String> replies = addSelfAsPublishListener();
		JsonPayload payload = new JsonPayload("aloha");
		JsonPayload payloadTwo = new JsonPayload("bonza");
		subscriptionManager.notifyPublishListeners(client, TOPIC, payload);
		subscriptionManager.notifyPublishListeners(clientTwo, TOPIC, payloadTwo);
		waitForReplies(replies, 2);
		assertTrue(replies.contains("onMessageReceived(1," + TOPIC + "," + payload.toString() + ")"));
		assertTrue(replies.contains("onMessageReceived(2," + TOPIC + "," + payloadTwo.toString() + ")"));
	}
	
	@Test
	public void testDoNotGetOnMessageReceivedCallbacksAfterRemovingListener() throws Exception {
		final List<String> replies = addSelfAsPublishListener();
		JsonPayload payload = new JsonPayload("aloha");
		JsonPayload payloadTwo = new JsonPayload("bonza");
		Sleep.millis(250);
		subscriptionManager.notifyPublishListeners(client, TOPIC, payload);
		Sleep.millis(250);
		removeSelfAsPublishListener();
		Sleep.millis(250);
		subscriptionManager.notifyPublishListeners(clientTwo, TOPIC, payloadTwo);
		Sleep.millis(250);
		waitForReplies(replies, 1);
		assertTrue(replies.contains("onMessageReceived(1," + TOPIC + "," + payload.toString() + ")"));
		Sleep.millis(250);
		assertEquals(1, replies.size());
	}
	
	@Test
	public void testPublishing() throws Exception {
		String expectedJson = "{\"topic\":\"AAPL\",\"price\":\"23.78234\"}";
		MockBrowser browser = browserSubscribedTo("AAPL");
		final List<String> replies = new ArrayList<String>();
		socket = new Socket("localhost", SOCKET_API_PORT);
		startStreamReader(replies);
		socket.getOutputStream().write(generateConnectionString().getBytes());
		socket.getOutputStream().write(PUBLISH_REQUEST.getBytes());
		browser.waitForMessages(2);
		assertTrue(browser.hasReceived(expectedJson));
	}

	@Test
	public void testPublishingMultipleMessages() throws Exception {
		String expectedJson = "{\"topic\":\"AAPL\",\"price\":\"23.78234\"}";
		String expectedSecondJson = "{\"topic\":\"AAPL\",\"price\":\"43.544\"}";
		MockBrowser browser = browserSubscribedTo("AAPL");
		final List<String> replies = new ArrayList<String>();
		socket = new Socket("localhost", SOCKET_API_PORT);
		startStreamReader(replies);
		socket.getOutputStream().write(generateConnectionString().getBytes());
		socket.getOutputStream().write(PUBLISH_REQUEST.getBytes());
		socket.getOutputStream().write((SEP + "publish(AAPL," + expectedSecondJson + ")" + SEP).getBytes());
		browser.waitForMessages(2);
		assertTrue(browser.hasReceived(expectedJson));
		assertTrue(browser.hasReceived(expectedSecondJson));
	}
	
//	@Test
//	public void testSendingFragmentedMessages() throws Exception {
//		String expectedJson = "{\"topic\":\"AAPL\",\"price\":\"23.78234\"}";
//		String expectedSecondJson = "{\"topic\":\"AAPL\",\"price\":\"43.544\"}";
//		MockBrowser browser = browserSubscribedTo("AAPL");
//		final List<String> replies = new ArrayList<String>();
//		socket = new Socket("localhost", SOCKET_API_PORT);
//		startStreamReader(replies);
//		socket.getOutputStream().write(generateConnectionString().getBytes());
//		socket.getOutputStream().write(PUBLISH_REQUEST.getBytes());
//		String fragmentOne = SEP + "publish(AAPL," + expectedSecondJson;
//		String fragmentTwo = ")" + SEP;
//		socket.getOutputStream().write((fragmentOne).getBytes());
//		Sleep.millis(500);
//		socket.getOutputStream().write((fragmentTwo).getBytes());
//		browser.waitForMessages(3);
//		assertTrue(browser.hasReceived(expectedJson));
//		assertTrue(browser.hasReceived(expectedSecondJson));
//	}

// TODO: Should be in performance tests.	
//	@Test
//	public void testPublishingFloodOfMessages() throws Exception {
//		int numberOfMessages = 3000;
//		String[] expectedMessages = new String[numberOfMessages];
//		for (int i = 0; i < numberOfMessages; i ++) {
//			expectedMessages[i] = "{\"topic\":\"AAPL\",\"price\":\"" + i + "\"}";
//		}
//		MockBrowser browser = browserSubscribedTo("AAPL");
//		final List<String> replies = new ArrayList<String>();
//		socket = new Socket("localhost", SOCKET_API_PORT);
//		startStreamReader(replies);
//		socket.getOutputStream().write(generateConnectionString().getBytes());
//		for (int i = 0; i < numberOfMessages; i ++) {
//			socket.getOutputStream().write((SEP + "publish(AAPL," + expectedMessages[i] + ")" + SEP).getBytes());
//		}
//		browser.waitForMessages(numberOfMessages + 1);
//		for (int i = 0; i < numberOfMessages; i ++) {
//			assertTrue(browser.hasReceived(expectedMessages[i]));
//		}
//	}
//	@Test
//	public void testPublishingFloodOfMessagesIntermittently() throws Exception {
//		int numberOfMessages = 1000;
//		String[] expectedMessages = new String[numberOfMessages];
//		for (int i = 0; i < numberOfMessages; i ++) {
//			expectedMessages[i] = "{\"topic\":\"AAPL\",\"price\":\"" + i + "\"}";
//		}
//		MockBrowser browser = browserSubscribedTo("AAPL");
//		final List<String> replies = new ArrayList<String>();
//		socket = new Socket("localhost", SOCKET_API_PORT);
//		startStreamReader(replies);
//		socket.getOutputStream().write(generateConnectionString().getBytes());
//		for (int i = 0; i < numberOfMessages; i ++) {
//			socket.getOutputStream().write((SEP + "publish(AAPL," + expectedMessages[i] + ")" + SEP).getBytes());
//			Random.sleepBetween(1, 40);
//		}
//		browser.waitForMessages(numberOfMessages + 1);
//		for (int i = 0; i < numberOfMessages; i ++) {
//			assertTrue(browser.hasReceived(expectedMessages[i]));
//		}
//	}
	
	@Test
	public void testSendingToIndividualClient() throws Exception {
		String expectedJson = "{\"topic\":\"AAPL\",\"price\":\"23.78234\"}";
		MockBrowser clientOne = browserSubscribedTo("AAPL");
		MockBrowser clientTwo = new MockBrowser(serverUrl, 35);
		clientTwo.connectToStreamingServer();
		clientTwo.subscribe("AAPL");
		clientTwo.waitForSubscribed();
		final List<String> replies = new ArrayList<String>();
		socket = new Socket("localhost", SOCKET_API_PORT);
		startStreamReader(replies);
		socket.getOutputStream().write(generateConnectionString().getBytes());
		socket.getOutputStream().write(SEND_REQUEST.getBytes());
		clientOne.waitForMessages(2);
		assertTrue(clientOne.hasReceived(expectedJson));
		assertFalse(clientTwo.hasReceived(expectedJson));
	}

	@Test
	public void testSendingToMultipleIndividualClients() throws Exception {
		String expectedJson = "{\"topic\":\"AAPL\",\"price\":\"23.78234\"}";
		MockBrowser clientOne = browserSubscribedTo("AAPL");
		MockBrowser clientTwo = new MockBrowser(serverUrl, 35);
		clientTwo.connectToStreamingServer();
		clientTwo.subscribe("AAPL");
		clientTwo.waitForSubscribed();
		MockBrowser clientThree = new MockBrowser(serverUrl, 36);
		clientThree.connectToStreamingServer();
		clientThree.subscribe("AAPL");
		clientThree.waitForSubscribed();
		final List<String> replies = new ArrayList<String>();
		socket = new Socket("localhost", SOCKET_API_PORT);
		startStreamReader(replies);
		socket.getOutputStream().write(generateConnectionString().getBytes());
		socket.getOutputStream().write(SEND_REQUEST.getBytes());
		socket.getOutputStream().write((SEP + "send(35,AAPL," + expectedJson + ")" + SEP).getBytes());
		clientOne.waitForMessages(2);
		clientTwo.waitForMessages(2);
		assertTrue(clientOne.hasReceived(expectedJson));
		assertTrue(clientTwo.hasReceived(expectedJson));
		assertFalse(clientThree.hasReceived(expectedJson));
	}
	
	@Test
	public void testRemovesRemoteSubscriptionListenersAfterDisconnection() throws Exception {
		addSelfAsSubscriptionListener();
		SocketUtils.closeQuietly(socket);
		Sleep.seconds(2);
		browserSubscribedTo("RemovesRemoteListener");
		browserSubscribedTo("ShouldGetClosedChannelException");
		waitForRemovedAsSubscriptionListener();
	}
	
	@Test
	public void testRemovesRemotePublishListenersAfterDisconnection() throws Exception {
		addSelfAsPublishListener();
		SocketUtils.closeQuietly(socket);
		Sleep.seconds(2);
		Payload payload = new JsonPayload("SOMETHING");
		subscriptionManager.notifyPublishListeners(client, TOPIC, payload);
		subscriptionManager.notifyPublishListeners(client, TOPIC, payload);
		waitForRemovedAsPublishListener();
	}

	private MockBrowser browserSubscribedTo(String topic) throws MalformedURLException, Exception, UnknownHostException,
			IOException {
		MockBrowser browser = new MockBrowser(serverUrl, 34);
		browser.connectToStreamingServer();
		browser.subscribe(topic);
		browser.waitForSubscribed();
		return browser;
	}
	
	private void waitForReplies(final List<String> replies, final int numberOfReplies) {
		conditionRunner.waitFor(new Condition() {
			@Override
			public boolean isTrue(Context arg0) {
				return replies.size() == numberOfReplies;
			}});
	}
	
	private List<String> addSelfAsPublishListener() throws UnknownHostException, IOException {
		final List<String> replies = new ArrayList<String>();
		socket = new Socket("localhost", SOCKET_API_PORT);
		startStreamReader(replies);
		socket.getOutputStream().write(generateConnectionString().getBytes());
		socket.getOutputStream().write(ADD_PUBLISH_LISTENER_REQUEST.getBytes());
		waitForAddedAsPublishListener();
		return replies;
	}

	private List<String> addSelfAsSubscriptionListener() throws UnknownHostException, IOException {
		final List<String> replies = new ArrayList<String>();
		socket = new Socket("localhost", SOCKET_API_PORT);
		startStreamReader(replies);
		socket.getOutputStream().write(generateConnectionString().getBytes());
		socket.getOutputStream().write(ADD_SUBSCRIPTION_LISTENER_REQUEST.getBytes());
		waitForAddedAsSubscriptionListener();
		return replies;
	}
	
	private void removeSelfAsSubscriptionListener() throws IOException {
		socket.getOutputStream().write(REMOVE_SUBSCRIPTION_LISTENER_REQUEST.getBytes());
		waitForRemovedAsSubscriptionListener();
	}
	
	private void removeSelfAsPublishListener() throws IOException {
		socket.getOutputStream().write(REMOVE_PUBLISH_LISTENER_REQUEST.getBytes());
		waitForRemovedAsPublishListener();
	}

	private void waitForAddedAsSubscriptionListener() {
		conditionRunner.waitFor(new Condition() {
			@Override
			public boolean isTrue(Context arg0) {
				return subscriptionManager.getSubscriptionListeners().size() == 1;
			}});
	}
	
	private void waitForRemovedAsSubscriptionListener() {
		conditionRunner.waitFor(new Condition() {
			@Override
			public boolean isTrue(Context arg0) {
				return subscriptionManager.getSubscriptionListeners().size() == 0;
			}});
	}
	
	private void waitForAddedAsPublishListener() {
		conditionRunner.waitFor(new Condition() {
			@Override
			public boolean isTrue(Context arg0) {
				return subscriptionManager.getPublishListeners().size() == 1;
			}});
	}

	private void waitForRemovedAsPublishListener() {
		conditionRunner.waitFor(new Condition() {
			@Override
			public boolean isTrue(Context arg0) {
				return subscriptionManager.getPublishListeners().size() == 0;
			}});
	}

	private void startStreamReader(final List<String> replies) throws IOException {
		DirectStreamReader streamReader = new DirectStreamReader(socket.getInputStream());
		streamReader.setMessageListener(new MessageListener() {
			public void onMessage(String message) {
				replies.add(message);
			}
		});
		new Thread(streamReader).start();
	}
	
	private static String generateConnectionString() {
		return "@@uid=" + System.currentTimeMillis() + "@@";
	}
	
}
