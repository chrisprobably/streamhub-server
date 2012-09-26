package com.streamhub.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.streamhub.util.Sleep;
import com.streamhub.util.SocketUtils;

class NIOMockClientSimulator {
	private static final NumberFormat formatter = new DecimalFormat("0.00");
	private final Logger log = Logger.getLogger(NIOMockClientSimulator.class);
	private static final InetSocketAddress STREAMING_SERVER_ADDRESS = new InetSocketAddress("localhost", 6767);
	private final int numberOfClients;
	private LinkedList<MockNIOClient> pendingConnect = new LinkedList<MockNIOClient>();
	private List<MockNIOClient> clients = new ArrayList<MockNIOClient>();
	private Selector selector;
	private ConnectorLoop connectorLoop;
	private final int updatesPerSecond;
	private StatsMonitor statsMonitor;
	private final ExecutorService threadPool = Executors.newFixedThreadPool(2);
	//private Set<Integer> writableClients = new TreeSet<Integer>();
	//private Set<Integer> connectableClients = new TreeSet<Integer>();

	static {
		DOMConfigurator.configure("conf/log4j.xml");
	}

	public static void main(String[] args) throws IOException {
		NIOMockClientSimulator mockClientSimulator = new NIOMockClientSimulator(2000, 1);
		mockClientSimulator.start();
		System.out.println("NIOMockClientSimulator started. Press any key to stop...");
		System.in.read();
		mockClientSimulator.stop();
		System.out.println("NIOMockClientSimulator stopped");
	}

	public void stop() {
		connectorLoop.stop();
		SocketUtils.closeQuietly(selector);
		statsMonitor.stop();
		threadPool.shutdownNow();
	}

	public NIOMockClientSimulator(int numberOfClients, int updatesPerSecond) {
		this.numberOfClients = numberOfClients;
		this.updatesPerSecond = updatesPerSecond;
	}

