package com.streamhub.nio;

import java.nio.channels.SocketChannel;

import com.streamhub.Connection;

class NIOConnectionFactory implements ConnectionFactory {

	public Connection createConnection(SocketChannel channel) {
		return new NIOConnection(channel);
	}

}
