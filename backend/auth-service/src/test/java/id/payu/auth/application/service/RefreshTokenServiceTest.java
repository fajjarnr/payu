package id.payu.auth.application.service;

import id.payu.cache.service.DistributedCache;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private DistributedCache distributedCache;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    void createRefreshToken_shouldStoreTokenAndReverseIndexInCache() {
        refreshTokenService.createRefreshToken("user-1");

        verify(distributedCache, times(2)).put(anyString(), any(), any(Duration.class));
    }
}
