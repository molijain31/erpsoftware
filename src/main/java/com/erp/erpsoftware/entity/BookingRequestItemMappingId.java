package com.erp.erpsoftware.entity;

import java.io.Serializable;
import java.util.Objects;

public class BookingRequestItemMappingId implements Serializable {

    private String bookingNo;
    private Integer itemId;

    public BookingRequestItemMappingId() {
    }

    public BookingRequestItemMappingId(String bookingNo, Integer itemId) {
        this.bookingNo = bookingNo;
        this.itemId = itemId;
    }

    public String getBookingNo() {
        return bookingNo;
    }

    public void setBookingNo(String bookingNo) {
        this.bookingNo = bookingNo;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookingRequestItemMappingId that = (BookingRequestItemMappingId) o;
        return Objects.equals(bookingNo, that.bookingNo) &&
                Objects.equals(itemId, that.itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookingNo, itemId);
    }
}
