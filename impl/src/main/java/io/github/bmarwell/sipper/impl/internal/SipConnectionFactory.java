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
package io.github.bmarwell.sipper.impl.internal;

import io.github.bmarwell.sipper.api.RegisteredSipConnection;
import io.github.bmarwell.sipper.api.SipConfiguration;
import io.github.bmarwell.sipper.api.SipConnection;
import io.github.bmarwell.sipper.impl.SocketInConnectionReader;
import io.github.bmarwell.sipper.impl.ip.IpUtil;
import io.github.bmarwell.sipper.impl.proto.NotifyingSipIncomingAuthenticationRequestHandler;
import io.github.bmarwell.sipper.impl.proto.QueueingSipIncomingMessageHandler;
import io.github.bmarwell.sipper.impl.proto.SipMessageFactory;
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
        try {
            return buildSocketSipConnection();
        } catch (IOException ioException) {
            LOG.error("Problem creating SipConnection.", ioException);
            throw new UncheckedIOException(ioException);
        }
    }

    public RegisteredSipConnection register(SipConnection sipConnection) {
        final var tagBytes = new byte[12];
        final var callIdBytes = new byte[12];
        RandomGeneratorFactory.getDefault().create().nextBytes(tagBytes);
        RandomGeneratorFactory.getDefault().create().nextBytes(callIdBytes);
        var tag = Base64.getEncoder().encodeToString(tagBytes);
        var callId = Base64.getEncoder().encodeToString(callIdBytes);

        final var connectedSipConnection = (ConnectedSipConnection) sipConnection;

        final var registerPreflight = this.messageFactory.getRegisterPreflight(
                tag, callId, connectedSipConnection.getSocket(), IpUtil.getPublicIpv4());

        registerPreflight(connectedSipConnection, registerPreflight);

        return new DefaultRegisteredSipConnection(connectedSipConnection);
    }

    public void registerPreflight(ConnectedSipConnection connectedSipConnection, String message) {
        final var inReader = connectedSipConnection.getInReader();

        connectedSipConnection.writeAndFlush(message);

        final var msgHandler = new NotifyingSipIncomingAuthenticationRequestHandler(inReader.getMsgHandler());
        final var join = CompletableFuture.runAsync(msgHandler)
                // wait 2 secs
                .orTimeout(this.sipConfiguration.getReadTimeout().toMillis(), TimeUnit.MILLISECONDS)
                // then act
                .handle((var result, var error) -> {
                    if (error != null) {
                        LOG.error("Error occurred while waiting: ", error);
                        throw new IllegalStateException("Invalid or missing reply while registering", error);
                    }

                    LOG.info("Auth request: " + msgHandler.getSipAuthenticationRequest());
                    return msgHandler.getSipAuthenticationRequest();
                })
                .whenComplete(((sipAuthenticationRequest, error) -> {
                    if (error != null) {
                        return;
                    }

                    final var login = this.messageFactory.getLogin(sipAuthenticationRequest);
                    connectedSipConnection.writeAndFlush(login);
                }))
                .join();
    }

    protected ConnectedSipConnection buildSocketSipConnection() throws IOException {
        var socket = createSocket();
        var out = new BufferedOutputStream(socket.getOutputStream());

        var onResponse = new QueueingSipIncomingMessageHandler();
        var inReader = new SocketInConnectionReader(socket.getInputStream(), onResponse);

        return new ConnectedSipConnection(socket, out, inReader);
    }

    protected Socket createSocket() throws IOException {
        final var socket = new Socket();

        final var remoteSocketAddress =
                new InetSocketAddress(IpUtil.getRegistrarEndpoint(this.sipConfiguration.getRegistrar()), 5060);

        LOG.info("Waiting for connection to succeed.");
        socket.connect(
                remoteSocketAddress,
                Math.toIntExact(this.sipConfiguration.getConnectTimeout().toMillis()));

        return socket;
    }
}
