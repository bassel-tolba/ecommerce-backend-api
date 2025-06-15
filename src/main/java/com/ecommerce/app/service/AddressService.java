package com.ecommerce.app.service;

import com.ecommerce.app.dto.AddressDto;
import com.ecommerce.app.exception.InvalidOperationException;
import com.ecommerce.app.exception.ResourceNotFoundException;
import com.ecommerce.app.model.Address;
import com.ecommerce.app.model.User;
import com.ecommerce.app.repository.AddressRepository;
import com.ecommerce.app.repository.UserRepository;
import com.ecommerce.app.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Transactional
    public AddressDto createAddress(Long userId, AddressDto addressDto, UserPrincipal currentUser) {
        if (!Objects.equals(currentUser.getId(), userId) && !currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
             throw new InvalidOperationException("Cannot add address for another user.");
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        Address address = addressDto.toEntity();
        address.setUser(user);
        
        return AddressDto.toDto(addressRepository.save(address));
    }

    @Transactional(readOnly = true)
    public List<AddressDto> findAddressesByUserId(Long userId, UserPrincipal currentUser) {
        if (!Objects.equals(currentUser.getId(), userId) && !currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
             throw new InvalidOperationException("Cannot view addresses of another user.");
        }
        return addressRepository.findByUserId(userId).stream().map(AddressDto::toDto).collect(Collectors.toList());
    }

    @Transactional
    public void deleteAddress(Long addressId, UserPrincipal currentUser) {
        Address address = addressRepository.findById(addressId)
            .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        if (!Objects.equals(currentUser.getId(), address.getUser().getId()) && !currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new InvalidOperationException("Cannot delete address of another user.");
        }
        addressRepository.delete(address);
    }
}
