package com.streamhub.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import com.streamhub.Connection;
import com.streamhub.WriteRegister;
import com.streamhub.handler.Handler;
import com.streamhub.request.HttpRequest;
import com.streamhub.request.Request;
import com.streamhub.util.ArrayUtils;
import com.streamhub.util.SocketUtils;

class NIOConnection implements Connection {
	private static final String CRLFx2 = "\r\n\r\n";
	private static final String DIRECT_SEP = "@@";
	
	protected ByteBuffer readBuffer = ByteBuffer.allocateDirect(1024);
	protected boolean selfClosing = false;
	protected byte[] readBytes = new byte[0];
	protected byte[] inputSoFar = new byte[0];
	private final SocketChannel channel;
	private final List<ByteBuffer> writeBuffers = new ArrayList<ByteBuffer>();
	private String attachment;
	private ByteBuffer currentBuffer;
	private IOException exception;
	private WriteRegister writeRegister;
	private Request request;
	private Connection interceptor;
	private boolean writeInterestSet = false;

	public NIOConnection(Channel channel) {
		this.channel = (SocketChannel) channel;
	}

	public SocketChannel getChannel() {
		return channel;
	}
	
	public byte[] readBytes() throws IOException {
		return readBytes;
	}

	protected byte[] readAsMuchAsPossible() throws IOException {
		int bytesRead = 0;
		byte[] input = new byte[0];
		
		while ((bytesRead = channel.read(readBuffer)) > 0) {
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
	
	public byte[] peekBytes() throws IOException {
		if (readBytes.length < 2) {
			return new byte[0];
		}
		byte[] peek = new byte[2];
		System.arraycopy(readBytes, 0, peek, 0, 2);
		return peek;
	}

	public synchronized void write(String data) throws IOException {
		throwAnyExceptions();
		
		if (data != null) {
			ByteBuffer buffer = ByteBuffer.wrap(data.getBytes());
			writeAll(buffer);
		}
	}

	public synchronized void write(ByteBuffer buffer) throws IOException {
		throwAnyExceptions();
		writeAll(buffer);
	}

	public Request getRequest() throws IOException {
		if (request == null) {
			request = HttpRequest.createFrom(this);
		}
		return request;
	}

	public void close() {
		if (! selfClosing) {
			closeChannel();
		}
	}

	public void setReadBufferForTesting(ByteBuffer readBuffer) {
		this.readBuffer = readBuffer;
	}

	public String getAttachment() {
		return attachment;
	}

	public void setAttachment(String attachment) {
		this.attachment = attachment;
	}

	public SSLChannel getSSLChannel() {
		throw new UnsupportedOperationException("Not implemented for NIOConnection");
	}

	public void setSSLChannel(SSLChannel sslChannel) {
		throw new UnsupportedOperationException("Not implemented for NIOConnection");
	}

	public boolean isSecure() {
		return false;
	}

	public void onReadableEvent(Handler handler) {
		if (interceptor != null) {
			interceptor.onReadableEvent(handler);
		} else {
			try {
				inputSoFar = ArrayUtils.concat(inputSoFar, this.readAsMuchAsPossible());
			} catch (IOException e) {
				this.close();
			}
	
			if (isEndOfStream(inputSoFar)) {
				readBytes = new byte[inputSoFar.length];
				System.arraycopy(inputSoFar, 0, readBytes, 0, inputSoFar.length);
				inputSoFar = new byte[0];
				try {
					handler.handle(this);
				} catch (Exception e) {
				}
			}
		}
	}
	
	public void setWriteRegister(WriteRegister writeRegister) {
		this.writeRegister = writeRegister;
	}

	public void onWriteableEvent() {
		if (exception == null) {
			do {
				if (currentBuffer == null || !currentBuffer.hasRemaining()) {
					synchronized (writeBuffers) {
						if (writeBuffers.size() == 0) {
							deregisterWriteInterest();
							return;
						}
						currentBuffer = writeBuffers.remove(0);
					}
				}

				int bytesWritten = 1;

				while (currentBuffer.hasRemaining() && bytesWritten > 0) {
					try {
						bytesWritten = channel.write(currentBuffer);
					} catch (IOException e) {
						exception = e;
						return;
					}
				}
			} while (!currentBuffer.hasRemaining());
		}
		
		synchronized (writeBuffers) {
			if (!currentBuffer.hasRemaining() && writeBuffers.size() == 0) {
				deregisterWriteInterest();
			}
		}
	}
	
	public boolean isSelfClosing() {
		return selfClosing;
	}

	public void setSelfClosing(boolean selfClosing) {
		this.selfClosing = selfClosing;
	}
	
	public void setReadableEventInterceptor(Connection interceptor) {
		this.interceptor = interceptor;
	}
	
	protected boolean isEndOfStream(byte[] bytes) {
		if (bytes.length < 4) {
			return false;
		}
		
		byte[] lastFourBytes =  new byte[4];
		System.arraycopy(bytes, bytes.length-4, lastFourBytes, 0, 4);
		String firstTwo = new String(bytes, 0, 2);
		String lastFour = new String(lastFourBytes);
		
		if (CRLFx2.equals(lastFour)) {
			return true;
		} else if (firstTwo.startsWith(DIRECT_SEP)) {
			return lastFour.endsWith(DIRECT_SEP);
		} else {
			String string = new String(bytes);
			if (string.contains("WebSocket") && string.contains(CRLFx2)) {
				return true;
			}
		}

		return false;
	}
	
	protected void closeChannel() {
		SocketUtils.closeQuietly(channel);
	}

	private void deregisterWriteInterest() {
		writeRegister.deregisterForWrite(this);
		writeInterestSet = false;
		if (selfClosing) {
			closeChannel();
		}
	}

	private void writeAll(ByteBuffer buffer) throws IOException {
		synchronized (writeBuffers) {
			writeBuffers.add(buffer);
		}
		writeAsMuchAsPossible();
		if (buffer.hasRemaining() && !writeInterestSet) {
			setWriteInterest();
		}
	}
	
	private void setWriteInterest() {
		writeRegister.registerForWrite(this);
		writeInterestSet = true;
	}

	private void throwAnyExceptions() throws IOException {
		if (exception != null) {
			throw exception;
		}
	}

	private void writeAsMuchAsPossible() throws IOException {
		onWriteableEvent();
	}
}
