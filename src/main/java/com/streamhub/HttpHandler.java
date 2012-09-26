package com.streamhub;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import com.streamhub.handler.Handler;
import com.streamhub.request.Request;
import com.streamhub.util.ArrayUtils;
import com.streamhub.util.StreamUtils;

public class HttpHandler implements Handler {
	public static final String _404_NOT_FOUND = "HTTP/1.1 404 Not Found\r\nServer: StreamHub\r\nCache-Control: private, max-age=0\r\nExpires: -1\r\n\r\n404 not found";
	private static final Logger log = Logger.getLogger(HttpHandler.class);
	private static final String CACHEABLE_HTTP_LEAD_LINE = "HTTP/1.1 200 OK\r\nServer: StreamHub\r\nConnection: close\r\n";
	private static File directory;
	private static String context;

	public void handle(Connection connection) throws IOException {
		Request request = connection.getRequest();
		handle(connection, request);
	}

	public void handle(Connection connection, Request request) throws IOException {
		if (directory == null || request == null) {
			connection.write(_404_NOT_FOUND);
		} else {
			connection.setSelfClosing(true);
			String filename = request.getUrl();
			
			if (request.getUrl().endsWith("/")) {
				filename += "index.html";
			}

			if (context != null && filename.startsWith(context)) {
				filename = filename.replaceFirst(context, "");
			}

			File requestFile = new File(directory, filename);
			if (!requestFile.exists() || filename.contains("..")) {
				connection.write(_404_NOT_FOUND);
			} else {
				String contentType = getContentType(requestFile);

				try {
					if (contentType.startsWith("Content-Type: image/")) {
						byte[] imageBytes = StreamUtils.toBytes(requestFile);
						byte[] headerBytes = new StringBuilder(CACHEABLE_HTTP_LEAD_LINE).append("Content-Length: " + imageBytes.length + "\r\n").append(contentType).toString().getBytes();
						byte[] allBytes = ArrayUtils.concat(headerBytes, imageBytes);
						connection.write(ByteBuffer.wrap(allBytes));
					} else {
						connection.write(new StringBuilder(CACHEABLE_HTTP_LEAD_LINE).append(contentType).append(StreamUtils.toString(new FileInputStream(requestFile))).toString());
					}
				} catch (IOException e) {
					log.error("Error serving page " + filename, e);
					connection.write(_404_NOT_FOUND);
				}
			}
		}
	}
	
	public static void addStaticDirectory(File directory) {
		HttpHandler.directory = directory;
	}

	public static void addStaticDirectory(File directory, String context) {
		if (context != null && (!context.startsWith("/") || context.endsWith("/"))) {
			throw new IllegalArgumentException("Contexts must start with a trailing slash but not end with one e.g. '/images'");
		}
		HttpHandler.directory = directory;
		HttpHandler.context = context;
	}

	public static void setDefaultPushHeader(String name, String value) {
		ResponseFactory.setDefaultPushHeader(name, value);
	}

	public static void setDefaultHeader(String name, String value) {
		ResponseFactory.setDefaultHeader(name, value);
	}
	
	private static String getContentType(File requestFile) {
		String contentType = "\r\n";
		String filename = requestFile.getName();

		if (filename.endsWith(".png")) {
			contentType = "Content-Type: image/png\r\n\r\n";
		} else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
			contentType = "Content-Type: image/jpeg\r\n\r\n";
		} else if (filename.endsWith(".gif")) {
			contentType = "Content-Type: image/gif\r\n\r\n";
		} else if (filename.endsWith(".html") || filename.endsWith(".htm")) {
			contentType = "Content-Type: text/html\r\n\r\n";
		} else if (filename.endsWith(".css")) {
			contentType = "Content-Type: text/css\r\n\r\n";
		} else if (filename.endsWith(".js")) {
			contentType = "Content-Type: application/x-javascript\r\n\r\n";
		}

		return contentType;
	}
}
