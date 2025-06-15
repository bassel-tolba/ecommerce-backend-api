// --- FILE: AddressDto.java ---
package com.ecommerce.app.dto;

import com.ecommerce.app.model.Address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressDto(
        Long id,
        @NotBlank @Size(max = 255) String addressLine1,
        @Size(max = 255) String addressLine2,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 100) String stateProvince,
        @NotBlank @Size(max = 20) String postalCode,
        @NotBlank @Size(max = 100) String country,
        // --- ADDED ---
        Boolean isDefaultShipping,
        // --- ADDED ---
        Boolean isDefaultBilling) {

    /**
     * Converts an Address entity to an AddressDto.
     */
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
                address.getCountry(),
                address.isDefaultShipping(),
                address.isDefaultBilling());
    }

    /**
     * Converts this DTO to an Address entity.
     * Note: User association must be handled by the service layer.
     */
    public Address toEntity() {
        Address address = new Address();
        // ID is not set here, as it's for creation or handled separately in updates.
        address.setAddressLine1(this.addressLine1);
        address.setAddressLine2(this.addressLine2);
        address.setCity(this.city);
        address.setStateProvince(this.stateProvince);
        address.setPostalCode(this.postalCode);
        address.setCountry(this.country);
        // Set default flags, handling nulls to prevent errors.
        address.setDefaultShipping(this.isDefaultShipping != null && this.isDefaultShipping);
        address.setDefaultBilling(this.isDefaultBilling != null && this.isDefaultBilling);
        return address;
    }
}