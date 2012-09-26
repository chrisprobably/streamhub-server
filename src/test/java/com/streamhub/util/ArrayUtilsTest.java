package com.streamhub.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ArrayUtilsTest {
	@Test
	public void testConcattingTwoArrays() {
		byte[] one = { 1,2,3 };
		byte[] two = { 4,5,6 };
		byte[] combined = ArrayUtils.concat(one, two);
		assertEquals(1, combined[0]);
		assertEquals(2, combined[1]);
		assertEquals(3, combined[2]);
		assertEquals(4, combined[3]);
		assertEquals(5, combined[4]);
		assertEquals(6, combined[5]);
		assertEquals(6, combined.length);
	}
	
	@Test
	public void testConcattingMultipleArrays() {
		byte[] one = { 1,2,3 };
		byte[] two = { 4,5,6 };
		byte[] three = { 7,8,9 };
		byte[] combined = ArrayUtils.concatAll(one, two, three);
		assertEquals(1, combined[0]);
		assertEquals(2, combined[1]);
		assertEquals(3, combined[2]);
		assertEquals(4, combined[3]);
		assertEquals(5, combined[4]);
		assertEquals(6, combined[5]);
		assertEquals(7, combined[6]);
		assertEquals(8, combined[7]);
		assertEquals(9, combined[8]);
		assertEquals(9, combined.length);
	}
	
	@Test
	public void testConcattingDifferentSizedArrays() {
		byte[] one = { 1 };
		byte[] two = { 2,3,4 };
		byte[] three = { 5,6 };
		byte[] combined = ArrayUtils.concatAll(one, two, three);
		assertEquals(1, combined[0]);
		assertEquals(2, combined[1]);
		assertEquals(3, combined[2]);
		assertEquals(4, combined[3]);
		assertEquals(5, combined[4]);
		assertEquals(6, combined[5]);
		assertEquals(6, combined.length);
	}
	
	@Test
	public void testConcattingInLoop() {
		byte[] bytes = new byte[0];
		byte[] one = { 1 };
		byte[] two = { 2,3,4 };
		byte[] three = { 5,6 };
		byte[][] byteArray = new byte[][] { one, two, three }; 
		
		for (byte[] array : byteArray) {
			byte[] cometBytes = array;
			bytes = ArrayUtils.concat(bytes, cometBytes);
		}
		assertEquals(1, bytes[0]);
		assertEquals(2, bytes[1]);
		assertEquals(3, bytes[2]);
		assertEquals(4, bytes[3]);
		assertEquals(5, bytes[4]);
		assertEquals(6, bytes[5]);
		assertEquals(6, bytes.length);
	}
	
	@Test
	public void indexOfDoesNotBlowUpWhenPatternCrossesEndOfInput() {
		byte[] input = { 1,2,3,4,5,6,7,8,9,0 };
		byte[] pattern = { 9,0,1 };
		assertEquals(-1, ArrayUtils.indexOf(pattern, input));
	}
	
	@Test
	public void indexOfAtEndOfArray() {
		byte[] input = { 0,1,2,3,4,5,6,7,8,9 };
		byte[] pattern = { 8,9 };
		assertEquals(8, ArrayUtils.indexOf(pattern, input));
	}
	
	@Test
	public void indexOfAtStartOfArray() {
		byte[] input = { 0,1,2,3,4,5,6,7,8,9 };
		byte[] pattern = { 0,1,2 };
		assertEquals(0, ArrayUtils.indexOf(pattern, input));
	}
}
