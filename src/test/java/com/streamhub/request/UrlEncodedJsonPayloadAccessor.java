package com.streamhub.request;

import com.streamhub.api.Payload;

public class UrlEncodedJsonPayloadAccessor {
	public static Payload createFrom(String json) {
		return UrlEncodedJsonPayload.createFrom(json);
	}
}
