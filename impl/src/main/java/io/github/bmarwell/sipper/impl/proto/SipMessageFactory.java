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

import io.github.bmarwell.sipper.api.SipConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Locale;

public class SipMessageFactory {

    private static final Logger LOG = LoggerFactory.getLogger(SipMessageFactory.class);

    private final SipConfiguration conf;

    public SipMessageFactory(SipConfiguration conf) {
        this.conf = conf;
    }

    public String getRegisterPreflight(String tag, String callId, Socket socket, InetAddress publicIp) {
        final var template =
                """
                %1$s sip:%2$s SIP/2.0
                CSeq: 10 %1$s
                Via: SIP/2.0/TCP %8$s:%7$s;alias;branch=z9hG4bK.8i7nkaF9s;rport
                From: <sip:%3$s@%2$s>;tag=%4$s
                To: sip:%3$s@%2$s
                Call-ID: %5$s
                Max-Forwards: 70
                Supported: replaces, outbound, gruu, path, record-aware
                Contact: <sip:%3$s@%6$s:%7$s;transport=tcp>;q=1
                Expires: 600
                User-Agent: SIPper/0.1.0
                Content-Length: 0

                """;

        return String.format(
                //
                Locale.ROOT,
                template,
                // 1 - method
                "REGISTER",
                // 2- registrar
                this.conf.getRegistrar(),
                // 3 - sipID
                this.conf.getSipId(),
                // 4 - tag
                tag,
                // 5 - CallId
                callId,
                // 6 - public IP
                publicIp.getHostAddress(),
                // 7 - socket local port
                socket.getLocalPort(),
                // 8 - socket local address
                socket.getLocalAddress().getHostAddress());
    }

    private String getAuthorizationString(SipAuthenticationRequest sipAuthenticationRequest) {
        StringBuilder hash1Builder;
        String hash1;
        final var base64enc = Base64.getEncoder();

        try {
            // hash1: base64(md5(user:realm:pw))
            final var instance = MessageDigest.getInstance(
                    sipAuthenticationRequest.algorithm().toLowerCase(Locale.ROOT));
            final var hash1Contents = (this.conf.getLoginUserId() + ":" + sipAuthenticationRequest.realm()
                            + this.conf.getLoginPassword())
                    .getBytes(StandardCharsets.UTF_8);
            hash1 = base64enc.encodeToString(instance.digest(hash1Contents));

            // hash2: base64(md5(sipmethod:uri))

            // hash3: base64(md5(hash1:nonce:nc:cnonce:qop:hash2))

        } catch (NoSuchAlgorithmException nsae) {
            LOG.error("Problem with login algorithm", nsae);
        }

        // TODO: implement
        throw new UnsupportedOperationException(
                "not yet implemented: [io.github.bmarwell.sipper.impl.SipConnection::getAuthorizationString].");
    }

    public String getLogin(SipAuthenticationRequest sipAuthenticationRequest) {
        // TODO: implement
        throw new UnsupportedOperationException(
                "not yet implemented: [io.github.bmarwell.sipper.impl.SipConnection::getLogin].");
    }
}
