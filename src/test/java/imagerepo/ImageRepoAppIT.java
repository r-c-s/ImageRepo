package imagerepo;

import com.google.common.collect.ImmutableList;
import imagerepo.models.ImageRecord;
import lombok.SneakyThrows;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@TestPropertySource("classpath:test.properties")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class ImageRepoAppIT {

    private static final TestRestTemplate restTemplate = new TestRestTemplate();

    @Before
    public void cleanup() {
        deleteAllImages();
    }

    @AfterClass
    public static void cleanupAfter() {
        deleteAllImages();
    }

    @Test
    public void testGetImages() {
        // Arrange
        String filename = "sandiego.jpg";
        String userId = "raphael";

        Date beforeUpload = new Date();
        uploadImageRequest(filename, userId);
        Date afterUpload = new Date();

        // Act
        ResponseEntity<List<ImageRecord>> response = getImagesRequest();

        // Assert
        List<ImageRecord> records = response.getBody();
        assertThat(records.size()).isEqualTo(1);

        ImageRecord record = records.get(0);
        assertThat(record.getName()).isEqualTo(filename);
        assertThat(record.getType()).isEqualTo("image/jpeg");
        assertThat(record.getUserId()).isEqualTo(userId);
        assertThat(record.getDateUploaded()).isBetween(beforeUpload, afterUpload);
        assertThat(record.getUploadStatus()).isEqualTo(ImageRecord.UploadStatus.succeeded);
        assertThat(record.getUrl()).isEqualTo(createUrl("/imagerepo/api/images/" + filename));
    }

    @Test
    public void testGetImage() throws IOException {
        // Arrange
        String filename = "sandiego.jpg";
        String userId = "raphael";

        uploadImageRequest(filename, userId);

        // Act
        ResponseEntity<Resource> response = getImageRequest(filename);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody().contentLength()).isGreaterThan(0);
    }

    @Test
    public void testUploadImageHappyPath() {
        // Arrange
        String filename = "sandiego.jpg";
        String userId = "raphael";

        // Act
        Date beforeUpload = new Date();
        ResponseEntity<ImageRecord> response = uploadImageRequest(filename, userId);
        Date afterUpload = new Date();

        // Assert
        String expectedUrl = createUrl("/imagerepo/api/images/sandiego.jpg");

        assertThat(response.getStatusCodeValue()).isEqualTo(201);
        assertThat(response.getHeaders().getLocation().toString()).isEqualTo(expectedUrl);

        ImageRecord record = response.getBody();
        assertThat(record.getName()).isEqualTo(filename);
        assertThat(record.getType()).isEqualTo("image/jpeg");
        assertThat(record.getUserId()).isEqualTo(userId);
        assertThat(record.getDateUploaded()).isBetween(beforeUpload, afterUpload);
        assertThat(record.getUploadStatus()).isEqualTo(ImageRecord.UploadStatus.succeeded);
        assertThat(record.getUrl()).isEqualTo(expectedUrl);
    }

    @Test
    public void testUploadImageAlreadyExists() {
        // Arrange
        String filename = "sandiego.jpg";
        String userId = "raphael";

        uploadImageRequest(filename, userId);

        // Act
        ResponseEntity<ImageRecord> response = uploadImageRequest(filename, userId);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(409);
    }

    @Test
    public void testDeleteImageHappyPath() {
        // Arrange
        String filename = "sandiego.jpg";
        String userId = "raphael";

        uploadImageRequest(filename, userId);

        // Act
        ResponseEntity<String> response = deleteImageRequest(filename, userId);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(204);

        // no longer exists in storage
        ResponseEntity<Resource> getImageResponse = getImageRequest(filename);
        assertThat(getImageResponse.getStatusCodeValue()).isEqualTo(404);

        // record no longer exists
        ResponseEntity<List<ImageRecord>> getImagesResponse = getImagesRequest();
        assertThat(getImagesResponse.getBody().stream().filter(record -> record.getName().equals(filename)).findAny())
                .isEmpty();
    }

    @Test
    public void testDeleteImageNotAllowed() {
        // Arrange
        String userId = "raphael";
        String differentUserId = "laura";
        String filename = "sandiego.jpg";

        uploadImageRequest(filename, userId);

        // Act
        ResponseEntity<String> response = deleteImageRequest(filename, differentUserId);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(403);

        // check that the image has not been deleted from storage
        ResponseEntity<Resource> getImageResponse = getImageRequest(filename);
        assertThat(getImageResponse.getStatusCodeValue()).isEqualTo(200);

        // check that the record has not been deleted
        ResponseEntity<List<ImageRecord>> getImagesResponse = getImagesRequest();
        assertThat(getImagesResponse.getBody()
                .stream()
                .filter(record -> record.getName().equals(filename))
                .findFirst())
                .isPresent();
    }

    @SneakyThrows
    private static String createUrl(String uri) {
        return "http://localhost:8081" + uri;
    }

    @SneakyThrows
    private static Resource getResourceFile(String name) {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        String path = new URI(classloader.getResource(name).toString()).getPath();
        File file = new File(path);
        return new FileSystemResource(file);
    }

    private static ResponseEntity<List<ImageRecord>> getImagesRequest() {
        return restTemplate.exchange(
                createUrl("/imagerepo/api/images/"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ImageRecord>>() { });
    }

    private static ResponseEntity<Resource> getImageRequest(String filename) {
        return restTemplate.getForEntity(
                createUrl("/imagerepo/api/images/" + filename),
                Resource.class);
    }

    private static ResponseEntity<ImageRecord> uploadImageRequest(String filename, String userId) {
        LinkedMultiValueMap<String, Resource> payload = new LinkedMultiValueMap<>();
        payload.put("file", ImmutableList.of(getResourceFile(filename)));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<LinkedMultiValueMap<String, Resource>> request = new HttpEntity<>(payload, headers);
        return restTemplate.postForEntity(
                createUrl("/imagerepo/api/images?userId=" + userId),
                request,
                ImageRecord.class);
    }

    private static ResponseEntity<String> deleteImageRequest(String filename, String userId) {
        return restTemplate.exchange(
                createUrl("/imagerepo/api/images/" + filename + "?userId=" + userId),
                HttpMethod.DELETE,
                null,
                String.class);
    }

    private static void deleteAllImages() {
        ResponseEntity<List<ImageRecord>> images = getImagesRequest();
        images.getBody().forEach(record -> deleteImageRequest(record.getName(), record.getUserId()));
    }
}
