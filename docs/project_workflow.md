# 🔄 Project Workflow — InsureTrack Auto Insurance Platform

Development workflow, coding patterns, and how to extend the project.

---

## Development Workflow

### 1. Start the Dev Environment

```bash
# Default profile is 'local' — file-based H2, data persists across restarts
mvn spring-boot:run

# Watch for this log line — means app is fully ready:
# Started AutoInsuranceApplication in X.XXX seconds
```

To use MySQL locally instead:
```bash
SPRING_PROFILES_ACTIVE=mysql mvn spring-boot:run
```

### 2. Hot Reload with Spring DevTools

Add to `pom.xml` for automatic restart on file save:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

With DevTools active, the application restarts automatically whenever a Java class is recompiled in your IDE.

### 3. Inspecting State During Development

For backend changes:
- Use the H2 console at `http://localhost:8080/h2-console` to run SQL queries and inspect table state
- Enable SQL logging temporarily: `logging.level.org.hibernate.SQL=DEBUG` in `application.properties`

For frontend changes:
- Hard-refresh (`Ctrl+Shift+R`) to bypass browser caching after CSS/JS edits

---

## Git Workflow

### Branching Strategy

```
main          ← Production-ready code — deploys automatically to Render
  └── develop ← Integration branch
        ├── feature/premium-schedule-improvements
        ├── feature/email-notifications
        └── bugfix/advance-payment-edge-case
```

### Commit Conventions

```bash
# New feature
git commit -m "feat: add advance payment eligibility API endpoint"

# Bug fix
git commit -m "fix: handle null monthlyPremium in effectiveMonthlyPremium()"

# Documentation
git commit -m "docs: update premium schedule flow in PROCESS_FLOW.md"

# Refactor
git commit -m "refactor: extract late fee logic to dedicated method in PremiumScheduleService"

# Configuration
git commit -m "config: tune HikariCP pool size for Render free tier"
```

---

## Adding a New Feature — Step-by-Step

Example: Adding a **Claims Management** module.

### Step 1: Create the Entity

```java
// src/main/java/com/insurance/autoinsurance/model/Claim.java
@Entity
@Table(name = "claims")
public class Claim {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long claimId;

    @ManyToOne
    @JoinColumn(name = "policy_id")
    private Policy policy;

    private String claimDescription;
    private Double claimAmount;
    private LocalDate claimDate;

    @Enumerated(EnumType.STRING)
    private ClaimStatus claimStatus = ClaimStatus.PENDING;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void pre() { createdAt = LocalDateTime.now(); }

    public enum ClaimStatus { PENDING, APPROVED, REJECTED, SETTLED }

    // getters / setters / builder...
}
```

### Step 2: Create the Repository

```java
// src/.../repository/ClaimRepository.java
public interface ClaimRepository extends JpaRepository<Claim, Long> {

    List<Claim> findByPolicy_PolicyIdOrderByClaimDateDesc(Long policyId);

    List<Claim> findByClaimStatus(Claim.ClaimStatus status);
}
```

### Step 3: Create the Service

```java
// src/.../service/ClaimService.java
@Service
@Transactional
public class ClaimService {

    private final ClaimRepository claimRepo;
    private final AuditService audit;

    public ClaimService(ClaimRepository claimRepo, AuditService audit) {
        this.claimRepo = claimRepo;
        this.audit = audit;
    }

    public Claim submitClaim(Long policyId, String description, Double amount) {
        // business logic...
        Claim saved = claimRepo.save(claim);
        audit.log(AuditLog.ActionType.POLICY_UPDATED, "Claim",
            saved.getClaimId(), "Claim submitted: " + description);
        return saved;
    }
}
```

### Step 4: Create the Controller

```java
// src/.../controller/ClaimController.java
@Controller
@RequestMapping("/claims")
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("claims", claimService.getAllClaims());
        model.addAttribute("activePage", "claims");
        return "claims/list";
    }

    @PostMapping("/add")
    public String add(@Valid @ModelAttribute Claim claim,
                      BindingResult result,
                      RedirectAttributes flash) {
        if (result.hasErrors()) return "claims/form";
        claimService.submitClaim(claim);
        flash.addFlashAttribute("successMsg", "Claim submitted successfully.");
        return "redirect:/claims";
    }
}
```

### Step 5: Create Thymeleaf Templates

```
src/main/resources/templates/claims/
├── list.html    ← Extend fragments/navbar.html and fragments/footer.html
└── form.html
```

### Step 6: Add Navigation Link

Edit `templates/fragments/navbar.html`:

```html
<li class="nav-item">
    <a class="nav-link" th:href="@{/claims}"
       th:classappend="${activePage == 'claims'} ? 'active' : ''">
        Claims
    </a>
</li>
```

### Step 7: Add Security Rules (if needed)

Edit `SecurityConfig.java` if the route requires special access control:

```java
.requestMatchers("/claims/approve/**").hasAuthority("ROLE_ADMIN")
```

---

## Code Patterns to Follow

### Controller Pattern

