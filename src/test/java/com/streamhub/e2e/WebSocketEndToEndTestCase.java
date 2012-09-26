package com.streamhub.e2e;


import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import com.streamhub.api.JsonPayload;
import com.streamhub.demo.TestSubscriptionListener;
import com.streamhub.tools.proxy.Proxy;
import com.streamhub.util.Sleep;
import com.thoughtworks.selenium.Selenium;
import com.thoughtworks.selenium.condition.Condition;
import com.thoughtworks.selenium.condition.JUnitConditionRunner;
import com.thoughtworks.selenium.condition.Text;
import com.thoughtworks.selenium.condition.ConditionRunner.Context;

public abstract class WebSocketEndToEndTestCase extends TestCase {
	protected static final String CONNECTING_TO = "Connecting response iFrame to ";
	protected Selenium browser;
	protected JUnitConditionRunner conditionRunner;
	protected Proxy streamingServerProxy;
	protected Proxy streamingServerProxyTwo;
	protected TestSubscriptionListener subscriptionListener;
	protected boolean disconnectInTearDown = true;
	protected WebSocketEndToEndFramework framework;
	private boolean isPolling;
	

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		framework = WebSocketEndToEndFramework.getInstance();
		streamingServerProxy = framework.getStreamingServerProxy();
		streamingServerProxyTwo = framework.getStreamingServerProxyTwo();
		browser = framework.getBrowser();
		conditionRunner = framework.getConditionRunner();
		subscriptionListener = framework.getSubscriptionListener();
	}
	
	@Override
	protected void tearDown() throws Exception {
		if (streamingServerProxy.isStopped()) {
			streamingServerProxy.start();
		}
		
		if (streamingServerProxyTwo.isStopped()) {
			streamingServerProxyTwo.start();
		}		
		
		if (! isDisconnected() && disconnectInTearDown) {
			disconnect();
			waitForDisconnection();
		}
		
		subscriptionListener.clear();
		
		disconnectInTearDown = true;
		super.tearDown();
	}

	protected void waitForText(String text) {
		try {
			conditionRunner.waitFor(new Text(text));
		} catch(Throwable t) {
			throw new AssertionFailedError("Expecting text " + text + " failed to become true. Complete text [" + browser.getBodyText() + "]");
		}
	}
	
	protected void open(String page) {
		try {
			browser.open(page);
		} catch (Exception e) {
			browser.open(page);
		}
		
		browser.waitForPageToLoad("5000");
	}

	protected void waitForConnection() {
		waitForConnection(framework.getStreamingServerUrl());
	}

	protected void waitForConnectionViaProxy() {
		waitForConnection(framework.getStreamingServerProxyUrl());
	}
	
	protected void connect() {
		connect(framework.getStreamingServerUrl());
	}

	protected void connectViaProxy() {
		connect(framework.getStreamingServerProxyUrl());
	}
	
	protected void disconnect() {
		browser.click("disconnect");
	}
	
	protected void subscribe(final String topic) {
		browser.type("topic", topic);
		browser.click("subscribe");
		conditionRunner.waitFor(new Condition("Subscription to " + topic) {
			@Override
			public boolean isTrue(Context arg0) {
				return subscriptionListener.hasSubscribed(topic);
			}
		});
	}

	protected void unsubscribe(final String topic) {
		browser.type("unsubscribeTopic", topic);
		browser.click("unsubscribe");
		conditionRunner.waitFor(new Condition() {
			@Override
			public boolean isTrue(Context arg0) {
				return subscriptionListener.hasUnSubscribed(topic);
			}
		});
	}
	
	protected void publish(String topic, String json) {
		browser.type("topic", topic);
		browser.type("json", json);
		browser.click("publish");
	}
	
	protected void fakeDisconnection() {
		streamingServerProxy.stop();
	}

	protected void fakeDisconnectionViaProxyTwo() {
		streamingServerProxyTwo.stop();
	}
	
	protected void fakeConnectionBackUp() throws IOException {
		streamingServerProxy.start();
	}
	
	protected void fakeProxyTwoConnectionBackUp() throws IOException {
		streamingServerProxyTwo.start();
	}

	protected void waitForDisconnection() {
		waitForText("Disconnection response is : disconnection OK");
	}
	
	protected void waitForReconnection() {
		waitForText("onResponseData via response iFrame : [response OK]");
	}

	protected void waitForSubscription() {
		waitForSubscriptionResponseOk();
		waitForText("priceChangeListener response for topic 'AAPL' is Name: 'Apple'");
		waitForText("priceChangeListener response for topic 'KO' is Name: 'Coca-Cola'");
	}

	protected void waitForSubscriptionResponseOk() {
		waitForText("Subscription response is : subscription OK");
	}

	protected void sendPrice(String topic, final String price) {
		subscriptionListener.publish(topic, new JsonPayload(topic) {{
			addField("Last", price);
		}});
	}
	
	protected void clearText() {
		browser.click("clearText");
	}

	private void connect(String url) {
		waitForText("Server URL:");
		browser.type("url", url);
		browser.click("connect");
	}
	
	private void waitForConnection(String url) {
		waitForText("Connecting request iFrame to " + url + "request/?uid=1");
		waitForText("Connecting response iFrame to " + url + "response/?uid=1");
		waitForText("Connection response is : request OK");
		waitForText("onResponseData via response iFrame : [response OK]");
	}
	
	private boolean isDisconnected() {
		return browser.isTextPresent("Lost connection to server");
	}

	protected void connectWithFailover(String[] serverList, String failoverAlgorithm, Map<String, String> options) {
		open("index.html");
		String serverListAsString = Arrays.toString(serverList).replaceAll("[\\[\\] ]", "");
		browser.type("serverList", serverListAsString);
		browser.select("failoverAlgorithm", failoverAlgorithm);
		
		for (Map.Entry<String,String> option : options.entrySet()) {
			String optionName = option.getKey();
			
			if ("useExponentialBackOff".equals(optionName)) {
				if ("true".equals(option.getValue())) {
					browser.check(optionName);
				} else if ("false".equals(optionName)) {
					browser.uncheck(optionName);
				} else {
					throw new AssertionError("Must specify either true or false for checkbox option");
				}
			} else if ("connectionType".equals(optionName)) {
				String connectionType = option.getValue();
				browser.select(optionName, connectionType);
				if ("POLL".equals(connectionType)) {
					isPolling = true;
				}
			} else {
				browser.type(optionName, option.getValue());
			}
		}
		
		browser.click("connectAdvanced");

		if (! isPolling) {
			waitForText(CONNECTING_TO + framework.getStreamingServerProxyUrl());
			waitForConnectionViaProxy();
		}
	}
	
	protected void bringConnectionBackUpAndAssertClientReconnects() throws IOException {
		waitForText("Lost connection to server");
		Sleep.millis(250);
		fakeConnectionBackUp();
		browser.click("clearText");
		Sleep.millis(250);
		waitForReconnection();
	}	

	protected void connectThenTriggerDisconnection() {
		connectViaProxy();
		waitForConnectionViaProxy();
		fakeDisconnection();
	}

	protected void addConnectionListener() {
		browser.click("addConnectionListener");
	}
}

