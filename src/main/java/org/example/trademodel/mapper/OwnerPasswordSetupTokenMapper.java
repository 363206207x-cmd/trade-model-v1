package org.example.trademodel.mapper;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.trademodel.entity.OwnerPasswordSetupTokenDO;

@Mapper
public interface OwnerPasswordSetupTokenMapper {
    @Insert("INSERT INTO tm_owner_password_setup_token(user_id, token_hash, expires_at, used_at, created_at) "
            + "VALUES(#{userId}, #{tokenHash}, #{expiresAt}, #{usedAt}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(OwnerPasswordSetupTokenDO token);

    @Select("SELECT * FROM tm_owner_password_setup_token WHERE token_hash = #{tokenHash} "
            + "AND used_at IS NULL AND expires_at > #{now} FOR UPDATE")
    OwnerPasswordSetupTokenDO lockUsable(@Param("tokenHash") String tokenHash,
                                         @Param("now") LocalDateTime now);

    @Update("UPDATE tm_owner_password_setup_token SET used_at = #{usedAt} "
            + "WHERE id = #{id} AND used_at IS NULL")
    int markUsed(@Param("id") Long id, @Param("usedAt") LocalDateTime usedAt);

    @Update("UPDATE tm_owner_password_setup_token SET used_at = #{usedAt} "
            + "WHERE user_id = #{userId} AND used_at IS NULL")
    int invalidateUnused(@Param("userId") Long userId, @Param("usedAt") LocalDateTime usedAt);
}
