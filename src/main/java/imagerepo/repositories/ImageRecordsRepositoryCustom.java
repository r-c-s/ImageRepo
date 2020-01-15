package imagerepo.repositories;

import imagerepo.models.ImageRecord;

public interface ImageRecordsRepositoryCustom {

    ImageRecord updateStatus(String name, ImageRecord.UploadStatus uploadStatus);
    boolean existsByNameAndIsPendingOrSucceeded(String name);
}
