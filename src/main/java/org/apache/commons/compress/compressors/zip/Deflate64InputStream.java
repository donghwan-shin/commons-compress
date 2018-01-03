/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.commons.compress.compressors.zip;

import java.io.IOException;
import java.io.InputStream;

import static org.apache.commons.compress.utils.IOUtils.closeQuietly;

public class Deflate64InputStream extends InputStream {
    private HuffmanDecoder decoder;
    private long uncompressedSize;
    private long totalRead = 0;

    public Deflate64InputStream(InputStream in, long uncompressedSize) {
        this(new HuffmanDecoder(in), uncompressedSize);
    }

    Deflate64InputStream(HuffmanDecoder decoder, long uncompressedSize) {
        this.uncompressedSize = uncompressedSize;
        this.decoder = decoder;
    }

    @Override
    public int read() throws IOException {
        byte[] b = new byte[1];
        while (true) {
            int r = read(b);
            switch (r) {
                case 1:
                    return b[0] & 0xFF;
                case -1:
                    return -1;
                case 0:
                    continue;
                default:
                    throw new IllegalStateException("Invalid return value from read: " + r);
            }
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = -1;
        if (decoder != null) {
            read = decoder.decode(b, off, len);
            if (read == -1) {
                close();
            } else {
                totalRead += read;
            }
        }
        return read;
    }

    @Override
    public int available() throws IOException {
        long available = 0;
        if (decoder != null) {
            available = uncompressedSize - totalRead;
            if (Long.compare(available, Integer.MAX_VALUE) > 0) {
                available = Integer.MAX_VALUE;
            }
        }
        return (int) available;
    }

    @Override
    public void close() throws IOException {
        closeQuietly(decoder);
        decoder = null;
    }
}
