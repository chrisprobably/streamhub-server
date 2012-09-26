package com.streamhub.tools;

import java.util.Map;

import com.streamhub.api.Payload;

public class NullPayload implements Payload {
	public void addField(String key, String value) {}
	public void setOnlySendChangedFields(boolean value) {}

	public boolean isOnlySendChangedFields() {
		return false;
	}

	public String toOnlyChangedFieldsString(Payload previous) {
		return toString();
	}
	
	public Map<String, String> getFields() {
		return null;
	}
	
	public byte[] toCometBytes() {
		return new byte[0];
	}
	
	public void timestamp() {}
	
	public void toggleTimestamping(boolean onOrOff) {}
}
