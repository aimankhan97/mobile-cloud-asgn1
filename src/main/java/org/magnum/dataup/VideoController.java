package org.magnum.dataup;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import retrofit.http.Streaming;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;

@Controller
public class VideoController {
    @Autowired
    private VideoService videoService;

    @RequestMapping("/")
    public void index(HttpServletResponse response) {
        response.setContentType("text/plain");
        response.setStatus(200);
        try {
            PrintWriter sendToClient = response.getWriter();
            sendToClient.write("Hello User! This is a Video Control System. \nYou can see or upload any video. ");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    @ResponseBody
    @RequestMapping(value =  VideoSvcApi.VIDEO_SVC_PATH, method = RequestMethod.GET)
    public Collection<Video> getVideoList() {
        return videoService.getVideoList();
    }

    @ResponseBody
    @RequestMapping(value =  VideoSvcApi.VIDEO_SVC_PATH, method = RequestMethod.POST)
    public Video addVideo(@RequestBody Video video) {
        return videoService.addVideo(video);
    }

    @ResponseBody
    @RequestMapping(value = VideoSvcApi.VIDEO_DATA_PATH, method = RequestMethod.POST,
            consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public VideoStatus setVideoData(@PathVariable("id") Long id,
                                    @RequestParam("data") MultipartFile file, HttpServletResponse response) {
        return videoService.setVideoData(id, file, response);
    }

    @RequestMapping(value = VideoSvcApi.VIDEO_DATA_PATH, method = RequestMethod.GET)
    @Streaming
    public HttpServletResponse getData(@PathVariable("id") Long id, HttpServletResponse response) {
        return videoService.getData(id, response);
    }

}
