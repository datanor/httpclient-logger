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

package ee.datanor.httpclient.logger.processor.request;

import ee.datanor.httpclient.logger.masker.BodyMasker;
import ee.datanor.httpclient.logger.processor.LogProcessor;
import ee.datanor.httpclient.logger.processor.RequestLogProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpEntityContainer;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.nio.charset.Charset;
import java.util.Set;

@Slf4j
public class RequestBodyLogProcessor implements RequestLogProcessor {
    public static final String MDC_KEY = "HC_REQUEST_BODY";

    private final int maxLoggedRequestLength;
    private final Set<String> includedRequestBodyMediaSubtypes;
    private final Set<BodyMasker> sensitiveBodyMaskers;

    public RequestBodyLogProcessor(int maxLoggedRequestLength, Set<BodyMasker> sensitiveBodyMaskers) {
        this(maxLoggedRequestLength, sensitiveBodyMaskers, Set.of("json", "xml"));
    }

    public RequestBodyLogProcessor(int maxLoggedRequestLength, Set<BodyMasker> sensitiveBodyMaskers, Set<String> includedResponseBodyMediaSubtypes) {
        this.maxLoggedRequestLength = maxLoggedRequestLength;
        this.includedRequestBodyMediaSubtypes = includedResponseBodyMediaSubtypes;
        this.sensitiveBodyMaskers = sensitiveBodyMaskers;
    }

    @Override
    public void process(HttpRequest httpRequest, HttpContext context) {
        if (!requestBodyMediaSubtypeMatches(httpRequest)) {
            setMDCValue(MDC_KEY, LogProcessor.EMPTY_REPLACEMENT);
            return;
        }
        String requestBody = replaceEmpty(getRequestBody(httpRequest));
        if (requestBody.length() > maxLoggedRequestLength) {
            setMDCValue(MDC_KEY, requestBody.substring(0, maxLoggedRequestLength));
        } else {
            setMDCValue(MDC_KEY, requestBody);
        }
    }

    private String getRequestBody(HttpRequest request) {
        if (HttpEntityContainer.class.isAssignableFrom(request.getClass())) {
            HttpEntityContainer entityContainer = (HttpEntityContainer) request;
            if (entityContainer.getEntity() == null) {
                return null;
            }
            Charset charset = getCharset(entityContainer.getEntity());
            try {
                HttpEntity httpEntity = entityContainer.getEntity();
                if (httpEntity.isRepeatable()) {
                    String body = getEntityStream(((HttpEntityContainer) request).getEntity()).toString(charset);
                    return maskSensitivePatterns(sensitiveBodyMaskers, body);
                }
                return "";
            } catch (Exception e) {
                log.warn("Failed to parse httpclient request - " + e.getMessage(), e);
                return "";
            }
        }
        return null;
    }

    private boolean requestBodyMediaSubtypeMatches(HttpRequest httpRequest) {
        try {
            String mediaType = httpRequest.getHeader("Content-Type").getValue();
            ContentType contentType = ContentType.parseLenient(mediaType);
            String mimeType = contentType.getMimeType();
            if (StringUtils.isEmpty(mediaType)) {
                return false;
            }
            return includedRequestBodyMediaSubtypes.stream()
                    .map(String::toLowerCase)
                    .anyMatch(mimeType::contains);
        } catch (Exception e) {
            return false;
        }
    }
}
