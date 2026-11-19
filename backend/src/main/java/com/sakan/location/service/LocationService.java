package com.sakan.location.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.sakan.location.domain.LocationType;
import com.sakan.location.dto.LocationResponse;
import com.sakan.location.repository.LocationRepository;
import com.sakan.location.repository.LocationRepository.LocationRecord;

@Service
public class LocationService {

    private final LocationRepository locations;

    public LocationService(LocationRepository locations) {
        this.locations = locations;
    }

    public List<LocationResponse> governorates(String acceptLanguage) {
        return localize(locations.findGovernorates(), acceptLanguage);
    }

    public List<LocationResponse> cities(long governorateId, String acceptLanguage) {
        return localize(locations.findChildren(governorateId, LocationType.GOVERNORATE, LocationType.CITY), acceptLanguage);
    }

    public List<LocationResponse> districts(long cityId, String acceptLanguage) {
        return localize(locations.findChildren(cityId, LocationType.CITY, LocationType.DISTRICT), acceptLanguage);
    }

    public List<LocationResponse> compounds(long districtId, String acceptLanguage) {
        return localize(locations.findChildren(districtId, LocationType.DISTRICT, LocationType.COMPOUND), acceptLanguage);
    }

    private static List<LocationResponse> localize(List<LocationRecord> rows, String acceptLanguage) {
        boolean arabic = prefersArabic(acceptLanguage);
        return rows.stream()
                .map(location -> new LocationResponse(
                        location.id(),
                        location.parentId(),
                        location.type(),
                        arabic ? location.nameAr() : location.nameEn()))
                .toList();
    }

    private static boolean prefersArabic(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return false;
        }

        try {
            for (Locale.LanguageRange range : Locale.LanguageRange.parse(acceptLanguage)) {
                if (range.getWeight() <= 0) {
                    continue;
                }
                String language = Locale.forLanguageTag(range.getRange()).getLanguage();
                if ("ar".equals(language)) {
                    return true;
                }
                if ("en".equals(language)) {
                    return false;
                }
            }
        } catch (IllegalArgumentException ignored) {
            return false;
        }
        return false;
    }
}