```java
@Controller
@RequestMapping("/module")
public class ModuleController {

    private final ModuleService service;

    // Constructor injection — NOT field @Autowired
    public ModuleController(ModuleService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", service.getAll());
        model.addAttribute("activePage", "module");  // drives navbar active state
        return "module/list";
    }

    @PostMapping("/add")
    public String add(@Valid @ModelAttribute Entity entity,
                      BindingResult result,
                      RedirectAttributes flash) {
        if (result.hasErrors()) return "module/form";
        service.save(entity);
        flash.addFlashAttribute("successMsg", "Saved successfully.");
        return "redirect:/module";
    }
}
```

### Service Pattern

```java
@Service
@Transactional
public class ModuleService {

    private final ModuleRepository repo;
    private final AuditService audit;

    public ModuleService(ModuleRepository repo, AuditService audit) {
        this.repo = repo;
        this.audit = audit;
    }

    public Entity save(Entity entity) {
        Entity saved = repo.save(entity);
        audit.log(AuditLog.ActionType.POLICY_CREATED, "Entity",
            saved.getId(), "Created: " + saved.getName());
        return saved;
    }
}
```

### Repository Pattern

```java
public interface ModuleRepository extends JpaRepository<Entity, Long> {

    // Spring Data derives the query from the method name:
    List<Entity> findByStatusOrderByCreatedAtDesc(Status status);

    // Use @Query for complex queries:
    @Query("SELECT e FROM Entity e WHERE e.endDate < :date AND e.status = 'ACTIVE'")
    List<Entity> findExpiringBefore(@Param("date") LocalDate date);
}
```

---

## Testing Strategy

### Unit Tests (Service Layer)

```java
@ExtendWith(MockitoExtension.class)
class PremiumScheduleServiceTest {

    @Mock PolicyRepository policyRepo;
    @Mock PaymentRepository paymentRepo;
    @Mock AuditService auditService;
    @InjectMocks PremiumScheduleService premiumService;

    @Test
    void shouldCalculateZeroLateFeeWhenPaidOnTime() {
        LocalDate dueDate = LocalDate.now().plusDays(3);
        double fee = premiumService.calculateLateFee(dueDate, LocalDate.now());
        assertEquals(0.0, fee);
    }

    @Test
    void shouldCalculateTier1LateFeeWhenOverdueByTenDays() {
        LocalDate dueDate = LocalDate.now().minusDays(10);
        double fee = premiumService.calculateLateFee(dueDate, LocalDate.now());
        assertEquals(PremiumScheduleService.LATE_FEE_TIER1, fee); // ₹30
    }

    @Test
    void shouldCalculateTier2LateFeeWhenOverdueByFortyDays() {
        LocalDate dueDate = LocalDate.now().minusDays(40);
        double fee = premiumService.calculateLateFee(dueDate, LocalDate.now());
        assertEquals(PremiumScheduleService.LATE_FEE_TIER2, fee); // ₹100
    }
}
```

### Unit Tests (Policy Service)

```java
@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock PolicyRepository policyRepo;
    @Mock AuditService auditService;
    @InjectMocks PolicyService policyService;

    @Test
    void shouldCreatePolicyAndAuditLog() {
        Policy policy = Policy.builder()
            .policyName("Test Policy")
            .policyHolderName("Test Holder")
            .policyAmount(12000.0)
            .build();
        when(policyRepo.save(any())).thenReturn(policy);

        policyService.createPolicy(policy);
        verify(auditService).log(any(), eq("Policy"), any(), any());
    }
}
```

### Integration Tests (Controller Layer)

```java
@SpringBootTest
@AutoConfigureMockMvc
class PolicyControllerTest {

    @Autowired MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin", authorities = "ROLE_ADMIN")
    void shouldLoadPolicyList() throws Exception {
        mockMvc.perform(get("/policies"))
            .andExpect(status().isOk())
            .andExpect(view().name("policies/list"));
    }

    @Test
    @WithMockUser(username = "user", authorities = "ROLE_USER")
    void shouldDenyAdminUserManagementToNonAdmin() throws Exception {
        mockMvc.perform(get("/admin/users"))
            .andExpect(status().isForbidden());
    }
}
```

Run all tests:
```bash
mvn test
```

---

## Release Process

```bash
# 1. Ensure all tests pass
mvn test

# 2. Update version in pom.xml if releasing a new version
#    Change <version>2.1.0</version> to <version>2.2.0</version>

# 3. Build production JAR
mvn clean package -DskipTests

# 4. Commit, tag, and push
git add .
git commit -m "release: v2.2.0 — add claims management module"
git tag v2.2.0
git push origin main --tags

# 5. Render auto-deploys on push to main
#    Monitor: Render Dashboard → Web Service → Logs
#    Startup confirmation: "Started AutoInsuranceApplication"
```

---

## Useful Development Commands

```bash
# Run with explicit profile
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run

# Skip tests during build
mvn clean package -DskipTests

# Run only unit tests
mvn test

# Check for dependency updates
mvn versions:display-dependency-updates

# Show effective pom (resolved)
mvn help:effective-pom

# Clear local Maven cache (if dependency issues)
rm -rf ~/.m2/repository/com/insurance
mvn clean install -DskipTests
```
