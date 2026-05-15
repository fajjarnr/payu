package id.payu.productcatalog.config;

import id.payu.productcatalog.application.service.ProductNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Nested
    @DisplayName("ProductNotFoundException handling")
    class ProductNotFound {

        @Test
        @DisplayName("Should return 404 with error response")
        void shouldReturn404() {
            ProductNotFoundException ex = new ProductNotFoundException("TEST_001");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleProductNotFound(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(Objects.requireNonNull(response.getBody()).status()).isEqualTo(404);
            assertThat(response.getBody().error()).isEqualTo("Product not found");
            assertThat(response.getBody().message()).contains("TEST_001");
            assertThat(response.getBody().timestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("IllegalArgumentException handling")
    class IllegalArgument {

        @Test
        @DisplayName("Should return 400 with error response")
        void shouldReturn400() {
            IllegalArgumentException ex = new IllegalArgumentException("Invalid product data");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleIllegalArgument(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(Objects.requireNonNull(response.getBody()).status()).isEqualTo(400);
            assertThat(response.getBody().error()).isEqualTo("Bad request");
            assertThat(response.getBody().message()).isEqualTo("Invalid product data");
        }
    }

    @Nested
    @DisplayName("MethodArgumentNotValidException handling")
    class Validation {

        @Test
        @DisplayName("Should return 400 with field error details")
        void shouldReturn400WithFieldErrors() throws Exception {
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
                    new Object(), "createProductRequest");
            bindingResult.addError(new FieldError("createProductRequest", "productCode",
                    "Product code is required"));
            bindingResult.addError(new FieldError("createProductRequest", "name",
                    "Name must not exceed 100 characters"));

            // Use real MethodParameter from a test method to avoid NPE
            java.lang.reflect.Method dummyMethod = Validation.class.getDeclaredMethod("dummyMethodForValidationTest", Object.class);
            org.springframework.core.MethodParameter methodParam =
                    new org.springframework.core.MethodParameter(dummyMethod, 0);
            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParam, bindingResult);

            ResponseEntity<GlobalExceptionHandler.ValidationErrorResponse> response =
                    handler.handleValidation(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            GlobalExceptionHandler.ValidationErrorResponse body =
                    Objects.requireNonNull(response.getBody());
            assertThat(body.status()).isEqualTo(400);
            assertThat(body.error()).isEqualTo("Validation failed");
            assertThat(body.fieldErrors()).containsKeys("productCode", "name");
            assertThat(body.fieldErrors().get("productCode")).isEqualTo("Product code is required");
        }

        @SuppressWarnings("unused")
        void dummyMethodForValidationTest(Object request) {
            // Method used only for creating a real MethodParameter in tests
        }
    }

    @Nested
    @DisplayName("Generic Exception handling")
    class Generic {

        @Test
        @DisplayName("Should return 500 for unexpected exceptions")
        void shouldReturn500() {
            RuntimeException ex = new RuntimeException("Something went wrong");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleGeneric(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(Objects.requireNonNull(response.getBody()).status()).isEqualTo(500);
            assertThat(response.getBody().error()).isEqualTo("Internal server error");
            assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
        }
    }
}
