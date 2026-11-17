package com.apartment.tracker.complaint;

import java.io.IOException;
import java.nio.file.Files;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/complaints")
public class ComplaintImageController {
    private final ComplaintService service;

    public ComplaintImageController(ComplaintService service) {
        this.service = service;
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> image(@PathVariable Long id, Authentication authentication) throws IOException {
        boolean canViewAll = authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                || authority.getAuthority().equals("ROLE_MAINTENANCE_HEAD"));
        ComplaintImageStorage.StoredImageFile image = service.getImage(id, authentication.getName(), canViewAll);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(image.contentType()))
            .cacheControl(CacheControl.noStore())
            .header("X-Content-Type-Options", "nosniff")
            .body(Files.readAllBytes(image.path()));
    }
}
