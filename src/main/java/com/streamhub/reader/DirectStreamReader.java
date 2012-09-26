package com.streamhub.reader;

import java.io.InputStream;


public class DirectStreamReader extends StreamReader {
	private MessageListener listener;
	private int messagesRead;

	public DirectStreamReader(InputStream inputStream) {
		super(inputStream);
	}

	public void setMessageListener(MessageListener listener) {
		this.listener = listener;
	}

	@Override
	protected void receivedData() {
		String inputSoFar = getInputSoFar();
		int offset = messagesRead;
		messagesRead += DirectMessageReader.readDirectMessages(listener, inputSoFar, offset);
	}
}
