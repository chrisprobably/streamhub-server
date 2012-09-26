package com.streamhub.tools.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.streamhub.util.StreamUtils;

public class ProxyConnection implements Runnable {
	private static final String BLANK_RESPONSE = "HTTP/1.1 200 OK\r\nServer: BlankResponse\r\nCache-Control: no-cache\r\nPragma: no-cache\r\nExpires: Thu, 1 Jan 1970 00:00:00 GMT\r\nContent-Type: text/html\r\nConnection: close\r\n\r\n";
	private static final Logger log = Logger.getLogger(ProxyConnection.class);
	private final ExecutorService threadPool = Executors.newFixedThreadPool(2);
	private final Socket socket;
	private final Socket targetSocket;
	private final boolean isBlankResponseMode;

	public ProxyConnection(Socket socket, Socket targetSocket, boolean isBlankResponseMode) {
		this.socket = socket;
		this.targetSocket = targetSocket;
		this.isBlankResponseMode = isBlankResponseMode;
	}

	public void run() {
		try {
			startProxying();
		} catch (IOException e) {
			log.error("Error during proxying",e);
		}
	}

	private void startProxying() throws IOException, UnknownHostException {
		InputStream clientIn = socket.getInputStream();
		OutputStream clientOut = socket.getOutputStream();
		
		if (isBlankResponseMode) {
			returnBlankResponse(clientIn, clientOut);
		} else {
			proxyData(clientIn, clientOut);
		}
	}

	private void proxyData(InputStream clientIn, OutputStream clientOut) throws UnknownHostException, IOException {
		InputStream serverIn = targetSocket.getInputStream();
		OutputStream serverOut = targetSocket.getOutputStream();

		threadPool.execute(new InputToOutputStream(clientIn, serverOut, "clientIn->serverOut"));
		threadPool.execute(new InputToOutputStream(serverIn, clientOut, "serverIn->clientOut"));
	}

	private void returnBlankResponse(InputStream clientIn, OutputStream clientOut) throws IOException {
		StreamUtils.toString(clientIn);
		StreamUtils.write(clientOut, BLANK_RESPONSE);
		socket.close();
	}

	private static class InputToOutputStream implements Runnable {
		private final InputStream input;
		private final OutputStream output;
		private final String description;

		public InputToOutputStream(InputStream input, OutputStream output, String description) {
			this.input = input;
			this.output = output;
			this.description = description;
		}

		public void run() {
			int in;
			StringBuilder written = new StringBuilder();
			try {
				while ((in = input.read()) != -1) {
					written.append((char) in);
					output.write(in);
					output.flush();
				}
			} catch (IOException e) {
				log.error(description + ": Exception [" + e.getMessage() + "]");
			} finally {
				StreamUtils.closeQuietly(output);
				StreamUtils.closeQuietly(input);
			}
		}
	}

}
