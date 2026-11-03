package com.example.elderhero_caretakers.booking;

public class Booking {

    public String BookingID;
    public String CaretakerID;
    public String FamiliyElderlyID;
    public String AdvertismentID;

    public String BookingDate;
    public String BookingTime;
    public String BookingDuration;
    public String BookingNote;
    public String BookingStatus;
    public long BookingTimeStamp;

    public Booking() {}

    public Booking(String bookingID, String caretakerID, String familiyElderlyID, String advertismentID,
                   String bookingDate, String bookingTime, String bookingDuration, String bookingNote,
                   String bookingStatus, long bookingTimeStamp) {

        this.BookingID = bookingID;
        this.CaretakerID = caretakerID;
        this.FamiliyElderlyID = familiyElderlyID;
        this.AdvertismentID = advertismentID;
        this.BookingDate = bookingDate;
        this.BookingTime = bookingTime;
        this.BookingDuration = bookingDuration; // Initialize
        this.BookingNote = bookingNote;
        this.BookingStatus = bookingStatus;
        this.BookingTimeStamp = bookingTimeStamp;
    }
}