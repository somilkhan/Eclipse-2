package io.github.somilkhan.instaeclipse2.compat.callbacks;

public interface IXUnhook<T> {
    T getCallback();
    void unhook();
}
