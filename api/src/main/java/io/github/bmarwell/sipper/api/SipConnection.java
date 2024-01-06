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

import java.net.InetAddress;

/**
 * A sip connection is a handle which denotes an open and (network-wise) valid connection to a SIP registrar.
 *
 * <p>The SipConnection can be acquired via the SipClient, but will almost always be subclassed by {@link RegisteredSipConnection}.</p>
 */
public interface SipConnection extends AutoCloseable {

    void listen(SipEventHandler sipEventHandler);

    boolean isConnected();

    boolean isRegistered();

    /**
     * Returns the public IP address which was used to open the connection.
     * @return the public IP address which was used to open the connection.
     */
    InetAddress getPublicIp();
}
