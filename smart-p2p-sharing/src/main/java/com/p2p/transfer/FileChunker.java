package com.p2p.transfer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class FileChunker implements Iterable<FileChunker.Chunk>, AutoCloseable {

    public static class Chunk {
        private final int index;
        private final int totalChunks;
        private final byte[] data;

        public Chunk(int index, int totalChunks, byte[] data) {
            this.index = index;
            this.totalChunks = totalChunks;
            this.data = data;
        }

        public int getIndex() {
            return index;
        }

        public int getTotalChunks() {
            return totalChunks;
        }

        public byte[] getData() {
            return data;
        }
    }

    private final Path file;
    private final int chunkSize;
    private final long fileSize;
    private final int totalChunks;
    private final InputStream inputStream;

    public FileChunker(Path file, int chunkSize) throws IOException {
        this.file = file;
        this.chunkSize = chunkSize;
        this.fileSize = Files.size(file);
        this.totalChunks = (int) Math.ceil((double) fileSize / chunkSize);
        this.inputStream = Files.newInputStream(file);
    }

    public long getFileSize() {
        return fileSize;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    @Override
    public Iterator<Chunk> iterator() {
        return new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < totalChunks;
            }

            @Override
            public Chunk next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                try {
                    int remaining = (int) Math.min(chunkSize, fileSize - (long) index * chunkSize);
                    byte[] buffer = inputStream.readNBytes(remaining);
                    if (buffer.length != remaining) {
                        throw new IOException("Unexpected end of file while chunking " + file);
                    }
                    Chunk chunk = new Chunk(index, totalChunks, buffer);
                    index++;
                    return chunk;
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to read chunk " + index + " from file " + file, e);
                }
            }
        };
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}
