package com.streamhub;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.streamhub.api.Payload;
import com.streamhub.api.PublishListener;
import com.streamhub.api.SubscriptionListener;
import com.streamhub.api.SubscriptionManager;
import com.streamhub.client.CannotCreateClientException;
import com.streamhub.client.ClientConnectionListener;
import com.streamhub.client.ClientManager;
import com.streamhub.client.IStreamingClient;
import com.streamhub.client.NullClientManager;
import com.streamhub.request.Request;

public class StreamingSubscriptionManager implements SubscriptionManager, ClientConnectionListener {
	private static final Logger log = Logger.getLogger(StreamingSubscriptionManager.class);
	private final List<SubscriptionListener> subscriptionListeners = new ArrayList<SubscriptionListener>();
	private final List<PublishListener> publishListeners = new ArrayList<PublishListener>();
	private final Map<String, IStreamingClient[]> topicToClients = new HashMap<String, IStreamingClient[]>();
	private ClientManager clientManager = new NullClientManager();
	private final ExecutorService notifyPool = Executors.newFixedThreadPool(2);
	private final ExecutorService sendPool = Executors.newSingleThreadExecutor();
	private final Map<IStreamingClient, ScheduledFuture<?>> removalTasks = new HashMap<IStreamingClient, ScheduledFuture<?>>();
	private ScheduledExecutorService removalScheduler = Executors.newSingleThreadScheduledExecutor();
	private long reconnectionTimeoutMillis = 420000;

	public void addSubscriptionListener(SubscriptionListener subscriptionListener) {
		synchronized (subscriptionListeners) {
			subscriptionListeners.add(subscriptionListener);
		}
	}

	public void addPublishListener(PublishListener publishListener) {
		synchronized (publishListeners) {
			publishListeners.add(publishListener);
		}
	}

	public void removeSubscriptionListener(SubscriptionListener subscriptionListener) {
		synchronized (subscriptionListeners) {
			subscriptionListeners.remove(subscriptionListener);
		}
	}

	public void removePublishListener(PublishListener publishListener) {
		synchronized (publishListeners) {
			publishListeners.remove(publishListener);
		}
	}

	public void clientConnected(IStreamingClient client) {
		ScheduledFuture<?> removalTask = removalTasks.get(client);
		if (removalTask != null) {
			removalTask.cancel(false);
		}
	}

	public void clientLostConnection(final IStreamingClient client) {
		if (removalTasks.get(client) == null && !removalScheduler.isShutdown()) {
			ScheduledFuture<?> removalTask = removalScheduler.schedule(new Runnable() {
				public void run() {
					Thread.currentThread().setName("RemovalTask");
					deleteClient(client);
				}
			}, reconnectionTimeoutMillis, TimeUnit.MILLISECONDS);
			removalTasks.put(client, removalTask);
		}
	}

	public void clientDisconnected(final IStreamingClient client) {
		deleteClient(client);
	}

	public void start(ClientManager clientManager) {
		this.clientManager = clientManager;
	}

	public void send(final String topic, final Payload payload) {
		if (! sendPool.isShutdown()) {
			synchronized (topicToClients) {
				final IStreamingClient[] subscribedClients = topicToClients.get(topic);

				if (subscribedClients == null) {
					return;
				}

				try {
					int size = subscribedClients.length;
					for (int i = 0; i < size; i++) {
						final IStreamingClient client = subscribedClients[i];
						sendPool.execute(new Runnable() {
							public void run() {
								client.send(topic, payload);
							}
						});
					}
				} catch (Throwable e) {
					log.warn("Exception launching send thread", e);
				}
			}
		}
	}

	public void stop() {
		sendPool.shutdownNow();
		notifyPool.shutdownNow();
		removalScheduler.shutdownNow();
	}

	public ClientManager getClientManager() {
		return clientManager;
	}

	void notifyPublishListeners(final IStreamingClient client, final String topic, final Payload payload) {
		notifyPool.execute(new Runnable() {
			public void run() {
				Thread.currentThread().setName("PublishNotifier");
				synchronized (publishListeners) {
					for (PublishListener listener : publishListeners) {
						listener.onMessageReceived(client, topic, payload);
					}
				}
			}
		});
	}

	public void addSubscription(Request request) throws CannotCreateClientException {
		IStreamingClient client = findOrCreateClient(request);
		String[] subscriptionTopics = request.getSubscriptionTopics();
		log.info("Client-" + client.getUid() + " subscribed to '" + Arrays.toString(subscriptionTopics) + "'");

		for (String topic : subscriptionTopics) {
			String trimmedTopic = topic.trim();
			addClient(trimmedTopic, client);
			notifyOnSubscribeListeners(trimmedTopic, client);
		}
	}

