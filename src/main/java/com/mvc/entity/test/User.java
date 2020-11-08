package com.mvc.entity.test;

import com.mvc.annotation.jpa.Column;
import com.mvc.annotation.jpa.Id;
import com.mvc.annotation.jpa.Table;

import java.util.Date;

/**
 * @author xhzy
 */
@Table(create = true)
public class User {

    @Id
    @Column(length = 20)
    private Long userId;

    @Column(column = "user_name",length = 20)
    private String userName;

    @Column(length = 20)
    private String password;

    @Column(length = 20,defaultValue = "now()",nonnull = true)
    private Date gmtCreated;

    public Date getGmtCreated() {
        return gmtCreated;
    }

    public void setGmtCreated(Date gmtCreated) {
        this.gmtCreated = gmtCreated;
    }

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
                ", userName='" + userName + '\'' +
                ", password='" + password + '\'' +
                ", gmtCreated=" + gmtCreated +
                '}';
    }
}
