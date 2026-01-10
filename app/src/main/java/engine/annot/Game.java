package engine.annot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class as a game implementation.
 * The annotated class must extend {@link engine.core.CoreGame} to be valid.
 * 
 * This annotation is used by the engine loader to automatically discover and instantiate game classes.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Game {
    /**
     * The name of the game.
     * 
     * @return the game name, defaults to "Unknown Game"
     */
    String name() default "Unknown Game";
}