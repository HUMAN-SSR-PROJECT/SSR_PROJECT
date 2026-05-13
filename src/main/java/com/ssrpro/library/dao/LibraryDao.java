package com.ssrpro.library.dao;

import com.ssrpro.library.dto.entity.Library;
import com.ssrpro.library.dto.response.LibraryRes;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class LibraryDao {
    private final JdbcTemplate jdbcTemplate;

    // 도서관 주소로 도서관 정보 반환
    public List<Library> findByAddr(String addr){
        String sql = "SELECT * " +
                        "FROM LIBRARY l " +
                        "WHERE l.ADDR LIKE ? ";

        List<Library> libraryList = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Library.class), "%" + addr.trim() + "%");

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
