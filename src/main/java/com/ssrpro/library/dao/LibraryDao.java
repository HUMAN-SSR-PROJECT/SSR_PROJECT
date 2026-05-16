package com.ssrpro.library.dao;

import com.ssrpro.library.dto.entity.Library;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class LibraryDao {
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<Library> findByLibraryCode(List<String> libraryCodes) {
        String sql = "SELECT * FROM LIBRARY l WHERE l.LIBRARY_CODE IN (:codes) ";

        MapSqlParameterSource params = new MapSqlParameterSource("codes", libraryCodes);

        return namedParameterJdbcTemplate.query(sql, params, new BeanPropertyRowMapper<>(Library.class));
    }

    public boolean existsByLibraryCode(Long libraryCode) {
        String sql = "SELECT COUNT(*) FROM LIBRARY WHERE LIBRARY_CODE = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, libraryCode);
        return count != null && count > 0;
    }

    public boolean insertLibrary(Library library) {
        String sql = "INSERT INTO LIBRARY "
                + "(LIBRARY_ID, LIBRARY_CODE, LIBRARY_NAME, LIBRARY_ADDR, LIBRARY_LAT, LIBRARY_LON) "
                + "VALUES (LIBRARY_SEQ.NEXTVAL, ?, ?, ?, ?, ?) ";
        int rst = jdbcTemplate.update(sql,
                library.getLibraryCode(),
                library.getLibraryName(),
                library.getLibraryAddr(),
                library.getLibraryLat(),
                library.getLibraryLon());

        return rst > 0;
    }

    public int countAllLibrary() {
        String sql = "SELECT COUNT(*) FROM LIBRARY";
        Integer rst = jdbcTemplate.queryForObject(sql, Integer.class);
        return rst != null ? rst : 0;
    }
}
