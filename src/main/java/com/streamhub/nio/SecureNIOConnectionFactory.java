package com.streamhub.nio;

import java.nio.channels.SocketChannel;

import com.streamhub.Connection;

class SecureNIOConnectionFactory implements ConnectionFactory  {
	public Connection createConnection(SocketChannel channel) {
		return new SecureNIOConnection(channel);
	}
}
