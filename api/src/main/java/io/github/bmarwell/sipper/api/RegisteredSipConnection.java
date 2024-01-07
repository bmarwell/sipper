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
package io.github.bmarwell.sipper.api;

/**
 * A SipConnection with the {@literal "registered"} status, i.e. the connection did send a REGISTER command which was accepted
 * by the registrar.
 */
public interface RegisteredSipConnection extends SipConnection {

    /**
     * Send a {@code SIP/2.0 486 Busy Here}.
     * @param inviteInformation the invitation to reply to.
     */
    void sendBusy(SipEventHandler.SipInviteEvent inviteInformation);

    /**
     * Sends a {@code SIP/2.0 183 Session in Progress}.
     *
     * <p>The advantage over {@code SIP/2.0 100 Ringing} is that UAC and UAS and Caller
     * can already start opening a connection (early media).
     * This way, no time (speech) at the beginning of the session is lost.</p>
     * @param inviteInformation the invitation to reply to.
     */
    void sendRingAndSessionProgress(SipEventHandler.SipInviteEvent inviteInformation);
}
