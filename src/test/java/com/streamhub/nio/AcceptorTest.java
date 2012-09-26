package com.streamhub.nio;

import java.nio.channels.SocketChannel;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class AcceptorTest {
	private Mockery context;
	private ConnectionFactory factory;
	private SocketChannel channel;
	
	@Before
	public void setUp() {
		context = new Mockery() {{ setImposteriser(ClassImposteriser.INSTANCE); }};
		factory = context.mock(ConnectionFactory.class);
		channel = context.mock(SocketChannel.class);
	}
	
	@Test
	public void ConnectionFactoryIsUsedToCreateConnections() throws Exception {
		Acceptor acceptor = new Acceptor(3432, null, factory);
		
		context.checking(new Expectations() {{ 
			one(factory).createConnection(channel);
		}});
		
		acceptor.createConnection(channel);
	}
}
