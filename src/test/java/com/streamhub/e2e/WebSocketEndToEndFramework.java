package com.streamhub.e2e;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import org.openqa.selenium.server.SeleniumServer;

import com.streamhub.api.JsonPayload;
import com.streamhub.api.Payload;
import com.streamhub.api.PushServer;
import com.streamhub.demo.TestSubscriptionListener;
import com.streamhub.nio.NIOServer;
import com.streamhub.nio.SecureNIOServer;
import com.streamhub.tools.TestSSLContext;
import com.streamhub.tools.proxy.Proxy;
import com.streamhub.util.Browser;
import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.HttpCommandProcessor;
import com.thoughtworks.selenium.Selenium;
import com.thoughtworks.selenium.condition.JUnitConditionRunner;

public class WebSocketEndToEndFramework {
	private static final int STREAMING_SERVER_PORT = 7979;
	private static final int HTTPS_STREAMING_SERVER_PORT = 8889;
	private static final int STREAMING_SERVER_PROXY_PORT = 7654;
	private static final int STREAMING_SERVER_PROXY_TWO_PORT = 7656;
	private static final int HTTPS_STREAMING_SERVER_PROXY_PORT = 7655;
	private static final int HTTPS_STREAMING_SERVER_PROXY_TWO_PORT = 7657;
	private static final String STREAMHUB_CONTEXT = "/streamhub/";
	private static final String HOSTNAME = "localhost";
	private static final int SELENIUM_PORT = 4444;
	private static final int WAIT_FOR_INTERVAL = 50;
	private static final int WAIT_FOR_TIMEOUT = 7000;
	private static final WebSocketEndToEndFramework INSTANCE = new WebSocketEndToEndFramework();
	
	protected SeleniumServer seleniumServer;
	protected Selenium browser;
	protected HttpCommandProcessor commandProcessor;
	protected JUnitConditionRunner conditionRunner;
	protected Proxy streamingServerProxy;
	protected Proxy streamingServerProxyTwo;
	
	private PushServer streamingServer;
	private TestSubscriptionListener subscriptionListener;
	private String streamingServerUrl;
	private String streamingServerProxyUrl;
	private String streamingServerProxyTwoUrl;
	
	public static WebSocketEndToEndFramework getInstance() {
		return INSTANCE;
	}
	
	public void start(Browser browserType, boolean isHttps) throws Exception {
		initializeUrls(isHttps);
		startFramework(isHttps);
		startSeleniumServer();
		String seleniumUrl = streamingServerUrl + "test/";
		seleniumUrl = seleniumUrl.replaceFirst(STREAMHUB_CONTEXT, "/");
		commandProcessor = new HttpCommandProcessor(HOSTNAME, SELENIUM_PORT, browserType.getSeleniumSpec(), seleniumUrl);
		startBrowser();
	}
	
	public void stop() throws Exception {
		browser.stop();
		seleniumServer.stop();
		stopFramework();
	}

	public Selenium getBrowser() {
		return browser;
	}
	
	public JUnitConditionRunner getConditionRunner() {
		return conditionRunner;
	}
	
	public TestSubscriptionListener getSubscriptionListener() {
		return subscriptionListener;
	}

	public Proxy getStreamingServerProxy() {
		return streamingServerProxy;
	}
	
	public Proxy getStreamingServerProxyTwo() {
		return streamingServerProxyTwo;
	}

	public String getStreamingServerUrl() {
		return streamingServerUrl;
	}

	public String getStreamingServerProxyTwoUrl() {
		return streamingServerProxyTwoUrl;
	}

	public String getStreamingServerProxyUrl() {
		return streamingServerProxyUrl;
	}
	
	public static void main(String[] args) throws Exception {
		WebSocketEndToEndFramework framework = WebSocketEndToEndFramework.getInstance();
		try {
			framework.initializeUrls(false);
			framework.startFramework(false);
			System.out.println("Press any key to shutdown EndToEndFramework...");
			System.in.read();
		} catch(Throwable t) {
			t.printStackTrace();
		} finally {
			framework.stopFramework();
		}
	}

