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
package io.github.bmarwell.sipper.api;

import io.github.bmarwell.sipper.api.internal.SipClientBuilderLoader;

/**
 * The public builder which is to be used to obtain an instance of {@link SipClient}.
 */
public interface SipClientBuilder {

    /**
     * Creates an instance of SIP Client with the given configuration.
     * @param sipConfiguration the configuration to use.
     * @return a SIP client instance.
     */
    static SipClient build(SipConfiguration sipConfiguration) {
        final var sipClientBuilder = SipClientBuilderLoader.loadImplementation();
        return sipClientBuilder.create(sipConfiguration);
    }

    SipClient create(SipConfiguration sipConfiguration);
}
