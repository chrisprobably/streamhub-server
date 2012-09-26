package com.streamhub.e2e;

import org.junit.Test;

public class DomainsTest extends EndToEndTestCase {
	@Test
	public void testExtractingDomainFromSubdomainsAndIpAddresses() throws Exception {
		open("domainTests.html");
		browser.click("runDomainTests");
		waitForText("Tests finished");
		waitForText("DomainUtils.extractDomain(\"www.stream-hub.com\")=stream-hub.com");
		waitForText("DomainUtils.extractDomain(\"stream-hub.com\")=stream-hub.com");
		waitForText("DomainUtils.extractDomain(\"192.168.1.68\")=192.168.1.68");
		waitForText("DomainUtils.extractDomain(\"en.p1.stream-hub.com\")=stream-hub.com");
		waitForText("DomainUtils.extractDomain(\"123.com\")=123.com");
		waitForText("DomainUtils.extractDomain(\"00.123.com\")=123.com");
		waitForText("DomainUtils.extractDomain(\"www.streamhub.co.uk\")=streamhub.co.uk");
		waitForText("DomainUtils.extractDomain(\"push1.dmz.gov.uk\")=dmz.gov.uk");
		waitForText("DomainUtils.extractDomain(\"push1.dmz.net\")=dmz.net");
		waitForText("DomainUtils.extractDomain(\"push1.dmz.us\")=dmz.us");
		waitForText("DomainUtils.extractDomain(\"push1.dmz.org.uk\")=dmz.org.uk");
		waitForText("DomainUtils.extractDomain(\"localhost\")=localhost");
		waitForText("DomainUtils.extractDomain(\"localhost.localadmin\")=localhost.localadmin");
		waitForText("DomainUtils.extractDomain(\"2001:db8::1428:57ab\")=2001:db8::1428:57ab");
		waitForText("DomainUtils.extractDomain(\"1234.biz\")=1234.biz");
	}
}

