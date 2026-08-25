package org.example.trademodel.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.trademodel.entity.PersonalUserDO;

@Mapper
public interface PersonalUserMapper {

    @Select("SELECT id, username, password_hash, role, enabled, session_version, created_at, updated_at, last_login_at, disabled_at "
            + "FROM tm_user WHERE username = #{username}")
    PersonalUserDO findByUsername(@Param("username") String username);

    @Select("SELECT id, username, password_hash, role, enabled, session_version, created_at, updated_at, last_login_at, disabled_at "
            + "FROM tm_user WHERE id = #{id}")
    PersonalUserDO findById(@Param("id") Long id);

    @Select("SELECT id, username, password_hash, role, enabled, session_version, created_at, updated_at, last_login_at, disabled_at "
            + "FROM tm_user ORDER BY CASE WHEN role = 'OWNER' THEN 0 ELSE 1 END, created_at, id")
    List<PersonalUserDO> listAll();

    @Select("SELECT COUNT(*) FROM tm_user")
    int countAll();

    @Select("SELECT COUNT(*) FROM tm_user WHERE enabled = TRUE")
    int countEnabled();

    @Select("SELECT COUNT(*) FROM tm_user WHERE id = 1 AND LOWER(username) = 'xuchao' "
            + "AND role = 'OWNER' AND enabled = TRUE AND owner_slot = 1")
    int countCanonicalOwner();

    @Select("SELECT max_active_accounts FROM tm_user_registration_guard WHERE id = 1 FOR UPDATE")
    int lockRegistrationGuard();

    @Select("SELECT max_active_accounts FROM tm_user_registration_guard WHERE id = 1")
    int selectRegistrationLimit();

    @Insert({"<script>",
            "INSERT INTO tm_user(username, password_hash, role, enabled, session_version, created_at, updated_at, last_login_at, disabled_at, owner_slot)",
            "VALUES(#{username}, #{passwordHash},",
            "<choose><when test='role != null'>#{role}</when><otherwise>'USER'</otherwise></choose>,",
            "<choose><when test='enabled != null'>#{enabled}</when><otherwise>TRUE</otherwise></choose>,",
            "<choose><when test='sessionVersion != null'>#{sessionVersion}</when><otherwise>0</otherwise></choose>,",
            "#{createdAt},",
            "<choose><when test='updatedAt != null'>#{updatedAt}</when><otherwise>#{createdAt}</otherwise></choose>,",
            "#{lastLoginAt}, #{disabledAt},",
            "<choose><when test=\"role == 'OWNER'\">1</when><otherwise>NULL</otherwise></choose>)",
            "</script>"})
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(PersonalUserDO user);

    @Update("UPDATE tm_user SET last_login_at = #{lastLoginAt} WHERE username = #{username}")
    int updateLastLoginAt(@Param("username") String username,
                          @Param("lastLoginAt") LocalDateTime lastLoginAt);

    @Update("UPDATE tm_user SET enabled = FALSE, disabled_at = #{updatedAt}, updated_at = #{updatedAt}, "
            + "session_version = session_version + 1 WHERE id = #{id} AND role = 'USER' AND enabled = TRUE")
    int disableUser(@Param("id") Long id, @Param("updatedAt") LocalDateTime updatedAt);

    @Update("UPDATE tm_user SET enabled = TRUE, disabled_at = NULL, updated_at = #{updatedAt}, "
            + "session_version = session_version + 1 WHERE id = #{id} AND role = 'USER' AND enabled = FALSE")
    int enableUser(@Param("id") Long id, @Param("updatedAt") LocalDateTime updatedAt);

    @Update("UPDATE tm_user SET session_version = session_version + 1, updated_at = #{updatedAt} "
            + "WHERE id = #{id} AND role = 'USER'")
    int forceLogout(@Param("id") Long id, @Param("updatedAt") LocalDateTime updatedAt);

    @Update("UPDATE tm_user SET password_hash = #{passwordHash}, session_version = session_version + 1, "
            + "updated_at = #{updatedAt} WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash,
                       @Param("updatedAt") LocalDateTime updatedAt);
}
