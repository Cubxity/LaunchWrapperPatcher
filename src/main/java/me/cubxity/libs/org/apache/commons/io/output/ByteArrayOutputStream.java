/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.cubxity.libs.org.apache.commons.io.output;

import me.cubxity.libs.org.apache.commons.io.input.ClosedInputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ByteArrayOutputStream extends OutputStream {
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    private final List<byte[]> buffers = new ArrayList<>();
    private int currentBufferIndex;
    private int filledBufferSum;
    private byte[] currentBuffer;
    private int count;

    public ByteArrayOutputStream() {
        this(1024);
    }

    private ByteArrayOutputStream(int size) {
        if (size < 0) {
            throw new IllegalArgumentException(
                    "Negative initial size: " + size);
        }
        synchronized (this) {
            needNewBuffer(size);
        }
    }

    public static InputStream toBufferedInputStream(InputStream input)
            throws IOException {
        @SuppressWarnings("resource")
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(input);
        return output.toBufferedInputStream();
    }

    private void needNewBuffer(int newcount) {
        if (currentBufferIndex < buffers.size() - 1) {
            //Recycling old buffer
            filledBufferSum += currentBuffer.length;

            currentBufferIndex++;
            currentBuffer = buffers.get(currentBufferIndex);
        } else {
            //Creating new buffer
            int newBufferSize;
            if (currentBuffer == null) {
                newBufferSize = newcount;
                filledBufferSum = 0;
            } else {
                newBufferSize = Math.max(
                        currentBuffer.length << 1,
                        newcount - filledBufferSum);
                filledBufferSum += currentBuffer.length;
            }

            currentBufferIndex++;
            currentBuffer = new byte[newBufferSize];
            buffers.add(currentBuffer);
        }
    }

    @Override
    public synchronized void write(int b) {
        int inBufferPos = count - filledBufferSum;
        if (inBufferPos == currentBuffer.length) {
            needNewBuffer(count + 1);
            inBufferPos = 0;
        }
        currentBuffer[inBufferPos] = (byte) b;
        count++;
    }

    public synchronized int write(InputStream in) throws IOException {
        int readCount = 0;
        int inBufferPos = count - filledBufferSum;
        int n = in.read(currentBuffer, inBufferPos, currentBuffer.length - inBufferPos);
        while (n != -1) {
            readCount += n;
            inBufferPos += n;
            count += n;
            if (inBufferPos == currentBuffer.length) {
                needNewBuffer(currentBuffer.length);
                inBufferPos = 0;
            }
            n = in.read(currentBuffer, inBufferPos, currentBuffer.length - inBufferPos);
        }
        return readCount;
    }

    public synchronized int size() {
        return count;
    }

    @Override
    public void write(byte[] b, int off, int len) {
        if ((off < 0)
                || (off > b.length)
                || (len < 0)
                || ((off + len) > b.length)
                || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        synchronized (this) {
            int newcount = count + len;
            int remaining = len;
            int inBufferPos = count - filledBufferSum;
            while (remaining > 0) {
                int part = Math.min(remaining, currentBuffer.length - inBufferPos);
                System.arraycopy(b, off + len - remaining, currentBuffer, inBufferPos, part);
                remaining -= part;
                if (remaining > 0) {
                    needNewBuffer(newcount);
                    inBufferPos = 0;
                }
            }
            count = newcount;
        }
    }

    @Override
    public void close() {
        //nop
    }

    private InputStream toBufferedInputStream() {
        int remaining = count;
        if (remaining == 0) {
            return new ClosedInputStream();
        }
        List<ByteArrayInputStream> list = new ArrayList<ByteArrayInputStream>(buffers.size());
        for (byte[] buf : buffers) {
            int c = Math.min(buf.length, remaining);
            list.add(new ByteArrayInputStream(buf, 0, c));
            remaining -= c;
            if (remaining == 0) {
                break;
            }
        }
        return new SequenceInputStream(Collections.enumeration(list));
    }

    public synchronized byte[] toByteArray() {
        int remaining = count;
        if (remaining == 0) {
            return EMPTY_BYTE_ARRAY;
        }
        byte[] newbuf = new byte[remaining];
        int pos = 0;
        for (byte[] buf : buffers) {
            int c = Math.min(buf.length, remaining);
            System.arraycopy(buf, 0, newbuf, pos, c);
            pos += c;
            remaining -= c;
            if (remaining == 0) {
                break;
            }
        }
        return newbuf;
    }

    @Override
    public String toString() {
        return new String(toByteArray());
    }

    public String toString(String enc) throws UnsupportedEncodingException {
        return new String(toByteArray(), enc);
    }
}
