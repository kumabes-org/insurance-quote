package br.com.kumabe.quotes.domain.model;

import lombok.*;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
public class Smartphone extends Product {
    private Integer batteryCapacity;
    private String cameraSpecifications;
    private String memory;
    private String storageCapacity;
    private String brand;
    private String modelVersion;
    private String operatingSystem;
}
