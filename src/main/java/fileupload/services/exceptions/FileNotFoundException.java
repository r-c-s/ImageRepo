package fileupload.services.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class FileNotFoundException extends IOException {

    public FileNotFoundException(String name) {
        super(name + " not found");
    }
}
