package id.payu.auth.config;

import id.payu.api.common.exception.problem.Rfc9457GlobalExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@org.springframework.core.annotation.Order(0)
public class Rfc9457AuthExceptionHandler extends Rfc9457GlobalExceptionHandler {
}
