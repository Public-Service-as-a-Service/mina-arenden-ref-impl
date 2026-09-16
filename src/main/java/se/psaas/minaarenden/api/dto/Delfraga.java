package se.psaas.minaarenden.api.dto;

public record Delfraga(String producent, Part part, String status, String message, int httpCode) {
}
