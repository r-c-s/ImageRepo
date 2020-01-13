package imagerepo.services;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ImageStorageService {

    void save(MultipartFile file) throws IOException;
    void delete(String filename) throws IOException;
    Resource load(String filename) throws IOException;
}
