package io.ejat.core;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.ejat.core.internal.CoreManagerField;
import io.ejat.framework.spi.ValidAnnotatedFields;

/**
 * <p>
 * Fill this field with the Core Manager instance.
 * </p>
 *
 * <p>
 * Will only populate public {@link ICoreManager} fields.
 * </p>
 *
 * @author Michael Baylis
 * @see {@link ICoreManager}
 *
 */
@Retention(RUNTIME)
@Target(FIELD)
@CoreManagerField
@ValidAnnotatedFields({ ICoreManager.class })
public @interface CoreManager {

}
