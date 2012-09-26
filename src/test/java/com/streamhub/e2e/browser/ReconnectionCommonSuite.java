package com.streamhub.e2e.browser;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.streamhub.e2e.ConnectionListenerTest;
import com.streamhub.e2e.FailoverTest;
import com.streamhub.e2e.ReconnectionTest;
import com.streamhub.e2e.ReconnectionToDifferentServerTest;

@RunWith(Suite.class)
@SuiteClasses({
	ConnectionListenerTest.class,
	ReconnectionToDifferentServerTest.class,
	FailoverTest.class,
	ReconnectionTest.class
})
public class ReconnectionCommonSuite {

}
