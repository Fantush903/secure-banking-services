-- ============================================================
-- ONLINE BANKING MANAGEMENT SYSTEM
-- Database Schema - MySQL 8.0+
-- Run this script first before starting the application
-- ============================================================

CREATE DATABASE IF NOT EXISTS banking_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE banking_db;

-- ── CREATE APPLICATION USER ──────────────────────────────────
-- Run as root / admin user:
-- CREATE USER 'bank_user'@'localhost' IDENTIFIED BY 'BankPass@2024';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON banking_db.* TO 'bank_user'@'localhost';
-- FLUSH PRIVILEGES;

-- ── TABLE: Branch ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS Branch (
    BranchID     INT AUTO_INCREMENT PRIMARY KEY,
    BranchName   VARCHAR(100) NOT NULL,
    IFSCCode     CHAR(11) UNIQUE NOT NULL,
    Address      TEXT,
    City         VARCHAR(50),
    State        VARCHAR(50),
    Phone        VARCHAR(15),
    ManagerID    INT,
    CreatedDate  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ── TABLE: Customer ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS Customer (
    CustomerID   INT AUTO_INCREMENT PRIMARY KEY,
    Name         VARCHAR(100) NOT NULL,
    Email        VARCHAR(100) UNIQUE NOT NULL,
    Phone        VARCHAR(15)  NOT NULL,
    Address      TEXT,
    DOB          DATE,
    AadhaarNo    CHAR(12) UNIQUE NOT NULL,
    PAN          CHAR(10) UNIQUE NOT NULL,
    Password     VARCHAR(255) NOT NULL,
    Role         ENUM('CUSTOMER','ADMIN') DEFAULT 'CUSTOMER',
    Status       ENUM('ACTIVE','INACTIVE','LOCKED') DEFAULT 'ACTIVE',
    FailedLogins INT DEFAULT 0,
    CreatedDate  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ── TABLE: Account ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS Account (
    AccountID     INT AUTO_INCREMENT PRIMARY KEY,
    CustomerID    INT NOT NULL,
    BranchID      INT NOT NULL DEFAULT 1,
    AccountNumber VARCHAR(16) UNIQUE,
    AccountType   ENUM('SAVINGS','CURRENT') NOT NULL,
    Balance       DECIMAL(15,2) DEFAULT 0.00,
    Status        ENUM('PENDING','ACTIVE','INACTIVE','CLOSED') DEFAULT 'PENDING',
    OpenedDate    DATE,
    CONSTRAINT chk_balance CHECK (Balance >= 0),
    FOREIGN KEY (CustomerID) REFERENCES Customer(CustomerID) ON DELETE CASCADE,
    FOREIGN KEY (BranchID)   REFERENCES Branch(BranchID)
);

-- ── TABLE: Transaction ───────────────────────────────────────
CREATE TABLE IF NOT EXISTS Transaction (
    TransactionID     INT AUTO_INCREMENT PRIMARY KEY,
    SenderAccountID   INT,
    ReceiverAccountID INT,
    Amount            DECIMAL(15,2) NOT NULL,
    Type              ENUM('TRANSFER','DEPOSIT','WITHDRAWAL') NOT NULL,
    Status            ENUM('SUCCESS','FAILED','PENDING') NOT NULL,
    Description       VARCHAR(255),
    Timestamp         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (SenderAccountID)   REFERENCES Account(AccountID),
    FOREIGN KEY (ReceiverAccountID) REFERENCES Account(AccountID)
);

-- ── TABLE: Loan ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS Loan (
    LoanID        INT AUTO_INCREMENT PRIMARY KEY,
    CustomerID    INT NOT NULL,
    AccountID     INT NOT NULL,
    LoanType      ENUM('HOME','PERSONAL','EDUCATION') NOT NULL,
    Principal     DECIMAL(15,2) NOT NULL,
    InterestRate  DECIMAL(5,2) NOT NULL,
    TenureMonths  INT NOT NULL,
    EMIAmount     DECIMAL(10,2),
    PaidMonths    INT DEFAULT 0,
    Status        ENUM('PENDING','APPROVED','REJECTED','CLOSED') DEFAULT 'PENDING',
    AppliedDate   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_tenure CHECK (TenureMonths BETWEEN 6 AND 360),
    FOREIGN KEY (CustomerID) REFERENCES Customer(CustomerID),
    FOREIGN KEY (AccountID)  REFERENCES Account(AccountID)
);

-- ── TABLE: AuditLog ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS AuditLog (
    LogID       INT AUTO_INCREMENT PRIMARY KEY,
    AdminID     INT NOT NULL,
    Action      VARCHAR(100) NOT NULL,
    TargetTable VARCHAR(50),
    TargetID    INT,
    OldValue    TEXT,
    NewValue    TEXT,
    Timestamp   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ── TRIGGER: Auto-log balance changes ────────────────────────
DELIMITER $$
CREATE TRIGGER IF NOT EXISTS trg_account_balance_change
AFTER UPDATE ON Account
FOR EACH ROW
BEGIN
    IF OLD.Balance != NEW.Balance THEN
        INSERT INTO AuditLog (AdminID, Action, TargetTable, TargetID, OldValue, NewValue)
        VALUES (0, 'BALANCE_CHANGE', 'Account', NEW.AccountID,
                CONCAT('Balance: ', OLD.Balance),
                CONCAT('Balance: ', NEW.Balance));
    END IF;
END$$
DELIMITER ;

-- ── SEED DATA: Default Branch ────────────────────────────────
INSERT IGNORE INTO Branch (BranchID, BranchName, IFSCCode, Address, City, State, Phone)
VALUES (1, 'Main Branch', 'BANK0001001', '123 Main Street', 'Dehradun', 'Uttarakhand', '0135-1234567');

-- ── SEED DATA: Admin Account ─────────────────────────────────
-- Password: Admin@1234  (BCrypt hash below)
INSERT IGNORE INTO Customer (CustomerID, Name, Email, Phone, Address, DOB, AadhaarNo, PAN, Password, Role, Status)
VALUES (1, 'System Admin', 'admin@bank.com', '9999999999', 'Bank HQ, Dehradun',
        '1990-01-01', '999999999999', 'ADMIN9999A',
        '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
        'ADMIN', 'ACTIVE');

-- ── SEED DATA: Sample Customer ───────────────────────────────
-- Password: Pass@1234
INSERT IGNORE INTO Customer (CustomerID, Name, Email, Phone, Address, DOB, AadhaarNo, PAN, Password, Role, Status)
VALUES (2, 'Rahul Sharma', 'rahul@example.com', '9876543210', '45 MG Road, Dehradun',
        '1995-06-15', '123456789012', 'ABCDE1234F',
        '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
        'CUSTOMER', 'ACTIVE');

INSERT IGNORE INTO Account (AccountID, CustomerID, BranchID, AccountNumber, AccountType, Balance, Status, OpenedDate)
VALUES (1, 2, 1, '1001000000000001', 'SAVINGS', 50000.00, 'ACTIVE', CURDATE());

-- ============================================================
-- SETUP COMPLETE
-- Default Admin Login:  admin@bank.com  / Admin@1234
-- Sample Customer:      rahul@example.com / Pass@1234
-- ============================================================
