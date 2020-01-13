package imagerepo.services;

import com.google.common.collect.ImmutableList;
import imagerepo.models.ImageRecord;
import imagerepo.repositories.ImageRecordsRepository;
import imagerepo.services.exceptions.ImageTypeNotAllowedException;
import imagerepo.services.exceptions.ImageWithNameAlreadyExistsException;
import imagerepo.services.exceptions.NotAllowedToDeleteImageException;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

@RunWith(JUnitParamsRunner.class)
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
    @Parameters({
            "false | true | succeeded",
            "true | false | failed"
    })
    public void testUploadImage(
            boolean storageShouldFail,
            boolean shouldReturnUrl,
            ImageRecord.UploadStatus expectedUploadStatus) throws IOException {
        // Arrange
        String userId = "userId";
        String filename = "filename";
        Date timestamp = new Date(123);
        String type = "image/jpeg";

        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn(type);
        when(file.getOriginalFilename()).thenReturn(filename);

        ImageRecord repositoryResponse = new ImageRecord(filename, type, userId, timestamp, expectedUploadStatus, null);
        when(imageRecordsRepository.updateStatus(filename, expectedUploadStatus))
                .thenReturn(repositoryResponse);

        if (storageShouldFail) {
            doThrow(IOException.class).when(imageStorageService).save(file);
        }

        // Act
        ImageRecord actual = target.uploadImage(userId, file, timestamp);

        // Assert
        ImageRecord expected = withUrl(repositoryResponse, shouldReturnUrl ? "http://localhost:" + port + "/imagerepo/api/images/" + filename : null);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);

        InOrder inOrder = inOrder(imageRecordsRepository, imageStorageService);
        inOrder.verify(imageStorageService).save(file);
        inOrder.verify(imageRecordsRepository).updateStatus(filename, expectedUploadStatus);
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

        when(imageRecordsRepository.existsById(filename))
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
    @Parameters({
            "requester | false",
            "notRequester | true"
    })
    public void testDeleteImage(String ownerId, boolean expectThrow) throws IOException {
        // Arrange
        String userId = "requester";
        String name = "picture";
        when(imageRecordsRepository.findByName(name))
                .thenReturn(new ImageRecord(null, null, ownerId, null, null, null));

        // Act
        if (expectThrow) {
            assertThrows(
                    NotAllowedToDeleteImageException.class,
                    () ->  target.deleteImage(name, userId));
        } else {
            target.deleteImage(name, userId);
        }

        // Assert
        if (expectThrow) {
            verify(imageStorageService, never()).delete(any());
            verify(imageRecordsRepository, never()).deleteById(any());
        } else {
            verify(imageStorageService).delete(name);
            verify(imageRecordsRepository).deleteById(name);
        }
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
