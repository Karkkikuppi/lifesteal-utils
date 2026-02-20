package dev.candycup.lifestealutils.config.configurables;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface ConfigurableBoolean {
   /**
    * The full path to this entry, separated by dots. Goes in this order: category.group.entry
    */
   String location();
}
