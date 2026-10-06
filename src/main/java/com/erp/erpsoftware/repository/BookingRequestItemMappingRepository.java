package com.erp.erpsoftware.repository;

import com.erp.erpsoftware.entity.BookingRequestItemMapping;
import com.erp.erpsoftware.entity.BookingRequestItemMappingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRequestItemMappingRepository extends JpaRepository<BookingRequestItemMapping, BookingRequestItemMappingId> {

    List<BookingRequestItemMapping> findByBookingNo(String bookingNo);

    List<BookingRequestItemMapping> findByItemId(Integer itemId);
}
