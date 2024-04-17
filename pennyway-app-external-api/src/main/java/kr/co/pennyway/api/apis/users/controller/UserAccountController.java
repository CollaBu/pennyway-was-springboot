package kr.co.pennyway.api.apis.users.controller;

import kr.co.pennyway.api.common.response.SuccessResponse;
import kr.co.pennyway.api.common.security.authentication.SecurityUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v2/users/me")
public class UserAccountController {
    private final UserAccountUseCase userAccountUseCase;

    @PutMapping("/devices")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> registerDevice(@RequestBody @Validated DeviceDto.RegisterReq request, @AuthenticationPrincipal SecurityUserDetails user) {
        DeviceDto.RegisterRes response = DeviceDto.RegisterRes.of(userAuthUseCase.registerDevice(user.getUserId(), request));
        return ResponseEntity.ok(SuccessResponse.from("device", response)); // response -> {"id": ..}
    }

    @DeleteMapping("/devices/{deviceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> unregisterDevice(@PathVariable Long deviceId, @AuthenticationPrincipal SecurityUserDetails user) {
        userAuthUseCase.unregisterDevice(user.getUserId(), deviceId);
        return ResponseEntity.ok(SuccessResponse.noContent());
    }
}
