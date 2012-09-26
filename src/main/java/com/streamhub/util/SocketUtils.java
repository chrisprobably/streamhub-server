package com.streamhub.util;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import com.streamhub.Connection;

public class SocketUtils {
	private static final String ANONYMOUS = "anonymous";

	public static void closeQuietly(Socket socket) {
		try {
			if (socket != null) {
				socket.getInputStream().close();
				socket.getOutputStream().close();
				socket.close();
			}
		} catch (IOException e) {
		}
	}

	public static void closeQuietly(ServerSocket serverSocket) {
		try {
			if (serverSocket != null) {
				serverSocket.close();
			}
		} catch (IOException e) {
		}
	}


	public static void closeQuietly(SocketChannel channel) {
		try {
			if (channel != null) {
				channel.close();
			}
		} catch (Exception e) {
		}
	}

	public static void closeQuietly(ServerSocketChannel channel) {
		try {
			if (channel != null) {
				channel.close();
			}
		} catch (Exception e) {
		}
	}

	public static void closeQuietly(Selector selector) {
		if (selector != null) {
			try {
				selector.close();
			} catch (Throwable t) {
			}
		}
	}

	public static String toString(Socket socket) {
		try {
			return socket.getRemoteSocketAddress().toString();
		} catch (Throwable t) {}
		
		return ANONYMOUS;
	}
	
	public static String toString(Connection connection) {
		if (connection == null || connection.getChannel() == null || connection.getChannel().socket() == null) {
			return ANONYMOUS;
		}
		
		return toString(connection.getChannel().socket());
	}
}
