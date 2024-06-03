package kr.co.pennyway.api.apis.users.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotBlank;
import kr.co.pennyway.api.apis.users.api.UserAccountApi;
import kr.co.pennyway.api.apis.users.dto.DeviceDto;
import kr.co.pennyway.api.apis.users.dto.UserProfileUpdateDto;
import kr.co.pennyway.api.apis.users.usecase.UserAccountUseCase;
import kr.co.pennyway.api.common.response.SuccessResponse;
import kr.co.pennyway.api.common.security.authentication.SecurityUserDetails;
import kr.co.pennyway.domain.domains.user.domain.NotifySetting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v2/users/me")
public class UserAccountController implements UserAccountApi {
	private final UserAccountUseCase userAccountUseCase;

	@Override
	@PutMapping("/devices")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> putDevice(@RequestBody @Validated DeviceDto.RegisterReq request, @AuthenticationPrincipal SecurityUserDetails user) {
		return ResponseEntity.ok(SuccessResponse.from("device", userAccountUseCase.registerDevice(user.getUserId(), request)));
	}

	@Override
	@DeleteMapping("/devices")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> deleteDevice(@RequestParam("token") @Validated @NotBlank String token, @AuthenticationPrincipal SecurityUserDetails user) {
		userAccountUseCase.unregisterDevice(user.getUserId(), token);
		return ResponseEntity.ok(SuccessResponse.noContent());
	}

	@Override
	@GetMapping("")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> getMyAccount(@AuthenticationPrincipal SecurityUserDetails user) {
		return ResponseEntity.ok(SuccessResponse.from("user", userAccountUseCase.getMyAccount(user.getUserId())));
	}

	@Override
	@PatchMapping("/name")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> putName(UserProfileUpdateDto.NameReq request, SecurityUserDetails user) {
		userAccountUseCase.updateName(user.getUserId(), request.name());
		return ResponseEntity.ok(SuccessResponse.noContent());
	}

	@Override
	@PatchMapping("/username")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> putUsername(UserProfileUpdateDto.UsernameReq request, SecurityUserDetails user) {
		userAccountUseCase.updateUsername(user.getUserId(), request.username());
		return ResponseEntity.ok(SuccessResponse.noContent());
	}

	@Override
	@PostMapping("/password/verification")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> postPasswordVerification(UserProfileUpdateDto.PasswordVerificationReq request, SecurityUserDetails user) {
		userAccountUseCase.verifyPassword(user.getUserId(), request.password());
		return ResponseEntity.ok(SuccessResponse.noContent());
	}

	@Override
	@PatchMapping("/password")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> patchPassword(UserProfileUpdateDto.PasswordReq request, SecurityUserDetails user) {
		userAccountUseCase.updatePassword(user.getUserId(), request.oldPassword(), request.newPassword());
		return ResponseEntity.ok(SuccessResponse.noContent());
	}

	@Override
	@PatchMapping("/notifications")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> patchNotifySetting(@RequestParam NotifySetting.NotifyType type, @AuthenticationPrincipal SecurityUserDetails user) {
		return ResponseEntity.ok(SuccessResponse.from("notifySetting", userAccountUseCase.activateNotification(user.getUserId(), type)));
	}

	@Override
	@DeleteMapping("/notifications")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> deleteNotifySetting(@RequestParam NotifySetting.NotifyType type, @AuthenticationPrincipal SecurityUserDetails user) {
		return ResponseEntity.ok(SuccessResponse.from("notifySetting", userAccountUseCase.deactivateNotification(user.getUserId(), type)));
	}

	@Override
	@DeleteMapping("")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> deleteAccount(@AuthenticationPrincipal SecurityUserDetails user) {
		userAccountUseCase.deleteAccount(user.getUserId());
		return ResponseEntity.ok(SuccessResponse.noContent());
	}

	@Override
	@PostMapping("/profile-image")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> postProfileImage(UserProfileUpdateDto.ProfileImageReq request, SecurityUserDetails user) {
		userAccountUseCase.updateProfileImage(user.getUserId(), request);
		return ResponseEntity.ok(SuccessResponse.noContent());
	}
}
