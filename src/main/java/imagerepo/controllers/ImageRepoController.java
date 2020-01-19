package imagerepo.controllers;

import imagerepo.auth.AuthenticatedHttpServletRequest;
import imagerepo.auth.exceptions.UnauthorizedException;
import imagerepo.auth.models.AuthenticatedUser;
import imagerepo.models.ImageRecord;
import imagerepo.services.ImageRepoService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/images")
public class ImageRepoController {

    private static final MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();

    private ImageRepoService imageRepoService;

    public ImageRepoController(ImageRepoService imageRepoService) {
        this.imageRepoService = imageRepoService;
    }

    @GetMapping
    public ResponseEntity<List<ImageRecord>> getImages() {
        List<ImageRecord> images = imageRepoService.getImages();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(images);
    }

    @GetMapping("/{name}")
    public ResponseEntity<Resource> getImage(@PathVariable String name) throws IOException {
        Resource resource = imageRepoService.getImage(name);
        String contentType = mimeTypesMap.getContentType(resource.getFilename());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @PostMapping
    public ResponseEntity<ImageRecord> uploadImage(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) throws URISyntaxException {
        AuthenticatedHttpServletRequest authenticatedRequest = tryGetAuthenticatedRequest(request);
        AuthenticatedUser user = authenticatedRequest.getLoggedInUser();
        ImageRecord record = imageRepoService.uploadImage(user, file, new Date());
        boolean created = record.getUploadStatus().equals(ImageRecord.UploadStatus.succeeded);
        return (created
                ? ResponseEntity.created(new URI(record.getUrl()))
                : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR))
                .contentType(MediaType.APPLICATION_JSON)
                .body(record);
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable String name,
            HttpServletRequest request) throws IOException {
        AuthenticatedHttpServletRequest authenticatedRequest = tryGetAuthenticatedRequest(request);
        AuthenticatedUser user = authenticatedRequest.getLoggedInUser();
        imageRepoService.deleteImage(name, user);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .build();
    }

    private AuthenticatedHttpServletRequest tryGetAuthenticatedRequest(ServletRequest request) {
        if (request instanceof AuthenticatedHttpServletRequest) {
            return (AuthenticatedHttpServletRequest) request;
        }
        if (request instanceof HttpServletRequestWrapper) {
            return tryGetAuthenticatedRequest(((HttpServletRequestWrapper) request).getRequest());
        }
        throw new UnauthorizedException();
    }
}
