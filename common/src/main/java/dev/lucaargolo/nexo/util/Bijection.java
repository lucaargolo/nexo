package dev.lucaargolo.nexo.util;

public interface Bijection<A, B> {
    B forward(A a);
    A backward(B b);
}
