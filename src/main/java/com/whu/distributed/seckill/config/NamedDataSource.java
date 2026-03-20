package com.whu.distributed.seckill.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.AbstractDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class NamedDataSource extends AbstractDataSource {

    private static final Logger log = LoggerFactory.getLogger(NamedDataSource.class);

    private final String role;
    private final DataSource delegate;

    public NamedDataSource(String role, DataSource delegate) {
        this.role = role;
        this.delegate = delegate;
    }

    @Override
    public Connection getConnection() throws SQLException {
        log.debug("Acquire {} datasource connection", role);
        return delegate.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        log.debug("Acquire {} datasource connection", role);
        return delegate.getConnection(username, password);
    }
}
