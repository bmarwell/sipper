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

import java.io.IOException;

/**
 * The actual SIP client which can open connections.
 *
 * <p>Instances of SipClient are to be obtained via the ServiceLoader mechanism, more specifically via
 * the SipClientBuilder class.</p>
 */
public interface SipClient {

    /**
     * The connect method will, despite its name, not only try to establish a network connection, but also
     * send LOGIN commands via the REGISTER method.
     *
     * <p>The caller is supposed to check whether this connection is a {@link RegisteredSipConnection}.</p>
     * @return a SipConnection, possibly a {@link RegisteredSipConnection}.
     *
     * @throws IOException when there is a problem establishing the network connection via opening a TCP socket.
     * @throws RuntimeException When the public IPv4 could not be determined. TODO make a dedicated exception.
     */
    SipConnection connect() throws IOException;
}
