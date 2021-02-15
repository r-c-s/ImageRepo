package rcs.fileupload.api;

import rcs.fileupload.models.FileUploadRecord;
import lombok.SneakyThrows;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import rcs.auth.api.AuthService;
import rcs.auth.api.models.LoginCredentials;

import java.io.File;
import java.net.URI;
import java.util.List;

public class FileUploadService {

    private final String baseUrl;
    private final AuthService authService;
    private final RestTemplate restTemplate;

    public FileUploadService(String baseUrl, AuthService authService, RestTemplate restTemplate) {
        this.baseUrl = baseUrl;
        this.authService = authService;
        this.restTemplate = restTemplate;
    }

    public ResponseEntity<List<FileUploadRecord>> getFilesRequest() {
        return restTemplate.exchange(
                createUrl("/fileupload/api/files/"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<FileUploadRecord>>() { });
    }

    public ResponseEntity<Resource> getFileRequest(String filename) {
        return restTemplate.getForEntity(
                createUrl("/fileupload/api/files/" + filename),
                Resource.class);
    }

    public ResponseEntity<FileUploadRecord> uploadFileRequest(LoginCredentials creds, String filename) {
        return authService.login(creds)
                .map(authToken -> {
                    LinkedMultiValueMap<String, Resource> payload = new LinkedMultiValueMap<>();
                    payload.put("file", List.of(getResourceFile(filename)));

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                    headers.add("Cookie", AuthService.authTokenName + "=" + authToken);

                    HttpEntity<LinkedMultiValueMap<String, Resource>> request = new HttpEntity<>(payload, headers);

                    return restTemplate.postForEntity(
                            createUrl("/fileupload/api/files"),
                            request,
                            FileUploadRecord.class);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    public ResponseEntity<String> deleteFileRequest(LoginCredentials creds, String filename) {
        return authService.login(creds)
                .map(authToken -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.add("Cookie", AuthService.authTokenName + "=" + authToken);

                    HttpEntity<Object> request = new HttpEntity<>(null, headers);

                    return restTemplate.exchange(
                            createUrl("/fileupload/api/files/" + filename),
                            HttpMethod.DELETE,
                            request,
                            String.class);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
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
