package com.streamhub.nio;

import java.nio.channels.SocketChannel;

import com.streamhub.Connection;

interface ConnectionFactory {
	public Connection createConnection(SocketChannel channel);
}
