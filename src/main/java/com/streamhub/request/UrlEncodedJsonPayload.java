package com.streamhub.request;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.streamhub.api.Payload;

class UrlEncodedJsonPayload implements Payload {
	private static final Logger log = Logger.getLogger(UrlEncodedJsonPayload.class);
	private JSONObject json = new JSONObject();
	private byte[] bytes = new byte[0];
	private boolean isTimestampingEnabled; 
	
	private UrlEncodedJsonPayload(String jsonSource) {
		try {
			jsonSource = decode(jsonSource);
			json = new JSONObject(jsonSource);
			bytes = ("<script>x(" + json.toString() + ");</script>").getBytes();
		} catch (JSONException e) {
			log.error("Error creating JSONObject", e);
		}
	}

	public void addField(String key, String value) {
		try {
			json.put(key, value);
			bytes = ("<script>x(" + json.toString() + ");</script>").getBytes();
		} catch (JSONException e) {
			log.error("Problem adding field with key '" + key + "' and value '" + value + "'", e);
		}
	}

	@SuppressWarnings("rawtypes")
	public Map<String, String> getFields() {
		Map<String, String> fields = new HashMap<String, String>();
		Iterator keys = json.keys();
		
		while(keys.hasNext()) {
			String key = (String) keys.next();
			String value = null;
			try {
				value = json.getString(key);
			} catch (JSONException e) {
				log.error("Error getting field with key '" + key + "'", e);
			}
			
			fields.put(key, value);
		}

		return fields;
	}
	
	public String getField(String key) {
		String value = "";
		
		try {
			value = json.getString(key);
		} catch (JSONException e) {
			log.error("Error getting field with key '" +  key + "'", e);
		}
		
		return value;
	}

	public byte[] toCometBytes() {
		return bytes;
	}
	
	public void timestamp() {
		if (isTimestampingEnabled) {
			addField("timestamp", String.valueOf(System.currentTimeMillis()));
		}
	}

	public void toggleTimestamping(boolean onOrOff) {
		isTimestampingEnabled = onOrOff;
	}

	public static Payload createFrom(String jsonSource) {
		return new UrlEncodedJsonPayload(jsonSource);
	}
	
	@Override
	public String toString() {
		return json.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((json == null) ? 0 : json.toString().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof UrlEncodedJsonPayload))
			return false;
		final UrlEncodedJsonPayload other = (UrlEncodedJsonPayload) obj;
		if (json == null) {
			if (other.json != null)
				return false;
		} else if (!json.toString().equals(other.json.toString()))
			return false;
		return true;
	}

	private String decode(String jsonSource) {
		String decoded = jsonSource;
		
		try {
			decoded = URLDecoder.decode(jsonSource, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.error("Error decoding json '" + jsonSource + "'", e);
		}
		
		return decoded;
	}
}
