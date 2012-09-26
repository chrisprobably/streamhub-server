package com.streamhub;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import com.streamhub.handler.Handler;
import com.streamhub.nio.SSLChannel;
import com.streamhub.request.Request;

public interface Connection {
	SocketChannel getChannel();
	byte[] readBytes() throws IOException;
	byte[] peekBytes() throws IOException;
	void write(String data) throws IOException;
	void write(ByteBuffer buffer) throws IOException;
	Request getRequest() throws IOException;
	void close();
	void setAttachment(String attachment);
	String getAttachment();
	void setSSLChannel(SSLChannel sslChannel);
	void setSelfClosing(boolean selfClosing);
	boolean isSelfClosing();
	SSLChannel getSSLChannel();
	boolean isSecure();
	void onReadableEvent(Handler handler);
	void onWriteableEvent();
	void setWriteRegister(WriteRegister dispatcher);
	void setReadableEventInterceptor(Connection interceptor);
}