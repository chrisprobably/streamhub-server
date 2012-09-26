package com.streamhub.nio;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

final class SelectorThread implements Runnable {
	private static final Logger log = Logger.getLogger(SelectorThread.class);
	private Selector selector;

	private final Thread selectorThread;

	private boolean closeRequested = false;

	private final List<Runnable> pendingInvocations = new ArrayList<Runnable>(32);

	private final SSLChannelManager sscManager = new SSLChannelManager();

	public SelectorThread() {
		try {
			selector = Selector.open();
		} catch (IOException e) {
			log.error("Error opening selector", e);
		}
		selectorThread = new Thread(this);
		selectorThread.start();
	}

	public void requestClose() {
		closeRequested = true;
		selector.wakeup();
	}

	public void addChannelInterestNow(SelectableChannel channel, int interest) throws IOException {
		if (Thread.currentThread() != selectorThread) {
			throw new IOException("Method can only be called from selector thread");
		}
		SelectionKey sk = channel.keyFor(selector);
		changeKeyInterest(sk, sk.interestOps() | interest);
	}

	public void addChannelInterestLater(final SelectableChannel channel, final int interest,
			final CallbackErrorHandler errorHandler) {
		invokeLater(new Runnable() {
			public void run() {
				try {
					addChannelInterestNow(channel, interest);
				} catch (IOException e) {
					errorHandler.handleError(e);
				}
			}
		});
	}

	public void removeChannelInterestNow(SelectableChannel channel, int interest) throws IOException {
		if (Thread.currentThread() != selectorThread) {
			throw new IOException("Method can only be called from selector thread");
		}
		SelectionKey sk = channel.keyFor(selector);
		changeKeyInterest(sk, sk.interestOps() & ~interest);
	}

	public void removeChannelInterestLater(final SelectableChannel channel, final int interest,
			final CallbackErrorHandler errorHandler) {
		invokeLater(new Runnable() {
			public void run() {
				try {
					removeChannelInterestNow(channel, interest);
				} catch (IOException e) {
					errorHandler.handleError(e);
				}
			}
		});
	}

	private void changeKeyInterest(SelectionKey sk, int newInterest) throws IOException {
		try {
			sk.interestOps(newInterest);
		} catch (CancelledKeyException cke) {
			IOException ioe = new IOException("Failed to change channel interest.");
			ioe.initCause(cke);
			throw ioe;
		}
	}

	public void registerChannelLater(final SelectableChannel channel, final int selectionKeys,
			final SelectorHandler handlerInfo, final CallbackErrorHandler errorHandler) {
		invokeLater(new Runnable() {
			public void run() {
				try {
					registerChannelNow(channel, selectionKeys, handlerInfo);
				} catch (IOException e) {
					errorHandler.handleError(e);
				}
			}
		});
	}

	public void registerChannelNow(SelectableChannel channel, int selectionKeys, SelectorHandler handlerInfo)
			throws IOException {
		if (Thread.currentThread() != selectorThread) {
			throw new IOException("Method can only be called from selector thread");
		}

		if (!channel.isOpen()) {
			throw new IOException("Channel is not open.");
		}

		try {
			if (channel.isRegistered()) {
				SelectionKey sk = channel.keyFor(selector);
				sk.interestOps(selectionKeys);
				sk.attach(handlerInfo);
			} else {
				channel.configureBlocking(false);
				channel.register(selector, selectionKeys, handlerInfo);
			}
		} catch (Exception e) {
			IOException ioe = new IOException("Error registering channel.");
			ioe.initCause(e);
			throw ioe;
		}
	}

	public void invokeLater(Runnable run) {
		synchronized (pendingInvocations) {
			pendingInvocations.add(run);
		}
		selector.wakeup();
	}

	public void invokeAndWait(final Runnable task) throws InterruptedException {
		if (Thread.currentThread() == selectorThread) {
			task.run();
		} else {
			final Object latch = new Object();
			synchronized (latch) {
				this.invokeLater(new Runnable() {
					public void run() {
						task.run();
						latch.notify();
					}
				});
				latch.wait();
			}
		}
	}

	private void doInvocations() {
		synchronized (pendingInvocations) {
			for (int i = 0; i < pendingInvocations.size(); i++) {
				Runnable task = (Runnable) pendingInvocations.get(i);
				task.run();
			}
			pendingInvocations.clear();
		}
	}

	public void run() {
		while (true) {
			doInvocations();

			if (closeRequested) {
				return;
			}

			sscManager.fireEvents();

			int selectedKeys = 0;
			try {
				selectedKeys = selector.select();
			} catch (IOException ioe) {
				log.warn("Error selecting - continuing", ioe);
				continue;
			}

			if (selectedKeys == 0) {
				continue;
			}

			Iterator<SelectionKey> it = selector.selectedKeys().iterator();
			while (it.hasNext()) {
				SelectionKey sk = (SelectionKey) it.next();
				it.remove();
				try {
					int readyOps = sk.readyOps();
					sk.interestOps(sk.interestOps() & ~readyOps);
					SelectorHandler handler = (SelectorHandler) sk.attachment();

					if (sk.isAcceptable()) {
						((AcceptSelectorHandler) handler).handleAccept();

					} else if (sk.isConnectable()) {
						((ConnectorSelectorHandler) handler).handleConnect();
					} else {
						ReadWriteSelectorHandler rwHandler = (ReadWriteSelectorHandler) handler;
						if (sk.isReadable()) {
							rwHandler.handleRead();
						}
						if (sk.isValid() && sk.isWritable()) {
							rwHandler.handleWrite();
						}
					}
				} catch (Throwable t) {
					closeSelectorAndChannels();
					log.error("Error during SelectorThread loop", t);
					return;
				}
			}
		}
	}

	private void closeSelectorAndChannels() {
		Set<SelectionKey> keys = selector.keys();
		for (Iterator<SelectionKey> iter = keys.iterator(); iter.hasNext();) {
			SelectionKey key = (SelectionKey) iter.next();
			try {
				key.channel().close();
			} catch (IOException e) {
			}
		}
		try {
			selector.close();
		} catch (IOException e) {
		}
	}

	public SSLChannelManager getSscManager() {
		return sscManager;
	}

	public void runTask(Runnable runnable) {
		runnable.run();
	}
}