package imagerepo.services;

import imagerepo.models.ImageRecord;
import imagerepo.repositories.ImageRecordsRepository;
import imagerepo.services.exceptions.ImageTypeNotAllowedException;
import imagerepo.services.exceptions.ImageWithNameAlreadyExistsException;
import lombok.SneakyThrows;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rcs.auth.api.models.AuthenticatedUser;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@PropertySource("file:${app.properties}")
public class ImageRepoService {

    private static final Logger logger = LoggerFactory.getLogger(ImageRepoService.class);

    private static final Set<String> allowedContentTypes = Set.of(
            ContentType.IMAGE_PNG, ContentType.IMAGE_BMP, ContentType.IMAGE_GIF, ContentType.IMAGE_JPEG)
            .stream()
            .map(Object::toString)
            .collect(Collectors.toSet());

    private ImageRecordsRepository imageRecordsRepository;
    private ImageStorageService imageStorageService;
    private String baseUrl;

    public ImageRepoService(
            ImageRecordsRepository imageRecordsRepository,
            ImageStorageService imageStorageService,
            @Value("${service.baseUrl}") String baseUrl) {
        this.imageRecordsRepository = imageRecordsRepository;
        this.imageStorageService = imageStorageService;
        this.baseUrl = baseUrl;
    }

    public List<ImageRecord> getImages() {
        return imageRecordsRepository.findAll()
                .stream()
                .map(this::enrichWithUrl)
                .collect(Collectors.toList());
    }

    public Resource getImage(String name) throws IOException {
        return imageStorageService.load(name);
    }

    @Transactional
    public ImageRecord uploadImage(AuthenticatedUser user, MultipartFile file, Date timestamp) {
        String contentType = file.getContentType();
        if (!allowedContentTypes.contains(contentType)) {
            throw new ImageTypeNotAllowedException(contentType, allowedContentTypes);
        }

        String filename = file.getOriginalFilename();
        if (imageRecordsRepository.isPendingOrSucceeded(filename)) {
            throw new ImageWithNameAlreadyExistsException(filename);
        }

        ImageRecord record = new ImageRecord(
                filename,
                contentType,
                user.getUsername(),
                timestamp,
                ImageRecord.UploadStatus.pending,
                null);

        imageRecordsRepository.save(record);

        ImageRecord.UploadStatus uploadStatus;
        try {
            imageStorageService.save(file);
            uploadStatus = ImageRecord.UploadStatus.succeeded;
        } catch (Exception e) {
            logger.error("Failed to save image {} with error message: {}", filename, e.getMessage());
            uploadStatus = ImageRecord.UploadStatus.failed;
        }

        return enrichWithUrl(imageRecordsRepository.updateStatus(filename, uploadStatus));
    }

    @Transactional
    public void deleteImage(String filename) throws IOException {
        imageStorageService.delete(filename);
        imageRecordsRepository.deleteById(filename);
    }

    private ImageRecord enrichWithUrl(ImageRecord imageRecord) {
        return new ImageRecord(
                imageRecord.getName(),
                imageRecord.getType(),
                imageRecord.getUsername(),
                imageRecord.getDateUploaded(),
                imageRecord.getUploadStatus(),
                ImageRecord.UploadStatus.succeeded.equals(imageRecord.getUploadStatus())
                        ? buildUrl(imageRecord.getName())
                        : null);
    }

    @SneakyThrows
    private String buildUrl(String name) {
        return baseUrl + "/imagerepo/api/images/" + URLEncoder.encode(name, "UTF-8");
    }
}