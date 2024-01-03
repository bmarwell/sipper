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

import java.util.Locale;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotifyingSipIncomingAuthenticationRequestHandler extends AbstractNotifyingMessageHandler
        implements Runnable {

    private static final Pattern ALGORITHM = Pattern.compile(".*\\balgorithm=(?<algo>[a-zA-Z0-9]+)[,$].*");
    private static final Pattern REALM = Pattern.compile(".*\\brealm=\"(?<realm>[a-zA-Z0-9.-]+)\".*");
    private static final Pattern NONCE = Pattern.compile(".*\\bnonce=\"(?<nonce>[a-zA-Z0-9]+)\".*");
    private static final Pattern QOP = Pattern.compile(".*\\bqop=\"(?<qop>[a-zA-Z0-9]+)\".*");

    private static final Logger LOG = LoggerFactory.getLogger(NotifyingSipIncomingAuthenticationRequestHandler.class);

    private SipAuthenticationRequest sipAuthenticationRequest;

    public NotifyingSipIncomingAuthenticationRequestHandler(
            QueueingSipIncomingMessageHandler queueingSipIncomingMessageHandler) {
        super(queueingSipIncomingMessageHandler);
    }

    @Override
    boolean matchesMessage(RawSipMessage message) {
        return message.rawMessageHeader().startsWith("SIP/2.0 401 Unauthorized")
                && message.rawMessageHeader().toLowerCase(Locale.ROOT).contains("www-authenticate");
    }

    @Override
    void onMessageReceived(RawSipMessage rawSipMessage) {
        // Parse message
        var message = rawSipMessage.rawMessageHeader();
        LOG.trace("Parsing message: [{}].", message);
        final var lines = message.lines();
        final var wwwAuthOpt = lines.filter(l -> l.toLowerCase(Locale.ROOT).startsWith("www-authenticate:"))
                .findFirst();

        if (wwwAuthOpt.isEmpty()) {
            throw new IllegalArgumentException(
                    "Message has illegal content, no line starts with 'WWW-Authenticate: '. : " + message);
        }

        final var wwwAuth = wwwAuthOpt.orElseThrow().split(":", 2);
        final var algoMatcher = ALGORITHM.matcher(wwwAuth[1]);
        if (!algoMatcher.find()) {
            throw new IllegalArgumentException("Message has illegal content, no match for algorithm: " + message);
        }
        final var algo = algoMatcher.group("algo");

        final var realmMatcher = REALM.matcher(wwwAuth[1]);
        if (!realmMatcher.find()) {
            throw new IllegalArgumentException("Message has illegal content, no match for realm: " + message);
        }
        final var realm = realmMatcher.group("realm");

        final var nonceMatcher = NONCE.matcher(wwwAuth[1]);
        if (!nonceMatcher.find()) {
            throw new IllegalArgumentException("Message has illegal content, no match for nonce: " + message);
        }
        final var nonce = nonceMatcher.group("nonce");

        final var qopMatcher = QOP.matcher(wwwAuth[1]);
        if (!qopMatcher.find()) {
            throw new IllegalArgumentException("Message has illegal content, no match for qop: " + message);
        }
        final var qop = qopMatcher.group("qop");

        this.sipAuthenticationRequest = new SipAuthenticationRequest(algo, realm, nonce, qop);
        LOG.trace("Setting request: [{}].", sipAuthenticationRequest);
    }

    public SipAuthenticationRequest getSipAuthenticationRequest() {
        return sipAuthenticationRequest;
    }
}
