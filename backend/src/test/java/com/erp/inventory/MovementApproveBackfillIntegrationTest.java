package com.erp.inventory;

import com.erp.common.AbstractIntegrationTest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AC-14: V0006 백필 마이그레이션이 inventory:write 보유 역할에 inventory:movement:approve를 멱등하게
 * 부여하는지 실제 마이그레이션 SQL을 시드 데이터에 적용해 검증한다(중복 INSERT 없음).
 */
@Transactional
class MovementApproveBackfillIntegrationTest extends AbstractIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;

    private String migrationSql() throws Exception {
        return StreamUtils.copyToString(
                new ClassPathResource("db/migration/V0006__inventory_movement_approve_backfill.sql").getInputStream(),
                StandardCharsets.UTF_8);
    }

    @Test
    void backfill_grantsMovementApproveToInventoryWriteRoles_idempotently() throws Exception {
        Long roleId = jdbcTemplate.queryForObject(
                "INSERT INTO common.role (tenant_id, code, name) VALUES (?, ?, ?) RETURNING id",
                Long.class, TEST_TENANT_ID, "INV_WRITER_BF", "재고작성자");
        jdbcTemplate.update(
                "INSERT INTO common.role_permission (tenant_id, role_id, permission_code) VALUES (?, ?, ?)",
                TEST_TENANT_ID, roleId, "inventory:write");

        String sql = migrationSql();
        jdbcTemplate.execute(sql);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM common.role_permission WHERE role_id = ? AND permission_code = 'inventory:movement:approve'",
                Integer.class, roleId);
        assertThat(count).isEqualTo(1);

        // 멱등: 재실행해도 중복 INSERT 없음
        jdbcTemplate.execute(sql);
        count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM common.role_permission WHERE role_id = ? AND permission_code = 'inventory:movement:approve'",
                Integer.class, roleId);
        assertThat(count).isEqualTo(1);
    }
}
