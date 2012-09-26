package com.streamhub.util;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

import org.junit.Test;

public class WebSocketUtilsTest {
	@Test
	public void extractsDigits() throws Exception {
		String key1 = "18x 6]8vM;54 *(5:  {   U1]8  z [  8";
		String key2 = "1_ tx7X d  <  nw  334J702) 7]o}` 0";
		long key1Digits = 1868545188;
		long key2Digits = 1733470270;

		long result1 = WebSocketUtils.extractDigits(key1);
		long result2 = WebSocketUtils.extractDigits(key2);
		assertEquals(key1Digits, result1);
		assertEquals(key2Digits, result2);
	}

	@Test
	public void extractsDigitsFullRange() throws Exception {
		String key1 = "19x 6]8vM;54 *(5:  {   U1]8  z [  8";
		String key2 = "1_ tx70X d  <  nw  34J702) 7]o}` 0";
		long key1Digits = 1968545188;
		long key2Digits = 1703470270;

		long result1 = WebSocketUtils.extractDigits(key1);
		long result2 = WebSocketUtils.extractDigits(key2);
		assertEquals(key1Digits, result1);
		assertEquals(key2Digits, result2);
	}

	@Test
	public void extractsDigitsDoesNotBlowUpOnTooLargeNumber() throws Exception {
		String key1 = "99999999999999x 6]8vM;54(5:  U1]8 z [  8";
		String key2 = "9999999999999_ tx70X d   34J702)7]o}` 0";

		long result1 = WebSocketUtils.extractDigits(key1);
		long result2 = WebSocketUtils.extractDigits(key2);
		assertEquals(0, result1);
		assertEquals(0, result2);
	}

	@Test
	public void extractsDigitsDoesNotBlowUpOnNullKey() throws Exception {
		String key1 = null;

		long result1 = WebSocketUtils.extractDigits(key1);
		assertEquals(0, result1);
	}

	@Test
	public void extractsDigitsReturnsZeroForNoNumbersAtAll() throws Exception {
		String key1 = "rrx E]FvM;J. *( :  {   U ]   z [   ";
		String key2 = "<_ txrAX d  <  nw    J Oo) .]o}` K";

		long result1 = WebSocketUtils.extractDigits(key1);
		long result2 = WebSocketUtils.extractDigits(key2);
		assertEquals(0, result1);
		assertEquals(0, result2);
	}

	@Test
	public void countsSpaces() throws Exception {
		String key1 = "18x 6]8vM;54 *(5:  {   U1]8  z [  8";
		String key2 = "1_ tx7X d  <  nw  334J702) 7]o}` 0";

		int result1 = WebSocketUtils.countSpaces(key1);
		int result2 = WebSocketUtils.countSpaces(key2);
		assertEquals(12, result1);
		assertEquals(10, result2);
	}

	@Test
	public void countSpacesHandlesNullKey() throws Exception {
		String key = null;
		int result = WebSocketUtils.countSpaces(key);

		assertEquals(0, result);
	}

