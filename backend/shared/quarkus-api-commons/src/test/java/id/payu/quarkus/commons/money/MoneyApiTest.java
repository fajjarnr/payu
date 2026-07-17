package id.payu.quarkus.commons.money;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;

class MoneyApiTest {

    @Test
    void shouldNotExposeFloatingPointArithmetic() {
        assertFalse(Arrays.stream(Money.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("multiply") || method.getName().equals("divide"))
                .flatMap(method -> Arrays.stream(method.getParameterTypes()))
                .anyMatch(type -> type == double.class || type == float.class));
    }
}
