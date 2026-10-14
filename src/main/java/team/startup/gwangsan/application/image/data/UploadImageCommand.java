package team.startup.gwangsan.application.image.data;

public record UploadImageCommand(String fileName, byte[] content) {
}
