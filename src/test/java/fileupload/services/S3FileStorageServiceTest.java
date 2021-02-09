package fileupload.services;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import fileupload.services.exceptions.FileNotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class S3FileStorageServiceTest {

    private String bucket;
    private AmazonS3 s3client;
    private S3FileStorageService target;

    @Before
    public void setup() {
        bucket = "testbucket";
        s3client = mock(AmazonS3.class);
        target = new S3FileStorageService(s3client, bucket);
    }

    @Test
    public void testSave() throws IOException {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        String filename = "filename";
        InputStream mockInputStream = mock(InputStream.class);

        when(file.getOriginalFilename()).thenReturn(filename);
        when(file.getInputStream()).thenReturn(mockInputStream);

        // Act
        target.save(file);

        // Assert
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);

        verify(s3client).putObject(captor.capture());

        PutObjectRequest value = captor.getValue();
        assertThat(value.getBucketName()).isEqualTo(bucket);
        assertThat(value.getKey()).isEqualTo(filename);
        assertThat(value.getInputStream()).isEqualTo(mockInputStream);
        assertThat(value.getMetadata()).isEqualTo(S3FileStorageService.defaultObjectMetadata);
        assertThat(value.getCannedAcl()).isEqualTo(CannedAccessControlList.PublicRead);
    }

    @Test
    public void testDelete() {
        // Arrange
        String filename = "filename";

        // Act
        target.delete(filename);

        // Assert
        verify(s3client).deleteObject(bucket, filename);
    }

    @Test
    public void testLoadHappyPath() throws IOException {
        // Arrange
        String filename = "filename";
        URL url = new URL("https://www.s3.com/" + bucket + "/" + filename);

        when(s3client.doesObjectExist(bucket, filename)).thenReturn(true);
        when(s3client.getUrl(bucket, filename)).thenReturn(url);

        // Act
        Resource resource = target.load(filename);

        // Assert
        assertThat(resource.getURL()).isEqualTo(url);
    }

    @Test
    public void testLoadWhenFileDoesNotExist() throws IOException {
        // Arrange
        String filename = "filename";
        URL url = new URL("https://www.s3.com/" + bucket + "/" + filename);

        when(s3client.doesObjectExist(bucket, filename)).thenReturn(false);
        when(s3client.getUrl(bucket, filename)).thenReturn(url);

        // Act & Assert
        assertThrows(
                FileNotFoundException.class,
                () ->  target.load(filename));
    }
}
