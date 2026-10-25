/* ============================================================================
   UTETra - Kiem tra database da luu duoc tieng Viet chua
   ----------------------------------------------------------------------------
   Chay file nay SAU KHI da chay lai ung dung Spring Boot it nhat mot lan
   (de Hibernate tao bang va DataSeeder nap du lieu mau).

   Ket qua mong doi:
     Muc 1: cot varchar = 0, cot nvarchar > 0
     Muc 2: dong chu hien thi day du dau, khong co dau hoi
     Muc 3: du lieu mau co du 6 tai khoan, 2 shop, 12 san pham

   LUU Y: hay chay file nay trong SQL Server Management Studio. Neu chay bang
   cua so dong lenh (sqlcmd) thi chu tieng Viet o cot chuoi_doc_ra co the hien
   thi loi phong - do la loi cua cua so dong lenh, khong phai loi du lieu.
   Cu nhin cot ket_luan la biet dung hay sai.
   ============================================================================ */

USE utetra;
GO

PRINT '--- Muc 1: kiem tra kieu cot chuoi ---';
SELECT DATA_TYPE AS kieu_cot, COUNT(*) AS so_cot
FROM INFORMATION_SCHEMA.COLUMNS
WHERE DATA_TYPE IN ('varchar', 'nvarchar')
GROUP BY DATA_TYPE;
GO

PRINT '--- Muc 2: thu ghi va doc lai mot chuoi tieng Viet ---';
/* Chen tam mot danh muc co dau, doc lai roi xoa di */
INSERT INTO categories (name, slug, active, created_at, updated_at)
VALUES (N'Sinh tố trái cây ĐẶC BIỆT', 'kiem-tra-unicode', 0, SYSDATETIME(), SYSDATETIME());

SELECT name AS chuoi_doc_ra,
       CASE WHEN name = N'Sinh tố trái cây ĐẶC BIỆT'
            THEN 'DUNG - database luu duoc tieng Viet'
            ELSE 'SAI - van con mat dau, hay chay lai tao-lai-database.sql'
       END AS ket_luan
FROM categories WHERE slug = 'kiem-tra-unicode';

DELETE FROM categories WHERE slug = 'kiem-tra-unicode';
GO

PRINT '--- Muc 3: du lieu mau ---';
SELECT 'users' AS bang, COUNT(*) AS so_dong FROM users
UNION ALL SELECT 'shops', COUNT(*) FROM shops
UNION ALL SELECT 'categories', COUNT(*) FROM categories
UNION ALL SELECT 'products', COUNT(*) FROM products
UNION ALL SELECT 'carriers', COUNT(*) FROM carriers;
GO
