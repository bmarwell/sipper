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

import io.github.bmarwell.sipper.api.SipConnection;
import io.github.bmarwell.sipper.api.SipEventHandler;
import io.github.bmarwell.sipper.impl.SocketInConnectionReader;
import java.io.BufferedOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectedSipConnection implements SipConnection {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectedSipConnection.class);

    private final Socket socket;

    private final SocketInConnectionReader inReader;

    private final BufferedOutputStream out;
    private final PrintWriter outWriter;
    private ReentrantLock outWriterLock = new ReentrantLock();

    private final ExecutorService executorService;
    private final Future<?> inReaderThread;

    public ConnectedSipConnection(Socket socket, BufferedOutputStream out, SocketInConnectionReader inReader) {
        this.socket = socket;
        this.out = out;
        this.outWriter = new PrintWriter(out, false, StandardCharsets.UTF_8);
        this.inReader = inReader;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.inReaderThread = this.executorService.submit(inReader);
    }

    @Override
    public void listen(SipEventHandler sipEventHandler) {
        // TODO: implement
        throw new UnsupportedOperationException(
                "not yet implemented: [io.github.bmarwell.sipper.impl.internal.ConnectedSipConnection::listen].");
    }

    @Override
    public boolean isConnected() {
        return this.socket.isConnected();
    }

    @Override
    public boolean isRegistered() {
        return false;
    }

    protected void writeAndFlush(String message) {
        this.outWriterLock.lock();
        try {
            LOG.trace("Writing message: [{}]", message);
            this.outWriter.write(message);
            this.outWriter.flush();
        } finally {
            this.outWriterLock.unlock();
        }
    }

    @Override
    public void close() throws Exception {
        this.inReaderThread.cancel(true);
        this.socket.close();
    }

    protected Socket getSocket() {
        return this.socket;
    }

    protected BufferedOutputStream getOut() {
        return this.out;
    }

    protected SocketInConnectionReader getInReader() {
        return inReader;
    }

    protected PrintWriter getOutWriter() {
        return outWriter;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ConnectedSipConnection.class.getSimpleName() + "[", "]")
                .add("socket=" + socket)
                .add("inReader=" + inReader)
                .add("out=" + out)
                .add("outWriter=" + outWriter)
                .add("executorService=" + executorService)
                .add("inReaderThread=" + inReaderThread)
                .toString();
    }
}
