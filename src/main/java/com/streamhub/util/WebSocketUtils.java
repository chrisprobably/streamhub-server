package com.streamhub.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;


public class WebSocketUtils {
	private static final Logger log = Logger.getLogger(WebSocketUtils.class);
	private static final byte[] START_BYTES = new byte[] { 0x00 };
	private static final byte[] END_BYTES = new byte[] { (byte)0xff };	
	
	public static byte[] createMessage(String message) {
		byte[] messageBytes = message.getBytes();
		return ArrayUtils.concatAll(START_BYTES, messageBytes, END_BYTES);
	}
	
	public static long extractDigits(String key) {
		if (key == null) {
			return 0;
		}
		
		String justNumeric = "";
		for (int i = 0; i < key.length(); i++) {
			char c = key.charAt(i);
			if (c >= '0' && c <= '9') {
				justNumeric += c;
			}
		}
		
		long digits = 0;
		
		try {
			digits = Long.parseLong(justNumeric);
		} catch (Exception e) {
			log.debug("Could not parse secure web socket challenge", e);
		}
		
		return digits;
	}

	public static int countSpaces(String key) {
		int count = 0;
		
		if (key == null) {
			return 0;
		}
		 
		for (int i = 0; i < key.length(); i++) {
			char c = key.charAt(i);
			if (c == ' ') {
				count++;
			}
		}
		
		return count;
	}

	public static byte[] getChallengeBytes(byte[] inputAsByteArray) {
		byte[] bytes = new byte[8];
		int index = 0;

		for (int i = inputAsByteArray.length - 8; i < inputAsByteArray.length; i++) {
			bytes[index++] = inputAsByteArray[i];
		}
		
		return bytes;
	}

	public static byte[] getChallengeResponse(String key1, String key2, byte[] challengeBytes) {
		log.debug("Sec-key1 [" + key1 + "] hex [" + getHexString(key1.getBytes()) + "]");
		log.debug("Sec-key2 [" + key2 + "] hex [" + getHexString(key2.getBytes()) + "]");
		log.debug("Sec-key3 hex [" + getHexString(challengeBytes) + "]");
		long num1 = WebSocketUtils.extractDigits(key1);
		long num2 = WebSocketUtils.extractDigits(key2);
		int divisor1 = WebSocketUtils.countSpaces(key1);
		int divisor2 = WebSocketUtils.countSpaces(key2);
		Long dividedNum1 = (num1 / divisor1);
		Long dividedNum2 = (num2 / divisor2);
		BigInteger sec1 = new BigInteger(dividedNum1.toString());
		BigInteger sec2 = new BigInteger(dividedNum2.toString());
		byte[] response = new byte[8];
		byte[] l128Bit = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        byte[] lTmp;

        lTmp = sec1.toByteArray();
		int lIdx = lTmp.length;
		int lCnt = 0;
		while(lIdx > 0 && lCnt < 4) {
			lIdx--;
			lCnt++;
            l128Bit[4 - lCnt] = lTmp[lIdx];
        }

        lTmp = sec2.toByteArray();
		lIdx = lTmp.length;
		lCnt = 0;
		while(lIdx > 0 && lCnt < 4) {
			lIdx--;
			lCnt++;
            l128Bit[8 - lCnt] = lTmp[lIdx];
        }

        lTmp = challengeBytes;
		System.arraycopy(challengeBytes, 0, l128Bit, 8, 8);
		
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			log.error("MD5 algorithm not found", e);
		}
		response = md.digest(l128Bit);
		log.debug("Challenge response hex [" + getHexString(response) + "]");
		return response;
	}

	public static byte[] hexStringToByteArray(String s) {
		s = s.replaceAll("\\s", "");
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

	public static String getHexString(byte[] b) {
		String result = "";
		for (int i = 0; i < b.length; i++) {
			result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
		}
		return result;
	}
}
