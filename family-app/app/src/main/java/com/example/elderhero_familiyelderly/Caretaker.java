package com.example.elderhero_familiyelderly;

import java.util.List;

public class Caretaker {

    public String CaretakerID;
    public String CaretakerName;
    public String CaretakerEmail;
    public String CaretakerGender;
    public String CaretakerRace;
    public String CaretakerAge;

    // MUST BE LIST<String> to match Firebase
    public List<String> CaretakerLanguage;

    public String CaretakerPhoneNo;
    public String CaretakerExperience;
    public String CaretakerProfession;
    public String CaretakerProfileUrl;

    public Caretaker() {
        // Required empty constructor for Firebase
    }
}
