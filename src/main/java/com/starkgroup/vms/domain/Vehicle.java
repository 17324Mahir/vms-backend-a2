package com.starkgroup.vms.domain;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "vehicles", uniqueConstraints = {
    @UniqueConstraint(name = "uk_vehicles_vehicle_number", columnNames = "vehicle_number")
})
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_number", length = 64, nullable = false, unique = true)
    private String vehicleNumber;

    @Column(name = "company", length = 120, nullable = false)
    private String company;

    @Column(name = "type", length = 60, nullable = false)
    private String type;

    @Column(name = "brand", length = 80, nullable = false)
    private String brand;

    @Column(name = "model", length = 120, nullable = false)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private VehicleStatus status;

    public Vehicle() {
    }

    public Vehicle(String vehicleNumber, String company, String type, String brand, String model, VehicleStatus status) {
        this.vehicleNumber = vehicleNumber;
        this.company = company;
        this.type = type;
        this.brand = brand;
        this.model = model;
        this.status = status;
    }

    public Vehicle(Long id, String vehicleNumber, String company, String type, String brand, String model, VehicleStatus status) {
        this.id = id;
        this.vehicleNumber = vehicleNumber;
        this.company = company;
        this.type = type;
        this.brand = brand;
        this.model = model;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public void setStatus(VehicleStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vehicle vehicle = (Vehicle) o;
        return Objects.equals(id, vehicle.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Vehicle{" +
                "id=" + id +
                ", vehicleNumber='" + vehicleNumber + '\'' +
                ", company='" + company + '\'' +
                ", type='" + type + '\'' +
                ", brand='" + brand + '\'' +
                ", model='" + model + '\'' +
                ", status=" + status +
                '}';
    }
}
