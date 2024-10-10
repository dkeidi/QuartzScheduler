package com.quartz.repo;

import com.quartz.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserRepo {

    private JdbcTemplate template;

    public JdbcTemplate getTemplate() {
        return template;
    }

    @Autowired
    public void setTemplate(JdbcTemplate template) {
        this.template = template;
    }

    public void save(User user) {
        String sql = "insert into [dbo].[user] (id, name, gender) values (?, ?, ?)";
        int rows = template.update(sql, user.getId(), user.getName(), user.getGender());
        System.out.println(rows + " rows affected.");
    }

    public List<User> findAll() {
        String sql = "select * FROM [dbo].[user]";

        List<User> users = template.query(sql, (rs, row) -> {
            User a = new User();
            a.setId(rs.getInt(1));
            a.setName(rs.getString(2));
            a.setGender(rs.getString(3));
            return a;
        });

        return users;
    }
}
