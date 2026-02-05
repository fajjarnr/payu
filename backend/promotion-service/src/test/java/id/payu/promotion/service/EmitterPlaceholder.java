package id.payu.promotion.service;

public interface EmitterPlaceholder<T> {
    void send(T msg);
}
