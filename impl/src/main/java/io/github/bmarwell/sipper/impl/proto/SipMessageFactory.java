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

import io.github.bmarwell.sipper.api.SipConfiguration;
import io.github.bmarwell.sipper.impl.internal.ConnectedSipConnection;
import io.github.bmarwell.sipper.impl.internal.DefaultRegisteredSipConnection;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.random.RandomGeneratorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SipMessageFactory {

    private static final Logger LOG = LoggerFactory.getLogger(SipMessageFactory.class);

    private final String registrar;
    private final String sipId;

    public SipMessageFactory(SipConfiguration conf) {
        this.registrar = conf.getRegistrar();
        this.sipId = conf.getSipId();
    }

    public SipMessageFactory(String registrar, String sipId) {
        this.registrar = registrar;
        this.sipId = sipId;
    }

    public String getRegisterPreflight(ConnectedSipConnection sipConnection) {
        final var template =
                """
                %1$s sip:%2$s SIP/2.0
                CSeq: %9$s %1$s
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
                this.registrar,
                // 3 - sipID
                this.sipId,
                // 4 - tag
                sipConnection.getTag(),
                // 5 - CallId
                sipConnection.getCallId(),
                // 6 - public IP
                sipConnection.getPublicIp().getHostAddress(),
                // 7 - socket local port
                sipConnection.getSocket().getLocalPort(),
                // 8 - socket local address
                sipConnection.getSocket().getLocalAddress().getHostAddress(),
                // 9 - CSeq
                sipConnection.getAndUpdateCseq());
    }

    private AuthorizationResponse getAuthorizationString(
            SipAuthenticationRequest sipAuthenticationRequest,
            String uri,
            long nc,
            String qop,
            String loginUserId,
            String loginPassword) {
        final var ncHex = HexFormat.of().toHexDigits(nc, 6);
        final var base64enc = Base64.getEncoder();

        final var cnonceBytes = new byte[12];
        RandomGeneratorFactory.getDefault().create().nextBytes(cnonceBytes);
        final var cnonce = base64enc.encodeToString(cnonceBytes);

        try {
            // hash1: base64(md5(user:realm:pw))
            final var h1md5 = MessageDigest.getInstance(
                    sipAuthenticationRequest.algorithm().toLowerCase(Locale.ROOT));
            final var hash1Contents = (loginUserId + ":" + sipAuthenticationRequest.realm() + ":" + loginPassword)
                    .getBytes(StandardCharsets.UTF_8);
            final var hash1 = base64enc.encodeToString(h1md5.digest(hash1Contents));

            // hash2: base64(md5(sipmethod:uri))
            final var h2md5 = MessageDigest.getInstance(
                    sipAuthenticationRequest.algorithm().toLowerCase(Locale.ROOT));
            final var hash2Contents = ("REGISTER:" + uri).getBytes(StandardCharsets.UTF_8);
            final var hash2 = base64enc.encodeToString(h2md5.digest(hash2Contents));

            // hash3: base64(md5(hash1:nonce:nc:cnonce:qop:hash2))
            final var h3md5 = MessageDigest.getInstance(
                    sipAuthenticationRequest.algorithm().toLowerCase(Locale.ROOT));
            final var h3Contents = (hash1 + ":" + sipAuthenticationRequest.nonce() + ":" + ncHex + ":" + cnonce + ":"
                            + qop + ":" + hash2)
                    .getBytes(StandardCharsets.UTF_8);
            final var hashResponse = base64enc.encodeToString(h3md5.digest(h3Contents));

            return new AuthorizationResponse(hashResponse, cnonce);
        } catch (NoSuchAlgorithmException nsae) {
            LOG.error("Problem with login algorithm", nsae);
            throw new IllegalArgumentException(
                    "Problem with login algorithm: " + sipAuthenticationRequest.algorithm(), nsae);
        }
    }

    public LoginRequest getLogin(
            SipAuthenticationRequest sipAuthenticationRequest,
            ConnectedSipConnection sipConnection,
            String loginUserId,
            String loginPassword) {
        final var qop = "auth";
        final var nc = 1L;
        final var ncHex = HexFormat.of().toHexDigits(nc, 8);

        final var authorizationResponse =
                getAuthorizationString(sipAuthenticationRequest, this.registrar, nc, qop, loginUserId, loginPassword);

        final var authValue = String.format(
                Locale.ROOT,
                "Digest realm=\"%1$s\", nonce=\"%2$s\", algorithm=%3$s, username=\"%4$s\", uri=\"sip:%5$s\", response=\"%6$s\", cnonce=\"%8$s\", nc=%8$s, qop=%9$s",
                // 1 - realm
                sipAuthenticationRequest.realm(),
                // 2 - nonce
                sipAuthenticationRequest.nonce(),
                // 3 - algorithm
                sipAuthenticationRequest.algorithm(),
                // 4 - username
                loginUserId,
                // 5 - registrar
                this.registrar,
                // 6 - response (see #getAuthorizationString)
                authorizationResponse.response(),
                // 7 - client nonce
                authorizationResponse.clientNonce(),
                // 8 - nc
                ncHex,
                // 9 - qop
                qop
                // end
                );

        String template =
                """
                %1$s sip:%2$s SIP/2.0
                Via: SIP/2.0/TCP %8$s:%7$s;alias;branch=z9hG4bK.8i7nkaF9s;rport
                From: <sip:%3$s@%2$s>;tag=%4$s
                To: sip:%3$s@%2$s
                CSeq: %9$s %1$s
                Call-ID: %5$s
                Max-Forwards: 70
                Supported: replaces, outbound, gruu, path, record-aware
                Contact: <sip:%3$s@%6$s:%7$s;transport=tcp>
                Expires: 600
                User-Agent: SIPper/0.1.0
                Content-Length: 0
                Authorization: %10$s


                """;

        final var register = String.format(
                //
                Locale.ROOT,
                template,
                // 1 - method
                "REGISTER",
                // 2- registrar
                this.registrar,
                // 3 - sipID
                this.sipId,
                // 4 - tag
                sipConnection.getTag(),
                // 5 - CallId
                sipConnection.getCallId(),
                // 6 - public IP
                sipConnection.getPublicIp().getHostAddress(),
                // 7 - socket local port
                sipConnection.getSocket().getLocalPort(),
                // 8 - socket local address
                sipConnection.getSocket().getLocalAddress().getHostAddress(),
                // 9 - CSeq
                sipConnection.getAndUpdateCseq(),
                // 10 - authValue
                authValue
                // end
                );

        return new LoginRequest(register, authValue);
    }

    public String getUnregister(DefaultRegisteredSipConnection registeredSipConnection) {
        final var template =
                """
                %1$s sip:%2$s SIP/2.0
                CSeq: %9$s %1$s
                Via: SIP/2.0/TCP %8$s:%7$s;alias;branch=z9hG4bK.8i7nkaF9s;rport
                From: <sip:%3$s@%2$s>;tag=%4$s
                To: sip:%3$s@%2$s
                Call-ID: %5$s
                Max-Forwards: 70
                Supported: replaces, outbound, gruu, path, record-aware
                Contact: <sip:%3$s@%6$s:%7$s;transport=tcp>;q=1
                Authorization: %10$s
                Expires: 0
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
                this.registrar,
                // 3 - sipID
                this.sipId,
                // 4 - tag
                registeredSipConnection.getTag(),
                // 5 - CallId
                registeredSipConnection.getCallId(),
                // 6 - public IP
                registeredSipConnection.getPublicIp().getHostAddress(),
                // 7 - socket local port
                registeredSipConnection.getSocket().getLocalPort(),
                // 8 - socket local address
                registeredSipConnection.getSocket().getLocalAddress().getHostAddress(),
                // 9 - CSeq
                registeredSipConnection.getAndUpdateCseq(),
                // 10 - auth,
                registeredSipConnection.getAuthorization().orElse("")
                // end
                );
    }

    /**
     *
     * @param response The response string (hashed and hashed again...).
     * @param clientNonce The client nonce.
     */
    private record AuthorizationResponse(String response, String clientNonce) {}

    public record RegisterPreflightRequest(String message, InetAddress publicIp) {}

    public record LoginRequest(String message, String authorization) {}
}
