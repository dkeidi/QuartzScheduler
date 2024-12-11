package com.quartz.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    private static final Logger LOG = LogManager.getLogger(JobUtils.class);

    private final DataSource dataSource;

    @Autowired
    public JobUtils(@QuartzDataSource DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public char getJobDeletionStatus(String jobName, String jobGroup) {
        String query = "SELECT ISDELETED FROM QRTZ_JOB_DETAILS WHERE JOB_NAME = ? AND JOB_GROUP = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, jobName);
            preparedStatement.setString(2, jobGroup);

            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("ISDELETED").charAt(0); // Assuming it is 'Y' or 'N'
                } else {
                    LOG.error("Job not found in the database.");
                    throw new SQLException("Job not found in the database.");
                }
            }
        } catch (SQLException e) {
            LOG.error(e);
        }

        return 0;
    }

    public boolean softDeleteJob(String jobName, String jobGroup) {
        String query = "UPDATE QRTZ_JOB_DETAILS SET ISDELETED='Y' WHERE JOB_NAME = ? AND JOB_GROUP = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, jobName);
            preparedStatement.setString(2, jobGroup);

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected == 0) {
                LOG.error("Job not found or already soft deleted in the database.");
                return false;
            } else {
                LOG.info("Job successfully soft deleted: {} in group {}", jobName, jobGroup);
                return true;
            }
        } catch (SQLException e) {
            LOG.error("Error soft deleting job: {} in group {}", jobName, jobGroup, e);
        }
        return false;
    }
}

