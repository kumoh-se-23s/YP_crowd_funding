package ypfunding.server.util;

import ypfunding.common.dto.ImageDTO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
public final class FileUtil {

    private FileUtil() {
    }

    /**
     * 파일 경로 문자열에서 디렉터리 경로('\' 또는 '/')를 제거하고 순수 파일명만 추출
     * 입력이 null이거나 공백이면 빈 문자열("")을 반환
     *
     * @param rawPath 원본 파일 경로 또는 파일명
     * @return 경로가 제거된 순수 파일명
     */
    public static String extractFileName(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return "";
        }
        String normalized = rawPath.trim().replace('\\', '/');
        int lastSlashIndex = normalized.lastIndexOf('/');
        return (lastSlashIndex >= 0) ? normalized.substring(lastSlashIndex + 1) : normalized;
    }

    /**
     * 바이트 배열의 앞부분 시그니처(Magic Number)를 검사하여 실제 이미지 MIME 타입을 반환
     *
     * @param bytes 이미지 바이트 배열
     * @return 판별된 MIME 타입 문자열 ("image/png", "image/jpeg" 등), 이미지가 아니면 null
     */
    public static String detectImageMimeType(byte[] bytes) {
        if (bytes == null || bytes.length < 2) {
            return null;
        }
        if (isPng(bytes)) return "image/png";
        if (isJpeg(bytes)) return "image/jpeg";
        if (isGif(bytes)) return "image/gif";
        if (isWebp(bytes)) return "image/webp";
        if (isBmp(bytes)) return "image/bmp";

        return null;
    }

    // =================================================================
    // 이미지 포맷별 Magic Number 판별 순수 헬퍼
    // =================================================================

    /** PNG 시그니처: 89 50 4E 47 0D 0A 1A 0A */
    private static boolean isPng(byte[] bytes) {
        return bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50 // 'P'
                && bytes[2] == 0x4E // 'N'
                && bytes[3] == 0x47 // 'G'
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A;
    }

    /** JPEG 시그니처: FF D8 FF */
    private static boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
    }

    /** GIF 시그니처: "GIF87a" 또는 "GIF89a" */
    private static boolean isGif(byte[] bytes) {
        return bytes.length >= 6
                && bytes[0] == 'G'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == '8'
                && (bytes[4] == '7' || bytes[4] == '9')
                && bytes[5] == 'a';
    }

    /** WEBP 시그니처: 0~3바이트 "RIFF" + 8~11바이트 "WEBP" */
    private static boolean isWebp(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == 'R'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == 'F'
                && bytes[8] == 'W'
                && bytes[9] == 'E'
                && bytes[10] == 'B'
                && bytes[11] == 'P';
    }

    /** BMP 시그니처: "BM" */
    private static boolean isBmp(byte[] bytes) {
        return bytes.length >= 2
                && bytes[0] == 'B'
                && bytes[1] == 'M';
    }

    /**
     * 두 디렉토리 경로가 서로 다른 로컬 디렉토리인지 확인한다. (동일 경로면 false)
     */
    public static boolean isDifferentDirectory(Path sourceDir, Path targetDir) {
        if (sourceDir == null || targetDir == null) {
            return false;
        }
        return !sourceDir.toAbsolutePath().normalize().equals(targetDir.toAbsolutePath().normalize());
    }

    /**
     * 원본 로컬 파일 경로에서 이미지를 읽어 업로드용 ImageDTO로 생성한다.
     */
    public static ImageDTO readImageFromFile(Path sourceFilePath) throws IOException {
        byte[] bytes = Files.readAllBytes(sourceFilePath);
        ImageDTO dto = new ImageDTO();
        dto.setName(extractFileName(sourceFilePath.toString()));
        dto.setType(detectImageMimeType(bytes));
        dto.setImageData(bytes);
        return dto;
    }

    /**
     * 다운로드받은 ImageDTO를 지정된 로컬 디렉토리(targetDir)에 파일로 저장하고 저장된 파일 경로를 반환한다.
     */
    public static Path saveImageToDirectory(ImageDTO imageDTO, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        String safeFileName = extractFileName(imageDTO.getName());
        Path savedFilePath = targetDir.resolve(safeFileName);
        return Files.write(savedFilePath, imageDTO.getImageData());
    }

}