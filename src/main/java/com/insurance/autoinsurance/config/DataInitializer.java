package com.insurance.autoinsurance.config;
import com.insurance.autoinsurance.model.*;
import com.insurance.autoinsurance.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.logging.Logger;

@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = Logger.getLogger(DataInitializer.class.getName());

    private final PolicyRepository       polRepo;
    private final PaymentRepository      payRepo;
    private final AppUserRepository      userRepo;
    private final NotificationRepository notifRepo;
    private final PasswordEncoder        enc;

    public DataInitializer(PolicyRepository polRepo, PaymentRepository payRepo,
                           AppUserRepository userRepo, NotificationRepository notifRepo,
                           PasswordEncoder enc) {
        this.polRepo = polRepo; this.payRepo = payRepo;
        this.userRepo = userRepo; this.notifRepo = notifRepo; this.enc = enc;
    }

    @Override
    public void run(String... args) {
        long existing = polRepo.count();
        if (existing > 0) {
            log.info("════ DB already contains " + existing + " policies — skipping seed. ════");
            return;
        }
        log.info("════ First run — seeding enterprise demo data... ════");
        seedUsers();
        seedPolicies();
        seedNotifications();
        log.info("════ Seeded: " + polRepo.count() + " policies, " +
                 payRepo.count() + " payments, " + userRepo.count() + " users ════");
    }

    // ── Users ──────────────────────────────────────────────────────────────
    private void seedUsers() {
        userRepo.save(AppUser.builder()
            .username("admin").passwordHash(enc.encode("admin123"))
            .fullName("System Administrator").email("admin@insuretrack.com")
            .role(AppUser.Role.ROLE_ADMIN).enabled(true).build());
        userRepo.save(AppUser.builder()
            .username("manager").passwordHash(enc.encode("manager123"))
            .fullName("Operations Manager").email("manager@insuretrack.com")
            .role(AppUser.Role.ROLE_USER).enabled(true).build());
        userRepo.save(AppUser.builder()
            .username("agent").passwordHash(enc.encode("agent123"))
            .fullName("Insurance Agent").email("agent@insuretrack.com")
            .role(AppUser.Role.ROLE_USER).enabled(true).build());
        userRepo.save(AppUser.builder()
            .username("Vicky").passwordHash(enc.encode("Vicky@123"))
            .fullName("Vicky").email("vicky@insuretrack.com")
            .role(AppUser.Role.ROLE_USER).enabled(true).build());
        log.info("  Seeded 4 users: admin / manager / agent / Vicky");
    }

    // ── Policies + Payments — rich data across ALL months, ALL statuses ────
    private void seedPolicies() {
        LocalDate now = LocalDate.now();

        // ── ACTIVE policies ─────────────────────────────────────────────
        Policy p1 = pol("Comprehensive Auto Shield", "Ravi Kumar", 18000d,
            "MH02AB1234", "Honda City 2021", 2021, "Comprehensive",
            "ravi.kumar@email.com", "9876543210",
            ld(2024,1,15), ld(2026,1,15), Policy.PolicyStatus.ACTIVE);
        pay(p1,18000d,ld(2024,2,15),  Payment.PaymentStatus.SUCCESS, "Q1 Premium");
        pay(p1,18000d,ld(2024,3,15),  Payment.PaymentStatus.SUCCESS, null);
        pay(p1,18000d,ld(2024,4,15),  Payment.PaymentStatus.FAILED,  "Gateway timeout");
        pay(p1,18000d,ld(2024,5,15),  Payment.PaymentStatus.SUCCESS, null);
        pay(p1,18000d,ld(2024,6,15),  Payment.PaymentStatus.SUCCESS, null);
        pay(p1,18000d,ld(2024,7,15),  Payment.PaymentStatus.SUCCESS, null);
        pay(p1,18000d,ld(2024,8,15),  Payment.PaymentStatus.FAILED,  "Card declined");
        pay(p1,18000d,ld(2024,9,15),  Payment.PaymentStatus.SUCCESS, null);
        pay(p1,18000d,ld(2024,10,15), Payment.PaymentStatus.SUCCESS, null);
        pay(p1,18000d,ld(2024,11,15), Payment.PaymentStatus.SUCCESS, null);
        pay(p1,18000d,ld(2024,12,15), Payment.PaymentStatus.SUCCESS, "Year-end payment");
        pay(p1,18000d,ld(2025,1,15),  Payment.PaymentStatus.SUCCESS, null);
        pay(p1,18000d,ld(2025,2,15),  Payment.PaymentStatus.PENDING, "Processing");

        Policy p2 = pol("Zero Depreciation Elite", "Anita Patel", 22000d,
            "GJ01GH3456", "Toyota Fortuner 2023", 2023, "Zero Dep",
            "anita.patel@email.com", "9871234560",
            ld(2023,7,1), ld(2026,7,1), Policy.PolicyStatus.ACTIVE);
        pay(p2,22000d,ld(2023,8,1),   Payment.PaymentStatus.SUCCESS, null);
        pay(p2,22000d,ld(2023,9,1),   Payment.PaymentStatus.SUCCESS, null);
        pay(p2,22000d,ld(2023,10,1),  Payment.PaymentStatus.SUCCESS, null);
        pay(p2,22000d,ld(2023,11,1),  Payment.PaymentStatus.SUCCESS, null);
        pay(p2,22000d,ld(2023,12,1),  Payment.PaymentStatus.SUCCESS, "Dec payment");
        pay(p2,22000d,ld(2024,1,1),   Payment.PaymentStatus.SUCCESS, null);
        pay(p2,22000d,ld(2024,3,1),   Payment.PaymentStatus.FAILED,  "Insufficient funds");
        pay(p2,22000d,ld(2024,4,1),   Payment.PaymentStatus.SUCCESS, null);
        pay(p2,22000d,ld(2024,6,1),   Payment.PaymentStatus.SUCCESS, null);
        pay(p2,22000d,ld(2024,9,1),   Payment.PaymentStatus.SUCCESS, null);
        pay(p2,22000d,ld(2024,12,1),  Payment.PaymentStatus.SUCCESS, null);
        pay(p2,22000d,ld(2025,3,1),   Payment.PaymentStatus.SUCCESS, null);

        Policy p3 = pol("EV Shield Premium", "Kavya Nair", 25000d,
            "KL07KL2345", "Tata Nexon EV 2023", 2023, "EV Comprehensive",
            "kavya.nair@email.com", "9654321098",
            ld(2024,4,10), ld(2026,4,10), Policy.PolicyStatus.ACTIVE);
        pay(p3,25000d,ld(2024,5,10),  Payment.PaymentStatus.SUCCESS, null);
        pay(p3,25000d,ld(2024,6,10),  Payment.PaymentStatus.SUCCESS, null);
        pay(p3,25000d,ld(2024,7,10),  Payment.PaymentStatus.SUCCESS, null);
        pay(p3,25000d,ld(2024,8,10),  Payment.PaymentStatus.FAILED,  "Bank server error");
        pay(p3,25000d,ld(2024,9,10),  Payment.PaymentStatus.SUCCESS, null);
        pay(p3,25000d,ld(2024,10,10), Payment.PaymentStatus.SUCCESS, null);
        pay(p3,25000d,ld(2024,11,10), Payment.PaymentStatus.PENDING, "Processing");

        Policy p4 = pol("Commercial Fleet Cover", "Mohammed Ansari", 45000d,
            "UP32IJ7890", "Tata Ace 2022", 2022, "Commercial",
            "m.ansari@business.com", "9765432100",
            ld(2023,5,20), ld(2026,5,20), Policy.PolicyStatus.ACTIVE);
        pay(p4,45000d,ld(2023,6,20),  Payment.PaymentStatus.SUCCESS, "Fleet Q1");
        pay(p4,45000d,ld(2023,9,20),  Payment.PaymentStatus.SUCCESS, "Fleet Q2");
        pay(p4,45000d,ld(2023,12,20), Payment.PaymentStatus.SUCCESS, "Fleet Q3");
        pay(p4,45000d,ld(2024,3,20),  Payment.PaymentStatus.SUCCESS, "Fleet Q4");
        pay(p4,45000d,ld(2024,6,20),  Payment.PaymentStatus.SUCCESS, "Fleet Q5");
        pay(p4,45000d,ld(2024,9,20),  Payment.PaymentStatus.SUCCESS, "Fleet Q6");
        pay(p4,45000d,ld(2024,12,20), Payment.PaymentStatus.FAILED,  "Routing error");
        pay(p4,45000d,ld(2025,1,20),  Payment.PaymentStatus.SUCCESS, "Retry success");

        Policy p5 = pol("Own Damage Premier", "Suresh Reddy", 14000d,
            "TS09EF9012", "Hyundai i20 2022", 2022, "Own Damage",
            "suresh.reddy@email.com", "9988776655",
            ld(2024,3,1), ld(2025,3,1), Policy.PolicyStatus.ACTIVE);
        pay(p5,14000d,ld(2024,4,1),   Payment.PaymentStatus.SUCCESS, null);
        pay(p5,14000d,ld(2024,5,1),   Payment.PaymentStatus.SUCCESS, null);
        pay(p5,14000d,ld(2024,6,1),   Payment.PaymentStatus.PENDING, "Processing");

        // ── Expiring policies — within NEXT 8 months (as requested) ─────
        Policy exp1 = pol("Budget Third Party Cover", "Priya Sharma", 7500d,
            "DL05CD5678", "Maruti Swift 2022", 2022, "Third Party",
            "priya.sharma@email.com", "9123456789",
            ld(2024,4,15), now.plusDays(18), Policy.PolicyStatus.ACTIVE);
        pay(exp1,7500d,ld(2024,5,15),  Payment.PaymentStatus.SUCCESS, null);
        pay(exp1,7500d,ld(2024,8,15),  Payment.PaymentStatus.SUCCESS, null);
        pay(exp1,7500d,ld(2024,11,15), Payment.PaymentStatus.SUCCESS, null);
        pay(exp1,7500d,ld(2025,2,15),  Payment.PaymentStatus.FAILED,  "Card expired");

        Policy exp2 = pol("Mid-Range Own Damage", "Arjun Verma", 16500d,
            "RJ14PQ3421", "Maruti Ertiga 2021", 2021, "Own Damage",
            "arjun.verma@email.com", "9712345678",
            ld(2024,3,1), now.plusDays(42), Policy.PolicyStatus.ACTIVE);
        pay(exp2,16500d,ld(2024,4,1),  Payment.PaymentStatus.SUCCESS, null);
        pay(exp2,16500d,ld(2024,7,1),  Payment.PaymentStatus.SUCCESS, null);
        pay(exp2,16500d,ld(2024,10,1), Payment.PaymentStatus.SUCCESS, null);

        Policy exp3 = pol("SUV Comprehensive Plus", "Deepa Krishnan", 32000d,
            "KA03RJ5566", "Mahindra XUV700 2022", 2022, "Comprehensive",
            "deepa.krishnan@email.com", "9845678901",
            ld(2024,5,10), now.plusDays(75), Policy.PolicyStatus.ACTIVE);
        pay(exp3,32000d,ld(2024,6,10),  Payment.PaymentStatus.SUCCESS, null);
        pay(exp3,32000d,ld(2024,9,10),  Payment.PaymentStatus.SUCCESS, null);
        pay(exp3,32000d,ld(2024,12,10), Payment.PaymentStatus.SUCCESS, null);
        pay(exp3,32000d,ld(2025,1,10),  Payment.PaymentStatus.FAILED,  "Network error");

        Policy exp4 = pol("Hatchback Shield Basic", "Rahul Gupta", 9800d,
            "WB20CD7788", "Tata Altroz 2023", 2023, "Third Party",
            "rahul.gupta@email.com", "9934567890",
            ld(2024,7,22), now.plusDays(95), Policy.PolicyStatus.ACTIVE);
        pay(exp4,9800d,ld(2024,8,22),  Payment.PaymentStatus.SUCCESS, null);
        pay(exp4,9800d,ld(2024,11,22), Payment.PaymentStatus.SUCCESS, null);

        Policy exp5 = pol("Sedan Comprehensive Cover", "Meena Joshi", 19500d,
            "MP09XY4433", "Honda Amaze 2022", 2022, "Comprehensive",
            "meena.joshi@email.com", "9867891234",
            ld(2024,6,5), now.plusDays(115), Policy.PolicyStatus.ACTIVE);
        pay(exp5,19500d,ld(2024,7,5),  Payment.PaymentStatus.SUCCESS, null);
        pay(exp5,19500d,ld(2024,10,5), Payment.PaymentStatus.SUCCESS, null);
        pay(exp5,19500d,ld(2025,1,5),  Payment.PaymentStatus.PENDING, "Processing");

        Policy exp6 = pol("Premium EV Zero Dep", "Sanjay Mehta", 38000d,
            "MH04ZZ1122", "BYD Atto 3 2024", 2024, "EV Comprehensive",
            "sanjay.mehta@email.com", "9823456789",
            ld(2024,8,1), now.plusDays(145), Policy.PolicyStatus.ACTIVE);
        pay(exp6,38000d,ld(2024,9,1),  Payment.PaymentStatus.SUCCESS, null);
        pay(exp6,38000d,ld(2024,12,1), Payment.PaymentStatus.SUCCESS, null);

        Policy exp7 = pol("Pickup Truck Commercial", "Rajesh Singh", 28000d,
            "HR26FG8899", "Mahindra Bolero 2021", 2021, "Commercial",
            "rajesh.singh@email.com", "9778901234",
            ld(2024,2,14), now.plusDays(175), Policy.PolicyStatus.ACTIVE);
        pay(exp7,28000d,ld(2024,3,14),  Payment.PaymentStatus.SUCCESS, null);
        pay(exp7,28000d,ld(2024,6,14),  Payment.PaymentStatus.SUCCESS, null);
        pay(exp7,28000d,ld(2024,9,14),  Payment.PaymentStatus.SUCCESS, null);
        pay(exp7,28000d,ld(2024,12,14), Payment.PaymentStatus.FAILED,  "UPI timeout");

        Policy exp8 = pol("Two-Wheeler Add-on Shield", "Nisha Bansal", 4200d,
            "UP16AB5577", "Honda Activa 2022", 2022, "Comprehensive",
            "nisha.bansal@email.com", "9756789012",
            ld(2024,9,20), now.plusDays(220), Policy.PolicyStatus.ACTIVE);
        pay(exp8,4200d,ld(2024,10,20), Payment.PaymentStatus.SUCCESS, null);
        pay(exp8,4200d,ld(2025,1,20),  Payment.PaymentStatus.SUCCESS, null);

        // ── EXPIRED policies ─────────────────────────────────────────────
        Policy pExp1 = pol("Classic Car Heritage Cover", "Vijay Malhotra", 12000d,
            "MH01AA0011", "Maruti 800 2010", 2010, "Comprehensive",
            "vijay.malhotra@email.com", "9812345678",
            ld(2022,1,1), ld(2023,1,1), Policy.PolicyStatus.EXPIRED);
        pay(pExp1,12000d,ld(2022,2,1),  Payment.PaymentStatus.SUCCESS, null);
        pay(pExp1,12000d,ld(2022,5,1),  Payment.PaymentStatus.SUCCESS, null);
        pay(pExp1,12000d,ld(2022,8,1),  Payment.PaymentStatus.FAILED,  "Expired card");
        pay(pExp1,12000d,ld(2022,11,1), Payment.PaymentStatus.SUCCESS, null);

        Policy pExp2 = pol("Third Party Budget FY23", "Lakshmi Rao", 6500d,
            "TN10BB2233", "Ashok Leyland 2019", 2019, "Third Party",
            "lakshmi.rao@email.com", "9789012345",
            ld(2022,6,15), ld(2023,6,15), Policy.PolicyStatus.EXPIRED);
        pay(pExp2,6500d,ld(2022,7,15),  Payment.PaymentStatus.SUCCESS, null);
        pay(pExp2,6500d,ld(2022,12,15), Payment.PaymentStatus.SUCCESS, null);

        // ── CANCELLED policy ─────────────────────────────────────────────
        Policy pCan = pol("Cancelled Fleet Policy", "Arun Bose", 55000d,
            "WB01CC4455", "Tata Tigor Fleet", 2020, "Commercial",
            "arun.bose@fleet.com", "9745678901",
            ld(2023,3,1), ld(2025,3,1), Policy.PolicyStatus.CANCELLED);
        pay(pCan,55000d,ld(2023,4,1),  Payment.PaymentStatus.SUCCESS, null);
        pay(pCan,55000d,ld(2023,7,1),  Payment.PaymentStatus.FAILED,  "Dispute raised");

        // ── RENEWED policy ───────────────────────────────────────────────
        Policy pRen = pol("Renewed SUV Cover FY25", "Pooja Iyer", 26000d,
            "KA09DD6677", "Kia Seltos 2022", 2022, "Comprehensive",
            "pooja.iyer@email.com", "9734567890",
            ld(2024,1,1), ld(2026,1,1), Policy.PolicyStatus.RENEWED);
        pay(pRen,26000d,ld(2024,2,1),  Payment.PaymentStatus.SUCCESS, "Renewal payment");
        pay(pRen,26000d,ld(2024,5,1),  Payment.PaymentStatus.SUCCESS, null);
        pay(pRen,26000d,ld(2024,8,1),  Payment.PaymentStatus.SUCCESS, null);
        pay(pRen,26000d,ld(2024,11,1), Payment.PaymentStatus.SUCCESS, null);
        pay(pRen,26000d,ld(2025,2,1),  Payment.PaymentStatus.SUCCESS, null);

        // ── SUSPENDED policy ─────────────────────────────────────────────
        Policy pSus = pol("Suspended OD Policy", "Farhan Sheikh", 13000d,
            "GJ05EE8899", "Renault Kwid 2021", 2021, "Own Damage",
            "farhan.sheikh@email.com", "9712345890",
            ld(2024,1,1), ld(2025,1,1), Policy.PolicyStatus.SUSPENDED);
        pay(pSus,13000d,ld(2024,2,1),  Payment.PaymentStatus.SUCCESS, null);
        pay(pSus,13000d,ld(2024,4,1),  Payment.PaymentStatus.FAILED,  "Suspended — non-payment");

        // ── DRAFT policy ─────────────────────────────────────────────────
        Policy pDraft = pol("New Application Draft", "Sunita Kapoor", 21000d,
            "PB10FF3344", "Hyundai Creta 2024", 2024, "Comprehensive",
            "sunita.kapoor@email.com", "9701234567",
            now.plusDays(5), now.plusYears(1).plusDays(5), Policy.PolicyStatus.DRAFT);

        log.info("  Seeded " + polRepo.count() + " policies with " + payRepo.count() + " payments");
    }

    // ── Notifications ──────────────────────────────────────────────────────
    private void seedNotifications() {
        LocalDate now = LocalDate.now();
        notifRepo.save(Notification.builder()
            .type(Notification.Type.EXPIRY_WARNING)
            .title("3 Policies Expiring This Month")
            .message("Policies are expiring within 30 days. Please contact holders for renewal.")
            .build());
        notifRepo.save(Notification.builder()
            .type(Notification.Type.PAYMENT_FAILED)
            .title("Payment Failed")
            .message("Payment for Commercial Fleet Cover failed due to routing error.")
            .build());
        notifRepo.save(Notification.builder()
            .type(Notification.Type.SYSTEM)
            .title("System Initialized")
            .message("InsureTrack Enterprise system initialized with demo data on " + now)
            .build());
        log.info("  Seeded 3 initial notifications");
    }

    private Policy pol(String name, String holder, Double amt, String vNum, String vMod,
                       Integer vYr, String cov, String email, String phone,
                       LocalDate s, LocalDate e, Policy.PolicyStatus st) {
        return polRepo.save(Policy.builder()
            .policyName(name).policyHolderName(holder).policyAmount(amt)
            .vehicleNumber(vNum).vehicleModel(vMod).vehicleYear(vYr)
            .coverageType(cov).holderEmail(email).holderPhone(phone)
            .policyStartDate(s).policyEndDate(e).policyStatus(st)
            .createdBy("system").build());
    }

    private void pay(Policy pol, Double amt, LocalDate d,
                     Payment.PaymentStatus st, String remarks) {
        payRepo.save(Payment.builder()
            .policy(pol).paymentAmount(amt).paymentDate(d)
            .paymentStatus(st).retryCount(0).remarks(remarks).build());
    }

    private LocalDate ld(int y, int m, int d) { return LocalDate.of(y, m, d); }
}
