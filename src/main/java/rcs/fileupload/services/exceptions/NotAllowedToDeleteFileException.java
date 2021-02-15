package rcs.fileupload.services.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class NotAllowedToDeleteFileException extends RuntimeException {

    public NotAllowedToDeleteFileException(String userId, String fileName) {
        super(String.format(
                "User %s is not allowed to delete file %s",
                userId,
                fileName));
    }
}
