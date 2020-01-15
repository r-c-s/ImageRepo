package imagerepo.services;

import imagerepo.services.exceptions.ImageNotFoundException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

public class LocalStorageService implements ImageStorageService {

    private String storageDir;
    private FileFactory fileFactory;

    public LocalStorageService(String storageDir, FileFactory fileFactory) {
        this.storageDir = storageDir;
        this.fileFactory = fileFactory;
    }

    @Override
    public void save(MultipartFile file) throws IOException {
        file.transferTo(newFile(file.getOriginalFilename()));
    }

    @Override
    public void delete(String filename) throws IOException {
        File file = newFile(filename);
        boolean deleted = file.delete() || !file.exists();
        if (!deleted) {
            throw new IOException("Failed to delete " + filename);
        }
    }

    @Override
    public Resource load(String filename) throws IOException {
        File file = newFile(filename);
        if (file.exists()) {
            return new FileSystemResource(file);
        } else {
            throw new ImageNotFoundException(filename);
        }
    }

    private File newFile(String filename) {
        return fileFactory.newFile(buildFilePath(filename));
    }

    private String buildFilePath(String filename) {
        return storageDir + "/" + filename;
    }
}