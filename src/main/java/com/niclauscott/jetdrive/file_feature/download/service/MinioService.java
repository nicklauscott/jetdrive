package com.niclauscott.jetdrive.file_feature.download.service;

import com.niclauscott.jetdrive.file_feature.common.constant.FileStorageProperties;
import com.niclauscott.jetdrive.file_feature.common.exception.FileNotFoundException;
import com.niclauscott.jetdrive.file_feature.file.model.dtos.FileUrlResponseDTO;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

@Service
@AllArgsConstructor
@Slf4j
public class MinioService {

    private final MinioClient minioClient;
    private final FileStorageProperties fileStorageProperties;

    public InputStream getFileStream(String objetId)  {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(fileStorageProperties.uploadBucket())
                            .object(objetId)
                            .build()
            );
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            log.info("Error getting object from minio: {}", e.getMessage());
            throw new FileNotFoundException(e.getMessage());
        }
    }

    public InputStream getFileStreamWithOffset(String objetId, long start, long length)  {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(fileStorageProperties.uploadBucket())
                            .object(objetId)
                            .offset(start)
                            .length(length)
                            .build()
            );
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            log.info("Error getting object with offset from minio: {}", e.getMessage());
            throw new FileNotFoundException(e.getMessage());
        }
    }

    public Long getFileLength(String objetId)  {
        try {
            return minioClient.statObject(StatObjectArgs.builder()
                    .bucket(fileStorageProperties.uploadBucket())
                    .object(objetId)
                    .build()).size();

        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            log.info("Error getting object length from minio: {}", e.getMessage());
            throw new FileNotFoundException(e.getMessage());
        }
    }

    public void deleteFile(String objetId) {
        try {
            minioClient.deleteObjectTags(
                    DeleteObjectTagsArgs.builder()
                            .bucket(fileStorageProperties.uploadBucket())
                            .object(objetId)
                            .build()
            );
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            log.info("Error deleting file from minio: {}", e.getMessage());
            throw new FileNotFoundException(e.getMessage());
        }

    }

    public void copyObject(String sourceKey, String destinationKey) {
        try {
            CopySource source = CopySource.builder()
                    .bucket(fileStorageProperties.uploadBucket())
                    .object(sourceKey)
                    .build();

            CopyObjectArgs args = CopyObjectArgs.builder()
                    .bucket(fileStorageProperties.uploadBucket())
                    .object(destinationKey)
                    .source(source)
                    .build();

            minioClient.copyObject(args);
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            log.info("Error copying object from minio: {}", e.getMessage());
            throw new FileNotFoundException(e.getMessage());
        }
    }

    public void uploadProfilePicture(
            InputStream stream, long objectSize, long partSize, String objectName
    ) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(fileStorageProperties.profileBucket())
                            .object(objectName)
                            .stream(stream, objectSize, partSize)
                            .build()
            );
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            log.info("Error saving object to minio: {}", e.getMessage());
            throw new FileNotFoundException(e.getMessage());
        }
    }

    public void deleteProfilePicture(String objetId) {
        try {
            minioClient.deleteObjectTags(
                    DeleteObjectTagsArgs.builder()
                            .bucket(fileStorageProperties.profileBucket())
                            .object(objetId)
                            .build()
            );
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            log.info("Error deleting profile picture from minio: {}", e.getMessage());
            throw new FileNotFoundException(e.getMessage());
        }

    }

    public FileUrlResponseDTO getPresignedUrl(String objectName) {
        try {
            LocalDateTime now = LocalDateTime.now();
            String url =  minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(fileStorageProperties.uploadBucket())
                            .object(objectName)
                            .expiry(5, TimeUnit.MINUTES)
                            .build()
            );
            return new FileUrlResponseDTO(url.replace("localhost", getHostAddress()), now.plusMinutes(5));
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            throw new FileNotFoundException(e.getMessage());
        }
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
            log.info("------------------------- Error: {}", e.getMessage());
            return  "";
        }
        return "";
    }

}
