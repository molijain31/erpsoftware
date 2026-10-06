package com.erp.erpsoftware.service;

import com.erp.erpsoftware.entity.BookingRequest;
import com.erp.erpsoftware.entity.BookingRequestItemMapping;
import com.erp.erpsoftware.repository.BookingRequestItemMappingRepository;
import com.erp.erpsoftware.repository.BookingRequestRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookingRequestService {

    @Autowired
    private BookingRequestRepository repository;

    @Autowired
    private BookingRequestItemMappingRepository itemMappingRepository;

    public List<BookingRequest> getAllBookingRequests() {
        return repository.findAll();
    }

    @Transactional
    public BookingRequest save(BookingRequest request) {
        return repository.saveAndFlush(request);
    }

    @Transactional
    public BookingRequestItemMapping saveItemMapping(BookingRequestItemMapping itemMapping) {
        return itemMappingRepository.saveAndFlush(itemMapping);
    }

    public List<BookingRequestItemMapping> getItemMappingsByBookingNo(String bookingNo) {
        return itemMappingRepository.findByBookingNo(bookingNo);
    }

    public BookingRequest getBookingRequestById(Long bookingId) {
        return repository.findById(bookingId).orElseThrow(() -> new IllegalArgumentException("BookingRequest not found with bookingId: " + bookingId));
    }

    public void deleteBookingRequest(Long bookingId) {
        repository.deleteById(bookingId);
    }

    public BookingRequest getRequestById(Long bookingId){
        return repository.findById(bookingId).orElse(null);
    }

    public List<BookingRequest> getRequestsByBookingNo(String bookingNo) {
        return repository.findByBookingNo(bookingNo);
    }
}
