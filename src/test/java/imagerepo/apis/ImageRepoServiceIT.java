package imagerepo.apis;

import imagerepo.auth.models.LoginCredentials;
import imagerepo.models.ImageRecord;
import net.bytebuddy.utility.RandomString;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@TestPropertySource("file:${app.properties}")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class ImageRepoServiceIT {

    private static LoginCredentials admin = new LoginCredentials("testAdmin", "password");
    private static LoginCredentials userA = new LoginCredentials(RandomString.make(), RandomString.make());
    private static LoginCredentials userB = new LoginCredentials(RandomString.make(), RandomString.make());

    @Value("${server.port}")
    private int port;

    @Value("${services.auth.baseUrl}")
    private String authServiceBaseUrl;
    private String imageRepoServiceBaseUrl;

    private AuthService authService;
    private ImageRepoService target;

    @Before
    public void setup() {
        RestTemplate template = new TestRestTemplate().getRestTemplate();
        imageRepoServiceBaseUrl = "http://localhost:" + port;
        authService = new AuthService(authServiceBaseUrl, template);
        target = new ImageRepoService(imageRepoServiceBaseUrl, authService, template);

        // register test users
        assertThat(authService.register(userA).getStatusCodeValue()).isEqualTo(200);
        assertThat(authService.register(userB).getStatusCodeValue()).isEqualTo(200);
    }

    @Rule
    public TestRule watchman = new TestWatcher() {
        // unlike @After, this also runs when exceptions are thrown inside test methods
        @Override
        protected void finished(Description ignored) {
            // delete test users
            Stream.of(userA, userB).forEach(user ->
                    assertThat(authService.delete(admin, user.getUsername()).getStatusCodeValue())
                            .satisfiesAnyOf(
                                    status -> assertThat(status).isEqualTo(200),
                                    status -> assertThat(status).isEqualTo(404)));
            // delete all images
            target.getImagesRequest().getBody().stream()
                    .map(ImageRecord::getName)
                    .forEach(name -> target.deleteImageRequest(admin, name));
        }
    };

    @Test
    public void testGetImages() {
        // Arrange
        String filename = "san diego.jpg";

        Date beforeUpload = new Date();
        target.uploadImageRequest(userA, filename);
        Date afterUpload = new Date();

        // Act
        ResponseEntity<List<ImageRecord>> response = target.getImagesRequest();

        // Assert
        List<ImageRecord> records = response.getBody();
        assertThat(records.size()).isEqualTo(1);

        ImageRecord record = records.get(0);
        assertThat(record.getName()).isEqualTo(filename);
        assertThat(record.getType()).isEqualTo("image/jpeg");
        assertThat(record.getUsername()).isEqualTo(userA.getUsername());
        assertThat(record.getDateUploaded()).isBetween(beforeUpload, afterUpload);
        assertThat(record.getUploadStatus()).isEqualTo(ImageRecord.UploadStatus.succeeded);
        assertThat(record.getUrl()).isEqualTo(imageRepoServiceBaseUrl + "/imagerepo/api/images/san+diego.jpg");
    }

    @Test
    public void testGetImage() throws IOException {
        // Arrange
        String filename = "san diego.jpg";
        target.uploadImageRequest(userA, filename);

        // Act
        ResponseEntity<Resource> response = target.getImageRequest(filename);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody().contentLength()).isGreaterThan(0);
    }

    @Test
    public void testUploadImageHappyPath() {
        // Arrange
        String filename = "san diego.jpg";

        // Act
        Date beforeUpload = new Date();
        ResponseEntity<ImageRecord> response = target.uploadImageRequest(userA, filename);
        Date afterUpload = new Date();

        // Assert
        String expectedUrl = imageRepoServiceBaseUrl + "/imagerepo/api/images/san+diego.jpg";

        assertThat(response.getStatusCodeValue()).isEqualTo(201);
        assertThat(response.getHeaders().getLocation().toString()).isEqualTo(expectedUrl);

        ImageRecord record = response.getBody();
        assertThat(record.getName()).isEqualTo(filename);
        assertThat(record.getType()).isEqualTo("image/jpeg");
        assertThat(record.getUsername()).isEqualTo(userA.getUsername());
        assertThat(record.getDateUploaded()).isBetween(beforeUpload, afterUpload);
        assertThat(record.getUploadStatus()).isEqualTo(ImageRecord.UploadStatus.succeeded);
        assertThat(record.getUrl()).isEqualTo(expectedUrl);
    }

    @Test
    public void testUploadImageAlreadyExists() {
        // Arrange
        String filename = "san diego.jpg";
        target.uploadImageRequest(userA, filename);

        // Act
        ResponseEntity<ImageRecord> response = target.uploadImageRequest(userA, filename);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(409);
    }

    @Test
    public void testDeleteImageHappyPath() {
        // Arrange
        String filename = "san diego.jpg";
        target.uploadImageRequest(userA, filename);

        // Act
        ResponseEntity<String> response = target.deleteImageRequest(userA, filename);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(204);

        // no longer exists in storage
        ResponseEntity<Resource> getImageResponse = target.getImageRequest(filename);
        assertThat(getImageResponse.getStatusCodeValue()).isEqualTo(404);

        // record no longer exists
        ResponseEntity<List<ImageRecord>> getImagesResponse = target.getImagesRequest();
        assertThat(getImagesResponse.getBody().stream().filter(record -> record.getName().equals(filename)).findAny())
                .isEmpty();
    }

    @Test
    public void testNotAllowedToDeleteOtherUsersImages() {
        // Arrange
        String filename = "san diego.jpg";

        target.uploadImageRequest(userA, filename);

        // Act
        ResponseEntity<String> response = target.deleteImageRequest(userB, filename);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(403);

        // check that the image has not been deleted from storage
        ResponseEntity<Resource> getImageResponse = target.getImageRequest(filename);
        assertThat(getImageResponse.getStatusCodeValue()).isEqualTo(200);

        // check that the record has not been deleted
        ResponseEntity<List<ImageRecord>> getImagesResponse = target.getImagesRequest();
        assertThat(getImagesResponse.getBody()
                .stream()
                .filter(record -> record.getName().equals(filename))
                .findFirst())
                .isPresent();
    }

    @Test
    public void testAdminsCanDeleteAnyImage() {
        // Arrange
        String filename = "san diego.jpg";
        target.uploadImageRequest(userA, filename);

        // Act
        ResponseEntity<String> response = target.deleteImageRequest(admin, filename);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(204);

        // no longer exists in storage
        ResponseEntity<Resource> getImageResponse = target.getImageRequest(filename);
        assertThat(getImageResponse.getStatusCodeValue()).isEqualTo(404);

        // record no longer exists
        ResponseEntity<List<ImageRecord>> getImagesResponse = target.getImagesRequest();
        assertThat(getImagesResponse.getBody().stream().filter(record -> record.getName().equals(filename)).findAny())
                .isEmpty();
    }
}
