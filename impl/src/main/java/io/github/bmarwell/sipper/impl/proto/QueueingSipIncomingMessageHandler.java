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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class QueueingSipIncomingMessageHandler implements SipIncomingMessageHandler {

    private static final Logger LOG = LoggerFactory.getLogger(QueueingSipIncomingMessageHandler.class);

    private final ArrayList<String> messages = new ArrayList<>();

    @Override
    public void accept(String message) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Incoming message:\n[{}]", message);
        }

        this.messages.add(message);
    }

    public List<String> getMessages() {
        return List.copyOf(this.messages);
    }

    public void remove(String message) {
        this.messages.remove(message);
    }
}
