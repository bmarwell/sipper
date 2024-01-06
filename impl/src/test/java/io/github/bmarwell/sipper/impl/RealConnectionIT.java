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
package io.github.bmarwell.sipper.impl;

import static org.awaitility.Awaitility.await;

import io.github.bmarwell.sipper.api.ImmutableSipConfiguration;
import io.github.bmarwell.sipper.api.SipClientBuilder;
import io.github.bmarwell.sipper.api.SipConnection;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class RealConnectionIT {
    @Test
    void connectAndRegister() {
        // given
        final var conf = ImmutableSipConfiguration.builder()
                .registrar(System.getProperty("sip.registrar"))
                .sipId(System.getProperty("sip.sipid"))
                .loginUserId(System.getProperty("sip.userid"))
                .loginPassword(System.getProperty("sip.password"))
                .build();
        final var sipClient = SipClientBuilder.build(conf);

        // when
        try (var connect = sipClient.connect()) {
            Assertions.assertThat(connect)
                    // then
                    .matches(SipConnection::isConnected);

            await().atMost(2_000L, TimeUnit.MILLISECONDS).until(connect::isRegistered);
        } catch (Exception ioException) {
            Assertions.fail("could not connect.", ioException);
        }
    }
}
