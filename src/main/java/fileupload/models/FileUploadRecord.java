package fileupload.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadRecord {

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
    private String username;
    private LocalDateTime dateUploaded;
    private UploadStatus uploadStatus;
    @Transient
    private String url;
}
