package com.ecommerce.controller;

import com.ecommerce.service.FileStorageService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/uploads")
public class UploadResourceController {

    private final FileStorageService fileStorageService;

    public UploadResourceController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<byte[]> serveUploadedOrSampleImage(@PathVariable("filename") String filename) {
        if (filename.startsWith("sample-") || filename.equals("fallback-product.svg")) {
            byte[] svgBytes = generateSampleSvgIllustration(filename).getBytes(StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("image/svg+xml"))
                    .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                    .body(svgBytes);
        }

        try {
            Path filePath = fileStorageService.resolveUploadedFile(filename);
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                byte[] fallback = generateSampleSvgIllustration("fallback-product.svg").getBytes(StandardCharsets.UTF_8);
                return ResponseEntity.ok()
                        .contentType(MediaType.valueOf("image/svg+xml"))
                        .body(fallback);
            }
            byte[] bytes = Files.readAllBytes(filePath);
            MediaType mediaType = determineMediaType(filename);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filePath.getFileName().toString() + "\"")
                    .contentType(mediaType)
                    .body(bytes);
        } catch (IOException | RuntimeException e) {
            byte[] fallback = generateSampleSvgIllustration("fallback-product.svg").getBytes(StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("image/svg+xml"))
                    .body(fallback);
        }
    }

    private MediaType determineMediaType(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".webp")) return MediaType.valueOf("image/webp");
        if (lower.endsWith(".svg")) return MediaType.valueOf("image/svg+xml");
        return MediaType.IMAGE_JPEG;
    }

    private String generateSampleSvgIllustration(String name) {
        String title = "Product Image";
        String subtitle = "E-Commerce Catalog";
        String bgStart = "#1e293b";
        String bgEnd = "#0f172a";
        String accent = "#38bdf8";

        if (name.contains("headphones")) {
            title = "Pro ANC Wireless";
            subtitle = "Studio Audio Gear";
            bgStart = "#1e1b4b";
            bgEnd = "#312e81";
            accent = "#818cf8";
        } else if (name.contains("keyboard")) {
            title = "Mechanical RGB 75%";
            subtitle = "Tactile Gasket Mount";
            bgStart = "#0f172a";
            bgEnd = "#1e293b";
            accent = "#22d3ee";
        } else if (name.contains("monitor")) {
            title = "34\" WQHD Curved";
            subtitle = "144Hz Ultra-Wide Display";
            bgStart = "#064e3b";
            bgEnd = "#022c22";
            accent = "#34d399";
        } else if (name.contains("smarthub")) {
            title = "Matter Smart Hub";
            subtitle = "Thread & Zigbee 3.0";
            bgStart = "#1e3a8a";
            bgEnd = "#172554";
            accent = "#60a5fa";
        } else if (name.contains("dock")) {
            title = "Thunderbolt 4 Dock";
            subtitle = "12-in-1 Workstation Hub";
            bgStart = "#3b0764";
            bgEnd = "#581c87";
            accent = "#c084fc";
        } else if (name.contains("book")) {
            title = "Distributed Systems";
            subtitle = "Architecture Reference";
            bgStart = "#451a03";
            bgEnd = "#78350f";
            accent = "#fbbf24";
        } else if (name.contains("lightbar")) {
            title = "Ambient RGBIC Pair";
            subtitle = "Smart Wi-Fi Lighting";
            bgStart = "#4c0519";
            bgEnd = "#881337";
            accent = "#fb7185";
        } else if (name.contains("fpga")) {
            title = "Cyclone V FPGA Kit";
            subtitle = "Digital Hardware Lab";
            bgStart = "#14532d";
            bgEnd = "#052e16";
            accent = "#4ade80";
        }

        return """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 600 420" width="100%%" height="100%%">
                  <defs>
                    <linearGradient id="bg" x1="0%%" y1="0%%" x2="100%%" y2="100%%">
                      <stop offset="0%%" stop-color="%s"/>
                      <stop offset="100%%" stop-color="%s"/>
                    </linearGradient>
                  </defs>
                  <rect width="600" height="420" rx="18" fill="url(#bg)"/>
                  <circle cx="490" cy="90" r="110" fill="%s" opacity="0.14"/>
                  <circle cx="110" cy="340" r="130" fill="%s" opacity="0.1"/>
                  <rect x="60" y="65" width="90" height="32" rx="16" fill="%s" opacity="0.22"/>
                  <text x="105" y="86" font-family="system-ui, sans-serif" font-size="13" font-weight="700" fill="%s" text-anchor="middle">VERIFIED</text>
                  <text x="60" y="215" font-family="system-ui, sans-serif" font-size="34" font-weight="800" fill="#f8fafc">%s</text>
                  <text x="60" y="255" font-family="system-ui, sans-serif" font-size="19" font-weight="500" fill="#94a3b8">%s</text>
                  <line x1="60" y1="295" x2="240" y2="295" stroke="%s" stroke-width="4" stroke-linecap="round"/>
                </svg>
                """.formatted(bgStart, bgEnd, accent, accent, accent, accent, title, subtitle, accent);
    }
}

