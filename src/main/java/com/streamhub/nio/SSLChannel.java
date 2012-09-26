package com.streamhub.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;

import org.apache.log4j.Logger;

import com.streamhub.util.SocketUtils;

public final class SSLChannel extends Channel {
	private final static Logger log = Logger.getLogger(SSLChannel.class);
	private final SSLSession session;
	private final SSLEngine engine;
	private final ByteBuffer peerAppData;
	private final ByteBuffer peerNetData;
	private final ByteBuffer netData;
	private boolean appReadInterestSet = false;
	private boolean appWriteInterestSet = false;
	private boolean channelReadInterestSet = false;
	private boolean channelWriteInterestSet = false;
	private boolean initialHandshake = false;
	private SSLEngineResult.HandshakeStatus hsStatus;
	private ByteBuffer dummy;
	private boolean shutdown = false;
	private boolean closed = false;
	private SSLEngineResult.Status status = null;
	private IOException asynchException = null;
	private boolean shouldHandleReadAfterHandShake;

	public SSLChannel(SelectorThread st, SocketChannel sc, ChannelListener listener, SSLEngine engine) throws Exception {
		super(st, sc, listener);

		this.engine = engine;

		session = engine.getSession();
		peerNetData = ByteBuffer.allocate(session.getPacketBufferSize());
		peerAppData = ByteBuffer.allocate(session.getApplicationBufferSize());
		netData = ByteBuffer.allocate(session.getPacketBufferSize());
		peerAppData.position(peerAppData.limit());
		netData.position(netData.limit());
		st.registerChannelNow(sc, 0, this);
		log.debug("Starting SSL handshake");
		engine.beginHandshake();
		hsStatus = engine.getHandshakeStatus();
		initialHandshake = true;
		dummy = ByteBuffer.allocate(0);
		doHandshake();
	}

	private void checkChannelStillValid() throws IOException {
		if (closed) {
			throw new ClosedChannelException();
		}
		if (asynchException != null) {
			IOException ioe = new IOException("Asynchronous failure: " + asynchException.getMessage());
			ioe.initCause(asynchException);
		}
	}

	public int read(ByteBuffer dst) throws IOException {
		checkChannelStillValid();
		if (initialHandshake) {
			return 0;
		}

		if (engine.isInboundDone()) {
			return -1;
		}

		if (!peerAppData.hasRemaining()) {
			int appBytesProduced = readAndUnwrap();
			if (appBytesProduced == -1 || appBytesProduced == 0) {
				return appBytesProduced;
			}
		}

		int limit = Math.min(peerAppData.remaining(), dst.remaining());

		for (int i = 0; i < limit; i++) {
			dst.put(peerAppData.get());
		}
		return limit;
	}

	private int readAndUnwrap() throws IOException {
		int bytesRead = sc.read(peerNetData);
		log.debug("Read " + bytesRead + " bytes");
		if (bytesRead == -1) {
			try {
				engine.closeInbound();
			} catch (SSLException e) {
				log.warn("Error closing inbound - continuing", e);
			}
			if (peerNetData.position() == 0 || status == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
				return -1;
			}
		}

		peerAppData.clear();
		peerNetData.flip();
		SSLEngineResult res;
		do {
			res = engine.unwrap(peerNetData, peerAppData);
			log.debug("Unwrapping:\n" + res);
		} while (res.getStatus() == SSLEngineResult.Status.OK
				&& res.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP && res.bytesProduced() == 0);
		if (res.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED) {
			finishInitialHandshake();
		}

		if (peerAppData.position() == 0 && res.getStatus() == SSLEngineResult.Status.OK && peerNetData.hasRemaining()) {
			res = engine.unwrap(peerNetData, peerAppData);
			log.debug("Unwrapping1:\n" + res);
			if (res.bytesConsumed() > 0) {
				shouldHandleReadAfterHandShake = true;
			}
		}

		status = res.getStatus();
		hsStatus = res.getHandshakeStatus();
		if (status == SSLEngineResult.Status.CLOSED) {
			log.debug("Connection is being closed by peer");
			shutdown = true;
			doShutdown();
			return -1;
		}

		peerNetData.compact();
		peerAppData.flip();

		if (hsStatus == SSLEngineResult.HandshakeStatus.NEED_TASK
				|| hsStatus == SSLEngineResult.HandshakeStatus.NEED_WRAP
				|| hsStatus == SSLEngineResult.HandshakeStatus.FINISHED) {
			doHandshake();
		}

		return peerAppData.remaining();
	}

	public int write(ByteBuffer src) throws IOException {
		checkChannelStillValid();
		if (initialHandshake) {
			log.warn("Writing not possible during handshake");
			return 0;
		}
		log.debug("Trying to write");

		if (netData.hasRemaining()) {
			return 0;
		}

		netData.clear();
		SSLEngineResult res = engine.wrap(src, netData);
		log.debug("Wrapping1:\n" + res);
		netData.flip();
		flushData();

		return res.bytesConsumed();
	}

	public void registerForRead() throws IOException {
		checkChannelStillValid();
		if (!appReadInterestSet) {
			appReadInterestSet = true;
			if (initialHandshake) {
				return;
			} else {
				if (peerAppData.hasRemaining()) {
					st.getSscManager().registerForRead(this);
				} else {
					if (peerNetData.position() == 0 || status == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
						selectorRegisterForRead();
					} else {
						if (readAndUnwrap() == 0) {
							selectorRegisterForRead();
						} else {
							st.getSscManager().registerForRead(this);
						}
					}
				}
			}
		}
	}

