package imagerepo.repositories;

import com.google.common.collect.ImmutableSet;
import imagerepo.models.ImageRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class ImageRecordsRepositoryImpl implements ImageRecordsRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    public ImageRecordsRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public ImageRecord updateStatus(String name, ImageRecord.UploadStatus uploadStatus) {
        return mongoTemplate.findAndModify(
                Query.query(Criteria.where(ImageRecord.Fields.name).is(name)),
                Update.update(ImageRecord.Fields.uploadStatus, uploadStatus),
                FindAndModifyOptions.options().returnNew(true),
                ImageRecord.class);
    }

    @Override
    public boolean isPendingOrSucceeded(String name) {
        return mongoTemplate.exists(
                Query.query(Criteria.where(ImageRecord.Fields.name).is(name))
                        .addCriteria(Criteria.where(ImageRecord.Fields.uploadStatus)
                                .in(ImmutableSet.of(ImageRecord.UploadStatus.pending, ImageRecord.UploadStatus.succeeded))),
                ImageRecord.class);
    }
}
