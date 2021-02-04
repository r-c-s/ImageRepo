package imagerepo.apis;

import imagerepo.auth.models.LoginCredentials;
import imagerepo.models.ImageRecord;
import lombok.SneakyThrows;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.net.URI;
import java.util.List;

public class ImageRepoService {

    private final String baseUrl;
    private final AuthService authService;
    private final RestTemplate restTemplate;

    public ImageRepoService(String baseUrl, AuthService authService, RestTemplate restTemplate) {
        this.baseUrl = baseUrl;
        this.authService = authService;
        this.restTemplate = restTemplate;
    }

    public ResponseEntity<List<ImageRecord>> getImagesRequest() {
        return restTemplate.exchange(
                createUrl("/imagerepo/api/images/"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ImageRecord>>() { });
    }

    public ResponseEntity<Resource> getImageRequest(String filename) {
        return restTemplate.getForEntity(
                createUrl("/imagerepo/api/images/" + filename),
                Resource.class);
    }

    public ResponseEntity<ImageRecord> uploadImageRequest(LoginCredentials creds, String filename) {
        LinkedMultiValueMap<String, Resource> payload = new LinkedMultiValueMap<>();
        payload.put("file", List.of(getResourceFile(filename)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        String authToken = authService.login(creds);
        headers.add("Cookie", AuthService.authTokenName + "=" + authToken);

        HttpEntity<LinkedMultiValueMap<String, Resource>> request = new HttpEntity<>(payload, headers);

        return restTemplate.postForEntity(
                createUrl("/imagerepo/api/images"),
                request,
                ImageRecord.class);
    }

    public ResponseEntity<String> deleteImageRequest(LoginCredentials creds, String filename) {
        HttpHeaders headers = new HttpHeaders();
        String authToken = authService.login(creds);
        headers.add("Cookie", AuthService.authTokenName + "=" + authToken);

        HttpEntity<Object> request = new HttpEntity<>(null, headers);

        return restTemplate.exchange(
                createUrl("/imagerepo/api/images/" + filename),
                HttpMethod.DELETE,
                request,
                String.class);
    }

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
}
