package id.payu.transaction.application.cqrs.command;

import id.payu.account.grpc.AccountResponse;
import id.payu.account.grpc.AccountServiceGrpc;
import id.payu.account.grpc.GetAccountByNumberRequest;
import id.payu.grpc.starter.config.GrpcChannelFactory;
import id.payu.grpc.starter.interceptor.GrpcAuthInterceptor;
import id.payu.transaction.adapter.client.AccountServiceAdapter;
import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.application.service.AuthorizationService;
import id.payu.transaction.application.service.VelocityGuard;
import id.payu.transaction.domain.model.Money;
import id.payu.transaction.domain.model.TransactionStatus;
import id.payu.transaction.domain.port.out.BifastServicePort;
import id.payu.transaction.domain.port.out.RgsServicePort;
import id.payu.transaction.domain.port.out.RiskEvaluationPort;
import id.payu.transaction.domain.port.out.SknServicePort;
import id.payu.transaction.domain.port.out.TransactionEventPublisherPort;
import id.payu.transaction.domain.port.out.TransactionPersistencePort;
import id.payu.transaction.domain.port.out.WalletServicePort;
import id.payu.transaction.interfaces.dto.TransactionType;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GRPC-AUTH-001: an INTERNAL_TRANSFER resolves the recipient account id from
 * the account number over gRPC. account-service rejects anonymous callers, so
 * the adapter must forward the caller's bearer token — otherwise every
 * internal transfer ends FAILED with "Recipient account not found".
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class InternalTransferGrpcAuthTest {

    private static final String RECIPIENT_ACCOUNT_NUMBER = "1001001002";
    private static final UUID SENDER_ACCOUNT_ID = UUID.fromString("750e8400-e29b-41d4-a716-446655440001");
    private static final UUID RECIPIENT_ACCOUNT_ID = UUID.fromString("750e8400-e29b-41d4-a716-446655440004");
    private static final String TOKEN = "jwt-token-value";

    @Mock
    private TransactionPersistencePort transactionPersistencePort;
    @Mock
    private WalletServicePort walletServicePort;
    @Mock
    private BifastServicePort bifastServicePort;
    @Mock
    private SknServicePort sknServicePort;
    @Mock
    private RgsServicePort rgsServicePort;
    @Mock
    private TransactionEventPublisherPort eventPublisherPort;
    @Mock
    private VelocityGuard velocityGuard;
    @Mock
    private RiskEvaluationPort riskEvaluationPort;
    @Mock
    private id.payu.transaction.domain.port.out.StepUpVerificationPort stepUpVerificationPort;
    @Mock
    private id.payu.transaction.application.service.InboxService inboxService;
    @Mock
    private id.payu.transaction.application.service.AggregateResultService aggregateResultService;

    private Server server;
    private AccountServiceAdapter accountServiceAdapter;
    private InitiateTransferCommandHandler handler;

    @BeforeEach
    void startFakeAccountService() throws IOException {
        ServerInterceptor denyAnonymous = new ServerInterceptor() {
            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                    ServerCall<ReqT, RespT> call, Metadata headers,
                    ServerCallHandler<ReqT, RespT> next) {
                String authorization = headers.get(GrpcAuthInterceptor.AUTHORIZATION_KEY);
                if (authorization == null || !authorization.startsWith("Bearer ")) {
                    call.close(io.grpc.Status.UNAUTHENTICATED
                            .withDescription("Authentication required"), headers);
                    return new ServerCall.Listener<ReqT>() {
                    };
                }
                return next.startCall(call, headers);
            }
        };

        server = NettyServerBuilder.forPort(0)
                .intercept(denyAnonymous)
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
                })
                .build()
                .start();

        GrpcChannelFactory factory = new GrpcChannelFactory(
                List.of(new GrpcAuthInterceptor.ClientInterceptor()));

        accountServiceAdapter = new AccountServiceAdapter();
        try {
            ReflectionTestUtils.setField(accountServiceAdapter, "channelFactory", factory);
        } catch (IllegalArgumentException e) {
            // RED phase: the adapter has no factory yet
        }
        ReflectionTestUtils.setField(accountServiceAdapter, "accountServiceAddress",
                "static://localhost:" + server.getPort());
        ReflectionTestUtils.invokeMethod(accountServiceAdapter, "init");

        handler = new InitiateTransferCommandHandler(
                transactionPersistencePort,
                walletServicePort,
                bifastServicePort,
                sknServicePort,
                rgsServicePort,
                eventPublisherPort,
                accountServiceAdapter,
                new AuthorizationService(transactionPersistencePort, accountServiceAdapter),
                velocityGuard,
                riskEvaluationPort,
                stepUpVerificationPort,
                inboxService,
                aggregateResultService);

        when(velocityGuard.isAllowed(anyString(), any())).thenReturn(true);
        when(riskEvaluationPort.score(anyString(), any(), any())).thenReturn(0);
        when(transactionPersistencePort.save(any(TransactionEntity.class)))
                .thenAnswer(invocation -> {
                    TransactionEntity saved = invocation.getArgument(0);
                    if (saved.getId() == null) {
                        saved.setId(UUID.randomUUID());
                    }
                    return saved;
                });
    }

    @AfterEach
    void stop() {
        SecurityContextHolder.clearContext();
        if (accountServiceAdapter != null) {
            ReflectionTestUtils.invokeMethod(accountServiceAdapter, "destroy");
        }
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Test
    void internalTransferResolvesRecipientAccountAndCompletes() {
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(Jwt.withTokenValue(TOKEN)
                        .header("alg", "RS256")
                        .subject("8d177115-af87-47d5-8b94-a09e7cfa55bc")
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(300))
                        .build(), List.of()));

        InitiateTransferCommand command = new InitiateTransferCommand(
                SENDER_ACCOUNT_ID,
                RECIPIENT_ACCOUNT_NUMBER,
                Money.idr("1000"),
                "internal transfer",
                TransactionType.INTERNAL_TRANSFER,
                null,
                null,
                "idem-internal-001",
                SENDER_ACCOUNT_ID.toString(),
                null,
                null);

        InitiateTransferCommandResult result = handler.handle(command);

        assertThat(result.status()).isEqualTo(TransactionStatus.COMPLETED.name());
        verify(walletServicePort).transferBalance(
                eq(SENDER_ACCOUNT_ID.toString()),
                eq(RECIPIENT_ACCOUNT_ID.toString()),
                eq(Money.idr("1000").getAmount()),
                anyString());
    }
}
