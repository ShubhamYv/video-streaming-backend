package com.stream.app.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.stream.app.constants.AppConstants;
import com.stream.app.entities.Video;
import com.stream.app.payload.CustomMessage;
import com.stream.app.services.VideoService;

@RestController
@RequestMapping("/api/v1/videos")
@CrossOrigin("*")
public class VideoController {

	private VideoService videoService;

	public VideoController(VideoService videoService) {
		this.videoService = videoService;
	}

	@PostMapping
	public ResponseEntity<?> createVideo(@RequestParam("file") MultipartFile file, 
			@RequestParam("title") String title,
			@RequestParam("description") String description) {

		Video video = new Video();
		video.setTitle(title);
		video.setDescription(description);
		video.setVideoId(UUID.randomUUID().toString());
		Video savedVideo = videoService.saveVideo(video, file);

		if (savedVideo != null) {
			return ResponseEntity.status(HttpStatus.CREATED).body(savedVideo);
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(CustomMessage.builder().message("Video could not be uploaded!").build());
		}
	}
	
	@GetMapping()
	public ResponseEntity<List<Video>> getAllVideos() {
		return ResponseEntity
				.ok()
				.body(videoService.getAllVideos());
	}

	@GetMapping("/stream/{videoId}")
	public ResponseEntity<Resource> stream(@PathVariable String videoId) {
		Video video = videoService.getVideoById(videoId);
		String contentType = video.getContentType();
		String filePath = video.getFilePath();

		if (contentType == null) {
			contentType = "application/octet-stream";
		}

		Resource resource = new FileSystemResource(filePath);

		return ResponseEntity
				.ok()
				.contentType(MediaType.parseMediaType(contentType))
				.body(resource);
	}
	
	
	// Stream video in chunks
	@GetMapping("/stream/range/{videoId}")
	public ResponseEntity<Resource> streamVideoRange(@PathVariable String videoId,
			@RequestHeader(value = "Range", required = false) String range) {
		
		Video video = videoService.getVideoById(videoId);
		Path path = Paths.get(video.getFilePath());

		Resource resource = new FileSystemResource(path);
		String contentType = video.getContentType();

		if (contentType == null) {
			contentType = "application/octet-stream";
		}

		long fileLength = path.toFile().length();

		if (range == null) {
			return ResponseEntity
					.ok()
					.contentType(MediaType.parseMediaType(contentType))
					.body(resource);
		}

		String[] ranges = range.replace("bytes=", "").split("-");
		Long rangeStart = Long.parseLong(ranges[0]);
		Long rangeEnd = rangeStart + AppConstants.CHUNK_SIZE - 1;
		
		if (rangeEnd >= fileLength) {
			rangeEnd = fileLength - 1;
		}

		InputStream inputStream;
		try {
			inputStream = Files.newInputStream(path);
			inputStream.skip(rangeStart);
			long contentLength = rangeEnd - rangeStart + 1;
			
			byte[] data = new byte[(int) contentLength];
			int read = inputStream.read(data, 0, data.length);
			System.out.println("Read number of bytes::"+ read);
			
			HttpHeaders headers = new HttpHeaders();
			headers.add("Content-Range", "bytes " + rangeStart + "-" + rangeEnd + "/" + fileLength);
			headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
			headers.add("Pragma", "no-cache");
			headers.add("Expires", "0");
			headers.add("X-Content-Type-Options", "nosniff");
			headers.setContentLength(contentLength);
			return ResponseEntity
					.status(HttpStatus.PARTIAL_CONTENT)
					.headers(headers)
					.contentType(MediaType.parseMediaType(contentType))
					.body(new ByteArrayResource(data));

		} catch (IOException ex) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
}
