package com.erp.erpsoftware.repository;

import com.erp.erpsoftware.entity.BookingRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookingRequestRepository extends JpaRepository<BookingRequest, Long> {
    List<BookingRequest> findByBookingNo(String bookingNo);

    default int updatePaymentDetails(String bookingNo, Double amount, String status) {
        return 0;
    }
}
