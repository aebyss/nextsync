import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.smallrye.common.annotation.Blocking;

import java.io.InputStream;
import java.nio.file.*;

@Path("/api/upload")
public class UploadResource {

    @Inject
    NextcloudNativeClient uploader;

    @POST
    @Path("/{filename}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Blocking // required for file IO
    public Response upload(@PathParam("filename") String filename,
                           @HeaderParam("X-Remote-Path") String remoteName,
                           @HeaderParam("X-URL") String url,
                           @HeaderParam("X-User") String username,
                           @HeaderParam("X-Pass") String password,
                           InputStream stream) {
        try {
            java.nio.file.Path temp = Files.createTempFile("upload_", "_" + filename);
            Files.copy(stream, temp, StandardCopyOption.REPLACE_EXISTING);

            String remotePath = NextcloudNativeClient.encodeFullRemotePath(remoteName);
            String fullUrl = NextcloudNativeClient.joinPaths(url, remotePath);



            long size = Files.size(temp);
            System.out.println("[JAVA] Temp file created: " + temp + " (" + size + " bytes)");

            System.out.println("Received:");
            System.out.println("X-URL: " + url);
            System.out.println("X-Remote-Path: " + remoteName);

            System.out.println("Upload to: " + fullUrl);

            int result = uploader.upload(temp.toString(), fullUrl, username, password);

            Files.deleteIfExists(temp);
            return Response.ok("Upload finished. Result code: " + result).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("Upload failed: " + e.getMessage()).build();
        }
    }
}
