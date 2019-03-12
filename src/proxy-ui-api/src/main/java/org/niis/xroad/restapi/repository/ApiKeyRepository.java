/**
 * The MIT License
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.niis.xroad.restapi.repository;

import org.niis.xroad.restapi.auth.Role;
import org.niis.xroad.restapi.domain.ApiKeyData;
import org.niis.xroad.restapi.exceptions.InvalidParametersException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dummy in-memory repository for api keys
 */
@Component
public class ApiKeyRepository {

    @Autowired
    private PasswordEncoder passwordEncoder;

    Set<ApiKeyData> keys = Collections.newSetFromMap(new ConcurrentHashMap<ApiKeyData, Boolean>());

    /**
     * Api keys are created with UUID.randomUUID which uses SecureRandom,
     * which is cryptographically secure.
     * @return
     */
    private String createApiKey() {
        return UUID.randomUUID().toString();
    }

    /**
     * create api key with one role
     */
    public String create(String role) {
        return create(Collections.singletonList(role));
    }

    /**
     * create api key with collection of roles
     * @return plaintext key
     */
    public String create(Collection<String> roles) {
        if (roles.isEmpty()) {
            throw new InvalidParametersException("missing roles");
        }
        for (String roleParam: roles) {
            if (!Role.contains(roleParam)) {
                throw new InvalidParametersException("invalid role " + roleParam);
            }
        }
        String key = createApiKey();
        String encoded = encode(key);
        ApiKeyData apiKeyData = new ApiKeyData(encoded, Collections.unmodifiableCollection(roles));
        keys.add(apiKeyData);
        return key;
    }

    private String encode(String key) {
        return passwordEncoder.encode(key);
    }

    /**
     * this scales linearly for key number.
     * Options:
     * - add "username"
     * - accept slowness, will not have millions of api keys
     * - get rid of encoding
     * @param key
     * @return
     */
    public ApiKeyData get(String key) {
        for (ApiKeyData apiKeyData: keys) {
            if (passwordEncoder.matches(key, apiKeyData.getEncodedKey())) {
                return apiKeyData;
            }
        }
        return null;
    }

}
