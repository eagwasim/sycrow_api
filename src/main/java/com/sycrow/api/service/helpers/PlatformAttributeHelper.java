package com.sycrow.api.service.helpers;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretManagerServiceSettings;
import com.sycrow.api.model.PlatformAttributeEntity;
import com.sycrow.api.repository.PlatformAttributeEntityRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

import javax.inject.Named;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Log4j2
@Named
public class PlatformAttributeHelper {
    private final PlatformAttributeEntityRepository platformAttributeEntityRepository;
    private final ResourceLoader resourceLoader;
    private final Environment environment;

    public PlatformAttributeHelper(PlatformAttributeEntityRepository platformAttributeEntityRepository, ResourceLoader resourceLoader, Environment environment) {
        this.platformAttributeEntityRepository = platformAttributeEntityRepository;
        this.resourceLoader = resourceLoader;
        this.environment = environment;
    }

    public void saveAttribute(String name, String value) {
        Optional<PlatformAttributeEntity> optionalLastBlockScanned = platformAttributeEntityRepository.findFirstByName(name);
        PlatformAttributeEntity platformAttributeEntity = optionalLastBlockScanned.orElseGet(() -> PlatformAttributeEntity.builder().name(name).build());
        if (platformAttributeEntity.getDateCreated() == null) {
            platformAttributeEntity.setDateCreated(LocalDateTime.now());
        }
        platformAttributeEntity.setValue(value);
        platformAttributeEntity.setDateModified(LocalDateTime.now());
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
            credentialsStream = Objects.requireNonNull(resourceLoader.getClassLoader()).getResourceAsStream("credentials/sycrow-api-gcp-credentials.json");
            if (credentialsStream == null) {
                throw new FileNotFoundException("credentials/sycrow-api-gcp-credentials.json");
            }

            GoogleCredentials credentials = ServiceAccountCredentials.fromStream(credentialsStream);
            SecretManagerServiceSettings secretManagerServiceSettings =
                    SecretManagerServiceSettings.newBuilder()
                            .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                            .build();

            String secretVersionName = String.format("projects/%s/secrets/%s/versions/%s", environment.getProperty("spring.cloud.gcp.project-number"), name, version);

            client = SecretManagerServiceClient.create(secretManagerServiceSettings);
            AccessSecretVersionResponse response = client.accessSecretVersion(secretVersionName);
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
