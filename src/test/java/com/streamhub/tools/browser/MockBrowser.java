package com.streamhub.tools.browser;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.streamhub.reader.HttpStreamReader;
import com.streamhub.tools.ConditionRunner;
import com.streamhub.util.HttpClient;
import com.streamhub.util.Random;
import com.streamhub.util.Sleep;
import com.thoughtworks.selenium.condition.Condition;
import com.thoughtworks.selenium.condition.ConditionRunner.Context;

public class MockBrowser {
	private static final Logger log = Logger.getLogger(MockBrowser.class);
	private static final String RESPONSE_URL_TEMPLATE = "response/?uid=@&domain=cheese.com";
	private static final String SUBSCRIPTION_URL_TEMPLATE = "subscribe/?uid=@&domain=cheese.com&topic=";
	private static final String CONNECTION_URL_TEMPLATE = "request/?uid=@&domain=cheese.com";
	private static final int WAIT_FOR_INTERVAL = 100;
	private static final int WAIT_FOR_TIMEOUT = 7000;
	private String responseUrl;
	private String subscriptionUrl;
	private String connectionUrl;
	protected long uid;
	private boolean hasConnectedOk;
	private boolean hasSubscribedOk;
	private final ConditionRunner conditionRunner = new ConditionRunner(WAIT_FOR_INTERVAL, WAIT_FOR_TIMEOUT);
	private String requestOk = "request OK";
	private String subscriptionOk = "subscription OK";
	protected boolean isResponseThreadStarted;
	protected final ExecutorService threadPool = Executors.newCachedThreadPool();
	protected HttpStreamReader responseIFrameReader;
	
	public MockBrowser() throws MalformedURLException {
		this(new URL("http://localhost:8888/streamhub/"), 1);
	}

	public MockBrowser(long uid) throws MalformedURLException {
		this(new URL("http://localhost:8888/streamhub/"), uid);
	}

	public MockBrowser(URL streamingServerUrl, long uid) {
		this.uid = uid;
		this.responseUrl = streamingServerUrl.toString() + RESPONSE_URL_TEMPLATE.replace("uid=@", "uid=" + uid);
		this.subscriptionUrl = streamingServerUrl.toString() + SUBSCRIPTION_URL_TEMPLATE.replace("uid=@", "uid=" + uid);
		this.connectionUrl = streamingServerUrl.toString() + CONNECTION_URL_TEMPLATE.replace("uid=@", "uid=" + uid);
	}

	public InputStream get(URL url) throws UnknownHostException, IOException {
		try {
			Socket socket = new Socket(url.getHost(), url.getPort());
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			out.println("GET " + url.getPath() + "?" + url.getQuery() + "\r\n\r\n");	
			return socket.getInputStream();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			log.error("Error getting url " + url, e);

			if (e.getMessage().contains("Address already in use") || e.getMessage().contains("Connection refused: connect")) {
				Sleep.seconds(5);
				return get(url);
			}
		}
		
		return null;
	}
	
	public void connectToStreamingServer() throws Exception {
		String connectResponse = getAndCloseStream(connectionUrl);
		if (! isConnectionOk(connectResponse)) {
			hasConnectedOk = false;
			throw new ConnectionFailedException("uid[" + uid + "]: " + connectResponse);
		} else {
			hasConnectedOk = true;
		}
	}
	
	public void subscribe(String topic) throws UnknownHostException, MalformedURLException, IOException {
		if (!isResponseThreadStarted) {
			startResponseThread();
		}
		String subscriptionResponse = getAndCloseStream(subscriptionUrl + topic);
		
		if (subscriptionResponse.length() == 0) {
			Sleep.seconds((int) Random.numberBetween(1, 10));
			subscribe(topic);
		} else {
			hasSubscribedOk = isSubscriptionOk(subscriptionResponse);
		}
	}

	public boolean hasConnectedOk() {
		return hasConnectedOk;
	}
	
	public boolean hasSubscribedOk() {
		return hasSubscribedOk;
	}
	
	public int numberOfMessagesReceived() {
		if (responseIFrameReader == null) {
			return 0;
		}
		
		return responseIFrameReader.getNumberOfMessagesReceived();
	}

	public List<String> getMessagesReceived() {
		return responseIFrameReader.getStreamingMessages();
	}

	public void waitForMessages(final int numMessages) {
		conditionRunner.waitFor(new Condition() {
			@Override
			public boolean isTrue(Context arg0) {
				return numberOfMessagesReceived() >= numMessages;
			}
			
			@Override
			public String getMessage() {
				return "Client-" + uid + " received " + numberOfMessagesReceived() + " messages, expected " + numMessages + ". Messages: " + getMessagesReceived();
			}
		});
	}
	
	public void waitForConnected() {
		conditionRunner.waitFor(new Condition() {
			@Override
			public boolean isTrue(Context arg0) {
				return hasConnectedOk();
			}
		});
	}
	
	public void waitForSubscribed() {
		conditionRunner.waitFor(new Condition() {
			@Override
			public boolean isTrue(Context arg0) {
				return hasSubscribedOk();
			}
		});
	}

	public boolean hasReceived(String messageSubstring) {
		for (String message : getMessagesReceived()) {
			if (message.contains(messageSubstring)) {
				return true;
			}
		}
		
		return false;
	}
	
	public long getUid() {
		return uid;
	}	

	public void unsafeSetUid(long uid) {
		this.uid = uid;
	}

	public void unsafeSetSubscriptionOk(String okResponse) {
		subscriptionOk = okResponse;
	}

	public void unsafeSetRequestOk(String okResponse) {
		requestOk = okResponse;
	}
	
	public boolean isConnected() {
		if (responseIFrameReader == null) {
			return false;
		}
		
		return responseIFrameReader.isStarted();
	}
	
	public void stop() {
		if (responseIFrameReader != null) {
			responseIFrameReader.close();
		}
	}

	protected void startResponseThread() {
		try {
			InputStream responseStream = openResponseIFrame();
			responseIFrameReader = new HttpStreamReader(responseStream);
			threadPool.execute(responseIFrameReader);
			isResponseThreadStarted = true;
		} catch (IOException e) {
			Sleep.seconds((int) Random.numberBetween(1, 10));
			startResponseThread();
		}
	}
	
	protected InputStream openResponseIFrame() throws UnknownHostException, IOException, MalformedURLException {
		return new MockBrowser().get(new URL(responseUrl));
	}
	
	private boolean isSubscriptionOk(String subscriptionResponse) {
		return subscriptionResponse.contains(subscriptionOk);
	}
	
	private boolean isConnectionOk(String connectResponse) {
		return connectResponse.contains(requestOk);
	}
	
	private String getAndCloseStream(String url) throws UnknownHostException, IOException, MalformedURLException {
		return HttpClient.get(new URL(url));
	}

}
