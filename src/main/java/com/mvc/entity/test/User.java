package com.mvc.entity.test;

import com.mvc.annotation.jpa.Column;
import com.mvc.annotation.jpa.Id;
import com.mvc.annotation.jpa.Table;

/**
 * @author xhzy
 */
@Table
public class User {

    @Id
    @Column
    private Long userId;

    @Column(column = "user_name")
    private String userName;

    @Column
    private String password;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String username) {
        this.userName = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + userName + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
