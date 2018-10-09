/*
 * Copyright (C) 2011 Thomas Akehurst
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.tomakehurst.wiremock;

import com.github.tomakehurst.wiremock.http.HttpClientFactory;
import com.github.tomakehurst.wiremock.http.RateLimitedChunkedDribbleDelay;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.Options.DYNAMIC_PORT;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class ResponseDribbleRateAcceptanceTest {

    private static final int SOCKET_TIMEOUT_MILLISECONDS = 500;
    private static final int DOUBLE_THE_SOCKET_TIMEOUT = SOCKET_TIMEOUT_MILLISECONDS * 2;

    private static final byte[] BODY_BYTES = "the long sentence being sent".getBytes();

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(DYNAMIC_PORT, DYNAMIC_PORT);

    private HttpClient httpClient;

    @Before
    public void init() {
        httpClient = HttpClientFactory.createClient(SOCKET_TIMEOUT_MILLISECONDS);
    }

    @Test
    public void requestIsSuccessfulButTakesLongerThanSocketTimeoutWhenDribbleIsEnabled() throws Exception {

        int rateBPS = 1024; //2k file

        stubFor(get("/delayedDribble").willReturn(
                ok()
                    .withBodyFile("media.m4s")
                    .withChunkedDribbleRateBytesPerSecond(rateBPS)
                ));

        long start = System.currentTimeMillis();
        HttpResponse response = httpClient.execute(new HttpGet(String.format("http://localhost:%d/delayedDribble", wireMockRule.port())));
        byte[] responseBody = IOUtils.toByteArray(response.getEntity().getContent());
        int duration = (int) (System.currentTimeMillis() - start);

        assertThat(response.getStatusLine().getStatusCode(), is(200));
        //assertThat(responseBody, is(BODY_BYTES));
        assertThat(duration, greaterThanOrEqualTo(2000));
        assertThat((double) duration, closeTo(DOUBLE_THE_SOCKET_TIMEOUT, 100.0));
    }


    @Test
    public void rateLimitsAndChunkDribbles() {
        RateLimitedChunkedDribbleDelay rateLimitedChunkedDribbleDelay = new RateLimitedChunkedDribbleDelay(512);

        //at 512 bytes per second
        // we expect 2048 to take 4 seconds
        // and so 1 second a chunk

        int i = rateLimitedChunkedDribbleDelay.chunkIntervalForBody(2048);
        assertThat(i,is((2048/512)*1000));
    }
}