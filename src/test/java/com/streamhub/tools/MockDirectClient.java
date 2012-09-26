package com.streamhub.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.streamhub.DirectHandler;
import com.streamhub.api.Payload;
import com.streamhub.reader.DirectStreamReader;
import com.streamhub.reader.MessageListener;
import com.streamhub.util.SocketUtils;
import com.streamhub.util.StreamUtils;
import com.thoughtworks.selenium.condition.Condition;
import com.thoughtworks.selenium.condition.ConditionRunner.Context;

public class MockDirectClient implements MessageListener {
	private static final String SEP = DirectHandler.DIRECT_MESSAGE_SEPARATOR;
	private final List<String> messages = new ArrayList<String>();
	private final ConditionRunner conditionRunner = new ConditionRunner(100, 5000);
	private final ExecutorService threadPool = Executors.newCachedThreadPool();
	private final String uid;
	
	private OutputStream outputStream;
	private InputStream inputStream;
	private Socket socket;
	

	public MockDirectClient(String uid) {
		this.uid = uid;
	}
	
	public void connect(String host, int port) throws UnknownHostException, IOException {
		socket = new Socket(host, port);
		outputStream = socket.getOutputStream();
		inputStream = socket.getInputStream();
		DirectStreamReader directStreamReader = new DirectStreamReader(inputStream);
		directStreamReader.setMessageListener(this);
		threadPool.execute(directStreamReader);
		StreamUtils.write(outputStream, DirectHandler.MAGIC_DIRECT_CONNECTION_STRING + SEP + "uid=" + uid + SEP);
	}
	
	public void subscribe(String topic) throws IOException {
		StreamUtils.write(outputStream, SEP + "subscribe=" + topic + SEP);
	}
	
	public void subscribe(String[] topics) throws IOException {
		String topicList = Arrays.toString(topics).replaceAll("[\\[\\] ]", "");
		System.out.println("topicList=" + topicList);
		StreamUtils.write(outputStream, SEP + "subscribe=" + topicList + SEP);
	}
	
	public void unsubscribe(String topic) throws IOException {
		StreamUtils.write(outputStream, SEP + "unsubscribe=" + topic + SEP);
	}

	public void publish(String topic, Payload payload) throws IOException {
		StreamUtils.write(outputStream, SEP + "publish(" + topic + "," + payload.toString() + ")" + SEP);
	}

	public void onMessage(String message) {
		synchronized (messages) {
			messages.add(message);
		}
	}

	public List<String> getMessages() {
		synchronized (messages) {
			return new ArrayList<String>(messages);
		}
	}

	public void waitForMessageContaining(final String substring) {
		conditionRunner.waitFor(new Condition("Client-" + uid + " failed to get message containing '" + substring + "'") {
			@Override
			public boolean isTrue(Context arg0) {
				synchronized (messages) {
					for (String message : messages) {
						if (message.contains(substring)) {
							return true;
						}
					}
				}
				
				return false;
			}
		});
	}

	public void disconnect() throws IOException {
		StreamUtils.write(outputStream, SEP + "disconnect" + SEP);
		SocketUtils.closeQuietly(socket);
	}
	
	public boolean isConnected() {
		return socket.isConnected();
	}

	public void waitForConnected() {
		conditionRunner.waitFor(new Condition() {
			@Override
			public boolean isTrue(Context arg0) {
				return isConnected();
			}
		});
	}

	public void waitForDisconnected() {
		conditionRunner.waitFor(new Condition() {
			@Override
			public boolean isTrue(Context arg0) {
				return socket.isClosed();
			}
		});
	}
}
