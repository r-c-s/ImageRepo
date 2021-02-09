package fileupload.services;

import fileupload.models.FileUploadRecord;
import fileupload.repositories.FileUploadRecordsRepository;
import fileupload.services.exceptions.FileWithNameAlreadyExistsException;
import lombok.SneakyThrows;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@PropertySource("file:${app.properties}")
public class FileUploadService {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadService.class);

    private FileUploadRecordsRepository fileUploadRecordsRepository;
    private FileStorageService fileStorageService;
    private String baseUrl;

    public FileUploadService(
            FileUploadRecordsRepository fileUploadRecordsRepository,
            FileStorageService fileStorageService,
            @Value("${service.baseUrl}") String baseUrl) {
        this.fileUploadRecordsRepository = fileUploadRecordsRepository;
        this.fileStorageService = fileStorageService;
        this.baseUrl = baseUrl;
    }

    public List<FileUploadRecord> getFiles() {
        return fileUploadRecordsRepository.findAll()
                .stream()
                .map(this::enrichWithUrl)
                .collect(Collectors.toList());
    }

    public Resource getFile(String name) throws IOException {
        return fileStorageService.load(name);
    }

    @Transactional
    public FileUploadRecord uploadFile(AuthenticatedUser user, MultipartFile file, LocalDateTime timestamp) {
        String filename = file.getOriginalFilename();

        // todo: create a directory per each user, decide what to do with clashing names
        if (fileUploadRecordsRepository.isPendingOrSucceeded(filename)) {
            throw new FileWithNameAlreadyExistsException(filename);
        }

        FileUploadRecord record = new FileUploadRecord(
                filename,
                file.getContentType(),
                user.getUsername(),
                timestamp,
                FileUploadRecord.UploadStatus.pending,
                null);

        fileUploadRecordsRepository.save(record);

        FileUploadRecord.UploadStatus uploadStatus;
        try {
            fileStorageService.save(file);
            uploadStatus = FileUploadRecord.UploadStatus.succeeded;
        } catch (Exception e) {
            logger.error("Failed to save file {} with error message: {}", filename, e.getMessage());
            uploadStatus = FileUploadRecord.UploadStatus.failed;
        }

        return enrichWithUrl(fileUploadRecordsRepository.updateStatus(filename, uploadStatus));
    }

    @Transactional
    public void deleteFile(String filename) throws IOException {
        fileStorageService.delete(filename);
        fileUploadRecordsRepository.deleteById(filename);
    }

    private FileUploadRecord enrichWithUrl(FileUploadRecord fileUploadRecord) {
        return new FileUploadRecord(
                fileUploadRecord.getName(),
                fileUploadRecord.getType(),
                fileUploadRecord.getUsername(),
                fileUploadRecord.getDateUploaded(),
                fileUploadRecord.getUploadStatus(),
                FileUploadRecord.UploadStatus.succeeded.equals(fileUploadRecord.getUploadStatus())
                        ? buildUrl(fileUploadRecord.getName())
                        : null);
    }

    @SneakyThrows
    private String buildUrl(String name) {
        return baseUrl + "/fileupload/api/files/" + URLEncoder.encode(name, "UTF-8");
    }
}