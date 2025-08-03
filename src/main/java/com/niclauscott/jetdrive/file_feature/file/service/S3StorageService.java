package com.niclauscott.jetdrive.file_feature.file.service;

import com.niclauscott.jetdrive.file_feature.common.constant.FileStorageProperties;
import com.niclauscott.jetdrive.file_feature.common.exception.FileNotFoundException;
import com.niclauscott.jetdrive.file_feature.file.model.dtos.FileUrlResponseDTO;
import com.niclauscott.jetdrive.file_feature.upload.model.entities.UploadSession;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class S3StorageService {

    private FileStorageProperties fileStorageProperties;
    private FileEncryptionService encryptionService;
    private final S3Client s3;
    private final S3Presigner s3Presigner;

    public InputStream getFileStream(String objectId) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(fileStorageProperties.uploadBucket())
                    .key(objectId)
                    .build();
            return s3.getObject(request);
        } catch (S3Exception e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    public void uploadChunkToS3(
            String objectKey, int partNumber, InputStream inputStream, long start, long end
    ) throws IOException {
        try {
            s3.putObject(PutObjectRequest.builder()
                            .bucket(fileStorageProperties.uploadBucket())
                            .key(objectKey + ".part" + partNumber)
                            .build(),
                    RequestBody.fromInputStream(inputStream, end - start + 1));
        } catch (SdkException e) {
            throw new IOException("Failed to upload chunk to S3", e);
        }
    }

    public void mergeChunksAndUpload(
            String finalObject, UploadSession session, UUID fileId
    ) {
        try (ByteArrayOutputStream combined = new ByteArrayOutputStream()) {
            for (int i : session.getUploadedParts().keySet().stream().sorted().toList()) {
                GetObjectRequest getRequest = GetObjectRequest.builder()
                        .bucket(fileStorageProperties.uploadBucket())
                        .key(finalObject + ".part" + i)
                        .build();

                try (InputStream in = s3.getObject(getRequest)) {
                    in.transferTo(combined);
                }
            }

            //byte[] encryptedData = encryptionService.encrypt(combined.toByteArray(), session.getUserId().toString(), fileId.toString());
            s3.putObject(PutObjectRequest.builder()
                            .bucket(fileStorageProperties.uploadBucket())
                            .key(finalObject)
                            .build(),
                    RequestBody.fromBytes(combined.toByteArray()));
                    //RequestBody.fromBytes(encryptedData));

            for (int i : session.getUploadedParts().keySet()) {
                try {
                    s3.deleteObject(DeleteObjectRequest.builder()
                            .bucket(fileStorageProperties.uploadBucket())
                            .key(finalObject + ".part" + i)
                            .build());
                } catch (Exception e) {
                    log.warn("Failed to delete chunk part {}: {}", i, e.getMessage());
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to assemble and complete upload", e);
        }
    }

    public void removeIncompleteUploads(UploadSession session) {
        String finalObject = session.getObjectKey();
        for (int i : session.getUploadedParts().keySet()) {
            try {
                s3.deleteObject(DeleteObjectRequest.builder()
                        .bucket(fileStorageProperties.uploadBucket())
                        .key(finalObject + ".part" + i)
                        .build());
            } catch (Exception e) {
                log.warn("Failed to delete chunk part {}: {}.", i, e.getMessage());
            }
        }
    }

    public InputStream getFileStreamWithOffset(String objectId, long start, long length) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(fileStorageProperties.uploadBucket())
                    .key(objectId)
                    .range("bytes=" + start + "-" + (start + length - 1))
                    .build();
            return s3.getObject(request);
        } catch (S3Exception e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    public Long getFileLength(String objectId) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(fileStorageProperties.uploadBucket())
                    .key(objectId)
                    .build();
            return s3.headObject(request).contentLength();
        } catch (S3Exception e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    public void deleteFile(String objectId) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(fileStorageProperties.uploadBucket())
                    .key(objectId)
                    .build();
            s3.deleteObject(request);
        } catch (S3Exception e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    public void copyObject(String sourceKey, String destinationKey) {
        try {
            CopyObjectRequest request = CopyObjectRequest.builder()
                    .sourceBucket(fileStorageProperties.uploadBucket())
                    .sourceKey(sourceKey)
                    .destinationBucket(fileStorageProperties.uploadBucket())
                    .destinationKey(destinationKey)
                    .build();
            s3.copyObject(request);
        } catch (S3Exception e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    public void uploadProfilePicture(InputStream stream, long objectSize, String objectName) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(fileStorageProperties.profileBucket())
                    .key(objectName)
                    .contentLength(objectSize)
                    .build();

            s3.putObject(request, RequestBody.fromInputStream(stream, objectSize));
        } catch (S3Exception e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    public ResponseInputStream<GetObjectResponse> serveProfilePicture(String objectName) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(fileStorageProperties.profileBucket())
                    .key(objectName)
                    .build();
            return s3.getObject(getRequest);
        } catch (Exception e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    public void deleteProfilePicture(String objectId) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(fileStorageProperties.profileBucket())
                    .key(objectId)
                    .build();
            s3.deleteObject(request);
        } catch (S3Exception e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    public FileUrlResponseDTO getPresignedUrl(String objectName) {
        try {
            // Duration the presigned URL is valid for
            Duration expiration = Duration.ofMinutes(5);

            // Build the S3 request
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(fileStorageProperties.uploadBucket())
                    .key(objectName)
                    .build();

            // Create presigned request
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .getObjectRequest(getObjectRequest)
                    .signatureDuration(Duration.ofMinutes(5))
                    .build();

            // Use S3Presigner to generate URL
            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

            return new FileUrlResponseDTO(
                    presignedRequest.url().toString(),
                    LocalDateTime.now().plus(expiration)
            );
        } catch (S3Exception e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

}