	private void startBrowser() {
		browser = new DefaultSelenium(commandProcessor);
		browser.start();
		browser.windowMaximize();
		conditionRunner = new JUnitConditionRunner(browser, WAIT_FOR_INTERVAL, WAIT_FOR_TIMEOUT);		
	}

	private void startSeleniumServer() throws Exception {
		seleniumServer = new SeleniumServer();
		seleniumServer.getConfiguration().setSingleWindow(true);
		seleniumServer.start();
	}
	
	private void startFramework(boolean isHttps) throws Exception {
		if (isHttps) {
			streamingServerProxy = new Proxy(HTTPS_STREAMING_SERVER_PROXY_PORT, new URL(streamingServerUrl));
			streamingServerProxy.start();
			streamingServerProxyTwo = new Proxy(HTTPS_STREAMING_SERVER_PROXY_TWO_PORT, new URL(streamingServerUrl));
			streamingServerProxyTwo.start();
			streamingServer = new SecureNIOServer(HTTPS_STREAMING_SERVER_PORT, TestSSLContext.newInstance());
		} else {
			streamingServerProxy = new Proxy(STREAMING_SERVER_PROXY_PORT, new URL(streamingServerUrl));
			streamingServerProxy.start();
			streamingServerProxyTwo = new Proxy(STREAMING_SERVER_PROXY_TWO_PORT, new URL(streamingServerUrl));
			streamingServerProxyTwo.start();
			streamingServer = new NIOServer(STREAMING_SERVER_PORT);
		}
		streamingServer.addStaticContent(new File("src/test/resources"), "/test");
		streamingServer.getSubscriptionManager().addPublishListener(new EchoPublishListener());
		subscriptionListener = new TestSubscriptionListener(streamingServer);
		streamingServer.start();
		addDemoResponses();
		System.out.println("Started http server at " + streamingServerUrl + "test/");
		System.out.println("Started streaming server at " + streamingServerUrl);
	}

	private void stopFramework() throws IOException, InterruptedException {
		streamingServerProxy.stop();
		streamingServerProxyTwo.stop();
		streamingServer.addStaticContent(null, null);
		streamingServer.stop();
		System.out.println("Stopped");
	}	

	@SuppressWarnings("serial")
	private void addDemoResponses() {
		subscriptionListener.setSubscriptionResponses(new HashMap<String, Payload>() {{
			put("AAPL", response("AAPL", "Apple", "115.75"));
			put("KO", response("KO", "Coca-Cola", "44.97"));
		}});
	}	
	
	private JsonPayload response(final String topic, final String name, final String price) {
		return new JsonPayload(topic) {{
			addField("Name", name);
			addField("Last", price);
		}};
	}
	
	private void initializeUrls(boolean isHttps) {
		if (isHttps) {
			streamingServerUrl = "https://" + HOSTNAME + ":" + HTTPS_STREAMING_SERVER_PORT + STREAMHUB_CONTEXT;
			streamingServerProxyUrl = "https://" + HOSTNAME + ":" + HTTPS_STREAMING_SERVER_PROXY_PORT + STREAMHUB_CONTEXT;
			streamingServerProxyTwoUrl = "https://" + HOSTNAME + ":" + HTTPS_STREAMING_SERVER_PROXY_TWO_PORT + STREAMHUB_CONTEXT;
		} else {
			streamingServerUrl = "http://" + HOSTNAME + ":" + STREAMING_SERVER_PORT + STREAMHUB_CONTEXT;
			streamingServerProxyUrl = "http://" + HOSTNAME + ":" + STREAMING_SERVER_PROXY_PORT + STREAMHUB_CONTEXT;
			streamingServerProxyTwoUrl = "http://" + HOSTNAME + ":" + STREAMING_SERVER_PROXY_TWO_PORT + STREAMHUB_CONTEXT;
		}
	}
}
