package com.streamhub.reader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpStreamReader extends StreamReader {
	public static final Pattern MESSAGE_PATTERN = Pattern.compile("x\\(.*?\\)");
	private final List<String> messagesReceived = new ArrayList<String>();

	public HttpStreamReader(InputStream inputStream) {
		super(inputStream);
	}

	public List<String> getStreamingMessages() {
		synchronized (messagesReceived) {
			return messagesReceived;
		}
	}

	public int getNumberOfMessagesReceived() {
		synchronized (messagesReceived) {
			return messagesReceived.size();
		}
	}

	@Override
	protected void receivedData() {
		synchronized (messagesReceived) {
			List<String> messages = new ArrayList<String>();
			String inputSoFar = getInputSoFar();
			Matcher matcher = MESSAGE_PATTERN.matcher(inputSoFar);
			while (matcher.find()) {
				messages.add(matcher.group());
			}

			for (int i = messagesReceived.size(); i < messages.size(); i++) {
				messagesReceived.add(messages.get(i));
			}
		}
	}
}
