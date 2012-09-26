package com.streamhub.nio;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.streamhub.ContextHandler;
import com.streamhub.DirectHandler;
import com.streamhub.HttpHandler;
import com.streamhub.StreamingSubscriptionManager;
import com.streamhub.api.Payload;
import com.streamhub.api.PushServer;
import com.streamhub.api.SubscriptionManager;
import com.streamhub.client.ClientFactory;
import com.streamhub.client.ClientManager;
import com.streamhub.client.StreamingClientManager;
import com.streamhub.handler.Handler;
import com.streamhub.handler.RawHandler;
import com.streamhub.nio.CommandLine.Options;
import com.streamhub.request.StreamingAdapterHandler;

/**
 * The main Comet and HTTP Push server to which connections are made by all clients.
 * <p>
 * A common idiom for running the server until it is stopped by pressing a key is:
 * 
<pre>
  PushServer server = new NIOServer(80);
  server.start();
  System.out.println("Server started on port 80");
  System.out.println("Press any key to stop...");
  System.in.read();
  server.stop();
  System.out.println("Server stopped");
</pre>
 * <p>
 * In order to stream data to clients, listeners need 
 * to be added to the {@link SubscriptionManager}.
 * 
<pre>
  PushServer server = new NIOServer(80);
  server.start();
  SubscriptionManager subscriptionManager = server.getSubscriptionManager();
  subscriptionManager.addSubscriptionListener(this);
  subscriptionManager.addPublishListener(this);
</pre>
 * <p>
 * For more details on getting started please refer to the guides below:
 * <ul>
 * <li><a href="http://streamhub.blogspot.com/2009/07/getting-started-with-streamhub-and.html">Getting Started with StreamHub and Comet</a></li>
 * <li><a href="http://streamhub.blogspot.com/2009/07/tutorial-building-comet-chat.html">Tutorial: Building a Comet Chat Application with StreamHub</a></li>
 * </ul>
 * 
 * @see SecureNIOServer
 */
public class NIOServer implements PushServer {
	private static final String DEFAULT_LOG4J_CONF_LOCATION = "conf/log4j.xml";
	private static final Logger log = Logger.getLogger(NIOServer.class);
	
	protected final int port;
	protected final InetSocketAddress address;
	protected StreamingSubscriptionManager subscriptionManager;
	protected Acceptor clientAcceptor;
	private final ExecutorService threadPool = Executors.newSingleThreadExecutor();
	private final ExecutorService adapterThreadPool = Executors.newSingleThreadExecutor();
	private final InetSocketAddress streamingAdapterAddress;
	private final Handler cometHandler;
	private StreamingClientManager streamingClientManager;
	private boolean isStarted;
	private int userLimit = 1;
	private Acceptor streamingAdapterAcceptor;
	private URL log4jConfigurationUrl;
	private ConnectionFactory connectionFactory = new NIOConnectionFactory();

	public static void main(String[] args) throws Exception {
		Options options = CommandLine.parse(args);
		PushServer server = null;
		
		if (options.loggingUrl == null) {
			server = new NIOServer(options.serverAddress, options.streamingAdapterAddress);
		} else {
			server = new NIOServer(options.serverAddress, options.streamingAdapterAddress, options.loggingUrl);
		}
		
		server.start();
		System.out.println("Press any key to stop...");
		System.in.read();
		server.stop();
	}
	
	/**
	 * Creates a new NIOServer bound to the wildcard address
	 * 
	 * @param port	the port to listen on
	 */
	public NIOServer(int port) {
		this(new InetSocketAddress(port));
	}
	
	/**
	 * Creates a new NIOServer bound to address.
	 * 
	 * @param address	the address to bind to
	 */
	public NIOServer(InetSocketAddress address) {
		this(address, null);
	}

	/**
	 * Creates a new NIOServer bound to address and a streaming adapter 
	 * bound to streamingAdapterAddress.  To choose not to start a streaming 
	 * adapter, pass <code>null</code> as the second argument.
	 * 
	 * @param address					the address to bind the server to
	 * @param streamingAdapterAddress	the address to listen for streaming adapters or <code>null</code> if 
	 * 									a streaming adapter is not required
	 */
	public NIOServer(InetSocketAddress address, InetSocketAddress streamingAdapterAddress) {
		this.address = address;
		this.streamingAdapterAddress = streamingAdapterAddress;
		this.port = address.getPort();
		subscriptionManager = new StreamingSubscriptionManager();
		cometHandler = new ContextHandler(subscriptionManager);
		Handler directHandler = new DirectHandler(subscriptionManager);
		Handler handler = new RawHandler(cometHandler, directHandler);
		clientAcceptor = new Acceptor(address.getAddress(), port, handler, connectionFactory);
		if (streamingAdapterAddress != null) {
			startStreamingAdapterListener(streamingAdapterAddress);
		}
	}

