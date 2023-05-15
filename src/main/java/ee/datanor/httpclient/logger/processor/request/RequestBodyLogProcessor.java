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
import ee.datanor.httpclient.logger.processor.RequestLogProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.HttpEntityContainer;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.nio.charset.Charset;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class RequestBodyLogProcessor implements RequestLogProcessor {
    public static final String MDC_KEY = "HC_REQUEST_BODY";

    private final int maxLoggedRequestLength;
    private final Set<BodyMasker> sensitiveBodyMaskers;

    @Override
    public void process(HttpRequest httpRequest, HttpContext context) {

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
                String body = new String(entityContainer.getEntity().getContent().readAllBytes(), charset);
                return maskSensitivePatterns(sensitiveBodyMaskers, body);
            } catch (Exception e) {
                log.error("Failed to parse httpclient request - " + e.getMessage(), e);
                return getEntityStream(((HttpEntityContainer) request).getEntity()).toString(charset);
            }
        }
        return null;
    }

}
