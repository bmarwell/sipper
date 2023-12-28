/*
 * Copyright (C) ${project.inceptionYear} The SIPper project team.
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

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bmarwell.sipper.api.ImmutableSipConfiguration;
import io.github.bmarwell.sipper.api.SipClientBuilder;
import org.junit.jupiter.api.Test;

class DefaultSipClientBuilderTest {

    @Test
    void can_be_instantiated_via_ServiceLoader() {
        // given
        var sc = ImmutableSipConfiguration.builder().build();

        // when
        final var sipClient = SipClientBuilder.build(sc);

        // then
        assertThat(sipClient).isNotNull();
    }
}
