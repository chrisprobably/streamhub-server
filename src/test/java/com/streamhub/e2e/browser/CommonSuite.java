package com.streamhub.e2e.browser;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.streamhub.e2e.ConnectionTest;
import com.streamhub.e2e.MultipleSubscriptionTest;
import com.streamhub.e2e.PollingConnectionTest;
import com.streamhub.e2e.PublishTest;
import com.streamhub.e2e.StockDemoTest;
import com.streamhub.e2e.UniqueIdStockDemoTest;

@RunWith(Suite.class)
@SuiteClasses({
	PublishTest.class,
	MultipleSubscriptionTest.class,
	UniqueIdStockDemoTest.class,
	StockDemoTest.class, 
	ConnectionTest.class,
	PollingConnectionTest.class
})
public class CommonSuite {

}
