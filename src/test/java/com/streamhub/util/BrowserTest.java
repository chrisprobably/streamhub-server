package com.streamhub.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BrowserTest {
	@Test
	public void browserFamily() throws Exception {
		assertTrue(Browser.CHROME.isWebKitFamily());
		assertTrue(Browser.FF3.isFirefoxFamily());
		assertTrue(Browser.FF2.isFirefoxFamily());
		assertTrue(Browser.IE.isIEFamily());
		assertTrue(Browser.IE7.isIEFamily());
		assertTrue(Browser.IE8.isIEFamily());
		assertTrue(Browser.SAFARI.isWebKitFamily());
		assertFalse(Browser.CHROME.isIEFamily());
	}
	
	@Test
	public void ieBrowserDetection() throws Exception {
		String sixtyFourBitUserAgentHeader = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.2; WOW64; SV1; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30)";
		String userAgentHeader = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 2.0.50727; .NET CLR 3.0.04506.648; .NET CLR 3.5.21022; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)";
		assertEquals(Browser.IE, Browser.fromUserAgent(userAgentHeader));
		assertEquals(Browser.IE, Browser.fromUserAgent(sixtyFourBitUserAgentHeader));
	}
	
	@Test
	public void ie7BrowserDetection() throws Exception {
		String userAgentHeader = "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; .NET CLR 2.0.50727; .NET CLR 3.0.04506.648; .NET CLR 3.5.21022; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)";
		assertEquals(Browser.IE7, Browser.fromUserAgent(userAgentHeader));
	}
	
	@Test
	public void ie8BrowserDetection() throws Exception {
		String userAgentHeader = "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0; .NET CLR 1.1.4322)";
		assertEquals(Browser.IE8, Browser.fromUserAgent(userAgentHeader));
	}
	
	@Test
	public void safariBrowserDetection() throws Exception {
		String userAgentHeader = "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/525.13 (KHTML, like Gecko) Version/3.1 Safari/525.13.3";
		String safariFourHeader = "Mozilla/5.0 (Windows; U; Windows NT 5.2; en-US) AppleWebKit/528.18 (KHTML, like Gecko) Version/4.0 Safari/528.17";
		assertEquals(Browser.SAFARI, Browser.fromUserAgent(userAgentHeader));
		assertEquals(Browser.SAFARI, Browser.fromUserAgent(safariFourHeader));
	}
	
	@Test
	public void chromeBrowserDetection() throws Exception {
		String userAgentHeader = "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/525.19 (KHTML, like Gecko) Chrome/1.0.154.65 Safari/525.19";
		assertEquals(Browser.CHROME, Browser.fromUserAgent(userAgentHeader));
	}
	
	@Test
	public void ff2BrowserDetection() throws Exception {
		String userAgentHeader = "Mozilla/5.0 (Windows; U; Windows NT 5.2; en-GB; rv:1.8.1.20) Gecko/20081217 Firefox/2.0.0.20";
		assertEquals(Browser.FF2, Browser.fromUserAgent(userAgentHeader));
	}
	
	@Test
	public void ff3BrowserDetection() throws Exception {
		String userAgentHeader = "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-GB; rv:1.9.0.10) Gecko/2009042316 Firefox/3.0.10 (.NET CLR 3.5.30729)";
		assertEquals(Browser.FF3, Browser.fromUserAgent(userAgentHeader));
	}
	
	@Test
	public void fromUserAgentStringPerformance() throws Exception {
		String userAgentHeader = "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-GB; rv:1.9.0.10) Gecko/2009042316 Firefox/3.0.10 (.NET CLR 3.5.30729)";
		long startTime = System.currentTimeMillis();
		int iterations = 100000;
		for(int i=0; i<iterations; i++) {
			Browser.fromUserAgent(userAgentHeader);
		}
		long elapse = System.currentTimeMillis() - startTime;
		System.out.println("Took " + elapse + "ms");
	}
	
	@Test
	public void unknownBrowserDetection() throws Exception {
		String javaUserAgentHeader = "Java/1.6.0_13";
		String userAgentHeader = "Opera/9.60 (J2ME/MIDP; Opera Mini/4.1.11320/608; U; en) Presto/2.2.0";
		assertEquals(Browser.UNKNOWN, Browser.fromUserAgent(userAgentHeader));
		assertEquals(Browser.UNKNOWN, Browser.fromUserAgent(javaUserAgentHeader));
	}
}
