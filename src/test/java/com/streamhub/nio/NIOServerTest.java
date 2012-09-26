package com.streamhub.nio;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NIOServerTest {
	private NIOServer server;

	@Before
	public void setUp() {
		server = new NIOServer(2323);
	}
	
	@After
	public void tearDown() throws IOException {
		if (server != null) {
			server.stop();
		}
	}
	
	@Test
	public void isStartedReturnsCorrectStatus() throws Exception {
		assertFalse(server.isStarted());
		server.start();
		assertTrue(server.isStarted());
		server.stop();
		assertFalse(server.isStarted());
	}
}
