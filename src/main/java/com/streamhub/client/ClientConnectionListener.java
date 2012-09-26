package com.streamhub.client;


public interface ClientConnectionListener {
	void clientConnected(IStreamingClient client);
	void clientLostConnection(IStreamingClient client);
	void clientDisconnected(IStreamingClient client);
}
