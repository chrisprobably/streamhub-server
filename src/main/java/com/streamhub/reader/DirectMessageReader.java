package com.streamhub.reader;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.streamhub.DirectHandler;

public class DirectMessageReader {
	private static final int GROUP = 1;
	private static final String MESSAGE_REGEX = DirectHandler.DIRECT_MESSAGE_SEPARATOR + "(.+?)" + DirectHandler.DIRECT_MESSAGE_SEPARATOR;
	private static final Pattern MESSAGE_PATTERN = Pattern.compile(MESSAGE_REGEX);
	
	public static int readDirectMessages(MessageListener listener, String inputSoFar, int offset) {
		int messagesRead = 0;
		List<String> messages = new ArrayList<String>();
		Matcher matcher = MESSAGE_PATTERN.matcher(inputSoFar);
		
		while(matcher.find()) {
			messages.add(matcher.group(GROUP));
		}
		
		for (int i = offset; i < messages.size(); i++) {
			messagesRead++;
			listener.onMessage(messages.get(i));
		}
		
		return messagesRead;
	}
	
	public static void readDirectMessages(MessageListener listener, String inputSoFar) {
		Matcher matcher = MESSAGE_PATTERN.matcher(inputSoFar);
		
		while(matcher.find()) {
			listener.onMessage(matcher.group(GROUP));
		}
	}
}