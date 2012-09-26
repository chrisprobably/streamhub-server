package com.streamhub.performance;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Test;

import com.streamhub.StreamingServerTestCase;
import com.streamhub.tools.browser.BrowserCounter;
import com.streamhub.tools.browser.CountStrategy;
import com.streamhub.tools.browser.MockBrowser;
import com.streamhub.tools.browser.MultipleClientSimulator;
import com.streamhub.tools.browser.MultipleClientSimulatorBase;
import com.streamhub.tools.browser.MultipleClientSimulatorBase.Strategy;

public class MultipleCometClientsTest extends StreamingServerTestCase {
	private MultipleClientSimulatorBase simulator = null;
	
	@After
	public void tearDown() throws Exception {
		if (simulator != null) {
			simulator.stop();
		}
	}
	
	@Test
	public void testConnectingMultipleClients() throws Exception {
		simulator = new MultipleClientSimulator(new URL("http://localhost:8888/streamhub/"), 2, Strategy.CONNECT);
		waitForConnectionOk();
	}
	
	@Test
	public void testSubscribingLotsOfClientsToManyUpdatesOnMultipleTopics() throws Exception {
		int numberOfClients = 200;
		final int numPricesSent = 10;
		final int numCurrenciesSent = 10;
		simulator = new MultipleClientSimulator(new URL("http://localhost:8888/streamhub/"), numberOfClients, Strategy.SUBSCRIBE_MULTIPLE_TOPICS, 10, 300);
		waitForSubscriptionOk();
		List<String> pricesSent = sendPrices(simulator, numPricesSent);
		List<String> currenciesSent = sendCurrencies(simulator, numCurrenciesSent);
		simulator.waitForMessages(3 + numPricesSent + numCurrenciesSent);
		assertEquals(numberOfClients, simulator.numberOfClientsReceivedMessage("Initial"));
		assertEquals(numberOfClients, simulator.numberOfClientsReceivedMessage("CcyBid"));
		
		for (String price : pricesSent) {
			assertEquals(numberOfClients, simulator.numberOfClientsReceivedMessage(price));
		}
		
		for (String currencyPrice : currenciesSent) {
			assertEquals(numberOfClients, simulator.numberOfClientsReceivedMessage(currencyPrice));
		}
		
		assertEquals(numberOfClients, simulator.numberOfClientsReceived(3 + numPricesSent + numCurrenciesSent));
		new BrowserCounter().printCount("received correct number of messages", simulator.clients(), new CountStrategy() {
			public boolean increaseCount(MockBrowser browser) {
				return browser.numberOfMessagesReceived() == 3 + numPricesSent + numCurrenciesSent;
			}
		});
	}

	@Test
	public void testConnectingFiftyClientsSimultaneously() throws Exception {
		simulator = new MultipleClientSimulator(new URL("http://localhost:8888/streamhub/"), 50, Strategy.CONNECT);
		waitForConnectionOk();
		new BrowserCounter().printCount("connected", simulator.clients(), new CountStrategy() {
			public boolean increaseCount(MockBrowser browser) {
				return browser.hasConnectedOk();
			}
		});
	}

	@Test
	public void testSubscribingMultipleClientsToInitialMessageOnSameTopic() throws Exception {
		simulator = new MultipleClientSimulator(new URL("http://localhost:8888/streamhub/"), 2, Strategy.SUBSCRIBE);
		waitForSubscriptionOk();
		simulator.waitForMessages(2);
		assertEquals(2, simulator.numberOfClientsReceivedMessage("Initial"));
		assertEquals(2, simulator.numberOfClientsReceived(2));
	}

	@Test
	public void testSubscribingMultipleClientsToUpdatesOnSameTopic() throws Exception {
		simulator = new MultipleClientSimulator(new URL("http://localhost:8888/streamhub/"), 2, Strategy.SUBSCRIBE);
		waitForSubscriptionOk();
		((MultipleClientSimulator) simulator).sendPrice("2.2343");
		simulator.waitForMessages(3);
		assertEquals(2, simulator.numberOfClientsReceivedMessage("Initial"));
		assertEquals(2, simulator.numberOfClientsReceivedMessage("2.2343"));
		assertEquals(2, simulator.numberOfClientsReceived(3));
	}

