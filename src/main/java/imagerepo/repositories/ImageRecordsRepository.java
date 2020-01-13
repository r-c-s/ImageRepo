package imagerepo.repositories;

import imagerepo.models.ImageRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ImageRecordsRepository extends MongoRepository<ImageRecord, String>, ImageRecordsRepositoryCustom {

    List<ImageRecord> findAll();
    ImageRecord findByName(String name);
}
