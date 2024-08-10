package com.stream.app.services.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.stream.app.entities.Video;
import com.stream.app.repositories.VideoRepository;
import com.stream.app.services.VideoService;

import jakarta.annotation.PostConstruct;

@Service
public class VideoServiceImpl implements VideoService {

	@Value("${files.video}")
	String dir;

	private VideoRepository videoRepository;

	public VideoServiceImpl(VideoRepository videoRepository) {
		this.videoRepository = videoRepository;
	}

	@PostConstruct
	public void init() {
		File file = new File(dir);
		if (!file.exists()) {
			file.mkdir();
			System.out.println("Folder Created");
		} else {
			System.out.println("Folder Already Created");
		}
	}

	@Override
	public Video saveVideo(Video video, MultipartFile file) {
		try {
			String fileName = file.getOriginalFilename();
			String contentType = file.getContentType();
			InputStream inputStream = file.getInputStream();

			String cleanFileName = StringUtils.cleanPath(fileName);
			String cleanFolderName = StringUtils.cleanPath(dir);

			Path path = Paths.get(cleanFolderName, cleanFileName);
			System.out.println("saveVideo||Path::" + path);

			Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
			video.setContentType(contentType);
			video.setFilePath(path.toString());

			return videoRepository.save(video);

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Video getVideoById(String videoId) {
		return videoRepository.findById(videoId)
				.orElseThrow(() -> new RuntimeException("Video not found"));
	}

	@Override
	public Video getVideoByTitle(String title) {
		Optional<Video> video = videoRepository.findByTitle(title);
		return null;
	}

	@Override
	public List<Video> getAllVideos() {
		return videoRepository.findAll();
	}

}
