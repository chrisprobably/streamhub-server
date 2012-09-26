package com.streamhub.util;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

public class ChannelUtils {
    private static Charset charset = Charset.forName("US-ASCII");
	
	public static void write(SocketChannel channel, String data) throws IOException {
		ByteBuffer encodedData = charset.newEncoder().encode(CharBuffer.wrap(data));
		channel.write(encodedData);
	}
	
	public static String read(SocketChannel channel, ByteBuffer buffer) throws IOException {
		String string = "";
		int bytesRead = 0;
		
		if ((bytesRead = channel.read(buffer)) > 0) {
			buffer.flip();
			byte[] bytes = new byte[bytesRead];
			try {
				buffer.get(bytes);
			} catch (BufferUnderflowException e) {
			}
			string += new String(bytes, 0, bytes.length);
			buffer.clear();
		}
		
		return string;
	}

}
