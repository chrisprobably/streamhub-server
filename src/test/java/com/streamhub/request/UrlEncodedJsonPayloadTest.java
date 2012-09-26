package com.streamhub.request;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Map;

import org.junit.Test;

import com.streamhub.api.Payload;
import com.streamhub.request.UrlEncodedJsonPayload;

public class UrlEncodedJsonPayloadTest {
	@Test
	public void toCometBytes() throws Exception {
		String jsonSource = "{\"hey\":\"fsdfsd\",\"wefewf\":\"ewfwef\"}";
		byte[] expectedBytes = ("<script>x(" + jsonSource + ");</script>").getBytes();
		Payload payload = UrlEncodedJsonPayload.createFrom(jsonSource);
		byte[] actualBytes = payload.toCometBytes();
		assertEquals(Arrays.toString(expectedBytes), Arrays.toString(actualBytes));
	}
	
	@Test
	public void testToString() throws Exception {
		String jsonSource = "{\"hey\":\"fsdfsd\",\"wefewf\":\"ewfwef\"}";
		Payload payload = UrlEncodedJsonPayload.createFrom(jsonSource);
		assertEquals(jsonSource, payload.toString());
	}
	
	@Test
	public void testDecodesSingleQuotes() throws Exception {
		String jsonSource = "{%22hey%22:%22fsdfsd%22}";
		Payload payload = UrlEncodedJsonPayload.createFrom(jsonSource);
		assertEquals("{\"hey\":\"fsdfsd\"}", payload.toString());
	}

	@Test
	public void getFields() throws Exception {
		String jsonSource = "{\"hey\":\"fsdfsd\",\"wefewf\":\"ewfwef\"}";
		Payload payload = UrlEncodedJsonPayload.createFrom(jsonSource);
		Map<String, String> fields = payload.getFields();
		assertEquals(2, fields.size());
		assertEquals("fsdfsd", fields.get("hey"));
		assertEquals("ewfwef", fields.get("wefewf"));
	}

	@Test
	public void addField() throws Exception {
		String jsonSource = "{\"hey\":\"fsdfsd\",\"wefewf\":\"ewfwef\"}";
		Payload payload = UrlEncodedJsonPayload.createFrom(jsonSource);
		payload.addField("woah", "totally");
		Map<String, String> fields = payload.getFields();
		assertEquals(3, fields.size());
		assertEquals("fsdfsd", fields.get("hey"));
		assertEquals("ewfwef", fields.get("wefewf"));
		assertEquals("totally", fields.get("woah"));
	}
	
	@Test
	public void equality() throws Exception {
		String jsonSource = "{\"hey\":\"fsdfsd\",\"wefewf\":\"ewfwef\"}";
		String jsonSourceTwo = "{\"hey\":\"fsdfsd\",\"diff\":\"diff2\"}";
		Payload payloadOne = UrlEncodedJsonPayload.createFrom(jsonSource);
		Payload payloadTwo = UrlEncodedJsonPayload.createFrom(jsonSource);
		Payload payloadDifferent = UrlEncodedJsonPayload.createFrom(jsonSourceTwo);
		assertTrue(payloadOne.equals(payloadTwo));
		assertTrue(payloadTwo.equals(payloadOne));
		assertFalse(payloadOne.equals(payloadDifferent));
		assertFalse(payloadOne.equals(null));
	}
}
