package com.streamhub.nio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.apache.commons.lang.StringUtils;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.streamhub.Connection;
import com.streamhub.DirectHandler;
import com.streamhub.handler.Handler;
import com.streamhub.request.Request;

@RunWith(JMock.class)
public class NIOConnectionTest {
	private Mockery context;
	private SocketChannel channel;
	private NIOConnection connection;
	
	@Before
	public void setUp() {
		context = new Mockery() {{ setImposteriser(ClassImposteriser.INSTANCE); }};
		channel = context.mock(SocketChannel.class);
		connection = new NIOConnection(channel);
	}
	
	@Test
	public void peeksAtNBytesOfAString() throws Exception {
		final ByteBuffer readBuffer = readBufferContaining(DirectHandler.MAGIC_DIRECT_CONNECTION_STRING);
		context.checking(new Expectations() {{
			exactly(2).of(channel).read(readBuffer);
				will(onConsecutiveCalls(returnValue(DirectHandler.MAGIC_DIRECT_CONNECTION_STRING.length()), returnValue(-1)));
		}});
		fireReadableEvent();
		String peek = new String(connection.peekBytes());
		assertEquals(DirectHandler.DIRECT_MESSAGE_SEPARATOR, peek);
	}
	
	@Test
	public void readingString() throws Exception {
		final ByteBuffer readBuffer = readBufferContaining(DirectHandler.MAGIC_DIRECT_CONNECTION_STRING);
		context.checking(new Expectations() {{
			exactly(2).of(channel).read(readBuffer);
				will(onConsecutiveCalls(returnValue(DirectHandler.MAGIC_DIRECT_CONNECTION_STRING.length()), returnValue(-1)));
		}});
		fireReadableEvent();
		assertEquals(DirectHandler.MAGIC_DIRECT_CONNECTION_STRING, new String(connection.readBytes()));
	}
	
	@Test
	public void readingReallyBigString() throws Exception {
		final String reallyBigString = "@@" + StringUtils.repeat("a", 2048) + "@@";
		final ByteBuffer readBuffer = readBufferContaining(reallyBigString);
		context.checking(new Expectations() {{
			exactly(2).of(channel).read(readBuffer);
				will(onConsecutiveCalls(returnValue(reallyBigString.length()), returnValue(-1)));
		}});
		fireReadableEvent();
		assertEquals(reallyBigString, new String(connection.readBytes()));
	}
	
	@Test
	public void readingEmptyBuffer() throws Exception {
		final ByteBuffer readBuffer = ByteBuffer.allocateDirect(9);
		connection.setReadBufferForTesting(readBuffer);
		context.checking(new Expectations() {{
			one(channel).read(readBuffer);
				will(returnValue(-1));
		}});
		fireReadableEvent();
		assertEquals("", new String(connection.readBytes()));
	}
	
	@Test
	public void peekingAtEmptyBuffer() throws Exception {
		final ByteBuffer readBuffer = ByteBuffer.allocateDirect(9);
		connection.setReadBufferForTesting(readBuffer);
		context.checking(new Expectations() {{
			one(channel).read(readBuffer);
				will(returnValue(-1));
		}});
		fireReadableEvent();
		assertEquals("", new String(connection.peekBytes()));
	}
	
	@Test
	public void peekingDoesNotAffectSubsequentRead() throws Exception {
		final ByteBuffer readBuffer = readBufferContaining(DirectHandler.MAGIC_DIRECT_CONNECTION_STRING);
		context.checking(new Expectations() {{
			exactly(2).of(channel).read(readBuffer);
				will(onConsecutiveCalls(returnValue(DirectHandler.MAGIC_DIRECT_CONNECTION_STRING.length()), returnValue(-1)));
		}});
		fireReadableEvent();
		String peek = new String(connection.peekBytes());
		assertEquals(DirectHandler.DIRECT_MESSAGE_SEPARATOR, peek);
		String read = new String(connection.readBytes());
		assertEquals(DirectHandler.MAGIC_DIRECT_CONNECTION_STRING, read);
	}
	
	@Test
	public void writingNullDoesNotWriteToChannel() throws Exception {
		context.checking(new Expectations() {{
			never(channel).write(with(any(ByteBuffer.class)));
		}});
		
		connection.close();
	}
	
	@Test
	public void gettingRequest() throws Exception {
		final ByteBuffer readBuffer = readBufferContaining("something");
		context.checking(new Expectations() {{
			exactly(2).of(channel).read(readBuffer);
				will(onConsecutiveCalls(returnValue(9), returnValue(-1)));
		}});
		connection.onReadableEvent(new Handler() {
			public void handle(Connection connection) {
			}});
		
		Request request = connection.getRequest();
		assertNotNull(request);
	}
	
	@Test
	public void closing() {
		connection.close();
	}

	private ByteBuffer readBufferContaining(String string) {
		final ByteBuffer readBuffer = ByteBuffer.allocateDirect(string.length());
		readBuffer.put(string.getBytes());
		connection.setReadBufferForTesting(readBuffer);
		return readBuffer;
	}
	
	private void fireReadableEvent() {
		connection.onReadableEvent(new Handler() {
			public void handle(Connection connection) {
			}
		});
	}
}
