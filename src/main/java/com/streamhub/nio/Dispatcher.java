package com.streamhub.nio;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.streamhub.Connection;
import com.streamhub.WriteRegister;
import com.streamhub.handler.Handler;
import com.streamhub.util.SocketUtils;

class Dispatcher implements Runnable, WriteRegister {
	private static final Logger log = Logger.getLogger(Dispatcher.class);
	public static int dispatcherCount = 0;
	private Object guard = new Object();
	private Selector selector;
	private boolean isRunning;
	private final Handler handler;

	public Dispatcher(Handler handler) {
		this.handler = handler;

		try {
			selector = Selector.open();
		} catch (IOException e) {
			log.error("Error opening Selector", e);
		}
	}

	void register(Connection con) throws IOException {
		// retrieve the guard lock and wake up the dispatcher thread
		// to register the connection's channel
		synchronized (guard) {
			selector.wakeup();
			SocketChannel channel = con.getChannel();
			con.setWriteRegister(this);
			channel.configureBlocking(false);
			channel.register(selector, SelectionKey.OP_READ, con);
		}

		// notify the application EventHandler about the new connection
	}

	public void run() {
		Thread.currentThread().setName("Dispatcher-" + dispatcherCount++);
		isRunning = true;

		while (isRunning) {
			synchronized (guard) {
				// suspend the dispatcher thead if guard is locked
			}

			try {
				selector.select();
			} catch (Throwable e) {
				log.error("Error waiting for events", e);
			}

			if (!isRunning || selector == null) {
				return;
			}

			Iterator<SelectionKey> it = selector.selectedKeys().iterator();
			while (it.hasNext()) {
				SelectionKey key = it.next();
				it.remove();

				if (key.isValid()) {
					Connection con = (Connection) key.attachment();

					if (key.isReadable()) {
						con.onReadableEvent(handler);
					} else if (key.isWritable()) {
						con.onWriteableEvent();
					}
				}
			}
		}
	}

	public void stop() {
		isRunning = false;
		SocketUtils.closeQuietly(selector);
	}

	public void deregisterForWrite(Connection connection) {
		synchronized (guard) {
			selector.wakeup();
			SocketChannel channel = connection.getChannel();
			try {
				channel.register(selector, SelectionKey.OP_READ, connection);
			} catch (Exception e) {
				log.debug("Could not deregister for writes", e);
			}
		}
	}

	public void registerForWrite(Connection connection) {
		synchronized (guard) {
			selector.wakeup();
			SocketChannel channel = connection.getChannel();
			try {
				channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, connection);
			} catch (ClosedChannelException e) {
				log.debug("Could not register for writes", e);
			}
		}
	}
}
