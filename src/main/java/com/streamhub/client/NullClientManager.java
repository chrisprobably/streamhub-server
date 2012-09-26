package com.streamhub.client;

import org.apache.log4j.Logger;

import com.streamhub.request.Request;

public class NullClientManager implements ClientManager {
	private static final Logger log = Logger.getLogger(NullClientManager.class);
	
	public IStreamingClient findOrCreate(Request request) throws CannotCreateClientException {
		throw new CannotCreateClientException("NullClientManager cannot create clients");
	}

	public void remove(IStreamingClient client) {
		log.error("Tried to remove client on NullClientManager");
	}

	public IStreamingClient find(String uid) {
		log.error("Tried to find client on NullClientManager uid '" + uid + "'");
		return null;
	}
}
