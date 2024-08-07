package com.stream.app.services;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.stream.app.entities.Video;

public interface VideoService {

	Video saveVideo(Video video, MultipartFile file);

	Video getVideoById(String videoId);

	Video getVideoByTitle(String title);

	List<Video> getAllVideos();
}
