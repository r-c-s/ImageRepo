package imagerepo.services;

import imagerepo.auth.models.AuthenticatedUser;
import imagerepo.models.ImageRecord;
import imagerepo.repositories.ImageRecordsRepository;
import imagerepo.services.exceptions.ImageTypeNotAllowedException;
import imagerepo.services.exceptions.ImageWithNameAlreadyExistsException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class ImageRepoServiceTest {

    private ImageRecordsRepository imageRecordsRepository;
    private ImageStorageService imageStorageService;
    private String baseUrl;
    private ImageRepoService target;

    @Before
    public void setup() {
        imageRecordsRepository = mock(ImageRecordsRepository.class);
        imageStorageService = mock(ImageStorageService.class);
        baseUrl = "https//imagerepo.com";
        target = new ImageRepoService(imageRecordsRepository, imageStorageService, baseUrl);
    }

    @Test
    public void testGetImages() {
        // Arrange
        List<ImageRecord> repositoryResponse = List.of(
                new ImageRecord("image1.jpg", "image/jpeg", "username", new Date(1), ImageRecord.UploadStatus.succeeded, null),
                new ImageRecord("image2.jpg", "image/jpeg", "username", new Date(2), ImageRecord.UploadStatus.pending, null),
                new ImageRecord("image2.jpg", "image/jpeg", "username", new Date(2), ImageRecord.UploadStatus.failed, null));

        when(imageRecordsRepository.findAll()).thenReturn(repositoryResponse);

        // Act
        List<ImageRecord> actual = target.getImages();

        // Assert
        List<ImageRecord> expected = List.of(
                new ImageRecord("image1.jpg", "image/jpeg", "username", new Date(1), ImageRecord.UploadStatus.succeeded, baseUrl + "/imagerepo/api/images/image1.jpg"),
                new ImageRecord("image2.jpg", "image/jpeg", "username", new Date(2), ImageRecord.UploadStatus.pending, null),
                new ImageRecord("image2.jpg", "image/jpeg", "username", new Date(2), ImageRecord.UploadStatus.failed, null));

        assertThat(actual).usingFieldByFieldElementComparator().isEqualTo(expected);
    }

    @Test
    public void testGetImage() throws IOException {
        // Arrange
        Resource expected = mock(Resource.class);
        when(imageStorageService.load("image-name")).thenReturn(expected);

        // Act
        Resource actual = target.getImage("image-name");

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testUploadImageHappyPath() throws IOException {
        // Arrange
        String username = "username";
        String filename = "filename";
        Date timestamp = new Date(123);
        String type = "image/jpeg";

        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.getUsername()).thenReturn(username);

        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn(type);
        when(file.getOriginalFilename()).thenReturn(filename);

        ImageRecord repositoryResponse = new ImageRecord(
                filename, type, username, timestamp, ImageRecord.UploadStatus.succeeded, null);
        when(imageRecordsRepository.updateStatus(filename, ImageRecord.UploadStatus.succeeded))
                .thenReturn(repositoryResponse);

        // Act
        ImageRecord actual = target.uploadImage(user, file, timestamp);

        // Assert
        ImageRecord expected = withUrl(repositoryResponse, baseUrl + "/imagerepo/api/images/" + filename);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);

        InOrder inOrder = inOrder(imageRecordsRepository, imageStorageService);
        inOrder.verify(imageStorageService).save(file);
        inOrder.verify(imageRecordsRepository).updateStatus(filename, ImageRecord.UploadStatus.succeeded);
    }

    @Test
    public void testUploadImageWhenStorageFails() throws IOException {
        // Arrange
        String username = "username";
        String filename = "filename";
        Date timestamp = new Date(123);
        String type = "image/jpeg";

        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.getUsername()).thenReturn(username);

        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn(type);
        when(file.getOriginalFilename()).thenReturn(filename);

        ImageRecord repositoryResponse = new ImageRecord(filename, type, username, timestamp, ImageRecord.UploadStatus.failed, null);
        when(imageRecordsRepository.updateStatus(filename, ImageRecord.UploadStatus.failed))
                .thenReturn(repositoryResponse);

        doThrow(IOException.class).when(imageStorageService).save(file);

        // Act
        ImageRecord actual = target.uploadImage(user, file, timestamp);

        // Assert
        assertThat(actual).usingRecursiveComparison().isEqualTo(repositoryResponse);

        InOrder inOrder = inOrder(imageRecordsRepository, imageStorageService);
        inOrder.verify(imageStorageService).save(file);
        inOrder.verify(imageRecordsRepository).updateStatus(filename, ImageRecord.UploadStatus.failed);
    }

    @Test
    public void testUploadImageAlreadyExists() {
        // Arrange
        String username = "username";
        String filename = "filename";
        Date timestamp = new Date(123);
        String type = "image/jpeg";

        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.getUsername()).thenReturn(username);

        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn(type);
        when(file.getOriginalFilename()).thenReturn(filename);

        when(imageRecordsRepository.isPendingOrSucceeded(filename))
                .thenReturn(true);

        // Act & Assert
        assertThrows(
                ImageWithNameAlreadyExistsException.class,
                () -> target.uploadImage(user, file, timestamp));
    }

    @Test
    public void testUploadImageInvalidType() throws IOException {
        // Arrange
        String username = "username";
        MultipartFile file = mock(MultipartFile.class);
        Date timestamp = new Date(123);

        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.getUsername()).thenReturn(username);

        when(file.getContentType()).thenReturn("text/html");

        // Act
        assertThrows(
                ImageTypeNotAllowedException.class,
                () -> target.uploadImage(user, file, timestamp));

        // Assert
        verify(imageRecordsRepository, never()).save(any());
        verify(imageStorageService, never()).save(any());
    }

    @Test
    public void testDeleteImage() throws IOException {
        // Arrange
        String name = "picture";

        // Act
        target.deleteImage(name);

        // Assert
        verify(imageStorageService).delete(name);
        verify(imageRecordsRepository).deleteById(name);
    }

    private static ImageRecord withUrl(ImageRecord record, String url) {
        return new ImageRecord(
                record.getName(),
                record.getType(),
                record.getUsername(),
                record.getDateUploaded(),
                record.getUploadStatus(),
                url);
    }
}