	@Test
	public void getChallengeBytes() throws Exception {
		byte[] input = WebSocketUtils.hexStringToByteArray(
					"474554202f6163746976656d713f656e"+
					"636f64696e673d626173653634204854"+
					"54502f312e310d0a557067726164653a"+
					"20576562536f636b65740d0a436f6e6e"+
					"656374696f6e3a20557067726164650d"+
					"0a486f73743a2073746f6d702e6b6161"+
					"7a696e672e6d650d0a4f726967696e3a"+
					"20687474703a2f2f6b61617a696e672e"+
					"6d650d0a436f6f6b69653a205f5f7574"+
					"6d613d3134383934383031322e323134"+
					"333131343734312e3132383138313137"+
					"34372e313238313831343331302e3132"+
					"38313831353032302e343b205f5f7574"+
					"6d623d3134383934383031322e312e31"+
					"302e313238313831353032303b205f5f"+
					"75746d633d3134383934383031323b20"+
					"5f5f75746d7a3d313438393438303132"+
					"2e313238313831343331302e332e332e"+
					"75746d6373723d6b61617a696e672e63"+
					"6f6d7c75746d63636e3d287265666572"+
					"72616c297c75746d636d643d72656665"+
					"7272616c7c75746d6363743d2f0d0a53"+
					"65632d576562536f636b65742d4b6579"+
					"313a205a315534383020344c39283337"+
					"31346f0d0a5365632d576562536f636b"+
					"65742d4b6579323a2037203728353839"+
					"3738202020202036220d0a0d0a1844a9"+
					"07f5a9e831");
		String key1 = "Z1U480 4L9(3714o";
		String key2 = "7 7(58978     6\"";
		long num1 = WebSocketUtils.extractDigits(key1);
		long num2 = WebSocketUtils.extractDigits(key2);
		int divisor1 = WebSocketUtils.countSpaces(key1);
		int divisor2 = WebSocketUtils.countSpaces(key2);
		
		int dividedNum1 = (int) (num1 / divisor1);
		int dividedNum2 = (int) (num2 / divisor2);
		byte[] challengeBytes = WebSocketUtils.getChallengeBytes(input);
		
		assertEquals("1844a907f5a9e831", WebSocketUtils.getHexString(challengeBytes));

		byte[] bytes1 = ByteBuffer.allocate(4).putInt(dividedNum1).array();
		byte[] bytes2 = ByteBuffer.allocate(4).putInt(dividedNum2).array();
		
		byte[] bytesOfMessage = ArrayUtils.concatAll(bytes1, bytes2, challengeBytes);
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] thedigest = md.digest(bytesOfMessage);
		
