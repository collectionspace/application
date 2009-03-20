package org.collectionspace.xxu.js.api;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(value={ElementType.TYPE,ElementType.METHOD,ElementType.FIELD})
public @interface JavascriptVisible {}