	@Test
	public void testSubscribingMultipleClientsToManyUpdatesOnSameTopic() throws Exception {
		simulator = new MultipleClientSimulator(new URL("http://localhost:8888/streamhub/"), 2, Strategy.SUBSCRIBE);
		waitForSubscriptionOk();
		int numPricesSent = 10;
		List<String> pricesSent = sendPrices(simulator, numPricesSent);
		simulator.waitForMessages(2 + numPricesSent);
		assertEquals(2, simulator.numberOfClientsReceivedMessage("Initial"));

		for (String price : pricesSent) {
			assertEquals(2, simulator.numberOfClientsReceivedMessage(price));
		}

		assertEquals(2, simulator.numberOfClientsReceived(2 + numPricesSent));
	}

	@Test
	public void testSubscribingFiftyClientsToInitialMessageSimultaneously() throws Exception {
		simulator = new MultipleClientSimulator(new URL("http://localhost:8888/streamhub/"), 50, Strategy.SUBSCRIBE);
		waitForSubscriptionOk();
		simulator.waitForMessages(2);
		assertEquals(50, simulator.numberOfClientsReceivedMessage("Initial"));
		assertEquals(50, simulator.numberOfClientsReceived(2));
		new BrowserCounter().printCount("received 'Initial' message", simulator.clients(), new CountStrategy() {
			public boolean increaseCount(MockBrowser browser) {
				return browser.hasReceived("Initial");
			}
		});
	}
	
	@Test
	public void testSubscribingFiftyClientsToUpdatesSimultaneously() throws Exception {
		simulator = new MultipleClientSimulator(new URL("http://localhost:8888/streamhub/"), 50, Strategy.SUBSCRIBE);
		waitForSubscriptionOk();
		((MultipleClientSimulator) simulator).sendPrice("2.2343");
		simulator.waitForMessages(3);
		assertEquals(50, simulator.numberOfClientsReceivedMessage("Initial"));
		assertEquals(50, simulator.numberOfClientsReceivedMessage("2.2343"));
		assertEquals(50, simulator.numberOfClientsReceived(3));
		new BrowserCounter().printCount("received update", simulator.clients(), new CountStrategy() {
			public boolean increaseCount(MockBrowser browser) {
				return browser.hasReceived("2.2343");
			}
		});
	}
	
	@Test
	public void testSubscribingFiftyClientsToManyUpdatesSimultaneously() throws Exception {
		simulator = new MultipleClientSimulator(new URL("http://localhost:8888/streamhub/"), 50, Strategy.SUBSCRIBE);
		final int numPricesSent = 10;
		waitForSubscriptionOk();
		List<String> pricesSent = sendPrices(simulator, numPricesSent);
		simulator.waitForMessages(2 + numPricesSent);
		assertEquals(50, simulator.numberOfClientsReceivedMessage("Initial"));

		for (String price : pricesSent) {
			assertEquals(50, simulator.numberOfClientsReceivedMessage(price));
		}
		assertEquals(50, simulator.numberOfClientsReceived(2 + numPricesSent));
		
		new BrowserCounter().printCount("received correct number of messages", simulator.clients(), new CountStrategy() {
			public boolean increaseCount(MockBrowser browser) {
				return browser.numberOfMessagesReceived() == 2 + numPricesSent;
			}
		});
	}
	
	@Test
	public void testSubscribingMultipleClientsToInitialMessagesOnMultipleTopics() throws Exception {
		simulator = new MultipleClientSimulator(new URL("http://localhost:8888/streamhub/"), 2, Strategy.SUBSCRIBE_MULTIPLE_TOPICS);
		waitForSubscriptionOk();
		simulator.waitForMessages(3);
		assertEquals(2, simulator.numberOfClientsReceivedMessage("Initial"));
		assertEquals(2, simulator.numberOfClientsReceivedMessage("CcyBid"));
		assertEquals(2, simulator.numberOfClientsReceived(3));
	}
	
