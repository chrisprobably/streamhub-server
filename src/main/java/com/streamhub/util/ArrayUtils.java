package com.streamhub.util;


public class ArrayUtils {
	public static byte[] concat(byte[] A, byte[] B) {
		byte[] C = new byte[A.length + B.length];
		System.arraycopy(A, 0, C, 0, A.length);
		System.arraycopy(B, 0, C, A.length, B.length);
		return C;
	}
	
	public static int indexOf(byte[] pattern, byte[] input) {
		outer:
		for (int i = 0; i < input.length; i++) {
			if (input[i] == pattern[0]) {
				for (int j = 1; j < pattern.length; j++) {
					if (i+j+1 > input.length || input[i+j] != pattern[j]) {
						continue outer;
					}
				}
				return i;
			}
		}
		
		return -1;
	}

	public static byte[] concatAll(byte[] first, byte[]... rest) {
		int totalLength = first.length;
		for (byte[] array : rest) {
			totalLength += array.length;
		}
		byte[] result = ArrayUtils.copyOf(first, totalLength);
		int offset = first.length;
		for (byte[] array : rest) {
			System.arraycopy(array, 0, result, offset, array.length);
			offset += array.length;
		}
		return result;
	}
	
	public static byte[] copyOf(byte[] original, int length) {
		byte[] copy = new byte[length];
		System.arraycopy(original, 0, copy, 0, Math.min(length, original.length));
		return copy;
	}
}
