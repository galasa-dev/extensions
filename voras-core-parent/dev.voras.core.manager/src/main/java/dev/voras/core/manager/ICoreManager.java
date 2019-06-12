package dev.voras.core.manager;

import javax.validation.constraints.NotNull;

/**
 * <p>
 * The Core Manager provides Tests with access to some of the most common
 * features within the eJAT Framework
 * </p>
 *
 * <p>
 * To gain access to the Core Manager, include the following in the test class:-
 * </p>
 * 
 * <pre>
 * &#64;CoreManager
 * public ICoreManager coreManager;
 * </pre>
 *
 * @author Michael Baylis
 * @See {@link CoreManager}
 *
 */
public interface ICoreManager {

    /**
     * Returns the Run Name of the Test Run, unique during the length of this Test
     * Run
     *
     * @return Unique Test Run name
     */
    @NotNull String getRunName();

}
