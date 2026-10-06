package com.erp.erpsoftware;

import com.erp.erpsoftware.entity.Supplier;
import com.erp.erpsoftware.entity.SupplierItemMapping;
import com.erp.erpsoftware.entity.Item;
import com.erp.erpsoftware.entity.Type;
import com.erp.erpsoftware.entity.CurrentStock;
import com.erp.erpsoftware.repository.ItemRepository;
import com.erp.erpsoftware.repository.TypeRepository;
import com.erp.erpsoftware.repository.CurrentStockRepository;
import com.erp.erpsoftware.service.SupplierService;
import com.erp.erpsoftware.service.ItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.Date;

@SpringBootTest
@ActiveProfiles("test")
class ErpsoftwareApplicationTests {

	@Autowired
	private SupplierService supplierService;

	@Autowired
	private ItemRepository itemRepository;

	@Autowired
	private TypeRepository typeRepository;

	@Autowired
	private CurrentStockRepository currentStockRepository;

	@Autowired
	private ItemService itemService;

	@Test
	void contextLoads() {
	}

	@Test
	void testSaveItemAndStock() {
		try {
			Item item = new Item();
			item.setItemCode("TEST_CODE_" + System.currentTimeMillis());
			item.setItemName("TEST ITEM");
			item.setRate(10.0);
			item.setIsValid(1);
			
			System.out.println("DEBUG: Saving item...");
			Item saved = itemService.saveItem(item);
			System.out.println("DEBUG: Saved Item ID = " + saved.getItemId());
			
			System.out.println("DEBUG: Manually creating and saving stock...");
			CurrentStock stock = new CurrentStock();
			stock.setItemId(saved.getItemId());
			stock.setCurrentStock(50.0);
			currentStockRepository.save(stock);

			System.out.println("DEBUG: Checking current stock...");
			java.util.List<CurrentStock> stocks = currentStockRepository.findAll();
			stocks.forEach(s -> {
				System.out.println("DEBUG: Stock ID = " + s.getStockId() + ", Item = " + s.getItemId() + ", CurrentStock = " + s.getCurrentStock());
			});
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	@Test
	void testSaveSupplier() {
		// 1. Save Type
		Type type = new Type();
		type.setTypeName("Test Type");
		type.setIsValid(1);
		type = typeRepository.save(type);

		// 2. Save Item
		Item item = new Item();
		item.setItemCode("TEST-ITEM");
		item.setItemName("Test Item");
		item.setType(type);
		item.setRate(100.0);
		item = itemRepository.save(item);

		// 3. Save Supplier
		Supplier supplier = new Supplier();
		supplier.setSupplierName("Test Supplier");
		supplier.setContactPerson("Test Contact");
		supplier.setMobile("1234567890");
		supplier.setEmail("test@supplier.com");
		supplier.setAddress("Test Address");
		supplier.setGstNo("1234567890ABC");
		supplier.setCity("Test City");
		supplier.setState("Test State");
		supplier.setPincode(123456);

		// Create mapping referencing persisted item and type
		SupplierItemMapping mapping = new SupplierItemMapping();
		
		// Set item and type with populated IDs
		Item nestedItem = new Item();
		nestedItem.setItemId(item.getItemId());
		mapping.setItem(nestedItem);

		Type nestedType = new Type();
		nestedType.setTypeId(type.getTypeId());
		mapping.setType(nestedType);

		mapping.setRate(100.0);
		mapping.setFromDate(new Date());
		mapping.setUptoDate(new Date());

		supplier.getMappings().add(mapping);

		// Call save supplier
		supplierService.saveSupplier(supplier);
	}

	@Test
	void testSaveSupplierWithNullType() {
		// Save Item without Type
		Item item = new Item();
		item.setItemCode("TEST-ITEM2");
		item.setItemName("Test Item 2");
		item.setRate(100.0);
		item = itemRepository.save(item);

		Supplier supplier = new Supplier();
		supplier.setSupplierName("Test Supplier 2");
		supplier.setContactPerson("Test Contact 2");
		supplier.setMobile("1234567890");
		supplier.setPincode(123456);

		SupplierItemMapping mapping = new SupplierItemMapping();
		
		Item nestedItem = new Item();
		nestedItem.setItemId(item.getItemId());
		mapping.setItem(nestedItem);

		// type is initialized as new Type(), but its typeId is null
		mapping.setRate(100.0);
		mapping.setFromDate(new Date());
		mapping.setUptoDate(new Date());

		supplier.getMappings().add(mapping);

		// With the filter, this should save successfully and the invalid mapping is ignored
		Supplier saved = supplierService.saveSupplier(supplier);
		org.junit.jupiter.api.Assertions.assertNotNull(saved.getSupplierId());
		org.junit.jupiter.api.Assertions.assertTrue(saved.getMappings().isEmpty());
	}

	@Test
	void testEditSupplier() {
		// 1. Save Type
		Type type = new Type();
		type.setTypeName("Edit Type");
		type.setIsValid(1);
		type = typeRepository.save(type);

		// 2. Save Item
		Item item = new Item();
		item.setItemCode("EDIT-ITEM");
		item.setItemName("Edit Item");
		item.setType(type);
		item.setRate(100.0);
		item = itemRepository.save(item);

		// 3. Save Supplier with one mapping
		Supplier supplier = new Supplier();
		supplier.setSupplierName("Original Supplier");
		supplier.setContactPerson("Original Contact");
		supplier.setMobile("1112223333");
		supplier.setPincode(123456);

		SupplierItemMapping mapping = new SupplierItemMapping();
		Item nestedItem = new Item();
		nestedItem.setItemId(item.getItemId());
		mapping.setItem(nestedItem);

		Type nestedType = new Type();
		nestedType.setTypeId(type.getTypeId());
		mapping.setType(nestedType);

		mapping.setRate(100.0);
		mapping.setFromDate(new Date());
		mapping.setUptoDate(new Date());

		supplier.getMappings().add(mapping);

		Supplier saved = supplierService.saveSupplier(supplier);
		Long supplierId = saved.getSupplierId();
		Long mappingId = saved.getMappings().get(0).getMappingId();

		// 4. Simulate editing the supplier (e.g. updating rate and adding a new mapping)
		Supplier editSupplier = new Supplier();
		editSupplier.setSupplierId(supplierId);
		editSupplier.setSupplierName("Updated Supplier");
		editSupplier.setContactPerson("Original Contact");
		editSupplier.setMobile("1112223333");
		editSupplier.setPincode(123456);

		// Submitted mapping 1: existing updated mapping
		SupplierItemMapping submittedMapping1 = new SupplierItemMapping();
		submittedMapping1.setMappingId(mappingId);
		Item itemRef1 = new Item();
		itemRef1.setItemId(item.getItemId());
		submittedMapping1.setItem(itemRef1);
		Type typeRef1 = new Type();
		typeRef1.setTypeId(type.getTypeId());
		submittedMapping1.setType(typeRef1);
		submittedMapping1.setRate(200.0); // updated rate
		submittedMapping1.setFromDate(new Date());
		submittedMapping1.setUptoDate(new Date());
		editSupplier.getMappings().add(submittedMapping1);

		// Submitted mapping 2: new mapping
		SupplierItemMapping submittedMapping2 = new SupplierItemMapping();
		Item itemRef2 = new Item();
		itemRef2.setItemId(item.getItemId());
		submittedMapping2.setItem(itemRef2);
		Type typeRef2 = new Type();
		typeRef2.setTypeId(type.getTypeId());
		submittedMapping2.setType(typeRef2);
		submittedMapping2.setRate(300.0);
		submittedMapping2.setFromDate(new Date());
		submittedMapping2.setUptoDate(new Date());
		editSupplier.getMappings().add(submittedMapping2);

		Supplier updated = supplierService.saveSupplier(editSupplier);

		org.junit.jupiter.api.Assertions.assertEquals(supplierId, updated.getSupplierId());
		org.junit.jupiter.api.Assertions.assertEquals("Updated Supplier", updated.getSupplierName());
		org.junit.jupiter.api.Assertions.assertEquals(2, updated.getMappings().size());

		// Verify existing mapping rate was updated
		SupplierItemMapping updatedMapping = updated.getMappings().stream()
				.filter(m -> m.getMappingId().equals(mappingId))
				.findFirst()
				.orElseThrow();
		org.junit.jupiter.api.Assertions.assertEquals(200.0, updatedMapping.getRate());
	}

	@Autowired
	private javax.sql.DataSource dataSource;

	@Test
	void printDatabaseMetadata() {
		System.out.println("=== RUNNING BOOKING REQUEST CLEANUP ===");
		try (java.sql.Connection conn = dataSource.getConnection()) {
			try (java.sql.Statement stmt = conn.createStatement()) {
				int deleted = stmt.executeUpdate("DELETE FROM erp.booking_request");
				System.out.println("Deleted " + deleted + " rows from booking_request.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("=======================================");
	}

	@Autowired
	private com.erp.erpsoftware.repository.BookingRequestRepository bookingRequestRepository;

	@Test
	void testPrintBookingRequests() {
		System.out.println("=== PRINTING BOOKING REQUESTS FOR BR ===");
		java.util.List<com.erp.erpsoftware.entity.BookingRequest> requests = bookingRequestRepository.findAll();
		for (com.erp.erpsoftware.entity.BookingRequest req : requests) {
			System.out.println("BookingNo: [" + req.getBookingNo() +
					                   "], Status: " + req.getStatus() +
					                   ", SupplierName: " + req.getSupplierName());
		}
		System.out.println("========================================");
	}
}
