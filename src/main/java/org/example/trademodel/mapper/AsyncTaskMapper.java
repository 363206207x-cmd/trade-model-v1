package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.trademodel.entity.AsyncTaskDO;

import java.util.List;

@Mapper
public interface AsyncTaskMapper {
    @Insert("INSERT INTO tm_async_task(task_id, owner_type, owner_id, task_type, state, stage, resource_type, "
            + "resource_id, trace_id, retry_count, max_retries, error_code, error_message, started_at, "
            + "completed_at, created_at, updated_at) VALUES(#{taskId}, #{ownerType}, #{ownerId}, #{taskType}, "
            + "#{state}, #{stage}, #{resourceType}, #{resourceId}, #{traceId}, #{retryCount}, #{maxRetries}, "
            + "#{errorCode}, #{errorMessage}, #{startedAt}, #{completedAt}, #{createdAt}, #{updatedAt})")
    int insert(AsyncTaskDO row);

    @Insert(value = "INSERT INTO tm_async_task(task_id, owner_type, owner_id, task_type, state, stage, "
            + "resource_type, resource_id, idempotency_key, trace_id, retry_count, max_retries, error_code, "
            + "error_message, started_at, completed_at, created_at, updated_at) "
            + "SELECT #{taskId}, #{ownerType}, #{ownerId}, #{taskType}, #{state}, #{stage}, "
            + "#{resourceType}, #{resourceId}, #{idempotencyKey}, #{traceId}, #{retryCount}, #{maxRetries}, "
            + "#{errorCode}, #{errorMessage}, #{startedAt}, #{completedAt}, #{createdAt}, #{updatedAt} "
            + "WHERE NOT EXISTS (SELECT 1 FROM tm_async_task WHERE owner_type = #{ownerType} "
            + "AND owner_id = #{ownerId} AND idempotency_key = #{idempotencyKey})")
    @Insert(value = "INSERT INTO tm_async_task(task_id, owner_type, owner_id, task_type, state, stage, "
            + "resource_type, resource_id, idempotency_key, trace_id, retry_count, max_retries, error_code, "
            + "error_message, started_at, completed_at, created_at, updated_at) VALUES(#{taskId}, #{ownerType}, "
            + "#{ownerId}, #{taskType}, #{state}, #{stage}, #{resourceType}, #{resourceId}, #{idempotencyKey}, "
            + "#{traceId}, #{retryCount}, #{maxRetries}, #{errorCode}, #{errorMessage}, #{startedAt}, "
            + "#{completedAt}, #{createdAt}, #{updatedAt}) ON CONFLICT (owner_type, owner_id, idempotency_key) "
            + "DO NOTHING", databaseId = "postgresql")
    int insertIfAbsent(AsyncTaskDO row);

    @Select("SELECT * FROM tm_async_task WHERE owner_type = #{ownerType} AND owner_id = #{ownerId} "
            + "AND idempotency_key = #{idempotencyKey}")
    AsyncTaskDO selectByIdempotencyKey(@Param("ownerType") String ownerType,
                                       @Param("ownerId") Long ownerId,
                                       @Param("idempotencyKey") String idempotencyKey);

    @Update("UPDATE tm_async_task SET state = 'RUNNING', stage = #{stage}, started_at = #{startedAt}, "
            + "updated_at = #{updatedAt} WHERE task_id = #{taskId} AND state = 'QUEUED'")
    int claimQueued(@Param("taskId") String taskId,
                    @Param("stage") String stage,
                    @Param("startedAt") java.time.LocalDateTime startedAt,
                    @Param("updatedAt") java.time.LocalDateTime updatedAt);

    @Update("UPDATE tm_async_task SET result_resource_id = #{resultResourceId}, trace_id = #{traceId}, "
            + "updated_at = #{updatedAt} WHERE task_id = #{taskId} "
            + "AND owner_type = #{ownerType} AND owner_id = #{ownerId}")
    int updateResultIdentity(AsyncTaskDO row);

    @Update("UPDATE tm_async_task SET state = #{state}, stage = #{stage}, error_code = #{errorCode}, "
            + "error_message = #{errorMessage}, completed_at = #{completedAt}, updated_at = #{updatedAt} "
            + "WHERE result_resource_id = #{resultResourceId} AND state IN ('QUEUED', 'RUNNING')")
    int completeByResultResourceId(@Param("resultResourceId") String resultResourceId,
                                   @Param("state") String state,
                                   @Param("stage") String stage,
                                   @Param("errorCode") String errorCode,
                                   @Param("errorMessage") String errorMessage,
                                   @Param("completedAt") java.time.LocalDateTime completedAt,
                                   @Param("updatedAt") java.time.LocalDateTime updatedAt);

    @Select("SELECT * FROM tm_async_task WHERE task_id = #{taskId} AND owner_type = 'USER' AND owner_id = #{userId}")
    AsyncTaskDO selectForUser(@Param("taskId") String taskId, @Param("userId") Long userId);

    @Select("SELECT * FROM tm_async_task WHERE owner_type = 'USER' AND owner_id = #{userId} "
            + "ORDER BY created_at DESC, task_id DESC LIMIT #{limit}")
    List<AsyncTaskDO> listForUser(@Param("userId") Long userId, @Param("limit") int limit);

    @Update("UPDATE tm_async_task SET state = #{state}, stage = #{stage}, retry_count = #{retryCount}, "
            + "error_code = #{errorCode}, error_message = #{errorMessage}, started_at = #{startedAt}, "
            + "completed_at = #{completedAt}, result_resource_id = #{resultResourceId}, "
            + "trace_id = #{traceId}, updated_at = #{updatedAt} "
            + "WHERE task_id = #{taskId} AND owner_type = #{ownerType} AND owner_id = #{ownerId}")
    int updateState(AsyncTaskDO row);
}
