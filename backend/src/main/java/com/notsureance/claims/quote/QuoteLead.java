package com.notsureance.claims.quote;

import com.notsureance.claims.common.BaseEntity;
import com.notsureance.claims.policy.EquipmentCategory;
import com.notsureance.claims.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "quote_leads")
public class QuoteLead extends BaseEntity {

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String email;

    private String phone;

    @Column(name = "state_code", nullable = false, length = 2)
    private String stateCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "equipment_category", nullable = false)
    private EquipmentCategory category;

    @Column(name = "equipment_value", nullable = false)
    private BigDecimal equipmentValue;

    @Column(name = "model_year", nullable = false)
    private int modelYear;

    @Column(nullable = false)
    private BigDecimal deductible;

    @Column(name = "annual_premium", nullable = false)
    private BigDecimal annualPremium;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadStatus status = LeadStatus.NEW;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_agent_id")
    private AppUser assignedAgent;

    protected QuoteLead() {
    }

    QuoteLead(QuoteDtos.LeadRequest request, BigDecimal annualPremium, AppUser assignedAgent) {
        this.fullName = request.fullName().trim();
        this.email = request.email().trim().toLowerCase();
        this.phone = request.phone() == null || request.phone().isBlank() ? null : request.phone().trim();
        this.stateCode = request.quote().stateCode();
        this.category = request.quote().category();
        this.equipmentValue = request.quote().equipmentValue();
        this.modelYear = request.quote().modelYear();
        this.deductible = request.quote().deductible();
        this.annualPremium = annualPremium;
        this.assignedAgent = assignedAgent;
    }

    void changeStatus(LeadStatus status) {
        this.status = status;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getStateCode() {
        return stateCode;
    }

    public EquipmentCategory getCategory() {
        return category;
    }

    public BigDecimal getEquipmentValue() {
        return equipmentValue;
    }

    public BigDecimal getAnnualPremium() {
        return annualPremium;
    }

    public LeadStatus getStatus() {
        return status;
    }

    public AppUser getAssignedAgent() {
        return assignedAgent;
    }
}
