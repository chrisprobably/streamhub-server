package com.streamhub.handler;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.streamhub.util.ArrayUtils;
import com.streamhub.util.WebSocketUtils;

public class WebSocketConnectionTest {
	@Test
	public void parsesSingleWellFormedMessage() throws Exception {
		WebSocketConnection connection = new WebSocketConnection(null, null);
		byte[] arrayOfMessages = WebSocketUtils.createMessage("Hello World");
		List<String> result = connection.parseMessages(arrayOfMessages);
		assertEquals(1, result.size());
		assertEquals("Hello World", result.get(0));
	}
	
	@Test
	public void parsesMultipleWellFormedMessages() throws Exception {
		WebSocketConnection connection = new WebSocketConnection(null, null);
		byte[] first = WebSocketUtils.createMessage("Hello World");
		byte[] second = WebSocketUtils.createMessage("Bye Now");
		byte[] messages = ArrayUtils.concat(first, second);
		List<String> result = connection.parseMessages(messages);
		assertEquals(2, result.size());
		assertEquals("Hello World", result.get(0));
		assertEquals("Bye Now", result.get(1));
	}

	@Test
	public void returnsOnlyTheWellFormedMessagesFromRawDataContainingIncompleteMessages() throws Exception {
		WebSocketConnection connection = new WebSocketConnection(null, null);
		byte[] arrayOfMessages = ArrayUtils.concat("aiofiashof".getBytes(), WebSocketUtils.createMessage("Hello World"));
		List<String> result = connection.parseMessages(arrayOfMessages);
		assertEquals(1, result.size());
		assertEquals("Hello World", result.get(0));
	}
	
	@Test
	public void returnsOnlyTheWellFormedMessagesFromRawDataContainingSeveralIncompleteMessages() throws Exception {
		WebSocketConnection connection = new WebSocketConnection(null, null);
		byte[] arrayOfMessages = ArrayUtils.concatAll("aiofiashof".getBytes(), WebSocketUtils.createMessage("Hello World"), "aiofiashof".getBytes(), new byte[] { 0x00 }, "sadfsf".getBytes());
		List<String> result = connection.parseMessages(arrayOfMessages);
		assertEquals(1, result.size());
		assertEquals("Hello World", result.get(0));
	}
	
	@Test
	public void parsesBlankMessages() throws Exception {
		WebSocketConnection connection = new WebSocketConnection(null, null);
		byte[] arrayOfMessages = WebSocketUtils.createMessage("");
		List<String> result = connection.parseMessages(arrayOfMessages);
		assertEquals(1, result.size());
		assertEquals("", result.get(0));
	}
}
