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

import io.github.bmarwell.sipper.api.RegisteredSipConnection;
import io.github.bmarwell.sipper.api.SipEventHandler;
import io.github.bmarwell.sipper.impl.util.LangUtil;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericSipIncomingMessageHandler implements SipIncomingMessageHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GenericSipIncomingMessageHandler.class);

    private final Set<SipEventHandler> listeners = new HashSet<>();
    private final RegisteredSipConnection registeredSipConnection;

    public GenericSipIncomingMessageHandler(
            RegisteredSipConnection registeredSipConnection, final Collection<SipEventHandler> listeners) {
        this.registeredSipConnection = registeredSipConnection;
        this.listeners.addAll(Set.copyOf(listeners));
    }

    @Override
    public void accept(RawSipMessage sipMessage) {
        LOG.trace("Incoming message:\n[{}]", sipMessage);

        // pass
        if (sipMessage.method().equals("INVITE")) {
            final var cSeqStr = sipMessage.header().get("CSeq").trim();
            final var cseqOpt = LangUtil.isIntegerOrEmpty(cSeqStr.split(" ")[0]);
            if (cseqOpt.isEmpty()) {
                LOG.error("Message did not have valid cseq: " + cSeqStr);
                throw new IllegalArgumentException("Message did not have valid cseq: " + cSeqStr);
            }
            final var inviteEvent = new SipEventHandler.SipInviteEvent(
                    sipMessage.header().get("Call-ID"),
                    sipMessage.header().get("From"),
                    cseqOpt.getAsInt(),
                    sipMessage.header().get("Via"));
            for (SipEventHandler listener : this.listeners) {
                LOG.trace("RING RING to " + listener);
                try {
                    listener.onRing(this.registeredSipConnection, inviteEvent);
                } catch (RuntimeException rtEx) {
                    LOG.error("listener " + listener + " threw exception with onRing()", rtEx);
                }
            }
        }
    }

    public void setListeners(Collection<SipEventHandler> listeners) {
        this.listeners.addAll(listeners);
        this.listeners.removeIf(l -> !listeners.contains(l));
    }
}
