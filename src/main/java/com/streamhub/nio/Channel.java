package com.streamhub.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

abstract class Channel implements ReadWriteSelectorHandler {
	protected final SelectorThread st;
	protected final SocketChannel sc;
	protected final ChannelListener listener;
	
	public Channel(
			SelectorThread st, 
			SocketChannel sc,
			ChannelListener listener) {
		this.st = st;
		this.sc = sc;
		this.listener = listener;
	}

	public abstract int read(ByteBuffer bb) throws IOException; 
	public abstract int write(ByteBuffer bb) throws IOException;

	public abstract void registerForRead() throws IOException;
	public abstract void unregisterForRead() throws IOException;
	
	public abstract void registerForWrite() throws IOException;
	public abstract void unregisterForWrite() throws IOException;

	public abstract void close() throws IOException;
	
	
	public SocketChannel getSocketChannel() {
		return sc;
	}
}
