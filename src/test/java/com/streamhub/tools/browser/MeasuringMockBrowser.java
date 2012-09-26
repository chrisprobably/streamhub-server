package com.streamhub.tools.browser;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import com.streamhub.reader.MeasuringHttpStreamReader;
import com.streamhub.util.Random;
import com.streamhub.util.Sleep;

public class MeasuringMockBrowser extends MockBrowser {
	public MeasuringMockBrowser() throws MalformedURLException {
		super();
	}

	public MeasuringMockBrowser(URL streamingServerUrl, int i) {
		super(streamingServerUrl, i);
	}

	@Override
	protected void startResponseThread() {
		try {
			InputStream responseStream = openResponseIFrame();
			responseIFrameReader = new MeasuringHttpStreamReader(responseStream, uid);
			threadPool.execute(responseIFrameReader);
			isResponseThreadStarted = true;
		} catch (IOException e) {
			Sleep.seconds((int) Random.numberBetween(1, 10));
			startResponseThread();
		}
	}

	public double getBytesPerSecond() {
		if (responseIFrameReader == null) {
			return 0;
		}
		
		return ((MeasuringHttpStreamReader) responseIFrameReader).getBytesPerSecond();
	}

	public double getUpdatesPerSecond() {
		if (responseIFrameReader == null) {
			return 0;
		}
		
		return ((MeasuringHttpStreamReader) responseIFrameReader).getUpdatesPerSecond();
	}
}
