package com.example.elderhero_caretakers;

import java.util.List;

public class Caretaker {

    public String CaretakerID;
    public String CaretakerName;
    public String CaretakerEmail;
    public String CaretakerPhoneNo;
    public String CaretakerProfession;
    public String CaretakerExperience;
    public String CaretakerProfileUrl;
    public String CaretakerRace;
    public List<String> CaretakerLanguage;
    public String CaretakerGender;
    public String CaretakerAge;

    public Caretaker() {}

    public Caretaker(String caretakerID, String caretakerName, String caretakerEmail,
                     String caretakerPhoneNo, String caretakerProfession, String caretakerExperience,
                     String caretakerProfileUrl, String caretakerRace, List<String> caretakerLanguage,
                     String caretakerGender, String caretakerAge) {

        this.CaretakerID = caretakerID;
        this.CaretakerName = caretakerName;
        this.CaretakerEmail = caretakerEmail;
        this.CaretakerPhoneNo = caretakerPhoneNo;
        this.CaretakerProfession = caretakerProfession;
        this.CaretakerExperience = caretakerExperience;
        this.CaretakerProfileUrl = caretakerProfileUrl;
        this.CaretakerRace = caretakerRace;
        this.CaretakerLanguage = caretakerLanguage;
        this.CaretakerGender = caretakerGender;
        this.CaretakerAge = caretakerAge;
    }
}
