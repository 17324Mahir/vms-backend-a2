package com.starkgroup.vms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "vehicle_maintenance_records")
public class MaintenanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "maintenance_date", nullable = false)
    private LocalDate maintenanceDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "maintenance_type", length = 60, nullable = false)
    private MaintenanceType maintenanceType;

    @Column(name = "cost", precision = 12, scale = 2, nullable = false)
    private BigDecimal cost = BigDecimal.ZERO;

    @Column(name = "workshop_vendor", length = 160, nullable = false)
    private String workshopVendor;

    @Column(name = "odometer_reading")
    private Integer odometerReading;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public MaintenanceRecord() {
    }

    public MaintenanceRecord(Vehicle vehicle, LocalDate maintenanceDate, MaintenanceType maintenanceType,
                             BigDecimal cost, String workshopVendor, Integer odometerReading, String notes) {
        this.vehicle = vehicle;
        this.maintenanceDate = maintenanceDate;
        this.maintenanceType = maintenanceType;
        this.cost = cost;
        this.workshopVendor = workshopVendor;
        this.odometerReading = odometerReading;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public LocalDate getMaintenanceDate() {
        return maintenanceDate;
    }

    public void setMaintenanceDate(LocalDate maintenanceDate) {
        this.maintenanceDate = maintenanceDate;
    }

    public MaintenanceType getMaintenanceType() {
        return maintenanceType;
    }

    public void setMaintenanceType(MaintenanceType maintenanceType) {
        this.maintenanceType = maintenanceType;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public String getWorkshopVendor() {
        return workshopVendor;
    }

    public void setWorkshopVendor(String workshopVendor) {
        this.workshopVendor = workshopVendor;
    }

    public Integer getOdometerReading() {
        return odometerReading;
    }

    public void setOdometerReading(Integer odometerReading) {
        this.odometerReading = odometerReading;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MaintenanceRecord that = (MaintenanceRecord) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

