package dev.huskuraft.universal.api.platform;

import com.google.auto.service.AutoService;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

@Retention(CLASS)
@Target(TYPE)
public @interface Entrypoint {
  AutoService auto() default @AutoService(Entrance.class);

}
