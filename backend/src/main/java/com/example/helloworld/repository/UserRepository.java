package com.example.helloworld.repository;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> findAllUserNames() {
        return jdbcTemplate.query("SELECT * FROM users", (rs, rowNum) -> resolveUserName(rs)).stream()
                .filter(StringUtils::hasText)
                .toList();
    }

    private String resolveUserName(ResultSet rs) throws SQLException {
        String username = readIfPresent(rs, "username");
        if (StringUtils.hasText(username)) {
            return username;
        }

        String name = readIfPresent(rs, "name");
        if (StringUtils.hasText(name)) {
            return name;
        }

        ResultSetMetaData metaData = rs.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            if (isStringColumn(metaData.getColumnType(i))) {
                String value = rs.getString(i);
                if (StringUtils.hasText(value)) {
                    return value;
                }
            }
        }

        return null;
    }

    private String readIfPresent(ResultSet rs, String columnName) {
        try {
            return rs.getString(columnName);
        } catch (SQLException ignored) {
            return null;
        }
    }

    private boolean isStringColumn(int sqlType) {
        return sqlType == Types.VARCHAR
                || sqlType == Types.CHAR
                || sqlType == Types.LONGVARCHAR
                || sqlType == Types.NVARCHAR
                || sqlType == Types.NCHAR
                || sqlType == Types.LONGNVARCHAR;
    }
}
