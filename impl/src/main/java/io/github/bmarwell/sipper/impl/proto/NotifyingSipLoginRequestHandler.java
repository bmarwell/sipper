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

public class NotifyingSipLoginRequestHandler extends AbstractNotifyingMessageHandler {

    public NotifyingSipLoginRequestHandler(QueueingSipIncomingMessageHandler msgHandler) {
        super(msgHandler);
    }

    @Override
    boolean matchesMessage(RawSipMessage message) {
        return matchesOkMessage(message.rawMessageHeader());
    }

    private boolean matchesOkMessage(String message) {
        return message.startsWith("SIP/2.0 200 OK");
    }

    @Override
    void onMessageReceived(RawSipMessage message) {
        if (message.rawMessageHeader().startsWith("SIP/2.0 200 OK")) {
            return;
        }

        throw new IllegalStateException("login not successful: \n[" + message + "]");
    }
}
