package com.example.ajouevent.service;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {
	@Value("${cloud.aws.s3.bucket}")
	private String bucket;

	private final AmazonS3 amazonS3;

	/**
	 * presigned url 발급
	 * @param prefix 버킷 디렉토리 이름
	 * @param fileName 클라이언트가 전달한 파일명 파라미터
	 * @return presigned url
	 */
	public Map<String, String> getPresignedUrl(String prefix, String fileName) {
		if (!prefix.isEmpty()) {
			fileName = createPath(prefix, fileName);
			log.info("fileName" + fileName);
		}

		GeneratePresignedUrlRequest generatePresignedUrlRequest = getGeneratePreSignedUrlRequest(bucket, fileName);
		URL url = amazonS3.generatePresignedUrl(generatePresignedUrlRequest);

		return Map.of("url", url.toString());
	}

	// 여러 개의 사전 서명된 URL을 생성하는 메서드
	public List<Map<String, String>> generateMultiplePresignedUrls(String prefix, int fileCount) {
		List<Map<String, String>> urls = new ArrayList<>();
		for (int i = 0; i < fileCount; i++) {
			String fileName = createPath(prefix, UUID.randomUUID().toString());  // 각 파일에 대해 고유한 경로를 생성
			GeneratePresignedUrlRequest request = getGeneratePreSignedUrlRequest(bucket, fileName);
			URL url = amazonS3.generatePresignedUrl(request);
			urls.add(Map.of("url", url.toString()));
		}
		return urls;
	}

	/**
	 * 파일 업로드용(PUT) presigned url 생성
	 * @param bucket 버킷 이름
	 * @param fileName S3 업로드용 파일 이름
	 * @return presigned url
	 */
	private GeneratePresignedUrlRequest getGeneratePreSignedUrlRequest(String bucket, String fileName) {
		GeneratePresignedUrlRequest generatePresignedUrlRequest =
			new GeneratePresignedUrlRequest(bucket, fileName)
				.withMethod(HttpMethod.PUT)
				.withExpiration(getPreSignedUrlExpiration());
		generatePresignedUrlRequest.addRequestParameter(
			Headers.S3_CANNED_ACL,
			CannedAccessControlList.PublicRead.toString());
		return generatePresignedUrlRequest;
	}

	/**
	 * presigned url 유효 기간 설정
	 * @return 유효기간
	 */
	private Date getPreSignedUrlExpiration() {
		Date expiration = new Date();
		long expTimeMillis = expiration.getTime();
		expTimeMillis += 1000 * 60 * 2;
		expiration.setTime(expTimeMillis);
		return expiration;
	}

	/**
	 * 파일 고유 ID를 생성
	 * @return 36자리의 UUID
	 */
	private String createFileId() {
		return UUID.randomUUID().toString();
	}

	/**
	 * 파일의 전체 경로를 생성
	 * @param prefix 디렉토리 경로
	 * @return 파일의 전체 경로
	 */
	private String createPath(String prefix, String fileName) {
		String fileId = createFileId();
		return String.format("%s/%s", prefix, fileId + fileName);
	}

	// s3 버킷에 올라간 객체 url 리턴
	public String getS3(String fileName) {
		return amazonS3.getUrl(bucket, fileName).toString();
	}

	public void deleteFile(String fileName) throws IOException {
		try {
			amazonS3.deleteObject(bucket, fileName);
		} catch (SdkClientException e) {
			throw new IOException("Error deleting file from S3", e);
		}
	}

}