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
package io.github.bmarwell.sipper.impl.proto;

import java.util.concurrent.TimeUnit;

public abstract class AbstractNotifyingMessageHandler implements Runnable {

    private final QueueingSipIncomingMessageHandler queueingSipIncomingMessageHandler;

    private boolean interrupted = false;

    public AbstractNotifyingMessageHandler(QueueingSipIncomingMessageHandler queueingSipIncomingMessageHandler) {
        this.queueingSipIncomingMessageHandler = queueingSipIncomingMessageHandler;
    }

    abstract void onMessageReceived(String message);

    abstract boolean matchesMessage(String message);

    @Override
    public void run() {
        while (!interrupted) {
            try {
                TimeUnit.MILLISECONDS.sleep(200L);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                this.interrupted = true;
                return;
            }

            if (this.interrupted) {
                return;
            }

            var msgFound = this.queueingSipIncomingMessageHandler.getMessages().stream()
                    .filter(this::matchesMessage)
                    .findFirst();
            if (msgFound.isPresent()) {
                this.queueingSipIncomingMessageHandler.remove(msgFound.orElseThrow());
                onMessageReceived(msgFound.orElseThrow());
                break;
            }
        }
    }

    public void interrupt() {
        this.interrupted = true;
        Thread.currentThread().interrupt();
    }
}