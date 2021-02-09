package fileupload.config;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import fileupload.services.FileStorageService;
import fileupload.services.LocalStorageService;
import fileupload.services.S3FileStorageService;
import fileupload.services.utils.FileFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.io.File;
import java.util.Optional;

@Configuration
@PropertySource("file:${app.properties}")
public class StorageConfig {

    @Autowired
    private AwsConfig awsConfig;

    @Value("${local.storage.dir:#{null}}")
    private Optional<String> storageDir;

    @Bean
    public FileStorageService imageStorageService() {
        return storageDir.map(dir -> (FileStorageService) new LocalStorageService(dir, fileFactory()))
                .orElseGet(() -> new S3FileStorageService(s3client(awsConfig), awsConfig.getBucket()));
    }

    public FileFactory fileFactory() {
        return File::new;
    }

    public AmazonS3 s3client(AwsConfig config) {
        AWSCredentialsProvider credentials = new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(config.getAccessKey(), config.getSecretKey()));
        return AmazonS3ClientBuilder.standard()
                .withRegion(Regions.fromName(config.getRegion()))
                .withCredentials(credentials)
                .build();
    }
}
