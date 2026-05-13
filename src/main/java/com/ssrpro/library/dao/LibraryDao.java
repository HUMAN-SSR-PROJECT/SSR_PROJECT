package com.ssrpro.library.dao;

import com.ssrpro.library.dto.entity.Library;
import com.ssrpro.library.dto.response.LibraryRes;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class LibraryDao {
    private final JdbcTemplate jdbcTemplate;

    // 도서관 코드로 도서관 정보 반환
    public List<Library> findByLibraryCode(Long libraryCode){
        String sql = "SELECT * " +
                        "FROM LIBRARY l " +
                        "WHERE l.LIBRARY_CODE = ? ";

        List<Library> libraryList = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Library.class), libraryCode);

        return libraryList;
    }

    // 도서관 정보 입력
    public boolean insertLibrary(Library library){
        String sql = "INSERT INTO LIBRARY " +
                        "(LIBRARY_ID, LIBRARY_CODE, LIBRARY_NAME, LIBRARY_ADDR, LIBRARY_LAT, LIBRARY_LON) " +
                        "VALUES (LIBRARY_SEQ.NEXTVAL, ?, ?, ?, ?, ?) ";
        int rst = jdbcTemplate.update(sql,
                                        library.getLibraryCode(),
                                        library.getLibraryName(),
                                        library.getLibraryAddr(),
                                        library.getLibraryLat(),
                                        library.getLibraryLon());

        return rst > 0;
    }
}
