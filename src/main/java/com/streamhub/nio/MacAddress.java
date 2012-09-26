package com.streamhub.nio;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

class MacAddress {
	static Pattern macPattern = Pattern
			.compile("[0-9a-fA-F]{2}[-:][0-9a-fA-F]{2}[-:][0-9a-fA-F]{2}[-:][0-9a-fA-F]{2}[-:][0 -9a-fA-F]{2}[-:][0-9a-fA-F]{2}");

	private static List<String> getWindowsMACAddresses() {
		try {
			Process conf = Runtime.getRuntime().exec("ipconfig /all");
			BufferedReader input = new BufferedReader(new InputStreamReader(conf.getInputStream()));
			return getMACAddresses(input);
		} catch (Exception ignore) {
		}

		return new ArrayList<String>(1);
	}

	private static List<String> getLinuxMACAddresses() {
		try {
			Process conf = Runtime.getRuntime().exec("/sbin/ifconfig");
			BufferedReader input = new BufferedReader(new InputStreamReader(conf.getInputStream()));
			return getMACAddresses(input);
		} catch (Exception ignore) {
		}

		return new ArrayList<String>(1);
	}

	private static List<String> getHPUXMACAddresses() {
		try {
			Process conf = Runtime.getRuntime().exec("/etc/lanscan");
			BufferedReader input = new BufferedReader(new InputStreamReader(conf.getInputStream()));
			return getMACAddresses(input);
		} catch (Exception ignore) {
		}

		return new ArrayList<String>(1);
	}

	private static List<String> getSolarisMACAddresses() {
		try {
			List<String> rtc = new ArrayList<String>(1);
			Process conf = Runtime.getRuntime().exec("/usr/sbin/arp " + InetAddress.getLocalHost().getHostAddress());
			BufferedReader input = new BufferedReader(new InputStreamReader(conf.getInputStream()));
			rtc.addAll(getMACAddresses(input));
			input.close();
			input = null;
			conf = null;

			// Solaris reports MAC address without first 0, change the pattern
			// at re-test
			macPattern = Pattern
					.compile("[0-9a-fA-F][-:][0-9a-fA-F]{2}[-:][0-9a-fA-F]{2}[-:][0-9a-fA-F]{2}[-:][0 -9a-fA-F]{2}[-:][0-9a-fA-F]{2}");

			conf = Runtime.getRuntime().exec("/usr/sbin/arp " + InetAddress.getLocalHost().getHostAddress());
			input = new BufferedReader(new InputStreamReader(conf.getInputStream()));
			rtc.addAll(getMACAddresses(input));
			return rtc;
		} catch (Exception ignore) {
		} finally {
			// Revert pattern
			macPattern = Pattern
					.compile("[0-9a-fA-F]{2}[-:][0-9a-fA-F]{2}[-:][0-9a-fA-F]{2}[-:][0-9a-fA-F]{2}[-:][0 -9a-fA-F]{2}[-:][0-9a-fA-F]{2}");

		}

		return new ArrayList<String>(1);
	}

	private static List<String> getMACAddresses(BufferedReader input) throws Exception {
		List<String> MACs = new ArrayList<String>(1);
		String theLine;

		while ((theLine = input.readLine()) != null) {
			String[] ss = macPattern.split(theLine);
			for (int p = 0; p < ss.length; p++) {
				String s = theLine.substring(theLine.indexOf(ss[p]) + ss[p].length()).trim();
				if (!s.equals("")) {
					String s1 = s.replaceAll("-", ":");
					String s2 = s1.substring(0, s1.lastIndexOf(":") + 3);
					if (s2.length() == 16 || s2.length() == 17) {
						MACs.add(s2);
					}
				}
			}
		}
		return MACs;
	}
	
	static List<String> getAllMACAddresses() {
		List<String> MACS = new ArrayList<String>();
		MACS.addAll(getWindowsMACAddresses());
		MACS.addAll(getLinuxMACAddresses());
		MACS.addAll(getSolarisMACAddresses());
		MACS.addAll(getHPUXMACAddresses());
		return MACS;
	}
}