	@Test
	public void testSubscribingMultipleClientsToUpdatesOnMultipleTopics() throws Exception {
		simulator = new MultipleClientSimulator(new URL("http://localhost:8888/streamhub/"), 2, Strategy.SUBSCRIBE_MULTIPLE_TOPICS);
		waitForSubscriptionOk();
		((MultipleClientSimulator) simulator).sendPrice("125.222");
		((MultipleClientSimulator) simulator).sendCurrency("3-232-22");
		simulator.waitForMessages(5);
		assertEquals(2, simulator.numberOfClientsReceivedMessage("Initial"));
		assertEquals(2, simulator.numberOfClientsReceivedMessage("CcyBid"));
		assertEquals(2, simulator.numberOfClientsReceivedMessage("125.222"));
		assertEquals(2, simulator.numberOfClientsReceivedMessage("3-232-22"));
		assertEquals(2, simulator.numberOfClientsReceived(5));
	}
	
	@Test
	public void testSubscribingMultipleClientsToManyUpdatesOnMultipleTopics() throws Exception {
		simulator = new MultipleClientSimulator(new URL("http://localhost:8888/streamhub/"), 2, Strategy.SUBSCRIBE_MULTIPLE_TOPICS);
		waitForSubscriptionOk();
		int numPricesSent = 10;
		List<String> pricesSent = sendPrices(simulator, numPricesSent);
		int numCurrenciesSent = 10;
		List<String> currenciesSent = sendCurrencies(simulator, numCurrenciesSent);
		simulator.waitForMessages(3 + numPricesSent + numCurrenciesSent);
		assertEquals(2, simulator.numberOfClientsReceivedMessage("Initial"));
		assertEquals(2, simulator.numberOfClientsReceivedMessage("CcyBid"));

		for (String price : pricesSent) {
			assertEquals(2, simulator.numberOfClientsReceivedMessage(price));
		}
		
		for (String currencyPrice : currenciesSent) {
			assertEquals(2, simulator.numberOfClientsReceivedMessage(currencyPrice));
		}
		
		assertEquals(2, simulator.numberOfClientsReceived(3 + numPricesSent + numCurrenciesSent));
	}
	
	@Test
	public void testSubscribingFiftyClientsToManyUpdatesOnMultipleTopics() throws Exception {
		simulator = new MultipleClientSimulator(new URL("http://localhost:8888/streamhub/"), 50, Strategy.SUBSCRIBE_MULTIPLE_TOPICS);
		final int numPricesSent = 10;
		final int numCurrenciesSent = 10;
		waitForSubscriptionOk();
		List<String> pricesSent = sendPrices(simulator, numPricesSent);
		List<String> currenciesSent = sendCurrencies(simulator, numCurrenciesSent);
		simulator.waitForMessages(3 + numPricesSent + numCurrenciesSent);
		assertEquals(50, simulator.numberOfClientsReceivedMessage("Initial"));
		assertEquals(50, simulator.numberOfClientsReceivedMessage("CcyBid"));
		
		for (String price : pricesSent) {
			assertEquals(50, simulator.numberOfClientsReceivedMessage(price));
		}
		
		for (String currencyPrice : currenciesSent) {
			assertEquals(50, simulator.numberOfClientsReceivedMessage(currencyPrice));
		}
		
		assertEquals(50, simulator.numberOfClientsReceived(3 + numPricesSent + numCurrenciesSent));
		new BrowserCounter().printCount("received correct number of messages", simulator.clients(), new CountStrategy() {
			public boolean increaseCount(MockBrowser browser) {
				return browser.numberOfMessagesReceived() == 3 + numPricesSent + numCurrenciesSent;
			}
		});
	}

	private void waitForConnectionOk() {
		simulator.start();
		simulator.startSimulation();
		simulator.waitForConnected();
	}
	
	private void waitForSubscriptionOk() {
		simulator.start();
		simulator.startSimulation();
		simulator.waitForSubscribed();
	}	

	private List<String> sendCurrencies(MultipleClientSimulatorBase simulator, int numCurrenciesSent) {
		List<String> currencisSent = new ArrayList<String>();
		for (int i = 0; i < numCurrenciesSent; i++) {
			String currencyPrice = i + ".000" + i;
			((MultipleClientSimulator) simulator).sendCurrency(currencyPrice);
			currencisSent.add(currencyPrice);
		}
		return currencisSent;
	}

	private List<String> sendPrices(MultipleClientSimulatorBase simulator, int numPrices) {
		List<String> pricesSent = new ArrayList<String>();
		for (int i = 0; i < numPrices; i++) {
			String price = i + ".000" + i;
			((MultipleClientSimulator) simulator).sendPrice(price);
			pricesSent.add(price);
		}
		return pricesSent;
	}
}
