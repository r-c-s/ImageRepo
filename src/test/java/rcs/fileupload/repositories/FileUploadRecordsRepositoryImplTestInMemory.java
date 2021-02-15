package rcs.fileupload.repositories;

import rcs.fileupload.models.FileUploadRecord;
import rcs.fileupload.testutils.InMemoryMongoRepositoryTestBase;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitParamsRunner.class)
public class FileUploadRecordsRepositoryImplTestInMemory extends InMemoryMongoRepositoryTestBase {

    private MongoTemplate mongoTemplate;
    private FileUploadRecordsRepositoryImpl target;

    @Before
    public void setup() {
        mongoTemplate = getMongoTemplate();
        target = new FileUploadRecordsRepositoryImpl(mongoTemplate);
    }

    @Test
    public void testUpdateStatus() {
        // Arrange
        FileUploadRecord existing = new FileUploadRecord(
                "image.png",
                "image/png",
                "userId",
                LocalDateTime.now(),
                FileUploadRecord.UploadStatus.pending,
                null);

        mongoTemplate.save(existing);

        // Act
        FileUploadRecord updated = target.updateStatus(existing.getName(), FileUploadRecord.UploadStatus.succeeded);

        // Assert
        assertThat(updated).usingRecursiveComparison()
                .isEqualTo(withStatus(existing, FileUploadRecord.UploadStatus.succeeded));
    }

    @Test
    @Parameters({
            "failed | false",
            "succeeded | true",
            "pending | true"
    })
    public void testExistsByNameAndIsPendingOrSucceeded(FileUploadRecord.UploadStatus status, boolean expectedResult) {
        // Arrange
        FileUploadRecord existing = new FileUploadRecord(
                "image.png",
                "image/png",
                "userId",
                LocalDateTime.now(),
                status,
                null);

        mongoTemplate.save(existing);

        // Act
        boolean actual = target.isPendingOrSucceeded(existing.getName());

        // Assert
        assertThat(actual).isEqualTo(expectedResult);
    }

    private FileUploadRecord withStatus(FileUploadRecord record, FileUploadRecord.UploadStatus uploadStatus) {
        return new FileUploadRecord(
                record.getName(),
                record.getType(),
                record.getUsername(),
                record.getDateUploaded(),
                uploadStatus,
                record.getUrl());
    }
}