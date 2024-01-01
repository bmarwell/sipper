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

import io.github.bmarwell.sipper.api.RegisteredSipConnection;
import io.github.bmarwell.sipper.api.SipClient;
import io.github.bmarwell.sipper.api.SipConfiguration;
import io.github.bmarwell.sipper.impl.internal.SipConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSipClient implements SipClient {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultSipClient.class);

    private final SipConfiguration sipConfiguration;

    public DefaultSipClient(SipConfiguration sipConfiguration) {
        this.sipConfiguration = sipConfiguration;
    }

    @Override
    public RegisteredSipConnection connect() {
        final var sipConnectionFactory = new SipConnectionFactory(this.sipConfiguration);

        LOG.trace("Connecting…");
        final var connectedSipConnection = sipConnectionFactory.build();

        LOG.trace("Registering…");
        final var registeredSipConnection = sipConnectionFactory.register(connectedSipConnection);

        return registeredSipConnection;
    }
}
