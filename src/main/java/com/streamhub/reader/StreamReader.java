package com.streamhub.reader;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

import com.streamhub.util.Sleep;
import com.streamhub.util.StreamUtils;

public abstract class StreamReader implements Runnable {
	private static final Logger log = Logger.getLogger(StreamReader.class);
	protected final StringBuffer stringBuffer = new StringBuffer();
	protected String lastResponse = "";
	protected boolean isStarted = false;
	protected final InputStream inputStream;

	public StreamReader(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	public void run() {
		Thread.currentThread().setName("StreamReader");
		try {
			readStream();
		} catch (IOException e) {
			if (isStarted) {
				log.error("Error reading stream", e);
			}
		} finally {
			isStarted = false;
			StreamUtils.closeQuietly(inputStream);
		}
	}

	public String getInputSoFar() {
		String input = "";
	
		synchronized (stringBuffer) {
			input = stringBuffer.toString();
		}
	
		return input;
	}

	public String getLastResponse() {
		return lastResponse;
	}

	public void close() {
		isStarted = false;
		StreamUtils.closeQuietly(inputStream);
	}
	
	public boolean isStarted() {
		return isStarted;
	}

	private void readStream() throws IOException {
		if(inputStream == null) {
			return;
		}
		
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
	
				synchronized (stringBuffer) {
					stringBuffer.append(lastResponse);
				}
				
				receivedData();
			}
			Sleep.millis(100);
		}
	}

	protected abstract void receivedData();

	private static boolean isEndOfStream(int i) {
		return i < 0;
	}
}