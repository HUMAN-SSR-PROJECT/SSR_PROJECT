package com.ssrpro.library.service;

import com.ssrpro.library.dao.LibraryDao;
import com.ssrpro.library.dto.request.BookDetailReq;
import com.ssrpro.library.dto.response.LibraryRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LibraryService {
    private final LibraryDao libraryDao;

    // 도서관 찾기
    public List<LibraryRes> findByLibraryCode(BookDetailReq req){
        try{
            // 1. 검증
            if (req.getLibraryCodes() == null || req.getLibraryCodes().isEmpty()) {
                return List.of();
            }

            // 2. 조회 및 변환
            var rawList = libraryDao.findByLibraryCode(req.getLibraryCodes());

            // 결과가 null일 경우를 대비하여 처리 후 stream 진행
            if (rawList == null) {
                return List.of();
            }

            return rawList.stream()
                    .map(LibraryRes::of)
                    .toList();
        }catch(Exception e){
            throw new RuntimeException("예상치 못한 에러 발생: " + e.getMessage());
        }
    }
    // 도서관 입력
    public boolean insertLibrary(){
        try{
            // 외부 API 스케쥴러 사용 toEntity() 로 변환후 Dao에 넘기기
            // 결과를 libraryList 로 명명
            if (libraryList == null || libraryList.isEmpty()) {
                return false;
            }

            boolean isSuccess = libraryDao.insertLibrary(libraryList);

            if (!isSuccess) {
                throw new RuntimeException("저장중 에러가 발생했습니다.");
            }
            return true;
        }catch(Exception e){
            throw new RuntimeException("예상치 못한 에러: " + e.getMessage());
        }
    }
}
