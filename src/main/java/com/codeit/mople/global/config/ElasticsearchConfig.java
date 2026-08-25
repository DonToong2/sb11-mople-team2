package com.codeit.mople.global.config;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

@Configuration
public class ElasticsearchConfig extends ElasticsearchConfiguration {

  @Value("${spring.elasticsearch.uris}")
  private String elasticsearchUri;

  @Value("${spring.elasticsearch.username:}")
  private String elasticUsername;

  @Value("${spring.elasticsearch.password:}")
  private String elasticPassword;

  @Value("${ELASTICSEARCH_SSL_ENABLED:false}")
  private boolean sslEnabled;

  @Value("${ELASTICSEARCH_CA_PATH:}")
  private Resource caCertificate;

  @Override
  public ClientConfiguration clientConfiguration() {
    ClientConfiguration.MaybeSecureClientConfigurationBuilder builder =
        ClientConfiguration.builder()
            .connectedTo(removeProtocol(elasticsearchUri));

    if (sslEnabled) {
      builder
          .usingSsl(createSslContext())
          .withBasicAuth(elasticUsername, elasticPassword);
    }

    return builder.build();
  }

  private SSLContext createSslContext() {
    try {
      CertificateFactory certificateFactory =
          CertificateFactory.getInstance("X.509");

      Certificate certificate;

      try (InputStream inputStream = caCertificate.getInputStream()) {
        certificate = certificateFactory.generateCertificate(inputStream);
      }

      KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      keyStore.load(null, null);
      keyStore.setCertificateEntry("elasticsearch-ca", certificate);

      TrustManagerFactory trustManagerFactory =
          TrustManagerFactory.getInstance(
              TrustManagerFactory.getDefaultAlgorithm());

      trustManagerFactory.init(keyStore);

      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(
          null,
          trustManagerFactory.getTrustManagers(),
          null
      );

      return sslContext;

    } catch (Exception e) {
      throw new IllegalStateException(
          "Elasticsearch CA 인증서 설정에 실패했습니다.", e);
    }
  }

  private String removeProtocol(String uri) {
    return uri
        .replaceFirst("^https://", "")
        .replaceFirst("^http://", "");
  }
}