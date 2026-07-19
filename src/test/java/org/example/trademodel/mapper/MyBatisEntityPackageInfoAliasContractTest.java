package org.example.trademodel.mapper;

import org.apache.ibatis.session.SqlSessionFactory;
import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.entity.PackageInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TradeModelApplication.class)
class MyBatisEntityPackageInfoAliasContractTest {

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Test
    void packageInfoAliasTargetsRetainedEntityMarker() {
        Class<?> aliasTarget = sqlSessionFactory.getConfiguration()
                .getTypeAliasRegistry()
                .resolveAlias("packageinfo");

        assertThat(aliasTarget).isEqualTo(PackageInfo.class);
    }
}
