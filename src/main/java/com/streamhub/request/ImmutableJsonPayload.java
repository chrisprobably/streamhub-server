package com.streamhub.request;

import java.util.Collections;
import java.util.Map;

import org.apache.log4j.Logger;

import com.streamhub.api.Payload;

class ImmutableJsonPayload implements Payload {
	private static final String SCRIPT_END = ");</script>";
	private static final String SCRIPT_START = "<script>x(";
	private static final Logger log = Logger.getLogger(UrlEncodedJsonPayload.class);
	private final String toString;
	private final byte[] bytes;

	private ImmutableJsonPayload(String jsonSource) {
		toString = jsonSource;
		bytes = new StringBuilder(SCRIPT_START).append(jsonSource).append(SCRIPT_END).toString().getBytes();
	}

	public void addField(String key, String value) {
		log.warn("Attempt to add field to ImmutableJsonPayload");
	}
	
	public void timestamp() {
		log.warn("Attempt to timestamp ImmutableJsonPayload");
	}

	public void toggleTimestamping(boolean onOrOff) {
		log.warn("Attempt to toggle timestamping on an ImmutableJsonPayload");
	}

	public Map<String, String> getFields() {
		log.warn("Attempt to get fields to ImmutableJsonPayload");
		return Collections.emptyMap();
	}

	public byte[] toCometBytes() {
		return bytes;
	}

	public static Payload createFrom(String jsonSource) {
		return new ImmutableJsonPayload(jsonSource);
	}

	@Override
	public String toString() {
		return toString;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((toString == null) ? 0 : toString.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ImmutableJsonPayload))
			return false;
		final ImmutableJsonPayload other = (ImmutableJsonPayload) obj;
		if (toString == null) {
			if (other.toString != null)
				return false;
		} else if (!toString.equals(other.toString))
			return false;
		return true;
	}
}
