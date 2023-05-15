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

import ee.datanor.httpclient.logger.masker.ParameterMasker;
import org.apache.hc.client5.http.RouteInfo;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.util.HashSet;
import java.util.Set;

public class RequestLineLogProcessor extends ParameterMaskingRequestLogProcessor {
    public static final String MDC_KEY = "HC_REQUEST_LINE";

    public RequestLineLogProcessor() {
        this(new HashSet<>());
    }

    public RequestLineLogProcessor(Set<ParameterMasker> maskers) {
        super(maskers);
    }

    @Override
    public void process(HttpRequest httpRequest, HttpContext context) {
        setMDCValue(MDC_KEY, replaceEmpty(getRequestLine(httpRequest, context)));
    }

    private String getRequestLine(HttpRequest request, HttpContext context) {
        String httpHost = getHttpHost(context);
        String uri = request.getRequestUri();
        if (httpHost != null && uri.startsWith(httpHost)) {
            uri = uri.replaceFirst(httpHost, "");
        }
        String url = maskSensitiveParameters(httpHost + uri);
        return String.format("%s %s", request.getMethod(), url);
    }

    private String getHttpHost(HttpContext context) {
        if (context != null) {
            if (HttpClientContext.class.isAssignableFrom(context.getClass())) {
                return ((HttpClientContext) context).getHttpRoute().getTargetHost().toString();
            }

            if (context.getAttribute(HttpClientContext.HTTP_ROUTE) instanceof RouteInfo) {
                return ((RouteInfo) context.getAttribute(HttpClientContext.HTTP_ROUTE)).getTargetHost().toString();
            }
        }

        return null;
    }
}
