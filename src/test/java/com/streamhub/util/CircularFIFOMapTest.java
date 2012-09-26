package com.streamhub.util;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

public class CircularFIFOMapTest {
	@Test
	public void doesNotGrowBiggerThanMaxSize() throws Exception {
		Map<String, String> map = new CircularFIFOMap<String, String>(1);
		map.put("1", "V1");
		assertEquals(1, map.size());
		map.put("2", "V1");
		assertEquals(1, map.size());
	}
	
	@Test
	public void putReturnsRemovedElementIfMaxSizeReached() throws Exception {
		Map<String, String> map = new CircularFIFOMap<String, String>(1);
		map.put("1", "V1");
		String putResult = map.put("2", "V2");
		assertEquals("V1", putResult);
	}
	
	@Test
	public void putReturnsRemovedElementIfMaxSizeReachedAfterClear() throws Exception {
		Map<String, String> map = new CircularFIFOMap<String, String>(2);
		map.put("1", "V1");
		map.put("2", "V2");
		map.put("3", "V3");
		map.clear();
		map.put("4", "V4");
		map.put("5", "V5");
		String putResult = map.put("6", "V6");
		assertEquals("V4", putResult);
		assertEquals(2, map.size());
	}
	
	@Test
	public void putReturnsNullIfMaxSizeNotReached() throws Exception {
		Map<String, String> map = new CircularFIFOMap<String, String>(3);
		map.put("1", "V1");
		String putResult = map.put("2", "V2");
		assertEquals(null, putResult);
	}
	
	@Test
	public void putReturnsNullIfMaxSizeNotReachedAfterClear() throws Exception {
		Map<String, String> map = new CircularFIFOMap<String, String>(2);
		map.put("1", "V1");
		map.put("2", "V2");
		map.put("3", "V3");
		map.clear();
		String putResult = map.put("4", "V4");
		assertEquals(null, putResult);
		assertEquals(1, map.size());
	}
	
	@Test
	public void gettingElements() throws Exception {
		Map<String, String> map = new CircularFIFOMap<String, String>(2);
		map.put("1", "V1");
		map.put("2", "V2");
		map.put("3", "V3");
		assertEquals(null, map.get("1"));
		assertEquals("V2", map.get("2"));
		assertEquals("V3", map.get("3"));
	}
	
	@Test
	public void removingElements() throws Exception {
		Map<String, String> map = new CircularFIFOMap<String, String>(2);
		map.put("1", "V1");
		map.put("2", "V2");
		map.remove("2");
		assertEquals("V1", map.get("1"));
		assertEquals(null, map.get("2"));
		assertEquals(1, map.size());
	}
	
	@Test
	public void putOnlyReturnsNullUntilOverMaxSize() throws Exception {
		Map<String, String> map = new CircularFIFOMap<String, String>(2);
		assertEquals(null, map.put("1", "V1"));
		assertEquals(null, map.put("2", "V2"));
		assertEquals("V1", map.put("3", "V4"));
	}
	
	@Test
	public void putOnlyReturnsNullUntilOverMaxSizeAfterClear() throws Exception {
		Map<String, String> map = new CircularFIFOMap<String, String>(2);
		assertEquals(null, map.put("1", "V1"));
		assertEquals(null, map.put("2", "V2"));
		map.clear();
		assertEquals(null, map.put("1", "V1"));
		assertEquals(null, map.put("2", "V2"));
		assertEquals("V1", map.put("3", "V3"));
	}
	
	@Test
	public void putOnlyReturnsNullUntilOverMaxSizeAfterRemove() throws Exception {
		Map<String, String> map = new CircularFIFOMap<String, String>(2);
		assertEquals(null, map.put("1", "V1"));
		assertEquals(null, map.put("2", "V2"));
		map.remove("1");
		assertEquals(null, map.put("1", "V1"));
		assertEquals("V2", map.put("3", "V3"));
		assertEquals("V1", map.put("4", "V4"));
		assertEquals("V3", map.put("5", "V5"));
	}
}
