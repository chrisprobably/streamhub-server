package com.streamhub;

import static org.junit.Assert.*;

import org.junit.Test;

import com.streamhub.util.Browser;

public class ResponseFactoryTest {
	@Test
	public void synchronousResponsesDoNotContainContentLength() throws Exception {
		String closeResponseFF = ResponseFactory.closeResponse("bob.com", Browser.FF3);
		String closeResponseIE = ResponseFactory.closeResponse("bob.com", Browser.FF3);
		String disconnectionResponseFF = ResponseFactory.disconnectionResponse("bob.com", Browser.FF3);
		String disconnectionResponseIE = ResponseFactory.disconnectionResponse("bob.com", Browser.IE);
		String pollResponse = ResponseFactory.pollResponse("bob.com", Browser.FF3, "");
		String publishResponse = ResponseFactory.publishResponse("bob.com", Browser.FF3);
		String requestResponse = ResponseFactory.requestResponse("bob.com", Browser.FF3);
		String subscriptionResponse = ResponseFactory.subscriptionResponse("bob.com", Browser.IE7);
		String unSubscribeResponse = ResponseFactory.unSubscribeResponse("bob.com", Browser.CHROME);
		
		assertFalse(closeResponseFF.contains("Content-Length"));
		assertFalse(closeResponseIE.contains("Content-Length"));
		assertFalse(disconnectionResponseFF.contains("Content-Length"));
		assertFalse(disconnectionResponseIE.contains("Content-Length"));
		assertFalse(pollResponse.contains("Content-Length"));
		assertFalse(publishResponse.contains("Content-Length"));
		assertFalse(requestResponse.contains("Content-Length"));
		assertFalse(subscriptionResponse.contains("Content-Length"));
		assertFalse(unSubscribeResponse.contains("Content-Length"));
	}
	
	@Test
	public void responseChannelContainsContentLength() throws Exception {
		String responseChannelFF = ResponseFactory.foreverFramePageHeader("bob.com", Browser.FF3);
		String responseChannelIE = ResponseFactory.foreverFramePageHeader("bob.com", Browser.IE);
		String responseChannelSafari = ResponseFactory.foreverFramePageHeader("bob.com", Browser.SAFARI);
		
		assertTrue(responseChannelFF.contains("Content-Length"));
		assertTrue(responseChannelIE.contains("Content-Length"));
		assertTrue(responseChannelSafari.contains("Content-Length"));
	}
}