	public void start() {
		statsMonitor = new StatsMonitor();
		new Thread(statsMonitor).start();
		try {
			selector = Selector.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
		connectorLoop = new ConnectorLoop();
		new Thread(connectorLoop).start();
		setUpMockNIOClients();
		connect();
	}

	private void setUpMockNIOClients() {
		for (int i = 0; i < numberOfClients; i++) {
			clients.add(new MockNIOClient(i, STREAMING_SERVER_ADDRESS, updatesPerSecond));
		}
	}

	public void connect() {
		for (MockNIOClient client : clients) {
			connect(client);
		}
	}

	private class ConnectorLoop implements Runnable {
		private boolean isStopped;

		public void run() {
			Thread.currentThread().setName("ConnectorLoop");
			for (;;) {
				try {
					int n = selector.select();

					if (n > 0) {
						processSelectedKeys();
					}

					processPendingConnections();

					if (isStopped) {
						selector.close();
						return;
					}
				} catch (IOException x) {
					x.printStackTrace();
				}
			}
		}

		public void stop() {
			isStopped = true;
		}
	}

	private class StatsMonitor implements Runnable {

		private boolean isStopped;

		public void run() {
			Thread.currentThread().setName("StatsMonitor");
			while (!isStopped) {
				Sleep.seconds(5);
				int numberOfConnectedClients = numberOfConnectedClients();
				String averageUpdatesPerSecond = formatter.format(totalUpdatesPerSecond() / (double) numberOfConnectedClients);
				String averageBytesPerSecond = formatter.format(totalBytesPerSecond() / (double) numberOfConnectedClients);
				String averageLatency = formatter.format(totalLatency() / (double) numberOfConnectedClients);
				System.out.println(new StringBuilder()
									.append(numberOfConnectedClients)
									.append(",")
									.append(averageUpdatesPerSecond)
									.append(",")
									.append(averageBytesPerSecond)
									.append(",")
									.append(averageLatency)
									.append("ms"));
									//.append(",")
									//.append(missing(writableClients)));
			}
		}

		public void stop() {
			isStopped = true;
		}
	}

	private void connect(MockNIOClient client) {
		SocketChannel channel = null;
		try {
			channel = SocketChannel.open();
			channel.configureBlocking(false);
			boolean connected = channel.connect(STREAMING_SERVER_ADDRESS);
			client.setChannel(channel);

			if (connected) {

			} else {
				synchronized (pendingConnect) {
					pendingConnect.add(client);
				}

				selector.wakeup();
			}
		} catch (IOException x) {
			SocketUtils.closeQuietly(channel);
		}
	}

	public Set<Integer> missing(Set<Integer> set) {
		Set<Integer> missing = new TreeSet<Integer>();
		for (int i = 0; i < numberOfClients; i++) {
			if (!set.contains(i)) {
				missing.add(i);
			}
		}
		return missing;
	}

	public double totalLatency() {
		double totalLatency = 0.0;

		synchronized (clients) {
			for (MockNIOClient client : clients) {
				if (client.isConnected()) {
					double averageLatency = client.getAverageLatency();
					if (averageLatency >= 0.0) {
						totalLatency += averageLatency;
					}
				}
			}
		}

		return totalLatency;
	}

	public double totalBytesPerSecond() {
		double totalBytesPerSecond = 0;

		synchronized (clients) {
			for (MockNIOClient client : clients) {
				if (client.isConnected()) {
					totalBytesPerSecond += client.getBytesPerSecond();
				}
			}
		}

		return totalBytesPerSecond;
	}

	public double totalUpdatesPerSecond() {
		double totalUpdatesPerSecond = 0;

		synchronized (clients) {
			for (MockNIOClient client : clients) {
				if (client.isConnected()) {
					totalUpdatesPerSecond += client.getUpdatesPerSecond();
				}
			}
		}

		return totalUpdatesPerSecond;
	}

	public int numberOfConnectedClients() {
		int count = 0;

		synchronized (clients) {
			for (MockNIOClient client : clients) {
				if (client.isConnected()) {
					count++;
				}
			}
		}

		return count;
	}

	public void processPendingConnections() {
		synchronized (pendingConnect) {
			while (pendingConnect.size() > 0) {
				MockNIOClient client = pendingConnect.removeFirst();
				try {
					client.getChannel().register(selector, SelectionKey.OP_CONNECT, client);
				} catch (IOException x) {
					client.disconnect();
				}
			}
		}
	}

	public void processSelectedKeys() throws IOException {
		for (Iterator<SelectionKey> keys = selector.selectedKeys().iterator(); keys.hasNext();) {
			SelectionKey key = (SelectionKey) keys.next();
			keys.remove();

			final MockNIOClient client = (MockNIOClient) key.attachment();
			final SocketChannel channel = (SocketChannel) key.channel();

			if (key.isValid() && key.isConnectable()) { //&& !(key.isWritable() || key.isReadable())) {
				//connectableClients.add(client.getUid());
				try {
					if (channel.finishConnect()) {
						try {
							client.getChannel().register(selector, SelectionKey.OP_WRITE, client);
							client.onChannelConnect();
						} catch (IOException x) {
							System.out.println("Fail-1");
							//x.printStackTrace();
							client.disconnect();
						}
					} else {
						System.out.println("Client-" + client.getUid() + " not finished connecting!");
					} 
				} catch (IOException x) {
					//System.out.println("Fail-2");
					//x.printStackTrace();
					channel.close();
					retry(client);
				}
			} else if (key.isValid() && key.isWritable()) {
				if (key.interestOps() != SelectionKey.OP_READ) {
					log.info("Client-" + client.getUid() + " registering for reads");
					//writableClients.add(client.getUid());
					client.getChannel().register(selector, SelectionKey.OP_READ, client);
					threadPool.execute(new Runnable() {
						public void run() {
							Thread.currentThread().setName("onChannelWritable");
							client.onChannelWritable(channel);
						}
					});
				}
			} else if (!client.isReading() && key.isValid() && key.isReadable()) {
				client.onChannelReadable(channel);
			}
		}
	}

	private void retry(MockNIOClient client) {
		clients.remove(client);
		MockNIOClient newClient = new MockNIOClient(client.getUid(), STREAMING_SERVER_ADDRESS, updatesPerSecond);
		clients.add(newClient);
		connect(newClient);
		client = null;
	}
}