		assertEquals("bdef7b5737d251ac3e37fce32af37646", WebSocketUtils.getHexString(thedigest));
	}
	
	@Test
	public void getChallengeResponse() throws Exception {
		byte[] input = WebSocketUtils.hexStringToByteArray(
				"474554202f6163746976656d713f656e"+
				"636f64696e673d626173653634204854"+
				"54502f312e310d0a557067726164653a"+
				"20576562536f636b65740d0a436f6e6e"+
				"656374696f6e3a20557067726164650d"+
				"0a486f73743a2073746f6d702e6b6161"+
				"7a696e672e6d650d0a4f726967696e3a"+
				"20687474703a2f2f6b61617a696e672e"+
				"6d650d0a436f6f6b69653a205f5f7574"+
				"6d613d3134383934383031322e323134"+
				"333131343734312e3132383138313137"+
				"34372e313238313831343331302e3132"+
				"38313831353032302e343b205f5f7574"+
				"6d 62 3d3134383934383031322e312e31"+
				"302e313238313831353032303b205f5f"+
				"75746d633d3134383934383031323b20"+
				"5f5f75746d7a3d313438393438303132"+
				"2e313238313831343331302e332e332e"+
				"75746d6373723d6b61617a696e672e63"+
				"6f6d7c75746d63636e3d287265666572"+
				"72616c297c75746d636d643d72656665"+
				"7272616c7c75746d6363743d2f0d0a53"+
				"65632d576562536f636b65742d4b6579"+
				"313a205a315534383020344c39283337"+
				"31346f0d0a5365632d576562536f636b"+
				"65742d4b6579323a2037203728353839"+
				"373820202020203  6220d0a 0d0a1844a9"+
		"07f5a9e831");

		String key1 = "Z1U480 4L9(3714o";
		String key2 = "7 7(58978     6\"";
		byte[] challengeBytes = WebSocketUtils.getChallengeBytes(input);
		
		byte[] challengeResponse = WebSocketUtils.getChallengeResponse(key1, key2, challengeBytes);
		assertEquals("bdef7b5737d251ac3e37fce32af37646", WebSocketUtils.getHexString(challengeResponse));
	}
	
	@Test
	public void getChallengeResponseTwo() throws Exception {
		byte[] input = WebSocketUtils.hexStringToByteArray(
				"47 45 54 20 2f 61 63 74  69 76 65 6d 71 3f 65 6e" +
"63 6f 64 69 6e 67 3d 62  61 73 65 36 34 20 48 54" +
"54 50 2f 31 2e 31 0d 0a  55 70 67 72 61 64 65 3a" +
"20 57 65 62 53 6f 63 6b  65 74 0d 0a 43 6f 6e 6e" +
"65 63 74 69 6f 6e 3a 20  55 70 67 72 61 64 65 0d" +
"0a 48 6f 73 74 3a 20 73  74 6f 6d 70 2e 6b 61 61" +
"7a 69 6e 67 2e 6d 65 0d  0a 4f 72 69 67 69 6e 3a" +
"20 68 74 74 70 3a 2f 2f  6b 61 61 7a 69 6e 67 2e" +
"6d 65 0d 0a 53 65 63 2d  57 65 62 53 6f 63 6b 65" +
"74 2d 4b 65 79 31 3a 20  45 20 39 4a 28 38 35 2f" +
"36 20 63 31 39 75 37 49  36 75 62 36 5b 20 56 46" +
"0d 0a 53 65 63 2d 57 65  62 53 6f 63 6b 65 74 2d" +
"4b 65 79 32 3a 20 3c 20  20 32 28 39 35 34 20 49" +
"31 20 30 20 20 61 40 30  31 46 20 20 6f 20 39 20" +
"5e 20 32 20 71 0d 0a 43  6f 6f 6b 69 65 3a 20 5f" +
"5f 75 74 6d 7a 3d 31 34  38 39 34 38 30 31 32 2e" +
"31 32 39 30 38 30 37 34  39 37 2e 31 2e 31 2e 75" +
"74 6d 63 73 72 3d 6b 61  61 7a 69 6e 67 2e 63 6f" +
"6d 7c 75 74 6d 63 63 6e  3d 28 72 65 66 65 72 72" +
"61 6c 29 7c 75 74 6d 63  6d 64 3d 72 65 66 65 72" +
"72 61 6c 7c 75 74 6d 63  63 74 3d 2f 3b 20 5f 5f" +
"75 74 6d 61 3d 31 34 38  39 34 38 30 31 32 2e 31" +
"34 35 36 33 36 30 36 38  33 2e 31 32 39 30 38 30" +
"37 34 39 37 2e 31 32 39  30 38 30 37 34 39 37 2e" +
"31 32 39 30 38 30 37 34  39 37 2e 31 3b 20 5f 5f" +
"75 74 6d 63 3d 31 34 38  39 34 38 30 31 32 3b 20" + 
"5f 5f 75 74 6d 62 3d 31  34 38 39 34 38 30 31 32" +
"2e 31 2e 31 30 2e 31 32  39 30 38 30 37 34 39 37" +
"0d 0a 0d 0a 52 47 18 14  00 36 04 ad");
		
		String key1 = "E 9J(85/6 c19u7I6ub6[ VF";
		String key2 = "<  2(954 I1 0  a@01F  o 9 ^ 2 q";
		byte[] challengeBytes = WebSocketUtils.getChallengeBytes(input);
		
		byte[] challengeResponse = WebSocketUtils.getChallengeResponse(key1, key2, challengeBytes);
		assertEquals("515d556f884b1df500e0d425234c4d9d", WebSocketUtils.getHexString(challengeResponse));
	}
	
	@Test
	public void getChallengeBytesTwo() throws Exception {
		String GET = "GET /activemq?encoding=base64 HTTP/1.1\r\n" +
		"Upgrade: WebSocket\r\n" +
		"Connection: Upgrade\r\n" +
		"Host: stomp.kaazing.me\r\n" +
		"Origin: http://kaazing.me\r\n" +
		"Sec-WebSocket-Key1: 1E706 5 6~ <9 06O^\"0\r\n" +
		"Sec-WebSocket-Key2: 28 8 ?H*7      <4N  54u30\r\n\r\n";
		
		byte[] requestHeaders = GET.getBytes();
		byte[] challenge = WebSocketUtils.hexStringToByteArray("b5a99c0090c186bf");
		byte[] input = ArrayUtils.concat(requestHeaders, challenge);
		System.out.println("//" + WebSocketUtils.getHexString(WebSocketUtils.getChallengeBytes(input)));
	}
	
	@Test
	public void getChallengeResponseThree() throws Exception {
		String GET = "GET /activemq?encoding=base64 HTTP/1.1\r\n" +
		"Upgrade: WebSocket\r\n" +
		"Connection: Upgrade\r\n" +
		"Host: stomp.kaazing.me\r\n" +
		"Origin: http://kaazing.me\r\n" +
		"Sec-WebSocket-Key1: 1E706 5 6~ <9 06O^\"0\r\n" +
		"Sec-WebSocket-Key2: 28 8 ?H*7      <4N  54u30\r\n\r\n";
		
		byte[] requestHeaders = GET.getBytes();
		byte[] challenge = WebSocketUtils.hexStringToByteArray("b5a99c0090c186bf");
		System.out.println("//" + WebSocketUtils.getHexString(challenge));
		byte[] input = ArrayUtils.concat(requestHeaders, challenge);
		
		String key1 = "1E706 5 6~ <9 06O^\"0";
		String key2 = "28 8 ?H*7      <4N  54u30";
		byte[] challengeBytes = WebSocketUtils.getChallengeBytes(input);
		
		byte[] challengeResponse = WebSocketUtils.getChallengeResponse(key1, key2, challengeBytes);
		assertEquals("ce3f7453a539e0754133ce1d1a54e6a1", WebSocketUtils.getHexString(challengeResponse));
	}
	
	
