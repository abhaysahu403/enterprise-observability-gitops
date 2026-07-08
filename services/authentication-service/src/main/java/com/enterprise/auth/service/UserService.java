package com.enterprise.auth.service;

import com.enterprise.auth.dto.UpdateUserRequest;
import com.enterprise.auth.dto.UserResponse;
import com.enterprise.auth.entity.RoleType;
import com.enterprise.auth.entity.User;
import com.enterprise.auth.repository.UserRepository;
import com.enterprise.shared.dto.PageResponse;
import com.enterprise.shared.exception.BusinessException;
import com.enterprise.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(String search, Pageable pageable) {
        Page<User> page = (search == null || search.isBlank())
                ? userRepository.findAll(pageable)
                : userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search, pageable);
        return PageResponse.from(page.map(UserResponse::from));
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        return UserResponse.from(findUserOrThrow(id));
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = findUserOrThrow(id);

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getEmail() != null) {
            if (!request.getEmail().equalsIgnoreCase(user.getEmail())
                    && userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException("DUPLICATE_EMAIL", "Email already in use");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getEnabled() != null) user.setEnabled(request.getEnabled());
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            Set<RoleType> roles = request.getRoles().stream()
                    .map(r -> {
                        try {
                            return RoleType.valueOf(r.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            throw new BusinessException("INVALID_ROLE", "Unknown role: " + r);
                        }
                    })
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        }

        User saved = userRepository.save(user);
        log.info("User updated: id={} username={}", saved.getId(), saved.getUsername());
        return UserResponse.from(saved);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = findUserOrThrow(id);
        userRepository.delete(user);
        log.info("User deleted: id={} username={}", id, user.getUsername());
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }
}
