package ypfunding.server.util;

import ypfunding.common.constant.DomainRules;
import ypfunding.server.exception.BusinessException;

import java.sql.Blob;
import java.sql.SQLException;

public final class FileUtil {

    private FileUtil() {
    }

    /**
     * 파일 경로(Windows '\' 또는 Unix '/')가 포함된 문자열에서 순수 파일명만 추출하고 길이를 검증
     *
     * @param rawPath 클라이언트가 전달한 원본 파일명 또는 경로
     * @return 경로가 제거된 순수 파일명
     * @throws BusinessException 파일명이 비어있거나 최대 길이를 초과한 경우
     */
    public static String extractFileName(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new BusinessException("이미지 파일명이 비어 있습니다.");
        }
        String normalized = rawPath.trim().replace('\\', '/');
        int lastSlashIndex = normalized.lastIndexOf('/');
        String fileName = (lastSlashIndex >= 0) ? normalized.substring(lastSlashIndex + 1) : normalized;

        if (fileName.isBlank() || fileName.equals(".") || fileName.equals("..")) {
            throw new BusinessException("올바르지 않은 이미지 파일명입니다.");
        }
        if (fileName.length() > DomainRules.MAX_IMAGE_NAME_LENGTH) {
            throw new BusinessException("이미지 파일명은 " + DomainRules.MAX_IMAGE_NAME_LENGTH + "자를 초과할 수 없습니다.");
        }
        return fileName;
    }

    /**
     * Blob 객체에서 바이트 배열을 추출하고 최대 크기를 검증
     *
     * @param blob 이미지 Blob 객체
     * @return 이미지 바이트 배열
     * @throws BusinessException 데이터가 비어있거나 최대 용량을 초과한 경우
     */
    public static byte[] extractAndValidateBytes(Blob blob) {
        if (blob == null) {
            throw new BusinessException("이미지 데이터가 비어 있습니다.");
        }
        try {
            long length = blob.length();
            if (length <= 0) {
                throw new BusinessException("이미지 데이터가 비어 있습니다.");
            }
            if (length > DomainRules.MAX_IMAGE_BYTES) {
                throw new BusinessException("이미지 크기는 최대 " + (DomainRules.MAX_IMAGE_BYTES / (1024 * 1024)) + "MB를 초과할 수 없습니다.");
            }
            return blob.getBytes(1, (int) length);
        } catch (SQLException e) {
            throw new BusinessException("이미지 데이터를 읽는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 이미지 바이트 배열의 시그니처(Magic Number)를 검사하여 실제 MIME 타입을 판별
     *
     * @param bytes 이미지 바이트 배열
     * @return 판별된 MIME 타입 문자열 (예: "image/png", "image/jpeg")
     * @throws BusinessException 지원하지 않거나 손상된 이미지 형식인 경우
     */
    public static String detectImageMimeType(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            throw new BusinessException("유효하지 않은 이미지 데이터입니다.");
        }
        if (bytes.length > DomainRules.MAX_IMAGE_BYTES) {
            throw new BusinessException("이미지 크기는 최대 " + (DomainRules.MAX_IMAGE_BYTES / (1024 * 1024)) + "MB를 초과할 수 없습니다.");
        }

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A) {
            return "image/png";
        }

        // JPEG: FF D8 FF
        if ((bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }

        // GIF: 'G' 'I' 'F' '8' ('7' or '9') 'a'
        if (bytes.length >= 6
                && bytes[0] == 'G'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == '8'
                && (bytes[4] == '7' || bytes[4] == '9')
                && bytes[5] == 'a') {
            return "image/gif";
        }

        // WEBP: 'R' 'I' 'F' 'F' ... 'W' 'E' 'B' 'P'
        if (bytes.length >= 12
                && bytes[0] == 'R'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == 'F'
                && bytes[8] == 'W'
                && bytes[9] == 'E'
                && bytes[10] == 'B'
                && bytes[11] == 'P') {
            return "image/webp";
        }

        // BMP: 'B' 'M'
        if (bytes[0] == 'B' && bytes[1] == 'M') {
            return "image/bmp";
        }

        throw new BusinessException("지원하지 않는 이미지 형식입니다. (PNG, JPEG, GIF, WEBP, BMP만 허용)");
    }
}