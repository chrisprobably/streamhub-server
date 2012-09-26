package com.streamhub.e2e.browser;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.streamhub.e2e.DomainsTest;
import com.streamhub.e2e.LoggerTest;
import com.streamhub.e2e.UnSubscribeTest;

@RunWith(Suite.class)
@SuiteClasses({
	DomainsTest.class,
	UnSubscribeTest.class,
	LoggerTest.class
})
public class SelectCommonSuite {

}
