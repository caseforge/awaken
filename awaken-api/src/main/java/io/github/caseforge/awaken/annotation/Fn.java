/**
 * 
 */
package io.github.caseforge.awaken.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 用于处理方法重写时的路径冲突问题
 */
@Retention(RUNTIME)
@Target({ METHOD })
public @interface Fn {

    String value();
}
