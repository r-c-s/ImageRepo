package fileupload.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("file:${app.properties}")
@Getter
public class AwsConfig {

    @Value("${amazon.aws.region:#{null}}")
    private String region;

    @Value("${amazon.aws.accessKey:#{null}}")
    private String accessKey;

    @Value("${amazon.aws.secretKey:#{null}}")
    private String secretKey;

    @Value("${amazon.aws.s3.bucket:#{null}}")
    private String bucket;
}
