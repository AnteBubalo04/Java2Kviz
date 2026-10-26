package hr.algebra.kviz.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Oznacava klase, metode i polja koja sadrze specificnu poslovnu logiku igre.
 * DocumentationGenerator ovu anotaciju cita preko Reflection API-ja i opis
 * ukljucuje u generiranu dokumentaciju.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
public @interface GameDoc {
    String value();
}
