package dev.lucaargolo.nexo.api;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mod {

    @NotNull String value();

    @NotNull String name() default "";

    @NotNull String description() default "";

    @NotNull String version() default "0.0.0";

    @NotNull String[] authors() default {};

}
