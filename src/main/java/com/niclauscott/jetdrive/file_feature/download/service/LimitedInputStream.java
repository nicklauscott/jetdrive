package com.niclauscott.jetdrive.file_feature.download.service;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.io.InputStream;

@AllArgsConstructor
public class LimitedInputStream extends InputStream {

    private final InputStream inputStream;
    private long remaining;

    @Override
    public int read() throws IOException {
        if (remaining <= 0) return -1;
        int result = inputStream.read();
        if (result != -1) remaining--;
        return result;
    }

    @Override
    public int read(@NotNull byte[] b, int off, int len) throws IOException {
        if (remaining <= 0) return -1;
        len = (int) Math.min(len, remaining);
        int byteRead = inputStream.read(b, off, len);
        if (byteRead != -1) remaining -= byteRead;
        return byteRead;
    }

    @Override
    public int available() throws IOException {
        int available = inputStream.available();
        return (int) Math.max(available, remaining);
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

    @Override
    public long skip(long n) throws IOException {
        long skipped = inputStream.skip(Math.min(n, remaining));
        remaining -= skipped;
        return skipped;
    }
}
