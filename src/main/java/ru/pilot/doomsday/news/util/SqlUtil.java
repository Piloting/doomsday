package ru.pilot.doomsday.news.util;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;
import javax.sql.DataSource;

/**
 * Простые запросы к БД черед JDBC
 */
public class SqlUtil {

    public static int simpleUpdate(DataSource ds, String sql, List<Object> binds) {
        try (Connection con = ds.getConnection(); PreparedStatement stmt = con.prepareStatement(sql)) {
            for (int counter = 0; counter < binds.size(); counter++) {
                stmt.setObject(counter+1, binds.get(counter));
            }
            return stmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static LocalDate simpleSelectGetDate(DataSource ds, String sql, List<Object> binds) {
        try (Connection con = ds.getConnection(); PreparedStatement stmt = con.prepareStatement(sql)) {
            if (binds != null) {
                for (int counter = 0; counter < binds.size(); counter++) {
                    stmt.setObject(counter+1, binds.get(counter));
                }
            }
            ResultSet resultSet = stmt.executeQuery();
            resultSet.next();
            Date date = resultSet.getDate(1);
            return date.toLocalDate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
