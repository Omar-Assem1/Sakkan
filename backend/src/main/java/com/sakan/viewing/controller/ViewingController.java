package com.sakan.viewing.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sakan.common.security.CurrentUserProvider;
import com.sakan.viewing.dto.RequestViewingRequest;
import com.sakan.viewing.dto.ViewingResponse;
import com.sakan.viewing.service.ViewingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Viewings", description = "Request and manage property viewing appointments (Form 6, FR-17, FR-18)")
@RestController
@RequestMapping("/api/viewings")
public class ViewingController {

    private final ViewingService service;
    private final CurrentUserProvider currentUser;

    public ViewingController(ViewingService service, CurrentUserProvider currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @Operation(
            summary = "Request a property viewing",
            description = "Books an available viewing slot with pessimistic locking and conflict detection")
    @ApiResponse(responseCode = "201", description = "Viewing request created with status REQUESTED")
    @ApiResponse(responseCode = "400", description = "Validation error (missing slotId or notes too long)")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Role not allowed or self-booking own property")
    @ApiResponse(responseCode = "404", description = "Viewing slot or property not found")
    @ApiResponse(responseCode = "409", description = "Slot already has an active REQUESTED or CONFIRMED viewing")
    @PreAuthorize("hasAnyRole('BUYER_TENANT', 'OWNER', 'AGENT')")
    @PostMapping
    public ResponseEntity<ViewingResponse> requestViewing(@Valid @RequestBody RequestViewingRequest request) {
        ViewingResponse created = service.requestViewing(request, currentUser.require());
        URI location = URI.create("/api/viewings/" + created.id());
        return ResponseEntity.created(location).body(created);
    }
}
