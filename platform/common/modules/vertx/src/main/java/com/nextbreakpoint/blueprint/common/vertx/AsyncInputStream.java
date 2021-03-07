package com.nextbreakpoint.blueprint.common.vertx;

import io.vertx.core.streams.ReadStream;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.concurrent.Executor;

public class AsyncInputStream implements ReadStream<Buffer> {
    public static final int STATUS_PAUSED = 0, STATUS_ACTIVE = 1, STATUS_CLOSED = 2;
    private static final int DEFAULT_CHUNK_SIZE = 8192;
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    private final Vertx vertx;
    private final Executor executor;
    private final PushbackInputStream in;
    private final int chunkSize;

    private Handler<Buffer> handler;
    private Handler<Void> closeHandler;
    private Handler<Throwable> failureHandler;

    private int status = STATUS_ACTIVE;
    private int offset;

    public AsyncInputStream(Vertx vertx, Executor executor, InputStream in) {
        this(vertx, executor, in, DEFAULT_CHUNK_SIZE);
    }

    public AsyncInputStream(Vertx vertx, Executor executor, InputStream in, int chunkSize) {
        if (in == null) {
            throw new NullPointerException("in");
        }

        if (vertx == null) {
            throw new NullPointerException("vertx");
        }

        this.vertx = vertx;

        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize: " + chunkSize + " (expected: a positive integer)");
        }

        if (in instanceof PushbackInputStream) {
            this.in = (PushbackInputStream) in;
        } else {
            this.in = new PushbackInputStream(in);
        }

        this.chunkSize = chunkSize;
        this.executor = executor;
    }

    @Override
    public AsyncInputStream pause() {
        if (status == STATUS_ACTIVE) {
            status = STATUS_PAUSED;
        }
        return this;
    }

    @Override
    public AsyncInputStream resume() {
        switch (status) {
            case STATUS_CLOSED:
                throw new IllegalStateException();
            case STATUS_PAUSED:
                status = STATUS_ACTIVE;
                doRead();
        }
        return this;
    }

    @Override
    public AsyncInputStream fetch(long bytes) {
        return this;
    }

    @Override
    public AsyncInputStream exceptionHandler(Handler<Throwable> handler) {
        this.failureHandler = handler;
        return this;
    }

    @Override
    public ReadStream<Buffer> endHandler(Handler<Void> endHandler) {
        this.closeHandler = endHandler;
        return this;
    }

    @Override
    public AsyncInputStream handler(Handler<Buffer> handler) {
        if (handler == null) {
            throw new UnsupportedOperationException("not implemented");
        }
        this.handler = handler;
        doRead();
        return this;
    }

    public boolean isClosed() {
        return status == STATUS_CLOSED;
    }

    public int getOffset() {
        return offset;
    }

    private void doRead() {
        if (status == STATUS_ACTIVE) {
            final Handler<Buffer> dataHandler = this.handler;
            final Handler<Void> closeHandler = this.closeHandler;
            executor.execute(() -> {
                try {
                    final byte[] bytes = readChunk();
                    if (bytes == null || bytes.length == 0) {
                        status = STATUS_CLOSED;
                        vertx.runOnContext(event -> {
                            if (closeHandler != null) {
                                closeHandler.handle(null);
                            }
                        });
                    } else {
                        vertx.runOnContext(event -> {
                            dataHandler.handle(Buffer.buffer(bytes));
                            doRead();
                        });
                    }
                } catch (final Exception e) {
                    status = STATUS_CLOSED;
                    closeQuietly();
                    vertx.runOnContext(event -> {
                        if (failureHandler != null) {
                            failureHandler.handle(e);
                        }
                    });
                }
            });
        }
    }

    private void closeQuietly() {
        if (in != null) {
            try {
                in.close();
            } catch (IOException ignored) {
            }
        }
    }

    private boolean isEndOfInput() throws Exception {
        int b = in.read();
        if (b < 0) {
            return true;
        } else {
            in.unread(b);
            return false;
        }
    }

    private byte[] readChunk() throws Exception {
        if (isEndOfInput()) {
            return EMPTY_BYTE_ARRAY;
        }

        try {
            final int chunkSize = computeChunkSize(in.available());
            byte[] buffer = new byte[chunkSize];
            int length = in.read(buffer);
            if (length <= 0) {
                return null;
            }
            offset += buffer.length;
            return buffer;
        } catch (IOException e) {
            closeQuietly();
            return null;
        }
    }

    private int computeChunkSize(int availableBytes) {
        return (availableBytes <= 0) ? this.chunkSize : Math.min(this.chunkSize, availableBytes);
    }
}