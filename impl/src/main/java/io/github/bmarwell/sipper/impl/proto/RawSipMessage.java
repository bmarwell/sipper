/*
 * Copyright (C) 2023-2024 The SIPper project team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.bmarwell.sipper.impl.proto;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public record RawSipMessage(String rawMessageHeader, String method, Map<String, String> header, String rawBody) {
    public RawSipMessage(final String rawMessageHeader) {
        this(rawMessageHeader, getMethod(rawMessageHeader), getHeaders(rawMessageHeader), "");
    }

    public RawSipMessage(final String rawMessageHeader, final String body) {
        this(rawMessageHeader, getMethod(rawMessageHeader), getHeaders(rawMessageHeader), body);
    }

    private static Map<String, String> getHeaders(final String rawMessageHeader) {
        try {
            final Map<String, String> headers = rawMessageHeader
                    .lines()
                    .skip(1L)
                    .filter(line -> line.contains(":"))
                    .map(line -> {
                        var linesplit = line.split(":", 2);
                        return (Map.Entry<String, String>) new AbstractMap.SimpleEntry<>(linesplit[0], linesplit[1]);
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new));
            return Map.copyOf(headers);
        } catch (Exception anyEx) {
            throw new IllegalArgumentException("Invalid headers: " + rawMessageHeader);
        }
    }

    private static String getMethod(final String rawMessageHeader) {
        return rawMessageHeader
                .lines()
                // find method line
                .filter(line -> line.toLowerCase(Locale.ROOT).startsWith("cseq:"))
                .findFirst()
                .orElseThrow()
                .split(" ")[2];
    }

    public boolean hasBody() {
        return !rawBody.isEmpty();
    }
}
