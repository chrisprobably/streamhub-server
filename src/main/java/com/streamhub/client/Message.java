package com.streamhub.client;

import com.streamhub.api.Payload;

class Message {
	public final String topic;
	public final Payload payload;

	public Message(String topic, Payload payload) {
		this.topic = topic;
		this.payload = payload;
	}
	
	@Override
	public String toString() {
		return "Topic: " + topic + ", Payload: " + payload;
	}
}
