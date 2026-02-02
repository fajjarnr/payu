package id.payu.backoffice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@ConfigurationPropertiesScan
@ComponentScan(
    basePackages = "id.payu",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "id\\.payu\\.api\\.common\\.openapi\\..*"
    )
)
public class BackofficeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackofficeServiceApplication.class, args);
    }
}