	void removeSubscription(Request request) throws CannotCreateClientException {
		IStreamingClient client = findOrCreateClient(request);
		String[] subscriptionTopics = request.getSubscriptionTopics();
		log.info("Client-" + client.getUid() + " unsubscribed from '" + Arrays.toString(subscriptionTopics) + "'");

		for (String topic : subscriptionTopics) {
			String trimmedTopic = topic.trim();
			removeClient(trimmedTopic, client);
			notifyOnUnSubscribeListeners(trimmedTopic, client);
		}
	}

	IStreamingClient findOrCreateClient(Request request) throws CannotCreateClientException {
		return clientManager.findOrCreate(request);
	}

	Map<String, IStreamingClient[]> getTopicToClients() {
		return topicToClients;
	}

	void setReconnectionTimeout(long timeoutMillis) {
		this.reconnectionTimeoutMillis = timeoutMillis;
	}

	void setScheduler(ScheduledExecutorService scheduler) {
		this.removalScheduler = scheduler;
	}

	List<SubscriptionListener> getSubscriptionListeners() {
		return Collections.unmodifiableList(subscriptionListeners);
	}

	List<PublishListener> getPublishListeners() {
		return Collections.unmodifiableList(publishListeners);
	}

	private void addClient(String topic, IStreamingClient client) {
		synchronized (topicToClients) {
			IStreamingClient[] clients = topicToClients.get(topic);

			if (clients == null) {
				topicToClients.put(topic, new IStreamingClient[] { client });
			} else {
				int newSize = clients.length + 1;
				IStreamingClient[] copy = new IStreamingClient[newSize];
				System.arraycopy(clients, 0, copy, 0, newSize - 1);
				copy[newSize - 1] = client;
				topicToClients.put(topic, copy);
			}
		}

		client.addSubscription(topic);
	}

	private void removeClient(String topic, IStreamingClient client) {
		synchronized (topicToClients) {
			IStreamingClient[] clients = topicToClients.get(topic);

			if (clients != null) {
				int count = countSubscribersLeft(client, clients);

				if (count == 0) {
					topicToClients.remove(topic);
				} else {
					IStreamingClient[] copy = createNewSubscribersArray(client, clients, count);
					topicToClients.put(topic, copy);
				}
			}
		}
		client.removeSubscription(topic);
	}

	private IStreamingClient[] createNewSubscribersArray(IStreamingClient client, IStreamingClient[] clients, int count) {
		IStreamingClient[] copy = new IStreamingClient[count];
		int index = 0;

		for (IStreamingClient subscriber : clients) {
			if (subscriber != null && !subscriber.equals(client)) {
				copy[index++] = subscriber;
			}
		}
		return copy;
	}

	private int countSubscribersLeft(IStreamingClient client, IStreamingClient[] clients) {
		int count = 0;
		for (IStreamingClient subscriber : clients) {
			if (! subscriber.equals(client)) {
				count++;
			}
		}
		return count;
	}

	private void notifyOnSubscribeListeners(final String topic, final IStreamingClient client) {
		notifyPool.execute(new Runnable() {
			public void run() {
				Thread.currentThread().setName("OnSubscribeNotifier");
				synchronized (subscriptionListeners) {
					for (SubscriptionListener listener : subscriptionListeners) {
						listener.onSubscribe(topic, client);
					}
				}
			}
		});
	}

	private void notifyOnUnSubscribeListeners(final String topic, final IStreamingClient client) {
		notifyPool.execute(new Runnable() {
			public void run() {
				Thread.currentThread().setName("OnUnSubscribeNotifier");
				synchronized (subscriptionListeners) {
					for (SubscriptionListener listener : subscriptionListeners) {
						listener.onUnSubscribe(topic, client);
					}
				}
			}
		});
	}

	private void deleteClient(IStreamingClient client) {
		if (! client.isConnected()) {
			removeSubscriptions(client);
			clientManager.remove(client);

			for (String topic : client.getSubscriptions()) {
				notifyOnUnSubscribeListeners(topic, client);
			}

			log.info("Client-" + client.getUid() + " subscriptions removed");
			client.destroy();
			client = null;
		}
	}

	private void removeSubscriptions(IStreamingClient client) {
		synchronized (topicToClients) {
			for (String topic : client.getSubscriptions()) {
				IStreamingClient[] clients = topicToClients.get(topic);
				for (int i = 0; i < clients.length; i++) {
					if (client.equals(clients[i])) {
						clients[i] = new NullClient();
					}
				}
			}
		}
	}
}
