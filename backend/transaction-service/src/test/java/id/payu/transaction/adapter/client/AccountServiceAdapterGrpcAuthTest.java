package id.payu.transaction.adapter.client;

import id.payu.account.grpc.AccountResponse;
import id.payu.account.grpc.AccountServiceGrpc;
import id.payu.account.grpc.GetAccountByNumberRequest;
import id.payu.account.grpc.GetAccountsByUserRequest;
import id.payu.account.grpc.GetUserProfileRequest;
import id.payu.account.grpc.UserProfileResponse;
import id.payu.grpc.starter.config.GrpcChannelFactory;
import id.payu.grpc.starter.interceptor.GrpcAuthInterceptor;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerInterceptor;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GRPC-AUTH-001: the account-service adapter must build its stub through the
 * Spring {@link GrpcChannelFactory} so the starter's client interceptors run —
 * the raw channel it used before carried no {@code authorization} metadata and
 * every account lookup was rejected with UNAUTHENTICATED.
 */
class AccountServiceAdapterGrpcAuthTest {

    private static final String ACCOUNT_NUMBER = "1001001002";
    private static final UUID RECIPIENT_ACCOUNT_ID = UUID.fromString("750e8400-e29b-41d4-a716-446655440004");
    private static final UUID CALLER_ACCOUNT_ID = UUID.fromString("750e8400-e29b-41d4-a716-446655440001");
    private static final String IAM_SUBJECT = "8d177115-af87-47d5-8b94-a09e7cfa55bc";
    private static final UUID ACCOUNT_SERVICE_USER_ID = UUID.fromString("650e8400-e29b-41d4-a716-446655440004");
    private static final String TOKEN = "jwt-token-value";

    private Server server;
    private AccountServiceAdapter adapter;
    private final AtomicReference<Metadata> lastHeaders = new AtomicReference<>();

    @BeforeEach
    void startFakeAccountService() throws IOException {
        ServerInterceptor captureHeaders = new ServerInterceptor() {
            @Override
            public <ReqT, RespT> io.grpc.ServerCall.Listener<ReqT> interceptCall(
                    io.grpc.ServerCall<ReqT, RespT> call, Metadata headers,
                    io.grpc.ServerCallHandler<ReqT, RespT> next) {
                lastHeaders.set(headers);
                return next.startCall(call, headers);
            }
        };

        server = NettyServerBuilder.forPort(0)
                .intercept(captureHeaders)
                .addService(new AccountServiceGrpc.AccountServiceImplBase() {
                    @Override
                    public void getAccountByNumber(GetAccountByNumberRequest request,
                                                   StreamObserver<AccountResponse> observer) {
                        observer.onNext(AccountResponse.newBuilder()
                                .setAccountId(RECIPIENT_ACCOUNT_ID.toString())
                                .setAccountNumber(request.getAccountNumber())
                                .build());
                        observer.onCompleted();
                    }

                    /** account-service resolves users by their IAM id (users.external_id). */
                    @Override
                    public void getUserProfile(GetUserProfileRequest request,
                                              StreamObserver<UserProfileResponse> observer) {
                        if (!IAM_SUBJECT.equals(request.getUserId())) {
                            observer.onError(io.grpc.Status.NOT_FOUND
                                    .withDescription("User not found: " + request.getUserId())
                                    .asRuntimeException());
                            return;
                        }
                        observer.onNext(UserProfileResponse.newBuilder()
                                .setUserId(ACCOUNT_SERVICE_USER_ID.toString())
                                .setExternalId(IAM_SUBJECT)
                                .build());
                        observer.onCompleted();
                    }

                    @Override
                    public void getAccountsByUser(GetAccountsByUserRequest request,
                                                 StreamObserver<AccountResponse> observer) {
                        if (!ACCOUNT_SERVICE_USER_ID.toString().equals(request.getUserId())) {
                            observer.onCompleted();
                            return;
                        }
                        observer.onNext(AccountResponse.newBuilder()
                                .setAccountId(CALLER_ACCOUNT_ID.toString())
                                .setUserId(ACCOUNT_SERVICE_USER_ID.toString())
                                .build());
                        observer.onCompleted();
                    }
                })
                .build()
                .start();

        GrpcChannelFactory factory = new GrpcChannelFactory(
                List.of(new GrpcAuthInterceptor.ClientInterceptor()));

        adapter = new AccountServiceAdapter();
        injectChannelFactory(adapter, factory);
        ReflectionTestUtils.setField(adapter, "accountServiceAddress",
                "static://localhost:" + server.getPort());
        adapter.init();
    }

    /**
     * RED phase: the adapter had no factory field at all, so the lookup ran on
     * a raw channel without the auth interceptor.
     */
    private static void injectChannelFactory(AccountServiceAdapter adapter, GrpcChannelFactory factory) {
        try {
            ReflectionTestUtils.setField(adapter, "channelFactory", factory);
        } catch (IllegalArgumentException e) {
            // no factory yet — the assertion on the forwarded token fails below
        }
    }

    @AfterEach
    void stop() {
        SecurityContextHolder.clearContext();
        if (adapter != null) {
            adapter.destroy();
        }
        if (server != null) {
            server.shutdownNow();
        }
    }

    private static JwtAuthenticationToken authenticatedCaller() {
        Jwt jwt = Jwt.withTokenValue(TOKEN)
                .header("alg", "RS256")
                .subject("8d177115-af87-47d5-8b94-a09e7cfa55bc")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        return new JwtAuthenticationToken(jwt, List.of());
    }

    @Test
    void forwardsTheCallersBearerTokenToAccountService() {
        SecurityContextHolder.getContext().setAuthentication(authenticatedCaller());

        UUID accountId = adapter.getAccountIdByNumber(ACCOUNT_NUMBER).orElseThrow();

        assertThat(accountId).isEqualTo(RECIPIENT_ACCOUNT_ID);
        assertThat(lastHeaders.get().get(GrpcAuthInterceptor.AUTHORIZATION_KEY))
                .isEqualTo("Bearer " + TOKEN);
    }

    /**
     * GRPC-AUTH-001 (R4): the scope lookup is keyed by the IAM subject the
     * token carries — account-service maps it to its own user id
     * (users.id) before answering with the user's accounts.
     */
    @Test
    void resolvesTheAccountsOfTheAuthenticatedIamSubject() {
        SecurityContextHolder.getContext().setAuthentication(authenticatedCaller());

        List<UUID> accountIds = adapter.getAccountIdsByUserId(IAM_SUBJECT);

        assertThat(accountIds).containsExactly(CALLER_ACCOUNT_ID);
    }
}
