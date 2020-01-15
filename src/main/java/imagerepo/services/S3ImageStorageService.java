package imagerepo.services;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import imagerepo.services.exceptions.ImageNotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class S3ImageStorageService implements ImageStorageService {

    protected static ObjectMetadata defaultObjectMetadata = new ObjectMetadata();

    private AmazonS3 s3client;
    private String bucket;

    public S3ImageStorageService(AmazonS3 s3client, String bucket) {
        this.s3client = s3client;
        this.bucket = bucket;
    }

    @Override
    public void save(MultipartFile file) throws IOException {
        PutObjectRequest request =
                new PutObjectRequest(bucket, file.getOriginalFilename(), file.getInputStream(), defaultObjectMetadata)
                        .withCannedAcl(CannedAccessControlList.PublicRead);
        s3client.putObject(request);
    }

    @Override
    public void delete(String fileName) {
        s3client.deleteObject(bucket, fileName);
    }

    @Override
    public Resource load(String filename) throws IOException {
        if (!s3client.doesObjectExist(bucket, filename)) {
            throw new ImageNotFoundException(filename);
        }
        return new UrlResource(s3client.getUrl(bucket, filename));
    }
}