package com.streamhub.nio;

interface ChannelListener {
	public void handleRead();
	public void handleWrite();
}
