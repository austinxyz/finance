package com.finance.app.service;

import com.finance.app.dto.UserDTO;
import com.finance.app.model.User;
import com.finance.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Transactional
    public User createUser(User user) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists: " + user.getUsername());
        }
        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists: " + user.getEmail());
        }

        // 设置默认值
        if (user.getIsActive() == null) {
            user.setIsActive(true);
        }

        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, User userDetails) {
        User user = getUserById(id);

        // 检查用户名是否被其他用户使用
        if (!user.getUsername().equals(userDetails.getUsername())
            && userRepository.existsByUsername(userDetails.getUsername())) {
            throw new RuntimeException("Username already exists: " + userDetails.getUsername());
        }

        // 检查邮箱是否被其他用户使用
        if (!user.getEmail().equals(userDetails.getEmail())
            && userRepository.existsByEmail(userDetails.getEmail())) {
            throw new RuntimeException("Email already exists: " + userDetails.getEmail());
        }

        user.setUsername(userDetails.getUsername());
        user.setEmail(userDetails.getEmail());
        user.setFullName(userDetails.getFullName());
        user.setBirthDate(userDetails.getBirthDate());
        user.setAnnualIncome(userDetails.getAnnualIncome());
        user.setIncomeCurrency(userDetails.getIncomeCurrency());
        user.setRiskTolerance(userDetails.getRiskTolerance());
        user.setNotes(userDetails.getNotes());
        user.setIsActive(userDetails.getIsActive());

        // 只在提供新密码时更新
        if (userDetails.getPasswordHash() != null && !userDetails.getPasswordHash().isEmpty()) {
            user.setPasswordHash(userDetails.getPasswordHash());
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);
        // 软删除：标记为不活跃
        user.setIsActive(false);
        userRepository.save(user);
    }

    /**
     * 获取家庭成员列表
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getFamilyMembers(Long familyId) {
        List<User> users = userRepository.findByFamilyIdAndIsActiveTrue(familyId);
        return users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 将User实体转换为DTO
     */
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setFamilyId(user.getFamilyId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setBirthDate(user.getBirthDate());
        dto.setAnnualIncome(user.getAnnualIncome());
        dto.setIncomeCurrency(user.getIncomeCurrency());
        dto.setRiskTolerance(user.getRiskTolerance() != null ? user.getRiskTolerance().name() : null);
        dto.setNotes(user.getNotes());
        dto.setIsActive(user.getIsActive());
        return dto;
    }
}
