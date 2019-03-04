package io.ejat.core.manager;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.apache.commons.logging.Log;

import io.ejat.core.manager.internal.CoreManagerField;
import io.ejat.framework.spi.ValidAnnotatedFields;

/**
 * <p>
 * Fill this field with the Logger instance for this Test Class.
 * </p>
 * <p>
 * Will only populate public {@link org.apache.commons.logging.Log} fields.
 * </p>
 *
 * @see {@link org.apache.commons.logging.Log}
 * @author Michael Baylis
 *
 */
@Retention(RUNTIME)
@Target(FIELD)
@CoreManagerField
@ValidAnnotatedFields({ Log.class })
public @interface Logger {

}
