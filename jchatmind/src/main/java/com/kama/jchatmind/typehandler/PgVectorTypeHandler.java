package com.kama.jchatmind.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;

/**
 * PostgreSQL pgvector 类型处理器。
 * <p>
 * 负责在 MyBatis 与 PostgreSQL `vector` 字段之间完成 `float[]` 的双向转换。
 */
@MappedJdbcTypes(JdbcType.OTHER)
@MappedTypes(float[].class)
public class PgVectorTypeHandler extends BaseTypeHandler<float[]> {

    @Override
    /**
     * 将 Java 浮点数组写入 PreparedStatement。
     *
     * @param ps PreparedStatement
     * @param i 参数下标
     * @param parameter 向量参数
     * @param jdbcType JDBC 类型
     * @throws SQLException SQL 异常
     */
    public void setNonNullParameter(PreparedStatement ps, int i, float[] parameter, JdbcType jdbcType) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int j = 0; j < parameter.length; j++) {
            sb.append(parameter[j]);
            if (j < parameter.length - 1) sb.append(',');
        }
        sb.append(']');
        ps.setObject(i, sb.toString(), Types.OTHER);
    }

    @Override
    /**
     * 从结果集按列名读取向量。
     *
     * @param rs 结果集
     * @param columnName 列名
     * @return 浮点数组
     * @throws SQLException SQL 异常
     */
    public float[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    /**
     * 从结果集按列下标读取向量。
     *
     * @param rs 结果集
     * @param columnIndex 列下标
     * @return 浮点数组
     * @throws SQLException SQL 异常
     */
    public float[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    /**
     * 从存储过程调用结果中读取向量。
     *
     * @param cs CallableStatement
     * @param columnIndex 列下标
     * @return 浮点数组
     * @throws SQLException SQL 异常
     */
    public float[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    /**
     * 将 PostgreSQL vector 文本解析为浮点数组。
     *
     * @param vectorText PostgreSQL vector 文本
     * @return 浮点数组
     */
    private float[] parse(String vectorText) {
        if (vectorText == null) return null;
        // 去掉 "[ ]"
        vectorText = vectorText.replace("[", "").replace("]", "");
        if (vectorText.isBlank()) return new float[0];
        String[] parts = vectorText.split(",");
        float[] arr = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            arr[i] = Float.parseFloat(parts[i]);
        }
        return arr;
    }
}
