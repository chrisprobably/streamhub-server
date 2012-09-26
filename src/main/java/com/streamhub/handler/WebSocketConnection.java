package com.streamhub.handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import com.streamhub.Connection;
import com.streamhub.WebSocketMessageHandler;
import com.streamhub.WriteRegister;
import com.streamhub.nio.SSLChannel;
import com.streamhub.request.Request;
import com.streamhub.util.ArrayUtils;
import com.streamhub.util.SocketUtils;

public class WebSocketConnection implements Connection {
	private static final byte START_BYTE = 0x00;
	private static final byte END_BYTE = (byte) 0xff;
	protected ByteBuffer readBuffer = ByteBuffer.allocateDirect(1024);
	private byte[] inputSoFar = new byte[0];
	private final Connection connection;
	private String uid;
	private WebSocketMessageHandler messageHandler;

	public WebSocketConnection(Connection connection, WebSocketMessageHandler messageHandler) {
		this.connection = connection;
		this.messageHandler = messageHandler;
	}

	public void close() {
		SocketUtils.closeQuietly(getChannel());
	}

	public String getAttachment() {
		return connection.getAttachment();
	}

	public SocketChannel getChannel() {
		return connection.getChannel();
	}

	public Request getRequest() throws IOException {
		return connection.getRequest();
	}

	public SSLChannel getSSLChannel() {
		return connection.getSSLChannel();
	}

	public boolean isSecure() {
		return connection.isSecure();
	}

	public boolean isSelfClosing() {
		return connection.isSelfClosing();
	}

	public synchronized void onReadableEvent(Handler handler) {
		try {
			this.readAsMuchAsPossible();
		} catch (IOException e) {
			this.close();
		}

		if (inputSoFar.length > 0 && inputSoFar[inputSoFar.length - 1] == END_BYTE) {
			List<String> messages = parseMessages(inputSoFar);
			for (String message : messages) {
				if (message.startsWith("uid=")) {
					this.uid = message.split("=")[1];
				}

				messageHandler.handleMessage(message, uid, this);

			}
			inputSoFar = new byte[0];
		}
	}

	public void onWriteableEvent() {
		connection.onWriteableEvent();
	}

	public byte[] readBytes() throws IOException {
		return connection.readBytes();
	}
	
	public byte[] peekBytes() throws IOException {
		return connection.peekBytes();
	}

	public void setAttachment(String attachment) {
		connection.setAttachment(attachment);
	}

	public void setReadableEventInterceptor(Connection interceptor) {
		connection.setReadableEventInterceptor(interceptor);
	}

	public void setSSLChannel(SSLChannel sslChannel) {
		connection.setSSLChannel(sslChannel);
	}

	public void setSelfClosing(boolean selfClosing) {
		connection.setSelfClosing(selfClosing);
	}

	public void setWriteRegister(WriteRegister dispatcher) {
		connection.setWriteRegister(dispatcher);
	}

	public void write(String data) throws IOException {
		connection.write(data);
	}

	public void write(ByteBuffer buffer) throws IOException {
		connection.write(buffer);
	}

	protected void readAsMuchAsPossible() throws IOException {
		int bytesRead = 0;

		while ((bytesRead = getChannel().read(readBuffer)) > 0) {
			readBuffer.rewind();
			byte[] bytes = new byte[bytesRead];
			readBuffer.get(bytes, 0, bytesRead);
			inputSoFar = ArrayUtils.concat(inputSoFar, bytes);
			readBuffer.rewind();
		}

		if (bytesRead < 0) {
			close();
		}
	}

	List<String> parseMessages(byte[] rawMessage) {
		List<String> messages = new ArrayList<String>();
		int start = -1;
		int end = -1;

		for (int i = 0; i < rawMessage.length; i++) {
			if (rawMessage[i] == START_BYTE) {
				start = i + 1;
			} else if (rawMessage[i] == END_BYTE) {
				end = i;
			}

			if (end > -1 && start > -1) {
				messages.add(new String(rawMessage, start, end - start));
				start = -1;
				end = -1;
			}
		}

		return messages;
	}
}
