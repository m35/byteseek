package net.byteseek.util.bytes;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;

public class BytePermutationIteratorTest {

	@SuppressWarnings("unused")
	@Test
	public final void testBytePermutationIterator() {
		try {
			BytePermutationIterator it = new BytePermutationIterator(null);
			fail("Expected an Illegal Argument Exception for a null list");
		} catch (IllegalArgumentException expected) {}
		
		List<byte[]> byteArrayList = new ArrayList<byte[]>();
		try {
			BytePermutationIterator it = new BytePermutationIterator(null);
			fail("Expected an Illegal Argument Exception for an empty list");
		} catch (IllegalArgumentException expected) {}

		byteArrayList.add(null);
		try {
			BytePermutationIterator it = new BytePermutationIterator(null);
			fail("Expected an Illegal Argument Exception for a null byte array");
		} catch (IllegalArgumentException expected) {}
		
		byteArrayList.clear();
		byteArrayList.add(new byte[0]);
		try {
			BytePermutationIterator it = new BytePermutationIterator(null);
			fail("Expected an Illegal Argument Exception for an empty byte array");
		} catch (IllegalArgumentException expected) {}
		
		// A list with one array in it should construct properly:
		byteArrayList.clear();
		byteArrayList.add(new byte[] { (byte) 0x01, (byte) 0xff, (byte) 0x7f});
		BytePermutationIterator it = new BytePermutationIterator(byteArrayList);
		
		// As should two elements of with one have a length of one.
		byteArrayList.add(new byte[] { (byte) 0xC1});
		it = new BytePermutationIterator(byteArrayList);
		
		// And more elements:
		byteArrayList.add(new byte[] { (byte) 0xC1, (byte) 0x3f, (byte) 0x08, (byte) 0x5a});
		it = new BytePermutationIterator(byteArrayList);
		
		byteArrayList.add(new byte[] { (byte) 0x00});
		it = new BytePermutationIterator(byteArrayList);
	}

	@Test
	public final void testHasNext() {
		List<byte[]> contents = new ArrayList<byte[]>();
		contents.add(new byte[] {(byte) 0xb3});
		
		BytePermutationIterator it = new BytePermutationIterator(contents);
		assertTrue("Even the smallest list containing a single byte byte array has next", it.hasNext());
		assertTrue("A subsequent call to hasNext does not advance the state", it.hasNext());
		byte[] result = it.next();
		assertEquals("Result is one byte in length", 1, result.length);
		assertEquals("Result contains the byte b3", (byte) 0xb3, result[0]);
		assertFalse("The permutation has no more members", it.hasNext());
		
		try {
			it.next();
			fail("Expected a NoSuchElementException");
		} catch (NoSuchElementException expected) {};
	}

	@Test
	public final void testNext() {
		List<byte[]> byteArrayList = new ArrayList<byte[]>();
		byteArrayList.add(new byte[] {0x00});
		BytePermutationIterator it = new BytePermutationIterator(byteArrayList);
		
		byte[] expectedResult = new byte[] {(byte) 0x00};
		assertArrayEquals("Result is correct", expectedResult, it.next());
		assertFalse("The permutation has no more members", it.hasNext());
		
		byteArrayList.add(new byte[] { (byte) 0x01, (byte) 0xff, (byte) 0x7f});
		it = new BytePermutationIterator(byteArrayList);
		expectedResult = new byte[] {(byte) 0x00, (byte) 0x01};
		assertArrayEquals("1 permutation is correct", expectedResult, it.next());

		expectedResult = new byte[] {(byte) 0x00, (byte) 0xff};
		assertArrayEquals("2 permutation is correct", expectedResult, it.next());
		
		expectedResult = new byte[] {(byte) 0x00, (byte) 0x7f};
		assertArrayEquals("3 permutation is correct", expectedResult, it.next());
		assertFalse("The permutation has no more members", it.hasNext());
		
		byteArrayList.add(new byte[] { (byte) 0xC1, (byte) 0xee});
		it = new BytePermutationIterator(byteArrayList);
		expectedResult = new byte[] {(byte) 0x00, (byte) 0x01, (byte) 0xC1};
		assertArrayEquals("4 permutation is correct", expectedResult, it.next());

		expectedResult = new byte[] {(byte) 0x00, (byte) 0x01, (byte) 0xee};
		assertArrayEquals("5 permutation is correct", expectedResult, it.next());
				
		expectedResult = new byte[] {(byte) 0x00, (byte) 0xff, (byte) 0xc1};
		assertArrayEquals("6 permutation is correct", expectedResult, it.next());

		expectedResult = new byte[] {(byte) 0x00, (byte) 0xff, (byte) 0xee};
		assertArrayEquals("7 permutation is correct", expectedResult, it.next());

		expectedResult = new byte[] {(byte) 0x00, (byte) 0x7f, (byte) 0xc1};
		assertArrayEquals("8 permutation is correct", expectedResult, it.next());

		expectedResult = new byte[] {(byte) 0x00, (byte) 0x7f, (byte) 0xee};
		assertArrayEquals("9 permutation is correct", expectedResult, it.next());
		assertFalse("The permutation has no more members", it.hasNext());
	}


	@Test
	public final void testRemove() {
		List<byte[]> byteArrayList = new ArrayList<byte[]>();
		byteArrayList.add(new byte[] {0x00});
		
		try {
			BytePermutationIterator it = new BytePermutationIterator(byteArrayList);
			it.remove();
			fail("Expected an UnsupportedOperationException");
		} catch (UnsupportedOperationException expected) {}
		
		byteArrayList.add(new byte[] { (byte) 0x01, (byte) 0xff, (byte) 0x7f});
		BytePermutationIterator it = new BytePermutationIterator(byteArrayList);
		it.next();
		try {
			it.remove();
			fail("Expected an UnsupportedOperationException");
		} catch (UnsupportedOperationException expected) {}
		
		// As should two elements of with one have a length of one.
		byteArrayList.add(new byte[] { (byte) 0xC1});
		it = new BytePermutationIterator(byteArrayList);
		it.next();
		try {
			it.remove();
			fail("Expected an UnsupportedOperationException");
		} catch (UnsupportedOperationException expected) {}
	}

}
