package com.erp.erpsoftware.repository;

import com.erp.erpsoftware.entity.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    @Query("SELECT s FROM SalesOrder s ORDER BY s.orderId DESC")
    List<SalesOrder> findAllActive();

    @Query("SELECT s FROM SalesOrder s WHERE s.bookingNo = :bookingNo")
    List<SalesOrder> findByBookingNo(String bookingNo);

    @Query("SELECT s FROM SalesOrder s WHERE s.salesOrderNo = :salesOrderNo")
    List<SalesOrder> findBySalesOrderNo(@Param("salesOrderNo") String salesOrderNo);

    @Query("SELECT s FROM SalesOrder s WHERE s.orderId = :orderId")
    List<SalesOrder> findByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT s FROM SalesOrder s WHERE s.bookingNo = :b1 OR s.bookingNo = :b2 OR s.salesOrderNo = :b1 OR s.salesOrderNo = :b2")
    List<SalesOrder> findByBookingNoFlex(@Param("b1") String b1, @Param("b2") String b2);

    @Modifying
    @Query("UPDATE SalesOrder s SET s.receivedAmount = :amount, s.paymentStatus = :status WHERE s.bookingNo = :bookingNo")
    int updatePaymentDetails(@Param("bookingNo") String bookingNo, @Param("amount") Double amount, @Param("status") String status);

    @Modifying
    @Query("UPDATE SalesOrder s SET s.receivedAmount = :amount, s.paymentStatus = :status WHERE s.bookingNo = :b1 OR s.bookingNo = :b2 OR s.salesOrderNo = :b1 OR s.salesOrderNo = :b2")
    int updatePaymentDetailsFlex(@Param("b1") String b1, @Param("b2") String b2, @Param("amount") Double amount, @Param("status") String status);

    @Modifying
    @Query("UPDATE SalesOrder s SET s.receivedAmount = :amount, s.paymentStatus = :status WHERE s.supplierId = :supplierId")
    int updatePaymentDetailsBySupplierId(@Param("supplierId") Long supplierId, @Param("amount") Double amount, @Param("status") String status);
}
