package com.streamhub.tools.browser;

import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.streamhub.util.Sleep;

public class RemoteMultipleClientSimulator extends MultipleClientSimulatorBase {

	private static final ExecutorService threadPool = Executors.newSingleThreadExecutor();

	public RemoteMultipleClientSimulator(URL streamingServerUrl, int numberOfClients) {
		super(streamingServerUrl, numberOfClients, Strategy.CONNECT);
	}

	public RemoteMultipleClientSimulator(URL streamingServerUrl, int numberOfClients, int browserBatchSize,	long browserBatchInterval) {
		super(streamingServerUrl, numberOfClients, Strategy.CONNECT, browserBatchSize, browserBatchInterval);
	}

	public RemoteMultipleClientSimulator(URL url, int numberOfClients, Strategy subscribe) {
		super(url, numberOfClients, subscribe);
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 3) {
			usage();
		}

		URL streamingServerUrl = new URL(args[0]);
		int numberOfClients = Integer.valueOf(args[1]);
		int updatesPerSecond = Integer.valueOf(args[2]);

		startSimulator(streamingServerUrl, numberOfClients, updatesPerSecond);
	}
	
	public void stop() throws IOException {
		for (MockBrowser client : clients) {
			client.stop();
		}
	}
	
	public double averageUpdatesPerSecond() {
		double totalUpdatesPerSecond = 0;
		
		for (MockBrowser client : clients) {
			totalUpdatesPerSecond += ((MeasuringMockBrowser) client).getUpdatesPerSecond();
		}
		
		return totalUpdatesPerSecond / (double)numberOfConnectedClients();
	}
	
	public double averageBytesPerSecond() {
		int totalBytesPerSecond = 0;
		
		for (MockBrowser client : clients) {
			totalBytesPerSecond += ((MeasuringMockBrowser) client).getBytesPerSecond();
		}
		
		return (double)totalBytesPerSecond / numberOfConnectedClients();
	}
	
	protected void setupBrowsers() {
		for (int i = 0; i < numberOfClients; i++) {
			MeasuringMockBrowser browser = new MeasuringMockBrowser(streamingServerUrl, i);
			clients.add(browser);
			runnables.add(new BrowserRunnable(browser, strategy));
		}
	}

	private static void startSimulator(URL streamingServerUrl, int numberOfClients, int updatesPerSecond) throws Exception, IOException {
		RemoteMultipleClientSimulator simulator = new RemoteMultipleClientSimulator(streamingServerUrl, numberOfClients);
		simulator.start();
		
		StatsMonitor statsMonitor = new StatsMonitor(simulator);
		threadPool.execute(statsMonitor);

		for (int i = 0; i < updatesPerSecond; i++) {
			simulator.subscribeAllAtOnce("topic-" + i, 500);
		}

		System.out.println("Remote Client Simulator started");
		System.out.println("Press any key to stop...");
		System.in.read();
		System.out.println("Stopping...");
		statsMonitor.stop();
		simulator.stop();
		System.out.println("Stopped");
	}

	private static void usage() {
		System.out.println("Usage: RemoteMultipleClientSimultator <streamingServerUrl> <numberOfClients> <updatesPerSecond>");
		System.exit(-1);
	}
	
	private static class StatsMonitor implements Runnable {
		private static final NumberFormat formatter = new DecimalFormat("0.00");
		private final RemoteMultipleClientSimulator simulator;
		private boolean isStopped;

		public StatsMonitor(RemoteMultipleClientSimulator simulator) {
			this.simulator = simulator;
		}

		public void run() {
			while (!isStopped) {
				Sleep.seconds(2);
				System.out.print(simulator.numberOfConnectedClients() + ",");
				System.out.print(formatter.format(simulator.averageUpdatesPerSecond()) + ",");
				System.out.println(formatter.format(simulator.averageBytesPerSecond()));
			}
		}
		
		public void stop() {
			isStopped = true;
		}
	}
}
