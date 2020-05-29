package org.magnum.dataup;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import retrofit.client.Response;
import retrofit.http.*;
import retrofit.mime.TypedFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class VideoService {
    private List<Video> videoList = new ArrayList<>();
    private static final AtomicLong currentId = new AtomicLong(0L);

    private Map<Long,Video> videos = new HashMap<Long, Video>();
    /**
     * This endpoint in the API returns a list of the videos that have
     * been added to the server. The Video objects should be returned as
     * JSON.
     * <p>
     * To manually test this endpoint, run your server and open this URL in a browser:
     * http://localhost:8080/video
     *
     * @return
     */
    public Collection<Video> getVideoList() {
        return videoList;
    }

    /**
     * This endpoint allows clients to add Video objects by sending POST requests
     * that have an application/json body containing the Video object information.
     *
     * @return
     */
    public Video addVideo(Video v) {
        save(v);
        v.setDataUrl(getDataUrl(v.getId()));
        videoList.add(v);
        return v;
    }

    /**
     * This endpoint allows clients to set the mpeg video data for previously
     * added Video objects by sending multipart POST requests to the server.
     * The URL that the POST requests should be sent to includes the ID of the
     * Video that the data should be associated with (e.g., replace {id} in
     * the url /video/{id}/data with a valid ID of a video, such as /video/1/data
     * -- assuming that "1" is a valid ID of a video).
     *
     * @return
     */
    public VideoStatus setVideoData(long id, MultipartFile videoData, HttpServletResponse response) {
        VideoStatus videoStatus = new VideoStatus(VideoStatus.VideoState.PROCESSING);
        if  (!videos.containsKey(id)) {
            //response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "video not found"
            );
        }
        else {
            Video video = videos.get(id);
            try {
                VideoFileManager videoFileManager = VideoFileManager.get();
                videoFileManager.saveVideoData(video, videoData.getInputStream());
                videoData.getInputStream().close();
                return new VideoStatus(VideoStatus.VideoState.READY);
            } catch (IOException e) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                e.printStackTrace();
            }
        }
        return videoStatus;
    }

    /**
     * This endpoint should return the video data that has been associated with
     * a Video object or a 404 if no video data has been set yet. The URL scheme
     * is the same as in the method above and assumes that the client knows the ID
     * of the Video object that it would like to retrieve video data for.
     * <p>
     * This method uses Retrofit's @Streaming annotation to indicate that the
     * method is going to access a large stream of data (e.g., the mpeg video
     * data on the server). The client can access this stream of data by obtaining
     * an InputStream from the Response as shown below:
     * <p>
     * VideoSvcApi client = ... // use retrofit to create the client
     * Response response = client.getData(someVideoId);
     * InputStream videoDataStream = response.getBody().in();
     *
     * @param id
     * @return
     */
    public HttpServletResponse getData(long id,HttpServletResponse response) {
        if  (!videos.containsKey(id)) {
            //response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "video not found"
            );
        }
        try {
            VideoFileManager videoFileManager = VideoFileManager.get();
            Video video = videos.get(id);
            response.setContentType("video/mp4");
            videoFileManager.copyVideoData(video, response.getOutputStream());
            response.getOutputStream().flush();
            response.getOutputStream().close();
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
        return response;
    }
    private String getDataUrl(long videoId){
        String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
        return url;
    }

    private String getUrlBaseForLocalServer() {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String base =
                "http://"+request.getServerName()
                        + ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
        return base;
    }
    public Video save(Video entity) {
        checkAndSetId(entity);
        videos.put(entity.getId(), entity);
        return entity;
    }

    private void checkAndSetId(Video entity) {
        if(entity.getId() == 0){
            entity.setId(currentId.incrementAndGet());
        }
    }

}
