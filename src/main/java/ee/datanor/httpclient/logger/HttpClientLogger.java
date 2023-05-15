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

import ee.datanor.httpclient.logger.processor.RequestLogProcessor;
import ee.datanor.httpclient.logger.processor.ResponseLogProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.List;

@Slf4j
public class HttpClientLogger {

    private final Logger requestLogger = LoggerFactory.getLogger("httpclient-request-log");
    private final Logger responseLogger = LoggerFactory.getLogger("httpclient-response-log");
    private final List<RequestLogProcessor> requestLogProcessors;
    private final List<ResponseLogProcessor> responseLogProcessors;

    public HttpClientLogger(List<RequestLogProcessor> requestLogProcessors, List<ResponseLogProcessor> responseLogProcessors) {
        this.requestLogProcessors = requestLogProcessors;
        this.responseLogProcessors = responseLogProcessors;
    }

    public void logRequest(HttpRequest httpRequest, HttpContext httpContext) {
        requestLogProcessors.forEach(p -> p.process(httpRequest, httpContext));
        requestLogger.info("Incoming Request {}", MDC.get("HC_REQUEST_LINE"));
    }

    public void logResponse(HttpResponse httpResponse, HttpContext httpContext) {
        responseLogProcessors.forEach(p -> p.process(httpResponse, httpContext));
        responseLogger.info("Outgoing response {}", MDC.get("HC_REQUEST_LINE"));
    }

    public void cleanup() {
        MDC.getCopyOfContextMap().entrySet().stream().filter(e -> e.getKey().startsWith("HC_")).forEach(e -> MDC.remove(e.getKey()));
    }
}
