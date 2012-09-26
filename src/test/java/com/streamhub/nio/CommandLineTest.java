package com.streamhub.nio;

import static org.junit.Assert.assertEquals;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;

import org.junit.Test;

import com.streamhub.nio.CommandLine.Options;

public class CommandLineTest {
	@Test
	public void testParsesSinglePortArg() throws Exception {
		String[] args = { "7979" };
		Options options = CommandLine.parse(args);
		assertEquals(new InetSocketAddress(7979), options.serverAddress);
	}
	
	@Test
	public void testParsesSingleAddressAndPortArg() throws Exception {
		String[] args = { "192.34.54.2:7979" };
		Options options = CommandLine.parse(args);
		assertEquals(new InetSocketAddress(InetAddress.getByName("192.34.54.2"), 7979), options.serverAddress);
	}
	
	@Test
	public void testParsesDoublePortArg() throws Exception {
		String[] args = { "7979", "6969" };
		Options options = CommandLine.parse(args);
		assertEquals(new InetSocketAddress(7979), options.serverAddress);
		assertEquals(new InetSocketAddress(6969), options.streamingAdapterAddress);
	}
	
	@Test
	public void testParsesDoubleAddressArg() throws Exception {
		String[] args = { "192.34.54.2:7979", "10.3.0.2:6969" };
		Options options = CommandLine.parse(args);
		assertEquals(new InetSocketAddress(InetAddress.getByName("192.34.54.2"), 7979), options.serverAddress);
		assertEquals(new InetSocketAddress(InetAddress.getByName("10.3.0.2"), 6969), options.streamingAdapterAddress);
	}
	
	@Test
	public void testParsesDoubleAddressArgWithPairOfUrls() throws Exception {
		String[] args = { "192.34.54.2:7979", "10.3.0.2:6969", "http://localhost:232/conf/log4j.xml" };
		Options options = CommandLine.parse(args);
		assertEquals(new InetSocketAddress(InetAddress.getByName("192.34.54.2"), 7979), options.serverAddress);
		assertEquals(new InetSocketAddress(InetAddress.getByName("10.3.0.2"), 6969), options.streamingAdapterAddress);
		assertEquals(new URL("http://localhost:232/conf/log4j.xml"), options.loggingUrl);
	}
	
	@Test
	public void testPrintUsage() throws Exception {
		System.out.println(CommandLine.usage());
	}
}
