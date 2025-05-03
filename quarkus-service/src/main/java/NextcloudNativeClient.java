import jakarta.enterprise.context.ApplicationScoped;
import java.lang.foreign.*;
import java.util.List;

import static com.nativecloud.sync.sync_h.upload_file;
import static com.nativecloud.sync.sync_h.list_folders_into_buffer;

@ApplicationScoped
public class NextcloudNativeClient {

    public int upload(String localPath, String remoteUrl, String username, String password) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment local = arena.allocateUtf8String(localPath);
            MemorySegment remote = arena.allocateUtf8String(remoteUrl);
            MemorySegment user = arena.allocateUtf8String(username);
            MemorySegment pass = arena.allocateUtf8String(password);

            return upload_file(local, remote, user, pass);
        }
    }

    public List<String> listFolders(String url, String username, String password) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment remote = arena.allocateUtf8String(url);
            MemorySegment user = arena.allocateUtf8String(username);
            MemorySegment pass = arena.allocateUtf8String(password);
            MemorySegment outBuffer = arena.allocate(1024 * 10); // 10KB buffer

            int result = list_folders_into_buffer(remote, user, pass, outBuffer, 10_000);
            if (result != 0) {
                throw new RuntimeException("Native list_folders_into_buffer failed with code: " + result);
            }

            String all = outBuffer.getUtf8String(0);
            return List.of(all.split("\n"));
        }
    }

    public static String joinPaths(String base, String path) {
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if (path.startsWith("/")) path = path.substring(1);
        return base + "/" + path;
    }

    public static String encodePathComponent(String path) {
        try {
            return java.net.URLEncoder.encode(path, java.nio.charset.StandardCharsets.UTF_8)
                    .replace("+", "%20"); // spaces should be %20, not +
        } catch (Exception e) {
            return path; // fallback (safe)
        }
    }

    public static String encodeFullRemotePath(String path) {
        // Encode only the filename part, leave folders alone
        String[] parts = path.split("/");
        if (parts.length == 0) return encodePathComponent(path);

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) result.append("/");
            if (i == parts.length - 1) {
                result.append(encodePathComponent(parts[i]));
            } else {
                result.append(parts[i]); // folder names stay unencoded
            }
        }
        return result.toString();
    }
}

