package com.streamhub.api;

import java.io.File;
import java.io.IOException;

import com.streamhub.handler.Handler;
import com.streamhub.nio.NIOServer;

/**
 * The server to which connections are made by clients.  This interface represents  
 * the core functionality of the Comet and HTTP Push server.
 * <p>
 * To create and start an implementation of the server use one of the {@link NIOServer} constructors
 * 
 *  @see NIOServer
 */
public interface PushServer extends Publisher {
	/**
	 * Starts the server
	 * <p>
	 * Note this method is non-blocking
	 */
	void start();
	/**
	 * Stops the server
	 * <p>
	 * This method will block until all the connections have been 
	 * forcibly closed and internal threads shutdown
	 * 
	 * @throws IOException
	 */
	void stop() throws IOException;
	/**
	 * Identifies whether the server is currently started or not
	 * 
	 * @return <code>true</code> if the server is currently started and running, 
	 * <code>false</code> if it has not been started or has been stopped
	 */
	boolean isStarted();
	/**
	 * Returns the servers {@link SubscriptionManager}.
	 * <p>
	 * The {@link SubscriptionManager} provides the core publish and subscribe 
	 * listening capabilities of the server.
	 * <p>
	 * Use {@link SubscriptionManager#addPublishListener(PublishListener)} to 
	 * receive callbacks when a client publishes a message to the server.
	 * <p>
	 * Use {@link SubscriptionManager#addSubscriptionListener(SubscriptionListener)} to 
	 * receive callbacks when a client subscribes to a topic.
	 * <p>
	 * It is recommended to call this method after starting the server using {@link #start()}
	 * 
	 * @return	the {@link SubscriptionManager} associated with this server 
	 */
	SubscriptionManager getSubscriptionManager();
	/**
	 * Adds a directory to be served from the root context <code>/</code> as if it were 
	 * a standard HTTP server.  This allows static content, for example HTML pages, 
	 * to be retrieved via a web browser.
	 * <p>
	 * Although StreamHub is capable of serving normal static content it was 
	 * primarily designed as a Comet and HTTP Push server. For a live website or application 
	 * it is recommended to serve static content from a standard HTTP Web server 
	 * such as Apache or IIS. 
	 * 
	 * @param directory	the directory on the local filesytem to make available via the web
	 * @see #addStaticContent(File, String)
	 */
	void addStaticContent(File directory);
	/**
	 * Adds a directory to be served from the a specified context as if it were 
	 * a standard HTTP server.  This allows static content, for example HTML pages, 
	 * to be retrieved via a web browser.
	 * <p>
	 * Although StreamHub is capable of serving normal static content it was 
	 * primarily designed as a Comet and HTTP Push server. For a live website or application 
	 * it is recommended to serve static content from a standard HTTP Web server 
	 * such as Apache or IIS. 
	 * 
	 * 
	 * @param directory	the directory on the local filesytem to make available via the web
	 * @param context	the context to serve the directory under.  For example using a context 
	 *					of <code>site</code> would make content available under 
	 *					<code>http://serverurl/site/</code>
	 * @see #addStaticContent(File)
	 */
	void addStaticContent(File directory, String context);
	/**
	 * Sets the value of a default HTTP header for asynchronous push responses.
	 * 
	 * @param name	the name of the header
	 * @param value	the value of the header
	 */
	void setDefaultPushHeader(String name, String value);
	/**
	 * Sets the value of a default HTTP header for synchronous responses.
	 * 
	 * @param name	the name of the header
	 * @param value	the value of the header
	 */	
	void setDefaultHeader(String name, String value);
	/**
	 * Adds or overrides a context.  An example of a context is <code>/examples/*</code>. 
	 * Any incoming requests which match the context will be routed to the handler.
	 * 
	 * @param context	a context specifier string e.g. <code>/examples/*</code>
	 * @param handler	a handler which will handle requests matching the context
	 */
	void addContext(String context, Handler handler);
}