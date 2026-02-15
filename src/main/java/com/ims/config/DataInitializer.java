package com.ims.config;

import com.ims.entity.Branch;
import com.ims.entity.Category;
import com.ims.entity.Product;
import com.ims.entity.User;
import com.ims.enums.Role;
import com.ims.repository.BranchRepository;
import com.ims.repository.CategoryRepository;
import com.ims.repository.ProductRepository;
import com.ims.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Initializing database with sample data...");
            initializeData();
            log.info("Sample data initialized successfully!");
        }
    }

    private void initializeData() {
        // Create Branches
        Branch mainBranch = Branch.builder()
                .code("BR01")
                .name("Main Branch")
                .address("123 Main Street")
                .phoneNumber("+237-123-4567")
                .city("Yaoundé")
                .country("Cameroon")
                .isActive(true)
                .build();
        mainBranch = branchRepository.save(mainBranch);

        Branch secondBranch = Branch.builder()
                .code("BR02")
                .name("Downtown Branch")
                .address("456 Commerce Ave")
                .phoneNumber("+237-234-5678")
                .city("Douala")
                .country("Cameroon")
                .isActive(true)
                .build();
        secondBranch = branchRepository.save(secondBranch);

        // Create Users
        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .fullName("System Administrator")
                .email("admin@ims.com")
                .phoneNumber("+237-100-0001")
                .role(Role.ADMIN)
                .branch(mainBranch)
                .isActive(true)
                .build();
        userRepository.save(admin);

        User manager = User.builder()
                .username("manager")
                .password(passwordEncoder.encode("manager123"))
                .fullName("Branch Manager")
                .email("manager@ims.com")
                .phoneNumber("+237-100-0002")
                .role(Role.MANAGER)
                .branch(mainBranch)
                .isActive(true)
                .build();
        userRepository.save(manager);

        User seller = User.builder()
                .username("seller")
                .password(passwordEncoder.encode("seller123"))
                .fullName("Sales Representative")
                .email("seller@ims.com")
                .phoneNumber("+237-100-0003")
                .role(Role.SELLER)
                .branch(mainBranch)
                .isActive(true)
                .build();
        userRepository.save(seller);

        // Create Categories
        Category brakes = Category.builder()
                .name("Brake System")
                .description("Brake pads, rotors, and components")
                .build();
        brakes = categoryRepository.save(brakes);

        Category engine = Category.builder()
                .name("Engine Parts")
                .description("Engine components and accessories")
                .build();
        engine = categoryRepository.save(engine);

        Category suspension = Category.builder()
                .name("Suspension")
                .description("Shocks, struts, and suspension components")
                .build();
        suspension = categoryRepository.save(suspension);

        // Create Sample Products
        Product brakePads = Product.builder()
                .sku("BRK-PAD-001")
                .name("Ceramic Brake Pads - Front")
                .description("High-performance ceramic brake pads for front wheels")
                .category(brakes)
                .brand("Brembo")
                .model("P06039")
                .barcode("1234567890123")
                .unitPrice(new BigDecimal("89.99"))
                .costPrice(new BigDecimal("45.00"))
                .unit("Set")
                .reorderLevel(10)
                .minimumStock(5)
                .isActive(true)
                .build();
        productRepository.save(brakePads);

        Product oilFilter = Product.builder()
                .sku("ENG-FILT-001")
                .name("Oil Filter")
                .description("Premium oil filter for most vehicles")
                .category(engine)
                .brand("Fram")
                .model("PH3593A")
                .barcode("2345678901234")
                .unitPrice(new BigDecimal("12.99"))
                .costPrice(new BigDecimal("6.50"))
                .unit("Piece")
                .reorderLevel(20)
                .minimumStock(10)
                .isActive(true)
                .build();
        productRepository.save(oilFilter);

        Product shockAbsorber = Product.builder()
                .sku("SUS-SHCK-001")
                .name("Front Shock Absorber")
                .description("Heavy-duty front shock absorber")
                .category(suspension)
                .brand("Monroe")
                .model("58640")
                .barcode("3456789012345")
                .unitPrice(new BigDecimal("75.50"))
                .costPrice(new BigDecimal("38.00"))
                .unit("Piece")
                .reorderLevel(8)
                .minimumStock(4)
                .isActive(true)
                .build();
        productRepository.save(shockAbsorber);

        log.info("Created {} branches, {} users, {} categories, {} products",
                branchRepository.count(),
                userRepository.count(),
                categoryRepository.count(),
                productRepository.count());
    }
}
