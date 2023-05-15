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

package ee.datanor.httpclient.logger.processor;

import ee.datanor.httpclient.logger.masker.BodyMasker;
import ee.datanor.httpclient.logger.util.EscapeUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Set;

public interface LogProcessor {
    String EMPTY_REPLACEMENT = "-";

    default void setMDCValue(String attribute, Object value) {
        MDC.put(attribute, EscapeUtil.escape(replaceEmpty(value)));
    }

    default String replaceEmpty(Object value) {
        String parsedValue = "null".equalsIgnoreCase("" + value) ? EMPTY_REPLACEMENT : "" + value;
        return StringUtils.firstNonBlank(parsedValue, EMPTY_REPLACEMENT);
    }

    default Charset getCharset(HttpEntity httpEntity) {
        ContentType contentType = ContentType.parseLenient(httpEntity.getContentType());
        if (contentType == null) {
            return Charset.defaultCharset();
        }
        Charset charset = contentType.getCharset();
        return charset == null ? Charset.defaultCharset() : charset;
    }

    default ByteArrayOutputStream getEntityStream(HttpEntity httpEntity) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            httpEntity.writeTo(out);
            return out;
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass().getName()).warn(e.getMessage(), e);
            return new ByteArrayOutputStream();
        }
    }

    default String maskSensitivePatterns(Set<BodyMasker> maskers, String content) {
        if (content == null) {
            return null;
        }
        String result = content;
        for (BodyMasker masker : maskers) {
            result = masker.mask(result);
        }
        return result;
    }
}
