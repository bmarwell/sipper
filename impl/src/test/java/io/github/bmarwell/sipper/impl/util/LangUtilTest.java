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
package io.github.bmarwell.sipper.impl.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class LangUtilTest {

    @ParameterizedTest
    @ValueSource(strings = {"asd", "0xff"})
    void invalid_string_returns_empty_opt(String input) {
        // when
        final var integerOrEmpty = LangUtil.isIntegerOrEmpty(input);

        // then
        assertThat(integerOrEmpty).isEmpty();
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            textBlock =
                    """
            10 | 10 | simple integer is recognized
            011 | 11 | octal is interpreted as decimal
            """)
    void accepts_decimal_input(String input, int expected, String reason) {
        // when
        final var integerOrEmpty = LangUtil.isIntegerOrEmpty(input);

        // then
        assertThat(integerOrEmpty).as(reason).isPresent().as(reason).hasValue(expected);
    }
}
