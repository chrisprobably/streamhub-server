package com.streamhub.client;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.log4j.Logger;

import com.streamhub.request.Request;
import com.streamhub.util.CircularFIFOMap;

public class StreamingClientManager implements ClientManager, StreamingClientManagerMBean {
	private static final Logger log = Logger.getLogger(StreamingClientManager.class);
	private final Map<String, IStreamingClient> clients;
	private final ClientFactory clientFactory;
	private final int clientLimit;

	public StreamingClientManager(ClientFactory clientFactory, int clientLimit) {
		this.clientFactory = clientFactory;
		this.clients = Collections.synchronizedMap(new CircularFIFOMap<String, IStreamingClient>(clientLimit));
		this.clientLimit = clientLimit;
		registerSelfAsMBean();
	}

	private void registerSelfAsMBean() {
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			ObjectName name = new ObjectName("com.streamhub.client:type=StreamingClientManager");
			mbs.registerMBean(this, name);
		} catch (Exception e) {
			log.warn("Could not register MBean", e);
		}

	}

	public synchronized IStreamingClient findOrCreate(Request request) throws CannotCreateClientException {
		String uid = request.getUid();
		IStreamingClient client = clients.get(uid);

		if (client == null) {
			client = createNewClient(request, uid);
		}

		if (request.isResponseConnection()) {
			client.setConnection(request.getConnection());
		}

		return client;
	}

	public synchronized IStreamingClient find(String uid) {
		IStreamingClient streamingClient = clients.get(uid);

		if (streamingClient == null) {
			log.warn("Could not find Client-" + uid + " returning null");
		}

		return streamingClient;
	}

	public synchronized void remove(IStreamingClient client) {
		clients.remove(client.getUid());
	}

	public int getClientLimit() {
		return clientLimit;
	}

	public int getConnectedClients() {
		int connectedCount = 0;
		for (IStreamingClient client : clients.values()) {
			if (client.isConnected()) {
				connectedCount++;
			}
		}
		return connectedCount;
	}

	public int getTotalClients() {
		return clients.size();
	}

	Map<String, IStreamingClient> getClients() {
		return clients;
	}

	private IStreamingClient createNewClient(Request request, String uid) throws CannotCreateClientException {
		IStreamingClient client = clientFactory.createFrom(request);
		add(uid, client);
		return client;
	}

	private void add(String uid, IStreamingClient client) {
		// Returns any clients that were pushed out because of breaching the
		// user limit
		IStreamingClient pushedOutClient = clients.put(uid, client);

		if (pushedOutClient != null) {
			log.error("MAX USER LIMIT REACHED: Disconnecting Client-" + pushedOutClient.getUid());
			pushedOutClient.disconnect();
		}
	}
}
