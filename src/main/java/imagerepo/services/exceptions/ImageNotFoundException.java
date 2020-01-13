package imagerepo.services.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ImageNotFoundException extends IOException {

    public ImageNotFoundException(String name) {
        super(name + " not found");
    }
}