//	@Test
//	public void kaazingTest() throws Exception {
//		String GET = "GET /activemq?encoding=base64 HTTP/1.1\r\n" +
//		"Upgrade: WebSocket\r\n" +
//		"Connection: Upgrade\r\n" +
//		"Host: stomp.kaazing.me\r\n" +
//		"Origin: http://kaazing.me\r\n" +
//		"Sec-WebSocket-Key1: 1E706 5 6~ <9 06O^\"0\r\n" +
//		"Sec-WebSocket-Key2: 28 8 ?H*7      <4N  54u30\r\n\r\n";
//		
//		byte[] requestHeaders = GET.getBytes();
//		byte[] challenge = WebSocketUtils.hexStringToByteArray("b5a99c0090c186bf");
//		byte[] input = ArrayUtils.concat(requestHeaders, challenge);
//		Socket socket = new Socket(InetAddress.getByName("kaazing.me"), 80);
//		socket.setSoTimeout(2000);
//		
//		System.out.println("Writing");
//		socket.getOutputStream().write(input);
//		System.out.println("written");
//		InputStream inputStream = socket.getInputStream();
//		byte[] response = new byte[2568];
//		int count = 0, c;
//
//		System.out.println("Reading");
//		try {
//			while ((c = inputStream.read()) != -1) {
//				System.out.print(".");
//				response[count++] = (byte) c;
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		System.out.println("REsponse as String = [" + new String(response, 0, count) + " response = [" + WebSocketUtils.getHexString(response) + "]");
//	}
//	
//	@Test
//	public void streamhubTest() throws Exception {
//		String GET = "GET /streamhubws/ HTTP/1.1\r\n" +
//		"Upgrade: WebSocket\r\n" +
//		"Connection: Upgrade\r\n" +
//		"Host: localhost\r\n" +
//		"Origin: http://localhost:7979\r\n" +
//		"Sec-WebSocket-Key1: 1E706 5 6~ <9 06O^\"0\r\n" +
//		"Sec-WebSocket-Key2: 28 8 ?H*7      <4N  54u30\r\n\r\n";
//		
//		byte[] requestHeaders = GET.getBytes();
//		byte[] challenge = WebSocketUtils.hexStringToByteArray("b5a99c0090c186bf");
//		byte[] input = ArrayUtils.concat(requestHeaders, challenge);
//		Socket socket = new Socket(InetAddress.getByName("localhost"), 7979);
//		socket.setSoTimeout(2000);
//		
//		System.out.println("Writing");
//		socket.getOutputStream().write(input);
//		System.out.println("written");
//		InputStream inputStream = socket.getInputStream();
//		byte[] response = new byte[2568];
//		int count = 0, c;
//		
//		System.out.println("Reading");
//		try {
//			while ((c = inputStream.read()) != -1) {
//				System.out.print(".");
//				response[count++] = (byte) c;
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		System.out.println("REsponse as String = [" + new String(response, 0, count) + " response = [" + WebSocketUtils.getHexString(response) + "]");
//	}
}
