package com.example.ajouevent;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
public class S3Controller {

	private final S3Upload s3Upload;
	private final FileService fileService;

	@PostMapping("/api/auth/image")
	public String imageUpload(@RequestPart(required = false) List<MultipartFile> multipartFiles) {

		if (multipartFiles.isEmpty()) {
			return "파일이 유효하지 않습니다.";
		}
		try {
			// s3Upload.uploadFiles(multipartFiles, "static");
			return "파일 업로드 성공";
		} catch (Exception e) {
			e.printStackTrace();
			return "파일이 유효하지 않습니다.";
		}
	}

	@GetMapping("/api/file/presigned-url/{fileName}")
	public String getPresignedUrl(
		@PathVariable(name = "fileName") @Schema(description = "확장자명을 포함해주세요")
		String fileName) {

		return fileService.getPresignedUrl("images", fileName).toString();
	}

	// 여러 개의 사전 서명된 URL을 생성하고 반환하는 엔드포인트
	@PostMapping("/api/file/multiple-presigned-urls")
	public ResponseEntity<List<Map<String, String>>> getMultiplePresignedUrls(
		@RequestParam("prefix") String prefix,
		@RequestParam("fileCount") int fileCount) {
		try {
			List<Map<String, String>> presignedUrls = fileService.generateMultiplePresignedUrls(prefix, fileCount);
			return ResponseEntity.ok(presignedUrls);
		} catch (Exception e) {
			log.error("Error generating presigned URLs: ", e);
			return ResponseEntity.internalServerError().build();
		}
	}

	@GetMapping("/api/file/{fileName}")
	public String getS3(@PathVariable(name = "fileName") String fileName) {
		return fileService.getS3(fileName);
	}
}