package com.streamhub.client;

public interface StreamingClientManagerMBean {
	int getConnectedClients();
	int getTotalClients();
	int getClientLimit();
}
