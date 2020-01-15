package imagerepo.services;

import java.io.File;

/**
 * Allows injecting mock factories for unit-testing
 */
public interface FileFactory {

    File newFile(String filename);
}
