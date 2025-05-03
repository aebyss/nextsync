import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/api/folders")
public class FolderResource {

    @Inject
    NextcloudNativeClient uploader;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFolders(@QueryParam("url") String baseUrl,
                               @HeaderParam("X-User") String user,
                               @HeaderParam("X-Pass") String pass) {
        List<String> folders = uploader.listFolders(baseUrl, user, pass);
        return Response.ok(folders).build();
    }
}
