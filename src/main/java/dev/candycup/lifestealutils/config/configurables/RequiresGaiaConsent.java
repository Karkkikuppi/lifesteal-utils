package dev.candycup.lifestealutils.config.configurables;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RequiresGaiaConsent {
   /**
    * The forced state used while Gaia consent is disabled.
    */
   boolean forcedState() default false;
}