package com.example.lostfound.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class ItemCreateRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title length must be <= 100")
    private String title;

    @Size(max = 1000, message = "Description length must be <= 1000")
    private String description;

    @Size(max = 4000, message = "Image URL length must be <= 4000")
    private String imageUrls;

    @Size(max = 50, message = "Category length must be <= 50")
    private String category;

    @Size(max = 120, message = "Location length must be <= 120")
    private String location;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lostTime;

    @Size(max = 120, message = "Contact length must be <= 120")
    private String contact;

    @NotBlank(message = "Item type is required")
    private String itemType;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(String imageUrls) {
        this.imageUrls = imageUrls;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getLostTime() {
        return lostTime;
    }

    public void setLostTime(LocalDateTime lostTime) {
        this.lostTime = lostTime;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }
}
