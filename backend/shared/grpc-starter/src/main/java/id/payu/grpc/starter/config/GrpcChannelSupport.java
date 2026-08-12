package id.payu.grpc.starter.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.AbstractStub;

import java.util.concurrent.TimeUnit;

/**
 * GRPC-011: shared channel/stub construction for PayU gRPC clients.
 * Adapters used to build raw {@code ManagedChannelBuilder} channels with no
 * deadline — a hung wallet call could block a money path forever. All clients
 * must route through this helper so the deadline applies everywhere.
 */
public final class GrpcChannelSupport {

    private GrpcChannelSupport() {
    }

    public static final int DEFAULT_DEADLINE_SECONDS = 30;

    /**
     * Build a plaintext channel from a {@code static://host:port} target.
     */
    public static ManagedChannel channel(String target) {
        String address = target.replace("static://", "");
        String[] parts = address.split(":");
        String host = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9090;
        return ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
    }

    /**
     * Attach a bounded deadline to a blocking stub so a dead wallet never
     * hangs the caller indefinitely (GRPC-011).
     */
    public static <T extends AbstractStub<T>> T withDeadline(T stub, int deadlineSeconds) {
        return stub.withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS);
    }
}
