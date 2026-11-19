package com.sakan.location.web;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sakan.location.dto.LocationResponse;
import com.sakan.location.service.LocationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/locations")
@Tag(name = "Locations", description = "Public, cascading English and Arabic location data")
public class LocationController {

    private final LocationService locations;

    public LocationController(LocationService locations) {
        this.locations = locations;
    }

    @GetMapping("/governorates")
    @Operation(summary = "List governorates", description = "Returns root locations with names localized by Accept-Language.")
    @ApiResponse(responseCode = "200", description = "Governorates returned")
    public ResponseEntity<List<LocationResponse>> governorates(
            @Parameter(name = HttpHeaders.ACCEPT_LANGUAGE, in = ParameterIn.HEADER,
                    description = "Preferred name language; supports English and Arabic", example = "ar")
            @RequestHeader(name = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        return ResponseEntity.ok(locations.governorates(acceptLanguage));
    }

    @GetMapping("/cities")
    @Operation(summary = "List cities by governorate", description = "Returns only cities whose parent is the specified governorate.")
    @ApiResponse(responseCode = "200", description = "Cities returned")
    @ApiResponse(responseCode = "400", description = "parentId is required")
    public ResponseEntity<List<LocationResponse>> cities(
            @Parameter(description = "Governorate ID")
            @RequestParam long parentId,
            @Parameter(name = HttpHeaders.ACCEPT_LANGUAGE, in = ParameterIn.HEADER,
                    description = "Preferred name language; supports English and Arabic", example = "ar")
            @RequestHeader(name = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        return ResponseEntity.ok(locations.cities(parentId, acceptLanguage));
    }

    @GetMapping("/districts")
    @Operation(summary = "List districts by city", description = "Returns only districts whose parent is the specified city.")
    @ApiResponse(responseCode = "200", description = "Districts returned")
    @ApiResponse(responseCode = "400", description = "parentId is required")
    public ResponseEntity<List<LocationResponse>> districts(
            @Parameter(description = "City ID")
            @RequestParam long parentId,
            @Parameter(name = HttpHeaders.ACCEPT_LANGUAGE, in = ParameterIn.HEADER,
                    description = "Preferred name language; supports English and Arabic", example = "ar")
            @RequestHeader(name = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        return ResponseEntity.ok(locations.districts(parentId, acceptLanguage));
    }

    @GetMapping("/compounds")
    @Operation(summary = "List compounds by district", description = "Returns only compounds whose parent is the specified district.")
    @ApiResponse(responseCode = "200", description = "Compounds returned")
    @ApiResponse(responseCode = "400", description = "parentId is required")
    public ResponseEntity<List<LocationResponse>> compounds(
            @Parameter(description = "District ID")
            @RequestParam long parentId,
            @Parameter(name = HttpHeaders.ACCEPT_LANGUAGE, in = ParameterIn.HEADER,
                    description = "Preferred name language; supports English and Arabic", example = "ar")
            @RequestHeader(name = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        return ResponseEntity.ok(locations.compounds(parentId, acceptLanguage));
    }
}
