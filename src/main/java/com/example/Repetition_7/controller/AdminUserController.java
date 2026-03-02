package com.example.Repetition_7.controller;

import com.example.Repetition_7.response.UserResponse;
import com.example.Repetition_7.service.UserService;
import com.example.Repetition_7.validation.UserPageableValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
@Tag(name = "Admin - Users", description = "Administrative operations on users")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final UserService userService;
    private final UserPageableValidator userPageableValidator;

    @GetMapping
    @Operation(summary = "Search users (Admin only)",
            description = "Returns paginated list of users with optional filters by username and userId")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully returned paginated list of users"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - user is not ADMIN"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public Page<UserResponse> getAll(@PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) @ParameterObject Pageable pageable,
                                     @RequestParam (required = false)
                                     @Parameter(description = "Filter by username (contains, case-insensitive)", example = "testuser") String username,
                                     @RequestParam (required = false)
                                     @Parameter(description = "Filter by exact userId", example = "1") Long userId) {

        userPageableValidator.validate(pageable);

        return userService.search(pageable, userId, username);
    }
}
