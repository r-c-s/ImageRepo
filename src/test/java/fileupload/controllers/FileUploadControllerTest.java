package fileupload.controllers;

import fileupload.models.FileUploadRecord;
import fileupload.services.FileUploadService;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import rcs.auth.api.AuthUtils;
import rcs.auth.api.AuthenticatedHttpServletRequest;
import rcs.auth.api.models.AuthenticatedUser;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(JUnitParamsRunner.class)
public class FileUploadControllerTest {

    private FileUploadService service;
    private AuthUtils authUtils;
    private FileUploadController target;

    @Before
    public void setup() {
        service = mock(FileUploadService.class);
        authUtils = mock(AuthUtils.class);
        target = new FileUploadController(service, authUtils);
    }

    @Test
    public void testGetListOfFiles() {
        // Arrange
        List<FileUploadRecord> expected = List.of(mock(FileUploadRecord.class), mock(FileUploadRecord.class));
        when(service.getFiles()).thenReturn(expected);

        // Act
        ResponseEntity<List<FileUploadRecord>> actual = target.getFiles();

        // Assert
        assertThat(actual.getStatusCodeValue()).isEqualTo(200);
        assertThat(actual.getBody()).usingFieldByFieldElementComparator().isEqualTo(expected);
    }

    @Test
    public void testGetFile() throws IOException {
        // Arrange
        String filename = "filename.jpg";
        Resource mockResource = mock(Resource.class);
        when(mockResource.getFilename()).thenReturn(filename);
        when(service.getFile(filename)).thenReturn(mockResource);

        // Act
        ResponseEntity<Resource> actual = target.getFile(filename);

        // Assert
        assertThat(actual.getStatusCodeValue()).isEqualTo(200);
        assertThat(actual.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_JPEG);
    }

    @Test
    @Parameters({
            "succeeded | http://fileupload.com/someimage.jpg | 201",
            "failed | null | 500"
    })
    public void testUploadFile(
            FileUploadRecord.UploadStatus recordStatus,
            String url,
            int expectedHttpStatus) {

        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        url = url.equals("null") ? null : url;

        AuthenticatedUser user = mock(AuthenticatedUser.class);
        AuthenticatedHttpServletRequest mockRequest = mock(AuthenticatedHttpServletRequest.class);
        when(authUtils.tryGetLoggedInUser(mockRequest))
                .thenReturn(Optional.of(user));

        FileUploadRecord record = mock(FileUploadRecord.class);
        when(record.getUploadStatus()).thenReturn(recordStatus);
        when(record.getUrl()).thenReturn(url);

        when(service.uploadFile(eq(user), eq(file), any())).thenReturn(record);

        // Act
        ResponseEntity<FileUploadRecord> actual = target.uploadFile(file, mockRequest);

        // Assert
        assertThat(actual.getStatusCodeValue()).isEqualTo(expectedHttpStatus);
        assertThat(actual.getBody()).isEqualTo(record);
        assertThat(Optional.ofNullable(actual.getHeaders().getLocation()).map(Object::toString).orElse(null))
                .isEqualTo(url);
    }

    @Test
    public void testDeleteFile() throws IOException {
        // Arrange
        String name = "filename";

        // Act
        ResponseEntity<Void> actual = target.deleteFile(name);

        // Assert
        assertThat(actual.getStatusCodeValue()).isEqualTo(204);
        verify(service).deleteFile(name);
    }
}