package com.streamhub.reader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.log4j.xml.DOMConfigurator;

import com.streamhub.util.Sleep;
import com.streamhub.util.StreamUtils;

public class MeasuringHttpStreamReader extends HttpStreamReader {
	private int totalMessagesReceived = 0;
	private int totalBytesReceived = 0;
	private long startTimeMillis = 0;
	
	static {
		DOMConfigurator.configure("conf/log4j.xml");
	}
	
	public MeasuringHttpStreamReader(InputStream inputStream, long uid) throws IOException {
		super(inputStream);
		if (inputStream == null) {
			throw new IOException("InputStream is null!");
		}
	}

	public synchronized double getBytesPerSecond() {
		int secondsSinceStart = (int) ((System.currentTimeMillis() - startTimeMillis) / 1000);
		
		if (secondsSinceStart < 1) {
			secondsSinceStart = 1;
		}
		
		return (double)totalBytesReceived / secondsSinceStart;
	}

	public synchronized double getUpdatesPerSecond() {
		long secondsSinceStart = ((System.currentTimeMillis() - startTimeMillis) / 1000);
		
		if (secondsSinceStart < 1) {
			secondsSinceStart = 1;
		}
		
		int numberOfMessagesReceived = getNumberOfMessagesReceived();
		return (double)numberOfMessagesReceived / secondsSinceStart;
	}

	@Override
	protected synchronized void receivedData() {
		if (startTimeMillis == 0) {
			startTimeMillis = System.currentTimeMillis();
		}
		
		String data = lastResponse;
		totalBytesReceived += data.length();
		
		Matcher matcher = HttpStreamReader.MESSAGE_PATTERN.matcher(lastResponse);
		while(matcher.find()) {
			totalMessagesReceived++;
		}
	}

	@Override
	public int getNumberOfMessagesReceived() {
		return totalMessagesReceived;
	}

	@Override
	public List<String> getStreamingMessages() {
		return Collections.emptyList();
	}

	@Override
	public void close() {
		super.close();
	}

	@Override
	public String getInputSoFar() {
		return lastResponse;
	}

	@Override
	public String getLastResponse() {
		return lastResponse;
	}

	@Override
	public boolean isStarted() {
		return isStarted;
	}

	@Override
	public void run() {
		try {
			readStream();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			StreamUtils.closeQuietly(inputStream);
		}
	}
	
	private void readStream() throws IOException {
		isStarted = true;
		byte[] buffer = new byte[1024];
		
		outer:
		while (isStarted) {
			while (inputStream.available() > 0) {
				int ch = inputStream.read(buffer, 0, 1024);
	
				if (isEndOfStream(ch)) {
					break outer;
				}
	
				synchronized (lastResponse) {
					lastResponse = new String(buffer, 0, ch);
				}
				
				receivedData();
			}
			Sleep.millis(100);
		}
	}

	private static boolean isEndOfStream(int i) {
		return i < 0;
	}
}
