package engine.annot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME) // Critical: Keep this available at runtime for reflection
@Target(ElementType.TYPE)           // Critical: This annotation can only be used on Classes
public @interface Game {
    String name() default "Unknown Game";
}