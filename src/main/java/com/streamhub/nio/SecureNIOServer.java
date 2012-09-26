package com.streamhub.nio;

import java.net.InetSocketAddress;
import java.net.URL;

import javax.net.ssl.SSLContext;

import org.apache.log4j.Logger;

import com.streamhub.ContextHandler;
import com.streamhub.DirectHandler;
import com.streamhub.api.PushServer;
import com.streamhub.handler.Handler;
import com.streamhub.handler.RawHandler;

/**
 * A HTTPS version of {@link NIOServer}.
 * <p>
 * A {@link SSLContext} is passed to every constructor which defines where to 
 * load the certificate from and which protocol to use.  The {@link SSLContext} 
 * must be initialized before being passed to the constructor.
 * <p>
 * This example shows how to load a keystore named <code>.keystore</code> from the 
 * current directory, which is protected by the password <code>changeit</code>.  The 
 * protocol chosen in this case is <code>SSL</code>. 
 * 
<pre>
  SSLContext context = SSLContext.getInstance("SSL");
  KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
  Builder builder = Builder.newInstance(KeyStore.getDefaultType(), null, new File(".keystore"), new KeyStore.PasswordProtection("changeit"));
  KeyStore keystore = builder.getKeyStore();
  kmf.init(keystore, "changeit");
  // MUST initialize the SSLContext before passing to SecureNIOServer
  context.init(kmf.getKeyManagers(), null, null);
  PushServer server = new SecureNIOServer(443, context);
  server.start();
  System.out.println("HTTPS server started on port 443");
  System.out.println("Press any key to stop...");
  System.in.read();
  server.stop();
  System.out.println("Server stopped");
</pre>
 * <p>
 * For more information on importing SSL certificates in to keystores and 
 * using SSL in Java refer to some of the following resources:
 * <ul>
 * <li><a href="http://blog.pantek.com/opensores/2009/08/add-godaddy-ssl-certificate-to-tomcat-6.html">Importing GoDaddy SSL Certificates</a></li>
 * <li><a href="http://java.sun.com/j2se/1.5.0/docs/tooldocs/solaris/keytool.html">Sun's Keytool reference page</a></li>
 * <li><a href="https://knowledge.verisign.com/support/mpki-for-ssl-support/index?page=content&id=AR227">Verisign Certificate Signing Request Instructions</a></li>
 * </ul>
 * 
 * @see NIOServer
 */
public class SecureNIOServer extends NIOServer implements PushServer {
	private static final Logger log = Logger.getLogger(SecureNIOServer.class);
	private SecureNIOConnectionFactory connectionFactory;
	private final SSLContext sslContext;
	
	/**
	 * Creates a new HTTPS server bound to the wildcard address.
	 * 
	 * @param port			the port to listen on
	 * @param sslContext	an initiated {@link SSLContext} defining the 
	 * 						characteristics of the HTTPS certificate
	 */
	public SecureNIOServer(int port, SSLContext sslContext) {
		super(new InetSocketAddress(port));
		this.sslContext = sslContext;
		init();
	}
	
	/**
	 * Creates a new HTTPS server bound to address.
	 * 
	 * @param address		the address to bind to
	 * @param sslContext	an initiated {@link SSLContext} defining the 
	 * 						characteristics of the HTTPS certificate
	 */
	public SecureNIOServer(InetSocketAddress address, SSLContext sslContext) {
		super(address, null);
		this.sslContext = sslContext;
		init();
	}

	/**
	 * Creates a new HTTPS server bound to address and a streaming adapter 
	 * bound to streamingAdapterAddress
	 * 
	 * @param address					the address to bind the server to
	 * @param streamingAdapterAddress	the address to listen for streaming adapters
	 * @param sslContext				an initiated {@link SSLContext} defining the 
	 * 									characteristics of the HTTPS certificate
	 */
	public SecureNIOServer(InetSocketAddress address, InetSocketAddress streamingAdapterAddress, SSLContext sslContext) {
		super(address, streamingAdapterAddress);
		this.sslContext = sslContext;
		init();
	}

	/**
	 * Creates a new HTTPS server bound to address and a streaming adapter 
	 * bound to streamingAdapterAddress, loading the log4j xml configuration 
	 * from the alternative locations specified by 
	 * <code>log4jConfigurationUrl</code>w.  To choose not to start a streaming 
	 * adapter, pass <code>null</code> as the second argument.
	 * <p>
	 * The following URL formats are supported for the  
	 * <code>log4jConfigurationUrl</code> parameter:
	 * <ul>
	 * <li>File URLs e.g. <code>file:///C:/conf/log4j.xml</code></li>
	 * <li>Remote URLs e.g. <code>http://www.example.com/logging/log4j.xml</code></li>
	 * <li>JAR URLs e.g. <code>jar:http://www.example.com/logging/streamhub-logging.jar!/log4j.xml</code></li>
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
	 * @param sslContext				an initiated {@link SSLContext} defining the 
	 * 									characteristics of the HTTPS certificate
	 */
	public SecureNIOServer(InetSocketAddress address, InetSocketAddress streamingAdapterAddress, URL log4jConfigurationUrl, SSLContext sslContext) {
		super(address, streamingAdapterAddress, log4jConfigurationUrl);
		this.sslContext = sslContext;
		init();
	}

	private void init() {
		try {
			connectionFactory = new SecureNIOConnectionFactory();
			Handler cometHandler = new ContextHandler(subscriptionManager);
			Handler directHandler = new DirectHandler(subscriptionManager);
			Handler handler = new RawHandler(cometHandler, directHandler);
			clientAcceptor = new SecureAcceptor(address.getAddress(), port, handler, connectionFactory, sslContext);
		} catch (Exception e) {
			log.error("Error starting SecureAcceptor", e);
		}
	}
}
