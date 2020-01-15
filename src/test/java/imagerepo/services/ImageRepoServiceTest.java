package imagerepo.services;

import com.google.common.collect.ImmutableList;
import imagerepo.models.ImageRecord;
import imagerepo.repositories.ImageRecordsRepository;
import imagerepo.services.exceptions.ImageTypeNotAllowedException;
import imagerepo.services.exceptions.ImageWithNameAlreadyExistsException;
import imagerepo.services.exceptions.NotAllowedToDeleteImageException;
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
    private String host;
    private int port;
    private ImageRepoService target;

    @Before
    public void setup() {
        imageRecordsRepository = mock(ImageRecordsRepository.class);
        imageStorageService = mock(ImageStorageService.class);
        host = "localhost";
        port = 1234;
        target = new ImageRepoService(imageRecordsRepository, imageStorageService, host, port);
    }

    @Test
    public void testGetImages() {
        // Arrange
        List<ImageRecord> repositoryResponse = ImmutableList.of(
                new ImageRecord("image1.jpg", "image/jpeg", "userId", new Date(1), ImageRecord.UploadStatus.succeeded, null),
                new ImageRecord("image2.jpg", "image/jpeg", "userId", new Date(2), ImageRecord.UploadStatus.pending, null),
                new ImageRecord("image2.jpg", "image/jpeg", "userId", new Date(2), ImageRecord.UploadStatus.failed, null));

        when(imageRecordsRepository.findAll()).thenReturn(repositoryResponse);

        // Act
        List<ImageRecord> actual = target.getImages();

        // Assert
        List<ImageRecord> expected = ImmutableList.of(
                new ImageRecord("image1.jpg", "image/jpeg", "userId", new Date(1), ImageRecord.UploadStatus.succeeded, "http://localhost:" + port + "/imagerepo/api/images/image1.jpg"),
                new ImageRecord("image2.jpg", "image/jpeg", "userId", new Date(2), ImageRecord.UploadStatus.pending, null),
                new ImageRecord("image2.jpg", "image/jpeg", "userId", new Date(2), ImageRecord.UploadStatus.failed, null));

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
        String userId = "userId";
        String filename = "filename";
        Date timestamp = new Date(123);
        String type = "image/jpeg";

        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn(type);
        when(file.getOriginalFilename()).thenReturn(filename);

        ImageRecord repositoryResponse = new ImageRecord(filename, type, userId, timestamp, ImageRecord.UploadStatus.succeeded, null);
        when(imageRecordsRepository.updateStatus(filename, ImageRecord.UploadStatus.succeeded))
                .thenReturn(repositoryResponse);

        // Act
        ImageRecord actual = target.uploadImage(userId, file, timestamp);

        // Assert
        ImageRecord expected = withUrl(repositoryResponse, "http://localhost:" + port + "/imagerepo/api/images/" + filename);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);

        InOrder inOrder = inOrder(imageRecordsRepository, imageStorageService);
        inOrder.verify(imageStorageService).save(file);
        inOrder.verify(imageRecordsRepository).updateStatus(filename, ImageRecord.UploadStatus.succeeded);
    }

    @Test
    public void testUploadImageWhenStorageFails() throws IOException {
        // Arrange
        String userId = "userId";
        String filename = "filename";
        Date timestamp = new Date(123);
        String type = "image/jpeg";

        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn(type);
        when(file.getOriginalFilename()).thenReturn(filename);

        ImageRecord repositoryResponse = new ImageRecord(filename, type, userId, timestamp, ImageRecord.UploadStatus.failed, null);
        when(imageRecordsRepository.updateStatus(filename, ImageRecord.UploadStatus.failed))
                .thenReturn(repositoryResponse);

        doThrow(IOException.class).when(imageStorageService).save(file);

        // Act
        ImageRecord actual = target.uploadImage(userId, file, timestamp);

        // Assert
        assertThat(actual).usingRecursiveComparison().isEqualTo(repositoryResponse);

        InOrder inOrder = inOrder(imageRecordsRepository, imageStorageService);
        inOrder.verify(imageStorageService).save(file);
        inOrder.verify(imageRecordsRepository).updateStatus(filename, ImageRecord.UploadStatus.failed);
    }

    @Test
    public void testUploadImageAlreadyExists() {
        // Arrange
        String userId = "userId";
        String filename = "filename";
        Date timestamp = new Date(123);
        String type = "image/jpeg";

        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn(type);
        when(file.getOriginalFilename()).thenReturn(filename);

        when(imageRecordsRepository.existsByNameAndIsPendingOrSucceeded(filename))
                .thenReturn(true);

        // Act & Assert
        assertThrows(
                ImageWithNameAlreadyExistsException.class,
                () -> target.uploadImage(userId, file, timestamp));
    }

    @Test
    public void testUploadImageInvalidType() throws IOException {
        // Arrange
        String userId = "userId";
        MultipartFile file = mock(MultipartFile.class);
        Date timestamp = new Date(123);

        when(file.getContentType()).thenReturn("text/html");

        // Act
        assertThrows(
                ImageTypeNotAllowedException.class,
                () -> target.uploadImage(userId, file, timestamp));

        // Assert
        verify(imageRecordsRepository, never()).save(any());
        verify(imageStorageService, never()).save(any());
    }

    @Test
    public void testDeleteImageHappyPath() throws IOException {
        // Arrange
        String userId = "requester";
        String name = "picture";

        when(imageRecordsRepository.findByName(name))
                .thenReturn(new ImageRecord(null, null, userId, null, null, null));

        // Act
        target.deleteImage(name, userId);

        // Assert
        verify(imageStorageService).delete(name);
        verify(imageRecordsRepository).deleteById(name);
    }

    @Test
    public void testDeleteImageNotAllowed() throws IOException {
        // Arrange
        String userId = "requester";
        String ownerId = "ownderId";
        String name = "picture";

        when(imageRecordsRepository.findByName(name))
                .thenReturn(new ImageRecord(null, null, ownerId, null, null, null));

        // Act
        assertThrows(
                NotAllowedToDeleteImageException.class,
                () ->  target.deleteImage(name, userId));

        // Assert
        verify(imageStorageService, never()).delete(any());
        verify(imageRecordsRepository, never()).deleteById(any());
    }

    private static ImageRecord withUrl(ImageRecord record, String url) {
        return new ImageRecord(
                record.getName(),
                record.getType(),
                record.getUserId(),
                record.getDateUploaded(),
                record.getUploadStatus(),
                url);
    }
}