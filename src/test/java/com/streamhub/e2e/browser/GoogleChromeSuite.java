package com.streamhub.e2e.browser;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.streamhub.e2e.DomainsTest;
import com.streamhub.e2e.EndToEndFramework;
import com.streamhub.util.Browser;

@RunWith(Suite.class)
@SuiteClasses({ 
	DomainsTest.class
// Comet tests not working in chrome with selenium:
// 1. Get a 404 if you use localhost or 127.0.0.1 as selenium start page.
// 2. Setting document.domain to anything other than '127.0.0.1' in streamhub.js.connect() seems to
//    make every selenium command blow up with an unexpected exception.
//	StockDemoTest.class, 
//	ConnectionTest.class
})
public class GoogleChromeSuite {
	private static final EndToEndFramework FRAMEWORK = EndToEndFramework.getInstance();

	@BeforeClass
	public static void startEndToEndFramework() throws Exception {
		FRAMEWORK.start(Browser.CHROME, false);
	}

	@AfterClass
	public static void stopEndToEndFramework() throws Exception {
		FRAMEWORK.stop();
	}
}
