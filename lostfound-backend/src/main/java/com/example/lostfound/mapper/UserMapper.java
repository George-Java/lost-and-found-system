package com.example.lostfound.mapper;

import com.example.lostfound.entity.UserAccount;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("select * from user_account where username = #{username} limit 1")
    UserAccount findByUsername(@Param("username") String username);

    @Select("select * from user_account where id = #{id} limit 1")
    UserAccount findById(@Param("id") Long id);

    @Insert("""
            insert into user_account(username, password_hash, real_name, phone, role)
            values(#{username}, #{passwordHash}, #{realName}, #{phone}, #{role})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserAccount userAccount);

    @Select("select * from user_account order by created_at desc, id desc")
    List<UserAccount> findAll();

    @Update("update user_account set role = #{role} where id = #{id}")
    int updateRole(@Param("id") Long id, @Param("role") String role);

    @Select("select count(1) from user_account")
    long countAll();
}
