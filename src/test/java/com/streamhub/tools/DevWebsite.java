package com.streamhub.tools;

import com.streamhub.util.StaticHttpServer;

public class DevWebsite {
	public static void main(String[] args) throws Exception {
		StaticHttpServer httpServer = new StaticHttpServer(81, "../../../streamhub-website/src/main/website", "/");
		httpServer.start();
		System.out.println("StreamHub website available at http://localhost/");
		System.out.println("Press any key to stop");
		System.in.read();
		httpServer.stop();
		System.out.println("Stopped");
	}
}
