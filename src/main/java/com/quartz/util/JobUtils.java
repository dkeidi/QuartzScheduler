package com.quartz.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.quartz.QuartzDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class JobUtils {

    private final DataSource dataSource;

    @Autowired
    public JobUtils(@QuartzDataSource DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public char getJobDeletionStatus(String jobName, String jobGroup) {
        System.out.println("JobUtils.getJobDeletionStatus");
        System.out.println(dataSource);
        String query = "SELECT ISDELETED FROM QRTZ_JOB_DETAILS WHERE JOB_NAME = ? AND JOB_GROUP = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, jobName);
            preparedStatement.setString(2, jobGroup);

            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("ISDELETED").charAt(0); // Assuming it is 'Y' or 'N'
                } else {
                    throw new SQLException("Job not found in the database.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }
}

