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

import ee.datanor.httpclient.logger.processor.RequestLogProcessor;
import ee.datanor.httpclient.logger.util.HashUtil;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.protocol.HttpContext;

public class RequestHashLogProcessor implements RequestLogProcessor {
    public static final String MDC_KEY = "HC_REQUEST_HASH";
    public static final int DEFAULT_HASH_LENGTH = 8;

    private int hashLength;

    public RequestHashLogProcessor() {
        this(DEFAULT_HASH_LENGTH);
    }

    public RequestHashLogProcessor(int hashLength) {
        this.hashLength = hashLength;
    }

    @Override
    public void process(HttpRequest httpRequest, HttpContext httpContext) {
        setMDCValue(MDC_KEY, HashUtil.generateHash(hashLength));
    }
}
