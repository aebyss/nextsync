import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.inject.Inject;
import java.util.List;

@Path("/native")
public class NativeTestResource {

    @Inject
    NextcloudNativeClient nativeClient;

    @GET
    @Path("/sort")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Integer> testSort() {
        int[] array = {9, 1, 5, 3, 7, 2};
        return nativeClient.sortArray(array);
    }

    @GET
    @Path("/pi")
    @Produces(MediaType.TEXT_PLAIN)
    public String getPi() {
        double value = com.nativecloud.sync.sync_h.PI();
        return "PI = " + value;
    }
}