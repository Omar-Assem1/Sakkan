package com.sakan.property.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sakan.common.exception.ForbiddenOperationException;
import com.sakan.common.exception.InvalidStatusTransitionException;
import com.sakan.common.exception.ResourceNotFoundException;
import com.sakan.common.response.PageResponse;
import com.sakan.common.security.AuthenticatedUser;
import com.sakan.common.statemachine.StatusTransitionService;
import com.sakan.common.statemachine.StatusTransitions;
import com.sakan.property.domain.Property;
import com.sakan.property.domain.PropertyImage;
import com.sakan.property.domain.PropertyStatus;
import com.sakan.property.dto.PropertyMapper;
import com.sakan.property.dto.PropertyRequest;
import com.sakan.property.dto.PropertyResponse;
import com.sakan.property.dto.PropertySearchCriteria;
import com.sakan.property.dto.PropertySummaryResponse;
import com.sakan.property.repository.PropertyImageRepository;
import com.sakan.property.repository.PropertyRepository;

@Service
@Transactional
public class PropertyService {

    static final int DEFAULT_PAGE_SIZE = 12;
    static final int MAX_PAGE_SIZE = 50;

    private static final Logger log = LoggerFactory.getLogger(PropertyService.class);
    private static final StatusTransitions<PropertyStatus> PROPERTY_TRANSITIONS =
            StatusTransitions.from(PropertyStatus.class, PropertyStatus::allowedTargets);

    private final PropertyRepository repository;
    private final PropertyMapper mapper;
    private final LocationHierarchyService locationHierarchyService;
    private final PropertySortValidator sortValidator;
    private final StatusTransitionService statusTransitionService;
    private final PropertyImageRepository imageRepository;

    @Autowired
    public PropertyService(
            PropertyRepository repository,
            PropertyMapper mapper,
            LocationHierarchyService locationHierarchyService,
            PropertySortValidator sortValidator,
            StatusTransitionService statusTransitionService,
            PropertyImageRepository imageRepository) {
        this.repository = repository;
        this.mapper = mapper;
        this.locationHierarchyService = locationHierarchyService;
        this.sortValidator = sortValidator;
        this.statusTransitionService = statusTransitionService;
        this.imageRepository = imageRepository;
    }

    /** Convenience constructor retained for isolated unit tests that do not inject imageRepository. */
    public PropertyService(
            PropertyRepository repository,
            PropertyMapper mapper,
            LocationHierarchyService locationHierarchyService,
            PropertySortValidator sortValidator,
            StatusTransitionService statusTransitionService) {
        this(repository, mapper, locationHierarchyService, sortValidator, statusTransitionService, null);
    }

    /** Convenience constructor retained for isolated unit tests that do not assert domain events. */
    public PropertyService(PropertyRepository repository, PropertyMapper mapper) {
        this(repository, mapper, rootId -> rootId == null ? List.of() : List.of(rootId), new PropertySortValidator(),
                new StatusTransitionService(event -> { }), null);
    }

    @Transactional(readOnly = true)
    public PageResponse<PropertySummaryResponse> browse(PropertySearchCriteria criteria) {
        Sort sort = sortValidator.validateAndBuildSort(criteria.sort());
        Pageable pageable = (sort.getOrderFor("relevance") != null)
                ? PageRequest.of(criteria.page(), criteria.size(), Sort.unsorted())
                : PageRequest.of(criteria.page(), criteria.size(), sort);

        Specification<Property> spec = PropertySpecifications.isPublished();

        if (criteria.q() != null && !criteria.q().isBlank()) {
            spec = spec.and(PropertySpecifications.hasSearch(criteria.q()));
        }

        if (criteria.locationId() != null) {
            List<Long> locationIds = locationHierarchyService.findDescendantLocationIds(criteria.locationId());
            spec = spec.and(PropertySpecifications.inLocationIds(locationIds));
        }

        if (criteria.minPrice() != null || criteria.maxPrice() != null || criteria.currency() != null) {
            spec = spec.and(PropertySpecifications.hasPriceBetween(criteria.minPrice(), criteria.maxPrice(), criteria.currency()));
        }

        if (criteria.propertyTypeId() != null) {
            spec = spec.and(PropertySpecifications.hasPropertyTypeId(criteria.propertyTypeId()));
        }

        if (criteria.purpose() != null) {
            spec = spec.and(PropertySpecifications.hasPurpose(criteria.purpose()));
        }

        if (criteria.minBedrooms() != null) {
            spec = spec.and(PropertySpecifications.hasMinBedrooms(criteria.minBedrooms()));
        }

        if (criteria.amenityIds() != null && !criteria.amenityIds().isEmpty()) {
            spec = spec.and(PropertySpecifications.hasAllAmenities(criteria.amenityIds()));
        }

        Page<Property> pageResult = repository.findAll(spec, pageable);
        Map<Long, String> thumbnails = loadPrimaryThumbnails(pageResult.getContent());
        return PageResponse.from(pageResult.map(property -> {
            PropertySummaryResponse summary = mapper.toSummaryResponse(property);
            String thumbnailUrl = property.getId() != null ? thumbnails.get(property.getId()) : null;
            return (summary != null && thumbnailUrl != null)
                    ? PropertySummaryResponse.withThumbnailUrl(summary, thumbnailUrl)
                    : summary;
        }));
    }

