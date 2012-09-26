package com.streamhub.tools;

import junit.framework.Assert;

import com.streamhub.util.Sleep;
import com.thoughtworks.selenium.condition.Condition;

public class ConditionRunner {

	private final int waitForInterval;
	private final int waitForTimeout;

	public ConditionRunner(int waitForInterval, int waitForTimeout) {
		this.waitForInterval = waitForInterval;
		this.waitForTimeout = waitForTimeout;
	}

	public void waitFor(Condition condition) {
		if (condition.isTrue(null)) {
			return;
		}
		
		long endTime = System.currentTimeMillis() + waitForTimeout;
		
		while (! condition.isTrue(null) && System.currentTimeMillis() < endTime) {
			Thread.yield();
			Sleep.millis(waitForInterval);
			
			if (condition.isTrue(null)) {
				return;
			}
		}
		
		Assert.fail("Condition failed to become true after " + waitForTimeout + "ms. " + condition.getMessage());
	}
}
