package com.streamhub.nio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.nio.channels.SocketChannel;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.streamhub.Connection;

@RunWith(JMock.class)
public class NIOConnectionFactoryTest {
	private Mockery context;
	private SocketChannel channel;
	
	@Before
	public void setUp() {
		context = new Mockery() {{ setImposteriser(ClassImposteriser.INSTANCE); }};
		channel = context.mock(SocketChannel.class);
	}
	
	@Test
	public void CreatesNIOConnection() throws Exception {
		Connection connection = new NIOConnectionFactory().createConnection(channel);
		assertNotNull(connection);
		assertEquals(channel, connection.getChannel());
	}
}
