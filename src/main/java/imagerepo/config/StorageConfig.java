package imagerepo.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import imagerepo.services.ImageStorageService;
import imagerepo.services.LocalStorageService;
import imagerepo.services.S3ImageStorageService;
import imagerepo.services.utils.FileFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.io.File;
import java.util.Optional;

@Configuration
@PropertySource("file:${app.properties}")
public class StorageConfig {

    @Autowired
    private ApplicationContext context;

    @Value("${local.storage.dir:#{null}}")
    private Optional<String> storageDir;

    @Bean
    public ImageStorageService imageStorageService() {
        return storageDir
                .map(dir -> (ImageStorageService) new LocalStorageService(dir, fileFactory()))
                .orElseGet(() -> {
                    AwsConfig awsConfig = context.getBean(AwsConfig.class);
                    return new S3ImageStorageService(s3client(awsConfig), awsConfig.getBucket());
                });
    }

    public FileFactory fileFactory() {
        return File::new;
    }

    public AmazonS3 s3client(AwsConfig config) {
        return AmazonS3ClientBuilder.standard()
                .withRegion(Regions.fromName(config.getRegion()))
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(config.getAccessKey(), config.getSecretKey())))
                .build();
    }
}
