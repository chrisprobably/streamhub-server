package com.streamhub.e2e;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

public class PublishTest extends EndToEndTestCase {
	@Test
	public void testPublishingToStreamingServer() throws Exception {
		connectAndSubscribe();
		publish("myTopic", "{\"favouriteFruit\":\"apple\"}");
		waitForText("key=favouriteFruit value=apple");
	}

	@Test
	public void testPublishingMultipleFieldsToStreamingServer() throws Exception {
		connectAndSubscribe();
		publish("myTopic", "{\"favouriteFruit\":\"apple\",\"favouriteColour\":\"blue\"}");
		waitForText("key=favouriteFruit value=apple");
		waitForText("key=favouriteColour value=blue");
	}
	
	@Test
	public void testPublishingLotsInQuickSuccessionDoesNotCauseDuplicates() throws Exception {
		connectAndSubscribe();
		publish("myTopic", "{\"favouriteFruit\":\"apple\"}");
		publish("myTopic", "{\"favouriteFruit\":\"apple\"}");
		publish("myTopic", "{\"favouriteFruit\":\"apple\"}");
		publish("myTopic", "{\"favouriteFruit\":\"apple\"}");
		publish("myTopic", "{\"favouriteFruit\":\"apple\"}");
		publish("myTopic", "{\"favouriteFruit\":\"end\"}");
		waitForText("key=favouriteFruit value=end");
		assertEquals(5, StringUtils.countMatches(browser.getBodyText(), "key=favouriteFruit value=apple"));
	}
	
	@Test
	public void testPublishingFieldsWithSpaces() throws Exception {
		connectAndSubscribe();
		publish("myTopic", "{\"favouriteFruits\":\"apple banana pear\"}");
		waitForText("key=favouriteFruits value=apple banana pear");
	}
	
	@Test
	public void testPublishingWithTopic() throws Exception {
		connectAndSubscribe();
		publish("myTopic", "{\"favouriteFruits\":\"apple banana pear\"}");
		waitForText("topic=myTopic");
	}
	
	@Test
	public void testPublishingFieldsWithHtmlAndEqualsCharacters() throws Exception {
		connectAndSubscribe();
		publish("myTopic", "{\"favouriteFruits\":\"apple<b id='boldFruit'>banana</b>pear\"}");
		waitForText("key=favouriteFruits value=applebananapear");
		assertEquals("banana", browser.getText("boldFruit"));
	}
	
	@Test
	public void testPublishingFieldsWithAmpersands() throws Exception {
		connectAndSubscribe();
		publish("myTopic", "{\"favouriteFruits\":\"apple&lt;b&gt;banana&lt;/b&gt;pear\"}");
		waitForText("key=favouriteFruits value=apple<b>banana</b>pear");
	}
	
	private void connectAndSubscribe() {
		open("index.html");
		connect();
		waitForConnection();
		subscribe(EchoPublishListener.PUBLISH_ECHO_TOPIC);
		waitForSubscriptionResponseOk();
	}
}

