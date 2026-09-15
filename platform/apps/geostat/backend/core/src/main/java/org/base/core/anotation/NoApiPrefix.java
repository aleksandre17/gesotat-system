package org.base.core.anotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controllers annotated with @NoApiPrefix will NOT get the /api/v1 prefix
 * from CustomRequestMappingHandlerMapping.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NoApiPrefix {}

