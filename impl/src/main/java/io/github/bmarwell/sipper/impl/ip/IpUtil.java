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
package io.github.bmarwell.sipper.impl.ip;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

public final class IpUtil {

    private static final Logger LOG = LoggerFactory.getLogger(IpUtil.class);

    private IpUtil() {
        // util class
    }

    public static InetAddress getPublicIpv4() {
        try (var client = HttpClient.newHttpClient()) {
            final var request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create("https://ident.me/"))
                    .timeout(Duration.ofMillis(1_500L))
                    .header("Accept", "text/plain")
                    .build();

            final var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return InetAddress.getByName(response.body());
        } catch (IOException | InterruptedException ioEx) {
            throw new RuntimeException(ioEx);
        }
    }

    public static String getRegistrarEndpoint(String registrar) {
        try {
            final var lookup = getLookupFor(registrar);
            final var answers = lookup.run();

            return getFirstTargetFromAnswers(answers);
        } catch (TextParseException ex) {
            throw new IllegalStateException(ex);
        }
    }

    static String getFirstTargetFromAnswers(Record[] answers) {
        return Arrays.stream(answers)
                .map(rec -> (SRVRecord) rec)
                .map(SRVRecord::getTarget)
                .map(Name::toString)
                .findFirst()
                .orElseThrow();
    }

    static Lookup getLookupFor(String registrar) throws TextParseException {
        return new Lookup("_sip._udp." + registrar, Type.SRV);
    }
}
