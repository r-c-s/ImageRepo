package imagerepo.repositories;

import imagerepo.models.ImageRecord;
import imagerepo.testutils.MongoRepositoryTestBase;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitParamsRunner.class)
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

    @Test
    @Parameters({
            "failed | false",
            "succeeded | true",
            "pending | true"
    })
    public void testExistsByNameAndIsPendingOrSucceeded(ImageRecord.UploadStatus status, boolean expectedResult) {
        // Arrange
        ImageRecord existing = new ImageRecord(
                "image.png",
                "image/png",
                "userId",
                new Date(1),
                status,
                null);

        mongoTemplate.save(existing);

        // Act
        boolean actual = target.isPendingOrSucceeded(existing.getName());

        // Assert
        assertThat(actual).isEqualTo(expectedResult);
    }

    private ImageRecord withStatus(ImageRecord record, ImageRecord.UploadStatus uploadStatus) {
        return new ImageRecord(
                record.getName(),
                record.getType(),
                record.getUsername(),
                record.getDateUploaded(),
                uploadStatus,
                record.getUrl());
    }
}