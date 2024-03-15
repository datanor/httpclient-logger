/*
 * Copyright 2023 Datanor OÜ.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package ee.datanor.httpclient.logger;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import ee.datanor.httpclient.logger.processor.RequestLogProcessor;
import ee.datanor.httpclient.logger.processor.ResponseLogProcessor;
import ee.datanor.httpclient.logger.processor.request.RequestBodyLogProcessor;
import ee.datanor.httpclient.logger.processor.request.RequestHashLogProcessor;
import ee.datanor.httpclient.logger.processor.request.RequestHeadersLogProcessor;
import ee.datanor.httpclient.logger.processor.request.RequestLineLogProcessor;
import ee.datanor.httpclient.logger.processor.request.RequestTimeLogProcessor;
import ee.datanor.httpclient.logger.processor.response.ResponseBodyLogProcessor;
import ee.datanor.httpclient.logger.processor.response.ResponseHeadersLogProcessor;
import ee.datanor.httpclient.logger.processor.response.ResponseStatusLogProcessor;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@WireMockTest
public class HttpClientLoggerTest {

    private HttpClientLogger httpClientLogger;

    @BeforeEach
    void setUp() {
        List<RequestLogProcessor> requestLogProcessors = List.of(
                new RequestTimeLogProcessor(),
                new RequestHashLogProcessor(),
                new RequestLineLogProcessor(),
                new RequestHeadersLogProcessor(),
                new RequestBodyLogProcessor(2048, Set.of())
        );

        List<ResponseLogProcessor> responseLogProcessors = List.of(
                new ResponseStatusLogProcessor(),
                new ResponseHeadersLogProcessor(),
                new ResponseBodyLogProcessor()
        );
        httpClientLogger = new HttpClientLogger(requestLogProcessors, responseLogProcessors);
    }


    @Test
    void shouldLogRequest(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        // given
        int port = wmRuntimeInfo.getHttpPort();

        // when
        executeDefaultGetWithLogger(port);

        // then
        assertNotNull(MDC.get("HC_REQUEST_HASH"));
        assertNotNull(MDC.get("HC_REQUEST_TIME"));
        assertEquals("GET " + "http://localhost:" + port + "/exec", MDC.get("HC_REQUEST_LINE"));
        assertEquals("-", MDC.get("HC_REQUEST_BODY"));
    }

    @Test
    void shouldLogResponseWithBody(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        // given
        int port = wmRuntimeInfo.getHttpPort();

        // when
        executeDefaultGetWithLogger(port);

        // then
        assertEquals("200", MDC.get("HC_RESPONSE_STATUS"));
        assertEquals("8", MDC.get("HC_RESPONSE_BODY_LENGTH"));
        assertEquals("[\"test\"]", MDC.get("HC_RESPONSE_BODY"));
    }

    private void executeDefaultGetWithLogger(int port) throws IOException {
        stubFor(get("/exec").willReturn(ok().withBody("[\"test\"]").withHeader("Content-Type", "application/json").withHeader("X-Res-Test", "test")));
        HttpGet httpGet = new HttpGet("http://localhost:" + port + "/exec");
        httpGet.addHeader("X-Req-Test", "test");
        executeRequest(httpClientLogger, httpGet);
    }

    private void executeRequest(HttpClientLogger httpClientLogger, ClassicHttpRequest httpRequest) throws IOException {

        try (CloseableHttpClient httpclient = HttpClientBuilder.create()
                .addRequestInterceptorLast((HttpRequest request, EntityDetails entityDetails, HttpContext context) -> {
                    httpClientLogger.cleanup();
                    httpClientLogger.logRequest(request, context);
                }).addResponseInterceptorLast((HttpResponse response, EntityDetails entityDetails, HttpContext context) -> {
                    httpClientLogger.logResponse(response, context);
                }).build()) {

            httpclient.execute(httpRequest, (httpResponse) -> {
                HttpEntity entity = httpResponse.getEntity();
                EntityUtils.consume(entity);
                return null;
            });
        }
    }
}
