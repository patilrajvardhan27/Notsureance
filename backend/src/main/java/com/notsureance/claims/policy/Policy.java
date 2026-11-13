package com.notsureance.claims.policy;

import com.notsureance.claims.common.BaseEntity;
import com.notsureance.claims.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "policies")
public class Policy extends BaseEntity {

    @Column(name = "policy_number", nullable = false, unique = true)
    private String policyNumber;

    @Column(name = "farm_name", nullable = false)
    private String farmName;

    @Column(name = "holder_name", nullable = false)
    private String holderName;

    @Column(name = "holder_email", nullable = false)
    private String holderEmail;

    @Column(name = "state_code", nullable = false, length = 2)
    private String stateCode;

    @Column(nullable = false)
    private String county;

    @Column(nullable = false)
    private int acreage;

    @Column(name = "coverage_limit", nullable = false)
    private BigDecimal coverageLimit;

    @Column(nullable = false)
    private BigDecimal deductible;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id")
    private AppUser agent;

    @Column(name = "holder_phone")
    private String holderPhone;

    @Column(name = "payout_account_hash")
    private String payoutAccountHash;

    @Column(name = "annual_premium")
    private BigDecimal annualPremium;

    protected Policy() {
    }

    public boolean isActiveOn(LocalDate date) {
        return !date.isBefore(effectiveDate) && !date.isAfter(expiryDate);
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public String getFarmName() {
        return farmName;
    }

    public String getHolderName() {
        return holderName;
    }

    public String getHolderEmail() {
        return holderEmail;
    }

    public String getStateCode() {
        return stateCode;
    }

    public String getCounty() {
        return county;
    }

    public int getAcreage() {
        return acreage;
    }

    public BigDecimal getCoverageLimit() {
        return coverageLimit;
    }

    public BigDecimal getDeductible() {
        return deductible;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public AppUser getAgent() {
        return agent;
    }

    public String getHolderPhone() {
        return holderPhone;
    }

    public String getPayoutAccountHash() {
        return payoutAccountHash;
    }

    public BigDecimal getAnnualPremium() {
        return annualPremium;
    }
}
