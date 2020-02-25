package com.example.cassandratlspoc.config;

import com.datastax.driver.core.*;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.CassandraClusterFactoryBean;
import org.springframework.data.cassandra.config.CassandraCqlClusterFactoryBean;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

@Configuration
public class CassandraConfiguration extends AbstractCassandraConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraConfiguration.class);

    @Value("${spring.data.cassandra.trust_store}")
    private String trustStoreName;

    @Value("${spring.data.cassandra.trust_store_pass}")
    private String trustStorePass;

    @Value("${spring.data.cassandra.contact-points:placeholder}")
    private String contactPoints;

    @Value("${spring.data.cassandra.port:0000}")
    private int port;

    @Value("${spring.data.cassandra.keyspace:placeholder}")
    private String keySpace;

    @Value("${spring.data.cassandra.username}")
    private String username;

    @Value("${spring.data.cassandra.password}")
    private String password;

    @Value("${spring.data.cassandra.schema-action}")
    private String schemaAction;

    @Override
    protected String getKeyspaceName() {
        return keySpace;
    }

    @Override
    protected String getContactPoints() {
        return contactPoints;
    }

    @Override
    protected int getPort() {
        return port;
    }

    @Override
    protected AuthProvider getAuthProvider() {
        return new PlainTextAuthProvider(username, password);
    }

    @Override
    @Bean
    public CassandraClusterFactoryBean cluster() {
        LOGGER.info(">>Connecting to cassandra at:{}", contactPoints);
        final CassandraCqlClusterFactoryBean cluster = new CassandraCqlClusterFactoryBean();
        cluster.setSslEnabled(true);
        cluster.setSslOptions(createSslOptions());
        cluster.setContactPoints(getContactPoints());
        cluster.setPort(getPort());
        cluster.setMetricsEnabled(false);
        cluster.setAuthProvider(getAuthProvider());
        return cluster;
    }

    private SSLOptions createSslOptions() {
        return RemoteEndpointAwareJdkSSLOptions.builder()
                .withSSLContext(createSslContextWithTruststore())
                .build();
    }

    /**
     * this will require additional dependencies - Not implemented currently
     *
     * @return
     */
    private SSLOptions nettySslOptions() {
        return new RemoteEndpointAwareNettySSLOptions(nettyContext());
    }

    private SslContext nettyContext() {
        try {
            return SslContextBuilder.forClient().sslProvider(SslProvider.OPENSSL).trustManager(trustStoreFile()).build();
        } catch (SSLException e) {
            throw new RuntimeException("Err: Could not build netty ssl context", e);
        }
    }

    private File trustStoreFile() {
        URL trustStoreURL = getClass().getClassLoader().getResource(trustStoreName);
        if (trustStoreURL == null) {
            throw new RuntimeException("TrustStore name was null");
        }
        return new File(trustStoreURL.getFile());
    }

    private SSLContext createSslContextWithTruststore() {
        try {
            return new SSLContextBuilder()
                    .loadTrustMaterial(trustStoreFile(), trustStorePass.toCharArray())
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException | CertificateException | IOException e) {
            LOGGER.error(">>Error creating context", e);
            throw new RuntimeException("SSL Conn failed", e);
        }
    }
}
