package imagerepo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import imagerepo.auth.AuthenticationService;
import imagerepo.models.ImageRecord;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@TestPropertySource("file:${app.properties}")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class ImageRepoAppIT {

    @Getter
    @AllArgsConstructor
    private static final class LoginCredentials {
        private String username;
        private String password;
    }

    private static LoginCredentials admin = new LoginCredentials("testAdmin", "password");
    private static LoginCredentials userA = new LoginCredentials(RandomString.make(), RandomString.make());
    private static LoginCredentials userB = new LoginCredentials(RandomString.make(), RandomString.make());

    @Value("${service.baseUrl}")
    private String baseUrl;

    @Value("${services.auth.login}")
    private String authServiceLoginUrl;

    @Value("${services.auth.register}")
    private String authServiceRegisterUrl;

    @Value("${services.auth.deleteUser}")
    private String authServiceDeleteUserUrl;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Rule
    public TestRule watchman = new TestWatcher() {
        // unlike @After, this also runs when exceptions are thrown inside test methods
        @Override
        protected void finished(Description ignored) {
            // delete users
            deleteUserRequest(admin, userA.getUsername()).getStatusCodeValue();
            deleteUserRequest(admin, userB.getUsername()).getStatusCodeValue();
            // delete all images
            getImagesRequest().getBody().stream()
                    .map(ImageRecord::getName)
                    .forEach(name -> deleteImageRequest(admin, name));
        }
    };

    @Before
    public void registerTestUsers() {
        registerUser(userA).getStatusCodeValue();
        registerUser(userB).getStatusCodeValue();
    }

    @Test
    public void testGetImages() {
        // Arrange
        String filename = "san diego.jpg";

        Date beforeUpload = new Date();
        uploadImageRequest(userA, filename);
        Date afterUpload = new Date();

        // Act
        ResponseEntity<List<ImageRecord>> response = getImagesRequest();

        // Assert
        List<ImageRecord> records = response.getBody();
        assertThat(records.size()).isEqualTo(1);

        ImageRecord record = records.get(0);
        assertThat(record.getName()).isEqualTo(filename);
        assertThat(record.getType()).isEqualTo("image/jpeg");
        assertThat(record.getUsername()).isEqualTo(userA.getUsername());
        assertThat(record.getDateUploaded()).isBetween(beforeUpload, afterUpload);
        assertThat(record.getUploadStatus()).isEqualTo(ImageRecord.UploadStatus.succeeded);
        assertThat(record.getUrl()).isEqualTo(createUrl("/imagerepo/api/images/san+diego.jpg"));
    }

    @Test
    public void testGetImage() throws IOException {
        // Arrange
        String filename = "san diego.jpg";
        uploadImageRequest(userA, filename);

        // Act
        ResponseEntity<Resource> response = getImageRequest(filename);

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
        ResponseEntity<ImageRecord> response = uploadImageRequest(userA, filename);
        Date afterUpload = new Date();

        // Assert
        String expectedUrl = createUrl("/imagerepo/api/images/san+diego.jpg");

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
        uploadImageRequest(userA, filename);

        // Act
        ResponseEntity<ImageRecord> response = uploadImageRequest(userA, filename);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(409);
    }

    @Test
    public void testDeleteImageHappyPath() {
        // Arrange
        String filename = "san diego.jpg";
        uploadImageRequest(userA, filename);

        // Act
        ResponseEntity<String> response = deleteImageRequest(userA, filename);

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
    public void testNotAllowedToDeleteOtherUsersImages() {
        // Arrange
        String filename = "san diego.jpg";

        uploadImageRequest(userA, filename);

        // Act
        ResponseEntity<String> response = deleteImageRequest(userB, filename);

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

    @Test
    public void testAdminsCanDeleteAnyImage() {
        // Arrange
        String filename = "san diego.jpg";
        uploadImageRequest(userA, filename);

        // Act
        ResponseEntity<String> response = deleteImageRequest(admin, filename);

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

    @SneakyThrows
    private String createUrl(String uri) {
        return baseUrl + uri;
    }

    @SneakyThrows
    private Resource getResourceFile(String name) {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        String path = new URI(classloader.getResource(name).toString()).getPath();
        File file = new File(path);
        return new FileSystemResource(file);
    }

    public String login(LoginCredentials creds) {
        MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
        params.set("username", creds.getUsername());
        params.set("password", creds.getPassword());

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(params, new HttpHeaders());

        ResponseEntity<Void> response = restTemplate.exchange(
                authServiceLoginUrl,
                HttpMethod.POST,
                entity,
                Void.class);

        return Optional.ofNullable(response.getHeaders().get("Set-Cookie"))
                .map(cookies -> cookies.get(0))
                .map(setCookieHeader -> getCookieValue(AuthenticationService.authTokenName, setCookieHeader))
                .orElse(null);
    }

    private String getCookieValue(String cookieName, String setCookieHeader) {
        Pattern pattern = Pattern.compile(cookieName + "=(.*?);");
        Matcher matcher = pattern.matcher(setCookieHeader);
        return matcher.find() ? matcher.group(1) : null;
    }

    private ResponseEntity<List<ImageRecord>> getImagesRequest() {
        return restTemplate.exchange(
                createUrl("/imagerepo/api/images/"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ImageRecord>>() { });
    }

    private ResponseEntity<Resource> getImageRequest(String filename) {
        return restTemplate.getForEntity(
                createUrl("/imagerepo/api/images/" + filename),
                Resource.class);
    }

    private ResponseEntity<ImageRecord> uploadImageRequest(LoginCredentials creds, String filename) {
        LinkedMultiValueMap<String, Resource> payload = new LinkedMultiValueMap<>();
        payload.put("file", ImmutableList.of(getResourceFile(filename)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        String authToken = login(creds);
        headers.add("Cookie", AuthenticationService.authTokenName + "=" + authToken);

        HttpEntity<LinkedMultiValueMap<String, Resource>> request = new HttpEntity<>(payload, headers);

        return restTemplate.postForEntity(
                createUrl("/imagerepo/api/images"),
                request,
                ImageRecord.class);
    }

    private ResponseEntity<String> deleteImageRequest(LoginCredentials creds, String filename) {
        HttpHeaders headers = new HttpHeaders();
        String authToken = login(creds);
        headers.add("Cookie", AuthenticationService.authTokenName + "=" + authToken);

        HttpEntity<Object> request = new HttpEntity<>(null, headers);

        return restTemplate.exchange(
                createUrl("/imagerepo/api/images/" + filename),
                HttpMethod.DELETE,
                request,
                String.class);
    }

    private ResponseEntity<Void> registerUser(LoginCredentials creds) {
        Map<String, String> payload = ImmutableMap.of(
                "username", creds.getUsername(),
                "password", creds.getPassword());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);

        return restTemplate.exchange(
                authServiceRegisterUrl,
                HttpMethod.POST,
                request,
                Void.class);
    }

    private ResponseEntity<Void> deleteUserRequest(LoginCredentials creds, String usernameToDelete) {
        HttpHeaders headers = new HttpHeaders();
        String authToken = login(creds);
        headers.add("Cookie", AuthenticationService.authTokenName + "=" + authToken);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(null, headers);

        return restTemplate.exchange(
                authServiceDeleteUserUrl.replace(":username", usernameToDelete),
                HttpMethod.DELETE,
                request,
                Void.class);
    }
}
