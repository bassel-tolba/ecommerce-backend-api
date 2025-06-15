package com.ecommerce.app.controller;

import com.ecommerce.app.dto.AddressDto;
import com.ecommerce.app.security.UserPrincipal;
import com.ecommerce.app.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    @PreAuthorize("#userId == principal.id or hasRole('ADMIN')")
    public ResponseEntity<AddressDto> createAddress(@PathVariable Long userId, @RequestBody AddressDto addressDto, @AuthenticationPrincipal UserPrincipal principal) {
        AddressDto createdAddress = addressService.createAddress(userId, addressDto, principal);
        return new ResponseEntity<>(createdAddress, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("#userId == principal.id or hasRole('ADMIN')")
    public ResponseEntity<List<AddressDto>> getUserAddresses(@PathVariable Long userId, @AuthenticationPrincipal UserPrincipal principal) {
        List<AddressDto> addresses = addressService.findAddressesByUserId(userId, principal);
        return ResponseEntity.ok(addresses);
    }
    
    @DeleteMapping("/{addressId}")
    @PreAuthorize("hasRole('ADMIN') or @addressService.isOwner(#addressId, principal.id)")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long userId, @PathVariable Long addressId, @AuthenticationPrincipal UserPrincipal principal) {
        // The @PreAuthorize annotation handles most security, but we can pass the principal for service-level checks too.
        addressService.deleteAddress(addressId, principal);
        return ResponseEntity.noContent().build();
    }
}
