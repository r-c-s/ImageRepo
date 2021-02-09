package fileupload.services;

import fileupload.models.FileUploadRecord;
import fileupload.repositories.FileUploadRecordsRepository;
import fileupload.services.exceptions.FileTypeNotAllowedException;
import fileupload.services.exceptions.FileWithNameAlreadyExistsException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.multipart.MultipartFile;
import rcs.auth.api.models.AuthenticatedUser;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class FileUploadServiceTest {

    private FileUploadRecordsRepository fileUploadRecordsRepository;
    private FileStorageService fileStorageService;
    private String baseUrl;
    private FileUploadService target;

    @Before
    public void setup() {
        fileUploadRecordsRepository = mock(FileUploadRecordsRepository.class);
        fileStorageService = mock(FileStorageService.class);
        baseUrl = "https//fileupload.com";
        target = new FileUploadService(fileUploadRecordsRepository, fileStorageService, baseUrl);
    }

    @Test
    public void testGetFiles() {
        // Arrange
        LocalDateTime date1 = LocalDateTime.now();
        LocalDateTime date2 = LocalDateTime.now();
        LocalDateTime date3 = LocalDateTime.now();

        List<FileUploadRecord> repositoryResponse = List.of(
                new FileUploadRecord("image1.jpg", "image/jpeg", "username", date1, FileUploadRecord.UploadStatus.succeeded, null),
                new FileUploadRecord("image2.jpg", "image/jpeg", "username", date2, FileUploadRecord.UploadStatus.pending, null),
                new FileUploadRecord("image2.jpg", "image/jpeg", "username", date3, FileUploadRecord.UploadStatus.failed, null));

        when(fileUploadRecordsRepository.findAll()).thenReturn(repositoryResponse);

        // Act
        List<FileUploadRecord> actual = target.getFiles();

        // Assert
        List<FileUploadRecord> expected = List.of(
                new FileUploadRecord("image1.jpg", "image/jpeg", "username", date1, FileUploadRecord.UploadStatus.succeeded, baseUrl + "/fileupload/api/files/image1.jpg"),
                new FileUploadRecord("image2.jpg", "image/jpeg", "username", date2, FileUploadRecord.UploadStatus.pending, null),
                new FileUploadRecord("image2.jpg", "image/jpeg", "username", date3, FileUploadRecord.UploadStatus.failed, null));

        assertThat(actual).usingFieldByFieldElementComparator().isEqualTo(expected);
    }

    @Test
    public void testGetFile() throws IOException {
        // Arrange
        Resource expected = mock(Resource.class);
        when(fileStorageService.load("image-name")).thenReturn(expected);

        // Act
        Resource actual = target.getFile("image-name");

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testUploadFileHappyPath() throws IOException {
        // Arrange
        String username = "username";
        String filename = "filename";
        LocalDateTime timestamp = LocalDateTime.now();
        String type = "image/jpeg";

        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.getUsername()).thenReturn(username);

        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn(type);
        when(file.getOriginalFilename()).thenReturn(filename);

        FileUploadRecord repositoryResponse = new FileUploadRecord(
                filename, type, username, timestamp, FileUploadRecord.UploadStatus.succeeded, null);
        when(fileUploadRecordsRepository.updateStatus(filename, FileUploadRecord.UploadStatus.succeeded))
                .thenReturn(repositoryResponse);

        // Act
        FileUploadRecord actual = target.uploadFile(user, file, timestamp);

        // Assert
        FileUploadRecord expected = withUrl(repositoryResponse, baseUrl + "/fileupload/api/files/" + filename);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);

        InOrder inOrder = inOrder(fileUploadRecordsRepository, fileStorageService);
        inOrder.verify(fileStorageService).save(file);
        inOrder.verify(fileUploadRecordsRepository).updateStatus(filename, FileUploadRecord.UploadStatus.succeeded);
    }

    @Test
    public void testUploadFileWhenStorageFails() throws IOException {
        // Arrange
        String username = "username";
        String filename = "filename";
        LocalDateTime timestamp = LocalDateTime.now();
        String type = "image/jpeg";

        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.getUsername()).thenReturn(username);

        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn(type);
        when(file.getOriginalFilename()).thenReturn(filename);

        FileUploadRecord repositoryResponse = new FileUploadRecord(filename, type, username, timestamp, FileUploadRecord.UploadStatus.failed, null);
        when(fileUploadRecordsRepository.updateStatus(filename, FileUploadRecord.UploadStatus.failed))
                .thenReturn(repositoryResponse);

        doThrow(IOException.class).when(fileStorageService).save(file);

        // Act
        FileUploadRecord actual = target.uploadFile(user, file, timestamp);

        // Assert
        assertThat(actual).usingRecursiveComparison().isEqualTo(repositoryResponse);

        InOrder inOrder = inOrder(fileUploadRecordsRepository, fileStorageService);
        inOrder.verify(fileStorageService).save(file);
        inOrder.verify(fileUploadRecordsRepository).updateStatus(filename, FileUploadRecord.UploadStatus.failed);
    }

    @Test
    public void testUploadFileAlreadyExists() {
        // Arrange
        String username = "username";
        String filename = "filename";
        LocalDateTime timestamp = LocalDateTime.now();
        String type = "image/jpeg";

        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.getUsername()).thenReturn(username);

        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn(type);
        when(file.getOriginalFilename()).thenReturn(filename);

        when(fileUploadRecordsRepository.isPendingOrSucceeded(filename))
                .thenReturn(true);

        // Act & Assert
        assertThrows(
                FileWithNameAlreadyExistsException.class,
                () -> target.uploadFile(user, file, timestamp));
    }

    @Test
    public void testDeleteFile() throws IOException {
        // Arrange
        String name = "picture";

        // Act
        target.deleteFile(name);

        // Assert
        verify(fileStorageService).delete(name);
        verify(fileUploadRecordsRepository).deleteById(name);
    }

    private static FileUploadRecord withUrl(FileUploadRecord record, String url) {
        return new FileUploadRecord(
                record.getName(),
                record.getType(),
                record.getUsername(),
                record.getDateUploaded(),
                record.getUploadStatus(),
                url);
    }
}