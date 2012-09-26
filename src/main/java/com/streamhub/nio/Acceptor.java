package com.streamhub.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

import com.streamhub.Connection;
import com.streamhub.UnrecoverableStartupException;
import com.streamhub.handler.Handler;
import com.streamhub.util.SocketUtils;

class Acceptor implements Runnable {
	protected static final Logger log = Logger.getLogger(Acceptor.class);
	protected final Handler handler;
	protected boolean isRunning;
	protected DispatcherPool dispatcherPool;
	protected ServerSocketChannel serverChannel;
	private final ConnectionFactory connectionFactory;
	private final int serverPort;
	private final InetAddress inetAddress;

	public Acceptor(int serverPort, Handler handler, ConnectionFactory connectionFactory) {
		this(null, serverPort, handler, connectionFactory);
	}

	public Acceptor(InetAddress inetAddress, int serverPort, Handler handler, ConnectionFactory connectionFactory) {
		this.inetAddress = inetAddress;
		this.serverPort = serverPort;
		this.handler = handler;
		this.connectionFactory = connectionFactory;
	}

	public void run() {
		Thread.currentThread().setName("Acceptor");
		this.dispatcherPool = new DispatcherPool(handler, 10);
		bind();

		isRunning = true;
		
		while (isRunning) {
			try {
				SocketChannel channel = serverChannel.accept();
				Connection con = createConnection(channel);
				dispatcherPool.nextDispatcher().register(con);
			} catch (Exception e) {
				if (isRunning) {
					log.error("Error accepting connections", e);
					
					if (e instanceof ClosedChannelException) {
						throw new UnrecoverableStartupException("Closed channel - cannot continue", e);
					}
				}
			}
		}
	}
	
	public void stop() throws IOException {
		isRunning = false;
		SocketUtils.closeQuietly(serverChannel);
		if (dispatcherPool != null) {
			dispatcherPool.stop();
		}
	}

	void bind() {
		try {
			serverChannel = ServerSocketChannel.open();
			serverChannel.configureBlocking(true);
			if (inetAddress != null) {
				serverChannel.socket().bind(new InetSocketAddress(inetAddress, serverPort));
			} else {
				serverChannel.socket().bind(new InetSocketAddress(serverPort));
			}
		} catch (Throwable t) {
			log.error("Error binding socket", t);
		}
	}

	Connection createConnection(SocketChannel channel) {
		return connectionFactory.createConnection(channel);
	}
}
