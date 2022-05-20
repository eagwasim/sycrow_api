package com.sycrow.api.service.helpers;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretManagerServiceSettings;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import com.sycrow.api.model.PlatformAttributeEntity;
import com.sycrow.api.repository.PlatformAttributeEntityRepository;
import org.springframework.core.env.Environment;
import org.springframework.util.ResourceUtils;

import javax.inject.Named;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Named
public class PlatformAttributeHelper {
    private final PlatformAttributeEntityRepository platformAttributeEntityRepository;
    private final Environment environment;

    public PlatformAttributeHelper(PlatformAttributeEntityRepository platformAttributeEntityRepository, Environment environment) {
        this.platformAttributeEntityRepository = platformAttributeEntityRepository;
        this.environment = environment;
    }

    public void saveAttribute(String name, String value) {
        Optional<PlatformAttributeEntity> optionalLastBlockScanned = platformAttributeEntityRepository.findFirstByName(name);
        PlatformAttributeEntity platformAttributeEntity = optionalLastBlockScanned.orElseGet(() -> PlatformAttributeEntity.builder().name(name).build());
        platformAttributeEntity.setValue(value);
        platformAttributeEntityRepository.save(platformAttributeEntity);
    }

    public Optional<String> getAttributeValue(String name) {
        Optional<PlatformAttributeEntity> attributeEntity = platformAttributeEntityRepository.findFirstByName(name);
        return attributeEntity.map(PlatformAttributeEntity::getValue);
    }

    public Optional<String> getSecretValue(String name, String version) {
        SecretManagerServiceClient client = null;
        InputStream credentialsStream = null;
        try {
            credentialsStream = new FileInputStream(ResourceUtils.getFile("classpath:credentials/sycrow-api-13a13153712a.json"));
            GoogleCredentials credentials = ServiceAccountCredentials.fromStream(credentialsStream);
            SecretManagerServiceSettings secretManagerServiceSettings =
                    SecretManagerServiceSettings.newBuilder()
                            .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                            .build();
            client = SecretManagerServiceClient.create(secretManagerServiceSettings);
            SecretVersionName secretVersionName = SecretVersionName.of(environment.getProperty("spring.cloud.gcp.project-id"), name, version);
            AccessSecretVersionResponse response = client.accessSecretVersion(name);
            return Optional.of(response.getPayload().getData().toStringUtf8());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (credentialsStream != null) {
                try {
                    credentialsStream.close();
                } catch (Exception ignore) {
                }
            }
            if (client != null) {
                try {
                    client.close();
                } catch (Exception ignore) {
                }
            }
        }
        return Optional.empty();
    }
}
