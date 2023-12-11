package com.alicloud.databox.opensdk;

public interface Consumer<T> {

    void accept(T value);
}
