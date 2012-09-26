package com.streamhub.demo;

import com.streamhub.StreamingServerTestCase;
import com.streamhub.demo.DemoStreamer;
import com.streamhub.demo.NIOMockClientSimulator;
import com.streamhub.nio.NIOServer;
import com.streamhub.nio.SecureNIOServer;
import com.streamhub.tools.TestSSLContext;
import com.streamhub.util.Sleep;

public class StoppingStreamingServerTest extends StreamingServerTestCase {
	public void testAllHttpThreadsAreStopped() throws Exception {
		int countBeforeStarting = Thread.activeCount();
		Thread[] beforeStopActiveThreads = new Thread[countBeforeStarting];
		Thread.enumerate(beforeStopActiveThreads);
		for (Thread thread : beforeStopActiveThreads) {
			System.out.println("Threads active before test: " + thread.getThreadGroup().getName() + "->" + thread.getName());
		}
		NIOServer streamingServer = new NIOServer(6767);
		streamingServer.start();
		DemoStreamer streamer = new DemoStreamer(streamingServer, 1);
		NIOMockClientSimulator mockClientSimulator = new NIOMockClientSimulator(50, 1);
		mockClientSimulator.start();
		Sleep.seconds(5);
		streamingServer.stop();
		streamer.stop();
		mockClientSimulator.stop();
		Sleep.seconds(5);
		int countAfterStopped = Thread.activeCount();
		Thread[] afterStopActiveThreads = new Thread[countAfterStopped];
		Thread.enumerate(afterStopActiveThreads);
		for (Thread thread : afterStopActiveThreads) {
			System.out.println("Thread still active after test: " + thread.getThreadGroup().getName() + "->" + thread.getName());
		}
		
		assertEquals(countBeforeStarting, countAfterStopped);
	}
	
	public void testAllHttpsThreadsAreStopped() throws Exception {
		int countBeforeStarting = Thread.activeCount();
		Thread[] beforeStopActiveThreads = new Thread[countBeforeStarting];
		Thread.enumerate(beforeStopActiveThreads);
		for (Thread thread : beforeStopActiveThreads) {
			System.out.println("Threads active before test: " + thread.getThreadGroup().getName() + "->" + thread.getName());
		}
		NIOServer streamingServer = new SecureNIOServer(6768, TestSSLContext.newInstance());
		streamingServer.start();
		DemoStreamer streamer = new DemoStreamer(streamingServer, 1);
		NIOMockClientSimulator mockClientSimulator = new NIOMockClientSimulator(50, 1);
		mockClientSimulator.start();
		Sleep.seconds(5);
		streamingServer.stop();
		streamer.stop();
		mockClientSimulator.stop();
		Sleep.seconds(5);
		int countAfterStopped = Thread.activeCount();
		Thread[] afterStopActiveThreads = new Thread[countAfterStopped];
		Thread.enumerate(afterStopActiveThreads);
		for (Thread thread : afterStopActiveThreads) {
			System.out.println("Thread still active after test: " + thread.getThreadGroup().getName() + "->" + thread.getName());
		}
		
		assertEquals(countBeforeStarting, countAfterStopped);
	}
}