	/**
	 * Creates a new NIOServer bound to address and a streaming adapter 
	 * bound to streamingAdapterAddress, loading the log4j xml configuration 
	 * from the alternative locations specified by 
	 * <code>log4jConfigurationUrl</code> respectively.  To choose not to start a streaming 
	 * adapter, pass <code>null</code> as the second argument.
	 * <p>
	 * The following URL formats are supported for the 
	 * <code>log4jConfigurationUrl</code> parameter:
	 * <ul>
	 * <li>File URLs e.g. <code>file:///C:/logging/log4j.xml</code></li>
	 * <li>Remote URLs e.g. <code>http://www.example.com/logging/log4j.xml</code></li>
	 * <li>JAR URLs e.g. <code>jar:file:///C:/lib/streamhub-logging.jar!/log4j.xml</code> or <code>jar:http://www.example.com/logging/streamhub-logging.jar!/log4j.xml</code></li>
	 * <li>Classpath URLs via <code>com.streamhub.util.UrlLoader</code> e.g. <code>UrlLoader.load("classpath:/conf/log4j.xml")</code></li>
	 * </ul>
	 * <p>
     * The default location for the log4j 
	 * configuration is "conf/log4j.xml" relative to the current working directory. 
	 * 
	 * @param address					the address to bind the server to
	 * @param streamingAdapterAddress	the address to listen for streaming adapters or <code>null</code> if 
	 * 									a streaming adapter is not required
	 * @param log4jConfigurationUrl		an alternative {@link URL} to load the log4j xml configuration from
	 */
	public NIOServer(InetSocketAddress address, InetSocketAddress streamingAdapterAddress, URL log4jConfigurationUrl) {
		this(address, streamingAdapterAddress);
		this.log4jConfigurationUrl = log4jConfigurationUrl;
	}

	/* (non-Javadoc)
	 * @see com.streamhub.api.PushServer#stop()
	 */
	public void stop() throws IOException {
		clientAcceptor.stop();
		if (streamingAdapterAcceptor != null) {
			streamingAdapterAcceptor.stop();
		}
		threadPool.shutdownNow();
		adapterThreadPool.shutdownNow();
		isStarted = false;
		subscriptionManager.stop();
        log.info("Server stopped");
	}

	/* (non-Javadoc)
	 * @see com.streamhub.api.PushServer#start()
	 */
	public void start() {
		startLogging();
		threadPool.execute(clientAcceptor);
		streamingClientManager = new StreamingClientManager(new ClientFactory(subscriptionManager), userLimit);
		subscriptionManager.start(streamingClientManager);
		isStarted = true;
        log.info("StreamHub Server " + this.getClass().getPackage().getImplementationVersion() + " started on port " + port);
        if (streamingAdapterAcceptor != null) {
        	adapterThreadPool.execute(streamingAdapterAcceptor);
        	log.info("Streaming adapter listening for connections on " + streamingAdapterAddress.getPort());
        }
	}

	/* (non-Javadoc)
	 * @see com.streamhub.api.PushServer#getSubscriptionManager()
	 */
	public SubscriptionManager getSubscriptionManager() {
		return subscriptionManager;
	}

	/* (non-Javadoc)
	 * @see com.streamhub.api.PushServer#isStarted()
	 */
	public boolean isStarted() {
		return isStarted;
	}

	/* (non-Javadoc)
	 * @see com.streamhub.api.Publisher#publish(java.lang.String, com.streamhub.api.Payload)
	 */
	public void publish(String topic, Payload payload) {
		subscriptionManager.send(topic, payload);
	}
	
	/* (non-Javadoc)
	 * @see com.streamhub.api.PushServer#addStaticContent(java.io.File)
	 */
	public void addStaticContent(File directory) {
		HttpHandler.addStaticDirectory(directory);
	}

	/* (non-Javadoc)
	 * @see com.streamhub.api.PushServer#addStaticContent(java.io.File, java.lang.String)
	 */
	public void addStaticContent(File directory, String context) {
		HttpHandler.addStaticDirectory(directory, context);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.streamhub.api.PushServer#setDefaultPushHeader(java.lang.String, java.lang.String)
	 */
	public void setDefaultPushHeader(String name, String value) {
		HttpHandler.setDefaultPushHeader(name, value);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.streamhub.api.PushServer#setDefaultHeader(java.lang.String, java.lang.String)
	 */
	public void setDefaultHeader(String name, String value) {
		HttpHandler.setDefaultHeader(name, value);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.streamhub.api.PushServer#addContext(java.lang.String, com.streamhub.handler.Handler)
	 */
	public void addContext(String context, Handler handler) {
		((ContextHandler)cometHandler).addContext(context, handler);
	}

	ClientManager getClientManager() {
		return streamingClientManager;
	}

	private void startLogging() {
		if (log4jConfigurationUrl == null) {
			DOMConfigurator.configure(DEFAULT_LOG4J_CONF_LOCATION);
		} else {
			DOMConfigurator.configure(log4jConfigurationUrl);
		}
	}

	private void startStreamingAdapterListener(InetSocketAddress address) {
		Handler streamingAdapterHandler = new StreamingAdapterHandler(subscriptionManager);
		streamingAdapterAcceptor = new Acceptor(address.getAddress(), address.getPort(), streamingAdapterHandler, connectionFactory);
	}	
}
