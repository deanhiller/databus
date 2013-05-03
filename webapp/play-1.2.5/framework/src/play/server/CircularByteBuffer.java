package play.server;
//package etch.util;

/* $Id$
*
* Copyright 2007-2008 Cisco Systems Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/


import java.nio.BufferOverflowException;
import java.nio.InvalidMarkException;

/**
* Description of CircularByteBuffer.
*/
public class CircularByteBuffer extends ByteBuffer implements CircBuffer
{
	/**
	 * Constructs the CircularByteBuffer.
	 *
	 * @param size
	 */
	public CircularByteBuffer( int size )
	{
		this.size = size;
		buf = new byte[size];
	}
	
	private final int size;
	
	private final byte[] buf;
	
	private int length;
	
	private int nextGet;
	
	private int nextPut;

	private int mark;
	
	@Override
	public int size()
	{
		return size;
	}
	
	@Override
	public int length()
	{
		return length;
	}
	
	@Override
	public void clear()
	{
		length = 0;
		nextGet = 0;
		nextPut = 0;
	}

	@Override
	public byte get()
	{
		if (isEmpty())
			throw new IllegalStateException("buffer is empty");
		
		length--;
		byte b = buf[nextGet++];
		if (nextGet >= size)
			nextGet = 0;
		return b;
	}

	@Override
	public void put( byte b ) throws BufferOverflowException
	{
		if (isFull())
			throw new BufferOverflowException();
		
		length++;
		buf[nextPut++] = b;
		if (nextPut >= size)
			nextPut = 0;
	}

	@Override
	public void mark() {
		mark = nextGet;		
	}

	public int nextGetPosition() {
		return nextGet;
	}
	
	public void nextGetPosition(int pos) {
		this.nextGet = pos;
		calculateLength();
		if(nextGet >= buf.length)
			throw new IllegalStateException("bug, nextget is same or greater than buflength="+buf.length+" nextget="+nextGet);
	}
	
	private void calculateLength() {
		if(nextGet <= nextPut) {
			length = nextPut - nextGet;
		} else {
			int toEnd = buf.length - nextGet;
			length = nextPut + toEnd;
		}
		
		if(length >= buf.length)
			throw new IllegalStateException("bug, nextGet="+nextGet+" nextPut="+nextPut+" size="+buf.length+" length was calculated to be too long");
	}

	@Override
	public void reset() {
		if (mark < 0)
		    throw new InvalidMarkException();
		nextGet = mark;
		calculateLength();
		if(nextGet >= buf.length)
			throw new IllegalStateException("bug, nextget is same or greater than buflength="+buf.length+" nextget="+nextGet);
	}

	public void discardBytes(int bytes) {
		if(bytes > length)
			throw new IllegalStateException("length="+length+" so numbytes of="+bytes+" cannot be discarded");
		else if(bytes < 0)
			throw new IllegalArgumentException("cannot discard negative byte count");
		
		if(nextPut > nextGet || buf.length-nextGet > bytes) {
			nextGet+=bytes;
			if(nextGet >= buf.length)
				throw new IllegalStateException("bug, nextget is same or greater than buflength="+buf.length+" nextget="+nextGet+" nextPut="+nextPut+" bytes="+bytes);
		} else if(buf.length-nextPut > bytes){
			int temp = buf.length-nextGet;
			int bytesToRemoveLeft = bytes - temp;
			nextGet = bytesToRemoveLeft;
			if(nextGet >= buf.length)
				throw new IllegalStateException("bug, nextget is same or greater than buflength="+buf.length+" nextget="+nextGet+" nextPut="+nextPut+" bytes="+bytes);
		}
		calculateLength();
	}
}