    public PropertyResponse create(PropertyRequest request, AuthenticatedUser actor) {
        if (!actor.canManageListings()) {
            throw new ForbiddenOperationException("Only owners and agents can create listings");
        }
        Property property = mapper.toEntity(request);
        property.setOwnerId(actor.id());
        if (actor.hasRole(AuthenticatedUser.AGENT)) {
            property.setAgentId(actor.id());
        }
        property.setStatus(PropertyStatus.DRAFT);
        Property saved = repository.save(property);
        log.info("Property {} created by user {}", saved.getId(), actor.id());
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true, noRollbackFor = ResourceNotFoundException.class)
    public PropertyResponse getById(Long id, AuthenticatedUser viewer) {
        Property property = findOrThrow(id);
        boolean visible = property.getStatus() == PropertyStatus.PUBLISHED
                || (viewer != null && (viewer.hasRole(AuthenticatedUser.ADMIN) || isManager(property, viewer)));
        if (!visible) {
            throw notFound(id);
        }
        PropertyResponse response = mapper.toResponse(property);
        String thumbnailUrl = property.getId() != null
                ? loadPrimaryThumbnails(List.of(property)).get(property.getId())
                : null;
        return (response != null && thumbnailUrl != null)
                ? PropertyResponse.withThumbnailUrl(response, thumbnailUrl)
                : response;
    }

    @Transactional(readOnly = true)
    public PageResponse<PropertyResponse> listMine(AuthenticatedUser actor, int page, int size) {
        if (!actor.canManageListings()) {
            throw new ForbiddenOperationException("Only owners and agents have listings");
        }
        Pageable pageable = PageRequest.of(
                Math.max(page, 0), clampSize(size), Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Property> result = repository.findManagedBy(actor.id(), pageable);
        Map<Long, String> thumbnails = loadPrimaryThumbnails(result.getContent());
        return PageResponse.from(result.map(property -> {
            PropertyResponse response = mapper.toResponse(property);
            String thumbnailUrl = property.getId() != null ? thumbnails.get(property.getId()) : null;
            return (response != null && thumbnailUrl != null)
                    ? PropertyResponse.withThumbnailUrl(response, thumbnailUrl)
                    : response;
        }));
    }

    public PropertyResponse update(Long id, PropertyRequest request, AuthenticatedUser actor) {
        Property property = findOrThrow(id);
        requireManager(property, actor);
        if (!property.getStatus().isEditable()) {
            throw new InvalidStatusTransitionException(
                    "A " + property.getStatus() + " listing cannot be edited");
        }
        mapper.updateEntity(request, property);
        return mapper.toResponse(repository.save(property));
    }

    public PropertyResponse publish(Long id, AuthenticatedUser actor) {
        return transition(id, PropertyStatus.PUBLISHED, actor);
    }

    public PropertyResponse archive(Long id, AuthenticatedUser actor) {
        return transition(id, PropertyStatus.ARCHIVED, actor);
    }

    private PropertyResponse transition(Long id, PropertyStatus target, AuthenticatedUser actor) {
        Property property = findOrThrow(id);
        requireManager(property, actor);
        PropertyStatus current = property.getStatus();
        statusTransitionService.transition("Property", id, current, target, "user:" + actor.id(),
                PROPERTY_TRANSITIONS, property::setStatus);
        Property saved = repository.save(property);
        log.info("Property {} moved {} -> {} by user {}", id, current, target, actor.id());
        return mapper.toResponse(saved);
    }

    private Property findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> notFound(id));
    }

    private ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("Property " + id + " not found");
    }

    private boolean isManager(Property property, AuthenticatedUser user) {
        return user.id().equals(property.getOwnerId()) || user.id().equals(property.getAgentId());
    }

    private void requireManager(Property property, AuthenticatedUser actor) {
        if (!isManager(property, actor)) {
            throw new ForbiddenOperationException("Only the owner or assigned agent can change this listing");
        }
    }

    private int clampSize(int size) {
        if (size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private Map<Long, String> loadPrimaryThumbnails(List<Property> properties) {
        if (imageRepository == null || properties.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = properties.stream()
                .map(Property::getId)
                .filter(id -> id != null)
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> thumbnails = new HashMap<>();
        List<PropertyImage> activeImages =
                imageRepository.findByPropertyIdInAndArchivedFalseOrderByDisplayOrderAsc(ids);
        for (PropertyImage img : activeImages) {
            String url = "/api/property-images/" + img.getThumbnailPath();
            if (img.isPrimary()) {
                thumbnails.put(img.getPropertyId(), url);
            } else {
                thumbnails.putIfAbsent(img.getPropertyId(), url);
            }
        }
        return thumbnails;
    }
}
