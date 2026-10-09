package ypfunding.server.bootstrap;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.Properties;

public class MyBatisConfig {
    public static SqlSessionFactory create(DataSource dataSource) {
        Objects.requireNonNull(dataSource, "dataSource");

        TransactionFactory transactionFactory = new ManagedTransactionFactory();

        Properties txProps = new Properties();
        txProps.setProperty("closeConnection", "false");
        transactionFactory.setProperties(txProps);

        Environment environment = new Environment("default", transactionFactory, dataSource);
        Configuration config = new Configuration(environment);

        config.setMapUnderscoreToCamelCase(true);

        //TODO: 쿼리매퍼 만들고 주석 풀 것
        //config.addMapper(ProjectQueryMapper.class);

        return new SqlSessionFactoryBuilder().build(config);
    }
}
