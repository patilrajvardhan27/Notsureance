package com.notsureance.claims.policy;

import com.notsureance.claims.common.BaseEntity;
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
@Table(name = "equipment")
public class Equipment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id")
    private Policy policy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentCategory category;

    @Column(nullable = false)
    private String make;

    @Column(nullable = false)
    private String model;

    @Column(name = "model_year", nullable = false)
    private int modelYear;

    @Column(name = "serial_number", nullable = false, unique = true)
    private String serialNumber;

    @Column(name = "insured_value", nullable = false)
    private BigDecimal insuredValue;

    @Column(name = "engine_hours", nullable = false)
    private int engineHours;

    @Column(name = "gps_tracked", nullable = false)
    private boolean gpsTracked;

    protected Equipment() {
    }

    public String displayName() {
        return modelYear + " " + make + " " + model;
    }

    public Policy getPolicy() {
        return policy;
    }

    public EquipmentCategory getCategory() {
        return category;
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public int getModelYear() {
        return modelYear;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public BigDecimal getInsuredValue() {
        return insuredValue;
    }

    public int getEngineHours() {
        return engineHours;
    }

    public boolean isGpsTracked() {
        return gpsTracked;
    }
}
