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
package io.github.bmarwell.sipper.api;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SipClientBuilderTest {

    /**
     * method contract test.
     */
    @Test
    void no_implementation_available_in_API_throws_ISE() {
        // given
        final SipConfiguration configuration = ImmutableSipConfiguration.builder()
                .registrar("")
                .sipId("")
                .loginUserId("")
                .loginPassword("")
                .build();

        // expect
        assertThatThrownBy(() -> SipClientBuilder.build(configuration))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("implementation found");
    }

    @Test
    void null_configuration_throws_NPE() {
        // expect
        assertThatThrownBy(() -> SipClientBuilder.build(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("sipConf");
    }
}
