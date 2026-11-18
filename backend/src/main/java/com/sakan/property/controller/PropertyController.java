package com.sakan.property.web;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sakan.common.domain.Currency;
import com.sakan.common.response.PageResponse;
import com.sakan.common.security.CurrentUserProvider;
import com.sakan.property.domain.PropertyPurpose;
import com.sakan.property.dto.PropertyRequest;
import com.sakan.property.dto.PropertyResponse;
import com.sakan.property.dto.PropertySearchCriteria;
import com.sakan.property.dto.PropertySummaryResponse;
import com.sakan.property.service.PropertyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertyService service;
    private final CurrentUserProvider currentUser;

    public PropertyController(PropertyService service, CurrentUserProvider currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @Operation(
            summary = "Browse and search properties",
            description = "Public endpoint for browsing published properties with full-text search across titles and descriptions, multi-criteria filtering, sorting, and pagination"
    )
    @ApiResponse(responseCode = "200", description = "Listings matching criteria returned successfully")
    @ApiResponse(responseCode = "400", description = "Invalid criteria or search parameter exceeding 100 characters")
    @GetMapping
    public ResponseEntity<PageResponse<PropertySummaryResponse>> browse(
            @Parameter(description = "Governorate, city, district, or compound location ID")
            @RequestParam(name = "locationId", required = false) Long locationId,
            @Parameter(description = "Minimum price filter")
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price filter")
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
            @Parameter(description = "Currency filter (e.g. EGP, USD)")
            @RequestParam(name = "currency", required = false) Currency currency,
            @Parameter(description = "Property type ID")
            @RequestParam(name = "propertyTypeId", required = false) Long propertyTypeId,
            @Parameter(description = "Listing purpose (SALE or RENT)")
            @RequestParam(name = "purpose", required = false) PropertyPurpose purpose,
            @Parameter(description = "Minimum bedrooms filter")
            @RequestParam(name = "minBedrooms", required = false) Integer minBedrooms,
            @Parameter(description = "List of required amenity IDs")
            @RequestParam(name = "amenityIds", required = false) List<Long> amenityIds,
            @Parameter(description = "Full-text search query across title and description (supports Arabic and English)")
            @RequestParam(name = "q", required = false) String q,
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @Parameter(description = "Page size (1 to 50)")
            @RequestParam(name = "size", defaultValue = "12") Integer size,
            @Parameter(description = "Sort field and direction (e.g. createdAt,desc; price,asc; relevance,desc)")
            @RequestParam(name = "sort", required = false) String sort
    ) {
        PropertySearchCriteria criteria = new PropertySearchCriteria(
                locationId,
                minPrice,
                maxPrice,
                currency,
                propertyTypeId,
                purpose,
                minBedrooms,
                amenityIds,
                page,
                size,
                sort,
                q
        );
        return ResponseEntity.ok(service.browse(criteria));
    }

    @PostMapping
    public ResponseEntity<PropertyResponse> create(@Valid @RequestBody PropertyRequest request) {
        PropertyResponse created = service.create(request, currentUser.require());
        return ResponseEntity.created(URI.create("/api/properties/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public PropertyResponse get(@PathVariable("id") Long id) {
        return service.getById(id, currentUser.find().orElse(null));
    }

    @GetMapping("/mine")
    public PageResponse<PropertyResponse> mine(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return service.listMine(currentUser.require(), page, size);
    }

    @PutMapping("/{id}")
    public PropertyResponse update(@PathVariable("id") Long id, @Valid @RequestBody PropertyRequest request) {
        return service.update(id, request, currentUser.require());
    }

    @PostMapping("/{id}/publish")
    public PropertyResponse publish(@PathVariable("id") Long id) {
        return service.publish(id, currentUser.require());
    }

    @PostMapping("/{id}/archive")
    public PropertyResponse archive(@PathVariable("id") Long id) {
        return service.archive(id, currentUser.require());
    }
}
