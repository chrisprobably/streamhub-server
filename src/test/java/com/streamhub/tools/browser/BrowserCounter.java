package com.streamhub.tools.browser;

import java.util.List;


public class BrowserCounter {
	public void printCount(String message, List<MockBrowser> browsers, CountStrategy countStrategy) {
		int count = 0;
		
		for (MockBrowser browser : browsers) {
			if (countStrategy.increaseCount(browser)) {
				count++;
			}
		}

		System.out.println(count + "/" + browsers.size() + " " + message);
	}
}
