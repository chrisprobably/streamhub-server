package com.streamhub.demo;

import java.io.IOException;

import com.streamhub.api.PushServer;
import com.streamhub.nio.NIOServer;

class DemoNIOServer {
	public static void main(String[] args) throws IOException {
		PushServer streamingServer = new NIOServer(6767);
		DemoStreamer demoStreamer = new DemoStreamer(streamingServer, 1);
		streamingServer.start();
		System.out.println("DemoStreamingServer started at http://localhost:6767/");
		System.out.println("Press any key to stop...");
		System.in.read();
		System.out.println("Stopping...");
		streamingServer.stop();
		demoStreamer.stop();
	}
}
