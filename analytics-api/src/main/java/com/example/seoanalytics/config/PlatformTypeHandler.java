package com.example.seoanalytics.config;

import com.example.seoanalytics.entity.Platform;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

@MappedTypes(Platform.class)
public class PlatformTypeHandler extends BaseTypeHandler<Platform> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Platform parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setObject(i, parameter.getValue(), Types.OTHER);
    }

    @Override
    public Platform getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : Platform.valueOf(value);
    }

    @Override
    public Platform getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : Platform.valueOf(value);
    }

    @Override
    public Platform getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : Platform.valueOf(value);
    }
}
