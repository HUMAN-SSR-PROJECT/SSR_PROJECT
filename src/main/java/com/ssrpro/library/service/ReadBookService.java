package com.ssrpro.library.service;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssrpro.library.dao.ReadBookDao;
import com.ssrpro.library.dto.request.ReadBookReq;
import com.ssrpro.library.dto.response.ReadBookRes;
import com.ssrpro.library.dto.response.ReadSoonRes;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadBookService {
  private final ReadBookDao readBookDao;

  // 아래 memberId는 전부 security에서 꺼내올 예정
  // 읽을 책 추가
  @Transactional
  public boolean addToReadSoon(Long memberId, Long bookId) {
    if (readBookDao.isReadSoonDuplicate(memberId, bookId)) {
      throw new IllegalStateException("이미 등록되어 있습니다.");
    }
    return readBookDao.addToReadSoon(memberId, bookId);
  }

  // 내 서재 - 읽을 책 삭제
  @Transactional
  public boolean deleteReadSoon(Long memberId, Long bookId) {
    if (!readBookDao.isReadSoonDuplicate(memberId, bookId)) {
      throw new IllegalStateException("등록되어있지 않은 책입니다.");
    }
    return readBookDao.deleteReadSoon(memberId, bookId);
  }

  // 내 서재 - 읽을 책 전체 조회
  public List<ReadSoonRes> readSoonList(Long memberId) {
    List<ReadSoonRes> readSoonRes = readBookDao.readSoonList(memberId);
    return (readSoonRes != null) ? readSoonRes : Collections.emptyList();
  }

  // 내 서재 - 읽을 책 → 읽는 중
  @Transactional
  public boolean addToReading(Long memberId, Long bookId) {
    if (readBookDao.isReadBookDuplicate(memberId, bookId)) {
      throw new IllegalStateException("이미 등록되어 있습니다.");
    }
    return readBookDao.addToReading(memberId, bookId);
  }

  // 내 서재 - 읽는 중 전체 조회
  public List<ReadBookRes> readingList(Long memberId) {
    List<ReadBookRes> readBookRes = readBookDao.readingList(memberId);
    return (readBookRes != null) ? readBookRes : Collections.emptyList();
  }

  // 내 서재 - 읽는 중 / 완독 삭제
  @Transactional
  public boolean deleteReading(Long memberId, Long bookId) {
    if (!readBookDao.isReadBookDuplicate(memberId, bookId)) {
      throw new IllegalStateException("등록되어있지 않은 책입니다.");
    }
    return readBookDao.deleteReading(memberId, bookId);
  }

  // 내 서재 - 읽는 중 → 완독, 완독 상세 수정
  @Transactional
  public boolean changeToReaded(ReadBookReq readBookReq, Long memberId) {
    if (!readBookDao.isReadBookDuplicate(memberId, memberId)) {
      throw new IllegalStateException("존재하지 않는 정보입니다.");
    }
    return readBookDao.changeToReaded(readBookReq, memberId);
  }

  // 내 서재 - 완독 전체 조회
  public List<ReadBookRes> readedList(Long memberId) {
    List<ReadBookRes> readBookRes = readBookDao.readedList(memberId);
    return (readBookRes != null) ? readBookRes : Collections.emptyList();
  }

  // 내 서재 - 완독 상세 조회
  public ReadBookRes readedInfo(Long memberId, Long bookId) {
    ReadBookRes readBookRes = readBookDao.readedInfo(memberId, bookId);
    return readBookRes;
  }

  // 독서 분석 리포트 - 통계 계산용 리스트
  public List<ReadBookRes> getRawData(Long memberId) {
    List<ReadBookRes> readBookRes = readBookDao.getRawData(memberId);
    return (readBookRes != null) ? readBookRes : Collections.emptyList();
  }
}
