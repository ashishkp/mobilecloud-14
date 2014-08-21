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

package org.magnum.mobilecloud.video;

import java.security.Principal;
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import org.magnum.mobilecloud.video.client.VideoSvcApi;
import org.magnum.mobilecloud.video.repository.Video;
import org.magnum.mobilecloud.video.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Lists;

@Controller
public class VideoAuth2Controller {
	
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
	@Autowired
	private VideoRepository videos;
	
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList() {
		return Lists.newArrayList(videos.findAll());
	}
	
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH + "/{id}", method=RequestMethod.GET)
	public @ResponseBody Video getVideoById(@PathVariable("id") long id,
			HttpServletResponse response) {
		
		Video vid = videos.findOne(id);
		
		if (vid != null) {
			return vid;
		}
		
		// In case id not found, return 404 - not found.
		response.setStatus(404);
		return null;
	}
	
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v) {
		return videos.save(v);
	}
	
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH + "/{id}/like", method=RequestMethod.POST)
	public void likeVideo(@PathVariable("id") long id,
			Principal p,
			HttpServletResponse response) {
		
		Video vid = videos.findOne(id);
		
		if (vid != null) {
			String likedUser = p.getName();
			if (vid.addLikedUser(likedUser)) {
				vid.incrementLikeByOne();
				videos.save(vid);
				response.setStatus(200);
			}
			else {
				// User has already liked the video
				response.setStatus(400);
			}
		}
		else {
			// In case id not found, return 404 - not found.
			response.setStatus(404);
		}
	}
	
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH + "/{id}/unlike", method=RequestMethod.POST)
	public void unlikeVideo(@PathVariable("id") long id,
			Principal p,
			HttpServletResponse response) {
		
		Video vid = videos.findOne(id);
		
		if (vid != null) {
			String likedUser = p.getName();
			if (vid.removeLikedUser(likedUser)) {
				vid.decrementLikeByOne();
				videos.save(vid);
				response.setStatus(200);
			}
			else {
				// User has not yet liked the video
				response.setStatus(400);
			}
		}
		else {
			// In case id not found, return 404 - not found.
			response.setStatus(404);
		}
	}
	
	@RequestMapping(value=VideoSvcApi.VIDEO_TITLE_SEARCH_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> findByTitle
							(@RequestParam(VideoSvcApi.TITLE_PARAMETER) String title) {
		
		return videos.findByName(title);
	}
	
	@RequestMapping(value=VideoSvcApi.VIDEO_DURATION_SEARCH_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> findByDurationLessThan 
					(@RequestParam(VideoSvcApi.DURATION_PARAMETER) long duration) {
		
		return videos.findByDurationLessThan(duration);
	}
	
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH + "/{id}/likedby", method=RequestMethod.GET)
	public @ResponseBody Collection<String> getUsersWhoLikedVideo(@PathVariable("id") long id,
			HttpServletResponse response) {
		
		Video vid = videos.findOne(id);
		
		if (vid != null) {
			return vid.getLikedUsers();
		}
		else {
			// In case id not found, return 404 - not found.
			response.setStatus(404);
			return null;
		}
	}
}
