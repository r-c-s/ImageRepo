package imagerepo;

import com.google.common.collect.ImmutableList;
import imagerepo.auth.AuthenticationService;
import imagerepo.models.ImageRecord;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@TestPropertySource("file:${app.properties}")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class ImageRepoAppIT {

    @Value("${userA.username}") private String userAUserName;
    @Value("${userA.password}") private String userAPassword;
    @Value("${userB.username}") private String userBUsername;
    @Value("${userB.password}") private String userBPassword;
    @Value("${admin.username}") private String adminUsername;
    @Value("${admin.password}") private String adminPassword;

    @Value("${service.baseUrl}")
    private String baseUrl;

    @Value("${services.auth.login}")
    private String authServiceLoginUrl;

    private static final TestRestTemplate restTemplate = new TestRestTemplate();

    @Before
    @After
    public void cleanup() {
        deleteAllImages();
    }

    @Test
    public void testGetImages() {
        // Arrange
        String filename = "san diego.jpg";

        Date beforeUpload = new Date();
        uploadImageRequest(userAUserName, userAPassword, filename);
        Date afterUpload = new Date();

        // Act
        ResponseEntity<List<ImageRecord>> response = getImagesRequest();

        // Assert
        List<ImageRecord> records = response.getBody();
        assertThat(records.size()).isEqualTo(1);

        ImageRecord record = records.get(0);
        assertThat(record.getName()).isEqualTo(filename);
        assertThat(record.getType()).isEqualTo("image/jpeg");
        assertThat(record.getUsername()).isEqualTo(userAUserName);
        assertThat(record.getDateUploaded()).isBetween(beforeUpload, afterUpload);
        assertThat(record.getUploadStatus()).isEqualTo(ImageRecord.UploadStatus.succeeded);
        assertThat(record.getUrl()).isEqualTo(createUrl("/imagerepo/api/images/san+diego.jpg"));
    }

    @Test
    public void testGetImage() throws IOException {
        // Arrange
        String filename = "san diego.jpg";
        uploadImageRequest(userAUserName, userAPassword, filename);

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
        ResponseEntity<ImageRecord> response = uploadImageRequest(userAUserName, userAPassword, filename);
        Date afterUpload = new Date();

        // Assert
        String expectedUrl = createUrl("/imagerepo/api/images/san+diego.jpg");

        assertThat(response.getStatusCodeValue()).isEqualTo(201);
        assertThat(response.getHeaders().getLocation().toString()).isEqualTo(expectedUrl);

        ImageRecord record = response.getBody();
        assertThat(record.getName()).isEqualTo(filename);
        assertThat(record.getType()).isEqualTo("image/jpeg");
        assertThat(record.getUsername()).isEqualTo(userAUserName);
        assertThat(record.getDateUploaded()).isBetween(beforeUpload, afterUpload);
        assertThat(record.getUploadStatus()).isEqualTo(ImageRecord.UploadStatus.succeeded);
        assertThat(record.getUrl()).isEqualTo(expectedUrl);
    }

    @Test
    public void testUploadImageAlreadyExists() {
        // Arrange
        String filename = "san diego.jpg";
        uploadImageRequest(userAUserName, userAPassword, filename);

        // Act
        ResponseEntity<ImageRecord> response = uploadImageRequest(userAUserName, userAPassword, filename);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(409);
    }

    @Test
    public void testDeleteImageHappyPath() {
        // Arrange
        String filename = "san diego.jpg";
        uploadImageRequest(userAUserName, userAPassword, filename);

        // Act
        ResponseEntity<String> response = deleteImageRequest(userAUserName, userAPassword, filename);

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

        uploadImageRequest(userAUserName, userAPassword, filename);

        // Act
        ResponseEntity<String> response = deleteImageRequest(userBUsername, userBPassword, filename);

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
        uploadImageRequest(userAUserName, userAPassword, filename);

        // Act
        ResponseEntity<String> response = deleteImageRequest(adminUsername, adminPassword, filename);

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

    private void deleteAllImages() {
        getImagesRequest().getBody().stream()
                .map(ImageRecord::getName)
                .forEach(name -> deleteImageRequest(adminUsername, adminPassword, name));
    }

    public String login(String username, String password) {
        MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
        params.set("username", username);
        params.set("password", password);

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(params, new HttpHeaders());

        ResponseEntity<Void> response = restTemplate.exchange(
                authServiceLoginUrl,
                HttpMethod.POST,
                entity,
                Void.class);

        return getCookieValue(
                AuthenticationService.authTokenName,
                response.getHeaders().get("Set-Cookie").get(0));
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

    private ResponseEntity<ImageRecord> uploadImageRequest(String username, String password, String filename) {
        LinkedMultiValueMap<String, Resource> payload = new LinkedMultiValueMap<>();
        payload.put("file", ImmutableList.of(getResourceFile(filename)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        String authToken = login(username, password);
        headers.add("Cookie", AuthenticationService.authTokenName + "=" + authToken);

        HttpEntity<LinkedMultiValueMap<String, Resource>> request = new HttpEntity<>(payload, headers);

        return restTemplate.postForEntity(
                createUrl("/imagerepo/api/images"),
                request,
                ImageRecord.class);
    }

    private ResponseEntity<String> deleteImageRequest(String username, String password, String filename) {
        HttpHeaders headers = new HttpHeaders();
        String authToken = login(username, password);
        headers.add("Cookie", AuthenticationService.authTokenName + "=" + authToken);

        HttpEntity<Object> request = new HttpEntity<>(null, headers);

        return restTemplate.exchange(
                createUrl("/imagerepo/api/images/" + filename),
                HttpMethod.DELETE,
                request,
                String.class);
    }
}
