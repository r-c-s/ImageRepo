package fileupload.repositories;

import fileupload.models.FileUploadRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public class FileUploadRecordsRepositoryImpl implements FileUploadRecordsRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    public FileUploadRecordsRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public FileUploadRecord updateStatus(String name, FileUploadRecord.UploadStatus uploadStatus) {
        return mongoTemplate.findAndModify(
                Query.query(Criteria.where(FileUploadRecord.Fields.name).is(name)),
                Update.update(FileUploadRecord.Fields.uploadStatus, uploadStatus),
                FindAndModifyOptions.options().returnNew(true),
                FileUploadRecord.class);
    }

    @Override
    public boolean isPendingOrSucceeded(String name) {
        return mongoTemplate.exists(
                Query.query(Criteria.where(FileUploadRecord.Fields.name).is(name))
                        .addCriteria(Criteria.where(FileUploadRecord.Fields.uploadStatus)
                                .in(Set.of(FileUploadRecord.UploadStatus.pending, FileUploadRecord.UploadStatus.succeeded))),
                FileUploadRecord.class);
    }
}
