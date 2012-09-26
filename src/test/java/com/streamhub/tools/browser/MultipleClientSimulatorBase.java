package com.streamhub.tools.browser;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.streamhub.tools.ConditionRunner;
import com.streamhub.util.Sleep;
import com.thoughtworks.selenium.condition.Condition;
import com.thoughtworks.selenium.condition.ConditionRunner.Context;

public class MultipleClientSimulatorBase {
	private static final int WAIT_FOR_INTERVAL = 100;
	private static final int WAIT_FOR_TIMEOUT = 10000;
	protected final List<MockBrowser> clients = new ArrayList<MockBrowser>();
	protected final List<Runnable> runnables = new ArrayList<Runnable>();
	protected final int numberOfClients;
	protected final Strategy strategy;
	protected final int browserBatchSize;
	protected final long browserBatchInterval;
	protected final List<String> subscribedToStockUids = new ArrayList<String>();
	protected final List<String> subscribedToCurrencyUids = new ArrayList<String>();
	protected final URL streamingServerUrl;
	private final ConditionRunner conditionRunner = new ConditionRunner(WAIT_FOR_INTERVAL, WAIT_FOR_TIMEOUT);
	private final ExecutorService threadPool = Executors.newCachedThreadPool();

	public enum Strategy {
		CONNECT, SUBSCRIBE, SUBSCRIBE_MULTIPLE_TOPICS
	}
	
	public MultipleClientSimulatorBase(URL streamingServerUrl, int numberOfClients, Strategy strategy) {
		this(streamingServerUrl, numberOfClients, strategy, 10, 200);
	}

	public MultipleClientSimulatorBase(URL streamingServerUrl, int numberOfClients, Strategy strategy, int browserBatchSize, long browserBatchInterval) {
		this.streamingServerUrl = streamingServerUrl;
		this.numberOfClients = numberOfClients;
		this.strategy = strategy;
		this.browserBatchSize = browserBatchSize;
		this.browserBatchInterval = browserBatchInterval;
	}

	protected static class BrowserRunnable implements Runnable {
			public final MockBrowser browser;
			private final Strategy strategy;
	
			public BrowserRunnable(MockBrowser browser, Strategy strategy) {
				this.browser = browser;
				this.strategy = strategy;
			}
			
			public void run() {
				try {
					browser.connectToStreamingServer();
					
					if (Strategy.SUBSCRIBE == strategy || Strategy.SUBSCRIBE_MULTIPLE_TOPICS == strategy) {
						browser.subscribe("STOCK");
					}
					
					if (Strategy.SUBSCRIBE_MULTIPLE_TOPICS == strategy) {
						browser.subscribe("CURRENCY");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

	public void start() {
		setupBrowsers();
	}

	public void stop() throws IOException {
		for (MockBrowser client : clients) {
			client.stop();
		}
		threadPool.shutdownNow();
	}

	public void startSimulation() {
		threadPool.execute(new Runnable() {
			public void run() {
				startBrowsers();
			}
		});
	}

	public List<MockBrowser> clients() {
		return clients;
	}

	public void waitForMessages(int numMessages) {
		
		for (MockBrowser client : clients) {
			client.waitForMessages(numMessages);
		}
	}

	public void waitForConnected() {
		for (MockBrowser client : clients) {
			client.waitForConnected();
		}
	}

	public void waitForSubscribed() {
		for (final MockBrowser client : clients) {
			conditionRunner.waitFor(new Condition() {
				@Override
				public boolean isTrue(Context arg0) {
					boolean isSubscribedStockTopic = subscribedToStockUids.contains(String.valueOf(client.getUid()));
					boolean isSubscribedCurrencyTopic = subscribedToCurrencyUids.contains(String.valueOf(client.getUid()));
					
					if (strategy == Strategy.SUBSCRIBE_MULTIPLE_TOPICS) {
						return isSubscribedStockTopic && isSubscribedCurrencyTopic;
					}
					
					return isSubscribedStockTopic;
				}
	
				@Override
				public String getMessage() {
					return "Expected to be subscribed but wasn't. Messages received: " + client.getMessagesReceived();
				}
			});
			client.waitForSubscribed();
		}
	}

	public int numberOfClientsWithSuccessfulConnectionRequest() {
		int count = 0;
		
		for (MockBrowser client : clients) {
			if (client.hasConnectedOk()) {
				count++;
			}
		}
		
		return count;
	}

	public int numberOfSubscribedClients() {
		int count = 0;
		
		for (MockBrowser client : clients) {
			if (client.hasSubscribedOk()) {
				count++;
			}
		}
		
		return count;
	}

	public int numberOfClientsReceived(int numberOfMessages) {
		int count = 0;
		
		for (MockBrowser client : clients) {
			if (client.numberOfMessagesReceived() == numberOfMessages) {
				count++;
			}
		}
		
		return count;
	}

	public int numberOfClientsReceivedMessage(String messageSubstring) {
		int count = 0;
		
		for (MockBrowser client : clients) {
			if (client.hasReceived(messageSubstring)) {
				count++;
			}
		}
		
		return count;
	}
	
	public int numberOfConnectedClients() {
		int count = 0;
		
		for (MockBrowser client : clients) {
			if (client.isConnected()) {
				count++;
			}
		}
		
		return count;
	}


	
	public void subscribeAllAtOnce(final String topic, int batchSize) throws Exception {
		int numClients = clients.size();
		List<MockBrowser> batch = new ArrayList<MockBrowser>();
		
		for (int i = 0; i < numClients; i++) {
			batch.add(clients.get(i));
			
			if (i % (batchSize-1) == 0) {
				final List<MockBrowser> executeBatch = new ArrayList<MockBrowser>(batch);
				batch.clear();
				threadPool.execute(new Runnable() {
					public void run() {
						for (MockBrowser mockBrowser : executeBatch) {
							try {
								mockBrowser.subscribe(topic);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				});
			}
		}
	}

	protected void setupBrowsers() {
		for (int i = 0; i < numberOfClients; i++) {
			MockBrowser browser = new MockBrowser(streamingServerUrl, i);
			clients.add(browser);
			runnables.add(new BrowserRunnable(browser, strategy));
		}
	}

	private void startBrowsers() {
		int browsersStarted = 0;
		
		for (Runnable runnable : runnables) {
			threadPool.execute(runnable);
	
			if (++browsersStarted % browserBatchSize == 0) {
				Sleep.millis(browserBatchInterval);
			}
		}
	}

}