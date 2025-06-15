package com.ecommerce.app.service;

import com.ecommerce.app.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("addressSecurityService") // Bean name for use in @PreAuthorize
@RequiredArgsConstructor
public class SecurityHelperService {
    
    private final AddressRepository addressRepository;

    public boolean isOwner(Long addressId, Long userId) {
        return addressRepository.findById(addressId)
            .map(address -> address.getUser().getId().equals(userId))
            .orElse(false);
    }
}
