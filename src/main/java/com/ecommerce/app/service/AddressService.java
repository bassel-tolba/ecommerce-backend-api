// --- FILE: AddressService.java ---
package com.ecommerce.app.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.app.dto.AddressDto;
import com.ecommerce.app.exception.InvalidOperationException;
import com.ecommerce.app.exception.ResourceNotFoundException;
import com.ecommerce.app.model.Address;
import com.ecommerce.app.model.User;
import com.ecommerce.app.repository.AddressRepository;
import com.ecommerce.app.repository.UserRepository;
import com.ecommerce.app.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

/**
 * Service class for managing user addresses.
 *
 * Provides functionalities for creating, retrieving, updating, deleting,
 * and managing default shipping and billing addresses for users.
 * All operations include security checks to ensure users can only
 * manage their own data, unless they are an administrator.
 */
@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    /**
     * Creates a new address for a specified user.
     * If the new address is marked as a default, any existing default address of
     * the same type
     * for that user will be unset.
     *
     * @param userId      The ID of the user for whom the address is being created.
     * @param addressDto  The DTO containing the new address data.
     * @param currentUser The principal of the user making the request, for
     *                    authorization.
     * @return An AddressDto representing the newly created address.
     * @throws ResourceNotFoundException if the user does not exist.
     * @throws InvalidOperationException if the current user is not authorized to
     *                                   add an address for the target user.
     */
    @Transactional
    public AddressDto createAddress(Long userId, AddressDto addressDto, UserPrincipal currentUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        checkOwnershipOrAdmin(user.getId(), currentUser, "Cannot add address for another user.");

        Address address = addressDto.toEntity();
        address.setUser(user);

        // If this new address is set as a default, unset the old one.
        if (address.isDefaultShipping()) {
            addressRepository.findDefaultShippingByUserId(userId).ifPresent(oldDefault -> {
                oldDefault.setDefaultShipping(false);
                addressRepository.save(oldDefault);
            });
        }
        if (address.isDefaultBilling()) {
            addressRepository.findDefaultBillingByUserId(userId).ifPresent(oldDefault -> {
                oldDefault.setDefaultBilling(false);
                addressRepository.save(oldDefault);
            });
        }

        Address savedAddress = addressRepository.save(address);
        return AddressDto.toDto(savedAddress);
    }

    /**
     * Updates an existing address.
     *
     * @param addressId   The ID of the address to update.
     * @param addressDto  The DTO containing the updated address data.
     * @param currentUser The principal of the user making the request, for
     *                    authorization.
     * @return An AddressDto representing the updated address.
     * @throws ResourceNotFoundException if the address does not exist.
     * @throws InvalidOperationException if the current user is not authorized to
     *                                   update the address.
     */
    @Transactional
    public AddressDto updateAddress(Long addressId, AddressDto addressDto, UserPrincipal currentUser) {
        Address existingAddress = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        checkOwnershipOrAdmin(existingAddress.getUser().getId(), currentUser, "Cannot update address of another user.");

        // Update fields from DTO
        existingAddress.setAddressLine1(addressDto.addressLine1());
        existingAddress.setAddressLine2(addressDto.addressLine2());
        existingAddress.setCity(addressDto.city());
        existingAddress.setStateProvince(addressDto.stateProvince());
        existingAddress.setPostalCode(addressDto.postalCode());
        existingAddress.setCountry(addressDto.country());

        Address updatedAddress = addressRepository.save(existingAddress);
        return AddressDto.toDto(updatedAddress);
    }

    /**
     * Sets a specific address as the default shipping address for the current user.
     * This action will unset any other address that was previously the default
     * shipping address.
     *
     * @param addressId   The ID of the address to set as default.
     * @param currentUser The principal of the user making the request.
     * @return An AddressDto representing the updated address.
     * @throws ResourceNotFoundException if the address does not exist.
     * @throws InvalidOperationException if the address does not belong to the
     *                                   current user.
     */
    @Transactional
    public AddressDto setDefaultShippingAddress(Long addressId, UserPrincipal currentUser) {
        Address addressToSet = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        Long userId = currentUser.getId();
        checkOwnershipOrAdmin(addressToSet.getUser().getId(), currentUser,
                "Cannot set default address for another user.");

        // Unset the current default shipping address, if one exists and is not the same
        // address.
        addressRepository.findDefaultShippingByUserId(userId)
                .filter(oldDefault -> !oldDefault.getId().equals(addressId))
                .ifPresent(oldDefault -> {
                    oldDefault.setDefaultShipping(false);
                    addressRepository.save(oldDefault);
                });

        addressToSet.setDefaultShipping(true);
        Address savedAddress = addressRepository.save(addressToSet);
        return AddressDto.toDto(savedAddress);
    }

    /**
     * Sets a specific address as the default billing address for the current user.
     * This action will unset any other address that was previously the default
     * billing address.
     *
     * @param addressId   The ID of the address to set as default.
     * @param currentUser The principal of the user making the request.
     * @return An AddressDto representing the updated address.
     * @throws ResourceNotFoundException if the address does not exist.
     * @throws InvalidOperationException if the address does not belong to the
     *                                   current user.
     */
    @Transactional
    public AddressDto setDefaultBillingAddress(Long addressId, UserPrincipal currentUser) {
        Address addressToSet = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        Long userId = currentUser.getId();
        checkOwnershipOrAdmin(addressToSet.getUser().getId(), currentUser,
                "Cannot set default address for another user.");

        // Unset the current default billing address, if one exists and is not the same
        // address.
        addressRepository.findDefaultBillingByUserId(userId)
                .filter(oldDefault -> !oldDefault.getId().equals(addressId))
                .ifPresent(oldDefault -> {
                    oldDefault.setDefaultBilling(false);
                    addressRepository.save(oldDefault);
                });

        addressToSet.setDefaultBilling(true);
        Address savedAddress = addressRepository.save(addressToSet);
        return AddressDto.toDto(savedAddress);
    }

    /**
     * Finds all addresses associated with a specific user ID.
     *
     * @param userId      The ID of the user whose addresses are to be retrieved.
     * @param currentUser The principal of the user making the request, for
     *                    authorization.
     * @return A list of AddressDto objects.
     * @throws InvalidOperationException if the current user is not authorized to
     *                                   view the addresses.
     */
    @Transactional(readOnly = true)
    public List<AddressDto> findAddressesByUserId(Long userId, UserPrincipal currentUser) {
        checkOwnershipOrAdmin(userId, currentUser, "Cannot view addresses of another user.");
        return addressRepository.findByUserId(userId).stream()
                .map(AddressDto::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Deletes an address by its ID.
     *
     * @param addressId   The ID of the address to delete.
     * @param currentUser The principal of the user making the request, for
     *                    authorization.
     * @throws ResourceNotFoundException if the address does not exist.
     * @throws InvalidOperationException if the current user is not authorized to
     *                                   delete the address.
     */
    @Transactional
    public void deleteAddress(Long addressId, UserPrincipal currentUser) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        checkOwnershipOrAdmin(address.getUser().getId(), currentUser, "Cannot delete address of another user.");
        addressRepository.delete(address);
    }

    /**
     * A private helper method to centralize authorization logic.
     * Checks if the current user owns the resource or is an admin.
     *
     * @param ownerId      The ID of the resource's owner.
     * @param currentUser  The principal of the user making the request.
     * @param errorMessage The message for the exception if authorization fails.
     * @throws InvalidOperationException if authorization fails.
     */
    private void checkOwnershipOrAdmin(Long ownerId, UserPrincipal currentUser, String errorMessage) {
        boolean isOwner = Objects.equals(currentUser.getId(), ownerId);
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isOwner && !isAdmin) {
            throw new InvalidOperationException(errorMessage);
        }
    }
}