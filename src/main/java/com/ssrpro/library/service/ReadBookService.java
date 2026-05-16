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

  // 읽을 책 추가 / 삭제
  @Transactional
  public boolean addToReadSoon(Long memberId, Long bookId) {
    if (readBookDao.isReadSoonDuplicate(memberId, bookId)) {
      return readBookDao.deleteReadSoon(memberId, bookId);
    }
    return readBookDao.addToReadSoon(memberId, bookId);
  }

  // 내 서재 - 읽을 책 전체 조회
  public List<ReadSoonRes> readSoonList(Long memberId) {
    List<ReadSoonRes> readSoonRes = readBookDao.readSoonList(memberId);
    return (readSoonRes != null) ? readSoonRes : Collections.emptyList();
  }

  // 내 서재 - 읽는 중 추가 / 삭제 (읽는 중 일때만)
  // 1 : 데이터 없음, 2 : 읽는 중, 3 : 완독
  @Transactional
  public boolean addToReading(Long memberId, Long bookId) {
    int state = readBookDao.isReadBookDuplicate(memberId, bookId);
    switch (state) {
      case 1:
        return readBookDao.addToReading(memberId, bookId);
      case 2:
        return readBookDao.deleteReading(memberId, bookId);
      case 3:
        throw new IllegalStateException("완독한 책은 삭제할 수 없습니다.");
      default:
        throw new IllegalStateException("알 수 없는 상태입니다.");
    }
  }

  // 내 서재 - 읽는 중 전체 조회
  public List<ReadBookRes> readingList(Long memberId) {
    List<ReadBookRes> readBookRes = readBookDao.readingList(memberId);
    return (readBookRes != null) ? readBookRes : Collections.emptyList();
  }

  // 내 서재 - 읽는 중 → 완독, 완독 상세 수정
  @Transactional
  public boolean changeToReaded(Long memberId, ReadBookReq readBookReq) {
    if (readBookDao.isReadBookDuplicate(memberId, readBookReq.getBookId()) == 1) {
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
    if (readBookDao.isReadBookDuplicate(memberId, bookId) == 1) {
      throw new IllegalStateException("존재하지 않는 정보입니다.");
    }
    ReadBookRes readBookRes = readBookDao.readedInfo(memberId, bookId);
    return readBookRes;
  }

  // 독서 분석 리포트 - 통계 계산용 리스트
  public List<ReadBookRes> getRawData(Long memberId) {
    List<ReadBookRes> readBookRes = readBookDao.getRawData(memberId);
    return (readBookRes != null) ? readBookRes : Collections.emptyList();
  }
}
