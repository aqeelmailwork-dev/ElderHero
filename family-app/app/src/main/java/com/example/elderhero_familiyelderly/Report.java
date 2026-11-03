package com.example.elderhero_familiyelderly;

public class Report {

    public String ReportID;
    public String BookingID;
    public String ReportCategory;
    public String ReportDetails;
    public String ReportStatus;
    public long ReportTime;

    public String CaretakerID;
    public String FamiliyElderlyID;
    public String AdminID;

    public String ReporterID;
    public String ReporterRole;   // Caretaker | Family | Elderly

    public Report() {
        // Required empty constructor for Firebase
    }

    public Report(String reportID,
                  String bookingID,
                  String reportCategory,
                  String reportDetails,
                  String reportStatus,
                  long reportTime,
                  String caretakerID,
                  String familiyElderlyID,
                  String adminID,
                  String reporterID,
                  String reporterRole) {

        this.ReportID = reportID;
        this.BookingID = bookingID;
        this.ReportCategory = reportCategory;
        this.ReportDetails = reportDetails;
        this.ReportStatus = reportStatus;
        this.ReportTime = reportTime;
        this.CaretakerID = caretakerID;
        this.FamiliyElderlyID = familiyElderlyID;
        this.AdminID = adminID;
        this.ReporterID = reporterID;
        this.ReporterRole = reporterRole;
    }
}
