package com.whiletrue.sample;

import org.junit.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ManualTest {
    @Test
    @Disabled
    public void testinsert() throws SQLException {
        long start = System.currentTimeMillis();
        DataSource dataSource = new SingleConnectionDataSource("jdbc:postgresql://localhost:5432/cluster_tasks", "postgres", "", false);
        Connection cn = dataSource.getConnection();
        PreparedStatement ps = cn.prepareStatement("INSERT INTO public.testinsert(id, idx) values(?, ?)");
        cn.setAutoCommit(true);
        for (int i = 0; i<100000; i++) {
            ps.setInt(1, i+ 10000000);
            ps.setInt(2, i);
            ps.execute();
        }

        long end = System.currentTimeMillis();
        System.out.println("Time taken for connection + insert: " + (end - start) + "ms.");
    }
}
