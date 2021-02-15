package rcs.fileupload.services;

import rcs.fileupload.services.exceptions.FileNotFoundException;
import rcs.fileupload.services.utils.FileFactory;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

@RunWith(JUnitParamsRunner.class)
public class LocalStorageServiceTest {

    private String storageDir;
    private FileFactory mockFileFactory;
    private LocalStorageService target;

    @Before
    public void setup() {
        storageDir = "C:\\test-storage-dir";
        mockFileFactory = mock(FileFactory.class);
        target = new LocalStorageService(storageDir, mockFileFactory);
    }

    @Test
    public void save() throws IOException {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        String filename = "filename.gif";
        when(file.getOriginalFilename()).thenReturn(filename);
        File mockFile = mock(File.class);
        when(mockFileFactory.newFile(storageDir + "/" + filename))
                .thenReturn(mockFile);

        // Act
        target.save(file);

        // Assert
        verify(file).transferTo(mockFile);
    }

    @Test
    @Parameters({
            "true | true",
            "false | false",
    })
    public void testDeleteHappyPath(boolean deleted, boolean exists) throws IOException {
        // Arrange
        String filename = "filename.gif";
        File file = mock(File.class);
        when(mockFileFactory.newFile(storageDir + "/" + filename)).thenReturn(file);

        when(file.delete()).thenReturn(deleted);
        when(file.exists()).thenReturn(exists);

        // Act
        target.delete(filename);

        // Assert
        verify(file).delete();
    }

    @Test
    public void testDeleteWhenFails() {
        // Arrange
        String filename = "filename.gif";
        File file = mock(File.class);
        when(mockFileFactory.newFile(storageDir + "/" + filename)).thenReturn(file);

        when(file.delete()).thenReturn(false);
        when(file.exists()).thenReturn(true);

        // Act & Assert
        assertThrows(
                IOException.class,
                () -> target.delete(filename));
    }

    @Test
    public void testLoadHappyPath() throws IOException {
        // Arrange
        String filename = "filename.gif";
        File file = mock(File.class);
        when(mockFileFactory.newFile(storageDir + "/" + filename)).thenReturn(file);
        when(file.exists()).thenReturn(true);

        // Act
        Resource actual = target.load(filename);

        // Assert
        assertThat(actual.getFile()).isEqualTo(file);
    }

    @Test
    public void testLoadNotFound() {
        // Arrange
        String filename = "filename.gif";
        File file = mock(File.class);
        when(mockFileFactory.newFile(storageDir + "/" + filename)).thenReturn(file);
        when(file.exists()).thenReturn(false);

        // Act & Assert
        assertThrows(
                FileNotFoundException.class,
                () -> target.load(filename));
    }
}
