package com.apartment.tracker;

import com.apartment.tracker.notice.NoticeService;
import com.apartment.tracker.profile.ResidentProfile;
import com.apartment.tracker.profile.ResidentProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
    private final ResidentProfileService profileService;
    private final NoticeService noticeService;

    public PageController(ResidentProfileService profileService, NoticeService noticeService) {
        this.profileService = profileService;
        this.noticeService = noticeService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        boolean isMaintenanceHead = authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_MAINTENANCE_HEAD"));
        boolean isResident = authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_RESIDENT"));
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isMaintenanceHead", isMaintenanceHead);
        model.addAttribute("isResident", isResident);
        model.addAttribute("username", authentication.getName());
        ResidentProfile profile = profileService.getOrCreate(authentication.getName());
        model.addAttribute("displayName", profile.getFullName());
        model.addAttribute("apartmentNumber", profile.getApartmentNumber());
        model.addAttribute("latestNotices", noticeService.getLatest());
        return "dashboard";
    }
}
