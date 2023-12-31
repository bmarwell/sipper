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
package io.github.bmarwell.sipper.impl.proto;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueingSipIncomingMessageHandler implements SipIncomingMessageHandler {

    private static final Logger LOG = LoggerFactory.getLogger(QueueingSipIncomingMessageHandler.class);

    private final ArrayList<RawSipMessage> messages = new ArrayList<>();

    @Override
    public void accept(RawSipMessage sipMessage) {
        LOG.trace("Incoming message:\n[{}]", sipMessage);

        switch (sipMessage.method()) {
            case "REGISTER":
                this.messages.add(sipMessage);
                break;
            case "INVITE":
            default:
                throw new UnsupportedOperationException(
                        "not yet implemented: [io.github.bmarwell.sipper.impl.proto.QueueingSipIncomingMessageHandler::accept].");
        }
    }

    public List<RawSipMessage> getMessages() {
        return List.copyOf(this.messages);
    }

    public void remove(RawSipMessage message) {
        this.messages.remove(message);
    }
}
