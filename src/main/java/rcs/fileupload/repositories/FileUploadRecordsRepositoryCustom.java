package rcs.fileupload.repositories;

import rcs.fileupload.models.FileUploadRecord;

public interface FileUploadRecordsRepositoryCustom {

    FileUploadRecord updateStatus(String name, FileUploadRecord.UploadStatus uploadStatus);
    boolean isPendingOrSucceeded(String name);
}
