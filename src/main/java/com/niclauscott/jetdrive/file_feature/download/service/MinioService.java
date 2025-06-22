package com.niclauscott.jetdrive.file_feature.download.service;

import com.niclauscott.jetdrive.file_feature.common.constant.FileStorageProperties;
import com.niclauscott.jetdrive.file_feature.common.exception.FileNotFoundException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.errors.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

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
                            .bucket(fileStorageProperties.bucket())
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
                            .bucket(fileStorageProperties.bucket())
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
                    .bucket(fileStorageProperties.bucket())
                    .object(objetId)
                    .build()).size();

        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            log.info("Error getting object length from minio: {}", e.getMessage());
            throw new FileNotFoundException(e.getMessage());
        }
    }

}
