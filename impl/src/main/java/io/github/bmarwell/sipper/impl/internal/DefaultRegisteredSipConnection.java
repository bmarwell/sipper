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
package io.github.bmarwell.sipper.impl.internal;

import io.github.bmarwell.sipper.api.RegisteredSipConnection;
import io.github.bmarwell.sipper.api.SipEventHandler;
import io.github.bmarwell.sipper.impl.proto.SipMessageFactory;
import java.net.Socket;
import java.util.Optional;

public class DefaultRegisteredSipConnection implements RegisteredSipConnection {

    private final ConnectedSipConnection sipConnection;
    private boolean closedByHook;
    private boolean registered;

    private Thread shutdownHook;

    public DefaultRegisteredSipConnection(ConnectedSipConnection sipConnection) {
        this.sipConnection = sipConnection;
        this.registered = true;
        this.shutdownHook = new Thread(() -> {
            try {
                this.closedByHook = true;
                this.close();
            } catch (Exception e) {
                // ignore on shutdown
            }
        });
        Runtime.getRuntime().addShutdownHook(this.shutdownHook);
    }

    @Override
    public void listen(SipEventHandler sipEventHandler) {
        this.sipConnection.listen(sipEventHandler);
    }

    @Override
    public boolean isConnected() {
        return this.sipConnection.isConnected();
    }

    @Override
    public boolean isRegistered() {
        return this.registered;
    }

    @Override
    public void close() throws Exception {
        if (!closedByHook) {
            try {
                Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
            } catch (IllegalStateException ise) {
                // no worries.
            }
        }
        this.registered = false;
        final var unregister = new SipMessageFactory(this.sipConnection.getRegistrar(), this.sipConnection.getSipId())
                .getUnregister(this);
        this.sipConnection.writeAndFlush(unregister);
        // short amount of time of judiciously waiting for a response, as we don't want to unnecessarily lengthen the
        // shutdown.
        Thread.sleep(100);
        this.sipConnection.close();
    }

    public Socket getSocket() {
        return this.sipConnection.getSocket();
    }

    public String getTag() {
        return this.sipConnection.getTag();
    }

    public String getCallId() {
        return this.sipConnection.getCallId();
    }

    public long getAndUpdateCseq() {
        return this.sipConnection.getAndUpdateCseq();
    }

    public Optional<String> getAuthorization() {
        return this.sipConnection.getAuthorization();
    }
}
