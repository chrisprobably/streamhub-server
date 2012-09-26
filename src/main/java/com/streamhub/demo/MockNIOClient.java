package com.streamhub.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.streamhub.util.ChannelUtils;
import com.streamhub.util.HttpClient;
import com.streamhub.util.Random;
import com.streamhub.util.Sleep;
import com.streamhub.util.SocketUtils;

class MockNIOClient {
	private static final String STREAMHUB_CONTEXT = "/streamhub";
	private static final String RESPONSE_OK = "response OK";
	private static final String CRLFx2 = "\r\n\r\n";
	private static final String GET = "GET ";
	private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\"timestamp\":\"(\\d+)\"");
	private final int uid;
	private final String connectionUrl;
	private final int updatesPerSecond;
	private final String subscriptionBaseUrl;
	private SocketChannel channel;
	private int totalUpdatesReceived;
	private int totalBytesReceived;
	private long startTime;
	private boolean isConnected;
	private ByteBuffer readBuffer = ByteBuffer.allocate(1024);
	private double averageLatency;
	private boolean isReading;

	public MockNIOClient(int uid, InetSocketAddress serverAddress, int updatesPerSecond) {
		this.uid = uid;
		this.updatesPerSecond = updatesPerSecond;
		this.connectionUrl = STREAMHUB_CONTEXT + "/response/?uid=" + uid + "&domain=127.0.0.1";
		this.subscriptionBaseUrl = "http://" + serverAddress.getHostName() + ":" + serverAddress.getPort()
				+ STREAMHUB_CONTEXT + "/subscribe/?uid=" + uid + "&domain=127.0.0.1&topic=";
	}

	public void disconnect() {
		SocketUtils.closeQuietly(channel);
	}

	public void setChannel(SocketChannel channel) {
		this.channel = channel;
	}

	public AbstractSelectableChannel getChannel() {
		return channel;
	}

	public synchronized void onChannelWritable(SocketChannel channel) {
		try {
			String connectionRequest = new StringBuilder(GET).append(connectionUrl).append(CRLFx2).toString();
			ChannelUtils.write(channel, connectionRequest);
			subscribe();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void subscribe() throws MalformedURLException, IOException {
		for (int i = 0; i < updatesPerSecond; i++) {
			String topic = "topic-" + i;
			get(topic);
		}
	}

	private void get(String topic) {
		try {
			HttpClient.get(new URL(subscriptionBaseUrl + topic));
		} catch (Exception e) {
			System.out.println("MockClient.get() Error");
			e.printStackTrace();
			Sleep.seconds((int) Random.numberBetween(5, 30));
			get(topic);
		}
	}

	public synchronized void onChannelReadable(SocketChannel channel) {
		isReading = true;
		try {
			String input = "";
			synchronized (readBuffer) {
				readBuffer.clear();
				input = ChannelUtils.read(channel, readBuffer);
			}
			
			handle(input);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			isReading = false;
		}
	}

	private void handle(String input) {
		if (input.length() > 0) {
			if (input.contains(RESPONSE_OK)) {
				isConnected = true;
				startTime = System.currentTimeMillis();
			}

			long latency = 0;
			int updates = 0;
			Matcher timestampMatcher = TIMESTAMP_PATTERN.matcher(input);
			while(timestampMatcher.find()) {
				long timestamp = Long.parseLong(timestampMatcher.group(1));
				latency += System.currentTimeMillis() - timestamp;
				totalUpdatesReceived++;
				updates++;
			}
			
			averageLatency = (double) latency / (double) updates;
			totalBytesReceived += input.length();
		}
	}

	public double getBytesPerSecond() {
		double secondsSinceStart = (System.currentTimeMillis() - startTime) / 1000.0;
		return (double) totalBytesReceived / secondsSinceStart;
	}

	public double getUpdatesPerSecond() {
		double secondsSinceStart = (System.currentTimeMillis() - startTime) / 1000.0;
		return (double) totalUpdatesReceived / secondsSinceStart;
	}
	
	public double getAverageLatency() {
		return averageLatency;
	}

	public boolean isConnected() {
		return isConnected;
	}

	public void onChannelConnect() {
	}

	public int getUid() {
		return uid;
	}

	public boolean isReading() {
		return isReading;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + uid;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MockNIOClient other = (MockNIOClient) obj;
		if (uid != other.uid)
			return false;
		return true;
	}	
}
