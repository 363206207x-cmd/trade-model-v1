package org.example.trademodel.mapper;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.trademodel.entity.PersonalUserDO;

@Mapper
public interface PersonalUserMapper {

    @Select("SELECT id, username, password_hash, created_at, last_login_at "
            + "FROM tm_user WHERE username = #{username}")
    PersonalUserDO findByUsername(@Param("username") String username);

    @Select("SELECT COUNT(*) FROM tm_user")
    int countAll();

    @Insert("INSERT INTO tm_user(username, password_hash, created_at, last_login_at) "
            + "VALUES(#{username}, #{passwordHash}, #{createdAt}, #{lastLoginAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(PersonalUserDO user);

    @Update("UPDATE tm_user SET last_login_at = #{lastLoginAt} WHERE username = #{username}")
    int updateLastLoginAt(@Param("username") String username,
                          @Param("lastLoginAt") LocalDateTime lastLoginAt);
}
