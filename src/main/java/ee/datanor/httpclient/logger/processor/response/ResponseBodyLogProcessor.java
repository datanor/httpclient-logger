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

package ee.datanor.httpclient.logger.processor.response;

import ee.datanor.httpclient.logger.masker.BodyMasker;
import ee.datanor.httpclient.logger.processor.ResponseLogProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.entity.GZIPInputStreamFactory;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpEntityContainer;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.BasicHttpEntity;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

@Slf4j

public class ResponseBodyLogProcessor implements ResponseLogProcessor {
    private static final String LENGTH_MDC_KEY = "HC_RESPONSE_BODY_LENGTH";
    private static final String BODY_MDC_KEY = "HC_RESPONSE_BODY";
    private static final int DEFAULT_MAX_LOGGED_CONTENT_LENGTH = 2048;

    private final int maxLoggedResponseLength;
    private final Set<String> includedResponseBodyMediaSubtypes;
    private final Set<BodyMasker> sensitiveBodyMaskers;

    public ResponseBodyLogProcessor() {
        this(DEFAULT_MAX_LOGGED_CONTENT_LENGTH, Set.of("json", "xml"), Set.of());
    }


    public ResponseBodyLogProcessor(int maxLoggedResponseLength, Set<String> includedResponseBodyMediaSubtypes, Set<BodyMasker> sensitiveBodyMaskers) {
        this.maxLoggedResponseLength = maxLoggedResponseLength;
        this.includedResponseBodyMediaSubtypes = includedResponseBodyMediaSubtypes;
        this.sensitiveBodyMaskers = sensitiveBodyMaskers;
    }

    @Override
    public void process(HttpResponse httpResponse, HttpContext context) {
        if (!responseBodyMediaSubtypeMatches(httpResponse)) {
            return;
        }
        String responseBody = getResponseBody(httpResponse);
        if (responseBody != null && responseBody.length() > maxLoggedResponseLength) {
            responseBody = responseBody.substring(0, maxLoggedResponseLength);
        }
        responseBody = maskSensitivePatterns(sensitiveBodyMaskers, responseBody);
        setMDCValue(BODY_MDC_KEY, replaceEmpty(responseBody));
        long contentLength = responseBody == null ? 0 : responseBody.length();
        setMDCValue(LENGTH_MDC_KEY, replaceEmpty(String.valueOf(contentLength)));
    }

    public String getResponseBody(HttpResponse response) {
        if (HttpEntityContainer.class.isAssignableFrom(response.getClass())) {
            try {
                return cloneContentFromHttpEntity((HttpEntityContainer) response);
            } catch (IOException e) {
                log.error("Failed to read response entity", e);
            }
        }
        return null;
    }

    protected String cloneContentFromHttpEntity(HttpEntityContainer httpEntityContainer) throws IOException {
        HttpEntity httpEntity = httpEntityContainer.getEntity();

        if (httpEntity == null) {
            return null;
        }

        Charset charset = getCharset(httpEntity);

        String content;
        InputStream newInputStream;

        if ("gzip".equals(httpEntity.getContentEncoding())) {
            content = consumeGzipStream(httpEntity);
            newInputStream = createGzipInputStream(content.getBytes(charset));
        } else {
            content = consumeContent(httpEntity);
            newInputStream = new ByteArrayInputStream(content.getBytes(charset));
        }

        httpEntityContainer.setEntity(cloneEntity(httpEntity, newInputStream));
        return content;
    }

    public static BasicHttpEntity cloneEntity(EntityDetails originalEntity, InputStream content) {
        return new BasicHttpEntity(content, originalEntity.getContentLength(),
                ContentType.parseLenient(originalEntity.getContentType()),
                originalEntity.getContentEncoding(), originalEntity.isChunked());
    }

    protected String consumeGzipStream(HttpEntity httpEntity) throws IOException {
        if (httpEntity == null) {
            return null;
        }

        Charset charset = getCharset(httpEntity);

        try {
            return readGzipStream(httpEntity.getContent(), charset);
        } catch (UnsupportedOperationException e) {
            ByteArrayOutputStream contentStream = getEntityStream(httpEntity);
            return readGzipStream(new ByteArrayInputStream(contentStream.toByteArray()), charset);
        }
    }

    public static String readGzipStream(InputStream inputStream, Charset charset) throws IOException {
        try (InputStream gzip = GZIPInputStreamFactory.getInstance().create(inputStream)) {
            return new String(gzip.readAllBytes(), charset);
        }
    }

    public static InputStream createGzipInputStream(byte[] content) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStream gzip = new GZIPOutputStream(out);
        gzip.write(content);
        gzip.close();
        return new ByteArrayInputStream(out.toByteArray());
    }

    protected String consumeContent(HttpEntity httpEntity) throws IOException {
        if (httpEntity == null) {
            return null;
        }

        Charset charset = getCharset(httpEntity);

        try {
            return new String(httpEntity.getContent().readAllBytes(), charset);
        } catch (UnsupportedOperationException e) {
            return getEntityStream(httpEntity).toString(charset);
        }
    }

    private boolean responseBodyMediaSubtypeMatches(HttpResponse httpResponse) {
        try {
            String mediaType = httpResponse.getHeader("Content-Type").getValue();
            ContentType contentType = ContentType.parseLenient(mediaType);
            String mimeType = contentType.getMimeType();
            if (StringUtils.isEmpty(mediaType)) {
                return false;
            }
            return includedResponseBodyMediaSubtypes.stream()
                    .map(String::toLowerCase)
                    .anyMatch(mimeType::contains);
        } catch (Exception e) {
            return false;
        }
    }
}
