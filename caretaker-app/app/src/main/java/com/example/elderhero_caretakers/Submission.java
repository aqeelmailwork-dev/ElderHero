package com.example.elderhero_caretakers;

public class Submission {
    public String SubmissionID;
    public String CaretakerID;
    public String SubmissionURL;
    public String SubmissionStatus;
    public String SubmissionDate;
    public String SubmissionTime;
    public String SubmissionRemark;
    public String AdminID;

    public Submission() { }

    public Submission(String submissionID, String caretakerID, String submissionURL,
                      String submissionStatus, String submissionDate, String submissionTime,
                      String submissionRemark, String adminID) {
        this.SubmissionID = submissionID;
        this.CaretakerID = caretakerID;
        this.SubmissionURL = submissionURL;
        this.SubmissionStatus = submissionStatus;
        this.SubmissionDate = submissionDate;
        this.SubmissionTime = submissionTime;
        this.SubmissionRemark = submissionRemark;
        this.AdminID = adminID;
    }
}
