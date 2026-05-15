package id.payu.security.audit;

import id.payu.security.annotation.Audited;
import id.payu.security.config.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests for AuditAspect user extraction (IMP-065) and SLF4J fallback.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditAspectTest {

    @Mock
    private AuditLogPublisher publisher;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    @Mock
    private HttpServletRequest request;

    private SecurityProperties properties;
    private AuditAspect aspect;

    @BeforeEach
    void setUp() {
        properties = new SecurityProperties();
        properties.setAuditEnabled(true);
        aspect = new AuditAspect(properties, publisher);

        SecurityContextHolder.clearContext();
    }

    private void mockJoinPoint() throws NoSuchMethodException {
        when(joinPoint.getSignature()).thenReturn(signature);
        Method method = AuditAspectTest.class.getDeclaredMethod("annotatedMethod");
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.getTarget()).thenReturn(this);
    }

    @Audited(operation = Audited.AuditOperation.CREATE, entityType = "TestEntity")
    private void annotatedMethod() {
        // Annotated method for reflection
    }

    @Nested
    @DisplayName("IMP-065: extractUserId uses SecurityContext")
    class ExtractUserIdTest {

        @Test
        @DisplayName("Priority 1: Extracts userId from SecurityContext (JWT)")
        void extractsUserIdFromSecurityContext() throws Throwable {
            // Given: JWT-authenticated user in SecurityContext
            var auth = new UsernamePasswordAuthenticationToken("user-from-jwt", null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);

            ServletRequestAttributes attrs = new ServletRequestAttributes(request);
            RequestContextHolder.setRequestAttributes(attrs);

            mockJoinPoint();
            when(joinPoint.proceed()).thenReturn("result");

            // When
            aspect.auditOperation(joinPoint);

            // Then: audit event should have userId from SecurityContext
            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(publisher).publishSafe(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo("user-from-jwt");

            RequestContextHolder.resetRequestAttributes();
        }

        @Test
        @DisplayName("Priority 2: Falls back to X-User-Id header when SecurityContext empty")
        void fallsBackToXUserIdHeader() throws Throwable {
            // Given: no SecurityContext, but X-User-Id header present
            SecurityContextHolder.clearContext();

            when(request.getHeader("X-User-Id")).thenReturn("user-from-header");
            ServletRequestAttributes attrs = new ServletRequestAttributes(request);
            RequestContextHolder.setRequestAttributes(attrs);

            mockJoinPoint();
            when(joinPoint.proceed()).thenReturn("result");

            // When
            aspect.auditOperation(joinPoint);

            // Then: audit event should have userId from header
            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(publisher).publishSafe(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo("user-from-header");

            RequestContextHolder.resetRequestAttributes();
        }

        @Test
        @DisplayName("Priority 3: Returns 'anonymous' when no identity source available")
        void returnsAnonymousWhenNoIdentity() throws Throwable {
            // Given: no SecurityContext, no X-User-Id header
            SecurityContextHolder.clearContext();

            ServletRequestAttributes attrs = new ServletRequestAttributes(request);
            RequestContextHolder.setRequestAttributes(attrs);

            mockJoinPoint();
            when(joinPoint.proceed()).thenReturn("result");

            // When
            aspect.auditOperation(joinPoint);

            // Then: audit event should have "anonymous"
            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(publisher).publishSafe(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo("anonymous");

            RequestContextHolder.resetRequestAttributes();
        }

        @Test
        @DisplayName("Ignores 'anonymousUser' from SecurityContext (Spring default)")
        void ignoresAnonymousUserPrincipal() throws Throwable {
            // Given: SecurityContext has Spring's default "anonymousUser"
            var auth = new UsernamePasswordAuthenticationToken("anonymousUser", null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);

            when(request.getHeader("X-User-Id")).thenReturn("gateway-user");
            ServletRequestAttributes attrs = new ServletRequestAttributes(request);
            RequestContextHolder.setRequestAttributes(attrs);

            mockJoinPoint();
            when(joinPoint.proceed()).thenReturn("result");

            // When
            aspect.auditOperation(joinPoint);

            // Then: should skip anonymousUser and use X-User-Id header
            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(publisher).publishSafe(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo("gateway-user");

            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Nested
    @DisplayName("SLF4J fallback when Kafka publisher unavailable")
    class Slf4jFallbackTest {

        @Test
        @DisplayName("Works without publisher (no Kafka) — logs via SLF4J")
        void worksWithoutPublisher() throws Throwable {
            // Given: AuditAspect created without publisher (null)
            AuditAspect aspectWithoutKafka = new AuditAspect(properties, null);

            ServletRequestAttributes attrs = new ServletRequestAttributes(request);
            RequestContextHolder.setRequestAttributes(attrs);

            mockJoinPoint();
            when(joinPoint.proceed()).thenReturn("result");

            // When/Then: should NOT throw, should log via SLF4J
            Object result = aspectWithoutKafka.auditOperation(joinPoint);
            assertThat(result).isEqualTo("result");

            // Verify publisher was never called (it's null)
            verifyNoInteractions(publisher);

            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Nested
    @DisplayName("Audit disabled")
    class AuditDisabledTest {

        @Test
        @DisplayName("Skips audit when disabled in properties")
        void skipsAuditWhenDisabled() throws Throwable {
            properties.getAudit().setEnabled(false);

            mockJoinPoint();
            when(joinPoint.proceed()).thenReturn("result");

            Object result = aspect.auditOperation(joinPoint);
            assertThat(result).isEqualTo("result");

            verifyNoInteractions(publisher);
        }
    }
}
