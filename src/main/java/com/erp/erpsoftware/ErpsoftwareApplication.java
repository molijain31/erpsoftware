package com.erp.erpsoftware;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class ErpsoftwareApplication {

	public static void main(String[] args) {
		SpringApplication.run(ErpsoftwareApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(JdbcTemplate jdbcTemplate) {
		return args -> {
			try {
				jdbcTemplate.execute("ALTER TABLE erp.user_master DROP COLUMN IF EXISTS entry_date");
				System.out.println("Successfully dropped column 'entery_date' from erp.user_master");
			} catch (Exception e) {
				System.err.println("Failed to drop column: " + e.getMessage());
			}
			try {
				jdbcTemplate.execute("ALTER TABLE erp.current_stock DROP CONSTRAINT IF EXISTS fkpdcy7t2sfnir34o8q9282kqfq");
				System.out.println("Successfully dropped constraint 'fkpdcy7t2sfnir34o8q9282kqfq' from erp.current_stock");
			} catch (Exception e) {
				System.err.println("Failed to drop constraint: " + e.getMessage());
			}
			try {
				jdbcTemplate.execute("ALTER TABLE erp.booking_request DROP COLUMN IF EXISTS request_id");
				jdbcTemplate.execute("ALTER TABLE erp.booking_request DROP COLUMN IF EXISTS id");
				
				// Check if legacy 'book_id' column exists before querying it
				Integer colCount = 0;
				try {
					colCount = jdbcTemplate.queryForObject(
						"SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'booking_request' AND column_name = 'book_id'",
						Integer.class
					);
				} catch (Exception ignored) {}

				if (colCount != null && colCount > 0) {
					try {
						jdbcTemplate.execute(
							"INSERT INTO erp.booking_request_item_mapping (booking_no, item_id, quantity, status, entry_date, modified_date) " +
							"SELECT br.booking_no, br.book_id, COALESCE(br.quantity, 1), COALESCE(br.status, 'Pending'), COALESCE(br.entry_date, NOW()), NOW() " +
							"FROM erp.booking_request br " +
							"WHERE br.book_id IS NOT NULL " +
							"AND NOT EXISTS (" +
							"  SELECT 1 FROM erp.booking_request_item_mapping m " +
							"  WHERE m.booking_no = br.booking_no AND m.item_id = br.book_id" +
							")"
						);
					} catch (Exception ignored) {}
				}

				// Always safely drop the legacy columns if present
				jdbcTemplate.execute("ALTER TABLE erp.booking_request DROP COLUMN IF EXISTS book_id");
				jdbcTemplate.execute("ALTER TABLE erp.booking_request DROP COLUMN IF EXISTS quantity");
				jdbcTemplate.execute("ALTER TABLE erp.booking_request DROP COLUMN IF EXISTS rate");
				System.out.println("Successfully ensured legacy columns ('book_id', 'quantity', 'rate') are dropped from erp.booking_request");
			} catch (Exception e) {
				System.err.println("Failed to migrate/drop book_id columns from booking_request: " + e.getMessage());
			}
			try {
				jdbcTemplate.execute("ALTER TABLE erp.booking_request_item_mapping DROP COLUMN IF EXISTS request_id");
				jdbcTemplate.execute("ALTER TABLE erp.booking_request_item_mapping DROP COLUMN IF EXISTS book_id");
				jdbcTemplate.execute("ALTER TABLE erp.booking_request_item_mapping DROP COLUMN IF EXISTS mapping_id");
				jdbcTemplate.execute("ALTER TABLE erp.booking_request_item_mapping ADD COLUMN IF NOT EXISTS quantity INTEGER DEFAULT 1");
				System.out.println("Successfully updated columns on erp.booking_request_item_mapping");
			} catch (Exception e) {
				System.err.println("Failed to drop/add columns from booking_request_item_mapping: " + e.getMessage());
			}
			try {
				jdbcTemplate.execute("UPDATE erp.booking_request_item_mapping SET entry_date = CURRENT_TIMESTAMP WHERE entry_date IS NULL");
				jdbcTemplate.execute("UPDATE erp.booking_request_item_mapping SET modified_date = CURRENT_TIMESTAMP WHERE modified_date IS NULL");
			} catch (Exception ignored) {}
			try {
				jdbcTemplate.execute("ALTER TABLE erp.booking_request DROP COLUMN IF EXISTS supplier_name");
			} catch (Exception ignored) {}
			try {
				jdbcTemplate.execute("ALTER TABLE erp.account_paypal DROP COLUMN IF EXISTS paypal_email");
			} catch (Exception ignored) {}
			try {
				jdbcTemplate.execute("ALTER TABLE erp.supplier_account_paypal DROP COLUMN IF EXISTS paypal_email");
			} catch (Exception ignored) {}
			try {
				jdbcTemplate.execute("UPDATE erp.booking_request SET status = 'Pending' WHERE status IS NULL OR status IN ('Received', 'Fully Received', 'create', 'Create')");
				jdbcTemplate.execute("UPDATE erp.booking_request_item_mapping SET status = 'Pending' WHERE status IS NULL OR status IN ('Received', 'Fully Received', 'create', 'Create')");
				System.out.println("Successfully cleaned up invalid statuses in booking_request tables to 'Pending'");
			} catch (Exception e) {
				System.err.println("Failed to clean up status in booking_request tables: " + e.getMessage());
			}
			try {
				// Backfill entry_date from modified_date where entry_date is NULL (for existing records)
				jdbcTemplate.execute("UPDATE erp.booking_request SET entry_date = COALESCE(modified_date, CURRENT_TIMESTAMP) WHERE entry_date IS NULL");
				System.out.println("Successfully backfilled entry_date in erp.booking_request");
			} catch (Exception e) {
				System.err.println("Failed to backfill entry_date in booking_request: " + e.getMessage());
			}
			try {
				jdbcTemplate.execute("ALTER TABLE erp.stock_ledger ADD COLUMN IF NOT EXISTS balance DOUBLE PRECISION DEFAULT 0.0");
				System.out.println("Successfully ensured 'balance' column exists in erp.stock_ledger");
			} catch (Exception e) {
				try {
					jdbcTemplate.execute("ALTER TABLE erp.stock_ledger ADD COLUMN balance DOUBLE DEFAULT 0.0");
				} catch (Exception ex) {
					System.err.println("Note on stock_ledger balance column: " + ex.getMessage());
				}
			}
			try {
				jdbcTemplate.execute(
					"INSERT INTO erp.sales_order (sales_order_no, booking_no, booking_id, supplier_id, received_amount, payment_status, total_cost, total_quantity, status, entry_date, modified_date) " +
					"SELECT CONCAT('CO-', LPAD(CAST(COALESCE(MIN(b.booking_id), 1) AS VARCHAR(20)), 5, '0')), " +
					"b.booking_no, CAST(MIN(b.booking_id) AS VARCHAR(50)), CAST(MAX(b.supplier_id) AS BIGINT), 0.0, '1 - Unpaid', " +
					"COALESCE(MAX(b.total_cost), 0.0), COALESCE(MAX(b.total_quantity), 0), 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP " +
					"FROM erp.booking_request b " +
					"WHERE b.booking_no IS NOT NULL AND b.booking_no != '' " +
					"AND NOT EXISTS (SELECT 1 FROM erp.sales_order s WHERE s.booking_no = b.booking_no OR s.booking_no = REPLACE(b.booking_no, 'BR-', '')) " +
					"GROUP BY b.booking_no"
				);
				jdbcTemplate.execute("UPDATE erp.sales_order SET sales_order_no = CONCAT('CO-', LPAD(CAST(salesorder_id AS VARCHAR(20)), 5, '0')) WHERE (sales_order_no IS NULL OR sales_order_no = '' OR sales_order_no LIKE 'SO-%')");
				jdbcTemplate.execute("UPDATE erp.sales_order SET received_amount = 0.0 WHERE received_amount IS NULL");
				jdbcTemplate.execute("UPDATE erp.sales_order SET payment_status = '1 - Unpaid' WHERE payment_status IS NULL OR payment_status = ''");
			} catch (Exception e) {
				System.err.println("Note on sales_order payment cleanup: " + e.getMessage());
			}
			try {
				jdbcTemplate.execute("ALTER TABLE erp.purchase_order_mapping ADD COLUMN IF NOT EXISTS vendor_id BIGINT");
				jdbcTemplate.execute("ALTER TABLE erp.purchase_order_mapping ADD COLUMN IF NOT EXISTS vendor_name VARCHAR(255)");
				jdbcTemplate.execute("ALTER TABLE erp.account_ledger ADD COLUMN IF NOT EXISTS paid_date TIMESTAMP");
			} catch (Exception ignored) {}
			try {
				jdbcTemplate.execute(
					"CREATE TABLE IF NOT EXISTS erp.account_ledger (" +
					"  ledger_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, " +
					"  entity_type VARCHAR(20) NOT NULL, " +
					"  supplier_id BIGINT, " +
					"  vendor_id BIGINT, " +
					"  booking_no VARCHAR(100), " +
					"  account_detail VARCHAR(255), " +
					"  total_cost DOUBLE PRECISION DEFAULT 0.0, " +
					"  payment_amount DOUBLE PRECISION DEFAULT 0.0, " +
					"  payment_status VARCHAR(50) DEFAULT '1 - Unpaid', " +
					"  status_code INTEGER DEFAULT 1, " +
					"  is_valid INTEGER DEFAULT 1, " +
					"  entry_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
					"  modified_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
					")"
				);
				System.out.println("Successfully ensured erp.account_ledger table exists in database");

				// Initial backfill/sync from sales_order into account_ledger for suppliers
				jdbcTemplate.execute(
					"INSERT INTO erp.account_ledger (entity_type, supplier_id, booking_no, account_detail, total_cost, payment_amount, payment_status, status_code, is_valid, entry_date, modified_date) " +
					"SELECT 'SUPPLIER', s.supplier_id, s.booking_no, COALESCE(sup.supplier_name, CONCAT('Supplier #', CAST(s.supplier_id AS VARCHAR(20)))), COALESCE(s.total_cost, 0.0), COALESCE(s.received_amount, 0.0), COALESCE(s.payment_status, '1 - Unpaid'), " +
					"CASE WHEN COALESCE(s.received_amount, 0.0) >= COALESCE(s.total_cost, 0.0) - 0.01 AND COALESCE(s.total_cost, 0.0) > 0 THEN 3 WHEN COALESCE(s.received_amount, 0.0) > 0 THEN 2 ELSE 1 END, 1, COALESCE(s.entry_date, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP " +
					"FROM erp.sales_order s " +
					"LEFT JOIN erp.supplier_master sup ON s.supplier_id = sup.supplier_id " +
					"WHERE s.booking_no IS NOT NULL AND NOT EXISTS (SELECT 1 FROM erp.account_ledger al WHERE al.entity_type = 'SUPPLIER' AND al.booking_no = s.booking_no)"
				);

				// Initial backfill/sync from purchase_order into account_ledger for vendors
				jdbcTemplate.execute(
					"INSERT INTO erp.account_ledger (entity_type, vendor_id, booking_no, account_detail, total_cost, payment_amount, payment_status, status_code, is_valid, entry_date, modified_date) " +
					"SELECT 'VENDOR', p.vendor_id, COALESCE(p.purchase_order_no, CONCAT('PO-', CAST(p.order_id AS VARCHAR(20)))), COALESCE(v.vendor_name, CONCAT('Vendor #', CAST(p.vendor_id AS VARCHAR(20)))), COALESCE(p.total_cost, 0.0), COALESCE(p.payment_amount, 0.0), COALESCE(p.payment_status, '1 - Unpaid'), " +
					"CASE WHEN COALESCE(p.payment_amount, 0.0) >= COALESCE(p.total_cost, 0.0) - 0.01 AND COALESCE(p.total_cost, 0.0) > 0 THEN 3 WHEN COALESCE(p.payment_amount, 0.0) > 0 THEN 2 ELSE 1 END, 1, COALESCE(p.entry_date, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP " +
					"FROM erp.purchase_order p " +
					"LEFT JOIN erp.vendor_master v ON p.vendor_id = v.vendor_id " +
					"WHERE p.order_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM erp.account_ledger al WHERE al.entity_type = 'VENDOR' AND al.booking_no = COALESCE(p.purchase_order_no, CONCAT('PO-', CAST(p.order_id AS VARCHAR(20)))))"
				);
				System.out.println("Successfully synced supplier and vendor records into erp.account_ledger table");
			} catch (Exception e) {
				System.err.println("Note on account_ledger table initialization: " + e.getMessage());
			}
		};
	}
}
