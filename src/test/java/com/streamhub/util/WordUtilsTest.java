package com.streamhub.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WordUtilsTest {
	@Test
	public void capitalizesWord() throws Exception {
		String result = WordUtils.capitalizeFully("sErver");
		assertEquals("Server", result);
	}
	
	@Test
	public void capitalizesWordSeparatedByDash() throws Exception {
		String result = WordUtils.capitalizeFully("cONtent-typE");
		assertEquals("Content-Type", result);
	}
	
	@Test
	public void doesNotBlowUpOnStringEndingInDash() throws Exception {
		String result = WordUtils.capitalizeFully("conTent-");
		assertEquals("Content-", result);
	}
	
	@Test
	public void doesNotBlowUpOnStringStartingWithDash() throws Exception {
		String result = WordUtils.capitalizeFully("-type");
		assertEquals("-type", result);
	}
	
	@Test
	public void capitalizesRealShortWord() throws Exception {
		String result = WordUtils.capitalizeFully("a");
		assertEquals("A", result);
	}
	
	@Test
	public void capitalizesRealShortWordSeparatedByDash() throws Exception {
		String result = WordUtils.capitalizeFully("a-b");
		assertEquals("A-B", result);
	}
}