	public void unregisterForRead() throws IOException {
		checkChannelStillValid();
		appReadInterestSet = false;
		st.getSscManager().unregisterForRead(this);
	}

	public void registerForWrite() throws IOException {
		checkChannelStillValid();
		if (!appWriteInterestSet) {
			appWriteInterestSet = true;
			if (initialHandshake) {
				return;
			} else {
				if (netData.hasRemaining()) {
				} else {
					st.getSscManager().registerForWrite(this);
				}
			}
		}
	}

	public void unregisterForWrite() throws IOException {
		checkChannelStillValid();
		appWriteInterestSet = false;
		st.getSscManager().unregisterForWrite(this);
	}

	void fireReadEvent() {
		appReadInterestSet = false;
		listener.handleRead();
	}

	void fireWriteEvent() {
		appWriteInterestSet = false;
		listener.handleWrite();
	}

	private void doShutdown() throws IOException {
		if (asynchException != null || engine.isOutboundDone()) {
			log.debug("Outbound is finished - closing socket");
			try {
				sc.close();
			} catch (IOException e) { 
			}
			return;
		}
		netData.clear();
		try {
			SSLEngineResult res = engine.wrap(dummy, netData);
			log.debug("Wrapping2:\n" + res);
		} catch (SSLException e1) {
			log.error("Error during shutdown", e1);
			try {
				sc.close();
			} catch (IOException e) { /* Ignore. */
			}
			return;
		}
		netData.flip();
		flushData();
	}

	public void close() throws IOException {
		if (shutdown) {
			log.debug("Shutdown already in progress");
			return;
		}
		shutdown = true;
		closed = true;
		asynchException = null;
		engine.closeOutbound();
		if (netData.hasRemaining()) {
			return;
		} else {
			doShutdown();
		}
		SocketUtils.closeQuietly(sc);
	}

	private void finishInitialHandshake() throws IOException {
		initialHandshake = false;
		if (appReadInterestSet) {
			selectorRegisterForRead();
		}
		if (appWriteInterestSet) {
			st.getSscManager().registerForWrite(this);
		}
	}

	private void doHandshake() throws IOException {
		while (true) {
			SSLEngineResult res;
			switch (hsStatus) {
			case FINISHED:
				if (initialHandshake) {
					finishInitialHandshake();
				}
				return;

			case NEED_TASK:
				doTasks();
				break;

			case NEED_UNWRAP:
				readAndUnwrap();
				if (initialHandshake && status == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
					selectorRegisterForRead();
				}
				return;

			case NEED_WRAP:
				if (netData.hasRemaining()) {
					return;
				}
				netData.clear();
				res = engine.wrap(dummy, netData);
				log.debug("Wrapping3:\n" + res);
				hsStatus = res.getHandshakeStatus();
				netData.flip();

				if (!flushData()) {
					return;
				}
				break;

			case NOT_HANDSHAKING:
				return;
			}
		}
	}

	public void handleRead() {
		channelReadInterestSet = false;
		try {
			if (initialHandshake) {
				doHandshake();
				HandshakeStatus handshakeStatus = engine.getHandshakeStatus();
				if (handshakeStatus == HandshakeStatus.NOT_HANDSHAKING) {
					if (shouldHandleReadAfterHandShake) {
						shouldHandleReadAfterHandShake = false;
						listener.handleRead();
					}
				}
			} else if (shutdown) {
				doShutdown();

			} else {
				int bytesUnwrapped = readAndUnwrap();
				if (bytesUnwrapped == -1) {
					st.getSscManager().registerForRead(this);
				} else if (bytesUnwrapped == 0) {
					selectorRegisterForRead();
				} else {
					st.getSscManager().registerForRead(this);
				}
			}
		} catch (IOException e) {
			log.warn("Exception during secure conversation", e);
		}
	}

	private boolean flushData() throws IOException {
		if (!netData.hasRemaining()) {
			return true;
		}

		int written;
		try {
			written = sc.write(netData);
		} catch (IOException ioe) {
			netData.position(netData.limit());
			throw ioe;
		}
		log.debug("Written to socket: " + written);
		if (netData.hasRemaining()) {
			selectorRegisterForWrite();
			return false;
		} else {
			return true;
		}
	}

	public void handleWrite() {
		channelWriteInterestSet = false;
		try {
			if (flushData()) {
				if (initialHandshake) {
					doHandshake();
				} else if (shutdown) {
					doShutdown();
				} else {
					if (appWriteInterestSet) {
						st.getSscManager().registerForWrite(this);
					}
				}
			} else {
			}
		} catch (IOException e) {
			handleAsynchException(e);
		}
	}

	private void handleAsynchException(IOException e) {
		asynchException = e;
		if (appWriteInterestSet) {
			st.getSscManager().registerForWrite(this);
		}
		if (appReadInterestSet) {
			st.getSscManager().registerForRead(this);
		}
	}

	private void selectorRegisterForRead() throws IOException {
		if (channelReadInterestSet) {
			return;
		}
		channelReadInterestSet = true;
		st.addChannelInterestNow(sc, SelectionKey.OP_READ);
	}

	private void selectorRegisterForWrite() throws IOException {
		if (channelWriteInterestSet) {
			return;
		}
		channelWriteInterestSet = true;
		st.addChannelInterestNow(sc, SelectionKey.OP_WRITE);
	}

	private void doTasks() {
		Runnable task;
		while ((task = engine.getDelegatedTask()) != null) {
			task.run();
		}
		hsStatus = engine.getHandshakeStatus();
	}
}