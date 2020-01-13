package imagerepo.services.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class NotAllowedToDeleteImageException extends RuntimeException {

    public NotAllowedToDeleteImageException(String userId, String fileName) {
        super(String.format(
                "User %s is not allowed to delete image %s",
                userId,
                fileName));
    }
}
