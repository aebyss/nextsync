import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;


@Startup
@ApplicationScoped
public class FolderWatcher {


    @Inject
    NextcloudNativeClient uploader;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Logger log = Logger.getLogger(FolderWatcher.class);
    private static final String WATCH_FOLDER = "/home/edo/watch"; // your folder
    private static final String NEXTCLOUD_BASE_URL = "https://nextcloud.edinf.dev/remote.php/dav/files/admin/";

    @PostConstruct
    public void onStart() {
        log.info("FolderWatcher started...");
        executor.submit(this::watchFolder);
    }

    private void watchFolder() {
        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();
            Path dir = Paths.get(WATCH_FOLDER);
            dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);

            log.info("Watching folder: " + WATCH_FOLDER);

            while (true) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path fileName = (Path) event.context();
                    Path fullPath = dir.resolve(fileName);


                    String name = fileName.toString();

                    if (name.endsWith("~") || name.startsWith(".") || name.endsWith(".tmp")) {
                        log.info("Skipping temp file: " + name);
                        continue;
                    }

                    FolderWatcherResource.log("New file detected: " + fullPath);
                    uploadToNextcloud(fullPath);
                }
                key.reset();
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void uploadToNextcloud(Path filePath) {
        String localPath = filePath.toString();
        String remoteUrl = NEXTCLOUD_BASE_URL + filePath.getFileName().toString();
        String username = System.getenv("NEXTCLOUD_USER");
        String password = System.getenv("NEXTCLOUD_PASS");

        if (username == null || password == null) {
            System.err.println("Missing env vars for NEXTCLOUD_USER or NEXTCLOUD_PASS");
            return;
        }

        int result = uploader.upload(localPath, remoteUrl, username, password);
        System.out.println("Upload finished with result code: " + result);

        if (result == 0) {
            FolderWatcherResource.incrementUpload(filePath.getFileName().toString());
        } else {
            FolderWatcherResource.incrementError("Upload failed (code: " + result + ")");
        }
    }


}
