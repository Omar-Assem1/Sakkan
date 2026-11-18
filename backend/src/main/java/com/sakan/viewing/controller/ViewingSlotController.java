package com.sakan.viewing.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sakan.common.security.CurrentUserProvider;
import com.sakan.viewing.dto.CreateViewingSlotRequest;
import com.sakan.viewing.dto.ViewingSlotResponse;
import com.sakan.viewing.service.ViewingSlotService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Viewing Slots", description = "Owner/agent viewing availability for a property (Form 7, FR-16)")
@RestController
@RequestMapping("/api/properties/{propertyId}/slots")
public class ViewingSlotController {

    private final ViewingSlotService service;
    private final CurrentUserProvider currentUser;

    public ViewingSlotController(ViewingSlotService service, CurrentUserProvider currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @Operation(summary = "List viewing slots of a property", description = "Public for PUBLISHED properties; ordered by start time")
    @ApiResponse(responseCode = "200", description = "Slots returned")
    @ApiResponse(responseCode = "404", description = "Property not found or not visible")
    @GetMapping
    public List<ViewingSlotResponse> list(@PathVariable("propertyId") Long propertyId) {
        return service.listSlotsForProperty(propertyId, currentUser.find().orElse(null));
    }

    @Operation(summary = "Add a viewing slot", description = "Property owner or assigned agent only; property must be PUBLISHED")
    @ApiResponse(responseCode = "201", description = "Slot created")
    @ApiResponse(responseCode = "400", description = "Invalid or past time range")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Not the property owner or assigned agent")
    @ApiResponse(responseCode = "409", description = "Overlaps an existing slot, or property is not PUBLISHED")
    @PreAuthorize("hasAnyRole('OWNER', 'AGENT')")
    @PostMapping
    public ResponseEntity<ViewingSlotResponse> create(
            @PathVariable("propertyId") Long propertyId,
            @Valid @RequestBody CreateViewingSlotRequest request) {
        ViewingSlotResponse created = service.createSlot(propertyId, request, currentUser.require());
        URI location = URI.create("/api/properties/" + propertyId + "/slots/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Remove a viewing slot", description = "Property owner or assigned agent only")
    @ApiResponse(responseCode = "204", description = "Slot removed")
    @ApiResponse(responseCode = "403", description = "Not the property owner or assigned agent")
    @ApiResponse(responseCode = "404", description = "Slot not found on this property")
    @PreAuthorize("hasAnyRole('OWNER', 'AGENT')")
    @DeleteMapping("/{slotId}")
    public ResponseEntity<Void> delete(
            @PathVariable("propertyId") Long propertyId,
            @PathVariable("slotId") Long slotId) {
        service.deleteSlot(propertyId, slotId, currentUser.require());
        return ResponseEntity.noContent().build();
    }
}
