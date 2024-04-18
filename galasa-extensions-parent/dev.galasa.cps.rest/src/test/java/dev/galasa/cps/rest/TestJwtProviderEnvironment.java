/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.cps.rest;

import org.junit.Test;
import static org.assertj.core.api.Assertions.*;


public class TestJwtProviderEnvironment {
    
    @Test
    public void testCanGetJwtFromEnvironment() throws Exception {
        String dummyJwt = "A fake jwt";
        System.setProperty("GALASA_JWT", dummyJwt);
        JwtProvider provider = new JwtProviderEnvironment();
        String jwtGot = provider.getJwt();
        assertThat(jwtGot).isEqualTo(dummyJwt);
    }
}
