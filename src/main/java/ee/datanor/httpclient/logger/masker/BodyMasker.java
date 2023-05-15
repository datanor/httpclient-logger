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

package ee.datanor.httpclient.logger.masker;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class BodyMasker {

    private final Pattern paramPattern;

    public BodyMasker(String regexp) {
        this.paramPattern = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE & Pattern.MULTILINE);
    }

    public String mask(String content) {
        long start = System.currentTimeMillis();
        String response = content;
        Matcher matcher = paramPattern.matcher(response);
        ArrayList<int[]> replacePositions = new ArrayList<>();
        while (matcher.find()) {
            for (int i = matcher.groupCount(); i >0 ; i--) {
                int startPos = matcher.start(i);
                int endPos = matcher.end(i);
                replacePositions.add(new int[] {startPos, endPos});
            }
        }
        ListIterator<int[]> listIterator = replacePositions.listIterator(replacePositions.size());
        while (listIterator.hasPrevious()) {
            int[] postitions = listIterator.previous();
            response = new StringBuilder(response).replace(postitions[0], postitions[1], "***").toString();
        }

        log.trace("Sensitive patterns replaced in {} ms", System.currentTimeMillis() - start);

        return response;
    }
}
