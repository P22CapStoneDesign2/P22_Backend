package com.capstone.eqh.global.storage;

import com.capstone.eqh.global.config.SupabaseStorageProperties;
import com.capstone.eqh.global.exception.CustomException;
import com.capstone.eqh.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupabaseStorageService {

    private final SupabaseStorageProperties properties;
    private final RestClient restClient = RestClient.create();

    public void upload(String storagePath, byte[] content, String contentType) {
        String encodedPath = encodeStoragePath(storagePath);
        String uploadUrl = properties.normalizedUrl()
                + "/storage/v1/object/"
                + properties.getBucket()
                + "/"
                + encodedPath;

        try {
            restClient.post()
                    .uri(uploadUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getServiceKey())
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(content)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.warn("[SupabaseStorage] upload failed status={} path={}", e.getStatusCode(), storagePath);
            throw new CustomException(ErrorCode.STORAGE_UPLOAD_FAILED);
        } catch (Exception e) {
            log.warn("[SupabaseStorage] upload failed path={}", storagePath, e);
            throw new CustomException(ErrorCode.STORAGE_UPLOAD_FAILED);
        }
    }

    public void delete(String storagePath) {
        String encodedPath = encodeStoragePath(storagePath);
        String deleteUrl = properties.normalizedUrl()
                + "/storage/v1/object/"
                + properties.getBucket()
                + "/"
                + encodedPath;

        try {
            restClient.delete()
                    .uri(deleteUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getServiceKey())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.warn("[SupabaseStorage] delete failed status={} path={}", e.getStatusCode(), storagePath);
            throw new CustomException(ErrorCode.STORAGE_DELETE_FAILED);
        } catch (Exception e) {
            log.warn("[SupabaseStorage] delete failed path={}", storagePath, e);
            throw new CustomException(ErrorCode.STORAGE_DELETE_FAILED);
        }
    }

    public String getPublicUrl(String storagePath) {
        String encodedPath = encodeStoragePath(storagePath);
        return properties.normalizedUrl()
                + "/storage/v1/object/public/"
                + properties.getBucket()
                + "/"
                + encodedPath;
    }

    public String buildStoragePath(Long lessonId, String savedFileName) {
        return lessonId + "/" + savedFileName;
    }

    private String encodeStoragePath(String storagePath) {
        return Arrays.stream(storagePath.split("/"))
                .map(segment -> URLEncoder.encode(segment, StandardCharsets.UTF_8)
                        .replace("+", "%20"))
                .collect(Collectors.joining("/"));
    }
}
