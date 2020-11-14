package com.boot.mini.repository;

import com.boot.mini.annotation.type.repository.Repository;
import com.boot.mini.core.datasource.mapper.impl.BaseMapperImpl;
import com.boot.mini.entity.test.User;

/**
 * @author xhzy
 */
@Repository
public class UserRepository extends BaseMapperImpl<User> {

}
