package com.streamhub.tools.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.streamhub.UnrecoverableStartupException;
import com.streamhub.util.SocketUtils;

public class Proxy {
	private static final Logger log = Logger.getLogger(Proxy.class);	
	private final List<Socket> sockets = new ArrayList<Socket>();
	private final ExecutorService threadPool = Executors.newCachedThreadPool();
	private ServerSocket serverSocket;
	private final int listenPort;
	private final URL target;
	private final String host;
	private final int port;
	private boolean isStopped;
	private boolean isBlankResponseMode;

	public Proxy(int listenPort, URL target) {
		this.listenPort = listenPort;
		this.target = target;
		this.host = "localhost";
		this.port = listenPort;
	}
	
	public void start() throws IOException {
		log.info("Starting proxy");
		
		try {
			isStopped = false;
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(listenPort));
        } catch (IOException e) {
            throw new UnrecoverableStartupException("Could not listen on port: " + listenPort, e);
        }
        
        listenForConnections();
	}

	public void startBlankResponses() {
		isBlankResponseMode = true;
	}
	
	public void stopBlankResponses() {
		isBlankResponseMode = false;
	}

	public void stop() {
		isStopped = true;
		SocketUtils.closeQuietly(serverSocket);
		synchronized(sockets) {
			for (Socket socket : sockets) {
				SocketUtils.closeQuietly(socket);
			}
		}
		log.info("Stopped proxy");
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public boolean isStopped() {
		return isStopped;
	}

	private void listenForConnections() {
		threadPool.execute(new Runnable() {
			public void run() {
		        while (!isStopped) {
		        	try {
						Socket socket = serverSocket.accept();
						Socket targetSocket = new Socket(target.getHost(), target.getPort());
						synchronized(sockets) {
							sockets.add(socket);
							sockets.add(targetSocket);
						}
						ProxyConnection proxyConnection = new ProxyConnection(socket, targetSocket, isBlankResponseMode);
						threadPool.execute(proxyConnection);
					} catch (IOException e) {
						log.error("Error in test code", e);
					}
		        }
		}});
	}
	
}
