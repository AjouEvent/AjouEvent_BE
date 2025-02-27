package com.example.ajouevent.domain;

public enum JobStatus {
	PENDING,     // 작업 대기 상태 (초기값)
	IN_PROGRESS, // 작업 진행 중
	SUCCESS,     // 작업 완료 (성공)
	PARTIAL_FAIL, // 작업 완료 (부분 실패)
	FAIL,         // 작업 실패
	NONE		// 보낼 대상이 없음 (푸시 알림 대상 없음)
}