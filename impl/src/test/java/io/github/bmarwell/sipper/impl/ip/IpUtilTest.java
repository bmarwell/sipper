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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TextParseException;

class IpUtilTest {

    @Test
    void looks_up_srv_records() throws TextParseException {
        // given
        var registrar = "tel.telekom.de";

        // when
        final var lookupFor = IpUtil.getLookupFor(registrar);

        // then
        assertThat(lookupFor)
                // that
                .hasFieldOrPropertyWithValue("name", new Name("_sip._udp." + registrar))
                .hasFieldOrPropertyWithValue("name", new Name("_sip._udp." + registrar))
        // end
        ;
    }

    @Test
    void reads_results_from_dns_reply() throws TextParseException {
        // given
        var firstTarget = new Name("hno002-l01-mav-pc-rt-001.edns.t-ipnet.de.");
        var secondTarget = new Name("kln000-l01-mav-pc-rt-001.edns.t-ipnet.de.");
        var answers = new Record[] {
            new SRVRecord(firstTarget, 1, 3600, 20, 0, 5060, firstTarget),
            new SRVRecord(secondTarget, 1, 3600, 10, 0, 5060, secondTarget)
        };

        // when
        final var firstTargetFromAnswers = IpUtil.getFirstTargetFromAnswers(answers);

        // then
        assertThat(firstTargetFromAnswers).isEqualTo(firstTarget.toString());
    }
}
