package com.streamhub.nio;

interface ReadWriteSelectorHandler extends SelectorHandler {
	public void handleRead();
	public void handleWrite();
}