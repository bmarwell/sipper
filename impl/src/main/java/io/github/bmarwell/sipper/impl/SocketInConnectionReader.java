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
package io.github.bmarwell.sipper.impl;

import io.github.bmarwell.sipper.impl.proto.RawSipMessage;
import io.github.bmarwell.sipper.impl.proto.RegisterSipIncomingMessageHandler;
import io.github.bmarwell.sipper.impl.proto.SipIncomingMessageHandler;
import io.github.bmarwell.sipper.impl.util.LangUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketInConnectionReader implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(SocketInConnectionReader.class);

    private ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    private final BufferedReader socketReader;
    private final RegisterSipIncomingMessageHandler registerSipIncomingMessageHandler;
    private final Set<SipIncomingMessageHandler> messageHandlers = new HashSet<>();

    private boolean interrupted = false;

    public SocketInConnectionReader(
            InputStream socketInput, RegisterSipIncomingMessageHandler registerSipIncomingMessageHandler) {
        this.socketReader = new BufferedReader(new InputStreamReader(socketInput));
        this.registerSipIncomingMessageHandler = registerSipIncomingMessageHandler;
    }

    @Override
    public void run() {
        StringBuilder currentMessage = new StringBuilder();
        String readLine;
        try {
            checkInterrupted();
            LOG.trace("Now listening for incoming messages");
            while (!this.interrupted && (readLine = this.socketReader.readLine()) != null) {
                checkInterrupted();

                if (this.interrupted) {
                    return;
                }

                currentMessage.append(readLine);
                currentMessage.append('\n');

                final var currentMessageString = currentMessage.toString();
                if (currentMessageString.endsWith("\n\n")) {
                    checkInterrupted();
                    LOG.trace("Received message:\n[{}]", currentMessage);

                    final RawSipMessage rawSipMessage;

                    // probably end of message, UNLESS it is a header with a following body.
                    if (isMessageWithBody(currentMessageString)) {
                        LOG.trace("Message with body");
                        rawSipMessage = processMessageWithBody(currentMessageString);
                    } else {
                        rawSipMessage = new RawSipMessage(currentMessageString);
                    }

                    try {
                        sendToAllMessageHandlers(rawSipMessage);
                    } catch (Exception e) {
                        LOG.trace("Unable to process message.", e);
                    }

                    currentMessage = new StringBuilder();
                }
            }
        } catch (SocketException se) {
            // probably OK...
            Thread.currentThread().interrupt();
        } catch (IOException ioException) {
            LOG.error("Problem while reading input from socket.", ioException);
        } catch (InterruptedException interruptedException) {
            // all ok, nothing to clean up.
            Thread.currentThread().interrupt();
        }
    }

    private static boolean isMessageWithBody(String currentMessageString) {
        return currentMessageString.startsWith("INVITE")
                && currentMessageString.contains("\nContent-Length: ")
                && !currentMessageString.contains("\nContent-Length: 0\n");
    }

    private RawSipMessage processMessageWithBody(String currentMessageString) throws IOException {
        final String bodyLengthString = currentMessageString
                .lines()
                .filter(line -> line.startsWith("Content-Length: "))
                .findFirst()
                .orElseThrow();
        final var bodyLengthNumber = bodyLengthString.split(":", 2)[1].trim();
        final var bodyLengthOpt = LangUtil.isIntegerOrEmpty(bodyLengthNumber);

        if (bodyLengthOpt.isEmpty()) {
            throw new IllegalArgumentException(
                    "Illegal message: Content length is not a number! " + currentMessageString);
        }

        final var bodyLength = bodyLengthOpt.orElseThrow();

        final var bodyContent = new char[bodyLength];
        LOG.trace("Reading [{}] bytes of data", bodyLength);
        final var read = this.socketReader.read(bodyContent, 0, bodyLength);
        if (read < bodyLength) {
            LOG.warn("Not enough bytes read! Expected [" + bodyLength + "] but got " + read + "!");
        }

        final var rawSipMessage = new RawSipMessage(currentMessageString, new String(bodyContent));
        LOG.trace("Received raw sip message: [{}]", rawSipMessage);
        return rawSipMessage;
    }

    private void checkInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted() || Thread.interrupted() || this.interrupted) {
            this.interrupted = true;
            Thread.currentThread().interrupt();
            this.executorService.shutdownNow();
            throw new InterruptedException();
        }
    }

    public RegisterSipIncomingMessageHandler getRegisterSipIncomingMessageHandler() {
        return registerSipIncomingMessageHandler;
    }

    public void sendToAllMessageHandlers(RawSipMessage sipMessage) {
        CompletableFuture.runAsync(() -> this.registerSipIncomingMessageHandler.accept(sipMessage), executorService);

        for (SipIncomingMessageHandler messageHandler : messageHandlers) {
            CompletableFuture.runAsync(() -> messageHandler.accept(sipMessage), executorService);
            // TODO: add exception logging here?
        }
    }

    public void setMessageHandlers(final Collection<SipIncomingMessageHandler> messageHandlers) {
        this.messageHandlers.addAll(messageHandlers);
        this.messageHandlers.removeIf(msgH -> !messageHandlers.contains(msgH));
    }

    public void addMessageHandler(final SipIncomingMessageHandler messageHandler) {
        this.messageHandlers.add(messageHandler);
    }

    public void interrupt() {
        this.interrupted = true;
        this.executorService.shutdownNow();
        Thread.currentThread().interrupt();
    }
}
