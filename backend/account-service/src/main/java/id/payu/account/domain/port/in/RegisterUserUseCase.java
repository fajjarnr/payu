package id.payu.account.domain.port.in;

import id.payu.account.domain.model.User;
import id.payu.account.interfaces.dto.RegisterUserRequest;
import java.util.concurrent.CompletableFuture;

public interface RegisterUserUseCase {
    CompletableFuture<User> registerUser(RegisterUserRequest command);
}
