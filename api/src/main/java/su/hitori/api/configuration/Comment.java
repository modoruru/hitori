package su.hitori.api.configuration;

import su.hitori.api.configuration.serializer.Serializer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * By adding {@link Comment} annotation to a class or field, you can add comments to nodes.<br>
 * {@link Serializer} does not guarantee serialization of comments, as not all formats support comments.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Comment {

    String value();

}
