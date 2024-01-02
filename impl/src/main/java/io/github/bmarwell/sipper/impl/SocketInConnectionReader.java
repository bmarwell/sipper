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

import io.github.bmarwell.sipper.impl.proto.QueueingSipIncomingMessageHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocketInConnectionReader implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(SocketInConnectionReader.class);

    private final BufferedReader socketInput;
    private final QueueingSipIncomingMessageHandler msgHandler;
    private boolean interrupted = false;

    public SocketInConnectionReader(InputStream socketInput, QueueingSipIncomingMessageHandler msgHandler) {
        this.socketInput = new BufferedReader(new InputStreamReader(socketInput));
        this.msgHandler = msgHandler;
    }

    @Override
    public void run() {
        StringBuilder currentMessage = new StringBuilder();
        String readLine;
        try {
            checkInterrupted();
            LOG.trace("Now listening for incoming messages");
            while (!this.interrupted && (readLine = this.socketInput.readLine()) != null) {
                checkInterrupted();

                if (this.interrupted) {
                    return;
                }

                currentMessage.append(readLine);
                currentMessage.append('\n');

                if (currentMessage.toString().endsWith("\n\n")) {
                    checkInterrupted();
                    this.msgHandler.accept(currentMessage.toString());
                    LOG.trace("Received message:\n[{}]", currentMessage);
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

    private void checkInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted() || Thread.interrupted() || this.interrupted) {
            this.interrupted = true;
            Thread.currentThread().interrupt();
            throw new InterruptedException();
        }
    }

    public QueueingSipIncomingMessageHandler getMsgHandler() {
        return msgHandler;
    }

    public void interrupt() {
        this.interrupted = true;
        Thread.currentThread().interrupt();
    }
}
