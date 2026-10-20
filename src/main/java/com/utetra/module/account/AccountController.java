package com.utetra.module.account;

import com.utetra.entity.Address;
import com.utetra.entity.User;
import com.utetra.module.account.dto.AddressFormRequest;
import com.utetra.module.account.dto.PasswordChangeRequest;
import com.utetra.module.account.dto.ProfileUpdateRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * Controller quan ly thong tin ca nhan, doi mat khau va so dia chi nhan hang.
 */
@Controller
@RequestMapping("/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public String profile(Principal principal, Model model) {
        User user = accountService.getRequiredUser(principal.getName());

        ProfileUpdateRequest profileForm = new ProfileUpdateRequest();
        profileForm.setFullName(user.getFullName());
        profileForm.setEmail(user.getEmail());
        profileForm.setPhone(user.getPhone());
        profileForm.setAvatarUrl(user.getAvatarUrl());

        model.addAttribute("user", user);
        model.addAttribute("profileForm", profileForm);
        model.addAttribute("passwordForm", new PasswordChangeRequest());
        model.addAttribute("addresses", accountService.getAddresses(user));
        model.addAttribute("recentlyViewed", accountService.getRecentlyViewed(user));
        return "account/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute ProfileUpdateRequest profileForm,
                                @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            User user = accountService.getRequiredUser(principal.getName());
            accountService.updateProfile(user, profileForm, avatarFile);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật thông tin cá nhân thành công.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/account";
    }

    @PostMapping("/password")
    public String changePassword(@ModelAttribute PasswordChangeRequest passwordForm,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            User user = accountService.getRequiredUser(principal.getName());
            accountService.changePassword(user, passwordForm);
            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/account";
    }

    @GetMapping("/addresses")
    public String addresses(@RequestParam(required = false) Long editId,
                            Principal principal,
                            Model model) {
        User user = accountService.getRequiredUser(principal.getName());
        AddressFormRequest addressForm = new AddressFormRequest();

        if (editId != null) {
            accountService.findUserAddress(user, editId).ifPresent(addr -> {
                addressForm.setId(addr.getId());
                addressForm.setReceiverName(addr.getReceiverName());
                addressForm.setPhone(addr.getPhone());
                addressForm.setStreet(addr.getStreet());
                addressForm.setWard(addr.getWard());
                addressForm.setDistrict(addr.getDistrict());
                addressForm.setProvince(addr.getProvince());
                addressForm.setDefaultAddress(addr.isDefaultAddress());
            });
        } else {
            addressForm.setReceiverName(user.getFullName());
            addressForm.setPhone(user.getPhone());
        }

        model.addAttribute("user", user);
        model.addAttribute("addresses", accountService.getAddresses(user));
        model.addAttribute("addressForm", addressForm);
        return "account/addresses";
    }

    @PostMapping("/addresses/save")
    public String saveAddress(@ModelAttribute AddressFormRequest addressForm,
                              @RequestParam(required = false) String redirectTo,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        try {
            User user = accountService.getRequiredUser(principal.getName());
            Address saved = accountService.saveAddress(user, addressForm);
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu địa chỉ nhận hàng của " + saved.getReceiverName() + ".");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        if (redirectTo != null && !redirectTo.isBlank()) {
            return "redirect:" + redirectTo;
        }
        return "redirect:/account/addresses";
    }

    @PostMapping("/addresses/{id}/default")
    public String setDefaultAddress(@PathVariable Long id,
                                    Principal principal,
                                    RedirectAttributes redirectAttributes) {
        try {
            User user = accountService.getRequiredUser(principal.getName());
            accountService.setDefaultAddress(user, id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã đặt làm địa chỉ nhận hàng mặc định.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/account/addresses";
    }

    @PostMapping("/addresses/{id}/delete")
    public String deleteAddress(@PathVariable Long id,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            User user = accountService.getRequiredUser(principal.getName());
            accountService.deleteAddress(user, id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa địa chỉ nhận hàng.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/account/addresses";
    }
}
