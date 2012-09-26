package com.streamhub.handler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import com.streamhub.Connection;
import com.streamhub.request.Request;
import com.streamhub.util.StreamUtils;

public class ForwardingHandler implements Handler {

	private static final Logger log = Logger.getLogger(ForwardingHandler.class);
	private final InetSocketAddress targetAddress;

	public ForwardingHandler(InetSocketAddress targetAddress) {
		this.targetAddress = targetAddress;
	}

	public void handle(Connection connection) {
		try {
			Request request = connection.getRequest();
			handle(connection, request);
		} catch (IOException e) {
			log.warn("Could not forward request", e);
		}
	}

	private void handle(Connection connection, Request request) throws IOException {
		connection.setSelfClosing(true);
		byte[] input = connection.readBytes();
		byte[] response = StreamUtils.nioToBytes(targetAddress, new String(input));
		connection.write(ByteBuffer.wrap(response));
	}
}
