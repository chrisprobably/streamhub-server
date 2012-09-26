package com.streamhub.nio;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

class CommandLine {
	public static Options parse(String[] args) {
		Options options = new Options();
		
		try {
			options = parseArgs(args);
		} catch (Exception e) {
			System.out.println(usage());
			System.exit(-1);
		}
		
		return options;
	}

	private static Options parseArgs(String[] args) throws UnknownHostException, MalformedURLException {
		Options options = new Options();
		
		if (args.length == 0) {
			System.out.println(usage());
			System.exit(-1);
		}
		
		if (args.length >= 1 && args[0].contains(":")) {
			String[] split = args[0].split(":");
			String ipOrHost = split[0]; 
			int port = Integer.parseInt(split[1]);
			options.serverAddress = new InetSocketAddress(InetAddress.getByName(ipOrHost), port);
		} else if (args.length >= 1) {
			int port = Integer.parseInt(args[0]);
			options.serverAddress = new InetSocketAddress(port);
		}
		
		if (args.length >= 2 && args[1].contains(":")) {
			String[] split = args[1].split(":");
			String ipOrHost = split[0]; 
			int port = Integer.parseInt(split[1]);
			options.streamingAdapterAddress = new InetSocketAddress(InetAddress.getByName(ipOrHost), port);
		} else if (args.length >= 2) {
			int port = Integer.parseInt(args[1]);
			options.streamingAdapterAddress = new InetSocketAddress(port);
		}
		
		if (args.length == 3) {
			options.loggingUrl = new URL(args[2]);
		}
		
		return options;
	}
	
	public static String usage() {
		return "Usage: NIOServer <serverAddress> [streamingAdapterAddress] [<loggingUrl>]\n" + 
				"Examples:\n" + 
				"\tNIOServer 80\n" +
				"\tNIOServer 10.4.33.1:80\n" +
				"\tNIOServer 7979 6969\n" +
				"\tNIOServer 192.168.1.6:7979 192.168.1.5:6969\n" +
				"\tNIOServer 80 8484 http://data.intra/conf/prod.log4j.xml\n" +
				"\tNIOServer 10.4.33.1:80 10.4.33.2:8484 file:///etc/streamhub/prod/log4j.xml\n";
	}
	
	public static class Options {
		public InetSocketAddress serverAddress;
		public InetSocketAddress streamingAdapterAddress;
		public URL loggingUrl;
	}
}
