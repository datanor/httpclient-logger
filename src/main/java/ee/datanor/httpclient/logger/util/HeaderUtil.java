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

package ee.datanor.httpclient.logger.util;

import org.apache.hc.core5.http.Header;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class HeaderUtil {

    private HeaderUtil() { }

    public static String headersToString(Header[] headers, Set<String> includedHeaders) {
        return Arrays.stream(headers)
                .filter(h -> includedHeaders.contains(h.getName().toLowerCase()))
                .map(h -> {
                    String value = h.getValue();
                    return String.format("%s: %s", h.getName(), value);
                })
                .collect(Collectors.joining("\n"));
    }
}
