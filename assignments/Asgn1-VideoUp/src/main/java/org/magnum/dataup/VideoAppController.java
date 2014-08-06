/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.magnum.dataup;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class VideoAppController {

	/**
	 * You will need to create one or more Spring controllers to fulfill the
	 * requirements of the assignment. If you use this file, please rename it
	 * to something other than "AnEmptyController"
	 * 
	 * 
		 ________  ________  ________  ________          ___       ___  ___  ________  ___  __       
		|\   ____\|\   __  \|\   __  \|\   ___ \        |\  \     |\  \|\  \|\   ____\|\  \|\  \     
		\ \  \___|\ \  \|\  \ \  \|\  \ \  \_|\ \       \ \  \    \ \  \\\  \ \  \___|\ \  \/  /|_   
		 \ \  \  __\ \  \\\  \ \  \\\  \ \  \ \\ \       \ \  \    \ \  \\\  \ \  \    \ \   ___  \  
		  \ \  \|\  \ \  \\\  \ \  \\\  \ \  \_\\ \       \ \  \____\ \  \\\  \ \  \____\ \  \\ \  \ 
		   \ \_______\ \_______\ \_______\ \_______\       \ \_______\ \_______\ \_______\ \__\\ \__\
		    \|_______|\|_______|\|_______|\|_______|        \|_______|\|_______|\|_______|\|__| \|__|
                                                                                                                                                                                                                                                                        
	 * 
	 */
	
	private static final AtomicLong currentId = new AtomicLong(0L);
	
	private Map<Long,Video> videos = new HashMap<Long, Video>();
	
	private VideoFileManager videoDataMgr;
	
	public VideoAppController() {
		super();
		try {
			videoDataMgr = VideoFileManager.get();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*
	 *   GET /video
	 *   - Returns the list of videos that have been added to the
	 *     server as JSON. The list of videos does not have to be
	 *     persisted across restarts of the server. The list of
	 *     Video objects should be able to be unmarshalled by the
	 *     client into a Collection<Video>.
	 */
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList(){
		return videos.values();
	}
	
	/*
     * POST /video
	 *   - The video data is provided as an application/json request
	 *     body. The JSON should generate a valid instance of the 
	 *     Video class when deserialized by Spring's default 
	 *     Jackson library.
	 *   - Returns the JSON representation of the Video object that
	 *     was stored along with any updates to that object. 
	 *     --The server should generate a unique identifier for the Video
	 *     object and assign it to the Video by calling its setId(...)
	 *     method. The returned Video JSON should include this server-generated
	 *     identifier so that the client can refer to it when uploading the
	 *     binary mpeg video content for the Video.
	 *    -- The server should also generate a "data url" for the
	 *     Video. The "data url" is the url of the binary data for a
	 *     Video (e.g., the raw mpeg data). The URL should be the *full* URL
	 *     for the video and not just the path. You can use a method like the
	 *     following to figure out the name of your server:
	 *     
	 *     	private String getUrlBaseForLocalServer() {
	 *		   HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
	 *		   String base = "http://"+request.getServerName()+((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
	 *		   return base;
	 *		}
	 */
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v){
		checkAndSetId(v);
		v.setDataUrl(getDataUrl(v.getId()));
		videos.put(v.getId(), v);
		return v;
	}
	
	private void checkAndSetId(Video entity) {
		if(entity.getId() == 0){
			entity.setId(currentId.incrementAndGet());
		}
	}
	
	private String getDataUrl(long videoId){
        String url = getUrlBaseForLocalServer() + VideoSvcApi.VIDEO_SVC_PATH + "/" + videoId + "/" + VideoSvcApi.DATA_PARAMETER;
        return url;
    }
	
	private String getUrlBaseForLocalServer() {
		 HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		 String base = "http://"+request.getServerName()+((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
		 return base;
	}
	
	/* POST /video/{id}/data
	 *   - The binary mpeg data for the video should be provided in a multipart
	 *     request as a part with the key "data". The id in the path should be
	 *     replaced with the unique identifier generated by the server for the
	 *     Video. A client MUST *create* a Video first by sending a POST to /video
	 *     and getting the identifier for the newly created Video object before
	 *     sending a POST to /video/{id}/data. 
	 */
	@RequestMapping(value=VideoSvcApi.VIDEO_DATA_PATH, method=RequestMethod.POST)
	public @ResponseBody VideoStatus setVideoData(
			@PathVariable(VideoSvcApi.ID_PARAMETER) long id,
			@RequestParam(VideoSvcApi.DATA_PARAMETER) MultipartFile videoData,
            HttpServletResponse response) {
		
		if (videos.containsKey(id)) {
			Video v = videos.get(id);
			try {
				saveSomeVideo(v, videoData);
			} catch (IOException e) {
				e.printStackTrace();
				response.setStatus(500);
			}
			return new VideoStatus(VideoStatus.VideoState.READY);
		} 
		else {
			//response.setStatus(404);
			try {
				response.sendError(404, "Invalid Video Id");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
			//throw new RuntimeException();
		}
	}
	
	 /* GET /video/{id}/data
	 *   - Returns the binary mpeg data (if any) for the video with the given
	 *     identifier. If no mpeg data has been uploaded for the specified video,
	 *     then the server should return a 404 status code.
	 *     
	 */
	@RequestMapping(value=VideoSvcApi.VIDEO_DATA_PATH, method=RequestMethod.GET)
	public void getVideoData(@PathVariable(VideoSvcApi.ID_PARAMETER) long id,
			HttpServletResponse response) {
		
		if (videos.containsKey(id)) {
			Video v = videos.get(id);
			response.setContentType(v.getContentType());
			try {
				serveSomeVideo(v, response);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				response.setStatus(500);
			}
		}
		else {
			response.setStatus(404);
		}
	}
	
	private void saveSomeVideo(Video v, MultipartFile videoData) throws IOException {
		videoDataMgr.saveVideoData(v, videoData.getInputStream());
 	}
 	
	private void serveSomeVideo(Video v, HttpServletResponse response) throws IOException {
		if (videoDataMgr.hasVideoData(v)) {
			videoDataMgr.copyVideoData(v, response.getOutputStream());
		}
		else {
			response.setStatus(404);
		}
 	}
}
