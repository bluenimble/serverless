package com.bluenimble.platform.tools.binary.impls.netty.pool;

public interface ObjectFactory<T> {

    T create();

    void destroy(T t);

    boolean validate(T t);

}
