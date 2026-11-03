package com.example.elderhero_familiyelderly;

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

    public Advertisement() { }

    public String getAdvertisementID() { return AdvertisementID; }
    public String getCaretakerID() { return CaretakerID; }
    public String getCategoriesID() { return CategoriesID; }
    public List<String> getSubCategoriesID() { return SubCategoriesID; }
    public String getAdvertisementAvailability() { return AdvertisementAvailability; }
    public String getAdvertisementMinRate() { return AdvertisementMinRate; }
    public String getAdvertisementMaxRate() { return AdvertisementMaxRate; }
    public String getAdvertisementLocation() { return AdvertisementLocation; }
    public String getAdvertisementURL() { return AdvertisementURL; }
    public String getAdvertisementDate() { return AdvertisementDate; }
    public String getAdvertisementTime() { return AdvertisementTime; }

    public void setAdvertisementID(String advertisementID) { AdvertisementID = advertisementID; }
    public void setCaretakerID(String caretakerID) { CaretakerID = caretakerID; }
    public void setCategoriesID(String categoriesID) { CategoriesID = categoriesID; }
    public void setSubCategoriesID(List<String> subCategoriesID) { SubCategoriesID = subCategoriesID; }
    public void setAdvertisementAvailability(String advertisementAvailability) { AdvertisementAvailability = advertisementAvailability; }
    public void setAdvertisementMinRate(String advertisementMinRate) { AdvertisementMinRate = advertisementMinRate; }
    public void setAdvertisementMaxRate(String advertisementMaxRate) { AdvertisementMaxRate = advertisementMaxRate; }
    public void setAdvertisementLocation(String advertisementLocation) { AdvertisementLocation = advertisementLocation; }
    public void setAdvertisementURL(String advertisementURL) { AdvertisementURL = advertisementURL; }
    public void setAdvertisementDate(String advertisementDate) { AdvertisementDate = advertisementDate; }
    public void setAdvertisementTime(String advertisementTime) { AdvertisementTime = advertisementTime; }

    public String getDisplayRate() {
        if (AdvertisementMinRate != null && AdvertisementMaxRate != null &&
                !AdvertisementMinRate.isEmpty() && !AdvertisementMaxRate.isEmpty()) {
            return "RM" + AdvertisementMinRate + " - RM" + AdvertisementMaxRate + " / session";
        }
        if (AdvertisementMinRate != null && !AdvertisementMinRate.isEmpty()) {
            return "From RM" + AdvertisementMinRate + " / session";
        }
        return "Rate not specified";
    }
}
