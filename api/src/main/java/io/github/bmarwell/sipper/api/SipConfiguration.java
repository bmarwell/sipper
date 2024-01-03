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

import java.time.Duration;
import org.immutables.value.Value;

/**
 * Configuration used by SipClientBuilder, which will be passed to the created SipClient instance.
 */
@Value.Immutable
@Value.Style(jdkOnly = true, stagedBuilder = true)
public interface SipConfiguration {

    /**
     * Registrar is the domain of your SIP endpoint.
     *
     * <p>Typical values:</p>
     * <ul>
     *     <li>{@code tel.t-online.de} (Deutsche Telekom)</li>
     *     <li>{@code sip.kabelfon.vodafone.de} (Vodafone Cable)</li>
     * </ul>
     *
     * <p>You can check whether you have a valid registrar by executing:
     * {@code dig +short SRV _sip._udp.${domain}}.</p>
     *
     * @return the address of the registrar.
     */
    String getRegistrar();

    /**
     * The ID of your SIP account, which is usually the phone number in a specific format.
     *
     * <p>Typical values:</p>
     * <ul>
     *     <li>{@code 012345678901} (Deutsche Telekom)</li>
     *     <li>{@code +4912345678901} (Vodafone Cable)</li>
     * </ul>
     *
     * @return the SIP ID, which should usually correspond to a correctly formatted phone number.
     */
    String getSipId();

    @Value.Default
    default Duration getConnectTimeout() {
        return Duration.ofMillis(2_000L);
    }

    /**
     * The user to be used for Login. Depending on your provider, this could be the same
     * as the phone number, or your email-address or anything else.
     *
     * <p>Typical values:</p>
     * <ul>
     *     <li>{@code login email address} (Deutsche Telekom)</li>
     *     <li>{@code +4912345678901} (Vodafone Cable)</li>
     * </ul>
     *
     * @return the login user ID for authorization and authentication.
     */
    String getLoginUserId();

    String getLoginPassword();

    @Value.Default
    default Duration getReadTimeout() {
        return Duration.ofMillis(2_000);
    }
}
