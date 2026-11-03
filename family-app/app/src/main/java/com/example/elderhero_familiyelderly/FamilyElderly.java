package com.example.elderhero_familiyelderly;

public class FamilyElderly {

    public String FamilyElderlyID;
    public String FamilyElderlyEmail;
    public String FamilyElderlyName;
    public String FamilyElderlyAge;
    public String FamilyElderlyGender;
    public String FamilyElderlyPhone;
    public String FamilyElderlyType;
    public String FamilyElderlyURL;

    public FamilyElderly() {}

    public FamilyElderly(String id, String email, String name, String age, String gender,
                         String phone, String type, String imageURL) {

        this.FamilyElderlyID = id;
        this.FamilyElderlyEmail = email;
        this.FamilyElderlyName = name;
        this.FamilyElderlyAge = age;
        this.FamilyElderlyGender = gender;
        this.FamilyElderlyPhone = phone;
        this.FamilyElderlyType = type;
        this.FamilyElderlyURL = imageURL;
    }
}
