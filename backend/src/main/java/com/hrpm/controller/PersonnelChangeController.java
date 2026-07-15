package com.hrpm.controller;

import com.hrpm.common.ApiResponse;
import com.hrpm.dto.PersonnelChangeDTOs.ChangeAction;
import com.hrpm.dto.PersonnelChangeDTOs.ConfirmHandoverItem;
import com.hrpm.dto.PersonnelChangeDTOs.CreateChange;
import com.hrpm.dto.PersonnelChangeDTOs.CreateHandoverItem;
import com.hrpm.dto.PersonnelChangeDTOs.UpdateChange;
import com.hrpm.security.AuthenticatedUser;
import com.hrpm.service.PersonnelChangeService;
import com.hrpm.vo.PageVO;
import com.hrpm.vo.PersonnelChangeVOs.PersonnelChangeDetailVO;
import com.hrpm.vo.PersonnelChangeVOs.PersonnelChangeListItemVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/personnel-changes")
public class PersonnelChangeController {
    private final PersonnelChangeService service;

    public PersonnelChangeController(PersonnelChangeService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('personnel:read')")
    public ApiResponse<PageVO<PersonnelChangeListItemVO>> list(@AuthenticationPrincipal AuthenticatedUser user,
                                                               @RequestParam(defaultValue = "1") int page,
                                                               @RequestParam(defaultValue = "20") int pageSize,
                                                               @RequestParam(required = false) Long employeeId,
                                                               @RequestParam(required = false) String changeType,
                                                               @RequestParam(required = false) String status,
                                                               @RequestParam(required = false) LocalDate fromDate,
                                                               @RequestParam(required = false) LocalDate toDate) {
        return ApiResponse.success(service.list(user.userId(), page, pageSize, employeeId, changeType, status, fromDate, toDate));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('personnel:create')")
    public ApiResponse<PersonnelChangeDetailVO> create(@AuthenticationPrincipal AuthenticatedUser user,
                                                       @Valid @RequestBody CreateChange command) {
        return ApiResponse.success(service.create(user.userId(), command));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('personnel:read')")
    public ApiResponse<PersonnelChangeDetailVO> detail(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id) {
        return ApiResponse.success(service.detail(user.userId(), id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('personnel:create')")
    public ApiResponse<PersonnelChangeDetailVO> update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id,
                                                       @Valid @RequestBody UpdateChange command) {
        return ApiResponse.success(service.update(user.userId(), id, command));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('personnel:create')")
    public ApiResponse<PersonnelChangeDetailVO> submit(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id,
                                                       @Valid @RequestBody ChangeAction command) {
        return ApiResponse.success(service.submit(user.userId(), id, command));
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasAuthority('personnel:create')")
    public ApiResponse<PersonnelChangeDetailVO> withdraw(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id,
                                                         @Valid @RequestBody ChangeAction command) {
        return ApiResponse.success(service.withdraw(user.userId(), id, command));
    }

    @PostMapping("/{id}/effective")
    @PreAuthorize("hasAuthority('personnel:execute')")
    public ApiResponse<PersonnelChangeDetailVO> effective(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id,
                                                          @Valid @RequestBody ChangeAction command) {
        return ApiResponse.success(service.effective(user.userId(), id, command));
    }

    @PostMapping("/{id}/handover-items")
    @PreAuthorize("hasAuthority('personnel:create')")
    public ApiResponse<PersonnelChangeDetailVO> addHandoverItem(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id,
                                                                @Valid @RequestBody CreateHandoverItem command) {
        return ApiResponse.success(service.addHandoverItem(user.userId(), id, command));
    }

    @PostMapping("/{id}/handover-items/{itemId}/confirm")
    @PreAuthorize("hasAuthority('personnel:create')")
    public ApiResponse<PersonnelChangeDetailVO> confirmHandoverItem(@AuthenticationPrincipal AuthenticatedUser user,
                                                                    @PathVariable long id, @PathVariable long itemId,
                                                                    @Valid @RequestBody ConfirmHandoverItem command) {
        return ApiResponse.success(service.confirmHandoverItem(user.userId(), id, itemId, command));
    }
}
