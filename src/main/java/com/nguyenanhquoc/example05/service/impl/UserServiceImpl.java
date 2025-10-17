package com.nguyenanhquoc.example05.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.nguyenanhquoc.example05.config.AppConstants;
import com.nguyenanhquoc.example05.entity.Address;
import com.nguyenanhquoc.example05.entity.Role;
import com.nguyenanhquoc.example05.entity.User;
import com.nguyenanhquoc.example05.exceptions.APIException;
import com.nguyenanhquoc.example05.exceptions.ResourceNotFoundException;
import com.nguyenanhquoc.example05.payloads.AddressDTO;
import com.nguyenanhquoc.example05.payloads.UserDTO;
import com.nguyenanhquoc.example05.payloads.UserResponse;
import com.nguyenanhquoc.example05.repository.AddressRepo;
import com.nguyenanhquoc.example05.repository.RoleRepo;
import com.nguyenanhquoc.example05.repository.UserRepo;
import com.nguyenanhquoc.example05.service.UserService;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private RoleRepo roleRepo;

    @Autowired
    private AddressRepo addressRepo;

    // @Autowired
    // private CartService cartService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public UserDTO registerUser(UserDTO userDTO) {
        try {
            User user = modelMapper.map(userDTO, User.class);
            user.setPassword(passwordEncoder.encode(user.getPassword()));

            // // private Cart cart = new Cart();
            // // user.setCart(cart);

            Role role = roleRepo.findById(AppConstants.USER_ID).get();
            user.getRoles().add(role);

            if (userDTO.getAddress() != null) {
                String country = userDTO.getAddress().getCountry();
                String state = userDTO.getAddress().getState();
                String city = userDTO.getAddress().getCity();
                String pincode = userDTO.getAddress().getPincode();
                String street = userDTO.getAddress().getStreet();
                String buildingName = userDTO.getAddress().getBuildingName();
                
                Address address = addressRepo.findByCountryAndStateAndCityAndPincodeAndStreetAndBuildingName(country, state, city, pincode, street, buildingName);

                if (address == null) {
                    address = new Address(country, state, city, pincode, street, buildingName);
                    address = addressRepo.save(address);
                }
                
                user.setAddresses(List.of(address));
            }
            
            User registeredUser = userRepo.save(user);
            
            // // cart.setUser(registeredUser);
            
            userDTO = modelMapper.map(registeredUser, UserDTO.class);
            
            if (!registeredUser.getAddresses().isEmpty()) {
                userDTO.setAddress(modelMapper.map(registeredUser.getAddresses().stream().findFirst().get(), AddressDTO.class));
            }
            
            return userDTO;

        } catch (DataIntegrityViolationException e) {
            throw new APIException("User already exists with emailId: " + userDTO.getEmail());
        }
    }

    @Override
    public UserResponse getAllUsers(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sort = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sort);
        Page<User> pageUsers = userRepo.findAll(pageDetails);
        List<User> users = pageUsers.getContent();

        if (users.isEmpty()) {
            throw new APIException("No User exists!!!");
        }

        List<UserDTO> userDTOs = users.stream().map(user -> {
            UserDTO dto = modelMapper.map(user, UserDTO.class);
            if (!user.getAddresses().isEmpty()) {
                dto.setAddress(modelMapper.map(user.getAddresses().stream().findFirst().get(), AddressDTO.class));
            }
            // // CartDTO cart = modelMapper.map(user.getCart(), CartDTO.class);
            // // List<ProductDTO> products = user.getCart().getCartItems().stream().map(item -> modelMapper.map(item.getProduct(), ProductDTO.class)).collect(Collectors.toList());
            // // dto.setCart(cart);

            return dto;
        }).collect(Collectors.toList());

        UserResponse userResponse = new UserResponse();
        userResponse.setContent(userDTOs);
        userResponse.setPageNumber(pageUsers.getNumber());
        userResponse.setPageSize(pageUsers.getSize());
        userResponse.setTotalElements(pageUsers.getTotalElements());
        userResponse.setTotalPages(pageUsers.getTotalPages());
        userResponse.setLastPage(pageUsers.isLast());

        return userResponse;
    }

    @Override
    public UserDTO getUserById(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        UserDTO userDTO = modelMapper.map(user, UserDTO.class);
        
        if (!user.getAddresses().isEmpty()) {
            userDTO.setAddress(modelMapper.map(user.getAddresses().stream().findFirst().get(), AddressDTO.class));
        }

        // // Mapping for Cart and Products would go here
        
        return userDTO;
    }

    @Override
    public UserDTO updateUser(Long userId, UserDTO userDTO) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        String encodedPass = passwordEncoder.encode(userDTO.getPassword());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setMobileNumber(userDTO.getMobileNumber());
        user.setEmail(userDTO.getEmail());
        user.setPassword(encodedPass);

        if (userDTO.getAddress() != null) {
            String country = userDTO.getAddress().getCountry();
            String state = userDTO.getAddress().getState();
            String city = userDTO.getAddress().getCity();
            String pincode = userDTO.getAddress().getPincode();
            String street = userDTO.getAddress().getStreet();
            String buildingName = userDTO.getAddress().getBuildingName();
            
            Address address = addressRepo.findByCountryAndStateAndCityAndPincodeAndStreetAndBuildingName(country, state, city, pincode, street, buildingName);

            if (address == null) {
                address = new Address(country, state, city, pincode, street, buildingName);
                address = addressRepo.save(address);
            }
            
            user.setAddresses(List.of(address));
        }

        userDTO = modelMapper.map(user, UserDTO.class);
        if (!user.getAddresses().isEmpty()) {
            userDTO.setAddress(modelMapper.map(user.getAddresses().stream().findFirst().get(), AddressDTO.class));
        }
        
        // // Mapping for Cart and Products would go here

        return userDTO;
    }

    @Override
    public String deleteUser(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        // // Logic to handle cart deletion would go here

        userRepo.delete(user);
        return "User with userId " + userId + " deleted successfully!!!";
    }
}