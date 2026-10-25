/* ============================================================================
   UTETra - Tao lai database de sua loi mat dau tieng Viet
   ----------------------------------------------------------------------------
   AI CAN CHAY: ai da tao database utetra TRUOC ngay 22/10/2026.
   AI KHONG CAN: ai tao database lan dau sau ngay do.

   VI SAO PHAI CHAY:
   Truoc day Hibernate tao cot kieu varchar (khong phai Unicode) tren SQL Server.
   Ky tu tieng Viet nam ngoai bang Latin-1 bi luu thanh dau hoi:

       nhap vao: Sinh to trai cay DAC BIET
       luu thanh: Sinh t? trai cay D?C BI?T

   Tu nay application.properties da bat:
       spring.jpa.properties.hibernate.use_nationalized_character_data=true
   nen cot moi se duoc tao kieu nvarchar. Nhung ddl-auto=update KHONG doi kieu
   cot da ton tai, nen phai xoa database cu va de app tao lai tu dau.

   MAT GI KHONG: chi mat du lieu thu nghiem. Lan chay sau DataSeeder tao lai
   6 tai khoan mau, 2 shop, 4 danh muc, 12 san pham, topping va nha van chuyen.
   Don hang hay shop ban tu tao de test thi se mat, tao lai sau vai phut.

   CACH CHAY: mo SQL Server Management Studio, dan toan bo file nay vao
   cua so New Query roi bam Execute (F5). Nho TAT ung dung Spring Boot truoc.
   ============================================================================ */

USE master;
GO

/* Buoc 1: ngat moi ket noi dang mo toi database roi xoa */
IF DB_ID('utetra') IS NOT NULL
BEGIN
    ALTER DATABASE utetra SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE utetra;
    PRINT 'Da xoa database utetra cu';
END
GO

/* Buoc 2: tao lai database rong */
CREATE DATABASE utetra COLLATE Vietnamese_CI_AS;
GO

PRINT 'Da tao lai database utetra. Bay gio chay lai ung dung Spring Boot.';
PRINT 'Hibernate se tu tao bang voi cot nvarchar va DataSeeder tao lai du lieu mau.';
GO
