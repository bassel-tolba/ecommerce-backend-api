package com.ecommerce.app.dto;

import com.ecommerce.app.model.Address;

// FIX: Removed @Builder from record.
public record AddressDto(
        Long id,
        String addressLine1,
        String addressLine2,
        String city,
        String stateProvince,
        String postalCode,
        String country) {
    public static AddressDto toDto(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressDto(
                address.getId(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getCity(),
                address.getStateProvince(),
                address.getPostalCode(),
                address.getCountry());
    }

    public Address toEntity() {
        Address address = new Address();
        address.setId(this.id);
        address.setAddressLine1(this.addressLine1);
        address.setAddressLine2(this.addressLine2);
        address.setCity(this.city);
        address.setStateProvince(this.stateProvince);
        address.setPostalCode(this.postalCode);
        address.setCountry(this.country);
        return address;
    }
}
