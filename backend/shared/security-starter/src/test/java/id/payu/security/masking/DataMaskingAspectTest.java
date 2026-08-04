package id.payu.security.masking;

import id.payu.security.config.SecurityProperties;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class DataMaskingAspectTest {

    @Test
    void masksEnumWithoutReflectingIntoJdkInternals() throws Exception {
        DataMaskingAspect aspect = new DataMaskingAspect(new SecurityProperties());
        Method maskValue = DataMaskingAspect.class.getDeclaredMethod("maskValue", Object.class);
        maskValue.setAccessible(true);

        Object result = maskValue.invoke(aspect, SampleType.INTERNAL_TRANSFER);

        assertThat(result).isEqualTo("INTERNAL_TRANSFER");
    }

    private enum SampleType {
        INTERNAL_TRANSFER
    }
}
