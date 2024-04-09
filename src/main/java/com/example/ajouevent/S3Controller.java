package com.example.ajouevent;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class S3Controller {

	private final S3Upload s3Upload;

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
}