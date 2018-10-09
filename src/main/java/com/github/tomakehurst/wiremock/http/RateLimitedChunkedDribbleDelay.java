package com.github.tomakehurst.wiremock.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tomakehurst.wiremock.servlet.BodyChunker;

public class RateLimitedChunkedDribbleDelay extends ChunkedDribbleDelay {
    private static final int DEFAULT_CHUNK_SIZE_BYTES = 512;
    private final int rateBytesPerSecond;

    public int getRateBytesPerSecond() {
        return rateBytesPerSecond;
    }

    public RateLimitedChunkedDribbleDelay(@JsonProperty("rateBytesPerSecond") int rateBytesPerSecond) {
        super(-1, -1);
        this.rateBytesPerSecond = rateBytesPerSecond;
    }

    @Override
    public int chunkIntervalForBody(int lengthInBytes) {
        //assuming 4kb chunks
        //chunk time is
        // 4k / 4kbps = 1
        // 8k / 4 =2
        int numberOfChunks = lengthInBytes / DEFAULT_CHUNK_SIZE_BYTES;
        int totalTimeToDeliverBodyInSeconds = lengthInBytes / rateBytesPerSecond;
        int chunkIntervalInMilliSeconds = (totalTimeToDeliverBodyInSeconds * 1000) / numberOfChunks;
        return chunkIntervalInMilliSeconds;
    }

    @Override
    public byte[][] chunkBody(byte[] body) {
        int numberOfChunks = (body.length / DEFAULT_CHUNK_SIZE_BYTES) + 1;
        return BodyChunker.chunkBody(body, numberOfChunks);
    }
}
