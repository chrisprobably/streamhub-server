package com.streamhub.client;

import com.streamhub.request.Request;

public interface ClientManager {
	IStreamingClient findOrCreate(Request request) throws CannotCreateClientException;
	IStreamingClient find(String uid);
	void remove(IStreamingClient client);
	
}