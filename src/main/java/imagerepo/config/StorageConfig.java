package imagerepo.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import imagerepo.services.FileFactory;
import imagerepo.services.ImageStorageService;
import imagerepo.services.LocalStorageService;
import imagerepo.services.S3ImageStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.Optional;
import java.util.stream.Stream;

@Configuration
public class StorageConfig {

    @Autowired
    private ApplicationContext context;

    @Value("${local.storage.dir:#{null}}")
    private Optional<String> storageDir;

    @Value("${amazon.aws.s3.bucket:#{null}}")
    private Optional<String> s3bucket;

    @Value("${amazon.aws.region:#{null}}")
    private Optional<String> region;

    @Value("${amazon.aws.accessKey:#{null}}")
    private Optional<String> accessKey;

    @Value("${amazon.aws.secretKey:#{null}}")
    private Optional<String> secretKey;

    @Bean
    public ImageStorageService imageStorageService() {
        return storageDir
                .map(dir -> (ImageStorageService) new LocalStorageService(dir, fileFactory()))
                .orElseGet(() -> s3bucket
                        .map(bucket -> new S3ImageStorageService(s3client(), bucket))
                        .orElseThrow(() -> new RuntimeException("Either local.storage.dir or amazon.aws.s3.bucket must be provided.")));
    }

    public FileFactory fileFactory() {
        return File::new;
    }

    public AmazonS3 s3client() {
        if (Stream.of(region, accessKey, secretKey).allMatch(Optional::isPresent)) {
            return AmazonS3ClientBuilder.standard()
                    .withRegion(Regions.fromName(region.get()))
                    .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey.get(), secretKey.get())))
                    .build();
        } else {
            throw new RuntimeException("AWS credentials not provided.");
        }
    }
}
