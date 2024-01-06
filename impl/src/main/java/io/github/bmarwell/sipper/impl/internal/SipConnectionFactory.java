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
import io.github.bmarwell.sipper.api.SipConfiguration;
import io.github.bmarwell.sipper.api.SipConnection;
import io.github.bmarwell.sipper.impl.SocketInConnectionReader;
import io.github.bmarwell.sipper.impl.ip.IpUtil;
import io.github.bmarwell.sipper.impl.proto.*;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGeneratorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SipConnectionFactory {

    private static final Logger LOG = LoggerFactory.getLogger(SipConnectionFactory.class);

    private final SipConfiguration sipConfiguration;

    private final SipMessageFactory messageFactory;

    public SipConnectionFactory(SipConfiguration sipConfiguration) {
        this.sipConfiguration = sipConfiguration;
        this.messageFactory = new SipMessageFactory(this.sipConfiguration);
    }

    public SipConnection build() {
        final var tagBytes = new byte[12];
        final var callIdBytes = new byte[12];
        RandomGeneratorFactory.getDefault().create().nextBytes(tagBytes);
        RandomGeneratorFactory.getDefault().create().nextBytes(callIdBytes);
        final var encoder = Base64.getEncoder().withoutPadding();
        var tag = encoder.encodeToString(tagBytes).replaceAll("/", "g");
        var callId = encoder.encodeToString(callIdBytes).replaceAll("/", "g");

        try {
            return buildSocketSipConnection(tag, callId);
        } catch (IOException ioException) {
            LOG.error("Problem creating SipConnection.", ioException);
            throw new UncheckedIOException(ioException);
        }
    }

    public RegisteredSipConnection register(SipConnection sipConnection) {
        final var connectedSipConnection = (ConnectedSipConnection) sipConnection;

        final var registerPreflight =
                this.messageFactory.getRegisterPreflight(IpUtil.getPublicIpv4(), connectedSipConnection);

        CompletableFuture.supplyAsync(() -> this.registerPreflight(connectedSipConnection, registerPreflight))
                .orTimeout(2_000L, TimeUnit.MILLISECONDS)
                .thenAcceptAsync(
                        (SipAuthenticationRequest authRequest) -> doRegister(authRequest, connectedSipConnection))
                .orTimeout(2_000L, TimeUnit.MILLISECONDS)
                .join();

        return new DefaultRegisteredSipConnection(connectedSipConnection);
    }

    private void doRegister(SipAuthenticationRequest authRequest, ConnectedSipConnection connectedSipConnection) {
        final var msgHandler = new NotifyingSipLoginRequestHandler(
                connectedSipConnection.getInReader().getMsgHandler());

        final var login = this.messageFactory.getLogin(
                authRequest,
                connectedSipConnection,
                this.sipConfiguration.getLoginUserId(),
                this.sipConfiguration.getLoginPassword());
        connectedSipConnection.writeAndFlush(login);
        connectedSipConnection.setAuthorizationString(
                this.messageFactory.getAuthorization().orElseThrow());
        msgHandler.run();

        LOG.debug("Login successful");
    }

    public SipAuthenticationRequest registerPreflight(ConnectedSipConnection connectedSipConnection, String message) {
        final var inReader = connectedSipConnection.getInReader();

        connectedSipConnection.writeAndFlush(message);

        final var msgHandler = new NotifyingSipIncomingAuthenticationRequestHandler(inReader.getMsgHandler());
        msgHandler.run();

        return msgHandler.getSipAuthenticationRequest();
    }

    protected ConnectedSipConnection buildSocketSipConnection(String tag, String callId) throws IOException {
        var socket = createSocket();
        var out = new BufferedOutputStream(socket.getOutputStream());

        var onResponse = new QueueingSipIncomingMessageHandler();
        var inReader = new SocketInConnectionReader(socket.getInputStream(), onResponse);

        return new ConnectedSipConnection(
                socket,
                out,
                inReader,
                this.sipConfiguration.getRegistrar(),
                this.sipConfiguration.getSipId(),
                tag,
                callId);
    }

    protected Socket createSocket() throws IOException {
        final var socket = new Socket();

        final var remoteSocketAddress =
                new InetSocketAddress(IpUtil.getRegistrarEndpoint(this.sipConfiguration.getRegistrar()), 5060);

        LOG.trace("Waiting for connection to succeed.");
        socket.connect(
                remoteSocketAddress,
                Math.toIntExact(this.sipConfiguration.getConnectTimeout().toMillis()));

        return socket;
    }
}
