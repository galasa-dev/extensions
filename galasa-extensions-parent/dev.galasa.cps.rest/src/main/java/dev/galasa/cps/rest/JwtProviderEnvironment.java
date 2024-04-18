package dev.galasa.cps.rest;

import dev.galasa.framework.spi.ConfigurationPropertyStoreException;

public class JwtProviderEnvironment implements JwtProvider {

    @Override
    public String getJwt() throws ConfigurationPropertyStoreException {
        String jwt = System.getProperty("GALASA_JWT");
        if (jwt==null || jwt.isEmpty()) {
            String msg = Errors.ERROR_GALASA_CANT_GET_JWT_TOKEN.getMessage();
            throw new ConfigurationPropertyStoreException(msg);
        }
        return jwt;
    }

}
