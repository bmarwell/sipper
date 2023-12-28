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
package io.github.bmarwell.sipper.api.internal;

import io.github.bmarwell.sipper.api.SipClientBuilder;
import java.util.ServiceLoader;

/**
 * Internal: Loads a SipClientBuilder implementation via Java {@link ServiceLoader} mechanisms.
 */
public class SipClientBuilderLoader {

    public static SipClientBuilder loadImplementation() {
        final var optSipClientBuilder =
                ServiceLoader.load(SipClientBuilder.class).findFirst();

        if (optSipClientBuilder.isEmpty()) {
            throw new IllegalStateException("No implementation found in class path/module path");
        }

        return optSipClientBuilder.orElseThrow();
    }
}
