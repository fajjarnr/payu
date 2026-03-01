package id.payu.grpc.starter.interceptor;

import id.payu.grpc.common.ErrorDetail;
import io.grpc.*;
import io.grpc.protobuf.StatusProto;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeoutException;

/**
 * gRPC interceptor for error handling.
 * Maps exceptions to appropriate gRPC status codes on server side.
 * Maps gRPC status codes back to domain exceptions on client side.
 *
 * @author PayU Logic Builder
 * @since 1.0.0
 */
@Slf4j
public class GrpcErrorHandlingInterceptor {

    /**
     * Server interceptor that catches exceptions and maps them to gRPC status codes.
     */
    public static class ServerInterceptor implements io.grpc.ServerInterceptor {

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                ServerCall<ReqT, RespT> call,
                Metadata headers,
                ServerCallHandler<ReqT, RespT> next) {

            ServerCall<ReqT, RespT> wrappedCall = new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
                @Override
                public void close(Status status, Metadata trailers) {
                    if (!status.isOk()) {
                        log.error("gRPC call failed - method: {}, status: {}, description: {}",
                                call.getMethodDescriptor().getFullMethodName(),
                                status.getCode(),
                                status.getDescription());
                    }
                    super.close(status, trailers);
                }
            };

            return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                    next.startCall(wrappedCall, headers)) {

                @Override
                public void onHalfClose() {
                    try {
                        super.onHalfClose();
                    } catch (IllegalArgumentException e) {
                        handleException(call, headers, e, Status.INVALID_ARGUMENT);
                    } catch (IllegalStateException e) {
                        handleException(call, headers, e, Status.FAILED_PRECONDITION);
                    } catch (SecurityException e) {
                        handleException(call, headers, e, Status.PERMISSION_DENIED);
                    } catch (UnsupportedOperationException e) {
                        handleException(call, headers, e, Status.UNIMPLEMENTED);
                    } catch (TimeoutException e) {
                        handleException(call, headers, e, Status.DEADLINE_EXCEEDED);
                    } catch (Exception e) {
                        handleException(call, headers, e, Status.INTERNAL);
                    }
                }
            };
        }

        private <ReqT, RespT> void handleException(
                ServerCall<ReqT, RespT> call,
                Metadata headers,
                Exception e,
                Status status) {

            log.error("Exception in gRPC call - method: {}, error: {}",
                    call.getMethodDescriptor().getFullMethodName(), e.getMessage(), e);

            ErrorDetail errorDetail = ErrorDetail.newBuilder()
                    .setCode(status.getCode().name())
                    .setMessage(e.getMessage())
                    .build();

            com.google.rpc.Status rpcStatus = com.google.rpc.Status.newBuilder()
                    .setCode(status.getCode().value())
                    .setMessage(e.getMessage())
                    .addDetails(com.google.protobuf.Any.pack(errorDetail))
                    .build();

            call.close(StatusProto.toStatusException(rpcStatus), headers);
        }
    }

    /**
     * Client interceptor that handles gRPC errors and maps them to domain exceptions.
     */
    public static class ClientInterceptor implements io.grpc.ClientInterceptor {

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                MethodDescriptor<ReqT, RespT> method,
                CallOptions callOptions,
                Channel next) {

            return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                    next.newCall(method, callOptions)) {

                @Override
                public void start(Listener<RespT> responseListener, Metadata headers) {
                    Listener<RespT> wrappedListener = new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {

                        @Override
                        public void onClose(Status status, Metadata trailers) {
                            if (!status.isOk()) {
                                log.error("gRPC client call failed - method: {}, status: {}, description: {}",
                                        method.getFullMethodName(),
                                        status.getCode(),
                                        status.getDescription());

                                // Map gRPC status to appropriate exception
                                RuntimeException exception = mapStatusToException(status, method);
                                if (exception != null) {
                                    throw exception;
                                }
                            }
                            super.onClose(status, trailers);
                        }
                    };
                    super.start(wrappedListener, headers);
                }
            };
        }

        private <ReqT, RespT> RuntimeException mapStatusToException(Status status, MethodDescriptor<ReqT, RespT> method) {
            return switch (status.getCode()) {
                case INVALID_ARGUMENT -> new IllegalArgumentException(
                        String.format("Invalid argument in %s: %s", method.getFullMethodName(), status.getDescription()));
                case FAILED_PRECONDITION -> new IllegalStateException(
                        String.format("Failed precondition in %s: %s", method.getFullMethodName(), status.getDescription()));
                case PERMISSION_DENIED -> new SecurityException(
                        String.format("Permission denied in %s: %s", method.getFullMethodName(), status.getDescription()));
                case UNAUTHENTICATED -> new SecurityException(
                        String.format("Authentication required for %s: %s", method.getFullMethodName(), status.getDescription()));
                case NOT_FOUND -> new IllegalArgumentException(
                        String.format("Resource not found in %s: %s", method.getFullMethodName(), status.getDescription()));
                case ALREADY_EXISTS -> new IllegalStateException(
                        String.format("Resource already exists in %s: %s", method.getFullMethodName(), status.getDescription()));
                case RESOURCE_EXHAUSTED -> new IllegalStateException(
                        String.format("Resource exhausted in %s: %s", method.getFullMethodName(), status.getDescription()));
                case DEADLINE_EXCEEDED -> new RuntimeException(
                        String.format("Deadline exceeded in %s: %s", method.getFullMethodName(), status.getDescription()));
                case UNAVAILABLE -> new RuntimeException(
                        String.format("Service unavailable for %s: %s", method.getFullMethodName(), status.getDescription()));
                default -> new RuntimeException(
                        String.format("gRPC error in %s: %s - %s", method.getFullMethodName(), status.getCode(), status.getDescription()));
            };
        }
    }
}
