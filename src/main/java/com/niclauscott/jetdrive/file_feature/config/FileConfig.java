package com.niclauscott.jetdrive.file_feature.config;

//import io.minio.MinioClient;
import com.niclauscott.jetdrive.file_feature.common.constant.FileStorageProperties;
import com.niclauscott.jetdrive.file_feature.file.service.S3StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import java.net.*;
import java.util.Enumeration;

@Configuration
@EnableScheduling
public class FileConfig {

    /*
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint("http://" + getHostAddress() + ":9000")
                //.endpoint("http://localhost:9000")
                .credentials("minioadmin", "minioadmin")
                .build();
    }

    // -------------------------------------- Remove later -----------------------------------
    private String getHostAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();

                // Ignore down or loopback interfaces
                if (!iface.isUp() || iface.isLoopback()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();

                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            return  "localhost";
        }
        return "localhost";
    }

     */


    @Bean
    public S3Client s3Client(
            @Value("${s3.endpoint}") String endpoint,
            @Value("${s3.region}") String region,
            @Value("${s3.access-key}") String accessKey,
            @Value("${s3.secret-key}") String secretKey
    ) {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .forcePathStyle(true)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(
            @Value("${s3.endpoint}") String endpoint,
            @Value("${s3.region}") String region,
            @Value("${s3.access-key}") String accessKey,
            @Value("${s3.secret-key}") String secretKey
    ) {
        return S3Presigner.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

}
