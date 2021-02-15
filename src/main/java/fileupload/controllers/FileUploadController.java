package fileupload.controllers;

import fileupload.models.FileUploadRecord;
import fileupload.services.FileUploadService;
import lombok.SneakyThrows;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import rcs.auth.api.AuthUtils;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private static final MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();

    private FileUploadService fileUploadService;
    private AuthUtils authUtils;

    public FileUploadController(FileUploadService fileUploadService, AuthUtils authUtils) {
        this.fileUploadService = fileUploadService;
        this.authUtils = authUtils;
    }

    @GetMapping
    public ResponseEntity<List<FileUploadRecord>> getFiles() {
        List<FileUploadRecord> files = fileUploadService.getFiles();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(files);
    }

    @GetMapping("/{name}")
    public ResponseEntity<Resource> getFile(@PathVariable String name) throws IOException {
        Resource resource = fileUploadService.getFile(name);
        String contentType = mimeTypesMap.getContentType(resource.getFilename());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @PostMapping
    public ResponseEntity<FileUploadRecord> uploadFile(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        return authUtils.tryGetLoggedInUser(request)
                .map(user -> fileUploadService.uploadFile(user, file, LocalDateTime.now()))
                .map(record -> {
                    boolean created = record.getUploadStatus().equals(FileUploadRecord.UploadStatus.succeeded);
                    return (created
                            ? ResponseEntity.created(getUri(record.getUrl()))
                            : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR))
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(record);
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> deleteFile(@PathVariable String name) throws IOException {
        fileUploadService.deleteFile(name);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .build();
    }

    @SneakyThrows
    private URI getUri(String url) {
        return new URI(url);
    }
}
