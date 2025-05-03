import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


@Path("/watcher")
public class FolderWatcherResource {

    private static final List<String> logEntries = new CopyOnWriteArrayList<>();
    private static int uploadCount = 0;
    private static int errorCount = 0;
    private static String lastUploaded = "–";

    public static void log(String message) {
        logEntries.add(0, message);
        if (logEntries.size() > 100) {
            logEntries.remove(logEntries.size() - 1);
        }
    }

    public static void incrementUpload(String fileName) {
        uploadCount++;
        lastUploaded = fileName;
        log("✅ Uploaded: " + fileName);
    }

    public static void incrementError(String error) {
        errorCount++;
        log("❌ Error: " + error);
    }

    @GET
    @Path("/log")
    @Produces(MediaType.APPLICATION_JSON)
    public static List<String> getLog() {
        return logEntries;
    }

    @GET
    @Path("/stats")
    @Produces(MediaType.APPLICATION_JSON)
    public Stats getStats() {
        return new Stats(uploadCount, errorCount, lastUploaded);
    }

    public static class Stats {
        public int uploads;
        public int errors;
        public String lastFile;

        public Stats(int uploads, int errors, String lastFile) {
            this.uploads = uploads;
            this.errors = errors;
            this.lastFile = lastFile;
        }
    }
}
