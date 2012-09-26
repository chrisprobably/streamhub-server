package com.streamhub.util;

import java.net.MalformedURLException;
import java.net.URL;

import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SocketListener;
import org.mortbay.http.handler.ResourceHandler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHttpContext;
import org.mortbay.util.InetAddrPort;

public class StaticHttpServer {
	private static final String DEFAULT_CONTEXT_PATH = "/test/*";
	private static final String DEFAULT_CONTENT_ROOT_PATH = "src/test/resources";
	private final int port;
	private final String contextPath;
	private final HttpServer httpServer;

	public StaticHttpServer(int port) {
		this(port, DEFAULT_CONTENT_ROOT_PATH, DEFAULT_CONTEXT_PATH);
		try {
			addProxyControllerServlet();
		} catch (Exception e) {
			throw new RuntimeException("Could not start StaticHttpServer", e);
		}
	}
	
	public StaticHttpServer(int port, String contentRootPath, String contextPath) {
		this.port = port;
		this.contextPath = contextPath;
		httpServer = new Server();
		httpServer.addListener(new SocketListener(new InetAddrPort(port)));
		addStaticContext(contentRootPath, contextPath);
	}

	public void start() throws Exception {
		httpServer.start();
	}

	public void stop() throws InterruptedException {
		httpServer.stop();
	}
	
	public URL getUrl() throws MalformedURLException {
		return new URL("http://localhost:" + port + contextPath);
	}

	public static void main(String[] args) throws Exception {
		StaticHttpServer server = new StaticHttpServer(8156);
		try {
			server.start();
			System.out.println("Press any key to stop server");
			System.in.read();
		} finally {
			server.stop();
		}
	}
	
	private void addStaticContext(String contentRootPath, String contextPath) {
		HttpContext context = new HttpContext();
		context.setContextPath(contextPath);
		httpServer.addContext(context);
		context.setResourceBase(contentRootPath);
		context.addHandler(new ResourceHandler());
	}
	
	private void addProxyControllerServlet() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		ServletHttpContext context = (ServletHttpContext) httpServer.getContext("/");
		context.addServlet("ProxyControllerServlet", "/proxy/*", "com.streamhub.tools.proxy.ProxyControllerServlet");
	}	
}
