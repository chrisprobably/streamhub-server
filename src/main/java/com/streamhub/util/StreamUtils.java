package com.streamhub.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StreamUtils {
	private static final byte[] CRLF_BYTES = "\r\n\r\n".getBytes();
	private static final Pattern CONTENT_LENGTH_PATTERN = Pattern.compile(".*?Content-Length:\\s*(\\d+).*", Pattern.DOTALL | Pattern.MULTILINE);
	private static final Pattern CHUNKED_PATTERN = Pattern.compile(".*?Transfer-Encoding:\\s*(chunked).*", Pattern.DOTALL | Pattern.MULTILINE);

	public static String toString(InputStream inputStream) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		StringBuilder request = new StringBuilder();
		int ch;

		while ((ch = reader.read()) != -1 && reader.ready()) {
			request.append((char) ch);
		}

		if (ch != -1) {
			request.append((char) ch);
		}

		return request.toString();
	}

	public static byte[] toBytes(File file) throws IOException {
		byte buf[] = new byte[(int) file.length()];
		FileInputStream fis = new FileInputStream(file);
		fis.read(buf);
		fis.close();
		return buf;
	}

	public static byte[] nioToBytes(InetSocketAddress socketAddress, String input) throws IOException {
		SocketChannel channel = null;
		byte[] allBytes = new byte[0];
		boolean hasFoundFirstChunkSize = false; 
		int nextBytesIndex = -1;
		List<Marker> markers = new ArrayList<Marker>();

		try {
			ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
			channel = SocketChannel.open();
			channel.configureBlocking(false);
			channel.connect(socketAddress);
			Selector selector = Selector.open();
			channel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ);

			while (selector.select() > 0) {
				Set<SelectionKey> readyKeys = selector.selectedKeys();
				Iterator<SelectionKey> readyItor = readyKeys.iterator();

				while (readyItor.hasNext()) {
					SelectionKey key = readyItor.next();
					readyItor.remove();
					SocketChannel keyChannel = (SocketChannel) key.channel();

					if (key.isConnectable()) {
						if (keyChannel.isConnectionPending()) {
							keyChannel.finishConnect();
							channel.register(selector, SelectionKey.OP_READ);
						}
						ByteBuffer encoded = ByteBuffer.wrap(input.getBytes());
						keyChannel.write(encoded);
					} else if (key.isReadable()) {
						int bytesRead;
						while ((bytesRead = keyChannel.read(buffer)) > 0) {
							byte[] justRead = new byte[bytesRead];
							buffer.flip();
							buffer.get(justRead, 0, bytesRead);
							allBytes = ArrayUtils.concat(allBytes, justRead);
							buffer.clear();
							int indexOfEndOfHeaders = -1;

							if ((indexOfEndOfHeaders = ArrayUtils.indexOf(CRLF_BYTES, allBytes)) > -1) {
								String headers = new String(allBytes, 0, indexOfEndOfHeaders);
								Matcher contentLengthMatcher = CONTENT_LENGTH_PATTERN.matcher(headers);
								if (contentLengthMatcher.matches()) {
									String match = contentLengthMatcher.group(1);
									int contentLength = Integer.parseInt(match);
									int totalLength = indexOfEndOfHeaders + contentLength + 4;
									if (allBytes.length == totalLength) {
										SocketUtils.closeQuietly(channel);
										SocketUtils.closeQuietly(selector);
										return allBytes;
									}
								} else {
									Matcher chunkedMatcher = CHUNKED_PATTERN.matcher(headers);
									if (chunkedMatcher.matches()) {
										int indexOfFirstChunkSize = indexOfEndOfHeaders + 4;
										if (! hasFoundFirstChunkSize && allBytes.length > indexOfFirstChunkSize + 4) {
											String firstChunkSize = new String(allBytes, indexOfFirstChunkSize, 4);
											int firstChunkSizeBytes = Integer.parseInt(firstChunkSize, 16);
											hasFoundFirstChunkSize = true;
											nextBytesIndex = indexOfFirstChunkSize + 8 + firstChunkSizeBytes;
											markers.add(new Marker(indexOfFirstChunkSize, indexOfFirstChunkSize + 6));
										} else if (hasFoundFirstChunkSize) {
											while(allBytes.length > nextBytesIndex + 4) {
												if ("0".equals(new String(allBytes, nextBytesIndex, 1))) {
													int resultLength = nextBytesIndex-2;
													byte[] result = new byte[resultLength];
													System.arraycopy(allBytes, 0, result, 0, resultLength);
													removeMarkers(allBytes, markers);
													SocketUtils.closeQuietly(channel);
													SocketUtils.closeQuietly(selector);
													return result;
												} else {
													markers.add(new Marker(nextBytesIndex, nextBytesIndex + 8));
													String nextChunkSize = new String(allBytes, nextBytesIndex, 4);
													int nextChunkSizeBytes = Integer.parseInt(nextChunkSize, 16);
													nextBytesIndex = nextBytesIndex + 8 + nextChunkSizeBytes;
												}
											}
										}
									}
								}
							}
						}

						if (bytesRead == -1) {
							SocketUtils.closeQuietly(channel);
							SocketUtils.closeQuietly(selector);
							return allBytes;
						}
					}
				}
			}
		} finally {
			SocketUtils.closeQuietly(channel);
		}
		return allBytes;
	}

	private static void removeMarkers(byte[] allBytes, List<Marker> markers) {
		for (int i = 0; i < markers.size(); i++) {
			Marker marker = markers.get(i);
			System.arraycopy(allBytes, marker.end - (i * 8), allBytes, marker.start - (i * 8), allBytes.length - marker.end);
		}
	}

	public static String bufferedToString(InputStream inputStream) throws IOException {
		StringBuilder builder = new StringBuilder();
		byte[] buffer = new byte[1024];
		long timeout = System.currentTimeMillis() + 5000;
		outer: while (System.currentTimeMillis() < timeout) {
			while (inputStream.available() > 0 && System.currentTimeMillis() < timeout) {
				int ch = inputStream.read(buffer, 0, 1024);

				if (isEndOfStream(ch)) {
					break outer;
				}

				builder.append(new String(buffer, 0, ch));
			}
			Sleep.millis(100);
			if (inputStream.available() <= 0) {
				break outer;
			}
		}
		return builder.toString();
	}

	public static void closeQuietly(OutputStream outputStream) {
		try {
			if (outputStream != null) {
				outputStream.close();
			}
		} catch (IOException ignored) {
		}
	}

	public static void closeQuietly(InputStream inputStream) {
		try {
			if (inputStream != null) {
				inputStream.close();
			}
		} catch (IOException ignored) {
		}
	}

	public static void write(OutputStream outputStream, String data) throws IOException {
		outputStream.write(data.getBytes());
		outputStream.flush();
	}

	public static String peek(InputStream inputStream, int numberOfBytes) throws IOException {
		StringBuilder string = new StringBuilder();
		inputStream.mark(Integer.MAX_VALUE);
		int bytesRead = 0;
		int ch;

		while ((ch = inputStream.read()) != -1 && bytesRead++ < numberOfBytes) {
			string.append((char) ch);
		}

		inputStream.reset();
		return string.toString();
	}

	private static boolean isEndOfStream(int i) {
		return i < 0;
	}
	
	private static class Marker {
		public final int start;
		public final int end;

		public Marker(int start, int end) {
			this.start = start;
			this.end = end;
		}
	}
}
