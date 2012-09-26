package com.streamhub.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;

import com.streamhub.util.ArrayUtils;

class SecureNIOConnection extends NIOConnection {
	private SSLChannel sslChannel;

	public SecureNIOConnection(Channel channel) {
		super(channel);
	}

	public SSLChannel getSSLChannel() {
		return sslChannel;
	}

	public void setSSLChannel(SSLChannel sslChannel) {
		this.sslChannel = sslChannel;
	}

	@Override
	public void close() {
		closeChannel();
	}

	@Override
	public synchronized void write(String data) throws IOException {
		if (data != null) {
			ByteBuffer buffer = ByteBuffer.wrap(data.getBytes());
			while(buffer.hasRemaining()) {
				sslChannel.write(buffer);
			}
			if (selfClosing) {
				closeChannel();
			}
		}
	}
	
	@Override
	public synchronized void write(ByteBuffer buffer) throws IOException {
		while(buffer.hasRemaining()) {
			sslChannel.write(buffer);
		}
		if (selfClosing) {
			closeChannel();
		}
	}

	@Override
	public boolean isSecure() {
		return true;
	}
	
	protected byte[] readAsMuchAsPossible() throws IOException {
		int bytesRead = 0;
		byte[] input = new byte[0];
		
		while ((bytesRead = sslChannel.read(readBuffer)) > 0) {
			readBuffer.rewind();
			byte[] bytes = new byte[bytesRead];
			readBuffer.get(bytes, 0, bytesRead);
			input = ArrayUtils.concat(input, bytes);
			readBuffer.rewind();
		}

		if (bytesRead < 0) {
			close();
		}

		return input;
	}
	
	@Override
	protected void closeChannel() {
		try {
			sslChannel.close();
		} catch (IOException e) {
		}
	}
}
