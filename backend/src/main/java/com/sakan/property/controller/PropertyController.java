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

import jakarta.validation.Valid;

/**
 * REST API for listings.
 *
 * <pre>
 * GET  /api/properties               public search & browse  (public)
 * POST /api/properties               create a draft          (owner, agent)   201
 * GET  /api/properties/{id}          one listing             (public if published)
 * GET  /api/properties/mine          my listings, paginated  (owner, agent)
 * PUT  /api/properties/{id}          edit                    (owner/agent of the listing)
 * POST /api/properties/{id}/publish  DRAFT -> PUBLISHED      (owner/agent of the listing)
 * POST /api/properties/{id}/archive  -> ARCHIVED             (owner/agent of the listing)
 * </pre>
 */
@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertyService service;
    private final CurrentUserProvider currentUser;

    public PropertyController(PropertyService service, CurrentUserProvider currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ResponseEntity<PageResponse<PropertySummaryResponse>> browse(
            @RequestParam(name = "locationId", required = false) Long locationId,
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(name = "currency", required = false) Currency currency,
            @RequestParam(name = "propertyTypeId", required = false) Long propertyTypeId,
            @RequestParam(name = "purpose", required = false) PropertyPurpose purpose,
            @RequestParam(name = "minBedrooms", required = false) Integer minBedrooms,
            @RequestParam(name = "amenityIds", required = false) List<Long> amenityIds,
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "12") Integer size,
            @RequestParam(name = "sort", defaultValue = "createdAt,desc") String sort
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
                sort
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
