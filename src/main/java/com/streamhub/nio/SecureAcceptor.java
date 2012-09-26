package com.streamhub.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import com.streamhub.Connection;
import com.streamhub.handler.Handler;

class SecureAcceptor extends Acceptor {

	private final SSLContext context;
	private SelectorThread selectorThread = new SelectorThread();
	private final ExecutorService dispatchPool = Executors.newFixedThreadPool(5);

	public SecureAcceptor(InetAddress inetAddress, int serverPort, Handler handler,
			ConnectionFactory connectionFactory, SSLContext context) {
		super(inetAddress, serverPort, handler, connectionFactory);
		this.context = context;
	}

	@Override
	void bind() {
		super.bind();
	}

	@Override
	Connection createConnection(SocketChannel channel) {
		return super.createConnection(channel);
	}

	@Override
	public void run() {
		Thread.currentThread().setName("SecureAcceptor");
		bind();

		isRunning = true;

		while (isRunning) {
			try {
				final SocketChannel channel = serverChannel.accept();
				dispatchPool.execute(new Runnable() {
					public void run() {
						final Connection con = createConnection(channel);
						final ChannelEventHandler channelEventHandler = new ChannelEventHandler(con);

						selectorThread.invokeLater(new Runnable() {

							public void run() {
								SSLChannel sslChannel;
								try {
									SSLEngine engine = context.createSSLEngine();
									engine.setUseClientMode(false);
									sslChannel = new SSLChannel(selectorThread, channel, channelEventHandler, engine);
									con.setSSLChannel(sslChannel);
									sslChannel.registerForRead();
									//sslChannel.registerForWrite();
								} catch (Exception e) {
									log.error("Error starting SSLEngine", e);
								}
							}
						});
					}
				});
			} catch (Exception e) {
				if (isRunning) {
					log.error("Error accepting connections", e);
				}
			}
		}
	}

	@Override
	public void stop() throws IOException {
		selectorThread.requestClose();
		dispatchPool.shutdownNow();
		super.stop();
	}

	private class ChannelEventHandler implements ChannelListener {
		private final Connection connection;
		private boolean hasRead = false;
		
		public ChannelEventHandler(Connection connection) {
			this.connection = connection;
		}

		public void handleRead() {
			if (!hasRead) {
				hasRead = true;
				connection.onReadableEvent(handler);
			}
		}

		public void handleWrite() {
		}
	}

}
