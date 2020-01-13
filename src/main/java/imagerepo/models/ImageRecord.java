package imagerepo.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImageRecord {

    public static final class Fields {
        public static final String name = "name";
        public static final String uploadStatus = "uploadStatus";
    }

    public enum UploadStatus {
        succeeded,
        failed,
        pending
    }

    @Id
    private String name;
    private String type;
    private String userId;
    private Date dateUploaded;
    private UploadStatus uploadStatus;
    @Transient
    private String url;
}
