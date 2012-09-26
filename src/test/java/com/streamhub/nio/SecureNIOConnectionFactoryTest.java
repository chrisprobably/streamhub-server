package com.streamhub.nio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.nio.channels.SocketChannel;

import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;

import com.streamhub.Connection;

public class SecureNIOConnectionFactoryTest {
	private Mockery context;
	private SocketChannel channel;
	
	@Before
	public void setUp() {
		context = new Mockery() {{ setImposteriser(ClassImposteriser.INSTANCE); }};
		channel = context.mock(SocketChannel.class);
	}
	
	@Test
	public void CreatesSecureNIOConnection() throws Exception {
		Connection connection = new SecureNIOConnectionFactory().createConnection(channel);
		assertNotNull(connection);
		assertEquals(channel, connection.getChannel());
		assertEquals(SecureNIOConnection.class, connection.getClass());
	}
}
