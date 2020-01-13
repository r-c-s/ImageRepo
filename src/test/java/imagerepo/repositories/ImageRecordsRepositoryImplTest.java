package imagerepo.repositories;

import imagerepo.models.ImageRecord;
import imagerepo.testutils.MongoRepositoryTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
public class ImageRecordsRepositoryImplTest extends MongoRepositoryTestBase {

    private MongoTemplate mongoTemplate;
    private ImageRecordsRepositoryImpl target;

    @Before
    public void setup() {
        mongoTemplate = getMongoTemplate();
        target = new ImageRecordsRepositoryImpl(mongoTemplate);
    }

    @Test
    public void testUpdateStatus() {
        // Arrange
        ImageRecord existing = new ImageRecord(
                "image.png",
                "image/png",
                "userId",
                new Date(1),
                ImageRecord.UploadStatus.pending,
                null);

        mongoTemplate.save(existing);

        // Act
        ImageRecord updated = target.updateStatus(existing.getName(), ImageRecord.UploadStatus.succeeded);

        // Assert
        assertThat(updated).usingRecursiveComparison()
                .isEqualTo(withStatus(existing, ImageRecord.UploadStatus.succeeded));
    }

    private ImageRecord withStatus(ImageRecord record, ImageRecord.UploadStatus uploadStatus) {
        return new ImageRecord(
                record.getName(),
                record.getType(),
                record.getUserId(),
                record.getDateUploaded(),
                uploadStatus,
                record.getUrl());
    }
}