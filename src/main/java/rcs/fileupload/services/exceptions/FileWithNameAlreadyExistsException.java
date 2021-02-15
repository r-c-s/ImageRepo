package rcs.fileupload.services.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT)
public class FileWithNameAlreadyExistsException extends RuntimeException {

    public FileWithNameAlreadyExistsException(String name) {
        super("A file already exists with the name " + name);
    }
}
