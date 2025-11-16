package com.finance.app.dto;

import com.finance.app.model.UserProfile;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserProfileDTO {
    private Long id;
    private Long userId;
    private BigDecimal estimatedAnnualExpenses;
    private Integer emergencyFundMonths;
    private String riskTolerance;
    private String notes;

    public static UserProfileDTO fromEntity(UserProfile profile) {
        if (profile == null) {
            return null;
        }
        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(profile.getId());
        dto.setUserId(profile.getUserId());
        dto.setEstimatedAnnualExpenses(profile.getEstimatedAnnualExpenses());
        dto.setEmergencyFundMonths(profile.getEmergencyFundMonths());
        dto.setRiskTolerance(profile.getRiskTolerance() != null ? profile.getRiskTolerance().name() : null);
        dto.setNotes(profile.getNotes());
        return dto;
    }
}
