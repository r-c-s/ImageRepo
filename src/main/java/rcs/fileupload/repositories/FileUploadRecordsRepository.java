package rcs.fileupload.repositories;

import rcs.fileupload.models.FileUploadRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FileUploadRecordsRepository extends MongoRepository<FileUploadRecord, String>, FileUploadRecordsRepositoryCustom {

    FileUploadRecord findByName(String name);
}
