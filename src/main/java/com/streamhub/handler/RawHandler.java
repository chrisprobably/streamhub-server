package com.streamhub.handler;

import java.nio.channels.ClosedChannelException;

import org.apache.log4j.Logger;

import com.streamhub.Connection;
import com.streamhub.DirectHandler;
import com.streamhub.util.SocketUtils;

public class RawHandler implements Handler {
	private static final Logger log = Logger.getLogger(RawHandler.class);
	private final Handler cometHandler;
	private final Handler directHandler;

	public RawHandler(Handler cometHandler, Handler directHandler) {
		this.cometHandler = cometHandler;
		this.directHandler = directHandler;
	}

	public void handle(Connection connection) {
		try {
			redirect(connection);
		} catch (Exception e) {
			if (e instanceof ClosedChannelException) {
				log.debug("Connection closed " + SocketUtils.toString(connection));
			} else {
				log.error("Error reading connection " + connection, e);
			}
			connection.close();
		}
	}

	private void redirect(Connection connection) throws Exception {
		String peek = new String(connection.peekBytes());
		log.debug("Peek was: " + peek);
		
		if (peek.length() == 0) {
			connection.close();
			return;
		}
		
		if (DirectHandler.DIRECT_MESSAGE_SEPARATOR.equals(peek)) {
			directHandler.handle(connection);
		} else {
			cometHandler.handle(connection);
		}
	}
}
