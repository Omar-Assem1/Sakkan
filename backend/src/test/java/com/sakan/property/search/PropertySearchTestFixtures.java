package com.sakan.property.search;

import java.math.BigDecimal;
import java.util.Set;

import com.sakan.common.domain.Currency;
import com.sakan.property.domain.Property;
import com.sakan.property.domain.PropertyPurpose;
import com.sakan.property.domain.PropertyStatus;

public final class PropertySearchTestFixtures {

    private PropertySearchTestFixtures() {}

    public static Property publishedEnglishSmouha() {
        Property p = new Property();
        p.setTitle("Luxury Apartment in Smouha");
        p.setDescription("Spacious residential flat close to sporting club and shopping centers");
        p.setOwnerId(100L);
        p.setLocationId(10L);
        p.setPropertyTypeId(1L);
        p.setPrice(new BigDecimal("2500000.00"));
        p.setCurrency(Currency.EGP);
        p.setPurpose(PropertyPurpose.SALE);
        p.setBedrooms(3);
        p.setBathrooms(2);
        p.setAreaSqm(new BigDecimal("150.00"));
        p.setStatus(PropertyStatus.PUBLISHED);
        p.setAmenityIds(Set.of(1L, 2L));
        return p;
    }

    public static Property publishedArabicSmouha() {
        Property p = new Property();
        p.setTitle("شقة فاخرة للبيع في سموحة");
        p.setDescription("تشطيب الترا لوكس بالقرب من جرين بلازا وخلف مستشفى سموحة");
        p.setOwnerId(101L);
        p.setLocationId(10L);
        p.setPropertyTypeId(1L);
        p.setPrice(new BigDecimal("3200000.00"));
        p.setCurrency(Currency.EGP);
        p.setPurpose(PropertyPurpose.SALE);
        p.setBedrooms(3);
        p.setBathrooms(2);
        p.setAreaSqm(new BigDecimal("165.00"));
        p.setStatus(PropertyStatus.PUBLISHED);
        p.setAmenityIds(Set.of(1L, 3L));
        return p;
    }

    public static Property publishedArabicDiacriticsAndVariants() {
        Property p = new Property();
        p.setTitle("إطلالة رائعة على البحر - روف فِي الْإِسْكَنْدَرِيَّةِ");
        p.setDescription("عقار استثماري مميز بإطلالة بانورامية ساحرة على البحر الأبيض المتوسط");
        p.setOwnerId(102L);
        p.setLocationId(11L);
        p.setPropertyTypeId(1L);
        p.setPrice(new BigDecimal("4500000.00"));
        p.setCurrency(Currency.EGP);
        p.setPurpose(PropertyPurpose.SALE);
        p.setBedrooms(4);
        p.setBathrooms(3);
        p.setAreaSqm(new BigDecimal("210.00"));
        p.setStatus(PropertyStatus.PUBLISHED);
        p.setAmenityIds(Set.of(2L, 4L));
        return p;
    }

    public static Property publishedDescriptionOnlyOlympic() {
        Property p = new Property();
        p.setTitle("Exclusive Modern Villa");
        p.setDescription("Private family residence featuring an olympic swimming pool and landscaped gardens");
        p.setOwnerId(103L);
        p.setLocationId(12L);
        p.setPropertyTypeId(2L);
        p.setPrice(new BigDecimal("8000000.00"));
        p.setCurrency(Currency.EGP);
        p.setPurpose(PropertyPurpose.SALE);
        p.setBedrooms(5);
        p.setBathrooms(4);
        p.setAreaSqm(new BigDecimal("420.00"));
        p.setStatus(PropertyStatus.PUBLISHED);
        p.setAmenityIds(Set.of(2L, 3L, 4L));
        return p;
    }

    public static Property publishedArabicDescriptionOnlyGarden() {
        Property p = new Property();
        p.setTitle("فيلا مستقلة في كفر عبده");
        p.setDescription("موقع هادئ جدا وراقي مع حديقة واسعة وحمام سباحة خاص للأطفال");
        p.setOwnerId(104L);
        p.setLocationId(11L);
        p.setPropertyTypeId(2L);
        p.setPrice(new BigDecimal("9000000.00"));
        p.setCurrency(Currency.EGP);
        p.setPurpose(PropertyPurpose.SALE);
        p.setBedrooms(6);
        p.setBathrooms(5);
        p.setAreaSqm(new BigDecimal("500.00"));
        p.setStatus(PropertyStatus.PUBLISHED);
        p.setAmenityIds(Set.of(1L, 2L, 3L, 4L));
        return p;
    }

    public static Property publishedTypoCompoundTarget() {
        Property p = new Property();
        p.setTitle("Alexandria Royal Compound Chalet");
        p.setDescription("Summer chalet directly on the sandy beach with private access");
        p.setOwnerId(105L);
        p.setLocationId(13L);
        p.setPropertyTypeId(4L);
        p.setPrice(new BigDecimal("1800000.00"));
        p.setCurrency(Currency.EGP);
        p.setPurpose(PropertyPurpose.SALE);
        p.setBedrooms(2);
        p.setBathrooms(1);
        p.setAreaSqm(new BigDecimal("95.00"));
        p.setStatus(PropertyStatus.PUBLISHED);
        p.setAmenityIds(Set.of(2L));
        return p;
    }

    public static Property draftSmouha() {
        Property p = new Property();
        p.setTitle("شقة غير منشورة في سموحة مسودة");
        p.setDescription("قيد المراجعة الفنية ولا يجوز ظهورها في البحث العام");
        p.setOwnerId(100L);
        p.setLocationId(10L);
        p.setPropertyTypeId(1L);
        p.setPrice(new BigDecimal("2100000.00"));
        p.setCurrency(Currency.EGP);
        p.setPurpose(PropertyPurpose.SALE);
        p.setBedrooms(2);
        p.setBathrooms(1);
        p.setAreaSqm(new BigDecimal("110.00"));
        p.setStatus(PropertyStatus.DRAFT);
        p.setAmenityIds(Set.of(1L));
        return p;
    }

    public static Property archivedSmouha() {
        Property p = new Property();
        p.setTitle("Archived Old Apartment in Smouha");
        p.setDescription("Historic listing marked as archived and unavailable");
        p.setOwnerId(100L);
        p.setLocationId(10L);
        p.setPropertyTypeId(1L);
        p.setPrice(new BigDecimal("1500000.00"));
        p.setCurrency(Currency.EGP);
        p.setPurpose(PropertyPurpose.SALE);
        p.setBedrooms(3);
        p.setBathrooms(2);
        p.setAreaSqm(new BigDecimal("130.00"));
        p.setStatus(PropertyStatus.ARCHIVED);
        p.setAmenityIds(Set.of(1L));
        return p;
    }
}
