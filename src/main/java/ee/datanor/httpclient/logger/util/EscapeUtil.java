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

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class EscapeUtil {

    private static String[] searchList = new String[] { "\b", "\n", "\t", "\f", "\r", "\\" };
    private static String[] replacementList = new String[] { "\\b", "\\n", "\\t", "\\f", "\\r", "\\\\" };

    static {
        List<String> search_list = new ArrayList<>();
        List<String> replacement_list = new ArrayList<>();

        List<String> existing_search_list = Arrays.asList(searchList);

        for (int i = 0x00; i <= 0x1f; i++) {
            if (existing_search_list.contains(intToSingleCharString(i))) {
                continue;
            }
            search_list.add(intToSingleCharString(i));
            replacement_list.add(intToSingleEscapedUnicodeCharString(i));
        }

        for (int i = 0x7f; i <= 0x9f; i++) {
            search_list.add(intToSingleCharString(i));
            replacement_list.add(intToSingleEscapedUnicodeCharString(i));
        }

        search_list.addAll(Arrays.asList(searchList));
        replacement_list.addAll(Arrays.asList(replacementList));

        searchList = search_list.toArray(searchList);
        replacementList = replacement_list.toArray(replacementList);
    }

    private EscapeUtil() {
    }

    public static String escape(String val) {
        return StringUtils.replaceEach(val, searchList, replacementList);
    }

    private static String intToSingleCharString(int x) {
        return String.format("%c", (char) x);
    }

    private static String intToSingleEscapedUnicodeCharString(int x) {
        return String.format("\\u00%02X", x);
    }


}
