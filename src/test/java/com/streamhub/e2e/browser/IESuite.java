package com.streamhub.e2e.browser;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.streamhub.e2e.EndToEndFramework;
import com.streamhub.util.Browser;

@RunWith(Suite.class)
@SuiteClasses({
	ReconnectionCommonSuite.class,
	SelectCommonSuite.class,
	CommonSuite.class
})
public class IESuite {
	private static final EndToEndFramework FRAMEWORK = EndToEndFramework.getInstance();

	@BeforeClass
	public static void startEndToEndFramework() throws Exception {
		FRAMEWORK.start(Browser.IE, false);
	}

	@AfterClass
	public static void stopEndToEndFramework() throws Exception {
		FRAMEWORK.stop();
	}
}
