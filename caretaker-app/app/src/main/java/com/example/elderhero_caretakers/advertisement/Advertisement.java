package com.example.elderhero_caretakers.advertisement;

import java.util.List;

public class Advertisement {

    private String AdvertisementID;
    private String CaretakerID;
    private String CategoriesID;
    private List<String> SubCategoriesID;
    private String AdvertisementAvailability;
    private String AdvertisementMinRate;
    private String AdvertisementMaxRate;
    private String AdvertisementLocation;
    private String AdvertisementURL;
    private String AdvertisementDate;
    private String AdvertisementTime;

    public Advertisement() {
        // Required empty constructor for Firebase
    }

    public Advertisement(String advertisementID,
                         String caretakerID,
                         String categoriesID,
                         List<String> subCategoriesID,
                         String advertisementAvailability,
                         String advertisementMinRate,
                         String advertisementMaxRate,
                         String advertisementLocation,
                         String advertisementURL,
                         String advertisementDate,
                         String advertisementTime) {

        this.AdvertisementID = advertisementID;
        this.CaretakerID = caretakerID;
        this.CategoriesID = categoriesID;
        this.SubCategoriesID = subCategoriesID;
        this.AdvertisementAvailability = advertisementAvailability;
        this.AdvertisementMinRate = advertisementMinRate;
        this.AdvertisementMaxRate = advertisementMaxRate;
        this.AdvertisementLocation = advertisementLocation;
        this.AdvertisementURL = advertisementURL;
        this.AdvertisementDate = advertisementDate;
        this.AdvertisementTime = advertisementTime;
    }

    // Getters
    public String getAdvertisementID() {
        return AdvertisementID;
    }
    public String getCaretakerID() {
        return CaretakerID;
    }
    public String getCategoriesID() {
        return CategoriesID;
    }
    public List<String> getSubCategoriesID() {
        return SubCategoriesID;
    }
    public String getAdvertisementAvailability() {
        return AdvertisementAvailability;
    }
    public String getAdvertisementMinRate() {
        return AdvertisementMinRate;
    }
    public String getAdvertisementMaxRate() {
        return AdvertisementMaxRate;
    }
    public String getAdvertisementLocation() {
        return AdvertisementLocation;
    }
    public String getAdvertisementURL() {
        return AdvertisementURL;
    }
    public String getAdvertisementDate() {
        return AdvertisementDate;
    }
    public String getAdvertisementTime() {
        return AdvertisementTime;
    }

    // Setters
    public void setAdvertisementID(String advertisementID) {
        this.AdvertisementID = advertisementID;
    }
    public void setCaretakerID(String caretakerID) {
        this.CaretakerID = caretakerID;
    }
    public void setCategoriesID(String categoriesID) {
        this.CategoriesID = categoriesID;
    }
    public void setSubCategoriesID(List<String> subCategoriesID) {
        this.SubCategoriesID = subCategoriesID;
    }
    public void setAdvertisementAvailability(String advertisementAvailability) {
        this.AdvertisementAvailability = advertisementAvailability;
    }
    public void setAdvertisementMinRate(String advertisementMinRate) {
        this.AdvertisementMinRate = advertisementMinRate;
    }
    public void setAdvertisementMaxRate(String advertisementMaxRate) {
        this.AdvertisementMaxRate = advertisementMaxRate;
    }
    public void setAdvertisementLocation(String advertisementLocation) {
        this.AdvertisementLocation = advertisementLocation;
    }
    public void setAdvertisementURL(String advertisementURL) {
        this.AdvertisementURL = advertisementURL;
    }
    public void setAdvertisementDate(String advertisementDate) {
        this.AdvertisementDate = advertisementDate;
    }
    public void setAdvertisementTime(String advertisementTime) {
        this.AdvertisementTime = advertisementTime;
    }

    // Rate formatting helper
    public String getDisplayRate() {
        if (AdvertisementMinRate != null &&
                AdvertisementMaxRate != null &&
                !AdvertisementMinRate.isEmpty() &&
                !AdvertisementMaxRate.isEmpty()) {
            return "RM" + AdvertisementMinRate + " - RM" + AdvertisementMaxRate + " / hour";
        }
        if (AdvertisementMinRate != null && !AdvertisementMinRate.isEmpty()) {
            return "From RM" + AdvertisementMinRate + " / hour";
        }
        return "Rate not specified";
    }
}
