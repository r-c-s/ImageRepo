package imagerepo.controllers;

import com.google.common.collect.ImmutableList;
import imagerepo.models.ImageRecord;
import imagerepo.services.ImageRepoService;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(JUnitParamsRunner.class)
public class ImageRepoControllerTest {

    private ImageRepoService service;
    private ImageRepoController target;

    @Before
    public void setup() {
        service = mock(ImageRepoService.class);
        target = new ImageRepoController(service);
    }

    @Test
    public void testGetListOfImages() {
        // Arrange
        List<ImageRecord> expected = ImmutableList.of(mock(ImageRecord.class), mock(ImageRecord.class));
        when(service.getImages()).thenReturn(expected);

        // Act
        ResponseEntity<List<ImageRecord>> actual = target.getImages();

        // Assert
        assertThat(actual.getStatusCodeValue()).isEqualTo(200);
        assertThat(actual.getBody()).usingFieldByFieldElementComparator().isEqualTo(expected);
    }

    @Test
    public void testGetImage() throws IOException {
        // Arrange
        String name = "imagename";
        Resource mockResource = mock(Resource.class);
        when(mockResource.getFilename()).thenReturn("image.jpg");
        when(service.getImage(name)).thenReturn(mockResource);

        // Act
        ResponseEntity<Resource> actual = target.getImage(name);

        // Assert
        assertThat(actual.getStatusCodeValue()).isEqualTo(200);
        assertThat(actual.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_JPEG);
    }

    @Test
    @Parameters({
            "succeeded | 201 | true",
            "failed | 500 | false"
    })
    public void testUploadImage(
            ImageRecord.UploadStatus recordStatus,
            int expectedHttpStatus,
            boolean expectLocationHeader) throws URISyntaxException {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        String userId = "userId";
        String url = "http://localhost:8080/imagerepo/api/images/someimage.jpg";

        ImageRecord record = mock(ImageRecord.class);
        when(record.getUploadStatus()).thenReturn(recordStatus);
        when(record.getUrl()).thenReturn(url);

        when(service.uploadImage(eq(userId), eq(file), any())).thenReturn(record);

        // Act
        ResponseEntity<ImageRecord> actual = target.uploadImage(file, userId);

        // Assert
        assertThat(actual.getStatusCodeValue()).isEqualTo(expectedHttpStatus);
        assertThat(actual.getBody()).isEqualTo(record);
        if (expectLocationHeader) {
            assertThat(actual.getHeaders().getLocation().toString()).isEqualTo(url);
        }
    }

    @Test
    public void testDeleteImage() throws IOException {
        // Arrange
        String name = "filename";
        String userId = "userId";

        // Act
        ResponseEntity<Void> actual = target.deleteImage(name, userId);

        // Assert
        assertThat(actual.getStatusCodeValue()).isEqualTo(204);
        verify(service).deleteImage(name, userId);
    }
}