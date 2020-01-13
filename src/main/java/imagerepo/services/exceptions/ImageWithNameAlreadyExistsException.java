package imagerepo.services.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT)
public class ImageWithNameAlreadyExistsException extends RuntimeException {

    public ImageWithNameAlreadyExistsException(String name) {
        super("An image already exists with the name " + name);
    }
}
